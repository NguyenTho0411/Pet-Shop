package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class OrderItem {

    public static final String PRODUCT_TYPE_PET  = "PET";
    public static final String PRODUCT_TYPE_FOOD = "FOOD";

    @SerializedName("id")
    private String id;

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("productType")
    private String productType;          // PET | FOOD

    @SerializedName("productId")
    private String productId;

    // Snapshot thông tin sản phẩm tại thời điểm đặt hàng
    @SerializedName("productName")
    private String productName;

    @SerializedName("productThumbnail")
    private String productThumbnail;

    @SerializedName("productBrand")
    private String productBrand;         // cho food

    @SerializedName("productBreed")
    private String productBreed;         // cho pet

    @SerializedName("productSpecies")
    private String productSpecies;       // cho pet

    @SerializedName("unitPrice")
    private double unitPrice;            // giá tại thời điểm mua

    @SerializedName("originalPrice")
    private double originalPrice;        // giá gốc (trước giảm giá)

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("subtotal")
    private double subtotal;             // unitPrice * quantity

    @SerializedName("note")
    private String note;

    @SerializedName("isReviewed")
    private boolean isReviewed;

    public OrderItem() {}

    public boolean isPet()  { return PRODUCT_TYPE_PET.equals(productType); }
    public boolean isFood() { return PRODUCT_TYPE_FOOD.equals(productType); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductThumbnail() { return productThumbnail; }
    public void setProductThumbnail(String productThumbnail) { this.productThumbnail = productThumbnail; }

    public String getProductBrand() { return productBrand; }
    public void setProductBrand(String productBrand) { this.productBrand = productBrand; }

    public String getProductBreed() { return productBreed; }
    public void setProductBreed(String productBreed) { this.productBreed = productBreed; }

    public String getProductSpecies() { return productSpecies; }
    public void setProductSpecies(String productSpecies) { this.productSpecies = productSpecies; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isReviewed() { return isReviewed; }
    public void setReviewed(boolean reviewed) { isReviewed = reviewed; }
    // endregion
}
