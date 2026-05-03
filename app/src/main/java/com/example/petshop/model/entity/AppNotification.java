package com.example.petshop.model.entity;

public class AppNotification {

    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_VOUCHER = "VOUCHER";
    public static final String TYPE_SYSTEM = "SYSTEM";

    private String id;
    private String type;
    private String title;
    private String message;
    private String createdAt;
    private String orderId;
    private boolean read;

    public AppNotification() {}

    public AppNotification(String id, String type, String title, String message,
                           String createdAt, String orderId, boolean read) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.orderId = orderId;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public boolean isRead() {
        return read;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}