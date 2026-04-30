package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class Favorite {

    public static final String PRODUCT_TYPE_PET  = "PET";
    public static final String PRODUCT_TYPE_FOOD = "FOOD";

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("productType")
    private String productType;          // PET | FOOD

    @SerializedName("productId")
    private String productId;

    // Snapshot để hiển thị danh sách yêu thích không cần fetch thêm
    @SerializedName("productName")
    private String productName;

    @SerializedName("productThumbnail")
    private String productThumbnail;

    @SerializedName("productPrice")
    private double productPrice;

    @SerializedName("productStatus")
    private String productStatus;        // AVAILABLE | SOLD | OUT_OF_STOCK

    @SerializedName("petInfo")
    private Pet petInfo;

    @SerializedName("foodInfo")
    private Food foodInfo;

    @SerializedName("createdAt")
    private String createdAt;

    public Favorite() {}

    public Favorite(String userId, String productType, String productId) {
        this.userId      = userId;
        this.productType = productType;
        this.productId   = productId;
    }

    public boolean isPet()  { return PRODUCT_TYPE_PET.equals(productType); }
    public boolean isFood() { return PRODUCT_TYPE_FOOD.equals(productType); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductThumbnail() { return productThumbnail; }
    public void setProductThumbnail(String productThumbnail) { this.productThumbnail = productThumbnail; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getProductStatus() { return productStatus; }
    public void setProductStatus(String productStatus) { this.productStatus = productStatus; }

    public Pet getPetInfo() { return petInfo; }
    public void setPetInfo(Pet petInfo) { this.petInfo = petInfo; }

    public Food getFoodInfo() { return foodInfo; }
    public void setFoodInfo(Food foodInfo) { this.foodInfo = foodInfo; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    // endregion
}
