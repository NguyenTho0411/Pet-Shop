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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminOrderListActivity extends AppCompatActivity {

    private List<Order>  allOrders = new ArrayList<>();
    private RecyclerView rv;
    private ProgressBar  pb;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    private static final String[] STATUS_OPTIONS = {
            Order.STATUS_CONFIRMED, Order.STATUS_PREPARING,
            Order.STATUS_SHIPPING,  Order.STATUS_DELIVERED,
            Order.STATUS_COMPLETED, Order.STATUS_CANCELLED,
            Order.STATUS_REFUNDED
    };

    private static final String[] STATUS_LABELS = {
            "✅ Xác nhận", "📦 Đang chuẩn bị", "🚚 Đang giao",
            "📬 Đã giao", "🏁 Hoàn thành", "❌ Huỷ đơn", "💸 Hoàn tiền"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);
        ((TextView) findViewById(R.id.tvUserCount)).setText("đơn hàng");

        rv = findViewById(R.id.rvUsers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        pb = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setFilter(R.id.chipAll,      null);
        setFilter(R.id.chipCustomer, Order.STATUS_PENDING);
        setFilter(R.id.chipAdmin,    Order.STATUS_SHIPPING);

        loadOrders();
    }

    private void setFilter(int id, String status) {
        View chip = findViewById(id);
        if (chip == null) return;
        chip.setOnClickListener(v -> renderList(
                status == null ? allOrders
                        : allOrders.stream().filter(o -> status.equals(o.getStatus())).collect(Collectors.toList())));
    }

    private void loadOrders() {
        pb.setVisibility(View.VISIBLE);
        new OrderRepository().getAllOrders(new OrderRepository.Callback<>() {
            public void onSuccess(List<Order> orders) {
                allOrders = orders != null ? orders : new ArrayList<>();
                runOnUiThread(() -> { pb.setVisibility(View.GONE); renderList(allOrders); });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> { pb.setVisibility(View.GONE); Toast.makeText(AdminOrderListActivity.this, err, Toast.LENGTH_SHORT).show(); });
            }
        });
    }

    private void renderList(List<Order> list) {
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_order_card, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                Order order = list.get(pos);
                View v = h.itemView;
                ((TextView) v.findViewById(R.id.tvOrderCode)).setText(order.getOrderCode());
                ((TextView) v.findViewById(R.id.tvTotal)).setText(VND.format((long) order.getTotalAmount()) + "đ");
                
                String dateStr = order.getCreatedAt();
                String displayDate = "";
                if (dateStr != null) {
                    if (dateStr.startsWith("Timestamp(")) {
                        displayDate = "N/A";
                    } else if (dateStr.length() >= 10) {
                        displayDate = dateStr.substring(0, 10);
                    } else {
                        displayDate = dateStr;
                    }
                }
                ((TextView) v.findViewById(R.id.tvDate)).setText(
                        displayDate + " · " + (order.getReceiverName() != null ? order.getReceiverName() : ""));

                TextView tvStatus = v.findViewById(R.id.tvStatus);
                tvStatus.setText(order.getStatus());
                tvStatus.getBackground().setTint(statusColor(order.getStatus()));

                v.findViewById(R.id.btnCancel).setVisibility(View.GONE);
                v.findViewById(R.id.btnReturn).setVisibility(View.GONE);
                
                Button btnUpdate = v.findViewById(R.id.btnUpdate);
                btnUpdate.setVisibility(View.VISIBLE);
                btnUpdate.setOnClickListener(x -> showStatusPicker(order));

                v.setOnClickListener(x -> {
                    Intent i = new Intent(AdminOrderListActivity.this, AdminOrderDetailActivity.class);
                    i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
                    startActivity(i);
                });
            }
            @Override public int getItemCount() { return list.size(); }
        });
    }

    private void showStatusPicker(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật: " + order.getOrderCode())
                .setItems(STATUS_LABELS, (dialog, which) -> {
                    String newStatus = STATUS_OPTIONS[which];
                    updateOrderStatus(order, newStatus);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        new OrderRepository().updateStatus(order.getId(), newStatus, null, new OrderRepository.Callback<>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    order.setStatus(newStatus);
                    loadOrders(); // Refresh list
                    Toast.makeText(AdminOrderListActivity.this, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(AdminOrderListActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private int statusColor(String s) {
        if (s == null) return 0xFF888888;
        switch (s) {
            case Order.STATUS_PENDING:   return 0xFFF5A623;
            case Order.STATUS_SHIPPING:  return 0xFF34AADC;
            case Order.STATUS_DELIVERED:
            case Order.STATUS_COMPLETED: return 0xFF34C759;
            case Order.STATUS_CANCELLED: return 0xFFFF3B30;
            default: return 0xFF007AFF;
        }
    }
}
