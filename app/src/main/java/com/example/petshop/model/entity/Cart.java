package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Cart {

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("items")
    private List<CartItem> items;

    @SerializedName("totalItems")
    private int totalItems;

    @SerializedName("subtotal")
    private double subtotal;             // tổng trước giảm giá

    @SerializedName("updatedAt")
    private String updatedAt;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(String userId) {
        this.userId = userId;
        this.items  = new ArrayList<>();
    }

    public double calculateSubtotal() {
        double total = 0;
        if (items != null) {
            for (CartItem item : items) {
                total += item.getSubtotal();
            }
        }
        return total;
    }

    public int calculateTotalItems() {
        int count = 0;
        if (items != null) {
            for (CartItem item : items) {
                count += item.getQuantity();
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
