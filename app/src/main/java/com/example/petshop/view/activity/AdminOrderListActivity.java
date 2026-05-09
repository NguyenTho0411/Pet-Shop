package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Order;
import com.example.petshop.repository.OrderRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminOrderListActivity extends AppCompatActivity {

    private List<Order> allOrders = new ArrayList<>();
    private List<Order> filteredOrders = new ArrayList<>();
    private RecyclerView rv;
    private ProgressBar pb;
    private TextView tvEmpty;
    private ChipGroup chipGroup;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_order_list);

        initViews();
        setupFilterChips();
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void initViews() {
        rv = findViewById(R.id.rvOrders);
        pb = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        chipGroup = findViewById(R.id.chipGroup);

        rv.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupFilterChips() {
        findViewById(R.id.chipAll).setOnClickListener(v -> filterByStatus(null));
        findViewById(R.id.chipPending).setOnClickListener(v -> filterByStatus(Order.STATUS_PENDING));
        findViewById(R.id.chipConfirmed).setOnClickListener(v -> filterByStatus(Order.STATUS_CONFIRMED));
        findViewById(R.id.chipPreparing).setOnClickListener(v -> filterByStatus(Order.STATUS_PREPARING));
        findViewById(R.id.chipShipping).setOnClickListener(v -> filterByStatus(Order.STATUS_SHIPPING));
        findViewById(R.id.chipDelivered).setOnClickListener(v -> filterByStatus(Order.STATUS_DELIVERED));
        findViewById(R.id.chipCancelled).setOnClickListener(v -> filterByStatus(Order.STATUS_CANCELLED));
    }

    private void filterByStatus(String status) {
        if (status == null) {
            filteredOrders = new ArrayList<>(allOrders);
        } else {
            filteredOrders = allOrders.stream()
                    .filter(o -> status.equals(o.getStatus()))
                    .collect(Collectors.toList());
        }
        renderList();
    }

    private void loadOrders() {
        pb.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.GONE);

        new OrderRepository().getAllOrders(new OrderRepository.Callback<>() {
            @Override
            public void onSuccess(List<Order> orders) {
                allOrders = orders != null ? orders : new ArrayList<>();
                filteredOrders = new ArrayList<>(allOrders);
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    renderList();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(AdminOrderListActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderList() {
        if (filteredOrders.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }

        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_order_card, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                Order order = filteredOrders.get(position);
                View v = holder.itemView;

                ((TextView) v.findViewById(R.id.tvOrderCode)).setText(order.getOrderCode());
                ((TextView) v.findViewById(R.id.tvTotal)).setText(VND.format((long) order.getTotalAmount()) + "đ");

                String dateStr = order.getCreatedAt();
                String displayDate = "";
                if (dateStr != null) {
                    if (dateStr.length() >= 10) {
                        displayDate = dateStr.substring(0, 10);
                    } else {
                        displayDate = dateStr;
                    }
                }
                ((TextView) v.findViewById(R.id.tvDate)).setText(
                        displayDate + " · " + (order.getReceiverName() != null ? order.getReceiverName() : ""));

                TextView tvStatus = v.findViewById(R.id.tvStatus);
                tvStatus.setText(getStatusLabel(order.getStatus()));
                tvStatus.getBackground().setTint(getStatusColor(order.getStatus()));

                v.findViewById(R.id.btnCancel).setVisibility(View.GONE);
                v.findViewById(R.id.btnReturn).setVisibility(View.GONE);

                Button btnUpdate = v.findViewById(R.id.btnUpdate);
                btnUpdate.setVisibility(View.VISIBLE);
                btnUpdate.setText("Cập nhật");
                btnUpdate.setOnClickListener(x -> showStatusPicker(order));

                v.setOnClickListener(x -> {
                    Intent i = new Intent(AdminOrderListActivity.this, AdminOrderDetailActivity.class);
                    i.putExtra("order_id", order.getId());
                    startActivity(i);
                });
            }

            @Override
            public int getItemCount() {
                return filteredOrders.size();
            }
        });
    }

    private void showStatusPicker(Order order) {
        String currentStatus = order.getStatus();
        List<String> statusLabels = new ArrayList<>();
        List<String> statusValues = new ArrayList<>();

        if (Order.STATUS_PENDING.equals(currentStatus)) {
            statusLabels.add("✅ Xác nhận đơn");
            statusValues.add(Order.STATUS_CONFIRMED);
        } else if (Order.STATUS_CONFIRMED.equals(currentStatus)) {
            statusLabels.add("📦 Đang chuẩn bị");
            statusValues.add(Order.STATUS_PREPARING);
        } else if (Order.STATUS_PREPARING.equals(currentStatus)) {
            statusLabels.add("🚚 Đang giao");
            statusValues.add(Order.STATUS_SHIPPING);
        } else if (Order.STATUS_SHIPPING.equals(currentStatus)) {
            statusLabels.add("📬 Đã giao");
            statusValues.add(Order.STATUS_DELIVERED);
        }

        // Always allow huỷ đơn từ mọi trạng thái quản lý
        statusLabels.add("❌ Huỷ đơn");
        statusValues.add(Order.STATUS_CANCELLED);

        if (statusLabels.isEmpty()) {
            Toast.makeText(this, "Không có trạng thái cập nhật phù hợp.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cập nhật trạng thái: " + order.getOrderCode())
                .setItems(statusLabels.toArray(new String[0]), (dialog, which) -> {
                    updateOrderStatus(order, statusValues.get(which));
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        new OrderRepository().updateStatus(order.getId(), newStatus, null, new OrderRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminOrderListActivity.this, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show();
                    loadOrders();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(AdminOrderListActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String getStatusLabel(String status) {
        if (status == null) return "N/A";
        switch (status) {
            case Order.STATUS_PENDING: return "Chờ xác nhận";
            case Order.STATUS_CONFIRMED: return "Đã xác nhận";
            case Order.STATUS_PREPARING: return "Đang chuẩn bị";
            case Order.STATUS_SHIPPING: return "Đang giao";
            case Order.STATUS_DELIVERED: return "Đã giao";
            case Order.STATUS_CANCELLED: return "Đã huỷ";
            case Order.STATUS_REFUNDED: return "Hoàn tiền";
            case Order.STATUS_WAIT_PAY: return "Chờ thanh toán";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFF888888;
        switch (status) {
            case Order.STATUS_PENDING:
            case Order.STATUS_WAIT_PAY: return 0xFFF5A623;
            case Order.STATUS_CONFIRMED: return 0xFF5856D6;
            case Order.STATUS_PREPARING: return 0xFFFF9500;
            case Order.STATUS_SHIPPING: return 0xFF34AADC;
            case Order.STATUS_DELIVERED: return 0xFF34C759;
            case Order.STATUS_CANCELLED: return 0xFFFF3B30;
            case Order.STATUS_REFUNDED: return 0xFFFF2D55;
            default: return 0xFF007AFF;
        }
    }
}
