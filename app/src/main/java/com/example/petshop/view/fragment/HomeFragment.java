package com.example.petshop.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Category;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.repository.NotificationRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.activity.CartActivity;
import com.example.petshop.view.activity.FoodDetailActivity;
import com.example.petshop.view.activity.LoginActivity;
import com.example.petshop.view.activity.NotificationActivity;
import com.example.petshop.view.activity.PetDetailActivity;
import com.example.petshop.view.adapter.HomeCategoryAdapter;
import com.example.petshop.view.adapter.HomeFoodAdapter;
import com.example.petshop.view.adapter.HomePetAdapter;
import com.example.petshop.viewmodel.CartViewModel;
import com.example.petshop.viewmodel.HomeViewModel;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel vm;
    private CartViewModel cartVm;

    private HomeCategoryAdapter categoryAdapter;
    private HomePetAdapter petAdapter;
    private HomeFoodAdapter foodAdapter;

    private TextView tvGreeting;
    private TextView tvTimeGreeting;
    private Button btnLoginTopBar;
    private EditText etSearch;
    private View rootView;

    private String currentCategoryType = null;
    private String currentCategoryName = null;

    @Nullable
    @Override
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

    @Override
    public void onResume() {
        super.onResume();
        updateTopBar();
        updateNotificationDot();
    }

    private void bindViews(View root) {
        tvGreeting = root.findViewById(R.id.tvGreeting);
        tvTimeGreeting = root.findViewById(R.id.tvTimeGreeting);
        btnLoginTopBar = root.findViewById(R.id.btnLoginTopBar);
        etSearch = root.findViewById(R.id.etSearch);

        btnLoginTopBar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class))
        );

        root.findViewById(R.id.btnCart).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CartActivity.class))
        );

        root.findViewById(R.id.btnNotification).setOnClickListener(v -> {
            if (FirebaseHelper.getCurrentUser() == null) {
                startActivity(new Intent(requireContext(), LoginActivity.class));
                return;
            }

            startActivity(new Intent(requireContext(), NotificationActivity.class));
        });

        root.findViewById(R.id.tvSeeAllPets).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Xem tất cả thú cưng", Toast.LENGTH_SHORT).show()
        );

        root.findViewById(R.id.tvSeeAllFood).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Xem tất cả đồ ăn", Toast.LENGTH_SHORT).show()
        );

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
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
        rvCat.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        categoryAdapter = new HomeCategoryAdapter(new ArrayList<>(), (cat, pos) -> {
            currentCategoryType = resolveCategoryType(cat);
            currentCategoryName = cat != null ? cat.getName() : null;

            updateSectionTitles();
            vm.filterByCategory(cat);
        });

        rvCat.setAdapter(categoryAdapter);

        RecyclerView rvPets = root.findViewById(R.id.rvFeaturedPets);
        rvPets.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        petAdapter = new HomePetAdapter(
                new ArrayList<>(),
                this::openPetDetail,
                pet -> cartVm.addPet(pet)
        );

        rvPets.setAdapter(petAdapter);

        RecyclerView rvFood = root.findViewById(R.id.rvFeaturedFood);
        rvFood.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        foodAdapter = new HomeFoodAdapter(
                new ArrayList<>(),
                this::openFoodDetail,
                food -> cartVm.addFood(food, 1)
        );

        rvFood.setAdapter(foodAdapter);
    }

    private void observeViewModel() {
        vm.getCategories().observe(getViewLifecycleOwner(), cats -> {
            if (categoryAdapter != null) {
                categoryAdapter.updateList(cats != null ? cats : new ArrayList<>());
            }
        });

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
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderPets(List<Pet> pets) {
        if (petAdapter != null) {
            petAdapter.updateList(pets != null ? pets : new ArrayList<>());
        }

        if (rootView == null) return;

        View petTitle = rootView.findViewById(R.id.tvFeaturedPetsTitle);
        View petHeader = petTitle != null ? (View) petTitle.getParent() : null;
        View rvPets = rootView.findViewById(R.id.rvFeaturedPets);
        View tvEmptyPets = rootView.findViewById(R.id.tvEmptyPets);

        boolean hidePetSection = Category.TYPE_FOOD.equals(currentCategoryType);

        if (hidePetSection) {
            if (petHeader != null) petHeader.setVisibility(View.GONE);
            if (rvPets != null) rvPets.setVisibility(View.GONE);
            if (tvEmptyPets != null) tvEmptyPets.setVisibility(View.GONE);
            return;
        }

        if (petHeader != null) petHeader.setVisibility(View.VISIBLE);

        boolean empty = pets == null || pets.isEmpty();

        if (rvPets != null) {
            rvPets.setVisibility(empty ? View.GONE : View.VISIBLE);
        }

        if (tvEmptyPets != null) {
            tvEmptyPets.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private void renderFoods(List<Food> foods) {
        if (foodAdapter != null) {
            foodAdapter.updateList(foods != null ? foods : new ArrayList<>());
        }

        if (rootView == null) return;

        View foodTitle = rootView.findViewById(R.id.tvFeaturedFoodTitle);
        View foodHeader = foodTitle != null ? (View) foodTitle.getParent() : null;
        View rvFood = rootView.findViewById(R.id.rvFeaturedFood);
        View tvEmptyFood = rootView.findViewById(R.id.tvEmptyFood);

        boolean hideFoodSection = Category.TYPE_PET.equals(currentCategoryType);

        if (hideFoodSection) {
            if (foodHeader != null) foodHeader.setVisibility(View.GONE);
            if (rvFood != null) rvFood.setVisibility(View.GONE);
            if (tvEmptyFood != null) tvEmptyFood.setVisibility(View.GONE);
            return;
        }

        if (foodHeader != null) foodHeader.setVisibility(View.VISIBLE);

        boolean empty = foods == null || foods.isEmpty();

        if (rvFood != null) {
            rvFood.setVisibility(empty ? View.GONE : View.VISIBLE);
        }

        if (tvEmptyFood != null) {
            tvEmptyFood.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSectionTitles() {
        if (rootView == null || vm == null) return;

        TextView tvPetTitle = rootView.findViewById(R.id.tvFeaturedPetsTitle);
        TextView tvFoodTitle = rootView.findViewById(R.id.tvFeaturedFoodTitle);

        if (Category.TYPE_PET.equals(currentCategoryType)) {
            if (tvPetTitle != null) {
                tvPetTitle.setText(currentCategoryName != null
                        ? "Danh sách " + currentCategoryName
                        : "Danh sách thú cưng");
            }
            return;
        }

        if (Category.TYPE_FOOD.equals(currentCategoryType)) {
            if (tvFoodTitle != null) {
                tvFoodTitle.setText(currentCategoryName != null
                        ? "Danh sách " + currentCategoryName
                        : "Danh sách đồ ăn");
            }
            return;
        }

        Boolean isSearching = vm.getIsSearching().getValue();

        if (Boolean.TRUE.equals(isSearching)) {
            if (tvPetTitle != null) {
                tvPetTitle.setText("Kết quả tìm kiếm thú cưng");
            }

            if (tvFoodTitle != null) {
                tvFoodTitle.setText("Kết quả tìm kiếm thức ăn");
            }
        } else {
            if (tvPetTitle != null) {
                tvPetTitle.setText(R.string.featured_pets);
            }

            if (tvFoodTitle != null) {
                tvFoodTitle.setText(R.string.pet_food);
            }
        }
    }

    private void updateTopBar() {
        if (tvGreeting == null || tvTimeGreeting == null || btnLoginTopBar == null) return;

        tvTimeGreeting.setText(getTimeGreeting());

        FirebaseUser user = FirebaseHelper.getCurrentUser();

        if (user != null) {
            String name = user.getDisplayName();

            if (name == null || name.isEmpty()) {
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

    private String getTimeGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (h >= 5 && h < 12) {
            return getString(R.string.good_morning);
        }

        if (h >= 12 && h < 18) {
            return getString(R.string.good_afternoon);
        }

        return getString(R.string.good_evening);
    }

    private String resolveCategoryType(Category category) {
        if (category == null) return null;

        if (Category.TYPE_FOOD.equalsIgnoreCase(category.getType())) {
            return Category.TYPE_FOOD;
        }

        if (Category.TYPE_PET.equalsIgnoreCase(category.getType())) {
            return Category.TYPE_PET;
        }

        String name = category.getName() != null
                ? category.getName().toLowerCase(Locale.ROOT)
                : "";

        String id = category.getId() != null
                ? category.getId().toLowerCase(Locale.ROOT)
                : "";

        String key = name + " " + id;

        if (key.contains("pate")
                || key.contains("wet")
                || key.contains("dry")
                || key.contains("snack")
                || key.contains("milk")
                || key.contains("food")
                || key.contains("thức ăn")
                || key.contains("do an")
                || key.contains("đồ ăn")) {
            return Category.TYPE_FOOD;
        }

        return Category.TYPE_PET;
    }

    private void openPetDetail(Pet pet) {
        if (pet == null || pet.getId() == null) return;

        Intent i = new Intent(requireContext(), PetDetailActivity.class);
        i.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
        startActivity(i);
    }

    private void openFoodDetail(Food food) {
        if (food == null || food.getId() == null) return;

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
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void updateNotificationDot() {
        if (rootView == null || !isAdded()) return;

        View dot = rootView.findViewById(R.id.vNotificationDot);
        if (dot == null) return;

        if (FirebaseHelper.getCurrentUser() == null) {
            dot.setVisibility(View.GONE);
            return;
        }

        String uid = FirebaseHelper.getCurrentUser().getUid();

        new NotificationRepository().getUnreadCount(
                requireContext(),
                uid,
                new NotificationRepository.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        if (!isAdded()) return;

                        requireActivity().runOnUiThread(() ->
                                dot.setVisibility(count != null && count > 0
                                        ? View.VISIBLE
                                        : View.GONE)
                        );
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()) return;

                        requireActivity().runOnUiThread(() ->
                                dot.setVisibility(View.GONE)
                        );
                    }
                }
        );
    }
}