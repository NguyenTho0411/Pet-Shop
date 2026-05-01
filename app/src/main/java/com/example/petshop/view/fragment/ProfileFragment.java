package com.example.petshop.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import java.text.NumberFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }
        bindUserInfo(root, user);
        setupMenuItems(root);
    }

    private void bindUserInfo(View root, FirebaseUser user) {
        ((TextView) root.findViewById(R.id.tvUserName)).setText(
                user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
        ((TextView) root.findViewById(R.id.tvUserEmail)).setText(user.getEmail());

        CircleImageView ivAvatar = root.findViewById(R.id.ivAvatar);
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
                        ((TextView) root.findViewById(R.id.tvTotalOrders)).setText(String.valueOf(data.size()));
                        double total = 0;
                        for (var o : data) total += o.getTotalAmount();
                        ((TextView) root.findViewById(R.id.tvTotalSpent)).setText(VND.format((long) total) + "đ");
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
        setItem(root, R.id.itemFavorites,     "❤️", "Yêu thích",
                v -> Toast.makeText(requireContext(), "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        setItem(root, R.id.itemLogout,        "🚪", "Đăng xuất",
                v -> confirmLogout());

        View btnAvatar = root.findViewById(R.id.btnChangeAvatar);
        if (btnAvatar != null) btnAvatar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Đổi ảnh đại diện — sắp ra mắt", Toast.LENGTH_SHORT).show());
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
