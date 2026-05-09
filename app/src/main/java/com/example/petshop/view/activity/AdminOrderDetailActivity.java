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
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Notification;
import com.example.petshop.model.entity.User;
import com.example.petshop.repository.OrderRepository;
import com.example.petshop.repository.NotificationRepository;
import com.example.petshop.repository.UserRepository;
import com.example.petshop.utils.FirebaseHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminOrderDetailActivity extends AppCompatActivity {

    private Order  currentOrder;
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    private static final String[] STATUS_OPTIONS = {
            Order.STATUS_CONFIRMED, Order.STATUS_PREPARING,
            Order.STATUS_SHIPPING,  Order.STATUS_DELIVERED,
            Order.STATUS_CANCELLED, Order.STATUS_REFUNDED
    };

    private static final String[] STATUS_LABELS = {
            "✅ Xác nhận đơn", "📦 Đang chuẩn bị", "🚚 Đang giao",
            "📬 Đã giao", "❌ Huỷ đơn", "💸 Hoàn tiền"
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

        List<String> statusLabels = new ArrayList<>();
        List<String> statusValues = new ArrayList<>();
        String currentStatus = currentOrder.getStatus();

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

        statusLabels.add("❌ Huỷ đơn");
        statusValues.add(Order.STATUS_CANCELLED);

        if (statusLabels.isEmpty()) {
            Toast.makeText(this, "Không có trạng thái cập nhật phù hợp.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cập nhật trạng thái đơn hàng")
                .setItems(statusLabels.toArray(new String[0]), (dialog, which) -> {
                    updateOrderStatus(statusValues.get(which));
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void updateOrderStatus(String newStatus) {
        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);

        // Cập nhật pet status khi admin xác nhận đơn
        updatePetStatusForOrder(currentOrder, newStatus, () -> {
            new OrderRepository().updateStatus(currentOrder.getId(), newStatus, null,
                    new OrderRepository.Callback<>() {
                        public void onSuccess(Void v) {
                            currentOrder.setStatus(newStatus);
                            runOnUiThread(() -> {
                                pb.setVisibility(View.GONE);
                                ((TextView) findViewById(R.id.tvStatusBadge)).setText(newStatus);
                                Toast.makeText(AdminOrderDetailActivity.this,
                                        "Đã cập nhật → " + newStatus, Toast.LENGTH_SHORT).show();
                            });
                            // Gửi thông báo cho người dùng
                            sendOrderStatusNotification(currentOrder, newStatus);
                        }
                        public void onFailure(String err) {
                            runOnUiThread(() -> {
                                pb.setVisibility(View.GONE);
                                Toast.makeText(AdminOrderDetailActivity.this,
                                        "Lỗi: " + err, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
        });
    }

    private void sendOrderStatusNotification(Order order, String newStatus) {
        if (order == null || order.getUserId() == null) return;

        String title = "Cập nhật đơn hàng " + order.getOrderCode();
        String message = getStatusNotificationMessage(newStatus);

        Notification notif = new Notification();
        notif.setUserId(order.getUserId());
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType("ORDER");
        notif.setOrderId(order.getId());

        new NotificationRepository().createNotification(notif, new NotificationRepository.Callback<>() {
            @Override
            public void onSuccess(String data) {
                // Notification sent successfully
            }

            @Override
            public void onFailure(String error) {
                // Log error but don't show to user (background operation)
            }
        });
    }

    private String getStatusNotificationMessage(String status) {
        switch (status) {
            case "CONFIRMED":
                return "Đơn hàng của bạn đã được xác nhận và đang được chuẩn bị.";
            case "PREPARING":
                return "Đơn hàng của bạn đang được chuẩn bị.";
            case "SHIPPING":
                return "Đơn hàng của bạn đang được giao đến bạn!";
            case "DELIVERED":
                return "Đơn hàng của bạn đã được giao thành công.";
            case "COMPLETED":
                return "Đơn hàng của bạn đã hoàn thành. Cảm ơn bạn đã mua sắm!";
            case "CANCELLED":
                return "Rất tiếc, đơn hàng của bạn đã bị hủy.";
            case "REFUNDED":
                return "Đơn hàng của bạn đã được hoàn tiền.";
            default:
                return "Trạng thái đơn hàng: " + status;
        }
    }

    /**
     * Cập nhật trạng thái pet theo trạng thái đơn hàng:
     * - Khi admin xác nhận (CONFIRMED) hoặc các trạng thái tiếp theo → pet = SOLD
     * - Khi hủy đơn → pet = AVAILABLE
     */
    private void updatePetStatusForOrder(Order order, String newStatus, Runnable onComplete) {
        if (order.getItems() == null) {
            onComplete.run();
            return;
        }

        // Nếu hủy đơn → hoàn trạng thái pet về AVAILABLE
        if (Order.STATUS_CANCELLED.equals(newStatus) || Order.STATUS_REFUNDED.equals(newStatus)) {
            for (OrderItem item : order.getItems()) {
                if ("PET".equals(item.getProductType())) {
                    FirebaseHelper.db()
                            .collection("pets")
                            .document(item.getProductId())
                            .update("status", Pet.STATUS_AVAILABLE)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi cập nhật pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        }
        // Nếu xác nhận đơn hoặc các trạng thái tiếp theo → pet = SOLD
        else if (Order.STATUS_CONFIRMED.equals(newStatus) || Order.STATUS_PREPARING.equals(newStatus)
                || Order.STATUS_SHIPPING.equals(newStatus) || Order.STATUS_DELIVERED.equals(newStatus)
                || Order.STATUS_COMPLETED.equals(newStatus)) {
            for (OrderItem item : order.getItems()) {
                if ("PET".equals(item.getProductType())) {
                    FirebaseHelper.db()
                            .collection("pets")
                            .document(item.getProductId())
                            .update("status", Pet.STATUS_SOLD)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi cập nhật pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        }

        onComplete.run();
    }
}
