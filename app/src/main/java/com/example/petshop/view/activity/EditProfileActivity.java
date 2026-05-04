package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class EditProfileActivity extends AppCompatActivity {

    public static final int RESULT_PROFILE_UPDATED = 101;

    private TextInputEditText etFullName, etPhone, etDob;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFullName  = findViewById(R.id.etFullName);
        etPhone     = findViewById(R.id.etPhone);
        etDob       = findViewById(R.id.etDob);
        progressBar = findViewById(R.id.progressBar);

        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user != null) {
            etFullName.setText(user.getDisplayName());
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ((Button) findViewById(R.id.btnSaveProfile)).setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) { etFullName.setError("Bắt buộc"); return; }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) { finish(); return; }

        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();
        user.updateProfile(req).addOnCompleteListener(task -> {
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : null;
            FirebaseHelper.updateUserProfile(user.getUid(), name, phone, null, () ->
                    runOnUiThread(() -> {
                        SessionManager.getInstance(EditProfileActivity.this).updateUserName(name);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cập nhật thành công ✓", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_PROFILE_UPDATED);
                        finish();
                    }));
        });
    }
}
