package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.repository.UserRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

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

        CircleImageView ivAvatar = findViewById(R.id.ivAvatar);
        if (user.getPhotoUrl() != null) {
            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);
        }

        // Load order stats from Firestore
        new UserRepository().getAllUsers(new UserRepository.Callback<>() {
            public void onSuccess(java.util.List<com.example.petshop.model.entity.User> list) {
                // find current user in list
                for (var u : list) {
                    if (user.getUid().equals(u.getId())) {
                        runOnUiThread(() -> {
                            ((TextView) findViewById(R.id.tvTotalOrders)).setText(String.valueOf(u.getTotalOrders()));
                            ((TextView) findViewById(R.id.tvTotalSpent)).setText(
                                    VND.format((long) u.getTotalSpent()) + "đ");
                            ((TextView) findViewById(R.id.tvRoleBadge)).setText(u.getRole());
                        });
                        break;
                    }
                }
            }
            public void onFailure(String err) {}
        });
    }

    private void setupMenuItems(String uid) {
        setupItem(R.id.itemEditProfile,    "✏️", "Chỉnh sửa thông tin", v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        setupItem(R.id.itemManageAddress,  "📍", "Địa chỉ giao hàng", v ->
                startActivity(new Intent(this, ManageAddressActivity.class)));

        setupItem(R.id.itemOrderHistory,   "📦", "Lịch sử đơn hàng", v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));

        setupItem(R.id.itemFavorites,      "❤️", "Yêu thích", v ->
                Toast.makeText(this, "Sắp ra mắt", Toast.LENGTH_SHORT).show());

        setupItem(R.id.itemLogout,         "🚪", "Đăng xuất", v -> confirmLogout());

        // Change avatar
        findViewById(R.id.btnChangeAvatar).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng đổi ảnh đại diện — sắp ra mắt", Toast.LENGTH_SHORT).show());
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
