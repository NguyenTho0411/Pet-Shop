package com.example.petshop.repository;

import com.example.petshop.model.entity.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL = "users";
    private static final String COL_ORDERS = "orders";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAllUsers(Callback<List<User>> cb) {
        db.collection(COL)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<User> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(User.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getUsersByRole(String role, Callback<List<User>> cb) {
        db.collection(COL).whereEqualTo("role", role).get()
                .addOnSuccessListener(snap -> {
                    List<User> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(User.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getAllCustomers(Callback<List<User>> cb) {
        db.collection(COL)
                .whereEqualTo("role", User.ROLE_CUSTOMER)
                .whereEqualTo("status", User.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snap -> {
                    List<User> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(User.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateUserStatus(String uid, String status, Callback<Void> cb) {
        db.collection(COL).document(uid)
                .update("status", status)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateUserRole(String uid, String role, Callback<Void> cb) {
        db.collection(COL).document(uid)
                .update("role", role)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateUser(String uid, Map<String, Object> fields, Callback<Void> cb) {
        db.collection(COL).document(uid)
                .update(fields)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void deleteUser(String uid, Callback<Void> cb) {
        if (uid == null || uid.trim().isEmpty()) {
            cb.onFailure("UID không hợp lệ");
            return;
        }

        // Xóa cascade các đơn hàng thuộc user trước khi xóa user (đảm bảo thống kê không bị lệch).
        // Lưu ý: Firestore batch giới hạn 500 operations; nếu user có quá nhiều đơn,
        // cần chia batch. Ở app demo, số lượng thường không lớn.
        db.collection(COL_ORDERS)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    for (var doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(db.collection(COL).document(uid));

                    batch.commit()
                            .addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getTotalUsers(Callback<Long> cb) {
        db.collection(COL).count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener(r -> cb.onSuccess(r.getCount()))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
