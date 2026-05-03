package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.petshop.R;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.fragment.HomeFragment;
import com.example.petshop.view.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PetShopActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavView;

    // Fragment instances — reuse để giữ state khi switch tab
    private final HomeFragment    homeFragment    = new HomeFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_shop);

        bottomNavView = findViewById(R.id.bottomNavView);

        // Pre-add cả 2 fragment, chỉ hiện HomeFragment lúc đầu
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                .add(R.id.fragmentContainer, homeFragment,    "home")
                .commit();

        setupBottomNav();
    }

    private void setupBottomNav() {
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showFragment(homeFragment);
                return true;

            } else if (id == R.id.nav_search) {
                showFragment(homeFragment);
                homeFragment.focusSearch();
                return true;

            } else if (id == R.id.nav_chat) {
                if (!requireLogin()) return false;
                startActivity(new Intent(this, ChatActivity.class));
                return true;

            } else if (id == R.id.nav_orders) {
                if (!requireLogin()) return false;
                startActivity(new Intent(this, OrderHistoryActivity.class));
                return true;

            } else if (id == R.id.nav_profile) {
                if (!requireLogin()) {
                    // Nếu chưa login → về Home
                    bottomNavView.setSelectedItemId(R.id.nav_home);
                    return false;
                }
                showFragment(profileFragment);
                return true;
            }
            return false;
        });

        // Home tab được chọn mặc định
        bottomNavView.setSelectedItemId(R.id.nav_home);
    }

    private void showFragment(Fragment fragment) {
        if (activeFragment == fragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
    }

    /** @return true nếu đã login, false nếu chưa (và redirect sang Login) */
    private boolean requireLogin() {
        if (FirebaseHelper.getCurrentUser() != null) return true;
        startActivity(new Intent(this, LoginActivity.class));
        return false;
    }
}
