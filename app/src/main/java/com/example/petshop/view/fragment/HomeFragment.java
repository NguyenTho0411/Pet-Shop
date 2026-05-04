package com.example.petshop.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Category;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.utils.SessionManager;
import com.example.petshop.view.activity.CartActivity;
import com.example.petshop.view.activity.FoodDetailActivity;
import com.example.petshop.view.activity.LoginActivity;
import com.example.petshop.view.activity.PetDetailActivity;
import com.example.petshop.view.activity.ProductListActivity;
import com.example.petshop.view.activity.PromotionActivity;
import com.example.petshop.view.adapter.HomeCategoryAdapter;
import com.example.petshop.view.adapter.HomeFoodAdapter;
import com.example.petshop.view.adapter.HomePetAdapter;
import com.example.petshop.viewmodel.CartViewModel;
import com.example.petshop.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class HomeFragment extends Fragment {

    private HomeViewModel       vm;
    private CartViewModel       cartVm;
    private HomeCategoryAdapter categoryAdapter;
    private HomePetAdapter      petAdapter;
    private HomeFoodAdapter     foodAdapter;
    private TextView            tvGreeting, tvTimeGreeting;
    private Button              btnLoginTopBar;
    private EditText            etSearch;
    private View                rootView;
    private String currentCategoryType = null;
    private String currentCategoryName = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        cartVm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        bindViews(root);
        setupRecyclerViews(root);
        observeViewModel();
        vm.loadHomeData();
    }

    @Override public void onResume() { super.onResume(); updateTopBar(); }

    private void bindViews(View root) {
        tvGreeting     = root.findViewById(R.id.tvGreeting);
        tvTimeGreeting = root.findViewById(R.id.tvTimeGreeting);
        btnLoginTopBar = root.findViewById(R.id.btnLoginTopBar);

        btnLoginTopBar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));

        root.findViewById(R.id.btnCart).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CartActivity.class)));

        root.findViewById(R.id.cardPromo).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PromotionActivity.class)));

        root.findViewById(R.id.tvSeeAllPets).setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), ProductListActivity.class);
                if (currentCategoryName != null && Category.TYPE_PET.equals(currentCategoryType)) {
                    i.putExtra(ProductListActivity.EXTRA_TITLE, "Danh sách " + currentCategoryName);
                    i.putExtra(ProductListActivity.EXTRA_FILTER_KEY, currentCategoryName);
                } else {
                    i.putExtra(ProductListActivity.EXTRA_TITLE, "Thú cưng");
                }
                i.putExtra(ProductListActivity.EXTRA_CATEGORY, ProductListActivity.CATEGORY_PET);
                startActivity(i);
        });
        root.findViewById(R.id.tvSeeAllFood).setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), ProductListActivity.class);
                if (currentCategoryName != null && Category.TYPE_FOOD.equals(currentCategoryType)) {
                    i.putExtra(ProductListActivity.EXTRA_TITLE, "Danh sách " + currentCategoryName);
                    i.putExtra(ProductListActivity.EXTRA_FILTER_KEY, currentCategoryName);
                } else {
                    i.putExtra(ProductListActivity.EXTRA_TITLE, "Đồ ăn thú cưng");
                }
                i.putExtra(ProductListActivity.EXTRA_CATEGORY, ProductListActivity.CATEGORY_FOOD);
                startActivity(i);
        });

        etSearch = root.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                currentCategoryType = null;
                currentCategoryName = null;
                vm.search(s.toString());
                updateSectionTitles();
            }
        });

        updateTopBar();
    }

    private void setupRecyclerViews(View root) {
        RecyclerView rvCat = root.findViewById(R.id.rvCategories);
        rvCat.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new HomeCategoryAdapter(new ArrayList<>(), (cat, pos) -> {
            currentCategoryType = resolveCategoryType(cat);
            currentCategoryName = cat.getName();

            updateSectionTitles();
            vm.filterByCategory(cat);
        });
        rvCat.setAdapter(categoryAdapter);

        RecyclerView rvPets = root.findViewById(R.id.rvFeaturedPets);
        rvPets.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        petAdapter = new HomePetAdapter(new ArrayList<>(), this::openPetDetail, pet -> cartVm.addPet(pet));
        rvPets.setAdapter(petAdapter);

        RecyclerView rvFood = root.findViewById(R.id.rvFeaturedFood);
        rvFood.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        foodAdapter = new HomeFoodAdapter(new ArrayList<>(), this::openFoodDetail,
                food -> cartVm.addFood(food, 1));
        rvFood.setAdapter(foodAdapter);
    }

    private void observeViewModel() {
        vm.getCategories().observe(getViewLifecycleOwner(), cats -> categoryAdapter.updateList(cats));

        vm.getFeaturedPets().observe(getViewLifecycleOwner(), this::renderPets);
        vm.getFeaturedFoods().observe(getViewLifecycleOwner(), this::renderFoods);

        vm.getIsSearching().observe(getViewLifecycleOwner(), isSearching -> updateSectionTitles());

        cartVm.getSuccess().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                cartVm.loadCart();
            }
        });
        cartVm.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateTopBar() {
        if (tvGreeting == null) return;

        // Initialize SessionManager if needed
        if (getContext() != null) {
            SessionManager session = SessionManager.getInstance(requireContext());
            tvTimeGreeting.setText(getTimeGreeting());

            if (session.isLoggedIn()) {
                String name = session.getUserName();
                if (name == null || name.isEmpty() || name.isBlank()) {
                    name = "bạn";
                } else {
                    name = name.split(" ")[0];
                }
                tvGreeting.setText("Hi, " + name + " 🐾");
                btnLoginTopBar.setVisibility(View.GONE);
            } else {
                tvGreeting.setText("Hi, bạn ơi 🐾");
                btnLoginTopBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getTimeGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5  && h < 12) return getString(R.string.good_morning);
        if (h >= 12 && h < 18) return getString(R.string.good_afternoon);
        return getString(R.string.good_evening);
    }

    private void renderPets(java.util.List<Pet> pets) {
        petAdapter.updateList(pets);

        View petHeader = (View) rootView.findViewById(R.id.tvFeaturedPetsTitle).getParent();
        boolean hidePetSection = Category.TYPE_FOOD.equals(currentCategoryType);

        if (hidePetSection) {
            petHeader.setVisibility(View.GONE);
            rootView.findViewById(R.id.rvFeaturedPets).setVisibility(View.GONE);
            rootView.findViewById(R.id.tvEmptyPets).setVisibility(View.GONE);
            return;
        }

        petHeader.setVisibility(View.VISIBLE);

        boolean empty = pets == null || pets.isEmpty();

        rootView.findViewById(R.id.rvFeaturedPets)
                .setVisibility(empty ? View.GONE : View.VISIBLE);

        rootView.findViewById(R.id.tvEmptyPets)
                .setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderFoods(java.util.List<Food> foods) {
        foodAdapter.updateList(foods);

        View foodHeader = (View) rootView.findViewById(R.id.tvFeaturedFoodTitle).getParent();
        boolean hideFoodSection = Category.TYPE_PET.equals(currentCategoryType);

        if (hideFoodSection) {
            foodHeader.setVisibility(View.GONE);
            rootView.findViewById(R.id.rvFeaturedFood).setVisibility(View.GONE);
            rootView.findViewById(R.id.tvEmptyFood).setVisibility(View.GONE);
            return;
        }

        foodHeader.setVisibility(View.VISIBLE);

        boolean empty = foods == null || foods.isEmpty();

        rootView.findViewById(R.id.rvFeaturedFood)
                .setVisibility(empty ? View.GONE : View.VISIBLE);

        rootView.findViewById(R.id.tvEmptyFood)
                .setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void updateSectionTitles() {
        TextView tvPetTitle = rootView.findViewById(R.id.tvFeaturedPetsTitle);
        TextView tvFoodTitle = rootView.findViewById(R.id.tvFeaturedFoodTitle);
        TextView tvSeeAllPets = rootView.findViewById(R.id.tvSeeAllPets);
        TextView tvSeeAllFood = rootView.findViewById(R.id.tvSeeAllFood);

        if (Category.TYPE_PET.equals(currentCategoryType)) {
            if (tvPetTitle != null) {
                tvPetTitle.setText(currentCategoryName != null
                        ? "Danh sách " + currentCategoryName
                        : "Danh sách thú cưng");
            }
            if (tvSeeAllPets != null) tvSeeAllPets.setVisibility(View.GONE);
            return;
        }

        if (Category.TYPE_FOOD.equals(currentCategoryType)) {
            if (tvFoodTitle != null) {
                tvFoodTitle.setText(currentCategoryName != null
                        ? "Danh sách " + currentCategoryName
                        : "Danh sách đồ ăn");
            }
            if (tvSeeAllFood != null) tvSeeAllFood.setVisibility(View.GONE);
            return;
        }

        // Hiện lại nút Xem tất cả khi không chọn danh mục cụ thể
        if (tvSeeAllPets != null) tvSeeAllPets.setVisibility(View.VISIBLE);
        if (tvSeeAllFood != null) tvSeeAllFood.setVisibility(View.VISIBLE);

        Boolean isSearching = vm.getIsSearching().getValue();

        if (Boolean.TRUE.equals(isSearching)) {
            if (tvPetTitle != null) tvPetTitle.setText("Kết quả tìm kiếm thú cưng");
            if (tvFoodTitle != null) tvFoodTitle.setText("Kết quả tìm kiếm thức ăn");
        } else {
            if (tvPetTitle != null) tvPetTitle.setText(R.string.featured_pets);
            if (tvFoodTitle != null) tvFoodTitle.setText(R.string.pet_food);
        }
    }

    private String resolveCategoryType(Category category) {
        if (category == null) return null;

        if (Category.TYPE_FOOD.equals(category.getType())) {
            return Category.TYPE_FOOD;
        }

        if (Category.TYPE_PET.equals(category.getType())) {
            return Category.TYPE_PET;
        }

        String name = category.getName() != null ? category.getName().toLowerCase() : "";
        String id = category.getId() != null ? category.getId().toLowerCase() : "";
        String key = name + " " + id;

        if (key.contains("pate")
                || key.contains("wet")
                || key.contains("dry")
                || key.contains("snack")
                || key.contains("milk")
                || key.contains("food")) {
            return Category.TYPE_FOOD;
        }

        return Category.TYPE_PET;
    }

    private void openPetDetail(Pet pet) {
        Intent i = new Intent(requireContext(), PetDetailActivity.class);
        i.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
        startActivity(i);
    }

    private void openFoodDetail(Food food) {
        Intent i = new Intent(requireContext(), FoodDetailActivity.class);
        i.putExtra(FoodDetailActivity.EXTRA_FOOD_ID, food.getId());
        startActivity(i);
    }
    public void focusSearch() {
        if (etSearch == null) return;

        etSearch.requestFocus();
        etSearch.post(() -> {
            if (etSearch.getText() != null) {
                etSearch.setSelection(etSearch.getText().length());
            }

            InputMethodManager imm =
                    (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}
