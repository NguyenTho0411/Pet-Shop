package com.example.petshop.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Notification;
import com.example.petshop.repository.NotificationRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.adapter.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private View progressBar;
    private NotificationAdapter adapter;
    private NotificationRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        initViews();
        loadNotifications();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new NotificationAdapter(new ArrayList<>());
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        repo = new NotificationRepository();
    }

    private void loadNotifications() {
        String uid = FirebaseHelper.getCurrentUser() != null ? FirebaseHelper.getCurrentUser().getUid() : null;
        if (uid == null) {
            showEmpty("Vui lòng đăng nhập để xem thông báo");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        repo.getNotifications(uid, new NotificationRepository.Callback<>() {
            @Override
            public void onSuccess(List<Notification> list) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (list == null || list.isEmpty()) {
                        createTestNotification(uid);
                    } else {
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.updateData(list);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showEmpty("Không thể tải thông báo");
                });
            }
        });
    }

    private void showEmpty(String message) {
        rvNotifications.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    private void createTestNotification(String uid) {
        Notification test = new Notification();
        test.setUserId(uid);
        test.setTitle("Chào mừng đến với Pet Shop 🐾");
        test.setMessage("Đây là thông báo thử nghiệm. Cảm ơn bạn đã sử dụng ứng dụng!");
        test.setType("SYSTEM");
        
        repo.createNotification(test, new NotificationRepository.Callback<String>() {
            @Override
            public void onSuccess(String data) {
                // Tải lại sau khi tạo
                runOnUiThread(() -> loadNotifications());
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showEmpty("Chưa có thông báo nào"));
            }
        });
    }
}
