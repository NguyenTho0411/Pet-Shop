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

    private com.google.firebase.firestore.ListenerRegistration cartListener;

    public void loadCart() {
        String uid = uid();
        if (uid == null) { cart.postValue(new Cart()); return; }

        if (cartListener != null) cartListener.remove();

        isLoading.postValue(true);
        cartListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("carts").document(uid)
                .addSnapshotListener((doc, e) -> {
                    isLoading.postValue(false);
                    if (e != null) {
                        error.postValue(e.getMessage());
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        Cart c = doc.toObject(Cart.class);
                        if (c != null) {
                            cart.postValue(c);
                        } else {
                            cart.postValue(new Cart(uid));
                        }
                    } else {
                        cart.postValue(new Cart(uid));
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (cartListener != null) cartListener.remove();
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
