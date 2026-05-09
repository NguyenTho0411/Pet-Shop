package com.example.petshop.view.activity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.ReturnRequest;
import com.example.petshop.repository.ReturnRepository;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminReturnListActivity extends AppCompatActivity {

    private final ReturnRepository repo = new ReturnRepository();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    private RecyclerView rvReturns;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ReturnAdapter adapter;
    private ChipGroup chipGroupFilters;

    private List<ReturnRequest> allReturns = new ArrayList<>();
    private String currentFilter = null; // null = all

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_return_list);

        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);
        rvReturns   = findViewById(R.id.rvReturns);

        rvReturns.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReturnAdapter();
        rvReturns.setAdapter(adapter);

        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupChips();
        loadReturns();
    }

    private void setupChips() {
        setupChip(R.id.chipAll,      null);
        setupChip(R.id.chipPending,  ReturnRequest.STATUS_PENDING);
        setupChip(R.id.chipApproved, ReturnRequest.STATUS_APPROVED);
        setupChip(R.id.chipRefunded, ReturnRequest.STATUS_REFUNDED);
        setupChip(R.id.chipRejected, ReturnRequest.STATUS_REJECTED);

        if (chipGroupFilters != null) {
            chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
                currentFilter = getFilterByChipId(checkedId);
                applyFilter();
                updateChipStyles(checkedId);
            });
            updateChipStyles(chipGroupFilters.getCheckedChipId());
        }
    }

    private void setupChip(int chipId, String filter) {
        Chip chip = findViewById(chipId);
        if (chip != null) {
            chip.setTag(filter);
        }
    }

    private String getFilterByChipId(int chipId) {
        if (chipId < 0) return null;
        Chip chip = findViewById(chipId);
        if (chip == null) return null;
        Object tag = chip.getTag();
        return tag instanceof String ? (String) tag : null;
    }

    private void updateChipStyles(int selectedId) {
        if (chipGroupFilters == null) return;
        for (int i = 0; i < chipGroupFilters.getChildCount(); i++) {
            View child = chipGroupFilters.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip) child;
            boolean selected = chip.getId() == selectedId;
            chip.setChipBackgroundColorResource(selected ? android.R.color.holo_orange_light : android.R.color.white);
            chip.setTextColor(selected ? getResources().getColor(android.R.color.white, null)
                    : getResources().getColor(android.R.color.black, null));
        }
    }

    private void loadReturns() {
        progressBar.setVisibility(View.VISIBLE);
        repo.getAll(new ReturnRepository.Callback<List<ReturnRequest>>() {
            public void onSuccess(List<ReturnRequest> data) {
                allReturns = data;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    applyFilter();
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminReturnListActivity.this, "Lỗi: " + err, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void applyFilter() {
        List<ReturnRequest> filtered = currentFilter == null ? allReturns
                : allReturns.stream()
                        .filter(r -> currentFilter.equals(r.getStatus()))
                        .collect(Collectors.toList());
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvReturns.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.update(filtered);
    }

    private void onApprove(ReturnRequest r) {
        DialogUtils.showConfirmDialog(this, "Duyệt yêu cầu hoàn trả", 
            "Xác nhận duyệt yêu cầu hoàn trả cho đơn " + r.getOrderCode() + "?",
            "Duyệt", "Huỷ",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    repo.approve(r.getId(), r.getOrderId(), new ReturnRepository.Callback<Void>() {
                        public void onSuccess(Void v) {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminReturnListActivity.this,
                                        "Đã duyệt yêu cầu hoàn trả", Toast.LENGTH_SHORT).show();
                                loadReturns();
                            });
                        }
                        public void onFailure(String err) {
                            runOnUiThread(() -> Toast.makeText(AdminReturnListActivity.this,
                                    "Lỗi: " + err, Toast.LENGTH_LONG).show());
                        }
                    });
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            }, "approveReturnDialog");
    }

    private void onReject(ReturnRequest r) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_note, null);
        TextInputEditText etNote = dialogView != null ? dialogView.findViewById(R.id.etNote) : null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Từ chối yêu cầu hoàn trả")
                .setMessage("Lý do từ chối (tuỳ chọn):");

        if (etNote != null) builder.setView(dialogView);

        builder.setPositiveButton("Từ chối", (d, w) -> {
                    String note = etNote != null && etNote.getText() != null
                            ? etNote.getText().toString().trim() : "";
                    repo.reject(r.getId(), r.getOrderId(), note, new ReturnRepository.Callback<Void>() {
                        public void onSuccess(Void v) {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminReturnListActivity.this,
                                        "Đã từ chối yêu cầu", Toast.LENGTH_SHORT).show();
                                loadReturns();
                            });
                        }
                        public void onFailure(String err) {
                            runOnUiThread(() -> Toast.makeText(AdminReturnListActivity.this,
                                    "Lỗi: " + err, Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void onMarkRefunded(ReturnRequest r) {
        DialogUtils.showConfirmDialog(this, "Xác nhận đã hoàn tiền", 
            "Xác nhận đã chuyển tiền hoàn trả cho đơn " + r.getOrderCode() + "?",
            "Đã hoàn tiền", "Huỷ",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    repo.markRefunded(r.getId(), r.getOrderId(), new ReturnRepository.Callback<Void>() {
                        public void onSuccess(Void v) {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminReturnListActivity.this,
                                        "Đã cập nhật trạng thái hoàn tiền", Toast.LENGTH_SHORT).show();
                                loadReturns();
                            });
                        }
                        public void onFailure(String err) {
                            runOnUiThread(() -> Toast.makeText(AdminReturnListActivity.this,
                                    "Lỗi: " + err, Toast.LENGTH_LONG).show());
                        }
                    });
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            }, "markRefundedDialog");
    }

    private void onDeleteReturn(ReturnRequest r) {
        DialogUtils.showConfirmDialog(this, "Xóa yêu cầu hoàn trả",
                "Bạn có chắc muốn xóa yêu cầu hoàn trả của đơn " + r.getOrderCode() + "?",
                "Xóa", "Huỷ",
                new ConfirmDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        repo.delete(r.getId(), new ReturnRepository.Callback<Void>() {
                            public void onSuccess(Void v) {
                                runOnUiThread(() -> {
                                    Toast.makeText(AdminReturnListActivity.this,
                                            "Đã xóa yêu cầu hoàn trả", Toast.LENGTH_SHORT).show();
                                    loadReturns();
                                });
                            }
                            public void onFailure(String err) {
                                runOnUiThread(() -> Toast.makeText(AdminReturnListActivity.this,
                                        "Lỗi: " + err, Toast.LENGTH_LONG).show());
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        // Không làm gì
                    }
                }, "deleteReturnDialog");
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    class ReturnAdapter extends RecyclerView.Adapter<ReturnAdapter.VH> {

        private final List<ReturnRequest> list = new ArrayList<>();

        void update(List<ReturnRequest> data) {
            list.clear();
            list.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_return_request, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            ReturnRequest r = list.get(pos);

            h.tvOrderCode.setText("Đơn: " + r.getOrderCode());
            h.tvCustomerName.setText("👤 " + r.getCustomerName());
            h.tvReason.setText("Lý do: " + r.getReason());
            h.tvRefundAmount.setText(VND.format((long) r.getRefundAmount()) + "đ");
            h.tvCreatedAt.setText(r.getCreatedAt() != null
                    ? r.getCreatedAt().substring(0, Math.min(10, r.getCreatedAt().length())) : "");

            // Status badge
            bindStatus(h.tvStatus, r.getStatus());

            // Bank info (COD)
            if (r.isCod() && r.getBankAccount() != null && !r.getBankAccount().isEmpty()) {
                h.llBankInfo.setVisibility(View.VISIBLE);
                h.tvBankName.setText("🏦 Ngân hàng: " + r.getBankName());
                h.tvBankAccount.setText("STK: " + r.getBankAccount());
            } else {
                h.llBankInfo.setVisibility(View.GONE);
            }

            // Buttons
            h.btnApprove.setVisibility(View.GONE);
            h.btnReject.setVisibility(View.GONE);
            h.btnMarkRefunded.setVisibility(View.GONE);
            h.btnDeleteRequest.setVisibility(View.GONE);

            if (r.isPending()) {
                h.btnApprove.setVisibility(View.VISIBLE);
                h.btnReject.setVisibility(View.VISIBLE);
                h.btnApprove.setOnClickListener(v -> onApprove(r));
                h.btnReject.setOnClickListener(v -> onReject(r));
            } else if (r.isApproved()) {
                h.btnMarkRefunded.setVisibility(View.VISIBLE);
                h.btnMarkRefunded.setOnClickListener(v -> onMarkRefunded(r));
            } else if (r.isRefunded() || r.isRejected()) {
                h.btnDeleteRequest.setVisibility(View.VISIBLE);
                h.btnDeleteRequest.setOnClickListener(v -> onDeleteReturn(r));
            }
        }

        private void bindStatus(TextView tv, String status) {
            if (status == null) return;
            switch (status) {
                case ReturnRequest.STATUS_PENDING:
                    tv.setText("Chờ duyệt");
                    tv.getBackground().setTint(Color.parseColor("#FF9800"));
                    break;
                case ReturnRequest.STATUS_APPROVED:
                    tv.setText("Đã duyệt");
                    tv.getBackground().setTint(Color.parseColor("#2196F3"));
                    break;
                case ReturnRequest.STATUS_REFUNDED:
                    tv.setText("Đã hoàn tiền");
                    tv.getBackground().setTint(Color.parseColor("#4CAF50"));
                    break;
                case ReturnRequest.STATUS_REJECTED:
                    tv.setText("Từ chối");
                    tv.getBackground().setTint(Color.parseColor("#F44336"));
                    break;
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvOrderCode, tvCustomerName, tvReason, tvStatus,
                     tvBankName, tvBankAccount, tvRefundAmount, tvCreatedAt;
            View llBankInfo;
            Button btnApprove, btnReject, btnMarkRefunded, btnDeleteRequest;

            VH(View v) {
                super(v);
                tvOrderCode    = v.findViewById(R.id.tvOrderCode);
                tvCustomerName = v.findViewById(R.id.tvCustomerName);
                tvReason       = v.findViewById(R.id.tvReason);
                tvStatus       = v.findViewById(R.id.tvStatus);
                tvBankName     = v.findViewById(R.id.tvBankName);
                tvBankAccount  = v.findViewById(R.id.tvBankAccount);
                tvRefundAmount = v.findViewById(R.id.tvRefundAmount);
                tvCreatedAt    = v.findViewById(R.id.tvCreatedAt);
                llBankInfo     = v.findViewById(R.id.llBankInfo);
                btnApprove     = v.findViewById(R.id.btnApprove);
                btnReject      = v.findViewById(R.id.btnReject);
                btnMarkRefunded= v.findViewById(R.id.btnMarkRefunded);
                btnDeleteRequest = v.findViewById(R.id.btnDeleteRequest);
            }
        }
    }
}
