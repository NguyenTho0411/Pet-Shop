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
                Promotion best = findBestPromoForProduct(p, activePromos);
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
                Promotion best = findBestPromoForProduct(f, activePromos);
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
            String categoryType = item.isPet() ? Promotion.CATEGORY_PET : Promotion.CATEGORY_FOOD;
            Promotion best = findBestPromoForCartItem(item, categoryType, promos);
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

    private static Promotion findBestPromoForProduct(Object product, List<Promotion> promos) {
        Promotion best = null;
        double maxSaving = 0;
        double price = 0;

        if (product instanceof Pet) price = ((Pet) product).getPrice();
        else if (product instanceof Food) price = ((Food) product).getPrice();

        for (Promotion p : promos) {
            if (p.appliesTo(product)) {
                double saving = p.calculateDiscount(price);
                if (saving > maxSaving) {
                    maxSaving = saving;
                    best = p;
                }
            }
        }
        return best;
    }

    private static Promotion findBestPromoForCartItem(CartItem item, String categoryType, List<Promotion> promos) {
        Promotion best = null;
        double maxSaving = 0;

        // Nếu có thông tin chi tiết (snapshot), ưu tiên dùng hàm appliesTo chuẩn
        Object productSnapshot = item.isPet() ? item.getPetInfo() : item.getFoodInfo();

        for (Promotion p : promos) {
            boolean applies = false;

            if (productSnapshot != null) {
                applies = p.appliesTo(productSnapshot);
            } else {
                // Fallback nếu không có snapshot
                if (!p.isActive() || !p.isWithinDateRange()) continue;

                if (Promotion.APPLY_ALL.equals(p.getApplyType())) {
                    applies = true;
                } else if (Promotion.APPLY_CATEGORY.equals(p.getApplyType())) {
                    applies = categoryType.equals(p.getApplyCategory());
                } else if (Promotion.APPLY_PRODUCT.equals(p.getApplyType())) {
                    applies = p.getProductIds() != null && p.getProductIds().contains(item.getProductId());
                }
                // APPLY_SPECIES không thể xác định nếu không có snapshot (vì CartItem không lưu species)
            }

            if (applies) {
                double saving = p.calculateDiscount(item.getOriginalPrice());
                if (saving > maxSaving) {
                    maxSaving = saving;
                    best = p;
                }
            }
        }
        return best;
    }
}
