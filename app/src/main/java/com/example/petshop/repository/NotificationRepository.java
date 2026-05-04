package com.example.petshop.repository;

import com.example.petshop.model.entity.Notification;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL = "notifications";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getNotifications(String userId, Callback<List<Notification>> cb) {
        db.collection(COL)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Notification> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        n.setId(doc.getId());
                        list.add(n);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void markAsRead(String notificationId, Callback<Void> cb) {
        db.collection(COL).document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void markAllAsRead(String userId, Callback<Void> cb) {
        db.collection(COL)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snap -> {
                    for (var doc : snap.getDocuments()) {
                        doc.getReference().update("isRead", true);
                    }
                    cb.onSuccess(null);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void deleteNotification(String notificationId, Callback<Void> cb) {
        db.collection(COL).document(notificationId).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void createNotification(Notification notification, Callback<String> cb) {
        String id = System.currentTimeMillis() + "_" + notification.getUserId();
        notification.setId(id);
        notification.setCreatedAt(Timestamp.now().toString());
        notification.setRead(false);
        db.collection(COL).document(id).set(notification)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getUnreadCount(String userId, Callback<Long> cb) {
        db.collection(COL)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess((long) snap.size()))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
