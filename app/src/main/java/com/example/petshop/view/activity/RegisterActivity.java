package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;
import com.example.petshop.utils.EmailHelper;
import com.example.petshop.viewmodel.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword, etOtp;
    private Button btnRegister, btnSendOtp;
    private ProgressBar progressBar;
    private TextView tvError;
    private String generatedOtp;
    private View tilOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        observeViewModel();
    }

    private void initViews() {
        etFullName       = findViewById(R.id.etFullName);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etOtp            = findViewById(R.id.etOtp);
        btnRegister      = findViewById(R.id.btnRegister);
        btnSendOtp       = findViewById(R.id.btnSendOtp);
        progressBar      = findViewById(R.id.progressBar);
        tvError          = findViewById(R.id.tvError);
        tilOtp           = findViewById(R.id.tilOtp);

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnRegister.setOnClickListener(v -> attemptRegister());

        findViewById(R.id.tvGoLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, this::setLoading);

        authViewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                showError(error);
            }
        });

        authViewModel.getUserRole().observe(this, role -> {
            if (role != null) {
                // Sau khi đăng ký xong luôn là customer → vào PetShopActivity
                Intent intent = new Intent(this, PetShopActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void sendOtp() {
        String email = getText(etEmail);
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Vui lòng nhập email hợp lệ");
            return;
        }

        setLoading(true);

        // Bước 1: Kiểm tra email đã tồn tại trong hệ thống chưa
        com.google.firebase.auth.FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        java.util.List<String> methods = task.getResult().getSignInMethods();
                        if (methods != null && !methods.isEmpty()) {
                            // Email đã được sử dụng
                            setLoading(false);
                            showError("Email này đã được đăng ký cho tài khoản khác");
                        } else {
                            // Email chưa tồn tại -> Gửi mã OTP
                            performSendEmail(email);
                        }
                    } else {
                        setLoading(false);
                        showError("Lỗi kiểm tra email: " + task.getException().getMessage());
                    }
                });
    }

    private void performSendEmail(String email) {
        // Tạo mã OTP 6 số ngẫu nhiên
        generatedOtp = String.valueOf((int) (Math.random() * 900000 + 100000));

        EmailHelper.sendOTP(email, generatedOtp, new EmailHelper.EmailCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    setLoading(false);
                    tilOtp.setVisibility(View.VISIBLE);
                    btnSendOtp.setText("Gửi lại mã");
                    tvError.setText("Mã xác thực đã được gửi đến email của bạn");
                    tvError.setTextColor(androidx.core.content.ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_green_dark));
                    tvError.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError("Email không tồn tại hoặc lỗi gửi: " + error);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        btnSendOtp.setEnabled(!loading);
    }

    private void attemptRegister() {
        String fullName       = getText(etFullName);
        String email          = getText(etEmail);
        String password       = getText(etPassword);
        String confirmPwd     = getText(etConfirmPassword);
        String userOtp        = getText(etOtp);

        if (TextUtils.isEmpty(fullName)) {
            showError("Vui lòng nhập họ và tên");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            showError("Vui lòng nhập email");
            return;
        }
        if (tilOtp.getVisibility() == View.GONE) {
            showError("Vui lòng nhấn 'Gửi mã' để xác thực email");
            return;
        }
        if (TextUtils.isEmpty(userOtp)) {
            showError("Vui lòng nhập mã OTP");
            return;
        }
        if (!userOtp.equals(generatedOtp)) {
            showError("Mã OTP không chính xác");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        if (!password.equals(confirmPwd)) {
            showError("Mật khẩu xác nhận không khớp");
            return;
        }

        tvError.setVisibility(View.GONE);
        authViewModel.registerWithEmail(email, password, fullName);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_error));
        tvError.setVisibility(View.VISIBLE);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
