package com.example.petshop.utils;

import com.example.petshop.model.entity.Cart;
import com.example.petshop.model.entity.CartItem;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Promotion;

import java.util.ArrayList;
import java.util.List;

public class PromotionManager {

    /**
     * Tìm khuyến mãi tốt nhất áp dụng cho sản phẩm và cập nhật giá đã giảm.
     */
    public static void applyPromotions(List<Pet> pets, List<Food> foods, List<Promotion> activePromos) {
        if (activePromos == null || activePromos.isEmpty()) return;

        // Apply for Pets
        if (pets != null) {
            for (Pet p : pets) {
                Promotion best = findBestPromo(p.getId(), Promotion.APPLY_PET, activePromos);
                if (best != null) {
                    p.setPromotionId(best.getId());
                    p.setPromotion(best);
                    p.setOriginalPrice(p.getPrice());
                    p.setDiscountedPrice(best.applyDiscount(p.getPrice()));
                }
            }
        }

        // Apply for Foods
        if (foods != null) {
            for (Food f : foods) {
                Promotion best = findBestPromo(f.getId(), Promotion.APPLY_FOOD, activePromos);
                if (best != null) {
                    f.setPromotionId(best.getId());
                    f.setPromotion(best);
                    f.setOriginalPrice(f.getPrice());
                    f.setDiscountedPrice(best.applyDiscount(f.getPrice()));
                }
            }
        }
    }

    /**
     * Tính lại giá cart items dựa theo promotion đang active.
     * Chỉ xử lý item có originalPrice > 0 (được lưu từ phiên trước).
     * Trả về true nếu có giá nào thay đổi (cần lưu lại Firestore).
     */
    public static boolean refreshCartPrices(Cart cart, List<Promotion> activePromos) {
        if (cart == null || cart.getItems() == null) return false;
        List<Promotion> promos = activePromos != null ? activePromos : new ArrayList<>();
        boolean changed = false;

        for (CartItem item : cart.getItems()) {
            if (item.getOriginalPrice() <= 0) continue; // item cũ không có giá gốc, bỏ qua
            String categoryType = item.isPet() ? Promotion.APPLY_PET : Promotion.APPLY_FOOD;
            Promotion best = findBestPromo(item.getProductId(), categoryType, promos);
            double newPrice = best != null
                    ? best.applyDiscount(item.getOriginalPrice())
                    : item.getOriginalPrice(); // hết KM → hoàn lại giá gốc
            if (Math.abs(newPrice - item.getUnitPrice()) > 0.01) {
                item.setUnitPrice(newPrice);
                item.recalculateSubtotal();
                changed = true;
            }
        }
        if (changed) {
            cart.setSubtotal(cart.calculateSubtotal());
            cart.setTotalItems(cart.calculateTotalItems());
        }
        return changed;
    }

    private static Promotion findBestPromo(String productId, String categoryType, List<Promotion> promos) {
        Promotion best = null;
        double maxSaving = 0;

        for (Promotion p : promos) {
            boolean applies = Promotion.APPLY_ALL.equals(p.getApplyTo())
                    || categoryType.equals(p.getApplyTo())
                    || (Promotion.APPLY_SPECIFIC.equals(p.getApplyTo()) && p.getProductIds() != null && p.getProductIds().contains(productId));

            if (applies) {
                // For simplicity, we assume price = 100 to compare percentage vs fixed
                double saving = p.calculateDiscount(1000000); 
                if (saving > maxSaving) {
                    maxSaving = saving;
                    best = p;
                }
            }
        }
        return best;
    }
}
