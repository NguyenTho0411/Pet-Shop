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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {

    private final CategoryRepository catRepo = new CategoryRepository();
    private final PetRepository petRepo = new PetRepository();
    private final FoodRepository foodRepo = new FoodRepository();
    private final PromotionRepository promoRepo = new PromotionRepository();

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Pet>> filteredPets = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Food>> filteredFoods = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSearching = new MutableLiveData<>(false);

    private List<Pet> allPets = new ArrayList<>();
    private List<Food> allFoods = new ArrayList<>();

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Pet>> getFeaturedPets() {
        return filteredPets;
    }

    public LiveData<List<Food>> getFeaturedFoods() {
        return filteredFoods;
    }

    public LiveData<Boolean> getLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getIsSearching() {
        return isSearching;
    }

    public void loadHomeData() {
        isLoading.setValue(true);
        isSearching.setValue(false);
        loadCount = 0;

        promoRepo.getActive(new PromotionRepository.Callback<List<Promotion>>() {
            @Override
            public void onSuccess(List<Promotion> promos) {
                loadMainData(promos);
            }

            @Override
            public void onFailure(String err) {
                loadMainData(null);
            }
        });
    }

    private void loadMainData(List<Promotion> activePromos) {
        loadCategories();

        petRepo.getAll(new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                allPets = data != null ? data : new ArrayList<>();

                if (activePromos != null) {
                    PromotionManager.applyPromotions(allPets, null, activePromos);
                }

                if (!Boolean.TRUE.equals(isSearching.getValue())) {
                    filteredPets.postValue(allPets.stream()
                            .limit(20)
                            .collect(Collectors.toList()));
                }

                checkLoadingDone();
            }

            @Override
            public void onFailure(String err) {
                error.postValue(err);
                checkLoadingDone();
            }
        });

        foodRepo.getAll(new FoodRepository.Callback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> data) {
                allFoods = data != null ? data : new ArrayList<>();

                if (activePromos != null) {
                    PromotionManager.applyPromotions(null, allFoods, activePromos);
                }

                if (!Boolean.TRUE.equals(isSearching.getValue())) {
                    filteredFoods.postValue(getAvailableFoods().stream()
                            .limit(20)
                            .collect(Collectors.toList()));
                }

                checkLoadingDone();
            }

            @Override
            public void onFailure(String err) {
                error.postValue(err);
                checkLoadingDone();
            }
        });
    }

    private void loadCategories() {
        catRepo.getAll(new CategoryRepository.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> data) {
                List<Category> active = data != null
                        ? data.stream()
                        .filter(Category::isActive)
                        .collect(Collectors.toList())
                        : new ArrayList<>();

                categories.postValue(active);
            }

            @Override
            public void onFailure(String err) {
                error.postValue(err);
            }
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

    public void filterByCategory(Category category) {
        if (category == null) {
            resetHomeLists();
            return;
        }

        isSearching.setValue(true);

        String categoryType = resolveCategoryType(category);

        if (Category.TYPE_FOOD.equals(categoryType)) {
            List<Food> filtered = getAvailableFoods().stream()
                    .filter(food -> matchesFoodCategory(food, category))
                    .collect(Collectors.toList());

            filteredFoods.setValue(filtered);
            filteredPets.setValue(new ArrayList<>());
            return;
        }

        List<Pet> filtered = allPets.stream()
                .filter(pet -> matchesPetCategory(pet, category))
                .collect(Collectors.toList());

        filteredPets.setValue(filtered);
        filteredFoods.setValue(new ArrayList<>());
    }

    private void resetHomeLists() {
        isSearching.setValue(false);

        filteredPets.setValue(allPets.stream()
                .limit(20)
                .collect(Collectors.toList()));

        filteredFoods.setValue(getAvailableFoods().stream()
                .limit(20)
                .collect(Collectors.toList()));
    }

    private List<Food> getAvailableFoods() {
        return allFoods.stream()
                .filter(food -> food.getStatus() == null
                        || Food.STATUS_AVAILABLE.equals(food.getStatus()))
                .collect(Collectors.toList());
    }

    private boolean matchesPetCategory(Pet pet, Category category) {
        if (pet == null || category == null) return false;

        if (sameText(pet.getCategoryId(), category.getId())
                || sameText(pet.getCategoryId(), category.getName())
                || matchesCategoryObject(pet.getCategory(), category)) {
            return true;
        }

        String petSpecies = pet.getSpecies() != null ? pet.getSpecies().toLowerCase() : "";
        String catName = category.getName() != null ? category.getName().toLowerCase() : "";
        String catId = category.getId() != null ? category.getId().toLowerCase() : "";

        if (petSpecies.contains("dog") && (catName.contains("chó") || catName.contains("dog") || catId.contains("dog") || catId.contains("chó"))) {
            return true;
        }
        if (petSpecies.contains("cat") && (catName.contains("mèo") || catName.contains("cat") || catId.contains("cat") || catId.contains("mèo"))) {
            return true;
        }
        if (petSpecies.contains("fish") && (catName.contains("cá") || catName.contains("fish") || catId.contains("fish") || catId.contains("cá"))) {
            return true;
        }
        if (petSpecies.contains("bird") && (catName.contains("chim") || catName.contains("bird") || catId.contains("bird") || catId.contains("chim"))) {
            return true;
        }
        if (petSpecies.contains("rabbit") && (catName.contains("thỏ") || catName.contains("rabbit") || catId.contains("rabbit") || catId.contains("thỏ"))) {
            return true;
        }
        if (petSpecies.contains("hamster") && (catName.contains("hamster") || catId.contains("hamster"))) {
            return true;
        }

        return sameCategoryKey(pet.getSpecies(), category.getId())
                || sameCategoryKey(pet.getSpecies(), category.getName());
    }

    private boolean matchesFoodCategory(Food food, Category category) {
        if (food == null || category == null) return false;

        return sameText(food.getCategoryId(), category.getId())
                || sameText(food.getCategoryId(), category.getName())
                || sameText(food.getFoodType(), category.getId())
                || sameText(food.getFoodType(), category.getName())
                || sameText(food.getTargetPetType(), category.getId())
                || sameText(food.getTargetPetType(), category.getName())
                || sameCategoryKey(food.getFoodType(), category.getId())
                || sameCategoryKey(food.getFoodType(), category.getName())
                || sameCategoryKey(food.getTargetPetType(), category.getId())
                || sameCategoryKey(food.getTargetPetType(), category.getName())
                || matchesCategoryObject(food.getCategory(), category);
    }

    private boolean matchesCategoryObject(Category productCategory, Category selectedCategory) {
        if (productCategory == null || selectedCategory == null) return false;

        return sameText(productCategory.getId(), selectedCategory.getId())
                || sameText(productCategory.getName(), selectedCategory.getName())
                || sameCategoryKey(productCategory.getId(), selectedCategory.getId())
                || sameCategoryKey(productCategory.getName(), selectedCategory.getName());
    }

    private String resolveCategoryType(Category category) {
        if (category == null) return Category.TYPE_PET;

        if (Category.TYPE_FOOD.equals(category.getType())) {
            return Category.TYPE_FOOD;
        }

        if (Category.TYPE_PET.equals(category.getType())) {
            return Category.TYPE_PET;
        }

        String key = normalizeCategoryKey(category.getName() + " " + category.getId());

        if ("pate".equals(key)
                || "dry".equals(key)
                || "snack".equals(key)
                || "milk".equals(key)
                || "supplement".equals(key)
                || "food".equals(key)) {
            return Category.TYPE_FOOD;
        }

        return Category.TYPE_PET;
    }

    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            resetHomeLists();
            return;
        }

        isSearching.setValue(true);

        String q = normalizeSearchText(query);

        filteredPets.setValue(allPets.stream()
                .filter(pet -> containsNormalized(pet.getName(), q)
                        || containsNormalized(pet.getBreed(), q)
                        || containsNormalized(pet.getSpecies(), q))
                .collect(Collectors.toList()));

        filteredFoods.setValue(getAvailableFoods().stream()
                .filter(food -> containsNormalized(food.getName(), q)
                        || containsNormalized(food.getBrand(), q)
                        || containsNormalized(food.getFoodType(), q)
                        || containsNormalized(food.getTargetPetType(), q))
                .collect(Collectors.toList()));
    }

    private boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private boolean sameCategoryKey(String a, String b) {
        String x = normalizeCategoryKey(a);
        String y = normalizeCategoryKey(b);

        if (x.isEmpty() || y.isEmpty()) return false;

        return x.equals(y) || x.contains(y) || y.contains(x);
    }

    private boolean containsNormalized(String source, String query) {
        if (source == null || query == null) return false;
        return normalizeSearchText(source).contains(query);
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";

        String s = value.trim().toLowerCase(Locale.ROOT);

        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        s = s.replace("đ", "d");

        return s;
    }

    private String normalizeCategoryKey(String value) {
        if (value == null) return "";

        String s = normalizeSearchText(value);

        if (s.contains("pate") || s.contains("wet")) {
            return "pate";
        }

        if (s.contains("dry") || s.contains("hat kho")) {
            return "dry";
        }

        if (s.contains("snack") || s.contains("banh thuong")) {
            return "snack";
        }

        if (s.contains("milk") || s.contains("sua")) {
            return "milk";
        }

        if (s.contains("supplement") || s.contains("thuc pham chuc nang")) {
            return "supplement";
        }

        if (s.contains("cat") || s.contains("meo")) {
            return "cat";
        }

        if (s.contains("dog") || s.contains("cho")) {
            return "dog";
        }

        if (s.contains("bird") || s.contains("chim")) {
            return "bird";
        }

        if (s.contains("fish") || s.contains("ca")) {
            return "fish";
        }

        if (s.contains("rabbit") || s.contains("tho")) {
            return "rabbit";
        }

        if (s.contains("hamster")) {
            return "hamster";
        }

        if (s.contains("food") || s.contains("thuc an") || s.contains("do an")) {
            return "food";
        }

        return s.replaceAll("[^a-z0-9]", "");
    }
}