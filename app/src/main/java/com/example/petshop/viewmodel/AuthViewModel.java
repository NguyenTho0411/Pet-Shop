package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.utils.FirebaseHelper;

public class AuthViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading    = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String>  userRole     = new MutableLiveData<>(null);
    private final MutableLiveData<String>  resetEmailSent = new MutableLiveData<>(null);

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
                setLoading(false);
                userRole.postValue(role);
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
                setLoading(false);
                userRole.postValue(role);
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
                setLoading(false);
                userRole.postValue(role);
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
                setLoading(false);
                userRole.postValue(role);
            }
            @Override
            public void onFailure(String errorMsg) {
                setLoading(false);
                errorMessage.postValue(errorMsg);
            }
        });
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
