package com.example.petshop.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.model.entity.Order;
import com.example.petshop.repository.OrderRepository;
import com.example.petshop.utils.VNPayHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VNPayResultActivity extends AppCompatActivity {

    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_result);

        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            finish();
            return;
        }

        // Parse VNPay response parameters
        Map<String, String> params = new HashMap<>();
        Set<String> queryNames = data.getQueryParameterNames();
        for (String name : queryNames) {
            params.put(name, data.getQueryParameter(name));
        }

        String responseCode = params.get("vnp_ResponseCode");
        orderId = params.get("vnp_TxnRef");

        boolean isSuccess = VNPayHelper.isSuccess(responseCode);
        
        updateUI(isSuccess);

        if (isSuccess && orderId != null) {
            updateOrderStatusToPaid(orderId);
        }
    }

    private void updateUI(boolean isSuccess) {
        ImageView ivIcon = findViewById(R.id.ivResultIcon);
        TextView tvTitle = findViewById(R.id.tvResultTitle);
        TextView tvMsg   = findViewById(R.id.tvResultMsg);
        Button btnDetail = findViewById(R.id.btnViewOrder);

        if (isSuccess) {
            ivIcon.setImageResource(R.drawable.ic_check_circle);
            ivIcon.setColorFilter(getColor(R.color.status_success));
            tvTitle.setText("Thanh toán thành công!");
            tvMsg.setText("Cảm ơn bạn đã tin tưởng PetShop. Đơn hàng đang được xử lý.");
        } else {
            ivIcon.setImageResource(R.drawable.ic_error);
            ivIcon.setColorFilter(getColor(R.color.status_error));
            tvTitle.setText("Thanh toán thất bại");
            tvMsg.setText("Đã có lỗi xảy ra hoặc bạn đã hủy giao dịch. Vui lòng thử lại.");
        }

        btnDetail.setOnClickListener(v -> {
            if (orderId != null) {
                Intent i = new Intent(this, OrderDetailActivity.class);
                i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
                startActivity(i);
            }
            finish();
        });

        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent i = new Intent(this, PetShopActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }

    private void updateOrderStatusToPaid(String orderCode) {
        OrderRepository repo = new OrderRepository();
        repo.getOrderByCode(orderCode, new OrderRepository.Callback<Order>() {
            @Override
            public void onSuccess(Order order) {
                // Sử dụng hàm completeVNPayOrder để trừ kho và cập nhật trạng thái đồng thời
                repo.completeVNPayOrder(order.getId(), new OrderRepository.Callback<Void>() {
                    @Override public void onSuccess(Void data) {
                        Toast.makeText(VNPayResultActivity.this, "Thanh toán thành công & Đã trừ kho", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(String error) {
                        Toast.makeText(VNPayResultActivity.this, "Lỗi hoàn tất đơn hàng: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onFailure(String error) {
                Toast.makeText(VNPayResultActivity.this, "Không tìm thấy đơn hàng: " + orderCode, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
