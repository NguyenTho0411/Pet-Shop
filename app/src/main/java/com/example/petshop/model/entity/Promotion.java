package com.example.petshop.model.entity;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Promotion {

    public static final String TYPE_PERCENT = "PERCENT";   // giảm theo %
    public static final String TYPE_FIXED   = "FIXED";     // giảm số tiền cố định

    // Loại áp dụng (phân cấp)
    public static final String APPLY_ALL       = "ALL";       // tất cả sản phẩm
    public static final String APPLY_CATEGORY  = "CATEGORY";  // theo danh mục (thú cưng/thức ăn)
    public static final String APPLY_SPECIES   = "SPECIES";   // theo giống (chó, mèo, cá...)
    public static final String APPLY_PRODUCT   = "PRODUCT";   // sản phẩm cụ thể

    // Danh mục (áp dụng khi APPLY_CATEGORY)
    public static final String CATEGORY_PET  = "PET";
    public static final String CATEGORY_FOOD = "FOOD";

    // Các loại giống phổ biến
    public static final String SPECIES_DOG     = "DOG";
    public static final String SPECIES_CAT     = "CAT";
    public static final String SPECIES_FISH    = "FISH";
    public static final String SPECIES_BIRD    = "BIRD";
    public static final String SPECIES_RABBIT  = "RABBIT";
    public static final String SPECIES_HAMSTER = "HAMSTER";

    // Loại sản phẩm (để xác định collection trong Firestore)
    public static final String PRODUCT_TYPE_PET = "PET";
    public static final String PRODUCT_TYPE_FOOD = "FOOD";

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("bannerUrl")
    private String bannerUrl;

    @SerializedName("discountType")
    private String discountType;

    @SerializedName("discountValue")
    private double discountValue;

    @SerializedName("maxDiscountAmount")
    private double maxDiscountAmount;

    // --- Nâng cấp: phân cấp áp dụng ---
    @SerializedName("applyType")
    private String applyType;              // ALL | CATEGORY | SPECIES | PRODUCT

    @SerializedName("applyCategory")
    private String applyCategory;          // PET | FOOD (khi applyType = CATEGORY)

    @SerializedName("applySpecies")
    private List<String> applySpecies;    // ["DOG", "CAT"] (khi applyType = SPECIES)

    @SerializedName("productIds")
    private List<String> productIds;       // danh sách product ID cụ thể

    @SerializedName("productTypes")
    private List<String> productTypes;    // ["PET", "FOOD"] khi áp dụng nhiều loại
    // ----------------------------------

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @PropertyName("active")
    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("usageCount")
    private int usageCount;

    @SerializedName("usageLimit")
    private int usageLimit;

    @SerializedName("perUserLimit")
    private int perUserLimit;

    @SerializedName("createdBy")
    private String createdBy;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Promotion() {
        this.applyType = APPLY_ALL;
    }

    public boolean isPercentType() { return TYPE_PERCENT.equals(discountType); }
    public boolean isFixedType()   { return TYPE_FIXED.equals(discountType); }

    public double calculateDiscount(double originalPrice) {
        if (isPercentType()) {
            double discount = originalPrice * discountValue / 100;
            return maxDiscountAmount > 0 ? Math.min(discount, maxDiscountAmount) : discount;
        }
        return Math.min(discountValue, originalPrice);
    }

    public double applyDiscount(double originalPrice) {
        return Math.max(0, originalPrice - calculateDiscount(originalPrice));
    }

    // Kiểm tra khuyến mãi có áp dụng cho sản phẩm không
    public boolean appliesTo(Object product) {
        if (!isActive) return false;
        if (!isWithinDateRange()) return false;

        switch (applyType) {
            case APPLY_ALL:
                return true;

            case APPLY_CATEGORY:
                if (product instanceof Pet) {
                    return CATEGORY_PET.equals(applyCategory);
                } else if (product instanceof Food) {
                    return CATEGORY_FOOD.equals(applyCategory);
                }
                return false;

            case APPLY_SPECIES:
                if (product instanceof Pet) {
                    Pet pet = (Pet) product;
                    if (applySpecies == null || applySpecies.isEmpty()) return false;
                    return applySpecies.contains(pet.getSpecies());
                }
                return false;

            case APPLY_PRODUCT:
                if (productIds == null || productIds.isEmpty()) return false;
                if (product instanceof Pet) {
                    return productIds.contains(((Pet) product).getId());
                } else if (product instanceof Food) {
                    return productIds.contains(((Food) product).getId());
                }
                return false;

            default:
                return false;
        }
    }

    public boolean isWithinDateRange() {
        if (startDate == null && endDate == null) return true;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date now = new java.util.Date();
            if (startDate != null && !startDate.isEmpty()) {
                java.util.Date start = sdf.parse(startDate);
                if (start != null && now.before(start)) return false;
            }
            if (endDate != null && !endDate.isEmpty()) {
                java.util.Date end = sdf.parse(endDate);
                if (end != null && now.after(end)) return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public String getApplyDescription() {
        switch (applyType) {
            case APPLY_ALL:
                return "Tất cả sản phẩm";
            case APPLY_CATEGORY:
                return CATEGORY_PET.equals(applyCategory) ? "Thú cưng" : "Thức ăn";
            case APPLY_SPECIES:
                if (applySpecies == null || applySpecies.isEmpty()) return "Các giống";
                return String.join(", ", applySpecies);
            case APPLY_PRODUCT:
                return productIds != null ? productIds.size() + " sản phẩm" : "Sản phẩm cụ thể";
            default:
                return "Không xác định";
        }
    }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public String getApplyType() { return applyType; }
    public void setApplyType(String applyType) { this.applyType = applyType; }

    // Legacy getter/setter để tương thích ngược
    public String getApplyTo() { return applyType; }
    public void setApplyTo(String applyTo) { this.applyType = applyTo; }

    public String getApplyCategory() { return applyCategory; }
    public void setApplyCategory(String category) { this.applyCategory = category; }

    public List<String> getApplySpecies() { return applySpecies; }
    public void setApplySpecies(List<String> species) { this.applySpecies = species; }

    public List<String> getProductIds() { return productIds; }
    public void setProductIds(List<String> productIds) { this.productIds = productIds; }

    public List<String> getProductTypes() { return productTypes; }
    public void setProductTypes(List<String> types) { this.productTypes = types; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    @PropertyName("active")
    public boolean isActive() { return isActive; }
    @PropertyName("active")
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }
    public void setTotalUsageLimit(int limit) { this.usageLimit = limit; }

    public int getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(int perUserLimit) { this.perUserLimit = perUserLimit; }
    public void setMaxUsagePerUser(int limit) { this.perUserLimit = limit; }
    // endregion
}
