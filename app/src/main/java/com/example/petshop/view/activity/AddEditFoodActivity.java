package com.example.petshop.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final int PICK_IMAGE = 100;
    private static final int PICK_VIDEO = 101;

    private FoodManageViewModel vm;
    private Food currentFood;
    private List<Category> categories = new ArrayList<>();

    private EditText etName, etType, etWeight, etBrand, etOrigin;
    private EditText etDesc, etNutrition, etUsage, etPrice, etOrigPrice, etStock;
    private AutoCompleteTextView actvCategory;
    private ProgressBar progressBar;
    private Button btnSave;
    private TextView tvTitle;

    private MediaPickerAdapter mediaAdapter;
    private List<MediaPickerAdapter.MediaItem> mediaItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_food);

        vm = new ViewModelProvider(this).get(FoodManageViewModel.class);

        initViews();
        setupMediaAdapter();
        loadCategories();
        observeViewModel();

        String foodId = getIntent().getStringExtra("foodId");
        if (foodId != null && !foodId.isEmpty()) {
            tvTitle.setText("Sửa thức ăn");
            btnSave.setText("Cập nhật");
            vm.loadById(foodId);
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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveFood());
        // Nút lưu ở bottom
        View btnSaveBottom = findViewById(R.id.btnSaveBottom);
        if (btnSaveBottom != null) btnSaveBottom.setOnClickListener(v -> saveFood());
    }

    private void setupMediaAdapter() {
        RecyclerView rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        mediaAdapter = new MediaPickerAdapter(mediaItems, new MediaPickerAdapter.OnMediaAction() {
            @Override
            public void onAddClick() {
                showMediaPickerMenu();
            }
            @Override
            public void onRemoveClick(int index) {
                mediaAdapter.removeItem(index);
            }
        });
        rvMedia.setAdapter(mediaAdapter);
    }

    private void showMediaPickerMenu() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Thêm media")
                .setItems(new String[]{"📷 Ảnh", "🎬 Video"}, (dialog, which) -> {
                    if (which == 0) pickImage();
                    else            pickVideo();
                }).show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == PICK_IMAGE) {
                    mediaAdapter.addItem(new MediaPickerAdapter.MediaItem(uri, MediaPickerAdapter.TYPE_IMAGE));
                } else if (requestCode == PICK_VIDEO) {
                    mediaAdapter.addItem(new MediaPickerAdapter.MediaItem(uri, MediaPickerAdapter.TYPE_VIDEO));
                }
            }
        }
    }

    private void loadCategories() {
        CategoryRepository repo = new CategoryRepository();
        repo.getByType(Category.TYPE_FOOD, new CategoryRepository.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> data) {
                categories = data != null ? data : new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.getName());
                setupAutocomplete(actvCategory, names.toArray(new String[0]));
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(AddEditFoodActivity.this, "Lỗi tải danh mục: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupAutocomplete(AutoCompleteTextView actv, String[] items) {
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, items);
        actv.setAdapter(adapter);
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
            btnSave.setEnabled(!loading);
        });

        vm.getCurrentFood().observe(this, food -> {
            if (food != null) {
                currentFood = food;
                boundFoodToUI(food);
            }
        });

        vm.getMediaList().observe(this, medias -> {
            if (medias != null) {
                mediaItems.clear();
                for (FoodMedia m : medias) {
                    int type = "VIDEO".equals(m.getMediaType()) ? MediaPickerAdapter.TYPE_VIDEO : MediaPickerAdapter.TYPE_IMAGE;
                    mediaItems.add(new MediaPickerAdapter.MediaItem(m.getMediaUrl(), m.getId(), type));
                }
                mediaAdapter.notifyDataSetChanged();
            }
        });

        vm.getUploadProgress().observe(this, progress -> {
            progressBar.setProgress(progress);
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

    private void boundFoodToUI(Food food) {
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

        actvCategory.setText(food.getCategoryId() != null ? getCategoryName(food.getCategoryId()) : "", false);
    }

    private String getCategoryName(String categoryId) {
        for (Category c : categories) {
            if (c.getId().equals(categoryId)) return c.getName();
        }
        return "";
    }

    private void saveFood() {
        String name = etName.getText().toString().trim();
        String categoryName = actvCategory.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Tên thức ăn bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Danh mục bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "Giá bán bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryId = getCategoryIdByName(categoryName);
        if (categoryId == null) {
            Toast.makeText(this, "Danh mục không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Food food = currentFood != null ? currentFood : new Food();
        food.setName(name);
        food.setCategoryId(categoryId);
        food.setFoodType(etType.getText().toString().trim());
        
        try {
            food.setWeightGram((int) Double.parseDouble(etWeight.getText().toString()));
        } catch (Exception e) {
            food.setWeightGram(0);
        }
        try {
            food.setPrice(Double.parseDouble(priceStr));
        } catch (Exception e) {
            food.setPrice(0);
        }
        try {
            food.setOriginalPrice(Double.parseDouble(etOrigPrice.getText().toString()));
        } catch (Exception e) {
            food.setOriginalPrice(0);
        }
        try {
            food.setStock(Integer.parseInt(stockStr));
        } catch (Exception e) {
            food.setStock(0);
        }

        food.setBrand(etBrand.getText().toString().trim());
        food.setOrigin(etOrigin.getText().toString().trim());
        food.setDescription(etDesc.getText().toString().trim());
        food.setNutritionInfo(etNutrition.getText().toString().trim());
        food.setUsageGuide(etUsage.getText().toString().trim());

        List<Uri> newUris = mediaAdapter.getNewUris();
        List<String> mediaTypes = mediaAdapter.getNewTypes();

        vm.saveFood(food, newUris, mediaTypes);
    }

    private String getCategoryIdByName(String name) {
        for (Category c : categories) {
            if (c.getName().equals(name)) return c.getId();
        }
        return null;
    }
}
