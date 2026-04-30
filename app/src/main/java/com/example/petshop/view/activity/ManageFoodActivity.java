package com.example.petshop.view.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.view.adapter.FoodAdminAdapter;
import com.example.petshop.viewmodel.FoodManageViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ManageFoodActivity extends AppCompatActivity {

    private FoodManageViewModel vm;
    private FoodAdminAdapter adapter;
    private List<Food> allFoods = new ArrayList<>();
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_foods);

        vm = new ViewModelProvider(this).get(FoodManageViewModel.class);

        initViews();
        observeViewModel();
        vm.loadAll();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);

        RecyclerView rv = findViewById(R.id.rvFoods);
        adapter = new FoodAdminAdapter(new ArrayList<>(), new FoodAdminAdapter.OnActionListener() {
            public void onEdit(Food food) {
                Intent intent = new Intent(ManageFoodActivity.this, AddEditFoodActivity.class);
                intent.putExtra("foodId", food.getId());
                startActivity(intent);
            }
            public void onDelete(Food food) {
                new AlertDialog.Builder(ManageFoodActivity.this)
                        .setMessage("Xoá \"" + food.getName() + "\"? Không thể hoàn tác!")
                        .setPositiveButton("Xoá", (d, w) -> vm.deleteFood(food.getId()))
                        .setNegativeButton("Huỷ", null).show();
            }
            public void onChangeStock(Food food) {
                showStockDialog(food);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditFoodActivity.class));
        });

        findViewById(R.id.chipAll).setOnClickListener(v         -> filterFoods("ALL"));
        findViewById(R.id.chipInStock).setOnClickListener(v     -> filterFoods("IN_STOCK"));
        findViewById(R.id.chipOutOfStock).setOnClickListener(v  -> filterFoods("OUT_OF_STOCK"));
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getFoods().observe(this, foods -> {
            allFoods = foods != null ? foods : new ArrayList<>();
            adapter.updateList(new ArrayList<>(allFoods));
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void filterFoods(String filter) {
        if ("ALL".equals(filter)) {
            adapter.updateList(new ArrayList<>(allFoods));
        } else {
            List<Food> filtered = new ArrayList<>();
            for (Food f : allFoods) {
                if ("IN_STOCK".equals(filter) && f.getStock() > 0) filtered.add(f);
                else if ("OUT_OF_STOCK".equals(filter) && f.getStock() == 0) filtered.add(f);
            }
            adapter.updateList(filtered);
        }
    }

    private void showStockDialog(Food food) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_update_stock, null);
        TextInputEditText etStock = view.findViewById(R.id.etStock);
        etStock.setText(String.valueOf(food.getStock()));

        builder.setView(view)
                .setTitle("Cập nhật kho: " + food.getName())
                .setPositiveButton("Cập nhật", (d, w) -> {
                    try {
                        int newStock = Integer.parseInt(etStock.getText().toString());
                        vm.updateStock(food.getId(), newStock);
                    } catch (Exception e) {
                        Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
