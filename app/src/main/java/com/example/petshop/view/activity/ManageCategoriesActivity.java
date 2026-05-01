package com.example.petshop.view.activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Category;
import com.example.petshop.viewmodel.CategoryManageViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends AppCompatActivity {

    private CategoryManageViewModel vm;
    private List<Category>          allCategories = new ArrayList<>();
    private RecyclerView            rv;
    private ProgressBar             progressBar;

    private android.widget.ImageView ivDialogPreview;
    private android.net.Uri          selectedImageUri;

    private final androidx.activity.result.ActivityResultLauncher<String[]> imagePicker =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (ivDialogPreview != null) {
                        com.bumptech.glide.Glide.with(this).load(uri).centerCrop().into(ivDialogPreview);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);

        vm = new ViewModelProvider(this).get(CategoryManageViewModel.class);

        initViews();
        observeViewModel();
        vm.loadAll();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        rv = findViewById(R.id.rvCategories);
        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddEditDialog(null));

        findViewById(R.id.chipAll).setOnClickListener(v  -> renderList(allCategories));
        findViewById(R.id.chipPet).setOnClickListener(v  -> renderList(filterBy(Category.TYPE_PET)));
        findViewById(R.id.chipFood).setOnClickListener(v -> renderList(filterBy(Category.TYPE_FOOD)));
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getCategories().observe(this, list -> {
            allCategories = list != null ? list : new ArrayList<>();
            renderList(allCategories);
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void renderList(List<Category> list) {
        // Build adapter inline
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_category_admin, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
                Category cat = list.get(pos);
                View v = holder.itemView;
                
                android.widget.ImageView ivCat = v.findViewById(R.id.ivCategoryImage);
                if (cat.getImageUrl() != null && !cat.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(v.getContext()).load(cat.getImageUrl()).centerCrop().into(ivCat);
                } else {
                    ivCat.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                ((TextView) v.findViewById(R.id.tvName)).setText(cat.getName());
                ((TextView) v.findViewById(R.id.tvType)).setText(cat.getType());
                TextView tvDesc = v.findViewById(R.id.tvDesc);
                tvDesc.setText(cat.getDescription() != null ? cat.getDescription() : "—");

                SwitchMaterial sw = v.findViewById(R.id.switchActive);
                sw.setChecked(cat.isActive());
                sw.setOnCheckedChangeListener((btn, checked) -> vm.toggleActive(cat.getId(), checked));

                v.findViewById(R.id.btnEdit).setOnClickListener(x -> showAddEditDialog(cat));
                v.findViewById(R.id.btnDelete).setOnClickListener(x ->
                        new AlertDialog.Builder(ManageCategoriesActivity.this)
                                .setMessage("Xoá danh mục \"" + cat.getName() + "\"?")
                                .setPositiveButton("Xoá", (d, w) -> vm.delete(cat.getId()))
                                .setNegativeButton("Huỷ", null).show());
            }
            @Override public int getItemCount() { return list.size(); }
        });
    }

    private void showAddEditDialog(Category existing) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_edit_category);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.96f),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(android.view.Gravity.CENTER);

        TextView tvTitle    = dialog.findViewById(R.id.tvDialogTitle);
        TextInputEditText etName       = dialog.findViewById(R.id.etName);
        TextInputEditText etDesc       = dialog.findViewById(R.id.etDesc);
        TextInputEditText etSortOrder  = dialog.findViewById(R.id.etSortOrder);
        Button btnTypePet  = dialog.findViewById(R.id.btnTypePet);
        Button btnTypeFood = dialog.findViewById(R.id.btnTypeFood);
        Button btnSave     = dialog.findViewById(R.id.btnSave);
        Button btnCancel   = dialog.findViewById(R.id.btnCancel);

        final String[] selectedType = { Category.TYPE_PET };
        ivDialogPreview = dialog.findViewById(R.id.ivCategory);
        selectedImageUri = null;

        tvTitle.setText(existing == null ? "Thêm danh mục" : "Sửa danh mục");

        if (existing != null) {
            etName.setText(existing.getName());
            etDesc.setText(existing.getDescription());
            etSortOrder.setText(String.valueOf(existing.getSortOrder()));
            selectedType[0] = existing.getType();
            if (existing.getImageUrl() != null) {
                com.bumptech.glide.Glide.with(this).load(existing.getImageUrl()).centerCrop().into(ivDialogPreview);
            }
        }

        ivDialogPreview.setOnClickListener(v -> imagePicker.launch(new String[]{"image/*"}));

        updateTypeButtons(btnTypePet, btnTypeFood, selectedType[0]);

        btnTypePet.setOnClickListener(v -> {
            selectedType[0] = Category.TYPE_PET;
            updateTypeButtons(btnTypePet, btnTypeFood, Category.TYPE_PET);
        });
        btnTypeFood.setOnClickListener(v -> {
            selectedType[0] = Category.TYPE_FOOD;
            updateTypeButtons(btnTypePet, btnTypeFood, Category.TYPE_FOOD);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (TextUtils.isEmpty(name)) { etName.setError("Bắt buộc"); return; }

            Category cat = existing != null ? existing : new Category();
            cat.setName(name);
            cat.setType(selectedType[0]);
            cat.setDescription(etDesc.getText() != null ? etDesc.getText().toString().trim() : "");
            try { cat.setSortOrder(Integer.parseInt(etSortOrder.getText().toString())); } catch (Exception e) { cat.setSortOrder(0); }
            cat.setActive(true);

            if (existing == null) vm.add(cat, selectedImageUri);
            else                  vm.update(cat, selectedImageUri);
            dialog.dismiss();
            ivDialogPreview = null;
        });

        dialog.show();
    }

    private void updateTypeButtons(Button btnPet, Button btnFood, String type) {
        int orange  = getResources().getColor(R.color.primary,    getTheme());
        int white   = getResources().getColor(R.color.white,      getTheme());
        int gray    = getResources().getColor(R.color.divider,    getTheme());
        int textDark= getResources().getColor(R.color.text_primary, getTheme());

        if (Category.TYPE_PET.equals(type)) {
            applySelected(btnPet,  orange, white);
            applyOutline (btnFood, gray,   textDark);
        } else {
            applySelected(btnFood, orange, white);
            applyOutline (btnPet,  gray,   textDark);
        }
    }

    private void applySelected(Button btn, int bgColor, int textColor) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        btn.setTextColor(textColor);
    }

    private void applyOutline(Button btn, int strokeColor, int textColor) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.TRANSPARENT));
        btn.setTextColor(textColor);
        // Nếu là MaterialButton thì set stroke
        if (btn instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btn)
                    .setStrokeColor(android.content.res.ColorStateList.valueOf(strokeColor));
        }
    }

    private List<Category> filterBy(String type) {
        List<Category> result = new ArrayList<>();
        for (Category c : allCategories) if (type.equals(c.getType())) result.add(c);
        return result;
    }
}
