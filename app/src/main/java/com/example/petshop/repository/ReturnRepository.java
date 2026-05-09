package com.example.petshop.repository;

import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.ReturnRequest;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ReturnRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL_RETURNS = "returns";
    private static final String COL_ORDERS  = "orders";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    /** Khách hàng tạo yêu cầu hoàn trả. Cập nhật order status → RETURN_REQUESTED. */
    public void create(ReturnRequest request, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        request.setId(id);
        request.setStatus(ReturnRequest.STATUS_PENDING);
        request.setCreatedAt(now());

        db.runTransaction(tx -> {
            // Tạo return request document
            tx.set(db.collection(COL_RETURNS).document(id), request);

            // Cập nhật order status
            Map<String, Object> upd = new HashMap<>();
            upd.put("status", Order.STATUS_RETURN_REQUESTED);
            upd.put("updatedAt", now());
            tx.update(db.collection(COL_ORDERS).document(request.getOrderId()), upd);
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(id))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Admin lấy tất cả yêu cầu hoàn trả, mới nhất trước. */
    public void getAll(Callback<List<ReturnRequest>> cb) {
        db.collection(COL_RETURNS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ReturnRequest> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        ReturnRequest r = doc.toObject(ReturnRequest.class);
                        if (r != null) { r.setId(doc.getId()); list.add(r); }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Khách hàng xem yêu cầu của mình. */
    public void getByUser(String userId, Callback<List<ReturnRequest>> cb) {
        db.collection(COL_RETURNS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ReturnRequest> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        ReturnRequest r = doc.toObject(ReturnRequest.class);
                        if (r != null) { r.setId(doc.getId()); list.add(r); }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Lấy return request theo orderId. */
    public void getByOrder(String orderId, Callback<ReturnRequest> cb) {
        db.collection(COL_RETURNS)
                .whereEqualTo("orderId", orderId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) { cb.onSuccess(null); return; }
                    ReturnRequest r = snap.getDocuments().get(0).toObject(ReturnRequest.class);
                    if (r != null) r.setId(snap.getDocuments().get(0).getId());
                    cb.onSuccess(r);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Admin duyệt yêu cầu: PENDING → APPROVED, order → RETURN_APPROVED. */
    public void approve(String returnId, String orderId, Callback<Void> cb) {
        String ts = now();
        db.runTransaction(tx -> {
            Map<String, Object> retUpd = new HashMap<>();
            retUpd.put("status", ReturnRequest.STATUS_APPROVED);
            retUpd.put("approvedAt", ts);
            tx.update(db.collection(COL_RETURNS).document(returnId), retUpd);

            Map<String, Object> ordUpd = new HashMap<>();
            ordUpd.put("status", Order.STATUS_RETURN_APPROVED);
            ordUpd.put("updatedAt", ts);
            tx.update(db.collection(COL_ORDERS).document(orderId), ordUpd);
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(null))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Admin xác nhận đã hoàn tiền: APPROVED → REFUNDED, order → REFUNDED. */
    public void markRefunded(String returnId, String orderId, Callback<Void> cb) {
        String ts = now();
        db.runTransaction(tx -> {
            Map<String, Object> retUpd = new HashMap<>();
            retUpd.put("status", ReturnRequest.STATUS_REFUNDED);
            retUpd.put("refundedAt", ts);
            tx.update(db.collection(COL_RETURNS).document(returnId), retUpd);

            Map<String, Object> ordUpd = new HashMap<>();
            ordUpd.put("status", Order.STATUS_REFUNDED);
            ordUpd.put("paymentStatus", Order.PAY_STATUS_REFUNDED);
            ordUpd.put("updatedAt", ts);
            tx.update(db.collection(COL_ORDERS).document(orderId), ordUpd);
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(null))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Xóa yêu cầu hoàn trả đã hoàn tiền hoặc từ chối. */
    public void delete(String returnId, Callback<Void> cb) {
        // First get the return request to know its status and orderId
        db.collection(COL_RETURNS).document(returnId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        cb.onFailure("Yêu cầu hoàn trả không tồn tại");
                        return;
                    }
                    String status = doc.getString("status");
                    String orderId = doc.getString("orderId");
                    if (orderId == null) {
                        cb.onFailure("Không tìm thấy orderId");
                        return;
                    }

                    // If refunded, rollback order status
                    if (ReturnRequest.STATUS_REFUNDED.equals(status)) {
                        db.runTransaction(tx -> {
                            // Delete return request
                            tx.delete(db.collection(COL_RETURNS).document(returnId));

                            // Rollback order to DELIVERED and payment to PAID
                            Map<String, Object> ordUpd = new HashMap<>();
                            ordUpd.put("status", Order.STATUS_DELIVERED);
                            ordUpd.put("paymentStatus", Order.PAY_STATUS_PAID);
                            ordUpd.put("updatedAt", now());
                            tx.update(db.collection(COL_ORDERS).document(orderId), ordUpd);
                            return null;
                        })
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                    } else {
                        // For rejected or other, just delete
                        db.collection(COL_RETURNS).document(returnId).delete()
                                .addOnSuccessListener(v -> cb.onSuccess(null))
                                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /** Admin từ chối yêu cầu: PENDING → REJECTED, order → DELIVERED (rollback). */
    public void reject(String returnId, String orderId, String note, Callback<Void> cb) {
        String ts = now();
        db.runTransaction(tx -> {
            Map<String, Object> retUpd = new HashMap<>();
            retUpd.put("status", ReturnRequest.STATUS_REJECTED);
            retUpd.put("adminNote", note != null ? note : "");
            retUpd.put("approvedAt", ts);
            tx.update(db.collection(COL_RETURNS).document(returnId), retUpd);

            Map<String, Object> ordUpd = new HashMap<>();
            ordUpd.put("status", Order.STATUS_DELIVERED);
            ordUpd.put("updatedAt", ts);
            tx.update(db.collection(COL_ORDERS).document(orderId), ordUpd);
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(null))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
