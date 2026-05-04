package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Product;
import com.example.petshop.repository.ProductRepository;
import com.example.petshop.view.adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CATEGORY = "category";
    public static final String CATEGORY_ALL = "all";
    public static final String CATEGORY_PET = "pet";
    public static final String CATEGORY_FOOD = "food";

    private RecyclerView rvProducts;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);

        initViews(title);
        loadProducts(category);
    }

    private void initViews(String title) {
        ((TextView) findViewById(R.id.tvTitle)).setText(
                title != null ? title : "Tất cả sản phẩm");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvProducts = findViewById(R.id.rvProducts);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(new ArrayList<>(), product -> {
            // Open product detail
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(i);
        });
        rvProducts.setAdapter(adapter);
    }

    private void loadProducts(String category) {
        progressBar.setVisibility(View.VISIBLE);
        rvProducts.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ProductRepository repo = new ProductRepository();

        if (CATEGORY_PET.equals(category)) {
            repo.getProductsByCategory(Product.CATEGORY_PET, new ProductRepository.Callback<>() {
                @Override
                public void onSuccess(List<Product> data) {
                    runOnUiThread(() -> displayProducts(data));
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> showEmpty());
                }
            });
        } else if (CATEGORY_FOOD.equals(category)) {
            repo.getProductsByCategory(Product.CATEGORY_FOOD, new ProductRepository.Callback<>() {
                @Override
                public void onSuccess(List<Product> data) {
                    runOnUiThread(() -> displayProducts(data));
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> showEmpty());
                }
            });
        } else {
            repo.getAllProducts(new ProductRepository.Callback<>() {
                @Override
                public void onSuccess(List<Product> data) {
                    runOnUiThread(() -> displayProducts(data));
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> showEmpty());
                }
            });
        }
    }

    private void displayProducts(List<Product> data) {
        progressBar.setVisibility(View.GONE);
        if (data == null || data.isEmpty()) {
            showEmpty();
        } else {
            rvProducts.setVisibility(View.VISIBLE);
            adapter.updateData(data);
        }
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }
}
