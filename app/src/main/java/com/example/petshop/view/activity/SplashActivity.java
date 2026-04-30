package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.utils.SessionManager;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_MS = 1500L;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startTime = System.currentTimeMillis();

        setupGetStartedButton();

        FirebaseUser currentUser = FirebaseHelper.getCurrentUser();
        if (currentUser != null) {
            // Đã login → kiểm tra role, nhưng vẫn chờ đủ MIN_SPLASH_MS
            FirebaseHelper.getUserRole(currentUser.getUid(), role -> {
                Runnable action = SessionManager.ROLE_ADMIN.equals(role)
                        ? this::goToAdmin : this::goToPetShop;
                navigateAfterMinDelay(action);
            });
        } else {
            // Chưa login → tự navigate sau 2s
            navigateAfterMinDelay(this::goToPetShop);
        }
    }

    private void navigateAfterMinDelay(Runnable action) {
        long elapsed   = System.currentTimeMillis() - startTime;
        long remaining = MIN_SPLASH_MS - elapsed;
        new Handler(Looper.getMainLooper())
                .postDelayed(action, Math.max(remaining, 0));
    }

    private void setupGetStartedButton() {
        int[] ids = { R.id.cardBtn, R.id.llBottom, R.id.llBottomRow };
        for (int id : ids) {
            android.view.View v = findViewById(id);
            if (v != null) { v.setOnClickListener(x -> goToPetShop()); return; }
        }
    }

    private void goToPetShop() {
        startActivity(new Intent(this, PetShopActivity.class));
        finish();
    }

    private void goToAdmin() {
        startActivity(new Intent(this, AdminActivity.class));
        finish();
    }
}
