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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.activity.CartActivity;
import com.example.petshop.view.activity.FoodDetailActivity;
import com.example.petshop.view.activity.LoginActivity;
import com.example.petshop.view.activity.PetDetailActivity;
import com.example.petshop.view.adapter.HomeCategoryAdapter;
import com.example.petshop.view.adapter.HomeFoodAdapter;
import com.example.petshop.view.adapter.HomePetAdapter;
import com.example.petshop.viewmodel.CartViewModel;
import com.example.petshop.viewmodel.HomeViewModel;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    private HomeViewModel       vm;
    private CartViewModel       cartVm;
    private HomeCategoryAdapter categoryAdapter;
    private HomePetAdapter      petAdapter;
    private HomeFoodAdapter     foodAdapter;
    private TextView            tvGreeting, tvTimeGreeting;
    private Button              btnLoginTopBar;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
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

        root.findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Thông báo — sắp ra mắt", Toast.LENGTH_SHORT).show());
        root.findViewById(R.id.tvSeeAllPets).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Xem tất cả thú cưng", Toast.LENGTH_SHORT).show());
        root.findViewById(R.id.tvSeeAllFood).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Xem tất cả đồ ăn", Toast.LENGTH_SHORT).show());

        ((EditText) root.findViewById(R.id.etSearch)).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) { vm.search(s.toString()); }
        });

        updateTopBar();
    }

    private void setupRecyclerViews(View root) {
        RecyclerView rvCat = root.findViewById(R.id.rvCategories);
        rvCat.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new HomeCategoryAdapter(new ArrayList<>(),
                (cat, pos) -> vm.filterByCategory(cat.getId(), cat.getType()));
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
        vm.getCategories().observe(getViewLifecycleOwner(),    cats  -> categoryAdapter.updateList(cats));
        vm.getFeaturedPets().observe(getViewLifecycleOwner(),  pets  -> petAdapter.updateList(pets));
        vm.getFeaturedFoods().observe(getViewLifecycleOwner(), foods -> foodAdapter.updateList(foods));

        cartVm.getSuccess().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                cartVm.loadCart(); // Optional: refresh cart state if needed
            }
        });
        cartVm.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateTopBar() {
        if (tvGreeting == null) return;
        tvTimeGreeting.setText(getTimeGreeting());
        FirebaseUser user = FirebaseHelper.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) name = "bạn";
            else name = name.split(" ")[0];
            tvGreeting.setText("Hi, " + name + " 🐾");
            btnLoginTopBar.setVisibility(View.GONE);
        } else {
            tvGreeting.setText("Hi, bạn ơi 🐾");
            btnLoginTopBar.setVisibility(View.VISIBLE);
        }
    }

    private String getTimeGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5  && h < 12) return getString(R.string.good_morning);
        if (h >= 12 && h < 18) return getString(R.string.good_afternoon);
        return getString(R.string.good_evening);
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
}
