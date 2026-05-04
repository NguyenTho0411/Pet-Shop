package com.example.petshop.view.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Address;
import com.example.petshop.repository.AddressRepository;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ManageAddressActivity extends AppCompatActivity {

    private final AddressRepository repo = new AddressRepository();
    private List<Address> addresses = new ArrayList<>();
    private RecyclerView  rv;
    private boolean       pickMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_address);

        pickMode = getIntent().getBooleanExtra("pick_mode", false);
        rv = findViewById(R.id.rvAddresses);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ((FloatingActionButton) findViewById(R.id.fabAdd)).setOnClickListener(v -> showDialog(null));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadAddresses();
    }

    private String uid() {
        var u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    private void loadAddresses() {
        String uid = uid(); if (uid == null) return;
        repo.getAddresses(uid, new AddressRepository.Callback<>() {
            public void onSuccess(List<Address> list) {
                addresses = list != null ? list : new ArrayList<>();
                runOnUiThread(() -> renderList());
            }
            public void onFailure(String err) { runOnUiThread(() -> Toast.makeText(ManageAddressActivity.this, err, Toast.LENGTH_SHORT).show()); }
        });
    }

    private void renderList() {
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_address, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                Address addr = addresses.get(pos);
                View v = h.itemView;
                ((TextView) v.findViewById(R.id.tvReceiverName)).setText(addr.getReceiverName());
                ((TextView) v.findViewById(R.id.tvPhone)).setText(addr.getReceiverPhone());
                ((TextView) v.findViewById(R.id.tvFullAddress)).setText(addr.getFullAddress());
                View defaultBadge = v.findViewById(R.id.tvDefaultBadge);
                defaultBadge.setVisibility(addr.isDefault() ? View.VISIBLE : View.GONE);

                v.setOnClickListener(x -> {
                    if (pickMode) {
                        Intent result = new Intent();
                        result.putExtra("selected_address_id", addr.getId());
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        showDialog(addr);
                    }
                });

                v.findViewById(R.id.btnEdit).setOnClickListener(x -> showDialog(addr));

                v.findViewById(R.id.btnSetDefault).setOnClickListener(x ->
                        repo.setDefault(uid(), addr.getId(), new AddressRepository.Callback<>() {
                            public void onSuccess(Void vv) { runOnUiThread(() -> loadAddresses()); }
                            public void onFailure(String err) {}
                        }));

                v.findViewById(R.id.btnDelete).setOnClickListener(x ->
                        confirmDeleteAddress(addr.getId()));
            }
            @Override public int getItemCount() { return addresses.size(); }
        });
    }

    private void showDialog(Address existing) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_address, null);
        Dialog dialog = new Dialog(this);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.94f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextInputEditText etName   = dialogView.findViewById(R.id.etReceiverName);
        TextInputEditText etPhone  = dialogView.findViewById(R.id.etReceiverPhone);
        TextInputEditText etLine   = dialogView.findViewById(R.id.etAddressLine);
        TextInputEditText etWard   = dialogView.findViewById(R.id.etWard);
        TextInputEditText etDist   = dialogView.findViewById(R.id.etDistrict);
        TextInputEditText etCity   = dialogView.findViewById(R.id.etCity);
        Button btnSave  = dialogView.findViewById(R.id.btnSave);
        Button btnCancel= dialogView.findViewById(R.id.btnCancel);

        if (existing != null) {
            etName.setText(existing.getReceiverName());
            etPhone.setText(existing.getReceiverPhone());
            etLine.setText(existing.getAddressLine());
            etWard.setText(existing.getWard());
            etDist.setText(existing.getDistrict());
            etCity.setText(existing.getCity());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = get(etName); if (TextUtils.isEmpty(name)) { etName.setError("Bắt buộc"); return; }
            String phone= get(etPhone); if (TextUtils.isEmpty(phone)) { etPhone.setError("Bắt buộc"); return; }
            String line = get(etLine);  if (TextUtils.isEmpty(line)) { etLine.setError("Bắt buộc"); return; }

            Address addr = existing != null ? existing : new Address();
            addr.setReceiverName(name);
            addr.setReceiverPhone(phone);
            addr.setAddressLine(line);
            addr.setWard(get(etWard));
            addr.setDistrict(get(etDist));
            addr.setCity(get(etCity));

            if (existing == null) {
                addr.setDefault(addresses.isEmpty());
                repo.addAddress(uid(), addr, new AddressRepository.Callback<>() {
                    public void onSuccess(String id)  { dialog.dismiss(); runOnUiThread(() -> loadAddresses()); }
                    public void onFailure(String err) { runOnUiThread(() -> Toast.makeText(ManageAddressActivity.this, err, Toast.LENGTH_SHORT).show()); }
                });
            } else {
                repo.updateAddress(uid(), addr, new AddressRepository.Callback<>() {
                    public void onSuccess(Void vv) { dialog.dismiss(); runOnUiThread(() -> loadAddresses()); }
                    public void onFailure(String err) {}
                });
            }
        });
        dialog.show();
    }

    private String get(TextInputEditText et) { return et.getText() != null ? et.getText().toString().trim() : ""; }

    private void confirmDeleteAddress(String addressId) {
        DialogUtils.showConfirmDialog(this, "Xoá địa chỉ này?",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    repo.deleteAddress(uid(), addressId, new AddressRepository.Callback<>() {
                        public void onSuccess(Void vv) { runOnUiThread(() -> loadAddresses()); }
                        public void onFailure(String err) {}
                    });
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
