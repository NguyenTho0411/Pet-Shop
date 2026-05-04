package com.example.petshop.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.petshop.model.entity.User;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.utils.SessionManager;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading    = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String>  userRole     = new MutableLiveData<>(null);
    private final MutableLiveData<String>  resetEmailSent = new MutableLiveData<>(null);

    private SessionManager sessionManager;

    public AuthViewModel(Application application) {
        super(application);
        sessionManager = SessionManager.getInstance(application);
    }

    public LiveData<Boolean> getIsLoading()     { return isLoading; }
    public LiveData<String>  getErrorMessage()  { return errorMessage; }
    public LiveData<String>  getUserRole()      { return userRole; }
    public LiveData<String>  getResetEmailSent(){ return resetEmailSent; }

    // ==================== EMAIL / PASSWORD ====================

    public void loginWithEmail(String email, String password) {
        setLoading(true);
        FirebaseHelper.loginWithEmail(email, password, new FirebaseHelper.OnAuthCallback() {
            @Override
            public void onSuccess(String uid, String role) {
                loadUserDataAndSaveSession(uid, role);
            }
            @Override
            public void onFailure(String errorMsg) {
                setLoading(false);
                errorMessage.postValue(errorMsg);
            }
        });
    }

    public void registerWithEmail(String email, String password, String fullName) {
        setLoading(true);
        FirebaseHelper.registerWithEmail(email, password, fullName, new FirebaseHelper.OnAuthCallback() {
            @Override
            public void onSuccess(String uid, String role) {
                loadUserDataAndSaveSession(uid, role);
            }
            @Override
            public void onFailure(String errorMsg) {
                setLoading(false);
                errorMessage.postValue(errorMsg);
            }
        });
    }

    // ==================== GOOGLE ====================

    public void loginWithGoogle(String idToken) {
        setLoading(true);
        FirebaseHelper.loginWithGoogle(idToken, new FirebaseHelper.OnAuthCallback() {
            @Override
            public void onSuccess(String uid, String role) {
                loadUserDataAndSaveSession(uid, role);
            }
            @Override
            public void onFailure(String errorMsg) {
                setLoading(false);
                errorMessage.postValue(errorMsg);
            }
        });
    }

    // ==================== FACEBOOK ====================

    public void loginWithFacebook(String accessToken) {
        setLoading(true);
        FirebaseHelper.loginWithFacebook(accessToken, new FirebaseHelper.OnAuthCallback() {
            @Override
            public void onSuccess(String uid, String role) {
                loadUserDataAndSaveSession(uid, role);
            }
            @Override
            public void onFailure(String errorMsg) {
                setLoading(false);
                errorMessage.postValue(errorMsg);
            }
        });
    }

    // ==================== SESSION MANAGEMENT ====================

    private void loadUserDataAndSaveSession(String uid, String role) {
        FirebaseHelper.getUserData(uid, new FirebaseHelper.OnUserDataCallback() {
            @Override
            public void onSuccess(User user) {
                sessionManager.saveSession(
                        uid,
                        user.getFullName() != null ? user.getFullName() : "",
                        user.getEmail() != null ? user.getEmail() : "",
                        role,
                        user.getAvatarUrl()
                );
                setLoading(false);
                userRole.postValue(role);
            }
            @Override
            public void onFailure(String error) {
                // Vẫn lưu session với dữ liệu cơ bản
                sessionManager.saveSession(uid, "", "", role, null);
                setLoading(false);
                userRole.postValue(role);
            }
        });
    }

    public void logout() {
        sessionManager.clearSession();
        FirebaseHelper.logout();
    }

    // ==================== RESET PASSWORD ====================

    public void sendPasswordReset(String email) {
        FirebaseHelper.sendPasswordReset(email, new FirebaseHelper.OnAuthCallback() {
            @Override
            public void onSuccess(String uid, String role) {
                resetEmailSent.postValue("Email đặt lại mật khẩu đã được gửi!");
            }
            @Override
            public void onFailure(String errorMsg) {
                errorMessage.postValue(errorMsg);
            }
        });
    }

    // ==================== HELPERS ====================

    private void setLoading(boolean value) {
        isLoading.postValue(value);
    }

    public void clearError() {
        errorMessage.setValue("");
    }
}
