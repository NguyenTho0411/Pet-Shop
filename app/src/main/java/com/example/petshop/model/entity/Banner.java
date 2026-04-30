package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class Banner {

    public static final String ACTION_PROMOTION  = "PROMOTION";
    public static final String ACTION_PET        = "PET";
    public static final String ACTION_FOOD       = "FOOD";
    public static final String ACTION_CATEGORY   = "CATEGORY";
    public static final String ACTION_URL        = "URL";
    public static final String ACTION_NONE       = "NONE";

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("actionType")
    private String actionType;           // PROMOTION | PET | FOOD | CATEGORY | URL | NONE

    @SerializedName("actionValue")
    private String actionValue;          // id hoặc url tùy actionType

    @SerializedName("sortOrder")
    private int sortOrder;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("createdAt")
    private String createdAt;

    public Banner() {}

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionValue() { return actionValue; }
    public void setActionValue(String actionValue) { this.actionValue = actionValue; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

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
