package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Promotion {

    public static final String TYPE_PERCENT = "PERCENT";   // giảm theo %
    public static final String TYPE_FIXED   = "FIXED";     // giảm số tiền cố định

    public static final String APPLY_ALL   = "ALL";        // áp dụng tất cả sản phẩm
    public static final String APPLY_PET   = "PET";        // chỉ thú cưng
    public static final String APPLY_FOOD  = "FOOD";       // chỉ thức ăn
    public static final String APPLY_SPECIFIC = "SPECIFIC"; // sản phẩm cụ thể

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("bannerUrl")
    private String bannerUrl;            // ảnh banner promotion

    @SerializedName("discountType")
    private String discountType;         // PERCENT | FIXED

    @SerializedName("discountValue")
    private double discountValue;        // % hoặc số tiền

    @SerializedName("maxDiscountAmount")
    private double maxDiscountAmount;    // giảm tối đa (cho loại PERCENT)

    @SerializedName("applyTo")
    private String applyTo;              // ALL | PET | FOOD | SPECIFIC

    @SerializedName("productIds")
    private List<String> productIds;     // khi applyTo = SPECIFIC

    @SerializedName("startDate")
    private String startDate;            // yyyy-MM-dd HH:mm:ss

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("usageCount")
    private int usageCount;              // số lần đã dùng

    @SerializedName("usageLimit")
    private int usageLimit;              // giới hạn tổng lượt dùng (0 = không giới hạn)

    @SerializedName("perUserLimit")
    private int perUserLimit;            // giới hạn mỗi user

    @SerializedName("createdBy")
    private String createdBy;            // adminId

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Promotion() {}

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

    public String getApplyTo() { return applyTo; }
    public void setApplyTo(String applyTo) { this.applyTo = applyTo; }

    public List<String> getProductIds() { return productIds; }
    public void setProductIds(List<String> productIds) { this.productIds = productIds; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
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
