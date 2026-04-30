package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class FoodMedia {

    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_VIDEO = "VIDEO";

    @SerializedName("id")
    private String id;

    @SerializedName("foodId")
    private String foodId;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("mediaType")
    private String mediaType;            // IMAGE | VIDEO

    @SerializedName("sortOrder")
    private int sortOrder;

    @SerializedName("caption")
    private String caption;

    @SerializedName("durationSeconds")
    private int durationSeconds;

    public FoodMedia() {}

    public FoodMedia(String foodId, String mediaUrl, String mediaType) {
        this.foodId    = foodId;
        this.mediaUrl  = mediaUrl;
        this.mediaType = mediaType;
    }

    public boolean isImage() { return TYPE_IMAGE.equals(mediaType); }
    public boolean isVideo() { return TYPE_VIDEO.equals(mediaType); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    // endregion
}
