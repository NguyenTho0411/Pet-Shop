package com.example.petshop.view.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.OrderItem;
import com.example.petshop.repository.OrderRepository;

import java.text.NumberFormat;
import java.util.List;
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

        // Setup UI for Admin
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnReturn).setVisibility(View.GONE);

        Button btnUpdate = findViewById(R.id.btnReorder); // Reuse the reorder button slot
        btnUpdate.setText("CẬP NHẬT TRẠNG THÁI");
        btnUpdate.setVisibility(View.VISIBLE);
        btnUpdate.setOnClickListener(v -> showStatusPicker());

        loadOrder(orderId);
    }

    private void loadOrder(String orderId) {
        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);

        new OrderRepository().getOrderById(orderId, new OrderRepository.Callback<>() {
            public void onSuccess(Order order) {
                if (order == null) { finish(); return; }
                currentOrder = order;
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    bindOrder(order);
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(AdminOrderDetailActivity.this, "Lỗi: " + err, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void bindOrder(Order order) {
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
        
        if (order.getVoucherDiscount() > 0) {
            findViewById(R.id.llDiscountRow).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvDiscount)).setText("-" + VND.format((long) order.getVoucherDiscount()) + "đ");
        } else {
            findViewById(R.id.llDiscountRow).setVisibility(View.GONE);
        }
        
        ((TextView) findViewById(R.id.tvTotal)).setText(VND.format((long) order.getTotalAmount()) + "đ");

        if (order.getNote() != null && !order.getNote().isEmpty()) {
            ((TextView) findViewById(R.id.tvOrderCode)).setText(order.getOrderCode() + "\n📝 " + order.getNote());
        }

        // Bind Items
        RecyclerView rvItems = findViewById(R.id.rvOrderItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(buildItemsAdapter(order.getItems()));
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> buildItemsAdapter(List<OrderItem> items) {
        return new RecyclerView.Adapter<>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_cart_item, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                OrderItem item = items.get(pos);
                View v = h.itemView;
                ((TextView) v.findViewById(R.id.tvProductName)).setText(item.getProductName());
                ((TextView) v.findViewById(R.id.tvUnitPrice)).setText(VND.format((long) item.getSubtotal()) + "đ");
                ((TextView) v.findViewById(R.id.tvProductDetail)).setText("x" + item.getQuantity()
                        + "  (" + VND.format((long) item.getUnitPrice()) + "đ/cái)");
                ((TextView) v.findViewById(R.id.tvTypeBadge)).setText(
                        "PET".equals(item.getProductType()) ? "THÚ CƯNG" : "ĐỒ ĂN");
                v.findViewById(R.id.btnRemove).setVisibility(View.GONE);
                v.findViewById(R.id.llQtyControl).setVisibility(View.GONE);
                if (item.getProductThumbnail() != null)
                    Glide.with(v).load(item.getProductThumbnail()).centerCrop()
                            .into((ImageView) v.findViewById(R.id.ivProduct));
            }
            @Override public int getItemCount() { return items != null ? items.size() : 0; }
        };
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
