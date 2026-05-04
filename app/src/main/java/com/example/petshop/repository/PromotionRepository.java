package com.example.petshop.repository;

import com.example.petshop.model.entity.Notification;
import com.example.petshop.model.entity.Promotion;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PromotionRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL = "promotions";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAll(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            list.add(p);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getById(String id, Callback<Promotion> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    Promotion p = doc.toObject(Promotion.class);
                    if (p != null) p.setId(doc.getId());
                    cb.onSuccess(p);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByCode(String code, Callback<Promotion> cb) {
        db.collection(COL)
                .whereEqualTo("voucherCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Promotion p = snap.getDocuments().get(0).toObject(Promotion.class);
                        if (p != null) p.setId(snap.getDocuments().get(0).getId());
                        cb.onSuccess(p);
                    } else {
                        cb.onFailure("Không tìm thấy mã voucher");
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getSystemVouchers(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .whereEqualTo("promotionType", "VOUCHER")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        if (p != null && p.isWithinDateRange()) {
                            p.setId(doc.getId());
                            list.add(p);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getActive(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            if (isNotExpired(p.getEndDate())) {
                                list.add(p);
                            }
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void add(Promotion promotion, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        promotion.setId(id);
        promotion.setCreatedAt(Timestamp.now().toString());
        promotion.setUpdatedAt(Timestamp.now().toString());
        promotion.setUsageCount(0);
        db.collection(COL).document(id).set(promotion)
                .addOnSuccessListener(v -> {
                    cb.onSuccess(id);
                    // Gửi thông báo khuyến mãi cho tất cả khách hàng
                    sendPromotionNotification(promotion);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private void sendPromotionNotification(Promotion promotion) {
        if (promotion == null || !promotion.isActive()) return;

        // Lấy tất cả khách hàng
        db.collection("users")
                .whereEqualTo("role", "CUSTOMER")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;

                    String title = "Khuyến mãi mới! 🎉";
                    String discountText = promotion.isPercentType()
                            ? " giảm " + (int) promotion.getDiscountValue() + "%"
                            : " giảm " + formatPrice(promotion.getDiscountValue());
                    String message = promotion.getName() + discountText
                            + (promotion.getVoucherCode() != null && !promotion.getVoucherCode().isEmpty()
                                ? "\nMã: " + promotion.getVoucherCode()
                                : "");

                    String createdAt = Timestamp.now().toString();

                    for (var doc : snap.getDocuments()) {
                        String notifId = System.currentTimeMillis() + "_" + doc.getId();
                        Notification notif = new Notification();
                        notif.setId(notifId);
                        notif.setUserId(doc.getId());
                        notif.setTitle(title);
                        notif.setMessage(message);
                        notif.setType("PROMO");
                        notif.setCreatedAt(createdAt);
                        notif.setRead(false);
                        db.collection("notifications").document(notifId).set(notif);
                    }
                })
                .addOnFailureListener(e -> {
                    // Silent fail - notification is not critical
                });
    }

    private String formatPrice(double price) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format((long) price) + "đ";
    }

    public void update(Promotion promotion, Callback<Void> cb) {
        promotion.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL).document(promotion.getId()).set(promotion)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void delete(String id, Callback<Void> cb) {
        db.collection(COL).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void toggleActive(String id, boolean isActive, Callback<Void> cb) {
        db.collection(COL).document(id).update("active", isActive)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void incrementUsageCount(String id, Callback<Void> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Promotion p = doc.toObject(Promotion.class);
                        p.setUsageCount(p.getUsageCount() + 1);
                        update(p, cb);
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private boolean isNotExpired(String endDate) {
        if (endDate == null || endDate.isEmpty()) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date end = sdf.parse(endDate);
            return end != null && !end.before(stripTime(new Date()));
        } catch (Exception e) {
            return true;
        }
    }

    private Date stripTime(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.parse(sdf.format(date));
        } catch (Exception e) {
            return date;
        }
    }
}
