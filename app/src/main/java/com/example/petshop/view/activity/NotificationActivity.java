package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.AppNotification;
import com.example.petshop.repository.NotificationRepository;
import com.example.petshop.view.adapter.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private LinearLayout llEmpty;
    private NotificationAdapter adapter;
    private NotificationRepository repo;
    private List<AppNotification> currentList = new ArrayList<>();
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        repo = new NotificationRepository();

        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        llEmpty = findViewById(R.id.llEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new NotificationAdapter(notification -> {
            if (AppNotification.TYPE_ORDER.equals(notification.getType())
                    && notification.getOrderId() != null) {
                Intent i = new Intent(this, OrderDetailActivity.class);
                i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, notification.getOrderId());
                startActivity(i);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        llEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);

        repo.getNotifications(this, uid, new NotificationRepository.Callback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> data) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    currentList = data != null ? data : new ArrayList<>();

                    if (currentList.isEmpty()) {
                        llEmpty.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        llEmpty.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.updateList(currentList);

                        // Vào màn thông báo thì xem như đã đọc hết
                        repo.markAllAsRead(NotificationActivity.this, uid, currentList);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    llEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(NotificationActivity.this,
                            "Không tải được thông báo: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}