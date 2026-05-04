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
import com.example.petshop.repository.PromotionRepository;
import com.example.petshop.utils.PromotionManager;
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
    public static final String EXTRA_FILTER_KEY = "filter_key";

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
        String filterKey = getIntent().getStringExtra(EXTRA_FILTER_KEY);

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        initViews(title);
        loadProducts(category, filterKey);
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

    private void loadProducts(String category, String filterKey) {
        progressBar.setVisibility(View.VISIBLE);
        rvProducts.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        if (CATEGORY_PET.equals(category)) {
            loadPets(filterKey);
        } else if (CATEGORY_FOOD.equals(category)) {
            loadFoods(filterKey);
        } else {
            loadAllProducts(filterKey);
        }
    }

    private void loadPets(String filterKey) {
        new PetRepository().getAll(new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                List<Pet> filtered = filterPets(data, filterKey);
                applyPromotionsAndDisplay(filtered, null);
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showEmpty());
            }
        });
    }

    private void loadFoods(String filterKey) {
        new FoodRepository().getAll(new FoodRepository.Callback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> data) {
                List<Food> filtered = filterFoods(data, filterKey);
                applyPromotionsAndDisplay(null, filtered);
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showEmpty());
            }
        });
    }

    private void loadAllProducts(String filterKey) {
        List<Pet> loadedPets = new ArrayList<>();
        List<Food> loadedFoods = new ArrayList<>();
        final int[] loadedCount = {0};

        PetRepository.Callback<List<Pet>> petCallback = new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                synchronized (loadedPets) {
                    if (data != null) {
                        loadedPets.addAll(filterPets(data, filterKey));
                    }
                }
                checkAllLoaded(loadedCount, loadedPets, loadedFoods);
            }
            @Override
            public void onFailure(String error) {
                checkAllLoaded(loadedCount, loadedPets, loadedFoods);
            }
        };

        FoodRepository.Callback<List<Food>> foodCallback = new FoodRepository.Callback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> data) {
                synchronized (loadedFoods) {
                    if (data != null) {
                        loadedFoods.addAll(filterFoods(data, filterKey));
                    }
                }
                checkAllLoaded(loadedCount, loadedPets, loadedFoods);
            }
            @Override
            public void onFailure(String error) {
                checkAllLoaded(loadedCount, loadedPets, loadedFoods);
            }
        };

        new PetRepository().getAll(petCallback);
        new FoodRepository().getAll(foodCallback);
    }

    private List<Pet> filterPets(List<Pet> pets, String filterKey) {
        if (filterKey == null || filterKey.trim().isEmpty() || pets == null) return pets != null ? new ArrayList<>(pets) : new ArrayList<>();
        
        String key = filterKey.toLowerCase(java.util.Locale.ROOT);
        List<Pet> result = new ArrayList<>();
        for (Pet p : pets) {
            boolean matches = false;
            if (p.getSpecies() != null && p.getSpecies().toLowerCase(java.util.Locale.ROOT).contains(key)) matches = true;
            if (p.getCategoryId() != null && p.getCategoryId().toLowerCase(java.util.Locale.ROOT).contains(key)) matches = true;
            if (p.getCategory() != null && p.getCategory().getName() != null && p.getCategory().getName().toLowerCase(java.util.Locale.ROOT).contains(key)) matches = true;
            
            // Dịch ngầm từ tiếng Việt sang tiếng Anh cho các loài phổ biến
            if (!matches) {
                String translated = "";
                if (key.contains("chó")) translated = "dog";
                else if (key.contains("mèo")) translated = "cat";
                else if (key.contains("cá")) translated = "fish";
                else if (key.contains("chim")) translated = "bird";
                else if (key.contains("thỏ")) translated = "rabbit";
                
                if (!translated.isEmpty() && p.getSpecies() != null && p.getSpecies().toLowerCase(java.util.Locale.ROOT).contains(translated)) {
                    matches = true;
                }
            }
            
            if (matches) result.add(p);
        }
        return result;
    }

    private List<Food> filterFoods(List<Food> foods, String filterKey) {
        if (filterKey == null || filterKey.trim().isEmpty() || foods == null) return foods != null ? new ArrayList<>(foods) : new ArrayList<>();
        
        String key = filterKey.toLowerCase(java.util.Locale.ROOT);
        List<Food> result = new ArrayList<>();
        for (Food f : foods) {
            boolean matches = false;
            if (f.getTargetPetType() != null && f.getTargetPetType().toLowerCase(java.util.Locale.ROOT).contains(key)) matches = true;
            if (f.getCategoryId() != null && f.getCategoryId().toLowerCase(java.util.Locale.ROOT).contains(key)) matches = true;
            if (f.getCategory() != null && f.getCategory().getName() != null && f.getCategory().getName().toLowerCase(java.util.Locale.ROOT).contains(key)) matches = true;
            
            // Dịch ngầm
            if (!matches) {
                String translated = "";
                if (key.contains("chó")) translated = "dog";
                else if (key.contains("mèo")) translated = "cat";
                
                if (!translated.isEmpty() && f.getTargetPetType() != null && f.getTargetPetType().toLowerCase(java.util.Locale.ROOT).contains(translated)) {
                    matches = true;
                }
            }
            
            if (matches) result.add(f);
        }
        return result;
    }

    private void checkAllLoaded(int[] loadedCount, List<Pet> pets, List<Food> foods) {
        synchronized (loadedCount) {
            loadedCount[0]++;
            if (loadedCount[0] >= 2) {
                applyPromotionsAndDisplay(pets, foods);
            }
        }
    }

    private void applyPromotionsAndDisplay(List<Pet> pets, List<Food> foods) {
        new PromotionRepository().getActive(new PromotionRepository.Callback<>() {
            @Override
            public void onSuccess(java.util.List<com.example.petshop.model.entity.Promotion> activePromos) {
                PromotionManager.applyPromotions(pets, foods, activePromos);
                
                List<Object> allProducts = new ArrayList<>();
                if (pets != null) allProducts.addAll(pets);
                if (foods != null) allProducts.addAll(foods);
                
                runOnUiThread(() -> displayProducts(allProducts));
            }

            @Override
            public void onFailure(String error) {
                List<Object> allProducts = new ArrayList<>();
                if (pets != null) allProducts.addAll(pets);
                if (foods != null) allProducts.addAll(foods);
                
                runOnUiThread(() -> displayProducts(allProducts));
            }
        });
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
