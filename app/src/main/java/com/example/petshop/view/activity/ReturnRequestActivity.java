package com.example.petshop.view.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.repository.OrderRepository;
import com.google.android.material.textfield.TextInputEditText;

public class ReturnRequestActivity extends AppCompatActivity {

    // Return policy constants
    private static final double PET_REFUND_PERCENT  = 0.70;  // 70% hoàn tiền cho pet
    private static final double FOOD_REFUND_PERCENT = 1.00;  // 100% nếu chưa dùng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_request);

        String  orderId = getIntent().getStringExtra("order_id");
        boolean hasPet  = getIntent().getBooleanExtra("has_pet", false);

        if (orderId == null) { finish(); return; }

        setupPolicyInfo(hasPet);

        ((Button) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        ((Button) findViewById(R.id.btnSubmitReturn)).setOnClickListener(v -> {
            TextInputEditText etReason = findViewById(R.id.etReturnReason);
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
            if (reason.isEmpty()) {
                etReason.setError("Vui lòng nhập lý do trả hàng");
                return;
            }
            submitReturn(orderId, reason, hasPet);
        });
    }

    private void setupPolicyInfo(boolean hasPet) {
        TextView tvPetPolicy  = findViewById(R.id.tvPetPolicy);
        TextView tvFoodPolicy = findViewById(R.id.tvFoodPolicy);
        LinearLayout llPet    = findViewById(R.id.llPetPolicy);
        LinearLayout llFood   = findViewById(R.id.llFoodPolicy);

        if (hasPet) {
            llPet.setVisibility(android.view.View.VISIBLE);
            tvPetPolicy.setText(
                    "🐾 Chính sách hoàn tiền thú cưng:\n"
                    + "• Hoàn " + (int)(PET_REFUND_PERCENT * 100) + "% giá trị thú cưng\n"
                    + "• Áp dụng trong 3 ngày sau khi nhận hàng\n"
                    + "• Thú cưng phải trong tình trạng khoẻ mạnh\n"
                    + "• Mất " + (int)((1 - PET_REFUND_PERCENT) * 100) + "% phí xử lý");
        } else {
            llPet.setVisibility(android.view.View.GONE);
        }

        llFood.setVisibility(android.view.View.VISIBLE);
        tvFoodPolicy.setText(
                "🍖 Chính sách hoàn tiền đồ ăn:\n"
                + "• Hoàn 100% nếu sản phẩm CHƯA mở bao bì\n"
                + "• Hoàn 50% nếu đã mở nhưng chưa sử dụng\n"
                + "• Không hoàn nếu đã sử dụng\n"
                + "• Áp dụng trong 7 ngày sau khi nhận");
    }

    private void submitReturn(String orderId, String reason, boolean hasPet) {
        new OrderRepository().requestReturn(orderId, reason, hasPet, new OrderRepository.Callback<>() {
            public void onSuccess(Void v) {
                runOnUiThread(() -> {
                    Toast.makeText(ReturnRequestActivity.this,
                            "Yêu cầu trả hàng đã được gửi. Chúng tôi sẽ liên hệ trong 24h.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> Toast.makeText(ReturnRequestActivity.this,
                        "Lỗi: " + err, Toast.LENGTH_LONG).show());
            }
        });
    }
}
