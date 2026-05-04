package com.example.petshop.view.activity;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.petshop.R;

import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.viewmodel.PromotionManageViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddEditPromotionActivity extends AppCompatActivity {

    private PromotionManageViewModel vm;
    private Promotion currentPromo;

    private EditText etName, etDesc, etDiscountValue, etMaxDiscount;
    private EditText etStartDate, etEndDate, etMaxPerUser, etTotalLimit;
    private MaterialButton btnTypePercent, btnTypeFixed;
    private ProgressBar progressBar;
    private TextView tvTitle;

    // Apply type buttons
    private MaterialButton btnApplyAll, btnApplyCategory, btnApplySpecies, btnApplyProduct;
    
    // Category choice
    private LinearLayout layoutCategoryChoice;
    private MaterialButton btnCatPet, btnCatFood;
    
    // Species choice
    private LinearLayout layoutSpeciesChoice;
    private ChipGroup chipGroupSpecies;
    private Chip chipDog, chipCat, chipFish, chipBird, chipRabbit, chipHamster;
    
    // Product choice
    private LinearLayout layoutProductChoice;
    private MaterialButton btnSelectProducts;
    private TextView tvSelectedProducts;

    private String selectedType = Promotion.TYPE_PERCENT;
    private String selectedApplyType = Promotion.APPLY_ALL;
    private String selectedCategory = null;
    private Set<String> selectedSpecies = new HashSet<>();
    private List<String> selectedProductIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_promotion);

        vm = new ViewModelProvider(this).get(PromotionManageViewModel.class);

        initViews();
        setupApplyTypeButtons();
        setupCategoryChoice();
        setupSpeciesChoice();
        setupProductChoice();
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

        // Apply type buttons
        btnApplyAll = findViewById(R.id.btnApplyAll);
        btnApplyCategory = findViewById(R.id.btnApplyCategory);
        btnApplySpecies = findViewById(R.id.btnApplySpecies);
        btnApplyProduct = findViewById(R.id.btnApplyProduct);
        
        // Category choice
        layoutCategoryChoice = findViewById(R.id.layoutCategoryChoice);
        btnCatPet = findViewById(R.id.btnCatPet);
        btnCatFood = findViewById(R.id.btnCatFood);
        
        // Species choice
        layoutSpeciesChoice = findViewById(R.id.layoutSpeciesChoice);
        chipGroupSpecies = findViewById(R.id.chipGroupSpecies);
        chipDog = findViewById(R.id.chipDog);
        chipCat = findViewById(R.id.chipCat);
        chipFish = findViewById(R.id.chipFish);
        chipBird = findViewById(R.id.chipBird);
        chipRabbit = findViewById(R.id.chipRabbit);
        chipHamster = findViewById(R.id.chipHamster);
        
        // Product choice
        layoutProductChoice = findViewById(R.id.layoutProductChoice);
        btnSelectProducts = findViewById(R.id.btnSelectProducts);
        tvSelectedProducts = findViewById(R.id.tvSelectedProducts);

        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);

        btnTypePercent.setOnClickListener(v -> setType(Promotion.TYPE_PERCENT));
        btnTypeFixed.setOnClickListener(v -> setType(Promotion.TYPE_FIXED));

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnSave).setOnClickListener(v -> savePromo());
        View btnSaveBottom = findViewById(R.id.btnSaveBottom);
        if (btnSaveBottom != null) btnSaveBottom.setOnClickListener(v -> savePromo());

        setType(Promotion.TYPE_PERCENT);
        setApplyType(Promotion.APPLY_ALL);
    }

    private void setupApplyTypeButtons() {
        btnApplyAll.setOnClickListener(v -> setApplyType(Promotion.APPLY_ALL));
        btnApplyCategory.setOnClickListener(v -> setApplyType(Promotion.APPLY_CATEGORY));
        btnApplySpecies.setOnClickListener(v -> setApplyType(Promotion.APPLY_SPECIES));
        btnApplyProduct.setOnClickListener(v -> setApplyType(Promotion.APPLY_PRODUCT));
    }

    private void setupCategoryChoice() {
        btnCatPet.setOnClickListener(v -> {
            selectedCategory = Promotion.CATEGORY_PET;
            updateCategoryButtonState();
        });
        btnCatFood.setOnClickListener(v -> {
            selectedCategory = Promotion.CATEGORY_FOOD;
            updateCategoryButtonState();
        });
    }

    private void setupSpeciesChoice() {
        View.OnClickListener speciesClickListener = v -> {
            Chip chip = (Chip) v;
            String species = getSpeciesFromChip(chip.getId());
            if (species != null) {
                if (chip.isChecked()) {
                    selectedSpecies.add(species);
                } else {
                    selectedSpecies.remove(species);
                }
            }
        };

        chipDog.setOnClickListener(speciesClickListener);
        chipCat.setOnClickListener(speciesClickListener);
        chipFish.setOnClickListener(speciesClickListener);
        chipBird.setOnClickListener(speciesClickListener);
        chipRabbit.setOnClickListener(speciesClickListener);
        chipHamster.setOnClickListener(speciesClickListener);
    }

    private void setupProductChoice() {
        btnSelectProducts.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            
            // Tải danh sách thú cưng và thức ăn
            new PetRepository().getAll(new PetRepository.Callback<List<Pet>>() {
                @Override
                public void onSuccess(List<Pet> pets) {
                    new FoodRepository().getAll(new FoodRepository.Callback<List<Food>>() {
                        @Override
                        public void onSuccess(List<Food> foods) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                showProductSelectionDialog(pets, foods);
                            });
                        }
                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AddEditPromotionActivity.this, "Lỗi tải thức ăn", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddEditPromotionActivity.this, "Lỗi tải thú cưng", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private void showProductSelectionDialog(List<Pet> pets, List<Food> foods) {
        List<String> productIds = new ArrayList<>();
        List<String> productNames = new ArrayList<>();

        for (Pet p : pets) {
            productIds.add(p.getId());
            productNames.add("[Thú cưng] " + p.getName());
        }
        for (Food f : foods) {
            productIds.add(f.getId());
            productNames.add("[Thức ăn] " + f.getName());
        }

        String[] itemsArray = productNames.toArray(new String[0]);
        boolean[] checkedItems = new boolean[itemsArray.length];

        // Khôi phục trạng thái đã chọn
        for (int i = 0; i < productIds.size(); i++) {
            if (selectedProductIds.contains(productIds.get(i))) {
                checkedItems[i] = true;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn sản phẩm áp dụng")
                .setMultiChoiceItems(itemsArray, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    selectedProductIds.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedProductIds.add(productIds.get(i));
                        }
                    }
                    updateProductSelection();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private String getSpeciesFromChip(int chipId) {
        if (chipId == R.id.chipDog) return Promotion.SPECIES_DOG;
        if (chipId == R.id.chipCat) return Promotion.SPECIES_CAT;
        if (chipId == R.id.chipFish) return Promotion.SPECIES_FISH;
        if (chipId == R.id.chipBird) return Promotion.SPECIES_BIRD;
        if (chipId == R.id.chipRabbit) return Promotion.SPECIES_RABBIT;
        if (chipId == R.id.chipHamster) return Promotion.SPECIES_HAMSTER;
        return null;
    }

    private void showDatePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            et.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setType(String type) {
        selectedType = type;
        int activeColor = Color.parseColor("#F5A623");
        int inactiveColor = Color.parseColor("#F5F5F5");
        
        updateButtonState(btnTypePercent, Promotion.TYPE_PERCENT.equals(type), activeColor, inactiveColor);
        updateButtonState(btnTypeFixed, Promotion.TYPE_FIXED.equals(type), activeColor, inactiveColor);
        
        findViewById(R.id.etMaxDiscount).setEnabled(Promotion.TYPE_PERCENT.equals(type));
    }

    private void setApplyType(String applyType) {
        selectedApplyType = applyType;
        int activeColor = Color.parseColor("#F5A623");
        int inactiveColor = Color.parseColor("#F5F5F5");

        updateButtonState(btnApplyAll, Promotion.APPLY_ALL.equals(applyType), activeColor, inactiveColor);
        updateButtonState(btnApplyCategory, Promotion.APPLY_CATEGORY.equals(applyType), activeColor, inactiveColor);
        updateButtonState(btnApplySpecies, Promotion.APPLY_SPECIES.equals(applyType), activeColor, inactiveColor);
        updateButtonState(btnApplyProduct, Promotion.APPLY_PRODUCT.equals(applyType), activeColor, inactiveColor);

        // Show/hide sub-options
        layoutCategoryChoice.setVisibility(
                Promotion.APPLY_CATEGORY.equals(applyType) ? View.VISIBLE : View.GONE);
        layoutSpeciesChoice.setVisibility(
                Promotion.APPLY_SPECIES.equals(applyType) ? View.VISIBLE : View.GONE);
        layoutProductChoice.setVisibility(
                Promotion.APPLY_PRODUCT.equals(applyType) ? View.VISIBLE : View.GONE);

        // Reset sub-selections when switching apply type
        if (!Promotion.APPLY_CATEGORY.equals(applyType)) {
            selectedCategory = null;
            updateCategoryButtonState();
        }
        if (!Promotion.APPLY_SPECIES.equals(applyType)) {
            selectedSpecies.clear();
            clearSpeciesChips();
        }
    }

    private void updateCategoryButtonState() {
        int activeColor = Color.parseColor("#F5A623");
        int inactiveColor = Color.WHITE;
        
        btnCatPet.setBackgroundTintList(ColorStateList.valueOf(
                Promotion.CATEGORY_PET.equals(selectedCategory) ? activeColor : inactiveColor));
        btnCatPet.setTextColor(Promotion.CATEGORY_PET.equals(selectedCategory) ? Color.WHITE : Color.parseColor("#666666"));
        
        btnCatFood.setBackgroundTintList(ColorStateList.valueOf(
                Promotion.CATEGORY_FOOD.equals(selectedCategory) ? activeColor : inactiveColor));
        btnCatFood.setTextColor(Promotion.CATEGORY_FOOD.equals(selectedCategory) ? Color.WHITE : Color.parseColor("#666666"));
    }

    private void clearSpeciesChips() {
        chipDog.setChecked(false);
        chipCat.setChecked(false);
        chipFish.setChecked(false);
        chipBird.setChecked(false);
        chipRabbit.setChecked(false);
        chipHamster.setChecked(false);
    }

    private void updateButtonState(MaterialButton btn, boolean isActive, int activeColor, int inactiveColor) {
        btn.setBackgroundTintList(ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
        btn.setTextColor(isActive ? Color.WHITE : Color.parseColor("#666666"));
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
                
                // Restore apply type
                String applyType = promo.getApplyType();
                setApplyType(applyType != null ? applyType : Promotion.APPLY_ALL);
                
                // Restore category selection
                if (promo.getApplyCategory() != null) {
                    selectedCategory = promo.getApplyCategory();
                    updateCategoryButtonState();
                }
                
                // Restore species selection
                if (promo.getApplySpecies() != null) {
                    selectedSpecies = new HashSet<>(promo.getApplySpecies());
                    updateSpeciesChips();
                }
                
                // Restore product selection
                if (promo.getProductIds() != null) {
                    selectedProductIds = new ArrayList<>(promo.getProductIds());
                    updateProductSelection();
                }
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

    private void updateSpeciesChips() {
        chipDog.setChecked(selectedSpecies.contains(Promotion.SPECIES_DOG));
        chipCat.setChecked(selectedSpecies.contains(Promotion.SPECIES_CAT));
        chipFish.setChecked(selectedSpecies.contains(Promotion.SPECIES_FISH));
        chipBird.setChecked(selectedSpecies.contains(Promotion.SPECIES_BIRD));
        chipRabbit.setChecked(selectedSpecies.contains(Promotion.SPECIES_RABBIT));
        chipHamster.setChecked(selectedSpecies.contains(Promotion.SPECIES_HAMSTER));
    }

    private void updateProductSelection() {
        if (selectedProductIds.isEmpty()) {
            tvSelectedProducts.setText("Chưa chọn sản phẩm nào");
            tvSelectedProducts.setTextColor(getColor(R.color.text_hint));
        } else {
            tvSelectedProducts.setText("Đã chọn " + selectedProductIds.size() + " sản phẩm");
            tvSelectedProducts.setTextColor(getColor(R.color.primary));
        }
    }

    private void savePromo() {
        String name = etName.getText().toString().trim();
        String discountStr = etDiscountValue.getText().toString().trim();

        if (name.isEmpty()) { 
            Toast.makeText(this, "Tên khuyến mãi không được để trống", Toast.LENGTH_SHORT).show(); 
            return; 
        }
        if (discountStr.isEmpty()) { 
            Toast.makeText(this, "Mức giảm không được để trống", Toast.LENGTH_SHORT).show(); 
            return; 
        }

        // Validate sub-selections
        if (Promotion.APPLY_CATEGORY.equals(selectedApplyType) && selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục (Thú cưng hoặc Thức ăn)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Promotion.APPLY_SPECIES.equals(selectedApplyType) && selectedSpecies.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một giống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Promotion.APPLY_PRODUCT.equals(selectedApplyType) && selectedProductIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        Promotion p = currentPromo != null ? currentPromo : new Promotion();
        p.setName(name);
        p.setDescription(etDesc.getText().toString().trim());
        p.setDiscountType(selectedType);
        try { p.setDiscountValue(Double.parseDouble(discountStr)); } catch (Exception e) { p.setDiscountValue(0); }
        try { p.setMaxDiscountAmount(Double.parseDouble(etMaxDiscount.getText().toString())); } catch (Exception e) { p.setMaxDiscountAmount(0); }
        
        // Apply type settings
        p.setApplyType(selectedApplyType);
        p.setApplyCategory(selectedCategory);
        p.setApplySpecies(new ArrayList<>(selectedSpecies));
        p.setProductIds(new ArrayList<>(selectedProductIds));
        
        p.setStartDate(etStartDate.getText().toString());
        p.setEndDate(etEndDate.getText().toString());
        p.setActive(true);
        try { p.setPerUserLimit(Integer.parseInt(etMaxPerUser.getText().toString())); } catch (Exception e) { p.setPerUserLimit(999); }
        try { p.setTotalUsageLimit(Integer.parseInt(etTotalLimit.getText().toString())); } catch (Exception e) { p.setTotalUsageLimit(9999); }

        if (currentPromo == null) vm.add(p);
        else vm.update(p);
    }
}
