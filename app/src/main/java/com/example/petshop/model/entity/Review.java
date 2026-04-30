package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Review {

    public static final String PRODUCT_TYPE_PET  = "PET";
    public static final String PRODUCT_TYPE_FOOD = "FOOD";

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("user")
    private User user;                   // thông tin người review

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("orderItemId")
    private String orderItemId;

    @SerializedName("productType")
    private String productType;          // PET | FOOD

    @SerializedName("productId")
    private String productId;

    @SerializedName("productName")
    private String productName;

    @SerializedName("productThumbnail")
    private String productThumbnail;

    @SerializedName("rating")
    private int rating;                  // 1 - 5 sao

    @SerializedName("comment")
    private String comment;

    @SerializedName("mediaList")
    private List<ReviewMedia> mediaList; // ảnh/video đính kèm

    @SerializedName("isVerifiedPurchase")
    private boolean isVerifiedPurchase;  // đã mua hàng thật

    @SerializedName("adminReply")
    private String adminReply;

    @SerializedName("adminReplyAt")
    private String adminReplyAt;

    @SerializedName("isHidden")
    private boolean isHidden;            // admin ẩn review

    @SerializedName("likeCount")
    private int likeCount;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Review() {}

    public boolean hasAdminReply() { return adminReply != null && !adminReply.isEmpty(); }
    public boolean hasMedia()      { return mediaList != null && !mediaList.isEmpty(); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductThumbnail() { return productThumbnail; }
    public void setProductThumbnail(String productThumbnail) { this.productThumbnail = productThumbnail; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<ReviewMedia> getMediaList() { return mediaList; }
    public void setMediaList(List<ReviewMedia> mediaList) { this.mediaList = mediaList; }

    public boolean isVerifiedPurchase() { return isVerifiedPurchase; }
    public void setVerifiedPurchase(boolean verifiedPurchase) { isVerifiedPurchase = verifiedPurchase; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

    public String getAdminReplyAt() { return adminReplyAt; }
    public void setAdminReplyAt(String adminReplyAt) { this.adminReplyAt = adminReplyAt; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
