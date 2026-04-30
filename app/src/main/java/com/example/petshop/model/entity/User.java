package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class User {

    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    public static final String LOGIN_EMAIL  = "EMAIL";
    public static final String LOGIN_GOOGLE = "GOOGLE";

    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_BANNED   = "BANNED";

    @SerializedName("id")
    private String id;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("role")
    private String role;                 // ADMIN | CUSTOMER

    @SerializedName("loginType")
    private String loginType;            // EMAIL | GOOGLE

    @SerializedName("googleId")
    private String googleId;

    @SerializedName("status")
    private String status;               // ACTIVE | INACTIVE | BANNED

    @SerializedName("gender")
    private String gender;               // MALE | FEMALE | OTHER

    @SerializedName("dateOfBirth")
    private String dateOfBirth;          // yyyy-MM-dd

    @SerializedName("totalOrders")
    private int totalOrders;

    @SerializedName("totalSpent")
    private double totalSpent;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public User() {}

    public User(String id, String fullName, String email, String role) {
        this.id        = id;
        this.fullName  = fullName;
        this.email     = email;
        this.role      = role;
        this.status    = STATUS_ACTIVE;
        this.loginType = LOGIN_EMAIL;
    }

    public boolean isAdmin()    { return ROLE_ADMIN.equals(role); }
    public boolean isCustomer() { return ROLE_CUSTOMER.equals(role); }
    public boolean isActive()   { return STATUS_ACTIVE.equals(status); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLoginType() { return loginType; }
    public void setLoginType(String loginType) { this.loginType = loginType; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
