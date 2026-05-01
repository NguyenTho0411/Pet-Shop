package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Cart;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.repository.CartRepository;
import com.example.petshop.repository.PromotionRepository;
import com.example.petshop.utils.PromotionManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class CartViewModel extends ViewModel {

    private final CartRepository repo = new CartRepository();

    private final MutableLiveData<Cart>    cart      = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String>  error     = new MutableLiveData<>();
    private final MutableLiveData<String>  success   = new MutableLiveData<>();

    public LiveData<Cart>    getCart()    { return cart; }
    public LiveData<Boolean> getLoading() { return isLoading; }
    public LiveData<String>  getError()   { return error; }
    public LiveData<String>  getSuccess() { return success; }

    private String uid() {
        var u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    public void loadCart() {
        String uid = uid();
        if (uid == null) { cart.setValue(new Cart()); return; }
        isLoading.setValue(true);
        repo.getCart(uid, new CartRepository.Callback<Cart>() {
            public void onSuccess(Cart c) {
                // Luôn re-apply promotion hiện tại để đảm bảo giá đúng
                new PromotionRepository().getActive(new PromotionRepository.Callback<List<Promotion>>() {
                    public void onSuccess(List<Promotion> promos) {
                        boolean changed = PromotionManager.refreshCartPrices(c, promos);
                        if (changed) {
                            repo.saveCart(uid, c, new CartRepository.Callback<Cart>() {
                                public void onSuccess(Cart saved) { isLoading.postValue(false); cart.postValue(saved); }
                                public void onFailure(String e)  { isLoading.postValue(false); cart.postValue(c); }
                            });
                        } else {
                            isLoading.postValue(false);
                            cart.postValue(c);
                        }
                    }
                    public void onFailure(String e) { isLoading.postValue(false); cart.postValue(c); }
                });
            }
            public void onFailure(String e) { isLoading.postValue(false); error.postValue(e); }
        });
    }

    public void addPet(Pet pet) {
        String uid = uid();
        if (uid == null) { error.setValue("Vui lòng đăng nhập"); return; }
        isLoading.setValue(true);
        repo.addPetToCart(uid, pet, new CartRepository.Callback<>() {
            public void onSuccess(Cart c)   { isLoading.postValue(false); cart.postValue(c); success.postValue("✓ Đã thêm " + pet.getName()); }
            public void onFailure(String e) { isLoading.postValue(false); error.postValue(e); }
        });
    }

    public void addFood(Food food, int qty) {
        String uid = uid();
        if (uid == null) { error.setValue("Vui lòng đăng nhập"); return; }
        isLoading.setValue(true);
        repo.addFoodToCart(uid, food, qty, new CartRepository.Callback<>() {
            public void onSuccess(Cart c)   { isLoading.postValue(false); cart.postValue(c); success.postValue("✓ Đã thêm " + food.getName()); }
            public void onFailure(String e) { isLoading.postValue(false); error.postValue(e); }
        });
    }

    public void removeItem(String itemId) {
        String uid = uid(); if (uid == null) return;
        repo.removeItem(uid, itemId, new CartRepository.Callback<>() {
            public void onSuccess(Cart c)   { cart.postValue(c); }
            public void onFailure(String e) { error.postValue(e); }
        });
    }

    public void updateFoodQty(String itemId, int newQty) {
        String uid = uid(); if (uid == null) return;
        repo.updateFoodQuantity(uid, itemId, newQty, new CartRepository.Callback<>() {
            public void onSuccess(Cart c)   { cart.postValue(c); }
            public void onFailure(String e) { error.postValue(e); }
        });
    }

    public void clearCart() {
        String uid = uid(); if (uid == null) return;
        repo.clearCart(uid, new CartRepository.Callback<>() {
            public void onSuccess(Void v)   { cart.postValue(new Cart(uid)); }
            public void onFailure(String e) { /* silently ignore */ }
        });
    }
}
