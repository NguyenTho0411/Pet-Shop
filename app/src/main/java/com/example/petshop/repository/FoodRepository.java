package com.example.petshop.repository;

import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.FoodMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
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
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(Food.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByCategory(String categoryId, Callback<List<Food>> cb) {
        db.collection(COL).whereEqualTo("categoryId", categoryId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Food> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(Food.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getById(String id, Callback<Food> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> cb.onSuccess(doc.toObject(Food.class)))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void add(Food food, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        food.setId(id);
        food.setCreatedAt(Timestamp.now().toString());
        food.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL).document(id).set(food)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void update(Food food, Callback<Void> cb) {
        food.setUpdatedAt(Timestamp.now().toString());
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
                "updatedAt", Timestamp.now().toString())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }


    public void addMedia(String foodId, FoodMedia media, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
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
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(FoodMedia.class));
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
