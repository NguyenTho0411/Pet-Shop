package com.example.petshop.model.entity;

import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Pet {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_SOLD      = "SOLD";
    public static final String STATUS_RESERVED  = "RESERVED";
    public static final String STATUS_INACTIVE  = "INACTIVE";

    public static final String GENDER_MALE    = "MALE";
    public static final String GENDER_FEMALE  = "FEMALE";
    public static final String GENDER_UNKNOWN = "UNKNOWN";

    // Vaccine status
    public static final String VACCINE_FULL    = "FULL";
    public static final String VACCINE_PARTIAL = "PARTIAL";
    public static final String VACCINE_NONE    = "NONE";

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("category")
    private Category category;

    // Species: DOG, CAT, FISH, BIRD, RABBIT, HAMSTER, REPTILE, etc.
    @SerializedName("species")
    private String species;

    // Breed: Corgi, Husky, Persian, Maine Coon, ...
    @SerializedName("breed")
    private String breed;

    @SerializedName("age")
    private int age;                     // tháng tuổi

    @SerializedName("ageUnit")
    private String ageUnit;              // MONTH | YEAR

    @SerializedName("gender")
    private String gender;               // MALE | FEMALE | UNKNOWN

    @SerializedName("weight")
    private double weight;               // kg

    @SerializedName("color")
    private String color;

    @SerializedName("origin")
    private String origin;               // xuất xứ

    @SerializedName("description")
    private String description;          // mô tả tổng quan

    @SerializedName("careGuide")
    private String careGuide;            // hướng dẫn nuôi

    @SerializedName("dietInfo")
    private String dietInfo;             // chế độ ăn

    @SerializedName("healthInfo")
    private String healthInfo;           // tình trạng sức khỏe

    @SerializedName("vaccineStatus")
    private String vaccineStatus;        // FULL | PARTIAL | NONE

    @SerializedName("isDewormed")
    private boolean dewormed;




    @SerializedName("isMicrochipped")
    private boolean microchipped;

    @SerializedName("hasCertificate")
    private boolean hasCertificate; // cái này đã đúng

    @SerializedName("price")
    private double price;

    @SerializedName("originalPrice")
    private double originalPrice;        // giá gốc (trước giảm giá)

    @SerializedName("promotionId")
    private String promotionId;

    @SerializedName("promotion")
    private Promotion promotion;

    @SerializedName("discountedPrice")
    private double discountedPrice;      // giá sau khuyến mãi (server tính)

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;         // ảnh đại diện

    @SerializedName("mediaList")
    private List<PetMedia> mediaList;    // nhiều ảnh + video

    @SerializedName("status")
    private String status;               // AVAILABLE | SOLD | RESERVED | INACTIVE

    @SerializedName("averageRating")
    private double averageRating;

    @SerializedName("reviewCount")
    private int reviewCount;

    @SerializedName("viewCount")
    private int viewCount;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Pet() {}

    @Exclude
    public boolean isAvailable()  { return STATUS_AVAILABLE.equals(status); }

    @Exclude
    public boolean isSold()       { return STATUS_SOLD.equals(status); }

    @Exclude
    public boolean hasPromotion() { return promotionId != null && !promotionId.isEmpty(); }

    @Exclude
    public double getEffectivePrice() {
        return discountedPrice > 0 ? discountedPrice : price;
    }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getAgeUnit() { return ageUnit; }
    public void setAgeUnit(String ageUnit) { this.ageUnit = ageUnit; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCareGuide() { return careGuide; }
    public void setCareGuide(String careGuide) { this.careGuide = careGuide; }

    public String getDietInfo() { return dietInfo; }
    public void setDietInfo(String dietInfo) { this.dietInfo = dietInfo; }

    public String getHealthInfo() { return healthInfo; }
    public void setHealthInfo(String healthInfo) { this.healthInfo = healthInfo; }

    public String getVaccineStatus() { return vaccineStatus; }
    public void setVaccineStatus(String vaccineStatus) { this.vaccineStatus = vaccineStatus; }


    public boolean isDewormed() { return dewormed; }
    public void setDewormed(boolean dewormed) { this.dewormed = dewormed; }

    public boolean isMicrochipped() { return microchipped; }
    public void setMicrochipped(boolean microchipped) { this.microchipped = microchipped; }

    public boolean isHasCertificate() { return hasCertificate; }
    public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(double discountedPrice) { this.discountedPrice = discountedPrice; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public List<PetMedia> getMediaList() { return mediaList; }
    public void setMediaList(List<PetMedia> mediaList) { this.mediaList = mediaList; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
