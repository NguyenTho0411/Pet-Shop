package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Address;
import com.example.petshop.model.entity.Cart;
import com.example.petshop.model.entity.CartItem;
import com.example.petshop.model.entity.Notification;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.model.entity.Voucher;
import com.example.petshop.repository.AddressRepository;
import com.example.petshop.repository.CartRepository;
import com.example.petshop.repository.NotificationRepository;
import com.example.petshop.repository.OrderRepository;
import com.example.petshop.repository.PromotionRepository;
import com.example.petshop.repository.VoucherRepository;
import com.example.petshop.utils.ShippingHelper;
import com.example.petshop.utils.VNPayHelper;
import com.example.petshop.view.adapter.CartItemAdapter;
import com.example.petshop.viewmodel.CartViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private CartViewModel vm;
    private Cart cart;
    private Address selectedAddress;
    private double shippingFee = 0;
    private String etaDays = "";
    private String paymentMethod = Order.PAYMENT_COD;

    private TextView tvReceiverName, tvReceiverPhone, tvAddressDetail;
    private TextView tvSubtotal, tvShipping, tvTotal, tvShipEta, tvDiscount;
    private TextView tvSelectedVoucher, tvChooseVoucher;
    private LinearLayout llDiscountRow, llVoucherChips;
    private CardView cvSelectedVoucher;
    private RadioButton rbCod, rbVnpay;
    private ProgressBar progressBar;

    private double voucherDiscount = 0;
    private boolean voucherChipsExpanded = false;

    private String selectedVoucherId = null;
    private String selectedVoucherCode = null;
    private boolean isPromotionVoucher = false;

    private final List<AppliedVoucher> appliedVouchers = new ArrayList<>();

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    private List<Voucher> systemVouchers = new ArrayList<>();
    private List<Promotion> promotionVouchers = new ArrayList<>();

    private static class AppliedVoucher {
        String id;
        String code;
        boolean promotionVoucher;
        double discount;

        AppliedVoucher(String id, String code, boolean promotionVoucher, double discount) {
            this.id = id;
            this.code = code;
            this.promotionVoucher = promotionVoucher;
            this.discount = discount;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        vm = new ViewModelProvider(this).get(CartViewModel.class);

        initViews();
        loadAddress(null);

        vm.getCart().observe(this, c -> {
            if (c != null) {
                this.cart = c;
                renderOrderItems(c.getItems());
                recalculateVoucherDiscount(c.calculateSubtotal());
                updatePriceSummary(c.calculateSubtotal());

                if (selectedAddress != null) {
                    calculateShipping();
                }
            }
        });

        vm.loadCart();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvReceiverPhone = findViewById(R.id.tvReceiverPhone);
        tvAddressDetail = findViewById(R.id.tvAddressDetail);
        tvSubtotal = findViewById(R.id.tvSummarySubtotal);
        tvShipping = findViewById(R.id.tvSummaryShipping);
        tvTotal = findViewById(R.id.tvSummaryTotal);
        tvShipEta = findViewById(R.id.tvShipEta);
        tvDiscount = findViewById(R.id.tvSummaryDiscount);
        llDiscountRow = findViewById(R.id.llDiscountRow);
        tvSelectedVoucher = findViewById(R.id.tvSelectedVoucher);
        tvChooseVoucher = findViewById(R.id.tvChooseVoucher);
        llVoucherChips = findViewById(R.id.llVoucherChips);
        cvSelectedVoucher = findViewById(R.id.cvSelectedVoucher);
        rbCod = findViewById(R.id.rbCod);
        rbVnpay = findViewById(R.id.rbVnpay);

        LinearLayout llCod = findViewById(R.id.llPayCod);
        LinearLayout llVnpay = findViewById(R.id.llPayVnpay);

        llCod.setOnClickListener(v -> selectPayment(Order.PAYMENT_COD));
        rbCod.setOnClickListener(v -> selectPayment(Order.PAYMENT_COD));
        llVnpay.setOnClickListener(v -> selectPayment(Order.PAYMENT_VNPAY));
        rbVnpay.setOnClickListener(v -> selectPayment(Order.PAYMENT_VNPAY));

        ((Button) findViewById(R.id.btnApplyVoucher)).setOnClickListener(v -> applyVoucher());
        tvChooseVoucher.setOnClickListener(v -> toggleVoucherChips());

        if (cvSelectedVoucher != null) {
            cvSelectedVoucher.findViewById(R.id.btnRemoveVoucher).setOnClickListener(v -> removeVoucher());
        }

        loadSystemVouchers();

        View.OnClickListener changeAddressListener = v -> {
            Intent i = new Intent(this, ManageAddressActivity.class);
            i.putExtra("pick_mode", true);
            startActivityForResult(i, 100);
        };

        findViewById(R.id.btnChangeAddress).setOnClickListener(changeAddressListener);
        findViewById(R.id.cvAddress).setOnClickListener(changeAddressListener);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ((Button) findViewById(R.id.btnPlaceOrder)).setOnClickListener(v -> placeOrder());
    }

    private void selectPayment(String method) {
        paymentMethod = method;
        rbCod.setChecked(Order.PAYMENT_COD.equals(method));
        rbVnpay.setChecked(Order.PAYMENT_VNPAY.equals(method));
    }

    private void loadAddress(String specificId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) return;

        new AddressRepository().getAddresses(uid, new AddressRepository.Callback<>() {
            @Override
            public void onSuccess(List<Address> list) {
                if (list == null || list.isEmpty()) {
                    runOnUiThread(() -> {
                        selectedAddress = null;
                        tvReceiverName.setText("Chọn địa chỉ giao hàng");
                        tvReceiverPhone.setText("");
                        tvAddressDetail.setText("");
                    });
                    return;
                }

                Address toSet = null;

                if (specificId != null) {
                    for (Address a : list) {
                        if (specificId.equals(a.getId())) {
                            toSet = a;
                            break;
                        }
                    }
                }

                if (toSet == null) {
                    for (Address a : list) {
                        if (a.isDefault()) {
                            toSet = a;
                            break;
                        }
                    }
                }

                if (toSet == null) toSet = list.get(0);

                final Address finalToSet = toSet;
                runOnUiThread(() -> setAddress(finalToSet));
            }

            @Override
            public void onFailure(String err) {
            }
        });
    }

    private void setAddress(Address addr) {
        selectedAddress = addr;
        tvReceiverName.setText(addr.getReceiverName() != null ? addr.getReceiverName() : "Người nhận");
        tvReceiverPhone.setText(addr.getReceiverPhone() != null ? addr.getReceiverPhone() : "");
        tvAddressDetail.setText(addr.getFullAddress());

        calculateShipping();
    }

    private void calculateShipping() {
        if (selectedAddress != null && cart != null) {
            ShippingHelper.calculate(selectedAddress, cart.calculateSubtotal(), new ShippingHelper.ShippingCallback() {
                @Override
                public void onResult(double fee, String eta) {
                    shippingFee = fee;
                    etaDays = eta;
                    runOnUiThread(() -> updatePriceSummary(cart.calculateSubtotal()));
                }

                @Override
                public void onError(String e) {
                }
            });
        }
    }

    private void renderOrderItems(List<CartItem> items) {
        RecyclerView rv = findViewById(R.id.rvOrderItems);
        rv.setLayoutManager(new LinearLayoutManager(this));

        CartItemAdapter a = new CartItemAdapter(new ArrayList<>(items),
                new CartItemAdapter.OnCartAction() {
                    @Override
                    public void onRemove(CartItem item) {
                    }

                    @Override
                    public void onQtyChange(CartItem item, int qty) {
                    }
                });

        rv.setAdapter(a);
    }

    private void updatePriceSummary(double subtotal) {
        tvSubtotal.setText(VND.format((long) subtotal) + "đ");

        tvShipping.setText(shippingFee == 0 && cart != null
                ? (cart.calculateSubtotal() >= 500_000 ? "Miễn phí 🎉" : "Đang tính...")
                : VND.format((long) shippingFee) + "đ");

        double total = subtotal + shippingFee - voucherDiscount;
        tvTotal.setText(VND.format((long) Math.max(0, total)) + "đ");

        if (!etaDays.isEmpty()) {
            tvShipEta.setText("⏱ Dự kiến giao: " + etaDays);
        }

        if (voucherDiscount > 0) {
            llDiscountRow.setVisibility(View.VISIBLE);
            tvDiscount.setText("-" + VND.format((long) voucherDiscount) + "đ");
        } else {
            llDiscountRow.setVisibility(View.GONE);
            tvDiscount.setText("-0đ");
        }

        refreshSelectedVoucherUi();
    }

    private void applyVoucher() {
        TextInputEditText etVoucher = findViewById(R.id.etVoucher);
        String code = etVoucher.getText() != null
                ? etVoucher.getText().toString().trim().toUpperCase()
                : "";

        if (code.isEmpty()) {
            Toast.makeText(this, "Nhập mã voucher", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cart == null) {
            Toast.makeText(this, "Giỏ hàng chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        new PromotionRepository().getByCode(code, new PromotionRepository.Callback<Promotion>() {
            @Override
            public void onSuccess(Promotion promo) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (promo != null && promo.isVoucher()) {
                        validateAndApplyPromotionVoucher(promo);
                    } else {
                        applyVoucherFromVouchers(code);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                applyVoucherFromVouchers(code);
            }
        });
    }

    private void applyVoucherFromVouchers(String code) {
        new VoucherRepository().getByCode(code, new VoucherRepository.Callback<Voucher>() {
            @Override
            public void onSuccess(Voucher v) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    validateAndApplyVoucher(v);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CheckoutActivity.this,
                            "Mã voucher không tồn tại hoặc đã hết hạn",
                            Toast.LENGTH_SHORT).show();

                    if (cart != null) {
                        updatePriceSummary(cart.calculateSubtotal());
                    }
                });
            }
        });
    }

    private void validateAndApplyPromotionVoucher(Promotion promo) {
        double subtotal = cart.calculateSubtotal();

        if (!promo.isActive()) {
            Toast.makeText(this, "Voucher hiện không khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!promo.isWithinDateRange()) {
            Toast.makeText(this, "Voucher đã hết hạn sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (promo.getUsageLimit() > 0 && promo.getUsageCount() >= promo.getUsageLimit()) {
            Toast.makeText(this, "Voucher đã hết lượt sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        double minOrder = 0;
        try {
            minOrder = promo.getMinOrderAmount();
        } catch (Exception ignored) {
        }

        if (subtotal < minOrder) {
            Toast.makeText(this,
                    "Đơn hàng tối thiểu " + VND.format((long) minOrder) + "đ để dùng voucher này",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để dùng voucher", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new PromotionRepository().getUserPromotionUsageCount(uid, promo.getId(), new PromotionRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long usageCountData) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    long usageCount = usageCountData != null ? usageCountData : 0L;
                    int perUserLimit = Math.max(0, promo.getPerUserLimit());
                    if (perUserLimit > 0 && usageCount >= perUserLimit) {
                        Toast.makeText(CheckoutActivity.this,
                                "Bạn đã dùng mã này tối đa " + perUserLimit + " lần",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (hasAppliedVoucher(promo.getId(), promo.getVoucherCode())) {
                        Toast.makeText(CheckoutActivity.this, "Mã này đã được áp dụng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double discount = promo.calculateDiscount(subtotal);
                    appliedVouchers.add(new AppliedVoucher(
                            promo.getId(),
                            promo.getVoucherCode(),
                            true,
                            discount
                    ));

                    recalculateVoucherDiscount(subtotal);
                    updatePriceSummary(subtotal);
                    Toast.makeText(CheckoutActivity.this,
                            "Đã cộng dồn mã " + promo.getVoucherCode(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CheckoutActivity.this,
                            "Không thể kiểm tra lượt dùng voucher. Vui lòng thử lại.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void validateAndApplyVoucher(Voucher v) {
        double subtotal = cart.calculateSubtotal();

        if (!v.isActive()) {
            Toast.makeText(this, "Voucher hiện không khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());

        if (v.isExpired(today)) {
            Toast.makeText(this, "Voucher đã hết hạn sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (v.isUsageLimitReached()) {
            Toast.makeText(this, "Voucher đã hết lượt sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subtotal < v.getMinOrderAmount()) {
            Toast.makeText(this,
                    "Đơn hàng tối thiểu " + VND.format(v.getMinOrderAmount()) + "đ để dùng voucher này",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để dùng voucher", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new VoucherRepository().getUserVoucherUsageCount(uid, v.getId(), new VoucherRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long usageCountData) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    long usageCount = usageCountData != null ? usageCountData : 0L;
                    int perUserLimit = Math.max(0, v.getPerUserLimit());
                    if (perUserLimit > 0 && usageCount >= perUserLimit) {
                        Toast.makeText(CheckoutActivity.this,
                                "Bạn đã dùng mã này tối đa " + perUserLimit + " lần",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (hasAppliedVoucher(v.getId(), v.getCode())) {
                        Toast.makeText(CheckoutActivity.this, "Mã này đã được áp dụng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double discount = v.calculateDiscount(subtotal);
                    appliedVouchers.add(new AppliedVoucher(
                            v.getId(),
                            v.getCode(),
                            false,
                            discount
                    ));

                    recalculateVoucherDiscount(subtotal);
                    updatePriceSummary(subtotal);
                    Toast.makeText(CheckoutActivity.this,
                            "Đã cộng dồn mã " + v.getCode(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CheckoutActivity.this,
                            "Không thể kiểm tra lượt dùng voucher. Vui lòng thử lại.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadSystemVouchers() {
        final boolean[] loaded = {false, false};

        new VoucherRepository().getSystemVouchers(new VoucherRepository.Callback<List<Voucher>>() {
            @Override
            public void onSuccess(List<Voucher> vouchers) {
                systemVouchers = vouchers != null ? vouchers : new ArrayList<>();
                loaded[0] = true;

                if (loaded[1]) runOnUiThread(() -> displayAllVouchers());
            }

            @Override
            public void onFailure(String err) {
                loaded[0] = true;

                if (loaded[1]) runOnUiThread(() -> displayAllVouchers());
            }
        });

        new PromotionRepository().getSystemVouchers(new PromotionRepository.Callback<List<Promotion>>() {
            @Override
            public void onSuccess(List<Promotion> promos) {
                promotionVouchers = promos != null ? promos : new ArrayList<>();
                loaded[1] = true;

                if (loaded[0]) runOnUiThread(() -> displayAllVouchers());
            }

            @Override
            public void onFailure(String err) {
                loaded[1] = true;

                if (loaded[0]) runOnUiThread(() -> displayAllVouchers());
            }
        });
    }

    private void displayAllVouchers() {
        llVoucherChips.removeAllViews();

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());

        int count = 0;

        for (Voucher v : systemVouchers) {
            if (count >= 8) break;
            if (!v.isActive() || v.isExpired(today) || v.isUsageLimitReached()) continue;

            addVoucherChip(v.getCode(), () -> {
                TextInputEditText etVoucher = findViewById(R.id.etVoucher);
                if (etVoucher != null) etVoucher.setText(v.getCode());
                applyVoucher();
            });

            count++;
        }

        for (Promotion p : promotionVouchers) {
            if (count >= 8) break;
            if (!p.isActive() || !p.isWithinDateRange()) continue;
            if (p.getVoucherCode() == null || p.getVoucherCode().isEmpty()) continue;
            if (p.getUsageLimit() > 0 && p.getUsageCount() >= p.getUsageLimit()) continue;

            addVoucherChip(p.getVoucherCode(), () -> {
                TextInputEditText etVoucher = findViewById(R.id.etVoucher);
                if (etVoucher != null) etVoucher.setText(p.getVoucherCode());
                applyVoucher();
            });

            count++;
        }

        if (count == 0) {
            tvChooseVoucher.setVisibility(View.GONE);
            llVoucherChips.setVisibility(View.GONE);
        } else {
            tvChooseVoucher.setVisibility(View.VISIBLE);
        }
    }

    private void addVoucherChip(String code, Runnable onClick) {
        TextView chip = new TextView(this);
        chip.setText(code);
        chip.setTextSize(13);
        chip.setPadding(dipToPx(12), dipToPx(8), dipToPx(12), dipToPx(8));
        chip.setTextColor(getResources().getColor(R.color.primary, null));
        chip.setBackgroundResource(R.drawable.bg_social_btn);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, dipToPx(8), dipToPx(8));
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> onClick.run());

        llVoucherChips.addView(chip);
    }

    private void toggleVoucherChips() {
        voucherChipsExpanded = !voucherChipsExpanded;
        tvChooseVoucher.setText(voucherChipsExpanded
                ? "Ẩn mã giảm giá ▲"
                : "Chọn mã giảm giá có sẵn ▼");

        llVoucherChips.setVisibility(voucherChipsExpanded ? View.VISIBLE : View.GONE);
    }

    private void removeVoucher() {
        appliedVouchers.clear();

        voucherDiscount = 0;
        selectedVoucherId = null;
        selectedVoucherCode = null;
        isPromotionVoucher = false;

        TextInputEditText etVoucher = findViewById(R.id.etVoucher);
        if (etVoucher != null) etVoucher.setText("");

        if (cvSelectedVoucher != null) cvSelectedVoucher.setVisibility(View.GONE);
        if (llDiscountRow != null) llDiscountRow.setVisibility(View.GONE);
        if (tvDiscount != null) tvDiscount.setText("-0đ");

        if (cart != null) updatePriceSummary(cart.calculateSubtotal());
    }

    private void placeOrder() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cart == null || cart.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        double subtotal = cart.calculateSubtotal();
        recalculateVoucherDiscount(subtotal);

        double total = subtotal + shippingFee - voucherDiscount;

        TextInputEditText etNote = findViewById(R.id.etNote);
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        Order order = new Order();
        order.setUserId(uid);
        order.setReceiverName(selectedAddress.getReceiverName());
        order.setReceiverPhone(selectedAddress.getReceiverPhone());
        order.setShippingAddress(selectedAddress.getAddressLine());
        order.setShippingWard(selectedAddress.getWard());
        order.setShippingDistrict(selectedAddress.getDistrict());
        order.setShippingCity(selectedAddress.getCity());
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setVoucherDiscount(voucherDiscount);
        order.setVoucherId(joinVoucherIds());
        order.setVoucherCode(joinVoucherCodes());
        order.setTotalAmount(Math.max(0, total));
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(Order.PAY_STATUS_PENDING);
        order.setNote(note);

        final List<AppliedVoucher> appliedVoucherSnapshot = new ArrayList<>(appliedVouchers);

        new OrderRepository().createOrder(order, cart.getItems(), new OrderRepository.Callback<>() {
            @Override
            public void onSuccess(String orderId) {
                new CartRepository().clearCart(uid, new CartRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                    }

                    @Override
                    public void onFailure(String e) {
                    }
                });

                recordVoucherUsageAfterOrder(uid, appliedVoucherSnapshot);
                sendOrderCreatedNotification(uid, orderId, order.getOrderCode());

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (Order.PAYMENT_VNPAY.equals(paymentMethod)) {
                        openVNPay(order.getOrderCode(), (long) total);
                    } else {
                        openOrderSuccess(orderId);
                    }
                });
            }

            @Override
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CheckoutActivity.this,
                            "Lỗi: " + err,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openVNPay(String orderCode, long amount) {
        String url = VNPayHelper.buildPaymentUrl(orderCode, amount, "Thanh toan don hang " + orderCode);

        Intent i = new Intent(this, VNPayWebViewActivity.class);
        i.putExtra(VNPayWebViewActivity.EXTRA_PAYMENT_URL, url);
        i.putExtra(VNPayWebViewActivity.EXTRA_ORDER_ID, orderCode);

        startActivity(i);
        finish();
    }

    private void openOrderSuccess(String orderId) {
        Intent i = new Intent(this, OrderDetailActivity.class);
        i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
        i.putExtra("from_checkout", true);

        startActivity(i);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String addrId = data.getStringExtra("selected_address_id");
            loadAddress(addrId);
        }
    }

    private boolean hasAppliedVoucher(String id, String code) {
        for (AppliedVoucher v : appliedVouchers) {
            if (id != null && id.equals(v.id)) return true;
            if (code != null && code.equalsIgnoreCase(v.code)) return true;
        }

        return false;
    }

    private void recalculateVoucherDiscount(double subtotal) {
        double totalDiscount = 0;

        for (AppliedVoucher v : appliedVouchers) {
            totalDiscount += v.discount;
        }

        voucherDiscount = Math.min(totalDiscount, subtotal);
        selectedVoucherId = joinVoucherIds();
        selectedVoucherCode = joinVoucherCodes();
        isPromotionVoucher = appliedVouchers.size() == 1 && appliedVouchers.get(0).promotionVoucher;

        refreshSelectedVoucherUi();
    }

    private String joinVoucherIds() {
        StringBuilder sb = new StringBuilder();

        for (AppliedVoucher v : appliedVouchers) {
            if (v.id == null || v.id.isEmpty()) continue;

            if (sb.length() > 0) sb.append(",");
            sb.append(v.id);
        }

        return sb.toString();
    }

    private String joinVoucherCodes() {
        StringBuilder sb = new StringBuilder();

        for (AppliedVoucher v : appliedVouchers) {
            if (v.code == null || v.code.isEmpty()) continue;

            if (sb.length() > 0) sb.append(", ");
            sb.append(v.code);
        }

        return sb.toString();
    }

    private void refreshSelectedVoucherUi() {
        if (cvSelectedVoucher == null || tvSelectedVoucher == null) return;

        if (appliedVouchers.isEmpty()) {
            cvSelectedVoucher.setVisibility(View.GONE);
        } else {
            cvSelectedVoucher.setVisibility(View.VISIBLE);
            tvSelectedVoucher.setText("Đã áp dụng: " + joinVoucherCodes());
        }
    }

    private void recordVoucherUsageAfterOrder(String userId, List<AppliedVoucher> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) return;

        for (AppliedVoucher applied : vouchers) {
            if (applied == null || applied.id == null || applied.id.isEmpty()) continue;

            if (applied.promotionVoucher) {
                new PromotionRepository().recordPromotionUsage(userId, applied.id, new PromotionRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        android.util.Log.d("Checkout", "Promotion voucher usage recorded: " + applied.id);
                    }

                    @Override
                    public void onFailure(String error) {
                        android.util.Log.e("Checkout", "Failed to record promotion usage: " + error);
                    }
                });
            } else {
                new VoucherRepository().recordVoucherUsage(userId, applied.id, new VoucherRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        android.util.Log.d("Checkout", "Voucher usage recorded: " + applied.id);
                    }

                    @Override
                    public void onFailure(String error) {
                        android.util.Log.e("Checkout", "Failed to record voucher usage: " + error);
                    }
                });
            }
        }
    }

    private void sendOrderCreatedNotification(String userId, String orderId, String orderCode) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setTitle("Đặt hàng thành công");
        notif.setMessage("Đơn hàng " + orderCode + " đã được tạo và đang chờ xác nhận.");
        notif.setType("ORDER");
        notif.setOrderId(orderId);

        new NotificationRepository().createNotificationAsync(notif);
    }

    private int dipToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}