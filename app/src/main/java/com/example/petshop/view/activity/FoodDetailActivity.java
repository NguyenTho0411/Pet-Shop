package com.example.petshop.view.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.viewmodel.CartViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class FoodDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ID = "food_id";

    private final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    private ImageView ivHero, ivFavoriteIcon;
    private TextView  tvFoodName, tvBrand, tvStockBadge, tvTargetPet;
    private TextView  tvWeightVal, tvFoodTypeVal, tvSuitableAgeVal;
    private TextView  tvPrice, tvOriginalPrice, tvStock, tvOrigin, tvSaleBadge;
    private TextView  tvDescription, tvIngredients, tvUsageGuide, tvStorageGuide;
    private TextView  tvQty, btnDecQty, btnIncQty;

    private Food currentFood;
    private int  qty = 1;
    private boolean isFavorite = false;
    private CartViewModel cartVm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        cartVm = new ViewModelProvider(this).get(CartViewModel.class);

        String foodId = getIntent().getStringExtra(EXTRA_FOOD_ID);
        if (foodId == null) { finish(); return; }

        bindViews();
        observeViewModel();
        loadFood(foodId);
    }

    private void observeViewModel() {
        cartVm.getSuccess().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        cartVm.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    private void bindViews() {
        ivHero          = findViewById(R.id.ivFoodHero);
        ivFavoriteIcon  = findViewById(R.id.ivFavoriteIcon);
        tvFoodName      = findViewById(R.id.tvFoodName);
        tvBrand         = findViewById(R.id.tvBrand);
        tvStockBadge    = findViewById(R.id.tvStockBadge);
        tvTargetPet     = findViewById(R.id.tvTargetPet);
        tvWeightVal     = findViewById(R.id.tvWeightVal);
        tvFoodTypeVal   = findViewById(R.id.tvFoodTypeVal);
        tvSuitableAgeVal= findViewById(R.id.tvSuitableAgeVal);
        tvPrice         = findViewById(R.id.tvPrice);
        tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        tvStock         = findViewById(R.id.tvStock);
        tvOrigin        = findViewById(R.id.tvOrigin);
        tvSaleBadge     = findViewById(R.id.tvSaleBadge);
        tvDescription   = findViewById(R.id.tvDescription);
        tvIngredients   = findViewById(R.id.tvIngredients);
        tvUsageGuide    = findViewById(R.id.tvUsageGuide);
        tvStorageGuide  = findViewById(R.id.tvStorageGuide);
        tvQty           = findViewById(R.id.tvQty);
        btnDecQty       = findViewById(R.id.btnDecQty);
        btnIncQty       = findViewById(R.id.btnIncQty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFavorite).setOnClickListener(v -> toggleFavorite());

        btnDecQty.setOnClickListener(v -> { if (qty > 1) { qty--; tvQty.setText(String.valueOf(qty)); } });
        btnIncQty.setOnClickListener(v -> {
            if (currentFood != null && qty < currentFood.getStock()) {
                qty++;
                tvQty.setText(String.valueOf(qty));
            }
        });

        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            if (FirebaseHelper.getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (currentFood != null) {
                cartVm.addFood(currentFood, qty);
            }
        });

        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            if (FirebaseHelper.getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (currentFood != null) {
                cartVm.addFood(currentFood, qty);
                startActivity(new Intent(this, CartActivity.class));
            }
        });

        // Add Chat button click if it exists in food detail (usually it does)
        View btnChat = findViewById(R.id.btnChat);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                if (FirebaseHelper.getCurrentUser() == null) {
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    startActivity(new Intent(this, ChatActivity.class));
                }
            });
        }
    }

    private void loadFood(String foodId) {
        new FoodRepository().getById(foodId, new FoodRepository.Callback<>() {
            public void onSuccess(Food food) {
                if (food == null) { finish(); return; }

                // Áp dụng khuyến mãi nếu có
                new com.example.petshop.repository.PromotionRepository().getActive(new com.example.petshop.repository.PromotionRepository.Callback<>() {
                    @Override
                    public void onSuccess(java.util.List<com.example.petshop.model.entity.Promotion> data) {
                        java.util.List<Food> list = new java.util.ArrayList<>();
                        list.add(food);
                        com.example.petshop.utils.PromotionManager.applyPromotions(null, list, data);
                        currentFood = food;
                        runOnUiThread(() -> bindFood(food));
                    }
                    @Override public void onFailure(String error) {
                        currentFood = food;
                        runOnUiThread(() -> bindFood(food));
                    }
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> { Toast.makeText(FoodDetailActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show(); finish(); });
            }
        });
    }

    private void bindFood(Food food) {
        // Hero image
        if (food.getThumbnailUrl() != null && !food.getThumbnailUrl().isEmpty()) {
            Glide.with(this).load(food.getThumbnailUrl()).centerCrop().into(ivHero);
        }

        tvFoodName.setText(food.getName());
        tvBrand.setText(food.getBrand() != null ? food.getBrand() : "");

        // Stock
        if (food.getStock() <= 0 || Food.STATUS_OUT_OF_STOCK.equals(food.getStatus())) {
            tvStockBadge.setText("Hết hàng");
            tvStockBadge.getBackground().setTint(getColor(R.color.status_error));
            findViewById(R.id.btnAddToCart).setEnabled(false);
        } else {
            tvStockBadge.setText("Còn hàng");
        }

        // Target pet
        tvTargetPet.setText(targetPetVi(food.getTargetPetType()));

        // Attribute chips
        String wt = food.getWeightGram() > 0
                ? (food.getWeightGram() >= 1000 ? food.getWeightGram() / 1000.0 + " kg" : food.getWeightGram() + " g")
                : (food.getUnit() != null ? food.getUnit() : "—");
        tvWeightVal.setText(wt);
        tvFoodTypeVal.setText(foodTypeVi(food.getFoodType()));
        tvSuitableAgeVal.setText(suitableAgeVi(food.getSuitableAge()));

        // Price
        double price = food.getEffectivePrice();
        tvPrice.setText(price > 0 ? VND.format((long) price) + "đ" : "Liên hệ");
        
        if (food.hasPromotion() && food.getOriginalPrice() > 0 && food.getOriginalPrice() > food.getEffectivePrice()) {
            // Giá gốc gạch ngang
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(VND.format((long) food.getOriginalPrice()) + "đ");
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Badge %
            int pct = (int) Math.round((1 - food.getEffectivePrice() / food.getOriginalPrice()) * 100);
            tvSaleBadge.setText(" GIẢM " + pct + "% ");
            tvSaleBadge.setVisibility(View.VISIBLE);
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
            tvSaleBadge.setVisibility(View.GONE);
        }

        // Stock count
        tvStock.setText("Kho: " + food.getStock() + " " + (food.getUnit() != null ? food.getUnit() : "sản phẩm"));
        tvOrigin.setText(food.getOrigin() != null ? "Xuất xứ: " + food.getOrigin() : "");

        // Sale badge
        if (food.hasPromotion()) {
            tvSaleBadge.setVisibility(View.VISIBLE);
            if (food.getOriginalPrice() > 0 && food.getDiscountedPrice() > 0) {
                int pct = (int) ((1 - food.getDiscountedPrice() / food.getOriginalPrice()) * 100);
                tvSaleBadge.setText("-" + pct + "%");
            }
        }

        // Text sections
        tvDescription.setText(orEmpty(food.getDescription()));
        tvIngredients.setText(orEmpty(food.getIngredients()));
        tvUsageGuide.setText(orEmpty(food.getUsageGuide()));
        tvStorageGuide.setText(food.getStorageGuide() != null
                ? "🗄 Bảo quản: " + food.getStorageGuide() : "");
    }

    private void toggleFavorite() {
        if (FirebaseHelper.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        isFavorite = !isFavorite;
        ivFavoriteIcon.setColorFilter(isFavorite ? getColor(R.color.badge_red) : getColor(R.color.text_hint));
        Toast.makeText(this, isFavorite ? "Đã yêu thích ❤️" : "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
    }

    private String targetPetVi(String t) {
        if (t == null) return "Cho mọi thú cưng";
        switch (t) {
            case "DOG":    return "🐕 Dành cho Chó";
            case "CAT":    return "🐈 Dành cho Mèo";
            case "FISH":   return "🐟 Dành cho Cá";
            case "BIRD":   return "🐦 Dành cho Chim";
            case "RABBIT": return "🐰 Dành cho Thỏ";
            default:       return "🐾 Cho mọi thú cưng";
        }
    }

    private String foodTypeVi(String t) {
        if (t == null) return "—";
        switch (t) {
            case "DRY":    return "Hạt khô";
            case "WET":    return "Pate";
            case "SNACK":  return "Bánh thưởng";
            case "MILK":   return "Sữa";
            case "SUPPLEMENT": return "Thực phẩm bổ sung";
            default:       return t;
        }
    }

    private String suitableAgeVi(String a) {
        if (a == null) return "Mọi độ tuổi";
        switch (a) {
            case "PUPPY":  return "Non / Nhỏ";
            case "ADULT":  return "Trưởng thành";
            case "SENIOR": return "Cao tuổi";
            default:       return "Mọi độ tuổi";
        }
    }

    private String orEmpty(String s) { return s != null ? s : ""; }
}
