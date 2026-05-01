package com.example.petshop.view.activity;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.viewmodel.PromotionManageViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

public class AddEditPromotionActivity extends AppCompatActivity {

    private PromotionManageViewModel vm;
    private Promotion currentPromo;

    private EditText etName, etDesc, etDiscountValue, etMaxDiscount;
    private EditText etStartDate, etEndDate, etMaxPerUser, etTotalLimit;
    private MaterialButton btnTypePercent, btnTypeFixed, btnApplyAll, btnApplyPet, btnApplyFood;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private String selectedType = Promotion.TYPE_PERCENT;
    private String selectedApplyTo = Promotion.APPLY_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_promotion);

        vm = new ViewModelProvider(this).get(PromotionManageViewModel.class);

        initViews();
        observeViewModel();

        String promoId = getIntent().getStringExtra("promoId");
        if (promoId != null) {
            tvTitle.setText("Sửa khuyến mãi");
            vm.loadById(promoId);
        }
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDesc = findViewById(R.id.etDesc);
        etDiscountValue = findViewById(R.id.etDiscountValue);
        etMaxDiscount = findViewById(R.id.etMaxDiscount);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etMaxPerUser = findViewById(R.id.etMaxPerUser);
        etTotalLimit = findViewById(R.id.etTotalLimit);

        btnTypePercent = findViewById(R.id.btnTypePercent);
        btnTypeFixed = findViewById(R.id.btnTypeFixed);
        btnApplyAll = findViewById(R.id.btnApplyAll);
        btnApplyPet = findViewById(R.id.btnApplyPet);
        btnApplyFood = findViewById(R.id.btnApplyFood);

        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);

        btnTypePercent.setOnClickListener(v -> setType(Promotion.TYPE_PERCENT));
        btnTypeFixed.setOnClickListener(v -> setType(Promotion.TYPE_FIXED));

        btnApplyAll.setOnClickListener(v -> setApplyTo(Promotion.APPLY_ALL));
        btnApplyPet.setOnClickListener(v -> setApplyTo(Promotion.APPLY_PET));
        btnApplyFood.setOnClickListener(v -> setApplyTo(Promotion.APPLY_FOOD));

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Cả 2 nút Lưu (Toolbar và Bottom) đều trỏ về hàm savePromo
        findViewById(R.id.btnSave).setOnClickListener(v -> savePromo());
        View btnSaveBottom = findViewById(R.id.btnSaveBottom);
        if (btnSaveBottom != null) btnSaveBottom.setOnClickListener(v -> savePromo());

        setType(Promotion.TYPE_PERCENT);
        setApplyTo(Promotion.APPLY_ALL);
    }

    private void showDatePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            et.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setType(String type) {
        selectedType = type;
        int activeColor = Color.parseColor("#F5A623"); // Màu cam của shop
        int inactiveColor = Color.parseColor("#F5F5F5"); // Màu xám nhạt
        
        updateButtonState(btnTypePercent, Promotion.TYPE_PERCENT.equals(type), activeColor, inactiveColor);
        updateButtonState(btnTypeFixed, Promotion.TYPE_FIXED.equals(type), activeColor, inactiveColor);
        
        // Ẩn/Hiện tối đa giảm nếu chọn fixed
        findViewById(R.id.etMaxDiscount).setEnabled(Promotion.TYPE_PERCENT.equals(type));
    }

    private void setApplyTo(String applyTo) {
        selectedApplyTo = applyTo;
        int activeColor = Color.parseColor("#F5A623");
        int inactiveColor = Color.parseColor("#F5F5F5");

        updateButtonState(btnApplyAll, Promotion.APPLY_ALL.equals(applyTo), activeColor, inactiveColor);
        updateButtonState(btnApplyPet, Promotion.APPLY_PET.equals(applyTo), activeColor, inactiveColor);
        updateButtonState(btnApplyFood, Promotion.APPLY_FOOD.equals(applyTo), activeColor, inactiveColor);
    }

    private void updateButtonState(MaterialButton btn, boolean isActive, int activeColor, int inactiveColor) {
        btn.setBackgroundTintList(ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
        btn.setTextColor(isActive ? Color.WHITE : Color.parseColor("#666666"));
        // Nếu dùng style Outline thì cần bỏ stroke khi active
        btn.setStrokeWidth(isActive ? 0 : 2);
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading -> progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getCurrentPromo().observe(this, promo -> {
            if (promo != null) {
                currentPromo = promo;
                etName.setText(promo.getName());
                etDesc.setText(promo.getDescription());
                etDiscountValue.setText(String.valueOf(promo.getDiscountValue()));
                etMaxDiscount.setText(String.valueOf(promo.getMaxDiscountAmount()));
                etStartDate.setText(promo.getStartDate());
                etEndDate.setText(promo.getEndDate());
                etMaxPerUser.setText(String.valueOf(promo.getPerUserLimit()));
                etTotalLimit.setText(String.valueOf(promo.getUsageLimit()));
                setType(promo.getDiscountType());
                setApplyTo(promo.getApplyTo());
            }
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void savePromo() {
        String name = etName.getText().toString().trim();
        String discountStr = etDiscountValue.getText().toString().trim();

        if (name.isEmpty()) { Toast.makeText(this, "Tên khuyến mãi không được để trống", Toast.LENGTH_SHORT).show(); return; }
        if (discountStr.isEmpty()) { Toast.makeText(this, "Mức giảm không được để trống", Toast.LENGTH_SHORT).show(); return; }

        Promotion p = currentPromo != null ? currentPromo : new Promotion();
        p.setName(name);
        p.setDescription(etDesc.getText().toString().trim());
        p.setDiscountType(selectedType);
        try { p.setDiscountValue(Double.parseDouble(discountStr)); } catch (Exception e) { p.setDiscountValue(0); }
        try { p.setMaxDiscountAmount(Double.parseDouble(etMaxDiscount.getText().toString())); } catch (Exception e) { p.setMaxDiscountAmount(0); }
        p.setApplyTo(selectedApplyTo);
        p.setStartDate(etStartDate.getText().toString());
        p.setEndDate(etEndDate.getText().toString());
        p.setActive(true);
        try { p.setPerUserLimit(Integer.parseInt(etMaxPerUser.getText().toString())); } catch (Exception e) { p.setPerUserLimit(999); }
        try { p.setTotalUsageLimit(Integer.parseInt(etTotalLimit.getText().toString())); } catch (Exception e) { p.setTotalUsageLimit(9999); }

        if (currentPromo == null) vm.add(p);
        else vm.update(p);
    }
}
