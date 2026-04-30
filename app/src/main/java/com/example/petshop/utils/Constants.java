package com.example.petshop.utils;

import com.example.petshop.BuildConfig;

/**
 * Tất cả constants của app — đọc từ BuildConfig (inject qua local.properties)
 * Để thay đổi giá trị: sửa local.properties rồi Sync Gradle
 */
public final class Constants {

    private Constants() {}

    // ===== Google Sign-In =====
    public static final String GOOGLE_WEB_CLIENT_ID  = BuildConfig.GOOGLE_WEB_CLIENT_ID;

    // ===== Facebook =====
    public static final String FACEBOOK_APP_ID       = BuildConfig.FACEBOOK_APP_ID;
    public static final String FACEBOOK_CLIENT_TOKEN = BuildConfig.FACEBOOK_CLIENT_TOKEN;

    // ===== Backend API =====
    public static final String BASE_URL              = BuildConfig.BASE_URL;

    // ===== VNPay =====
    public static final String VNPAY_TMN_CODE        = BuildConfig.VNPAY_TMN_CODE;
    public static final String VNPAY_HASH_SECRET     = BuildConfig.VNPAY_HASH_SECRET;
    public static final String VNPAY_URL             = BuildConfig.VNPAY_URL;
    public static final String VNPAY_RETURN_URL      = BuildConfig.VNPAY_RETURN_URL;

    // ===== Firestore collections =====
    public static final String COL_USERS      = "users";
    public static final String COL_PETS       = "pets";
    public static final String COL_FOODS      = "foods";
    public static final String COL_ORDERS     = "orders";
    public static final String COL_CATEGORIES = "categories";
    public static final String COL_PROMOTIONS = "promotions";
    public static final String COL_REVIEWS    = "reviews";
    public static final String COL_BANNERS    = "banners";
    public static final String COL_VOUCHERS   = "vouchers";

    // ===== Intent keys =====
    public static final String KEY_PET_ID        = "pet_id";
    public static final String KEY_FOOD_ID       = "food_id";
    public static final String KEY_ORDER_ID      = "order_id";
    public static final String KEY_CATEGORY_ID   = "category_id";
    public static final String KEY_PRODUCT_TYPE  = "product_type";

    // ===== SharedPrefs =====
    public static final String PREF_NAME         = "petshop_prefs";

    // ===== Pagination =====
    public static final int PAGE_SIZE            = 10;

    // ===== Image upload =====
    public static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB
}
