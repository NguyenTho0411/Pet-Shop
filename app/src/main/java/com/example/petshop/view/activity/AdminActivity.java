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
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.Locale;

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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Tạm thời set về 0 hoặc loading
        tvRevenue.setText("Đang tải...");
        tvTotalOrders.setText("...");
        tvTotalUsers.setText("...");
        tvPendingOrders.setText("...");

        // Load users count
        db.collection("users").get().addOnSuccessListener(snap -> {
            tvTotalUsers.setText(String.valueOf(snap.size()));
        });

        // Load orders
        db.collection("orders").get().addOnSuccessListener(snap -> {
            int orders = snap.size();
            int pending = 0;
            double revenue = 0;
            for (var doc : snap.getDocuments()) {
                String status = doc.getString("status");
                if ("PENDING".equals(status)) {
                    pending++;
                }
                if ("COMPLETED".equals(status) || "DELIVERED".equals(status)) {
                    Double amount = doc.getDouble("totalAmount");
                    if (amount != null) revenue += amount;
                }
            }
            
            tvTotalOrders.setText(String.valueOf(orders));
            tvPendingOrders.setText(String.valueOf(pending));
            NumberFormat vndFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            tvRevenue.setText(vndFormat.format((long) revenue) + " đ");
        });
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

        setupActionItem(R.id.itemManagePromotions, "🎁", "Quản lý khuyến mãi & voucher", v ->
                startActivity(new Intent(this, ManagePromotionsActivity.class)));

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
