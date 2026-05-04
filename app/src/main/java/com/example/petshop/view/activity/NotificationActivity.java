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
            android.util.Log.w("NotificationActivity", "loadNotifications: uid is null");
            showEmpty("Vui lòng đăng nhập để xem thông báo");
            return;
        }

        android.util.Log.d("NotificationActivity", "loadNotifications: loading for uid=" + uid);
        progressBar.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        repo.getNotifications(uid, new NotificationRepository.Callback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> list) {
                android.util.Log.d("NotificationActivity", "onSuccess: received " + (list != null ? list.size() : "null") + " notifications");
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (list == null || list.isEmpty()) {
                        android.util.Log.w("NotificationActivity", "onSuccess: list is empty");
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.updateData(new ArrayList<>());
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Chưa có thông báo nào");
                    } else {
                        android.util.Log.d("NotificationActivity", "onSuccess: showing " + list.size() + " notifications");
                        rvNotifications.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        adapter.updateData(list);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("NotificationActivity", "onFailure: " + error);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showEmpty("Không thể tải thông báo: " + error);
                });
            }
        });
    }

    private void showEmpty(String message) {
        rvNotifications.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }
}
