package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Category;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.repository.CategoryRepository;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.repository.PromotionRepository;
import com.example.petshop.utils.PromotionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {

    private final CategoryRepository  catRepo  = new CategoryRepository();
    private final PetRepository       petRepo  = new PetRepository();
    private final FoodRepository      foodRepo = new FoodRepository();
    private final PromotionRepository promoRepo = new PromotionRepository();

    private final MutableLiveData<List<Category>> categories    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Pet>>      filteredPets  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Food>>     filteredFoods = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>        isLoading     = new MutableLiveData<>(false);
    private final MutableLiveData<String>         error         = new MutableLiveData<>();
    private final MutableLiveData<Boolean>        isSearching   = new MutableLiveData<>(false);

    private List<Pet>  allPets  = new ArrayList<>();
    private List<Food> allFoods = new ArrayList<>();

    public LiveData<List<Category>> getCategories()    { return categories; }
    public LiveData<List<Pet>>      getFeaturedPets()  { return filteredPets; }
    public LiveData<List<Food>>     getFeaturedFoods() { return filteredFoods; }
    public LiveData<Boolean>        getLoading()       { return isLoading; }
    public LiveData<String>         getError()         { return error; }
    public LiveData<Boolean>        getIsSearching()   { return isSearching; }

    public void loadHomeData() {
        isLoading.setValue(true);
        isSearching.setValue(false);
        promoRepo.getActive(new PromotionRepository.Callback<List<Promotion>>() {
            @Override
            public void onSuccess(List<Promotion> promos) {
                loadMainData(promos);
            }
            @Override public void onFailure(String err) { loadMainData(null); }
        });
    }

    private void loadMainData(List<Promotion> activePromos) {
        loadCategories();
        
        petRepo.getAll(new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                allPets = data != null ? data : new ArrayList<>();
                if (activePromos != null) 
                    PromotionManager.applyPromotions(allPets, null, activePromos);
                
                if (!isSearching.getValue()) {
                    filteredPets.postValue(allPets.stream().limit(20).collect(Collectors.toList()));
                }
                checkLoadingDone();
            }
            @Override public void onFailure(String err) { checkLoadingDone(); }
        });

        foodRepo.getAll(new FoodRepository.Callback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> data) {
                allFoods = data != null ? data : new ArrayList<>();
                if (activePromos != null)
                    PromotionManager.applyPromotions(null, allFoods, activePromos);

                if (!isSearching.getValue()) {
                    filteredFoods.postValue(allFoods.stream()
                            .filter(f -> f.getStatus() == null || Food.STATUS_AVAILABLE.equals(f.getStatus()))
                            .limit(20).collect(Collectors.toList()));
                }
                checkLoadingDone();
            }
            @Override public void onFailure(String err) { checkLoadingDone(); }
        });
    }

    private void loadCategories() {
        catRepo.getAll(new CategoryRepository.Callback<List<Category>>() {
            public void onSuccess(List<Category> data) {
                List<Category> active = data.stream()
                        .filter(Category::isActive)
                        .collect(Collectors.toList());
                categories.postValue(active);
            }
            public void onFailure(String err) {}
        });
    }

    private int loadCount = 0;
    private synchronized void checkLoadingDone() {
        loadCount++;
        if (loadCount >= 2) {
            isLoading.postValue(false);
            loadCount = 0;
        }
    }

    public void filterByCategory(String categoryId, String categoryType) {
        if (categoryId == null) {
            isSearching.setValue(false);
            filteredPets.setValue(allPets.stream().limit(20).collect(Collectors.toList()));
            filteredFoods.setValue(allFoods.stream().limit(20).collect(Collectors.toList()));
            return;
        }

        isSearching.setValue(true);
        if (Category.TYPE_PET.equals(categoryType)) {
            List<Pet> filtered = allPets.stream()
                    .filter(p -> categoryId.equals(p.getCategoryId()))
                    .collect(Collectors.toList());
            filteredPets.setValue(filtered);
            filteredFoods.setValue(new ArrayList<>());
        } else if (Category.TYPE_FOOD.equals(categoryType)) {
            List<Food> filtered = allFoods.stream()
                    .filter(f -> categoryId.equals(f.getCategoryId()))
                    .collect(Collectors.toList());
            filteredFoods.setValue(filtered);
            filteredPets.setValue(new ArrayList<>());
        }
    }

    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            isSearching.setValue(false);
            filteredPets.setValue(allPets.stream().limit(20).collect(Collectors.toList()));
            filteredFoods.setValue(allFoods.stream().limit(20).collect(Collectors.toList()));
            return;
        }

        isSearching.setValue(true);
        String q = query.toLowerCase().trim();
        
        filteredPets.setValue(allPets.stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                          || (p.getBreed() != null && p.getBreed().toLowerCase().contains(q))
                          || (p.getSpecies() != null && p.getSpecies().toLowerCase().contains(q)))
                .collect(Collectors.toList()));

        filteredFoods.setValue(allFoods.stream()
                .filter(f -> (f.getName() != null && f.getName().toLowerCase().contains(q))
                          || (f.getBrand() != null && f.getBrand().toLowerCase().contains(q))
                          || (f.getFoodType() != null && f.getFoodType().toLowerCase().contains(q)))
                .collect(Collectors.toList()));
    }
}
