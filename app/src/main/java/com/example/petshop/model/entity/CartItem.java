package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class CartItem {

    public static final String PRODUCT_TYPE_PET  = "PET";
    public static final String PRODUCT_TYPE_FOOD = "FOOD";

    @SerializedName("id")
    private String id;

    @SerializedName("cartId")
    private String cartId;

    @SerializedName("productType")
    private String productType;          // PET | FOOD

    @SerializedName("productId")
    private String productId;

    @SerializedName("productName")
    private String productName;          // snapshot tên

    @SerializedName("productThumbnail")
    private String productThumbnail;     // snapshot ảnh

    @SerializedName("unitPrice")
    private double unitPrice;            // giá tại thời điểm thêm vào giỏ

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("subtotal")
    private double subtotal;             // unitPrice * quantity

    // Snapshot thông tin thêm để hiển thị
    @SerializedName("petInfo")
    private Pet petInfo;                 // null nếu là FOOD

    @SerializedName("foodInfo")
    private Food foodInfo;               // null nếu là PET

    @SerializedName("note")
    private String note;                 // ghi chú đặc biệt của khách

    @SerializedName("addedAt")
    private String addedAt;

    public CartItem() {}

    public CartItem(String productType, String productId, String productName,
                    String productThumbnail, double unitPrice, int quantity) {
        this.productType      = productType;
        this.productId        = productId;
        this.productName      = productName;
        this.productThumbnail = productThumbnail;
        this.unitPrice        = unitPrice;
        this.quantity         = quantity;
        this.subtotal         = unitPrice * quantity;
    }

    public void recalculateSubtotal() {
        this.subtotal = unitPrice * quantity;
    }

    public boolean isPet()  { return PRODUCT_TYPE_PET.equals(productType); }
    public boolean isFood() { return PRODUCT_TYPE_FOOD.equals(productType); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductThumbnail() { return productThumbnail; }
    public void setProductThumbnail(String productThumbnail) { this.productThumbnail = productThumbnail; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculateSubtotal();
    }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public Pet getPetInfo() { return petInfo; }
    public void setPetInfo(Pet petInfo) { this.petInfo = petInfo; }

    public Food getFoodInfo() { return foodInfo; }
    public void setFoodInfo(Food foodInfo) { this.foodInfo = foodInfo; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getAddedAt() { return addedAt; }
    public void setAddedAt(String addedAt) { this.addedAt = addedAt; }
    // endregion
}
