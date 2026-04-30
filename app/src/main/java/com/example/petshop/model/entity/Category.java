package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class Category {

    public static final String TYPE_PET  = "PET";
    public static final String TYPE_FOOD = "FOOD";

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;                 // e.g. "Chó", "Mèo", "Thức ăn cho chó"

    @SerializedName("type")
    private String type;                 // PET | FOOD

    @SerializedName("description")
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("sortOrder")
    private int sortOrder;

    @SerializedName("productCount")
    private int productCount;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("createdAt")
    private String createdAt;

    public Category() {}

    public Category(String id, String name, String type) {
        this.id       = id;
        this.name     = name;
        this.type     = type;
        this.isActive = true;
    }

    public boolean isPetCategory()  { return TYPE_PET.equals(type); }
    public boolean isFoodCategory() { return TYPE_FOOD.equals(type); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    // endregion
}
