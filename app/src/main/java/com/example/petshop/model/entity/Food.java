package com.example.petshop.model.entity;

import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Food {

    public static final String STATUS_AVAILABLE    = "AVAILABLE";
    public static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String STATUS_INACTIVE     = "INACTIVE";

    // Loại thú cưng áp dụng
    public static final String PET_TYPE_DOG    = "DOG";
    public static final String PET_TYPE_CAT    = "CAT";
    public static final String PET_TYPE_FISH   = "FISH";
    public static final String PET_TYPE_BIRD   = "BIRD";
    public static final String PET_TYPE_RABBIT = "RABBIT";
    public static final String PET_TYPE_ALL    = "ALL";

    // Loại thức ăn
    public static final String FOOD_TYPE_DRY    = "DRY";     // hạt khô
    public static final String FOOD_TYPE_WET    = "WET";     // pate
    public static final String FOOD_TYPE_SNACK  = "SNACK";   // bánh thưởng
    public static final String FOOD_TYPE_MILK   = "MILK";    // sữa
    public static final String FOOD_TYPE_SUPPL  = "SUPPLEMENT"; // thực phẩm chức năng

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("category")
    private Category category;

    @SerializedName("brand")
    private String brand;                // thương hiệu: Royal Canin, Whiskas, ...

    @SerializedName("foodType")
    private String foodType;             // DRY | WET | SNACK | MILK | SUPPLEMENT

    @SerializedName("targetPetType")
    private String targetPetType;        // DOG | CAT | FISH | BIRD | RABBIT | ALL

    @SerializedName("weightGram")
    private int weightGram;              // trọng lượng gói (gram)

    @SerializedName("unit")
    private String unit;                 // gói, hộp, kg, ...

    @SerializedName("description")
    private String description;

    @SerializedName("ingredients")
    private String ingredients;          // thành phần

    @SerializedName("nutritionInfo")
    private String nutritionInfo;        // thông tin dinh dưỡng

    @SerializedName("usageGuide")
    private String usageGuide;           // hướng dẫn sử dụng

    @SerializedName("storageGuide")
    private String storageGuide;         // hướng dẫn bảo quản

    @SerializedName("suitableAge")
    private String suitableAge;          // độ tuổi phù hợp: PUPPY, ADULT, SENIOR, ALL

    @SerializedName("origin")
    private String origin;               // xuất xứ

    @SerializedName("expiryMonths")
    private int expiryMonths;            // hạn sử dụng (tháng)

    @SerializedName("price")
    private double price;

    @SerializedName("originalPrice")
    private double originalPrice;

    @SerializedName("promotionId")
    private String promotionId;

    @SerializedName("promotion")
    private Promotion promotion;

    @SerializedName("discountedPrice")
    private double discountedPrice;

    @SerializedName("stock")
    private int stock;

    @SerializedName("sold")
    private int sold;                    // số lượng đã bán

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("mediaList")
    private List<FoodMedia> mediaList;

    @SerializedName("status")
    private String status;

    @SerializedName("averageRating")
    private double averageRating;

    @SerializedName("reviewCount")
    private int reviewCount;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Food() {}

    @Exclude
    public boolean isAvailable()  { return STATUS_AVAILABLE.equals(status) && stock > 0; }
    @Exclude
    public boolean hasPromotion() { return promotionId != null && !promotionId.isEmpty(); }

    @Exclude
    public double getEffectivePrice() {
        // Gunakan discountedPrice hanya jika promotion aktif
        if (promotionId != null && !promotionId.isEmpty() && promotion != null && promotion.isActive()) {
            return promotion.applyDiscount(price);
        }
        // Fallback ke price jika không có promosi hoặc promosi sudah expired
        return price;
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

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public String getTargetPetType() { return targetPetType; }
    public void setTargetPetType(String targetPetType) { this.targetPetType = targetPetType; }

    public int getWeightGram() { return weightGram; }
    public void setWeightGram(int weightGram) { this.weightGram = weightGram; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getNutritionInfo() { return nutritionInfo; }
    public void setNutritionInfo(String nutritionInfo) { this.nutritionInfo = nutritionInfo; }

    public String getUsageGuide() { return usageGuide; }
    public void setUsageGuide(String usageGuide) { this.usageGuide = usageGuide; }

    public String getStorageGuide() { return storageGuide; }
    public void setStorageGuide(String storageGuide) { this.storageGuide = storageGuide; }

    public String getSuitableAge() { return suitableAge; }
    public void setSuitableAge(String suitableAge) { this.suitableAge = suitableAge; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public int getExpiryMonths() { return expiryMonths; }
    public void setExpiryMonths(int expiryMonths) { this.expiryMonths = expiryMonths; }

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

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public List<FoodMedia> getMediaList() { return mediaList; }
    public void setMediaList(List<FoodMedia> mediaList) { this.mediaList = mediaList; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
