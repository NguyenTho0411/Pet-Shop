package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.view.adapter.PetAdminAdapter;
import com.example.petshop.viewmodel.PetManageViewModel;

import java.util.ArrayList;
import java.util.List;

public class ManagePetsActivity extends AppCompatActivity {

    private PetManageViewModel vm;
    private PetAdminAdapter adapter;
    private List<Pet> allPets = new ArrayList<>();
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_pets);

        vm = new ViewModelProvider(this).get(PetManageViewModel.class);

        initViews();
        observeViewModel();
        vm.loadAll();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);

        RecyclerView rv = findViewById(R.id.rvPets);
        adapter = new PetAdminAdapter(new ArrayList<>(), new PetAdminAdapter.OnActionListener() {
            public void onEdit(Pet pet) {
                Intent intent = new Intent(ManagePetsActivity.this, AddEditPetActivity.class);
                intent.putExtra(AddEditPetActivity.EXTRA_PET_ID, pet.getId());
                startActivity(intent);
            }
            public void onDelete(Pet pet) {
                new AlertDialog.Builder(ManagePetsActivity.this)
                        .setMessage("Xoá \"" + pet.getName() + "\"? Không thể hoàn tác!")
                        .setPositiveButton("Xoá", (d, w) -> vm.deletePet(pet.getId()))
                        .setNegativeButton("Huỷ", null).show();
            }
            public void onChangeStatus(Pet pet, String status) {
                vm.updateStatus(pet.getId(), status);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditPetActivity.class));
        });

        findViewById(R.id.chipAll).setOnClickListener(v       -> filterPets("ALL"));
        findViewById(R.id.chipAvailable).setOnClickListener(v -> filterPets("Available"));
        findViewById(R.id.chipSold).setOnClickListener(v      -> filterPets("SOLD"));
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getPets().observe(this, pets -> {
            allPets = pets != null ? pets : new ArrayList<>();
            adapter.updateList(new ArrayList<>(allPets));
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void filterPets(String filter) {
        if ("ALL".equals(filter)) {
            adapter.updateList(new ArrayList<>(allPets));
        } else {
            List<Pet> filtered = new ArrayList<>();
            for (Pet p : allPets) {
                if (filter.equals(p.getStatus())) filtered.add(p);
            }
            adapter.updateList(filtered);
        }
    }
}
