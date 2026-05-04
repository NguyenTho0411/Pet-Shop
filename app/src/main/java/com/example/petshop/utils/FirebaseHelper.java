package com.example.petshop.utils;

import com.example.petshop.model.entity.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private static final String COLLECTION_USERS = "users";

    private static final FirebaseAuth  auth      = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db     = FirebaseFirestore.getInstance();

    // Callback interfaces
    public interface OnRoleCallback { void onResult(String role); }
    public interface OnSuccessCallback { void onSuccess(); }
    public interface OnAuthCallback {
        void onSuccess(String uid, String role);
        void onFailure(String errorMessage);
    }

    public static FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public static void logout() {
        auth.signOut();
    }

    // Email + Password login
    public static void loginWithEmail(String email, String password, OnAuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    getUserRole(uid, role -> callback.onSuccess(uid, role));
                })
                .addOnFailureListener(e -> callback.onFailure(parseAuthError(e.getMessage())));
    }

    // Email + Password register
    public static void registerWithEmail(String email, String password, String fullName,
                                          OnAuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onFailure("Lỗi tạo tài khoản");
                        return;
                    }
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();
                    user.updateProfile(profileUpdate);

                    saveUserToFirestore(user.getUid(), fullName, email,
                            User.ROLE_CUSTOMER, User.LOGIN_EMAIL, null,
                            () -> callback.onSuccess(user.getUid(), User.ROLE_CUSTOMER));
                })
                .addOnFailureListener(e -> callback.onFailure(parseAuthError(e.getMessage())));
    }

    // Google Sign-In
    public static void loginWithGoogle(String idToken, OnAuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) { callback.onFailure("Lỗi đăng nhập Google"); return; }
                    boolean isNewUser = result.getAdditionalUserInfo() != null
                            && result.getAdditionalUserInfo().isNewUser();
                    if (isNewUser) {
                        saveUserToFirestore(
                                user.getUid(),
                                user.getDisplayName() != null ? user.getDisplayName() : "",
                                user.getEmail(),
                                User.ROLE_CUSTOMER,
                                User.LOGIN_GOOGLE,
                                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                                () -> callback.onSuccess(user.getUid(), User.ROLE_CUSTOMER)
                        );
                    } else {
                        getUserRole(user.getUid(), role -> callback.onSuccess(user.getUid(), role));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Google: " + parseAuthError(e.getMessage())));
    }


    // Password reset email
    public static void sendPasswordReset(String email, OnAuthCallback callback) {
        if (email == null || email.isEmpty()) {
            callback.onFailure("Vui lòng nhập email trước");
            return;
        }
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> callback.onSuccess(null, null))
                .addOnFailureListener(e -> callback.onFailure(parseAuthError(e.getMessage())));
    }


    public static void getUserRole(String uid, OnRoleCallback callback) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    callback.onResult(role != null ? role : User.ROLE_CUSTOMER);
                })
                .addOnFailureListener(e -> callback.onResult(User.ROLE_CUSTOMER));
    }

    public interface OnUserDataCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public static void getUserData(String uid, OnUserDataCallback callback) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setId(uid);
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("Không đọc được dữ liệu user");
                        }
                    } else {
                        callback.onFailure("Không tìm thấy user");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private static void saveUserToFirestore(String uid, String fullName, String email,
                                             String role, String loginType, String avatarUrl,
                                             OnSuccessCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id",        uid);
        userData.put("fullName",  fullName);
        userData.put("email",     email);
        userData.put("role",      role);
        userData.put("loginType", loginType);
        userData.put("avatarUrl", avatarUrl);
        userData.put("status",    User.STATUS_ACTIVE);
        userData.put("createdAt", com.google.firebase.Timestamp.now().toString());

        db.collection(COLLECTION_USERS).document(uid)
                .set(userData)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onSuccess()); // vẫn callback dù lỗi Firestore
    }

    public static void updateUserProfile(String uid, String fullName, String phone,
                                          String avatarUrl, OnSuccessCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        if (fullName  != null) updates.put("fullName",  fullName);
        if (phone     != null) updates.put("phone",     phone);
        if (avatarUrl != null) updates.put("avatarUrl", avatarUrl);
        updates.put("updatedAt", com.google.firebase.Timestamp.now().toString());

        db.collection(COLLECTION_USERS).document(uid)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> { /* handle silently */ });
    }


    private static String parseAuthError(String error) {
        if (error == null) return "Có lỗi xảy ra";
        if (error.contains("INVALID_EMAIL"))      return "Email không hợp lệ";
        if (error.contains("WRONG_PASSWORD") || error.contains("INVALID_LOGIN_CREDENTIALS"))
            return "Email hoặc mật khẩu không đúng";
        if (error.contains("EMAIL_NOT_FOUND"))    return "Tài khoản không tồn tại";
        if (error.contains("EMAIL_EXISTS") || error.contains("email address is already"))
            return "Email đã được sử dụng";
        if (error.contains("WEAK_PASSWORD"))      return "Mật khẩu quá yếu (tối thiểu 6 ký tự)";
        if (error.contains("TOO_MANY_REQUESTS") || error.contains("too many attempts"))
            return "Quá nhiều lần thử. Vui lòng thử lại sau";
        if (error.contains("NETWORK_ERROR"))      return "Lỗi mạng. Vui lòng kiểm tra kết nối";
        if (error.contains("USER_NOT_FOUND") || error.contains("user not found"))
            return "Tài khoản không tồn tại";
        return "Có lỗi xảy ra. Vui lòng thử lại";
    }
}
