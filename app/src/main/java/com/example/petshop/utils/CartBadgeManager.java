package com.example.petshop.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.petshop.model.entity.Cart;

public final class CartBadgeManager {

    private static final String PREF_NAME = "petshop_cart_badge";
    private static final String KEY_PREFIX = "cart_count_";

    private CartBadgeManager() {
    }

    public static void saveCartCount(Context context, String userId, int count) {
        if (context == null || userId == null) return;
        prefs(context).edit().putInt(KEY_PREFIX + userId, Math.max(0, count)).apply();
    }

    public static void saveCart(Context context, String userId, Cart cart) {
        if (context == null || userId == null) return;
        int count = 0;
        if (cart != null) {
            count = cart.getTotalItems();
            if (count <= 0 && cart.getItems() != null) {
                count = cart.calculateTotalItems();
            }
        }
        saveCartCount(context, userId, count);
    }

    public static int getCartCount(Context context, String userId) {
        if (context == null || userId == null) return 0;
        return prefs(context).getInt(KEY_PREFIX + userId, 0);
    }

    public static boolean hasItems(Context context, String userId) {
        return getCartCount(context, userId) > 0;
    }

    public static void clear(Context context, String userId) {
        if (context == null || userId == null) return;
        prefs(context).edit().remove(KEY_PREFIX + userId).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
