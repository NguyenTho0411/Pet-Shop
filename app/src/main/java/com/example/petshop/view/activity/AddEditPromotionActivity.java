package com.example.petshop.view.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.viewmodel.PromotionManageViewModel;

public class AddEditPromotionActivity extends AppCompatActivity {

    private PromotionManageViewModel vm;
    private Promotion currentPromo;

    private EditText etName, etDesc, etDiscountValue, etMaxDiscount;
    private EditText etStartDate, etEndDate, etMaxPerUser, etTotalLimit;
    private Button btnTypePercent, btnTypeFixed, btnApplyAll, btnApplyPet, btnApplyFood;
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
            // Load existing
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

        // Type toggle
        btnTypePercent.setOnClickListener(v -> setType(Promotion.TYPE_PERCENT));
        btnTypeFixed.setOnClickListener(v -> setType(Promotion.TYPE_FIXED));

        // ApplyTo toggle
        btnApplyAll.setOnClickListener(v -> setApplyTo(Promotion.APPLY_ALL));
        btnApplyPet.setOnClickListener(v -> setApplyTo(Promotion.APPLY_PET));
        btnApplyFood.setOnClickListener(v -> setApplyTo(Promotion.APPLY_FOOD));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> savePromo());

        setType(Promotion.TYPE_PERCENT);
        setApplyTo(Promotion.APPLY_ALL);
    }

    private void setType(String type) {
        selectedType = type;
        btnTypePercent.setBackgroundResource(Promotion.TYPE_PERCENT.equals(type) 
            ? R.drawable.bg_orange_pill : R.drawable.bg_social_btn);
        btnTypeFixed.setBackgroundResource(Promotion.TYPE_FIXED.equals(type) 
            ? R.drawable.bg_orange_pill : R.drawable.bg_social_btn);
    }

    private void setApplyTo(String applyTo) {
        selectedApplyTo = applyTo;
        btnApplyAll.setBackgroundResource(Promotion.APPLY_ALL.equals(applyTo)
            ? R.drawable.bg_orange_pill : R.drawable.bg_social_btn);
        btnApplyPet.setBackgroundResource(Promotion.APPLY_PET.equals(applyTo)
            ? R.drawable.bg_orange_pill : R.drawable.bg_social_btn);
        btnApplyFood.setBackgroundResource(Promotion.APPLY_FOOD.equals(applyTo)
            ? R.drawable.bg_orange_pill : R.drawable.bg_social_btn);
    }

    private void observeViewModel() {
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

        if (name.isEmpty()) { Toast.makeText(this, "Tên bắt buộc", Toast.LENGTH_SHORT).show(); return; }
        if (discountStr.isEmpty()) { Toast.makeText(this, "Mức giảm bắt buộc", Toast.LENGTH_SHORT).show(); return; }

        Promotion p = currentPromo != null ? currentPromo : new Promotion();
        p.setName(name);
        p.setDescription(etDesc.getText().toString().trim());
        p.setDiscountType(selectedType);
        try { p.setDiscountValue(Double.parseDouble(discountStr)); } catch (Exception e) {}
        try { p.setMaxDiscountAmount(Double.parseDouble(etMaxDiscount.getText().toString())); } catch (Exception e) {}
        p.setApplyTo(selectedApplyTo);
        p.setStartDate(etStartDate.getText().toString());
        p.setEndDate(etEndDate.getText().toString());
        p.setActive(true);
        try { p.setMaxUsagePerUser(Integer.parseInt(etMaxPerUser.getText().toString())); } catch (Exception e) {}
        try { p.setTotalUsageLimit(Integer.parseInt(etTotalLimit.getText().toString())); } catch (Exception e) {}

        if (currentPromo == null) vm.add(p);
        else vm.update(p);
    }
}
