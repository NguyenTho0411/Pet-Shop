package com.example.petshop.repository;

import com.example.petshop.model.entity.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
        db.collection(COL).document(uid)
                .delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getTotalUsers(Callback<Long> cb) {
        db.collection(COL).count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener(r -> cb.onSuccess(r.getCount()))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
