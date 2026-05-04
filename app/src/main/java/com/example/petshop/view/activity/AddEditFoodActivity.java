package com.example.petshop.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Category;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.FoodMedia;
import com.example.petshop.repository.CategoryRepository;
import com.example.petshop.view.adapter.MediaPickerAdapter;
import com.example.petshop.viewmodel.FoodManageViewModel;

import java.util.ArrayList;
import java.util.List;

public class AddEditFoodActivity extends AppCompatActivity {

    private FoodManageViewModel vm;
    private Food currentFood;
    private List<Category> categories = new ArrayList<>();
    private String editingFoodId;

    private EditText etName, etType, etWeight, etBrand, etOrigin;
    private EditText etDesc, etNutrition, etUsage, etPrice, etOrigPrice, etStock;
    private AutoCompleteTextView actvCategory;
    private ProgressBar progressBar;
    private Button btnSave;
    private TextView tvTitle;

    private MediaPickerAdapter mediaAdapter;

    private final ActivityResultLauncher<String[]> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null) {
                    for (Uri uri : uris) {
                        String mime = getContentResolver().getType(uri);
                        int type = (mime != null && mime.startsWith("video"))
                                ? MediaPickerAdapter.TYPE_VIDEO
                                : MediaPickerAdapter.TYPE_IMAGE;
                        mediaAdapter.addItem(new MediaPickerAdapter.MediaItem(uri, type));
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_food);

        vm = new ViewModelProvider(this).get(FoodManageViewModel.class);

        initViews();
        loadCategories();
        observeViewModel();

        editingFoodId = getIntent().getStringExtra("foodId");
        if (editingFoodId != null && !editingFoodId.isEmpty()) {
            tvTitle.setText("Sửa thức ăn");
            btnSave.setText("Cập nhật");
            vm.loadById(editingFoodId);
            vm.loadMedia(editingFoodId);
        }
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etType = findViewById(R.id.etType);
        etWeight = findViewById(R.id.etWeight);
        etBrand = findViewById(R.id.etBrand);
        etOrigin = findViewById(R.id.etOrigin);
        etDesc = findViewById(R.id.etDesc);
        etNutrition = findViewById(R.id.etNutrition);
        etUsage = findViewById(R.id.etUsage);
        etPrice = findViewById(R.id.etPrice);
        etOrigPrice = findViewById(R.id.etOrigPrice);
        etStock = findViewById(R.id.etStock);
        actvCategory = findViewById(R.id.actvCategory);

        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);
        tvTitle = findViewById(R.id.tvTitle);

        // Media RecyclerView - Code tương tự Pet
        RecyclerView rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaAdapter = new MediaPickerAdapter(new ArrayList<>(), new MediaPickerAdapter.OnMediaAction() {
            @Override
            public void onAddClick() {
                mediaPickerLauncher.launch(new String[]{"image/*", "video/*"});
            }

            @Override
            public void onRemoveClick(int index) {
                MediaPickerAdapter.MediaItem item = mediaAdapter.getItems().get(index);
                if (item.isExisting && editingFoodId != null) {
                    confirmDeleteMedia(index, item);
                } else {
                    mediaAdapter.removeItem(index);
                }
            }
        });
        rvMedia.setAdapter(mediaAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveFood());
        View btnSaveBottom = findViewById(R.id.btnSaveBottom);
        if (btnSaveBottom != null) btnSaveBottom.setOnClickListener(v -> saveFood());
    }

    private void loadCategories() {
        new CategoryRepository().getByType(Category.TYPE_FOOD, new CategoryRepository.Callback<>() {
            @Override
            public void onSuccess(List<Category> data) {
                categories = data != null ? data : new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.getName());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditFoodActivity.this,
                        android.R.layout.simple_dropdown_item_1line, names);
                actvCategory.setAdapter(adapter);
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSave.setEnabled(!loading);
        });

        vm.getCurrentFood().observe(this, food -> {
            if (food != null) {
                currentFood = food;
                fillForm(food);
            }
        });

        vm.getMediaList().observe(this, medias -> {
            if (medias == null) return;
            List<MediaPickerAdapter.MediaItem> items = new ArrayList<>();
            for (FoodMedia m : medias) {
                int type = "VIDEO".equals(m.getMediaType()) ? MediaPickerAdapter.TYPE_VIDEO : MediaPickerAdapter.TYPE_IMAGE;
                items.add(new MediaPickerAdapter.MediaItem(m.getMediaUrl(), m.getId(), type));
            }
            // Giữ lại các item mới chưa upload
            List<MediaPickerAdapter.MediaItem> current = new ArrayList<>(mediaAdapter.getItems());
            current.removeIf(i -> i.isExisting);
            items.addAll(current);

            mediaAdapter.getItems().clear();
            mediaAdapter.getItems().addAll(items);
            mediaAdapter.notifyDataSetChanged();
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fillForm(Food food) {
        etName.setText(food.getName());
        etType.setText(food.getFoodType());
        etWeight.setText(String.valueOf(food.getWeightGram()));
        etBrand.setText(food.getBrand());
        etOrigin.setText(food.getOrigin());
        etDesc.setText(food.getDescription());
        etNutrition.setText(food.getNutritionInfo());
        etUsage.setText(food.getUsageGuide());
        etPrice.setText(String.valueOf(food.getPrice()));
        etOrigPrice.setText(String.valueOf(food.getOriginalPrice()));
        etStock.setText(String.valueOf(food.getStock()));

        for (Category c : categories) {
            if (c.getId().equals(food.getCategoryId())) {
                actvCategory.setText(c.getName(), false);
                break;
            }
        }
    }

    private void saveFood() {
        String name = etName.getText().toString().trim();
        String categoryName = actvCategory.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (name.isEmpty() || categoryName.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryId = null;
        for (Category c : categories) {
            if (c.getName().equals(categoryName)) {
                categoryId = c.getId();
                break;
            }
        }

        if (categoryId == null) {
            Toast.makeText(this, "Danh mục không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Food food = currentFood != null ? currentFood : new Food();
        if (currentFood != null) {
            food.setThumbnailUrl(currentFood.getThumbnailUrl());
        }
        food.setName(name);
        food.setCategoryId(categoryId);
        food.setFoodType(etType.getText().toString().trim());
        try { food.setWeightGram((int) Double.parseDouble(etWeight.getText().toString())); } catch (Exception e) {}
        try { food.setPrice(Double.parseDouble(priceStr)); } catch (Exception e) {}
        try { food.setOriginalPrice(Double.parseDouble(etOrigPrice.getText().toString())); } catch (Exception e) {}
        try { food.setStock(Integer.parseInt(etStock.getText().toString())); } catch (Exception e) {}

        food.setBrand(etBrand.getText().toString().trim());
        food.setOrigin(etOrigin.getText().toString().trim());
        food.setDescription(etDesc.getText().toString().trim());
        food.setNutritionInfo(etNutrition.getText().toString().trim());
        food.setUsageGuide(etUsage.getText().toString().trim());

        vm.saveFood(food, mediaAdapter.getNewUris(), mediaAdapter.getNewTypes());
    }

    private void confirmDeleteMedia(int index, MediaPickerAdapter.MediaItem item) {
        DialogUtils.showConfirmDialog(this, "Xoá media này?",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    vm.deleteMediaItem(editingFoodId, item.mediaId, item.url);
                    mediaAdapter.removeItem(index);
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
