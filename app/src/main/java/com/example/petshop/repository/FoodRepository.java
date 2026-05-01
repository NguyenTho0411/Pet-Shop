package com.example.petshop.repository;

import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.FoodMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FoodRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL       = "foods";
    private static final String COL_MEDIA = "food_media";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAll(Callback<List<Food>> cb) {
        db.collection(COL)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Food> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Food food = doc.toObject(Food.class);
                        if (food != null) {
                            food.setId(doc.getId());
                            list.add(food);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByCategory(String categoryId, Callback<List<Food>> cb) {
        db.collection(COL).whereEqualTo("categoryId", categoryId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Food> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Food food = doc.toObject(Food.class);
                        if (food != null) {
                            food.setId(doc.getId());
                            list.add(food);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getById(String id, Callback<Food> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    Food food = doc.toObject(Food.class);
                    if (food != null) food.setId(doc.getId());
                    cb.onSuccess(food);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public void add(Food food, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        food.setId(id);
        food.setCreatedAt(now());
        food.setUpdatedAt(now());
        if (food.getStatus() == null) food.setStatus(Food.STATUS_AVAILABLE);
        db.collection(COL).document(id).set(food)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void update(Food food, Callback<Void> cb) {
        food.setUpdatedAt(now());
        db.collection(COL).document(food.getId()).set(food)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void delete(String id, Callback<Void> cb) {
        db.collection(COL).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateStock(String id, int stock, Callback<Void> cb) {
        db.collection(COL).document(id).update(
                "stock", stock,
                "updatedAt", now())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateThumbnail(String foodId, String url, Callback<Void> cb) {
        db.collection(COL).document(foodId)
                .update("thumbnailUrl", url, "updatedAt", now())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void addMedia(String foodId, FoodMedia media, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        media.setId(id);
        media.setFoodId(foodId);
        db.collection(COL).document(foodId)
                .collection(COL_MEDIA).document(id).set(media)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getMedia(String foodId, Callback<List<FoodMedia>> cb) {
        db.collection(COL).document(foodId)
                .collection(COL_MEDIA).orderBy("sortOrder").get()
                .addOnSuccessListener(snap -> {
                    List<FoodMedia> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        FoodMedia media = doc.toObject(FoodMedia.class);
                        if (media != null) {
                            media.setId(doc.getId());
                            list.add(media);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void deleteMedia(String foodId, String mediaId, Callback<Void> cb) {
        db.collection(COL).document(foodId)
                .collection(COL_MEDIA).document(mediaId).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
