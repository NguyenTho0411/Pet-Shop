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
import com.example.petshop.viewmodel.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvError;

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
        btnRegister      = findViewById(R.id.btnRegister);
        progressBar      = findViewById(R.id.progressBar);
        tvError          = findViewById(R.id.tvError);

        btnRegister.setOnClickListener(v -> attemptRegister());

        findViewById(R.id.tvGoLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegister.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        });

        authViewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
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

    private void attemptRegister() {
        String fullName       = getText(etFullName);
        String email          = getText(etEmail);
        String password       = getText(etPassword);
        String confirmPwd     = getText(etConfirmPassword);

        if (TextUtils.isEmpty(fullName)) {
            showError("Vui lòng nhập họ và tên");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            showError("Vui lòng nhập email");
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
        tvError.setVisibility(View.VISIBLE);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
