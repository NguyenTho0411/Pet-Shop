package com.example.petshop.view.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.repository.UserRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.activity.EditProfileActivity;
import com.example.petshop.view.activity.LoginActivity;
import com.example.petshop.view.activity.ManageAddressActivity;
import com.example.petshop.view.activity.OrderHistoryActivity;
import com.example.petshop.view.activity.PetShopActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final int REQUEST_IMAGE_PERMISSION = 101;
    
    private CircleImageView ivAvatar;
    private Uri selectedImageUri;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        loadUserProfile(root);
        setupMenuItems(root);
    }

    @Override
    public void onResume() {
        super.onResume();
        View root = getView();
        if (root != null) {
            loadUserProfile(root);
        }
    }

    private void loadUserProfile(View root) {
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }
        bindUserInfo(root, user);
    }

    private void bindUserInfo(View root, FirebaseUser user) {
        ((TextView) root.findViewById(R.id.tvUserName)).setText(
                user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
        ((TextView) root.findViewById(R.id.tvUserEmail)).setText(user.getEmail());

        ivAvatar = root.findViewById(R.id.ivAvatar);
        if (user.getPhotoUrl() != null)
            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);

        new UserRepository().getAllUsers(new UserRepository.Callback<>() {
            public void onSuccess(java.util.List<com.example.petshop.model.entity.User> list) {
                for (var u : list) {
                    if (user.getUid().equals(u.getId())) {
                        requireActivity().runOnUiThread(() -> {
                            ((TextView) root.findViewById(R.id.tvTotalOrders))
                                    .setText(String.valueOf(u.getTotalOrders()));
                            ((TextView) root.findViewById(R.id.tvTotalSpent))
                                    .setText(VND.format((long) u.getTotalSpent()) + "đ");
                            ((TextView) root.findViewById(R.id.tvRoleBadge))
                                    .setText(u.getRole());
                        });
                        break;
                    }
                }
            }
            public void onFailure(String err) {}
        });

        // Bổ sung: Đếm lại đơn hàng thực tế nếu con số ở trên bị sai (0)
        new com.example.petshop.repository.OrderRepository().getOrdersByUser(user.getUid(), new com.example.petshop.repository.OrderRepository.Callback<>() {
            @Override
            public void onSuccess(java.util.List<com.example.petshop.model.entity.Order> data) {
                if (data != null && !data.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        int validOrderCount = 0;
                        double total = 0;

                        for (var o : data) {
                            String status = o.getStatus();

                            boolean shouldCount =
                                    !com.example.petshop.model.entity.Order.STATUS_CANCELLED.equals(status)
                                            && !com.example.petshop.model.entity.Order.STATUS_REFUNDED.equals(status)
                                            && !com.example.petshop.model.entity.Order.STATUS_WAIT_PAY.equals(status);

                            if (shouldCount) {
                                validOrderCount++;
                                total += o.getTotalAmount();
                            }
                        }

                        ((TextView) root.findViewById(R.id.tvTotalOrders))
                                .setText(String.valueOf(validOrderCount));

                        ((TextView) root.findViewById(R.id.tvTotalSpent))
                                .setText(VND.format((long) total) + "đ");
                    });
                }
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void setupMenuItems(View root) {
        setItem(root, R.id.itemEditProfile,   "✏️", "Chỉnh sửa thông tin",
                v -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        setItem(root, R.id.itemManageAddress, "📍", "Địa chỉ giao hàng",
                v -> startActivity(new Intent(requireContext(), ManageAddressActivity.class)));
        setItem(root, R.id.itemOrderHistory,  "📦", "Lịch sử đơn hàng",
                v -> startActivity(new Intent(requireContext(), OrderHistoryActivity.class)));
        setItem(root, R.id.itemLogout,        "🚪", "Đăng xuất",
                v -> confirmLogout());

        View btnAvatar = root.findViewById(R.id.btnChangeAvatar);
        if (btnAvatar != null) btnAvatar.setOnClickListener(v -> showImagePickerDialog());
    }
    
    private void showImagePickerDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
                if (result.getResultCode() == requireActivity().RESULT_OK && selectedImageUri != null) {
                    uploadAvatar(selectedImageUri);
                }
            });
    
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        uploadAvatar(uri);
                    }
                }
            });
    
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_PERMISSION);
            return;
        }
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        selectedImageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
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
                Toast.makeText(requireContext(), "Cần cấp quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void uploadAvatar(Uri uri) {
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) return;
        
        Toast.makeText(requireContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
        
        String uid = user.getUid();
        StorageReference ref = FirebaseStorage.getInstance().getReference("avatars/" + uid + ".jpg");
        
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();
            
            ref.putBytes(data)
                    .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setPhotoUri(downloadUri)
                                .build())
                                .addOnSuccessListener(aVoid -> {
                                    Glide.with(this).load(downloadUri).circleCrop().into(ivAvatar);
                                    Toast.makeText(requireContext(), "Đổi ảnh thành công!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Lỗi đọc ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void setItem(View root, int id, String icon, String title, View.OnClickListener l) {
        View item = root.findViewById(id);
        if (item == null) return;
        ((TextView) item.findViewById(R.id.tvActionIcon)).setText(icon);
        ((TextView) item.findViewById(R.id.tvActionTitle)).setText(title);
        item.setOnClickListener(l);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    FirebaseHelper.logout();
                    Intent i = new Intent(requireContext(), PetShopActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Huỷ", null).show();
    }
}
