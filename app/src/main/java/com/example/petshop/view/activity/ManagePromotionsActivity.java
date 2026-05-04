package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.view.adapter.PromotionAdminAdapter;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.example.petshop.viewmodel.PromotionManageViewModel;

import java.util.ArrayList;
import java.util.List;

public class ManagePromotionsActivity extends AppCompatActivity {

    private PromotionManageViewModel vm;
    private PromotionAdminAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_promotions);

        vm = new ViewModelProvider(this).get(PromotionManageViewModel.class);

        initViews();
        observeViewModel();
        vm.loadAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.loadAll();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);

        RecyclerView rv = findViewById(R.id.rvPromotions);
        adapter = new PromotionAdminAdapter(new ArrayList<>(), new PromotionAdminAdapter.OnActionListener() {
            public void onEdit(Promotion promo) {
                Intent intent = new Intent(ManagePromotionsActivity.this, AddEditPromotionActivity.class);
                intent.putExtra("promoId", promo.getId());
                startActivity(intent);
            }
            public void onDelete(Promotion promo) {
                confirmDeletePromotion(promo.getName(), promo.getId());
            }
            public void onToggle(Promotion promo, boolean isActive) {
                vm.toggleActive(promo.getId(), isActive);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditPromotionActivity.class));
        });
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getPromotions().observe(this, promos -> {
            adapter.updateList(promos != null ? promos : new ArrayList<>());
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void confirmDeletePromotion(String promotionName, String promotionId) {
        DialogUtils.showDeleteConfirmDialog(this, promotionName,
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    vm.delete(promotionId);
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
