package com.example.petshop.view.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.repository.UserRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.utils.SessionManager;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));
    private static final int REQUEST_IMAGE_PERMISSION = 101;
    
    private CircleImageView ivAvatar;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindUserInfo(user);
        setupMenuItems(user.getUid());
    }

    private void bindUserInfo(FirebaseUser user) {
        ((TextView) findViewById(R.id.tvUserName)).setText(
                user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
        ((TextView) findViewById(R.id.tvUserEmail)).setText(user.getEmail());

        ivAvatar = findViewById(R.id.ivAvatar);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Lấy thông tin user từ Firestore (ưu tiên avatar từ đây)
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                if (role == null || role.isEmpty()) role = "Khách hàng";
                String finalRole = role;

                // Lấy avatar từ Firestore (ưu tiên) hoặc Firebase Auth
                String avatarUrl = doc.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this).load(avatarUrl).circleCrop().into(ivAvatar);
                } else if (user.getPhotoUrl() != null) {
                    Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);
                }

                // Lấy totalOrders và totalSpent
                Long totalOrders = doc.getLong("totalOrders");
                Double totalSpent = doc.getDouble("totalSpent");

                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.tvRoleBadge)).setText(finalRole);
                    ((TextView) findViewById(R.id.tvTotalOrders)).setText(
                            String.valueOf(totalOrders != null ? totalOrders : 0));
                    ((TextView) findViewById(R.id.tvTotalSpent)).setText(
                            VND.format(totalSpent != null ? Math.round(totalSpent) : 0) + "đ");
                });
            } else {
                // Fallback về Firebase Auth nếu Firestore không có
                if (user.getPhotoUrl() != null) {
                    Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);
                }
            }
        }).addOnFailureListener(e -> {
            // Fallback về Firebase Auth nếu Firestore lỗi
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);
            }
        });
    }

    private void setupMenuItems(String uid) {
        setupItem(R.id.itemEditProfile,    "✏️", "Chỉnh sửa thông tin", v -> {
                Intent i = new Intent(this, EditProfileActivity.class);
                startActivityForResult(i, 100);
        });

        setupItem(R.id.itemManageAddress,  "📍", "Địa chỉ giao hàng", v ->
                startActivity(new Intent(this, ManageAddressActivity.class)));

        setupItem(R.id.itemOrderHistory,   "📦", "Lịch sử đơn hàng", v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));

        setupItem(R.id.itemLogout,         "🚪", "Đăng xuất", v -> confirmLogout());

        // Change avatar
        findViewById(R.id.btnChangeAvatar).setOnClickListener(v -> showImagePickerDialog());
    }
    
    private void showImagePickerDialog() {
        DialogUtils.showConfirmDialog(this, "Đổi ảnh đại diện", "Chọn nguồn ảnh",
                "Chụp ảnh", "Chọn từ thư viện",
                new ConfirmDialog.OnConfirmListener() {
                    @Override public void onConfirm() { openCamera(); }
                    @Override public void onCancel() {}
                }, "cameraDialog");
        
        // Show second option
        new android.os.Handler().postDelayed(() -> {
            try {
                android.app.Dialog d = ((android.app.Dialog) java.lang.reflect.Method.class
                        .getDeclaredMethod("getDialog").invoke(new DialogUtils().getClass()));
            } catch (Exception e) {}
        }, 100);
        
        // Simple approach: show options directly
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(new String[]{"📷 Chụp ảnh mới", "🖼️ Chọn từ thư viện"}, (d, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
    
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && selectedImageUri != null) {
                    uploadAvatar(selectedImageUri);
                }
            });
    
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        uploadAvatar(uri);
                    }
                }
            });
    
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_PERMISSION);
            return;
        }
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        selectedImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                new android.content.ContentValues());
        i.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
        cameraLauncher.launch(i);
    }
    
    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(i);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void uploadAvatar(Uri uri) {
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) return;

        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        String uid = user.getUid();
        StorageReference ref = FirebaseStorage.getInstance().getReference("avatars/" + uid + ".jpg");

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            ref.putBytes(data)
                    .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String avatarUrl = downloadUri.toString();

                        // 1. Cập nhật Firebase Auth profile
                        user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setPhotoUri(downloadUri)
                                .build())
                                .addOnSuccessListener(aVoid -> {
                                    // 2. Cập nhật Firestore - QUAN TRỌNG để đồng bộ across devices
                                    FirebaseFirestore.getInstance().collection("users")
                                            .document(uid)
                                            .update("avatarUrl", avatarUrl,
                                                    "updatedAt", com.google.firebase.Timestamp.now().toString())
                                            .addOnSuccessListener(v -> {
                                                // 3. Cập nhật SessionManager (cache local)
                                                SessionManager.getInstance(this).updateUserAvatar(avatarUrl);
                                                runOnUiThread(() -> {
                                                    Glide.with(this).load(downloadUri).circleCrop().into(ivAvatar);
                                                    Toast.makeText(this, "Đổi ảnh thành công!", Toast.LENGTH_SHORT).show();
                                                });
                                            })
                                            .addOnFailureListener(e -> {
                                                // Vẫn cập nhật local nếu Firestore lỗi
                                                SessionManager.getInstance(this).updateUserAvatar(avatarUrl);
                                                runOnUiThread(() -> {
                                                    Glide.with(this).load(downloadUri).circleCrop().into(ivAvatar);
                                                    Toast.makeText(this, "Đổi ảnh thành công!", Toast.LENGTH_SHORT).show();
                                                });
                                            });
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi đọc ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == EditProfileActivity.RESULT_PROFILE_UPDATED) {
            FirebaseUser user = FirebaseHelper.getCurrentUser();
            if (user != null) {
                bindUserInfo(user);
            }
        }
    }

    private void setupItem(int viewId, String icon, String title, android.view.View.OnClickListener listener) {
        android.view.View item = findViewById(viewId);
        if (item == null) return;
        ((TextView) item.findViewById(R.id.tvActionIcon)).setText(icon);
        ((TextView) item.findViewById(R.id.tvActionTitle)).setText(title);
        item.setOnClickListener(listener);
    }

    private void confirmLogout() {
        DialogUtils.showConfirmDialog(this, "Đăng xuất", "Bạn có chắc muốn đăng xuất?",
            "Đăng xuất", "Huỷ",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    FirebaseHelper.logout();
                    Intent i = new Intent(ProfileActivity.this, PetShopActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            }, "logoutDialog");
    }
}
