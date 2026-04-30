package com.example.petshop.repository;

import com.example.petshop.model.entity.Category;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoryRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL = "categories";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAll(Callback<List<Category>> cb) {
        db.collection(COL).orderBy("sortOrder").get()
                .addOnSuccessListener(snap -> {
                    List<Category> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(Category.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByType(String type, Callback<List<Category>> cb) {
        db.collection(COL).whereEqualTo("type", type).get()
                .addOnSuccessListener(snap -> {
                    List<Category> list = new ArrayList<>();
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(Category.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void add(Category category, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        category.setId(id);
        category.setCreatedAt(Timestamp.now().toString());
        db.collection(COL).document(id).set(category)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void update(Category category, Callback<Void> cb) {
        db.collection(COL).document(category.getId()).set(category)
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
}
