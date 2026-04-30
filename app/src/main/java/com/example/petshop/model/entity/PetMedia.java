package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class PetMedia {

    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_VIDEO = "VIDEO";

    @SerializedName("id")
    private String id;

    @SerializedName("petId")
    private String petId;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;         // thumbnail cho video

    @SerializedName("mediaType")
    private String mediaType;            // IMAGE | VIDEO

    @SerializedName("sortOrder")
    private int sortOrder;               // thứ tự hiển thị

    @SerializedName("caption")
    private String caption;

    @SerializedName("durationSeconds")
    private int durationSeconds;         // chỉ dùng cho video

    public PetMedia() {}

    public PetMedia(String petId, String mediaUrl, String mediaType) {
        this.petId     = petId;
        this.mediaUrl  = mediaUrl;
        this.mediaType = mediaType;
    }

    public boolean isImage() { return TYPE_IMAGE.equals(mediaType); }
    public boolean isVideo() { return TYPE_VIDEO.equals(mediaType); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

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
