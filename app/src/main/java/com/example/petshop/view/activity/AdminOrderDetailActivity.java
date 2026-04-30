package com.example.petshop.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
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
import java.util.Locale;

public class AdminOrderDetailActivity extends AppCompatActivity {

    private Order  currentOrder;
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    private static final String[] STATUS_OPTIONS = {
            Order.STATUS_CONFIRMED, Order.STATUS_PREPARING,
            Order.STATUS_SHIPPING,  Order.STATUS_DELIVERED,
            Order.STATUS_COMPLETED, Order.STATUS_CANCELLED,
            Order.STATUS_REFUNDED
    };

    private static final String[] STATUS_LABELS = {
            "✅ Xác nhận đơn", "📦 Đang chuẩn bị", "🚚 Đang giao",
            "📬 Đã giao", "🏁 Hoàn thành", "❌ Huỷ đơn", "💸 Hoàn tiền"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        String orderId = getIntent().getStringExtra(OrderDetailActivity.EXTRA_ORDER_ID);
        if (orderId == null) { finish(); return; }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Hide customer actions, show admin update button
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnReturn).setVisibility(View.GONE);

        Button btnReorder = findViewById(R.id.btnReorder);
        btnReorder.setText("Cập nhật trạng thái");
        btnReorder.setVisibility(View.VISIBLE);

        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);

        new OrderRepository().getOrderById(orderId, new OrderRepository.Callback<>() {
            public void onSuccess(Order order) {
                if (order == null) { finish(); return; }
                currentOrder = order;
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    // Reuse the customer bindOrder view logic via reflection trick: just set texts manually
                    ((TextView) findViewById(R.id.tvOrderCode)).setText(order.getOrderCode());
                    TextView tvBadge = findViewById(R.id.tvStatusBadge);
                    tvBadge.setText(order.getStatus());

                    ((TextView) findViewById(R.id.tvReceiverName)).setText(order.getReceiverName());
                    ((TextView) findViewById(R.id.tvReceiverPhone)).setText(order.getReceiverPhone());
                    ((TextView) findViewById(R.id.tvAddress)).setText(
                            (order.getShippingAddress() != null ? order.getShippingAddress() + ", " : "")
                            + (order.getShippingCity() != null ? order.getShippingCity() : ""));

                    ((TextView) findViewById(R.id.tvPayMethod)).setText(
                            Order.PAYMENT_VNPAY.equals(order.getPaymentMethod()) ? "VNPay 🏦" : "COD 💵");
                    ((TextView) findViewById(R.id.tvSubtotal)).setText(VND.format((long) order.getSubtotal()) + "đ");
                    ((TextView) findViewById(R.id.tvShipping)).setText(VND.format((long) order.getShippingFee()) + "đ");
                    ((TextView) findViewById(R.id.tvTotal)).setText(VND.format((long) order.getTotalAmount()) + "đ");

                    // Note from customer
                    if (order.getNote() != null && !order.getNote().isEmpty()) {
                        ((TextView) findViewById(R.id.tvOrderCode)).setText(
                                order.getOrderCode() + "\n📝 " + order.getNote());
                    }
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> { pb.setVisibility(View.GONE); finish(); });
            }
        });

        btnReorder.setOnClickListener(v -> showStatusPicker());
    }

    private void showStatusPicker() {
        if (currentOrder == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật trạng thái đơn hàng")
                .setItems(STATUS_LABELS, (dialog, which) -> {
                    String newStatus = STATUS_OPTIONS[which];
                    updateOrderStatus(newStatus);
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void updateOrderStatus(String newStatus) {
        new OrderRepository().updateStatus(currentOrder.getId(), newStatus, null,
                new OrderRepository.Callback<>() {
                    public void onSuccess(Void v) {
                        currentOrder.setStatus(newStatus);
                        runOnUiThread(() -> {
                            ((TextView) findViewById(R.id.tvStatusBadge)).setText(newStatus);
                            Toast.makeText(AdminOrderDetailActivity.this,
                                    "Đã cập nhật → " + newStatus, Toast.LENGTH_SHORT).show();
                        });
                    }
                    public void onFailure(String err) {
                        runOnUiThread(() -> Toast.makeText(AdminOrderDetailActivity.this,
                                "Lỗi: " + err, Toast.LENGTH_LONG).show());
                    }
                });
    }
}
