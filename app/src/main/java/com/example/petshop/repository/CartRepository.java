package com.example.petshop.repository;

import com.example.petshop.model.entity.Cart;
import com.example.petshop.model.entity.CartItem;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CartRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL_CARTS = "carts";
    private static final String COL_PETS  = "pets";
    private static final String COL_FOODS = "foods";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ========== GET CART ==========
    public void getCart(String userId, Callback<Cart> cb) {
        db.collection(COL_CARTS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Cart cart = doc.toObject(Cart.class);
                        cb.onSuccess(cart != null ? cart : emptyCart(userId));
                    } else {
                        cb.onSuccess(emptyCart(userId));
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ========== ADD PET (only 1 per pet) ==========
    public void addPetToCart(String userId, Pet pet, Callback<Cart> cb) {
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            var petRef = db.collection(COL_PETS).document(pet.getId());
            var cartRef = db.collection(COL_CARTS).document(userId);

            // Step 1: All Reads first
            var petDoc = transaction.get(petRef);
            var cartDoc = transaction.get(cartRef);

            // Step 2: Validate
            String status = petDoc.getString("status");
            if (!Pet.STATUS_AVAILABLE.equals(status)) {
                throw new RuntimeException("Thú cưng này đã được đặt trước bởi người khác");
            }

            // Step 3: All Writes
            // Mark pet as RESERVED
            transaction.update(petRef, "status", Pet.STATUS_RESERVED);

            // Add item to cart
            CartItem item = new CartItem(
                    CartItem.PRODUCT_TYPE_PET, pet.getId(),
                    pet.getName(), pet.getThumbnailUrl(),
                    pet.getEffectivePrice(), 1);
            item.setOriginalPrice(pet.getPrice());
            item.setId(UUID.randomUUID().toString());
            item.setCartId(userId);
            item.setAddedAt(Timestamp.now().toString());

            Cart cart;
            if (cartDoc.exists()) {
                cart = cartDoc.toObject(Cart.class);
                if (cart == null) cart = emptyCart(userId);
            } else {
                cart = emptyCart(userId);
            }
            if (cart.getItems() == null) cart.setItems(new ArrayList<>());
            cart.getItems().add(item);
            cart.setSubtotal(cart.calculateSubtotal());
            cart.setTotalItems(cart.calculateTotalItems());
            cart.setUpdatedAt(Timestamp.now().toString());
            transaction.set(cartRef, cart);
            return null;
        })
        .addOnSuccessListener(v -> getCart(userId, cb))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ========== ADD FOOD (with quantity) ==========
    public void addFoodToCart(String userId, Food food, int quantity, Callback<Cart> cb) {
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            var foodRef = db.collection(COL_FOODS).document(food.getId());
            var foodDoc = transaction.get(foodRef);
            int stock = foodDoc.getLong("stock") != null ? foodDoc.getLong("stock").intValue() : 0;
            if (stock < quantity) {
                throw new RuntimeException("Kho chỉ còn " + stock + " sản phẩm");
            }

            var cartRef = db.collection(COL_CARTS).document(userId);
            var cartDoc = transaction.get(cartRef);
            Cart cart;
            if (cartDoc.exists()) {
                cart = cartDoc.toObject(Cart.class);
                if (cart == null) cart = emptyCart(userId);
            } else {
                cart = emptyCart(userId);
            }
            if (cart.getItems() == null) cart.setItems(new ArrayList<>());

            // Check if food already in cart → update quantity + refresh price
            boolean found = false;
            for (CartItem existing : cart.getItems()) {
                if (CartItem.PRODUCT_TYPE_FOOD.equals(existing.getProductType())
                        && food.getId().equals(existing.getProductId())) {
                    int newQty = existing.getQuantity() + quantity;
                    if (newQty > stock) throw new RuntimeException("Kho chỉ còn " + stock + " sản phẩm");
                    existing.setUnitPrice(food.getEffectivePrice());
                    existing.setOriginalPrice(food.getPrice());
                    existing.setQuantity(newQty);
                    found = true;
                    break;
                }
            }
            if (!found) {
                CartItem item = new CartItem(
                        CartItem.PRODUCT_TYPE_FOOD, food.getId(),
                        food.getName(), food.getThumbnailUrl(),
                        food.getEffectivePrice(), quantity);
                item.setOriginalPrice(food.getPrice());
                item.setId(UUID.randomUUID().toString());
                item.setCartId(userId);
                item.setAddedAt(Timestamp.now().toString());
                cart.getItems().add(item);
            }
            cart.setSubtotal(cart.calculateSubtotal());
            cart.setTotalItems(cart.calculateTotalItems());
            cart.setUpdatedAt(Timestamp.now().toString());
            transaction.set(cartRef, cart);
            return null;
        })
        .addOnSuccessListener(v -> getCart(userId, cb))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ========== UPDATE FOOD QUANTITY ==========
    public void updateFoodQuantity(String userId, String itemId, int newQty, Callback<Cart> cb) {
        getCart(userId, new Callback<Cart>() {
            public void onSuccess(Cart cart) {
                if (cart.getItems() == null) { cb.onFailure("Giỏ hàng trống"); return; }
                for (CartItem item : cart.getItems()) {
                    if (itemId.equals(item.getId())) {
                        if (newQty <= 0) { removeItem(userId, itemId, cb); return; }
                        item.setQuantity(newQty);
                        break;
                    }
                }
                cart.setSubtotal(cart.calculateSubtotal());
                cart.setTotalItems(cart.calculateTotalItems());
                cart.setUpdatedAt(Timestamp.now().toString());
                db.collection(COL_CARTS).document(userId).set(cart)
                        .addOnSuccessListener(v -> cb.onSuccess(cart))
                        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
            }
            public void onFailure(String err) { cb.onFailure(err); }
        });
    }

    // ========== REMOVE ITEM ==========
    public void removeItem(String userId, String itemId, Callback<Cart> cb) {
        getCart(userId, new Callback<Cart>() {
            public void onSuccess(Cart cart) {
                if (cart.getItems() == null) { cb.onSuccess(cart); return; }
                CartItem toRemove = null;
                for (CartItem item : cart.getItems()) {
                    if (itemId.equals(item.getId())) { toRemove = item; break; }
                }
                if (toRemove == null) { cb.onSuccess(cart); return; }

                final CartItem removed = toRemove;
                cart.getItems().remove(removed);
                cart.setSubtotal(cart.calculateSubtotal());
                cart.setTotalItems(cart.calculateTotalItems());
                cart.setUpdatedAt(Timestamp.now().toString());

                // If pet → release reservation
                if (CartItem.PRODUCT_TYPE_PET.equals(removed.getProductType())) {
                    db.collection(COL_PETS).document(removed.getProductId())
                            .update("status", Pet.STATUS_AVAILABLE);
                }
                db.collection(COL_CARTS).document(userId).set(cart)
                        .addOnSuccessListener(v -> cb.onSuccess(cart))
                        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
            }
            public void onFailure(String err) { cb.onFailure(err); }
        });
    }

    // ========== SAVE CART (refresh prices) ==========
    public void saveCart(String userId, Cart cart, Callback<Cart> cb) {
        cart.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL_CARTS).document(userId).set(cart)
                .addOnSuccessListener(v -> cb.onSuccess(cart))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ========== CLEAR CART (after order placed) ==========
    public void clearCart(String userId, Callback<Void> cb) {
        Cart empty = emptyCart(userId);
        empty.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL_CARTS).document(userId).set(empty)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private Cart emptyCart(String userId) {
        Cart c = new Cart(userId);
        c.setId(userId);
        c.setItems(new ArrayList<>());
        return c;
    }
}
