package com.example.petshop.view.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.petshop.model.entity.Cart;
import com.example.petshop.repository.CartRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    // All possible statuses in order
    private static final List<String> STATUS_FLOW = Arrays.asList(
            Order.STATUS_PENDING, Order.STATUS_CONFIRMED, Order.STATUS_PREPARING,
            Order.STATUS_SHIPPING, Order.STATUS_DELIVERED, Order.STATUS_COMPLETED);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null) { finish(); return; }

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);

        OrderRepository repo = new OrderRepository();
        OrderRepository.Callback<Order> callback = new OrderRepository.Callback<>() {
            public void onSuccess(Order order) {
                if (order == null) { finish(); return; }
                runOnUiThread(() -> { pb.setVisibility(View.GONE); bindOrder(order); });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> { pb.setVisibility(View.GONE); Toast.makeText(OrderDetailActivity.this, err, Toast.LENGTH_SHORT).show(); finish(); });
            }
        };

        // orderId có thể là UUID (document ID) hoặc orderCode (ORD-xxx) từ VNPay
        if (orderId.startsWith("ORD-")) {
            repo.getOrderByCode(orderId, callback);
        } else {
            repo.getOrderById(orderId, callback);
        }
    }

    private void bindOrder(Order order) {
        // Toolbar
        ((TextView) findViewById(R.id.tvOrderCode)).setText(order.getOrderCode());
        TextView tvBadge = findViewById(R.id.tvStatusBadge);
        tvBadge.setText(statusVi(order.getStatus()));
        tvBadge.getBackground().setTint(statusColor(order.getStatus()));

        // Timeline
        buildTimeline(order.getStatus());

        // Address
        ((TextView) findViewById(R.id.tvReceiverName)).setText(
                order.getReceiverName() != null ? order.getReceiverName() : "");
        ((TextView) findViewById(R.id.tvReceiverPhone)).setText(
                order.getReceiverPhone() != null ? "📱 " + order.getReceiverPhone() : "");
        String fullAddr = (order.getShippingAddress() != null ? order.getShippingAddress() + ", " : "")
                + (order.getShippingDistrict() != null ? order.getShippingDistrict() + ", " : "")
                + (order.getShippingCity() != null ? order.getShippingCity() : "");
        ((TextView) findViewById(R.id.tvAddress)).setText(fullAddr);

        // Items
        RecyclerView rv = findViewById(R.id.rvOrderItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(buildItemsAdapter(order.getItems()));

        // Payment
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

        // Bottom actions
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnReorder = findViewById(R.id.btnReorder);
        Button btnReturn = findViewById(R.id.btnReturn);

        if (Order.STATUS_WAIT_PAY.equals(order.getStatus()) && Order.PAYMENT_VNPAY.equals(order.getPaymentMethod())) {
            btnReorder.setVisibility(View.VISIBLE);
            btnReorder.setText("THANH TOÁN NGAY");
            btnReorder.setOnClickListener(v -> openVNPay(order));
        } else {
            // Mua lại đơn đã hủy hoặc đã hoàn thành
            btnReorder.setVisibility(Order.STATUS_CANCELLED.equals(order.getStatus())
                    || Order.STATUS_COMPLETED.equals(order.getStatus()) ? View.VISIBLE : View.GONE);

            btnReorder.setText("MUA LẠI");
            btnReorder.setOnClickListener(v -> reorder(order));
        }

        btnCancel.setVisibility(order.canCancel() ? View.VISIBLE : View.GONE);
        btnReturn.setVisibility(order.canReview() ? View.VISIBLE : View.GONE);

        btnCancel.setOnClickListener(v -> confirmCancel(order));
        btnReturn.setOnClickListener(v -> {
            boolean hasPet = order.getItems() != null
                    && order.getItems().stream().anyMatch(i -> "PET".equals(i.getProductType()));
            Intent i = new Intent(this, ReturnRequestActivity.class);
            i.putExtra("order_id", order.getId());
            i.putExtra("has_pet", hasPet);
            startActivity(i);
        });
    }

    private void buildTimeline(String currentStatus) {
        LinearLayout ll = findViewById(R.id.llTimeline);
        ll.removeAllViews();

        String[] steps = {
                Order.STATUS_PENDING,   "Chờ xác nhận",
                Order.STATUS_CONFIRMED, "Đã xác nhận",
                Order.STATUS_PREPARING, "Đang chuẩn bị",
                Order.STATUS_SHIPPING,  "Đang giao hàng",
                Order.STATUS_DELIVERED, "Đã giao thành công"
        };

        // If cancelled/refunded show special state
        if (Order.STATUS_CANCELLED.equals(currentStatus) || Order.STATUS_REFUNDED.equals(currentStatus)) {
            addTimelineStep(ll, Order.STATUS_CANCELLED.equals(currentStatus) ? "Đã huỷ" : "Hoàn tiền",
                    true, Color.parseColor("#FF3B30"));
            return;
        }

        int currentIdx = STATUS_FLOW.indexOf(currentStatus);
        for (int i = 0; i < steps.length; i += 2) {
            String s    = steps[i];
            String label = steps[i + 1];
            int    idx  = STATUS_FLOW.indexOf(s);
            boolean done   = idx <= currentIdx;
            boolean active = s.equals(currentStatus);
            int color = done ? Color.parseColor("#34C759") : Color.parseColor("#CCCCCC");
            if (active) color = Color.parseColor("#F5A623");
            addTimelineStep(ll, label, done || active, color);
        }
    }

    private void addTimelineStep(LinearLayout parent, String label, boolean filled, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = 12;
        row.setLayoutParams(rowLp);

        // Circle dot
        View dot = new View(this);
        int size = (int) (12 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(size, size);
        dotLp.rightMargin = (int) (12 * getResources().getDisplayMetrics().density);
        dot.setLayoutParams(dotLp);
        dot.setBackgroundResource(R.drawable.bg_circle_orange);
        if (!filled) dot.setBackgroundResource(R.drawable.bg_circle_white);
        dot.getBackground().setTint(color);
        row.addView(dot);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13f);
        tv.setTextColor(filled ? Color.parseColor("#1C1C1E") : Color.parseColor("#AEAEB2"));
        if (filled) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tv);

        parent.addView(row);
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> buildItemsAdapter(List<OrderItem> items) {
        return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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

    private void reorder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            Toast.makeText(this, "Đơn hàng không có sản phẩm để mua lại", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        Toast.makeText(this, "Đang tạo lại giỏ hàng...", Toast.LENGTH_SHORT).show();

        new CartRepository().reorderFromOrderItems(uid, order.getItems(), new CartRepository.Callback<Cart>() {
            @Override
            public void onSuccess(Cart cart) {
                runOnUiThread(() -> {
                    Toast.makeText(OrderDetailActivity.this, "Đã thêm lại sản phẩm vào giỏ", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(OrderDetailActivity.this, CheckoutActivity.class);
                    startActivity(i);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(
                        OrderDetailActivity.this,
                        "Không thể mua lại: " + error,
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }
    private void confirmCancel(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Huỷ đơn hàng")
                .setMessage("Xác nhận huỷ đơn " + order.getOrderCode() + "?\nKho hàng sẽ được hoàn trả.")
                .setPositiveButton("Huỷ đơn", (d, w) ->
                        new OrderRepository().cancelOrder(order.getId(), "Khách huỷ", new OrderRepository.Callback<>() {
                            public void onSuccess(Void v) { runOnUiThread(() -> { Toast.makeText(OrderDetailActivity.this, "Đã huỷ đơn", Toast.LENGTH_SHORT).show(); finish(); }); }
                            public void onFailure(String err) { runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, err, Toast.LENGTH_LONG).show()); }
                        }))
                .setNegativeButton("Không", null).show();
    }

    private void openVNPay(Order order) {
        String txnRef = order.getOrderCode() != null && !order.getOrderCode().isEmpty()
                ? order.getOrderCode()
                : order.getId();

        String url = com.example.petshop.utils.VNPayHelper.buildPaymentUrl(
                txnRef,
                (long) order.getTotalAmount(),
                "Thanh toan don hang " + txnRef
        );

        Intent i = new Intent(this, VNPayWebViewActivity.class);
        i.putExtra(VNPayWebViewActivity.EXTRA_PAYMENT_URL, url);
        i.putExtra(VNPayWebViewActivity.EXTRA_ORDER_ID, txnRef);
        startActivity(i);
    }

    private String statusVi(String s) {
        if (s == null) return "—";
        switch (s) {
            case Order.STATUS_PENDING:   return "Chờ xác nhận";
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
            case Order.STATUS_CONFIRMED: return 0xFF007AFF;
            case Order.STATUS_SHIPPING:  return 0xFF34AADC;
            case Order.STATUS_DELIVERED:
            case Order.STATUS_COMPLETED: return 0xFF34C759;
            case Order.STATUS_CANCELLED: return 0xFFFF3B30;
            case Order.STATUS_REFUNDED:  return 0xFFFF9500;
            default: return 0xFF888888;
        }
    }
    
    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra("from_checkout", false)) {
            Intent i = new Intent(this, PetShopActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
