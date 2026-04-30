package com.example.petshop.repository;

import com.example.petshop.model.entity.Promotion;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
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
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(Promotion.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getActive(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        // Check if not expired
                        if (p.getEndDate() != null && isNotExpired(p.getEndDate())) {
                            list.add(p);
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
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
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
        db.collection(COL).document(id).update("isActive", isActive)
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
        try {
            long endTime = Long.parseLong(endDate);
            long now = System.currentTimeMillis() / 1000;
            return now < endTime;
        } catch (Exception e) {
            return true;
        }
    }
}
