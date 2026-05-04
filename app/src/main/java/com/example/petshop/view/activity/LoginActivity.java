package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;
import com.example.petshop.utils.Constants;
import com.example.petshop.utils.SessionManager;
import com.example.petshop.viewmodel.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvError;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    authViewModel.loginWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    showError("Google sign-in thất bại: " + e.getMessage());
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        initGoogleSignIn();
        observeViewModel();
    }

    private void initViews() {
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvError    = findViewById(R.id.tvError);

        btnLogin.setOnClickListener(v -> attemptEmailLogin());

        LinearLayout btnGoogle = findViewById(R.id.btnGoogleLogin);
        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            String email = getEmail();
            if (TextUtils.isEmpty(email)) {
                tvError.setText("Vui lòng nhập email để đặt lại mật khẩu");
                tvError.setVisibility(View.VISIBLE);
                etEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvError.setText("Email không hợp lệ");
                tvError.setVisibility(View.VISIBLE);
                etEmail.requestFocus();
                return;
            }
            tvError.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            authViewModel.sendPasswordReset(email);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initGoogleSignIn() {
        // Web Client ID đọc từ local.properties → BuildConfig → Constants
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
                
        if (Constants.GOOGLE_WEB_CLIENT_ID != null && !Constants.GOOGLE_WEB_CLIENT_ID.trim().isEmpty()) {
            builder.requestIdToken(Constants.GOOGLE_WEB_CLIENT_ID);
        }
        
        googleSignInClient = GoogleSignIn.getClient(this, builder.build());
    }

    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnLogin.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        });

        authViewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                showError(error);
                progressBar.setVisibility(View.GONE);
            }
        });

        authViewModel.getResetEmailSent().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                // Clear email field after success
                etEmail.setText("");
                etPassword.setText("");
            }
        });

        authViewModel.getUserRole().observe(this, role -> {
            if (role != null) navigateByRole(role);
        });
    }

    private void attemptEmailLogin() {
        String email    = getEmail();
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            tvError.setText("Vui lòng nhập email");
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tvError.setText("Vui lòng nhập mật khẩu");
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        tvError.setVisibility(View.GONE);
        authViewModel.loginWithEmail(email, password);
    }

    private void startGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void navigateByRole(String role) {
        Intent intent;
        if (SessionManager.ROLE_ADMIN.equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, PetShopActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private String getEmail() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
