package com.example.petshop.repository;

import com.example.petshop.model.entity.Voucher;
import com.example.petshop.model.entity.Notification;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class VoucherRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL = "vouchers";
    private static final String COL_USAGE = "voucher_usage";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAll(Callback<List<Voucher>> cb) {
        db.collection(COL)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Voucher> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(Voucher.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // Get system vouchers (visible to all users)
    public void getSystemVouchers(Callback<List<Voucher>> cb) {
        db.collection(COL)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Voucher> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Voucher v = doc.toObject(Voucher.class);
                        if (v != null) {
                            v.setId(doc.getId());
                            list.add(v);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByCode(String code, Callback<Voucher> cb) {
        db.collection(COL)
                .whereEqualTo("code", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        cb.onSuccess(snap.getDocuments().get(0).toObject(Voucher.class));
                    } else {
                        cb.onFailure("Voucher not found");
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void add(Voucher voucher, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        voucher.setId(id);
        voucher.setCreatedAt(Timestamp.now().toString());
        voucher.setUsedCount(0);
        db.collection(COL).document(id).set(voucher)
                .addOnSuccessListener(v -> {
                    cb.onSuccess(id);
                    if (voucher != null && voucher.isActive()) {
                        sendVoucherNotification(voucher);
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void update(Voucher voucher, Callback<Void> cb) {
        db.collection(COL).document(voucher.getId()).set(voucher)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void delete(String id, Callback<Void> cb) {
        db.collection(COL).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void toggleActive(String id, boolean isActive, Callback<Void> cb) {
        // Get voucher info first to build notification message
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    Voucher voucher = doc.toObject(Voucher.class);
                    if (voucher != null) voucher.setId(doc.getId());

                    db.collection(COL).document(id).update("isActive", isActive)
                            .addOnSuccessListener(v -> {
                                cb.onSuccess(null);
                                if (isActive && voucher != null) {
                                    voucher.setActive(true);
                                    sendVoucherNotification(voucher);
                                }
                            })
                            .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // Track user voucher usage
    public void recordVoucherUsage(String userId, String voucherId, Callback<Void> cb) {
        String usageId = userId + "_" + voucherId + "_" + System.currentTimeMillis();
        db.collection(COL).document(voucherId)
                .collection(COL_USAGE).document(usageId)
                .set(java.util.Collections.singletonMap("timestamp", Timestamp.now()))
                .addOnSuccessListener(v -> {
                    // Increment usage count
                    incrementUsageCount(voucherId, cb);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getUserVoucherUsageCount(String userId, String voucherId, Callback<Long> cb) {
        db.collection(COL).document(voucherId)
                .collection(COL_USAGE)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess((long) snap.size()))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private void incrementUsageCount(String voucherId, Callback<Void> cb) {
        db.collection(COL).document(voucherId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Voucher v = doc.toObject(Voucher.class);
                        v.setUsedCount(v.getUsedCount() + 1);
                        update(v, cb);
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /**
     * Giảm usedCount khi đơn hàng bị huỷ (hoàn lại lượt sử dụng voucher).
     */
    public void decrementUsageCount(String voucherId, Callback<Void> cb) {
        db.collection(COL).document(voucherId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Voucher v = doc.toObject(Voucher.class);
                        v.setUsedCount(Math.max(0, v.getUsedCount() - 1));
                        update(v, cb);
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private void sendVoucherNotification(Voucher voucher) {
        if (voucher == null) return;
        if (!voucher.isActive()) return;

        db.collection("users")
                .whereEqualTo("role", "CUSTOMER")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;

                    String title = "Voucher mới! 🎁";
                    String message = buildVoucherMessage(voucher);
                    String createdAt = Timestamp.now().toString();

                    for (var doc : snap.getDocuments()) {
                        String notifId = UUID.randomUUID().toString() + "_" + doc.getId();
                        Notification notif = new Notification();
                        notif.setId(notifId);
                        notif.setUserId(doc.getId());
                        notif.setTitle(title);
                        notif.setMessage(message);
                        notif.setType("PROMO");
                        notif.setCreatedAt(createdAt);
                        notif.setRead(false);

                        db.collection("notifications").document(notifId).set(notif)
                                .addOnFailureListener(e ->
                                        android.util.Log.e("VoucherRepo", "sendVoucherNotification FAILED for user=" + doc.getId() + ": " + e.getMessage()));
                    }
                });
    }

    private String buildVoucherMessage(Voucher voucher) {
        String code = voucher.getCode() != null ? voucher.getCode() : "";
        String discountText;
        try {
            String type = voucher.getType() != null ? voucher.getType() : "";
            if (Voucher.TYPE_FREESHIP.equals(type)) {
                discountText = "Miễn phí vận chuyển";
            } else if (Voucher.TYPE_PERCENT.equals(type)) {
                discountText = "Giảm " + (int) voucher.getDiscountValue() + "%";
            } else {
                discountText = "Giảm " + formatPrice(voucher.getDiscountValue());
            }
        } catch (Exception e) {
            discountText = "Có ưu đãi mới";
        }

        String minOrderText = "";
        try {
            if (voucher.getMinOrderAmount() > 0) {
                minOrderText = "\nĐơn tối thiểu: " + formatPrice(voucher.getMinOrderAmount());
            }
        } catch (Exception ignored) { }

        return "Mã: " + code + "\n" + discountText + minOrderText;
    }

    private String formatPrice(double price) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format((long) price) + "đ";
    }
}
