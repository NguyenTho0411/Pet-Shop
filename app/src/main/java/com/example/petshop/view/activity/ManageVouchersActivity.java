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
import com.example.petshop.model.entity.Voucher;
import com.example.petshop.view.adapter.VoucherAdminAdapter;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.example.petshop.viewmodel.VoucherManageViewModel;

import java.util.ArrayList;
import java.util.List;

public class ManageVouchersActivity extends AppCompatActivity {

    private VoucherManageViewModel vm;
    private VoucherAdminAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_vouchers);

        vm = new ViewModelProvider(this).get(VoucherManageViewModel.class);

        initViews();
        observeViewModel();
        vm.loadAll();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);

        RecyclerView rv = findViewById(R.id.rvVouchers);
        adapter = new VoucherAdminAdapter(new ArrayList<>(), new VoucherAdminAdapter.OnActionListener() {
            public void onEdit(Voucher voucher) {
                Intent intent = new Intent(ManageVouchersActivity.this, AddEditVoucherActivity.class);
                intent.putExtra("voucherId", voucher.getId());
                startActivity(intent);
            }
            public void onDelete(Voucher voucher) {
                confirmDeleteVoucher(voucher.getCode(), voucher.getId());
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditVoucherActivity.class));
        });
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getVouchers().observe(this, vouchers -> {
            adapter.updateList(vouchers != null ? vouchers : new ArrayList<>());
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void confirmDeleteVoucher(String voucherCode, String voucherId) {
        DialogUtils.showDeleteConfirmDialog(this, voucherCode,
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    vm.delete(voucherId);
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
