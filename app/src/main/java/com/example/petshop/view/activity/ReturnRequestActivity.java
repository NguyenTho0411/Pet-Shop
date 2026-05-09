package com.example.petshop.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.ReturnRequest;
import com.example.petshop.repository.OrderRepository;
import com.example.petshop.repository.ReturnRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ReturnRequestActivity extends AppCompatActivity {

    private static final double PET_REFUND_PERCENT  = 0.70;
    private static final double FOOD_REFUND_PERCENT = 1.00;

    private Order currentOrder;
    private LinearLayout llBankInfo;
    private TextInputLayout tilBankName, tilBankAccount;
    private TextInputEditText etReturnReason, etBankName, etBankAccount;
    private ProgressBar progressBar;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_request);

        String orderId = getIntent().getStringExtra("order_id");
        boolean hasPet = getIntent().getBooleanExtra("has_pet", false);
        if (orderId == null) { finish(); return; }

        bindViews();
        setupPolicyInfo(hasPet);

        loadOrder(orderId, hasPet);
    }

    private void bindViews() {
        etReturnReason = findViewById(R.id.etReturnReason);
        etBankName     = findViewById(R.id.etBankName);
        etBankAccount  = findViewById(R.id.etBankAccount);
        tilBankName    = findViewById(R.id.tilBankName);
        tilBankAccount = findViewById(R.id.tilBankAccount);
        llBankInfo     = findViewById(R.id.llBankInfo);
        progressBar    = new ProgressBar(this); // not in layout, handled via button state
        btnSubmit      = findViewById(R.id.btnSubmitReturn);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void loadOrder(String orderId, boolean hasPet) {
        btnSubmit.setEnabled(false);
        new OrderRepository().getOrderById(orderId, new OrderRepository.Callback<Order>() {
            public void onSuccess(Order order) {
                currentOrder = order;
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    // Hiển thị thông tin ngân hàng nếu thanh toán COD
                    if (order != null && Order.PAYMENT_COD.equals(order.getPaymentMethod())) {
                        llBankInfo.setVisibility(View.VISIBLE);
                    }
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(ReturnRequestActivity.this,
                            "Không thể tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void validateAndSubmit() {
        String reason = etReturnReason.getText() != null
                ? etReturnReason.getText().toString().trim() : "";
        if (reason.isEmpty()) {
            etReturnReason.setError("Vui lòng nhập lý do trả hàng");
            return;
        }

        // Kiểm tra thông tin ngân hàng nếu COD
        String bankName = "", bankAccount = "";
        if (currentOrder != null && Order.PAYMENT_COD.equals(currentOrder.getPaymentMethod())) {
            bankName    = etBankName.getText() != null ? etBankName.getText().toString().trim() : "";
            bankAccount = etBankAccount.getText() != null ? etBankAccount.getText().toString().trim() : "";
            if (bankName.isEmpty()) {
                tilBankName.setError("Vui lòng nhập tên ngân hàng");
                return;
            }
            if (bankAccount.isEmpty()) {
                tilBankAccount.setError("Vui lòng nhập số tài khoản");
                return;
            }
            tilBankName.setError(null);
            tilBankAccount.setError(null);
        }

        submitReturn(reason, bankName, bankAccount);
    }

    private void submitReturn(String reason, String bankName, String bankAccount) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        ReturnRequest req = new ReturnRequest();
        req.setOrderId(currentOrder.getId());
        req.setOrderCode(currentOrder.getOrderCode());
        req.setReason(reason);
        req.setPaymentMethod(currentOrder.getPaymentMethod());
        req.setRefundAmount(currentOrder.getTotalAmount());

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        req.setUserId(uid);
        req.setCustomerName(currentOrder.getReceiverName());

        if (Order.PAYMENT_COD.equals(currentOrder.getPaymentMethod())) {
            req.setBankName(bankName);
            req.setBankAccount(bankAccount);
        }

        new ReturnRepository().create(req, new ReturnRepository.Callback<String>() {
            public void onSuccess(String id) {
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    Toast.makeText(ReturnRequestActivity.this,
                            "Yêu cầu trả hàng đã được gửi. Chúng tôi sẽ liên hệ trong 24h.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Gửi yêu cầu trả hàng");
                    Toast.makeText(ReturnRequestActivity.this,
                            "Lỗi: " + err, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupPolicyInfo(boolean hasPet) {
        TextView tvPetPolicy  = findViewById(R.id.tvPetPolicy);
        TextView tvFoodPolicy = findViewById(R.id.tvFoodPolicy);
        LinearLayout llPet    = findViewById(R.id.llPetPolicy);
        LinearLayout llFood   = findViewById(R.id.llFoodPolicy);

        if (hasPet) {
            llPet.setVisibility(View.VISIBLE);
            tvPetPolicy.setText(
                    "🐾 Chính sách hoàn tiền thú cưng:\n"
                    + "• Hoàn " + (int)(PET_REFUND_PERCENT * 100) + "% giá trị thú cưng\n"
                    + "• Áp dụng trong 3 ngày sau khi nhận hàng\n"
                    + "• Thú cưng phải trong tình trạng khoẻ mạnh\n"
                    + "• Mất " + (int)((1 - PET_REFUND_PERCENT) * 100) + "% phí xử lý");
        } else {
            llPet.setVisibility(View.GONE);
        }

        llFood.setVisibility(View.VISIBLE);
        tvFoodPolicy.setText(
                "🍖 Chính sách hoàn tiền đồ ăn:\n"
                + "• Hoàn 100% nếu sản phẩm CHƯA mở bao bì\n"
                + "• Hoàn 50% nếu đã mở nhưng chưa sử dụng\n"
                + "• Không hoàn nếu đã sử dụng\n"
                + "• Áp dụng trong 7 ngày sau khi nhận");
    }
}
