package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Category;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.repository.CategoryRepository;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.repository.PetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {

    private final CategoryRepository catRepo  = new CategoryRepository();
    private final PetRepository      petRepo  = new PetRepository();
    private final FoodRepository     foodRepo = new FoodRepository();

    private final MutableLiveData<List<Category>> categories    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Pet>>      featuredPets  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Food>>     featuredFoods = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>        isLoading     = new MutableLiveData<>(false);
    private final MutableLiveData<String>         error         = new MutableLiveData<>();

    // Filtered lists when category selected
    private final MutableLiveData<List<Pet>>  filteredPets  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Food>> filteredFoods = new MutableLiveData<>(new ArrayList<>());

    private List<Pet>  allPets  = new ArrayList<>();
    private List<Food> allFoods = new ArrayList<>();

    public LiveData<List<Category>> getCategories()    { return categories; }
    public LiveData<List<Pet>>      getFeaturedPets()  { return filteredPets; }
    public LiveData<List<Food>>     getFeaturedFoods() { return filteredFoods; }
    public LiveData<Boolean>        getLoading()       { return isLoading; }
    public LiveData<String>         getError()         { return error; }

    public void loadHomeData() {
        isLoading.setValue(true);
        loadCategories();
        loadPets();
        loadFoods();
    }

    private void loadCategories() {
        catRepo.getAll(new CategoryRepository.Callback<>() {
            public void onSuccess(List<Category> data) {
                List<Category> active = data.stream()
                        .filter(Category::isActive)
                        .collect(Collectors.toList());
                categories.postValue(active);
            }
            public void onFailure(String err) { /* silently skip */ }
        });
    }

    private void loadPets() {
        petRepo.getAll(new PetRepository.Callback<>() {
            public void onSuccess(List<Pet> data) {
                allPets = data.stream()
                        .limit(20)
                        .collect(Collectors.toList());
                filteredPets.postValue(allPets);
                checkLoadingDone();
            }
            public void onFailure(String err) {
                error.postValue(err);
                checkLoadingDone();
            }
        });
    }

    private void loadFoods() {
        foodRepo.getAll(new FoodRepository.Callback<>() {
            public void onSuccess(List<Food> data) {
                allFoods = data.stream()
                        .filter(f -> Food.STATUS_AVAILABLE.equals(f.getStatus()))
                        .limit(20)
                        .collect(Collectors.toList());
                filteredFoods.postValue(allFoods);
                checkLoadingDone();
            }
            public void onFailure(String err) {
                error.postValue(err);
                checkLoadingDone();
            }
        });
    }

    private int loadCount = 0;
    private synchronized void checkLoadingDone() {
        loadCount++;
        if (loadCount >= 2) { // pets + foods
            isLoading.postValue(false);
            loadCount = 0;
        }
    }

    /** Filter both pets and foods by category */
    public void filterByCategory(String categoryId, String categoryType) {
        if (categoryId == null) {
            filteredPets.setValue(allPets);
            filteredFoods.setValue(allFoods);
            return;
        }

        if (Category.TYPE_PET.equals(categoryType)) {
            List<Pet> filtered = allPets.stream()
                    .filter(p -> categoryId.equals(p.getCategoryId()))
                    .collect(Collectors.toList());
            filteredPets.setValue(filtered.isEmpty() ? allPets : filtered);
            filteredFoods.setValue(allFoods);
        } else if (Category.TYPE_FOOD.equals(categoryType)) {
            List<Food> filtered = allFoods.stream()
                    .filter(f -> categoryId.equals(f.getCategoryId()))
                    .collect(Collectors.toList());
            filteredFoods.setValue(filtered.isEmpty() ? allFoods : filtered);
            filteredPets.setValue(allPets);
        }
    }

    /** Search both pets and foods by name */
    public void search(String query) {
        if (query == null || query.isEmpty()) {
            filteredPets.setValue(allPets);
            filteredFoods.setValue(allFoods);
            return;
        }
        String q = query.toLowerCase();
        filteredPets.setValue(allPets.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(q)
                          || p.getBreed() != null && p.getBreed().toLowerCase().contains(q))
                .collect(Collectors.toList()));
        filteredFoods.setValue(allFoods.stream()
                .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(q)
                          || f.getBrand() != null && f.getBrand().toLowerCase().contains(q))
                .collect(Collectors.toList()));
    }
}
