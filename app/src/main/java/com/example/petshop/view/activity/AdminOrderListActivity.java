package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Reuse manage users layout structure (toolbar + filter + list)
        setContentView(R.layout.activity_manage_users);
        ((TextView) findViewById(R.id.tvUserCount)).setText("đơn hàng");

        // Hack: reuse manage_users layout for orders
        rv = findViewById(R.id.rvUsers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        pb = findViewById(R.id.progressBar);

        // Override title
        // Filter chips → reuse chipAll/Customer/Admin → All/Pending/Shipping
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
                String date = order.getCreatedAt() != null && order.getCreatedAt().length() >= 10
                        ? order.getCreatedAt().substring(0, 10) : "";
                ((TextView) v.findViewById(R.id.tvDate)).setText(
                        date + " · " + (order.getReceiverName() != null ? order.getReceiverName() : ""));

                TextView tvStatus = v.findViewById(R.id.tvStatus);
                tvStatus.setText(order.getStatus());
                tvStatus.getBackground().setTint(statusColor(order.getStatus()));

                v.findViewById(R.id.btnCancel).setVisibility(View.GONE);
                v.findViewById(R.id.btnReturn).setVisibility(View.GONE);

                v.setOnClickListener(x -> {
                    Intent i = new Intent(AdminOrderListActivity.this, AdminOrderDetailActivity.class);
                    i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
                    startActivity(i);
                });
            }
            @Override public int getItemCount() { return list.size(); }
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
