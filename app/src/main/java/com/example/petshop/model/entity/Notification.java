package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class Notification {

    // Loại thông báo
    public static final String TYPE_ORDER_STATUS   = "ORDER_STATUS";
    public static final String TYPE_PAYMENT        = "PAYMENT";
    public static final String TYPE_PROMOTION      = "PROMOTION";
    public static final String TYPE_SYSTEM         = "SYSTEM";
    public static final String TYPE_NEW_PRODUCT    = "NEW_PRODUCT";

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;               // null = broadcast đến tất cả

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("type")
    private String type;                 // ORDER_STATUS | PAYMENT | PROMOTION | SYSTEM

    @SerializedName("isRead")
    private boolean isRead;

    // Deep-link data
    @SerializedName("relatedType")
    private String relatedType;          // ORDER | PET | FOOD | PROMOTION

    @SerializedName("relatedId")
    private String relatedId;

    @SerializedName("createdAt")
    private String createdAt;

    public Notification() {}

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    // endregion
}
