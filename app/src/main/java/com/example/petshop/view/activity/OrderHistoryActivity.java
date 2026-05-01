package com.example.petshop.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class OrderHistoryActivity extends AppCompatActivity {
    private String currentStatusFilter = null;
    private List<Order>    allOrders = new ArrayList<>();
    private RecyclerView   rv;
    private ProgressBar    progressBar;
    private LinearLayout   llEmpty;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        progressBar = findViewById(R.id.progressBar);
        llEmpty     = findViewById(R.id.llEmpty);
        rv          = findViewById(R.id.rvOrders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chip filters
        setChipFilter(R.id.chipAll,       null);
        setChipFilter(R.id.chipPending,   Order.STATUS_PENDING);
        setChipFilter(R.id.chipShipping,  Order.STATUS_SHIPPING);
        setChipFilter(R.id.chipDelivered, Order.STATUS_DELIVERED);
        setChipFilter(R.id.chipCancelled, Order.STATUS_CANCELLED);

        loadOrders();
    }

    private void applyCurrentFilter() {
        List<Order> list;

        if (currentStatusFilter == null) {
            list = allOrders;
        } else {
            list = allOrders.stream()
                    .filter(o -> currentStatusFilter.equals(o.getStatus()))
                    .collect(Collectors.toList());
        }

        renderList(list);
    }
    private void setChipFilter(int chipId, String status) {
        findViewById(chipId).setOnClickListener(v -> {
            currentStatusFilter = status;
            applyCurrentFilter();
        });
    }

    private void loadOrders() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }

        progressBar.setVisibility(View.VISIBLE);
        new OrderRepository().getOrdersByUser(uid, new OrderRepository.Callback<>() {
            public void onSuccess(List<Order> orders) {
                allOrders = orders != null ? orders : new ArrayList<>();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    applyCurrentFilter();
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE); Toast.makeText(OrderHistoryActivity.this, err, Toast.LENGTH_SHORT).show(); });
            }
        });
    }

    private void renderList(List<Order> list) {
        if (list.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            return;
        }
        llEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
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

                // Date parsing fix
                String dateStr = order.getCreatedAt();
                String displayDate = "";
                if (dateStr != null) {
                    if (dateStr.startsWith("Timestamp(")) {
                        // If it's the messy Firebase string, try to keep it simple or hide it
                        displayDate = "Mới đây"; 
                    } else if (dateStr.length() >= 10) {
                        displayDate = dateStr.substring(0, 10);
                    } else {
                        displayDate = dateStr;
                    }
                }
                ((TextView) v.findViewById(R.id.tvDate)).setText(displayDate);

                // Status badge
                TextView tvStatus = v.findViewById(R.id.tvStatus);
                tvStatus.setText(statusVi(order.getStatus()));
                tvStatus.getBackground().setTint(statusColor(order.getStatus()));

                // Can cancel
                Button btnCancel = v.findViewById(R.id.btnCancel);
                Button btnReturn = v.findViewById(R.id.btnReturn);
                btnCancel.setVisibility(order.canCancel() ? View.VISIBLE : View.GONE);
                btnReturn.setVisibility(order.canReview() ? View.VISIBLE : View.GONE);

                btnCancel.setOnClickListener(x -> confirmCancel(order));
                btnReturn.setOnClickListener(x -> {
                    Intent i = new Intent(OrderHistoryActivity.this, ReturnRequestActivity.class);
                    i.putExtra("order_id", order.getId());
                    i.putExtra("has_pet", hasPetItem(order));
                    startActivity(i);
                });

                // Pay now button for VNPay pending
                Button btnPay = v.findViewById(R.id.btnUpdate); // Reuse btnUpdate for Pay
                if (Order.STATUS_WAIT_PAY.equals(order.getStatus()) && Order.PAYMENT_VNPAY.equals(order.getPaymentMethod())) {
                    btnPay.setVisibility(View.VISIBLE);
                    btnPay.setText("Thanh toán");
                    btnPay.setOnClickListener(x -> {
                        String url = com.example.petshop.utils.VNPayHelper.buildPaymentUrl(order.getId(), (long)order.getTotalAmount(), "Thanh toan don hang " + order.getOrderCode());
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                    });
                }

                v.setOnClickListener(x -> {
                    Intent i = new Intent(OrderHistoryActivity.this, OrderDetailActivity.class);
                    i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
                    startActivity(i);
                });
            }
            @Override public int getItemCount() { return list.size(); }
        });
    }

    private void confirmCancel(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Huỷ đơn hàng")
                .setMessage("Bạn có chắc muốn huỷ đơn " + order.getOrderCode() + "?")
                .setPositiveButton("Huỷ đơn", (d, w) -> {
                    new OrderRepository().cancelOrder(order.getId(), "Khách huỷ", new OrderRepository.Callback<>() {
                        public void onSuccess(Void v) { runOnUiThread(() -> { Toast.makeText(OrderHistoryActivity.this, "Đã huỷ đơn hàng", Toast.LENGTH_SHORT).show(); loadOrders(); }); }
                        public void onFailure(String err) { runOnUiThread(() -> Toast.makeText(OrderHistoryActivity.this, err, Toast.LENGTH_LONG).show()); }
                    });
                })
                .setNegativeButton("Không", null).show();
    }

    private boolean hasPetItem(Order order) {
        if (order.getItems() == null) return false;
        return order.getItems().stream().anyMatch(i -> "PET".equals(i.getProductType()));
    }

    private String statusVi(String s) {
        if (s == null) return "—";
        switch (s) {
            case Order.STATUS_PENDING:   return "Chờ xác nhận";
            case Order.STATUS_WAIT_PAY:  return "Chờ thanh toán";
            case Order.STATUS_CONFIRMED: return "Đã xác nhận";
            case Order.STATUS_PREPARING: return "Đang chuẩn bị";
            case Order.STATUS_SHIPPING:  return "Đang giao";
            case Order.STATUS_DELIVERED: return "Đã giao";
            case Order.STATUS_COMPLETED: return "Hoàn thành";
            case Order.STATUS_CANCELLED: return "Đã huỷ";
            case Order.STATUS_REFUNDED:  return "Hoàn tiền";
            default: return s;
        }
    }

    private int statusColor(String s) {
        if (s == null) return 0xFF888888;
        switch (s) {
            case Order.STATUS_PENDING:   return 0xFFF5A623;
            case Order.STATUS_WAIT_PAY:  return 0xFFFF3B30; // Red for attention
            case Order.STATUS_CONFIRMED: return 0xFF007AFF;
            case Order.STATUS_PREPARING: return 0xFF5856D6;
            case Order.STATUS_SHIPPING:  return 0xFF34AADC;
            case Order.STATUS_DELIVERED:
            case Order.STATUS_COMPLETED: return 0xFF34C759;
            case Order.STATUS_CANCELLED: return 0xFFFF3B30;
            case Order.STATUS_REFUNDED:  return 0xFFFF9500;
            default: return 0xFF888888;
        }
    }
}
