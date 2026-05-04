package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.view.adapter.ProductAdapter;
import com.example.petshop.viewmodel.CartViewModel;

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
    private CartViewModel cartViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        initViews(title);
        loadProducts(category);
        observeCart();
    }

    private void initViews(String title) {
        ((TextView) findViewById(R.id.tvTitle)).setText(
                title != null ? title : "Tất cả sản phẩm");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvProducts = findViewById(R.id.rvProducts);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(new ArrayList<>(), product -> openProductDetail(product),
                product -> addToCart(product));
        rvProducts.setAdapter(adapter);
    }

    private void openProductDetail(Object product) {
        Intent i;
        if (product instanceof Pet) {
            i = new Intent(this, PetDetailActivity.class);
            i.putExtra(PetDetailActivity.EXTRA_PET_ID, ((Pet) product).getId());
        } else if (product instanceof Food) {
            i = new Intent(this, FoodDetailActivity.class);
            i.putExtra(FoodDetailActivity.EXTRA_FOOD_ID, ((Food) product).getId());
        } else {
            return;
        }
        startActivity(i);
    }

    private void addToCart(Object product) {
        if (product instanceof Pet) {
            cartViewModel.addPet((Pet) product);
        } else if (product instanceof Food) {
            cartViewModel.addFood((Food) product, 1);
        }
    }

    private void observeCart() {
        cartViewModel.getSuccess().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        cartViewModel.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProducts(String category) {
        progressBar.setVisibility(View.VISIBLE);
        rvProducts.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        if (CATEGORY_PET.equals(category)) {
            loadPets();
        } else if (CATEGORY_FOOD.equals(category)) {
            loadFoods();
        } else {
            loadAllProducts();
        }
    }

    private void loadPets() {
        new PetRepository().getAll(new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                runOnUiThread(() -> displayProducts(new ArrayList<>(data)));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showEmpty());
            }
        });
    }

    private void loadFoods() {
        new FoodRepository().getAll(new FoodRepository.Callback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> data) {
                runOnUiThread(() -> displayProducts(new ArrayList<>(data)));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showEmpty());
            }
        });
    }

    private void loadAllProducts() {
        List<Object> allProducts = new ArrayList<>();
        final int[] loadedCount = {0};

        PetRepository.Callback<List<Pet>> petCallback = new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                synchronized (allProducts) {
                    if (data != null) allProducts.addAll(data);
                }
                synchronized (loadedCount) {
                    loadedCount[0]++;
                    if (loadedCount[0] >= 2) {
                        runOnUiThread(() -> displayProducts(allProducts));
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                synchronized (loadedCount) {
                    loadedCount[0]++;
                    if (loadedCount[0] >= 2) {
                        runOnUiThread(() -> displayProducts(allProducts));
                    }
                }
            }
        };

        FoodRepository.Callback<List<Food>> foodCallback = new FoodRepository.Callback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> data) {
                synchronized (allProducts) {
                    if (data != null) allProducts.addAll(data);
                }
                synchronized (loadedCount) {
                    loadedCount[0]++;
                    if (loadedCount[0] >= 2) {
                        runOnUiThread(() -> displayProducts(allProducts));
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                synchronized (loadedCount) {
                    loadedCount[0]++;
                    if (loadedCount[0] >= 2) {
                        runOnUiThread(() -> displayProducts(allProducts));
                    }
                }
            }
        };

        new PetRepository().getAll(petCallback);
        new FoodRepository().getAll(foodCallback);
    }

    private void displayProducts(List<Object> data) {
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
