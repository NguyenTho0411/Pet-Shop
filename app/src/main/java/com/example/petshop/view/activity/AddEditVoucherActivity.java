package com.example.petshop.view.activity;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;
import com.example.petshop.model.entity.Voucher;
import com.example.petshop.viewmodel.VoucherManageViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class AddEditVoucherActivity extends AppCompatActivity {

    public static final String EXTRA_VOUCHER_ID = "voucherId";

    private VoucherManageViewModel vm;
    private Voucher currentVoucher;

    private TextInputEditText etCode, etName, etValue, etMaxDiscount, etMinOrder;
    private TextInputEditText etStartDate, etEndDate, etPerUserLimit, etUsageLimit;
    private MaterialButton btnTypePercent, btnTypeFixed, btnTypeShip;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private String selectedType = Voucher.TYPE_PERCENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_voucher);

        vm = new ViewModelProvider(this).get(VoucherManageViewModel.class);

        initViews();
        observeViewModel();

        String voucherId = getIntent().getStringExtra(EXTRA_VOUCHER_ID);
        if (voucherId != null && !voucherId.isEmpty()) {
            tvTitle.setText("Sửa voucher");
            vm.loadById(voucherId); 
        }
    }

    private void initViews() {
        etCode = findViewById(R.id.etCode);
        etName = findViewById(R.id.etName);
        etValue = findViewById(R.id.etValue);
        etMaxDiscount = findViewById(R.id.etMaxDiscount);
        etMinOrder = findViewById(R.id.etMinOrder);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etPerUserLimit = findViewById(R.id.etPerUserLimit);
        etUsageLimit = findViewById(R.id.etUsageLimit);

        btnTypePercent = findViewById(R.id.btnTypePercent);
        btnTypeFixed = findViewById(R.id.btnTypeFixed);
        btnTypeShip = findViewById(R.id.btnTypeShip);

        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);

        btnTypePercent.setOnClickListener(v -> setType(Voucher.TYPE_PERCENT));
        btnTypeFixed.setOnClickListener(v -> setType(Voucher.TYPE_FIXED));
        btnTypeShip.setOnClickListener(v -> setType(Voucher.TYPE_FREESHIP));

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Toolbar Save
        findViewById(R.id.btnSave).setOnClickListener(v -> saveVoucher());
        // Bottom Save
        View btnSaveBottom = findViewById(R.id.btnSaveBottom);
        if (btnSaveBottom != null) btnSaveBottom.setOnClickListener(v -> saveVoucher());

        setType(Voucher.TYPE_PERCENT);
    }

    private void setType(String type) {
        selectedType = type;
        int activeColor = Color.parseColor("#F5A623"); 
        int inactiveColor = Color.parseColor("#F5F5F5"); 

        updateButtonState(btnTypePercent, Voucher.TYPE_PERCENT.equals(type), activeColor, inactiveColor);
        updateButtonState(btnTypeFixed, Voucher.TYPE_FIXED.equals(type), activeColor, inactiveColor);
        updateButtonState(btnTypeShip, Voucher.TYPE_FREESHIP.equals(type), activeColor, inactiveColor);
        
        // Disable value/max if freeship
        boolean isShip = Voucher.TYPE_FREESHIP.equals(type);
        etValue.setEnabled(!isShip);
        etMaxDiscount.setEnabled(!isShip);
        if (isShip) {
            etValue.setText("0");
            etMaxDiscount.setText("0");
        }
    }

    private void updateButtonState(MaterialButton btn, boolean isActive, int activeColor, int inactiveColor) {
        btn.setBackgroundTintList(ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
        btn.setTextColor(isActive ? Color.WHITE : Color.parseColor("#666666"));
    }

    private void showDatePicker(TextInputEditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            et.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading -> progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
        
        vm.getCurrentVoucher().observe(this, voucher -> {
            if (voucher != null) {
                currentVoucher = voucher;
                etCode.setText(voucher.getCode());
                etName.setText(voucher.getName());
                etValue.setText(String.valueOf(voucher.getDiscountValue()));
                etMaxDiscount.setText(String.valueOf(voucher.getMaxDiscountAmount()));
                etMinOrder.setText(String.valueOf(voucher.getMinOrderAmount()));
                etStartDate.setText(voucher.getStartDate());
                etEndDate.setText(voucher.getEndDate());
                etPerUserLimit.setText(String.valueOf(voucher.getPerUserLimit()));
                etUsageLimit.setText(String.valueOf(voucher.getUsageLimit()));
                setType(voucher.getType());
            }
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void saveVoucher() {
        String code = etCode.getText().toString().trim().toUpperCase();
        String valStr = etValue.getText().toString().trim();
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();

        if (code.isEmpty()) { Toast.makeText(this, "Mã voucher không được để trống", Toast.LENGTH_SHORT).show(); return; }
        if (!Voucher.TYPE_FREESHIP.equals(selectedType) && valStr.isEmpty()) { 
            Toast.makeText(this, "Mức giảm không được để trống", Toast.LENGTH_SHORT).show(); return; 
        }
        if (start.isEmpty() || end.isEmpty()) { Toast.makeText(this, "Vui lòng chọn ngày bắt đầu và kết thúc", Toast.LENGTH_SHORT).show(); return; }

        Voucher v = currentVoucher != null ? currentVoucher : new Voucher();
        v.setCode(code);
        v.setName(etName.getText().toString().trim());
        v.setType(selectedType);
        try { v.setDiscountValue(Double.parseDouble(valStr)); } catch (Exception e) { v.setDiscountValue(0); }
        try { v.setMaxDiscountAmount(Double.parseDouble(etMaxDiscount.getText().toString())); } catch (Exception e) { v.setMaxDiscountAmount(0); }
        try { v.setMinOrderAmount(Double.parseDouble(etMinOrder.getText().toString())); } catch (Exception e) { v.setMinOrderAmount(0); }
        v.setStartDate(start);
        v.setEndDate(end);
        try { v.setPerUserLimit(Integer.parseInt(etPerUserLimit.getText().toString())); } catch (Exception e) { v.setPerUserLimit(1); }
        try { v.setUsageLimit(Integer.parseInt(etUsageLimit.getText().toString())); } catch (Exception e) { v.setUsageLimit(1000); }
        v.setActive(true);

        if (currentVoucher == null) vm.add(v);
        else vm.update(v);
    }
}
