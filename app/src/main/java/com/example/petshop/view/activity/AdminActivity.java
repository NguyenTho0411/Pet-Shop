package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.example.petshop.viewmodel.AdminViewModel;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    // Views cho Dashboard Stats
    private TextView tvRevenue, tvTotalOrders, tvTotalUsers, tvPendingOrders;
    private TextView tvCompletedOrders, tvCancelledOrders, tvRefundedAmount;
    private TextView tvPreparingOrders, tvShippingOrders, tvDeliveredOrders;
    private View loadingView;

    // ViewModel
    private AdminViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Kiểm tra đăng nhập
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        initViewModel();
        loadAdminInfo(user);
        setupQuickActions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bắt đầu lắng nghe real-time updates
        if (viewModel != null) {
            viewModel.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dừng lắng nghe để tránh memory leak
        if (viewModel != null) {
            viewModel.stopListening();
        }
    }

    private void initViews() {
        tvAdminName    = findViewById(R.id.tvAdminName);
        tvAdminEmail   = findViewById(R.id.tvAdminEmail);
        tvRevenue      = findViewById(R.id.tvRevenue);
        tvTotalOrders  = findViewById(R.id.tvTotalOrders);
        tvTotalUsers   = findViewById(R.id.tvTotalUsers);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        
        // Các stat mới
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvCancelledOrders = findViewById(R.id.tvCancelledOrders);
        tvRefundedAmount = findViewById(R.id.tvRefundedAmount);
        tvPreparingOrders = findViewById(R.id.tvPreparingOrders);
        tvShippingOrders = findViewById(R.id.tvShippingOrders);
        tvDeliveredOrders = findViewById(R.id.tvDeliveredOrders);
        loadingView = findViewById(R.id.progressBar);

        Button btnLogout = findViewById(R.id.btnAdminLogout);
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // Observe LiveData và cập nhật UI
        observeStats();
    }

    private void observeStats() {
        // Revenue
        viewModel.getTotalRevenue().observe(this, revenue -> {
            if (revenue != null) {
                tvRevenue.setText(VND.format(revenue) + " đ");
            }
        });

        // Tổng đơn hàng
        viewModel.getTotalOrders().observe(this, total -> {
            if (total != null) {
                tvTotalOrders.setText(String.valueOf(total));
            }
        });

        // Tổng users
        viewModel.getTotalUsers().observe(this, users -> {
            if (users != null) {
                tvTotalUsers.setText(String.valueOf(users));
            }
        });

        // Đơn chờ xử lý - CÓ THỂ CLICK ĐƯỢC!
        viewModel.getPendingOrders().observe(this, pending -> {
            if (pending != null) {
                tvPendingOrders.setText(String.valueOf(pending));
            }
        });

        // Card pending orders clickable
        View cardPending = findViewById(R.id.cardPendingOrders);
        if (cardPending != null) {
            cardPending.setOnClickListener(v -> {
                Intent intent = new Intent(AdminActivity.this, AdminOrderListActivity.class);
                intent.putExtra("filter", "PENDING");
                startActivity(intent);
            });
        }

        // Đơn đang chuẩn bị
        viewModel.getPreparingOrders().observe(this, preparing -> {
            if (preparing != null && tvPreparingOrders != null) {
                tvPreparingOrders.setText(String.valueOf(preparing));
            }
        });

        // Đơn đang giao
        viewModel.getShippingOrders().observe(this, shipping -> {
            if (shipping != null && tvShippingOrders != null) {
                tvShippingOrders.setText(String.valueOf(shipping));
            }
        });

        // Đơn đã giao
        viewModel.getDeliveredOrders().observe(this, delivered -> {
            if (delivered != null && tvDeliveredOrders != null) {
                tvDeliveredOrders.setText(String.valueOf(delivered));
            }
        });

        // Đơn hoàn thành
        viewModel.getCompletedOrders().observe(this, completed -> {
            if (completed != null && tvCompletedOrders != null) {
                tvCompletedOrders.setText(String.valueOf(completed));
            }
        });

        // Đơn đã hủy
        viewModel.getCancelledOrders().observe(this, cancelled -> {
            if (cancelled != null && tvCancelledOrders != null) {
                tvCancelledOrders.setText(String.valueOf(cancelled));
            }
        });

        // Số tiền hoàn
        viewModel.getRefundedAmount().observe(this, refunded -> {
            if (refunded != null && tvRefundedAmount != null) {
                tvRefundedAmount.setText(VND.format(refunded) + " đ");
            }
        });

        // Loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (loadingView != null) {
                loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Error state
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAdminInfo(FirebaseUser user) {
        tvAdminName.setText("👋 " + (user.getDisplayName() != null ? user.getDisplayName() : "Admin"));
        tvAdminEmail.setText(user.getEmail());
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
    }

    private void setupActionItem(int viewId, String icon, String title, View.OnClickListener listener) {
        View item = findViewById(viewId);
        if (item == null) return;
        
        // Lấy nested views từ included layout
        TextView tvIcon = item.findViewById(R.id.tvActionIcon);
        TextView tvTitle = item.findViewById(R.id.tvActionTitle);
        
        if (tvIcon != null) tvIcon.setText(icon);
        if (tvTitle != null) tvTitle.setText(title);
        
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

    // Views
    private TextView tvAdminName, tvAdminEmail;
}
