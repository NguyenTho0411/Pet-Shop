package com.example.petshop.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Address;
import com.example.petshop.model.entity.Cart;
import com.example.petshop.model.entity.CartItem;
import com.example.petshop.model.entity.Order;
import com.example.petshop.repository.AddressRepository;
import com.example.petshop.repository.OrderRepository;
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
    private Cart          cart;
    private Address       selectedAddress;
    private double        shippingFee = 0;
    private String        etaDays     = "";
    private String        paymentMethod = Order.PAYMENT_COD;

    private TextView tvReceiverName, tvReceiverPhone, tvAddressDetail;
    private TextView tvSubtotal, tvShipping, tvTotal, tvShipEta, tvDiscount;
    private LinearLayout llDiscountRow;
    private RadioButton  rbCod, rbVnpay;
    private ProgressBar  progressBar;
    private double       voucherDiscount = 0;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

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
                updatePriceSummary(c.calculateSubtotal());
                if (selectedAddress != null) calculateShipping();
            }
        });
        vm.loadCart();
    }

    private void initViews() {
        progressBar      = findViewById(R.id.progressBar);
        tvReceiverName   = findViewById(R.id.tvReceiverName);
        tvReceiverPhone  = findViewById(R.id.tvReceiverPhone);
        tvAddressDetail  = findViewById(R.id.tvAddressDetail);
        tvSubtotal       = findViewById(R.id.tvSummarySubtotal);
        tvShipping       = findViewById(R.id.tvSummaryShipping);
        tvTotal          = findViewById(R.id.tvSummaryTotal);
        tvShipEta        = findViewById(R.id.tvShipEta);
        tvDiscount       = findViewById(R.id.tvSummaryDiscount);
        llDiscountRow    = findViewById(R.id.llDiscountRow);
        rbCod            = findViewById(R.id.rbCod);
        rbVnpay          = findViewById(R.id.rbVnpay);

        // Payment method selection
        LinearLayout llCod   = findViewById(R.id.llPayCod);
        LinearLayout llVnpay = findViewById(R.id.llPayVnpay);
        llCod.setOnClickListener(v -> selectPayment(Order.PAYMENT_COD));
        rbCod.setOnClickListener(v -> selectPayment(Order.PAYMENT_COD));
        llVnpay.setOnClickListener(v -> selectPayment(Order.PAYMENT_VNPAY));
        rbVnpay.setOnClickListener(v -> selectPayment(Order.PAYMENT_VNPAY));

        // Voucher
        ((Button) findViewById(R.id.btnApplyVoucher)).setOnClickListener(v -> applyVoucher());

        // Change address
        findViewById(R.id.btnChangeAddress).setOnClickListener(v -> {
            Intent i = new Intent(this, ManageAddressActivity.class);
            i.putExtra("pick_mode", true);
            startActivityForResult(i, 100);
        });

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
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        new AddressRepository().getAddresses(uid, new AddressRepository.Callback<>() {
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
                    toSet = list.stream().filter(a -> specificId.equals(a.getId())).findFirst().orElse(null);
                }
                if (toSet == null) {
                    toSet = list.stream().filter(Address::isDefault).findFirst().orElse(list.get(0));
                }
                final Address finalToSet = toSet;
                runOnUiThread(() -> setAddress(finalToSet));
            }
            public void onFailure(String err) {}
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
                public void onResult(double fee, String eta) {
                    shippingFee = fee;
                    etaDays     = eta;
                    runOnUiThread(() -> updatePriceSummary(cart.calculateSubtotal()));
                }
                public void onError(String e) { /* use default */ }
            });
        }
    }

    private void renderOrderItems(List<CartItem> items) {
        RecyclerView rv = findViewById(R.id.rvOrderItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        CartItemAdapter a = new CartItemAdapter(new ArrayList<>(items),
                new CartItemAdapter.OnCartAction() {
                    public void onRemove(CartItem item) { /* read-only in checkout */ }
                    public void onQtyChange(CartItem item, int qty) {}
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

        if (!etaDays.isEmpty()) tvShipEta.setText("⏱ Dự kiến giao: " + etaDays);
        if (voucherDiscount > 0) {
            llDiscountRow.setVisibility(View.VISIBLE);
            tvDiscount.setText("-" + VND.format((long) voucherDiscount) + "đ");
        }
    }

    private void applyVoucher() {
        TextInputEditText etVoucher = findViewById(R.id.etVoucher);
        String code = etVoucher.getText() != null ? etVoucher.getText().toString().trim() : "";
        if (code.isEmpty()) { Toast.makeText(this, "Nhập mã voucher", Toast.LENGTH_SHORT).show(); return; }
        // TODO: call VoucherRepository to validate
        Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
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
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { startActivity(new Intent(this, LoginActivity.class)); return; }

        progressBar.setVisibility(View.VISIBLE);

        double subtotal = cart.calculateSubtotal();
        double total    = subtotal + shippingFee - voucherDiscount;

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
        order.setTotalAmount(Math.max(0, total));
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(Order.PAY_STATUS_PENDING);
        order.setNote(note);

        new OrderRepository().createOrder(order, cart.getItems(), new OrderRepository.Callback<>() {
            public void onSuccess(String orderId) {
                vm.clearCart();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (Order.PAYMENT_VNPAY.equals(paymentMethod)) {
                        openVNPay(orderId, (long) total);
                    } else {
                        openOrderSuccess(orderId);
                    }
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CheckoutActivity.this, "Lỗi: " + err, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openVNPay(String orderId, long amount) {
        String url = VNPayHelper.buildPaymentUrl(orderId, amount, "Thanh toan don hang " + orderId);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
        openOrderSuccess(orderId);
    }

    private void openOrderSuccess(String orderId) {
        Intent i = new Intent(this, OrderDetailActivity.class);
        i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Address picked from ManageAddressActivity
            String addrId = data.getStringExtra("selected_address_id");
            loadAddress(addrId);
        }
    }
}
