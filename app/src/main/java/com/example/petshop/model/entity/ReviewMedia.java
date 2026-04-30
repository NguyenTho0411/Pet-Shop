package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class ReviewMedia {

    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_VIDEO = "VIDEO";

    @SerializedName("id")
    private String id;

    @SerializedName("reviewId")
    private String reviewId;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("mediaType")
    private String mediaType;            // IMAGE | VIDEO

    @SerializedName("sortOrder")
    private int sortOrder;

    public ReviewMedia() {}

    public boolean isImage() { return TYPE_IMAGE.equals(mediaType); }
    public boolean isVideo() { return TYPE_VIDEO.equals(mediaType); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    // endregion
}
