package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.google.firebase.auth.FirebaseUser;

public class AdminActivity extends AppCompatActivity {

    private TextView tvAdminName, tvAdminEmail;
    private TextView tvRevenue, tvTotalOrders, tvTotalUsers, tvPendingOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        initViews();
        loadAdminInfo();
        loadStats();
        setupQuickActions();
    }

    private void initViews() {
        tvAdminName    = findViewById(R.id.tvAdminName);
        tvAdminEmail   = findViewById(R.id.tvAdminEmail);
        tvRevenue      = findViewById(R.id.tvRevenue);
        tvTotalOrders  = findViewById(R.id.tvTotalOrders);
        tvTotalUsers   = findViewById(R.id.tvTotalUsers);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);

        Button btnLogout = findViewById(R.id.btnAdminLogout);
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadAdminInfo() {
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            tvAdminName.setText(name != null && !name.isEmpty() ? "👋 " + name : "Admin Dashboard");
            tvAdminEmail.setText(user.getEmail());
        }
    }

    private void loadStats() {
        // TODO: Load từ Firestore
        // Placeholder data
        tvRevenue.setText("12,500,000 đ");
        tvTotalOrders.setText("48");
        tvTotalUsers.setText("156");
        tvPendingOrders.setText("7");
    }

    private void setupQuickActions() {
        setupActionItem(R.id.itemManageUsers, "👥", "Quản lý người dùng", v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        setupActionItem(R.id.itemManageCategories, "📂", "Quản lý danh mục", v ->
                startActivity(new Intent(this, ManageCategoriesActivity.class)));

        setupActionItem(R.id.itemManagePets, "🐾", "Quản lý thú cưng", v ->
                startActivity(new Intent(this, ManagePetsActivity.class)));

        setupActionItem(R.id.itemManageFoods, "🍖", "Quản lý thức ăn", v ->
                startActivity(new Intent(this, ManageFoodActivity.class)));

        setupActionItem(R.id.itemManageOrders, "🛒", "Quản lý đơn hàng", v ->
                startActivity(new Intent(this, AdminOrderListActivity.class)));

        setupActionItem(R.id.itemManagePromotions, "🎁", "Quản lý khuyến mãi", v ->
                startActivity(new Intent(this, ManagePromotionsActivity.class)));

        setupActionItem(R.id.itemManageVouchers, "🎫", "Quản lý Voucher", v ->
                startActivity(new Intent(this, ManageVouchersActivity.class)));

        setupActionItem(R.id.itemManageReturns, "↩️", "Quản lý hoàn trả", v ->
                startActivity(new Intent(this, AdminReturnListActivity.class)));

        setupActionItem(R.id.itemStatistics, "📊", "Thống kê", v ->
                startActivity(new Intent(this, StatisticsActivity.class)));
    }

    private void setupActionItem(int viewId, String icon, String title, View.OnClickListener listener) {
        View item = findViewById(viewId);
        if (item == null) return;
        ((TextView) item.findViewById(R.id.tvActionIcon)).setText(icon);
        ((TextView) item.findViewById(R.id.tvActionTitle)).setText(title);
        item.setOnClickListener(listener);
    }

    private void confirmLogout() {
        DialogUtils.showConfirmDialog(this, "Đăng xuất", "Bạn có chắc muốn đăng xuất khỏi tài khoản Admin?",
            "Đăng xuất", "Hủy",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    FirebaseHelper.logout();
                    Intent intent = new Intent(AdminActivity.this, PetShopActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            }, "logoutDialog");
    }
}
