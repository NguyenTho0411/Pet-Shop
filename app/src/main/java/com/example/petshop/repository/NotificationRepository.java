package com.example.petshop.repository;

import com.example.petshop.model.entity.Notification;
import com.example.petshop.model.entity.User;
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
        android.util.Log.d("NotificationRepo", "getNotifications: loading for userId=" + userId);
        db.collection(COL)
                .whereEqualTo("userId", userId)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("NotificationRepo", "getNotifications: found " + snap.size() + " notifications");
                    List<Notification> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            android.util.Log.d("NotificationRepo", "  - Notification: id=" + n.getId() + ", title=" + n.getTitle() + ", createdAt=" + n.getCreatedAt());
                            list.add(n);
                        }
                    }
                    // Sort by createdAt (newest first)
                    list.sort((n1, n2) -> {
                        try {
                            long ts1 = extractTimestamp(n1.getCreatedAt());
                            long ts2 = extractTimestamp(n2.getCreatedAt());
                            return Long.compare(ts2, ts1); // ts2 - ts1 for descending (newest first)
                        } catch (Exception e) {
                            android.util.Log.w("NotificationRepo", "Sort error: " + e.getMessage());
                            return 0;
                        }
                    });
                    android.util.Log.d("NotificationRepo", "getNotifications: sorted " + list.size() + " notifications");
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationRepo", "getNotifications FAILED: " + e.getMessage());
                    e.printStackTrace();
                    cb.onSuccess(new ArrayList<>());
                });
    }

    private long extractTimestamp(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return 0;
        try {
            // Parse Firestore Timestamp format: "Timestamp(seconds=1654321200, nanoseconds=123456789)"
            if (createdAt.contains("Timestamp")) {
                String secondsStr = createdAt.replaceAll("[^0-9,]", "").split(",")[0];
                return Long.parseLong(secondsStr);
            }
            // Try parsing as regular timestamp string
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(createdAt.replace("T", " ").split("\\.")[0]);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            android.util.Log.w("NotificationRepo", "extractTimestamp error: " + e.getMessage());
            return 0;
        }
    }

    public void markAsRead(String notificationId, Callback<Void> cb) {
        db.collection(COL).document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("NotificationRepo", "markAsRead: id=" + notificationId);
                    cb.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationRepo", "markAsRead FAILED: " + e.getMessage());
                    cb.onFailure(e.getMessage());
                });
    }

    public void markAllAsRead(String userId, Callback<Void> cb) {
        android.util.Log.d("NotificationRepo", "markAllAsRead: userId=" + userId);
        db.collection(COL)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("NotificationRepo", "markAllAsRead: found " + snap.size() + " unread notifications");
                    for (var doc : snap.getDocuments()) {
                        doc.getReference().update("isRead", true);
                    }
                    cb.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationRepo", "markAllAsRead FAILED: " + e.getMessage());
                    cb.onFailure(e.getMessage());
                });
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

    public void createNotificationAsync(Notification notification) {
        String id = System.currentTimeMillis() + "_" + notification.getUserId();
        notification.setId(id);
        notification.setCreatedAt(Timestamp.now().toString());
        notification.setRead(false);
        db.collection(COL).document(id).set(notification);
    }

    public void sendToAllCustomers(String title, String message, String type, String orderId, List<com.example.petshop.model.entity.User> customers) {
        if (customers == null || customers.isEmpty()) return;
        String createdAt = Timestamp.now().toString();
        List<com.google.firebase.firestore.WriteBatch> batches = new ArrayList<>();

        for (User user : customers) {
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            String notifId = System.currentTimeMillis() + "_" + user.getId();
            Notification notif = new Notification();
            notif.setId(notifId);
            notif.setUserId(user.getId());
            notif.setTitle(title);
            notif.setMessage(message);
            notif.setType(type);
            notif.setOrderId(orderId);
            notif.setCreatedAt(createdAt);
            notif.setRead(false);
            batch.set(db.collection(COL).document(notifId), notif);
            batch.commit();
        }
    }

    public void getUnreadCount(String userId, Callback<Long> cb) {
        db.collection(COL)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess((long) snap.size()))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    cb.onSuccess(0L);
                });
    }
}
