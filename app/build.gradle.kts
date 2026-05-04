import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Đọc local.properties
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

fun localProp(key: String, fallback: String = "") =
    localProps.getProperty(key, fallback).trim('"')

android {
    namespace = "com.example.petshop"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.petshop"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ===== BuildConfig fields từ local.properties =====
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID",
            "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")

        buildConfigField("String", "BASE_URL",
            "\"${localProp("BASE_URL", "https://your-api.com/api/v1/")}\"")

        buildConfigField("String", "VNPAY_TMN_CODE",
            "\"${localProp("VNPAY_TMN_CODE")}\"")

        buildConfigField("String", "VNPAY_HASH_SECRET",
            "\"${localProp("VNPAY_HASH_SECRET")}\"")

        buildConfigField("String", "VNPAY_URL",
            "\"${localProp("VNPAY_URL", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")}\"")

        buildConfigField("String", "VNPAY_RETURN_URL",
            "\"${localProp("VNPAY_RETURN_URL", "petshop://payment/vnpay-return")}\"")

        buildConfigField("String", "OPENAI_API_KEY",
            "\"${localProp("OPENAI_API_KEY")}\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    implementation(libs.play.services.auth)
    implementation(libs.glide)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.circleimageview)
    implementation(libs.gson)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
