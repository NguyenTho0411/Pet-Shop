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
     * Chỉ áp dụng AUTOMATIC promotions cho hiển thị giá sản phẩm.
     * VOUCHER chỉ áp dụng khi user nhập mã ở checkout.
     */
    public static void applyPromotions(List<Pet> pets, List<Food> foods, List<Promotion> activePromos) {
        if (activePromos == null || activePromos.isEmpty()) return;

        // Apply for Pets
        if (pets != null) {
            for (Pet p : pets) {
                // Reset promotion first
                p.setPromotionId(null);
                p.setPromotion(null);
                p.setDiscountedPrice(0);
                
                Promotion best = findBestPromoForProduct(p, activePromos);
                if (best != null && best.isActive() && best.isWithinDateRange()) {  // Double check active
                    p.setPromotionId(best.getId());
                    p.setPromotion(best);
                    p.setOriginalPrice(p.getPrice());
                    double discounted = best.applyDiscount(p.getPrice());
                    p.setDiscountedPrice(discounted);
                }
            }
        }

        // Apply for Foods
        if (foods != null) {
            for (Food f : foods) {
                // Reset promotion first
                f.setPromotionId(null);
                f.setPromotion(null);
                f.setDiscountedPrice(0);
                
                Promotion best = findBestPromoForProduct(f, activePromos);
                if (best != null && best.isActive() && best.isWithinDateRange()) {  // Double check active
                    f.setPromotionId(best.getId());
                    f.setPromotion(best);
                    f.setOriginalPrice(f.getPrice());
                    double discounted = best.applyDiscount(f.getPrice());
                    f.setDiscountedPrice(discounted);
                }
            }
        }
    }

    /**
     * Tính lại giá cart items dựa theo promotion đang active.
     * Áp dụng tất cả promotions (AUTOMATIC + VOUCHER) để hiển thị giá khuyến mãi.
     * VOUCHER cần user nhập mã riêng nên chỉ áp dụng nếu voucher đã được chọn.
     * Trả về true nếu có giá nào thay đổi (cần lưu lại Firestore).
     */
    public static boolean refreshCartPrices(Cart cart, List<Promotion> activePromos, String selectedVoucherId) {
        if (cart == null || cart.getItems() == null) return false;

        List<Promotion> promos = activePromos != null ? activePromos : new ArrayList<>();
        boolean changed = false;

        // Nếu có voucher đang chọn, tìm nó trong danh sách promotions
        Promotion selectedVoucher = null;
        if (selectedVoucherId != null) {
            for (Promotion p : promos) {
                if (p.isVoucher() && selectedVoucherId.equals(p.getId())) {
                    selectedVoucher = p;
                    break;
                }
            }
        }

        for (CartItem item : cart.getItems()) {
            if (item.getOriginalPrice() <= 0) continue;
            String categoryType = item.isPet() ? Promotion.CATEGORY_PET : Promotion.CATEGORY_FOOD;
            Promotion best = findBestPromoForCartItem(item, categoryType, promos, selectedVoucher);
            double newPrice = best != null
                    ? best.applyDiscount(item.getOriginalPrice())
                    : item.getOriginalPrice();
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

        if (price <= 0) return null;

        for (Promotion p : promos) {
            // Chỉ xét AUTOMATIC promotions, bỏ qua VOUCHER
            if (p.isVoucher()) continue;
            
            // Kiểm tra promotion có active và nằm trong date range không
            if (!p.isActive() || !p.isWithinDateRange()) continue;

            // Kiểm tra promotion áp dụng cho sản phẩm này không
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

    private static Promotion findBestPromoForCartItem(CartItem item, String categoryType, List<Promotion> promos, Promotion selectedVoucher) {
        Promotion best = null;
        double maxSaving = 0;

        if (item.getOriginalPrice() <= 0) return null;

        Object productSnapshot = item.isPet() ? item.getPetInfo() : item.getFoodInfo();

        for (Promotion p : promos) {
            // Kiểm tra loại promotion
            if (p.isVoucher()) {
                // Chỉ áp dụng voucher được chọn
                if (selectedVoucher == null || !p.getId().equals(selectedVoucher.getId())) {
                    continue;
                }
            }

            // Kiểm tra promotion active và còn trong date range
            if (!p.isActive() || !p.isWithinDateRange()) {
                continue;
            }

            boolean applies = false;

            if (productSnapshot != null) {
                // Nếu có product snapshot, kiểm tra chi tiết
                applies = p.appliesTo(productSnapshot);
            } else {
                // Nếu không có snapshot, kiểm tra theo category/product ID
                if (Promotion.APPLY_ALL.equals(p.getApplyType())) {
                    applies = true;
                } else if (Promotion.APPLY_CATEGORY.equals(p.getApplyType())) {
                    applies = categoryType != null && categoryType.equals(p.getApplyCategory());
                } else if (Promotion.APPLY_PRODUCT.equals(p.getApplyType())) {
                    List<String> productIds = p.getProductIds();
                    applies = productIds != null && productIds.contains(item.getProductId());
                }
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
