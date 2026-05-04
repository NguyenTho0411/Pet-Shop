package com.example.petshop.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.repository.PromotionRepository;
import com.example.petshop.view.adapter.PromotionAdapter;

import java.util.ArrayList;
import java.util.List;

public class PromotionActivity extends AppCompatActivity {

    private RecyclerView rvPromotions;
    private TextView tvEmpty;
    private View progressBar;
    private PromotionAdapter adapter;
    private PromotionRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion);

        initViews();
        loadPromotions();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        rvPromotions = findViewById(R.id.rvPromotions);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new PromotionAdapter(new ArrayList<>());
        rvPromotions.setLayoutManager(new LinearLayoutManager(this));
        rvPromotions.setAdapter(adapter);

        repo = new PromotionRepository();
    }

    private void loadPromotions() {
        progressBar.setVisibility(View.VISIBLE);
        rvPromotions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        repo.getActive(new PromotionRepository.Callback<List<Promotion>>() {
            @Override
            public void onSuccess(List<Promotion> list) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (list == null || list.isEmpty()) {
                        // Load all promotions if no active ones
                        repo.getAll(new PromotionRepository.Callback<List<Promotion>>() {
                            @Override
                            public void onSuccess(List<Promotion> allList) {
                                runOnUiThread(() -> displayPromotions(allList));
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> showEmpty("Không thể tải khuyến mãi"));
                            }
                        });
                    } else {
                        displayPromotions(list);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showEmpty("Không thể tải khuyến mãi: " + error);
                });
            }
        });
    }

    private void displayPromotions(List<Promotion> list) {
        if (list == null || list.isEmpty()) {
            showEmpty("Chưa có khuyến mãi nào");
        } else {
            rvPromotions.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter.updateData(list);
        }
    }

    private void showEmpty(String message) {
        rvPromotions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }
}
