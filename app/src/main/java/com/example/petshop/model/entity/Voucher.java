package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class Voucher {

    public static final String TYPE_PERCENT  = "PERCENT";
    public static final String TYPE_FIXED    = "FIXED";
    public static final String TYPE_FREESHIP = "FREESHIP";

    public static final String STATUS_ACTIVE  = "ACTIVE";
    public static final String STATUS_USED    = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @SerializedName("id")
    private String id;

    @SerializedName("code")
    private String code;                 // mã voucher: PETSHOP20

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;                 // PERCENT | FIXED | FREESHIP

    @SerializedName("discountValue")
    private double discountValue;

    @SerializedName("maxDiscountAmount")
    private double maxDiscountAmount;

    @SerializedName("minOrderAmount")
    private double minOrderAmount;       // đơn tối thiểu để dùng voucher

    @SerializedName("usageLimit")
    private int usageLimit;              // tổng số lần dùng cho phép

    @SerializedName("usedCount")
    private int usedCount;               // số lần đã dùng

    @SerializedName("perUserLimit")
    private int perUserLimit;            // giới hạn mỗi user (1 = dùng 1 lần)

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("createdAt")
    private String createdAt;

    public Voucher() {}

    public boolean isExpired(String currentDate) {
        return endDate != null && currentDate.compareTo(endDate) > 0;
    }

    public boolean isUsageLimitReached() {
        return usageLimit > 0 && usedCount >= usageLimit;
    }

    public boolean isFreeship() { return TYPE_FREESHIP.equals(type); }

    public double calculateDiscount(double orderAmount) {
        if (!TYPE_FREESHIP.equals(type)) {
            double discount = TYPE_PERCENT.equals(type)
                    ? orderAmount * discountValue / 100
                    : discountValue;
            return maxDiscountAmount > 0 ? Math.min(discount, maxDiscountAmount) : discount;
        }
        return 0;
    }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    public int getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(int perUserLimit) { this.perUserLimit = perUserLimit; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    // endregion
}
