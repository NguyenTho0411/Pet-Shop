package com.example.petshop.utils;

import com.example.petshop.model.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Chạy 1 lần để tạo tài khoản admin.
 * Gọi AdminSetupHelper.createAdminAccount(email, password, name) từ bất kỳ Activity nào.
 * SAU KHI TẠO XONG NÊN XOÁ FILE NÀY.
 */
public class AdminSetupHelper {

    public interface SetupCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public static void createAdminAccount(String email, String password,
                                           String fullName, SetupCallback cb) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // Update display name
                    result.getUser().updateProfile(
                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName).build());

                    // Save to Firestore with ADMIN role
                    Map<String, Object> data = new HashMap<>();
                    data.put("id",        uid);
                    data.put("fullName",  fullName);
                    data.put("email",     email);
                    data.put("role",      User.ROLE_ADMIN);
                    data.put("loginType", User.LOGIN_EMAIL);
                    data.put("status",    User.STATUS_ACTIVE);
                    data.put("createdAt", com.google.firebase.Timestamp.now().toString());

                    db.collection("users").document(uid).set(data)
                            .addOnSuccessListener(v ->
                                    cb.onSuccess("✅ Admin tạo thành công!\nEmail: " + email))
                            .addOnFailureListener(e ->
                                    cb.onFailure("Firebase Auth OK nhưng lỗi Firestore: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    // Nếu email đã tồn tại → chỉ update role trong Firestore
                    if (e.getMessage() != null && e.getMessage().contains("email address is already")) {
                        upgradeExistingToAdmin(email, fullName, db, cb);
                    } else {
                        cb.onFailure("Lỗi tạo tài khoản: " + e.getMessage());
                    }
                });
    }

    /** Nếu tài khoản đã có → chỉ đổi role thành ADMIN */
    public static void upgradeExistingToAdmin(String email, String fullName,
                                               FirebaseFirestore db, SetupCallback cb) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        cb.onFailure("Tài khoản tồn tại trong Auth nhưng không có trong Firestore.\nTự sửa role trong Firebase Console.");
                        return;
                    }
                    String uid = snap.getDocuments().get(0).getId();
                    db.collection("users").document(uid)
                            .update("role", User.ROLE_ADMIN, "fullName", fullName)
                            .addOnSuccessListener(v ->
                                    cb.onSuccess("✅ Đã nâng cấp tài khoản lên ADMIN!\nEmail: " + email))
                            .addOnFailureListener(e ->
                                    cb.onFailure("Lỗi update role: " + e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
