package com.example.petshop.model.entity;

public class ReturnRequest {

    public static final String STATUS_PENDING  = "PENDING";   // chờ admin duyệt
    public static final String STATUS_APPROVED = "APPROVED";  // đã duyệt, chờ hoàn tiền
    public static final String STATUS_REFUNDED = "REFUNDED";  // đã hoàn tiền
    public static final String STATUS_REJECTED = "REJECTED";  // từ chối

    private String id;
    private String orderId;
    private String orderCode;
    private String userId;
    private String customerName;
    private String reason;
    private String paymentMethod;  // COD | VNPAY
    private String bankAccount;    // bắt buộc nếu COD
    private String bankName;       // bắt buộc nếu COD
    private double refundAmount;
    private String status;
    private String adminNote;
    private String createdAt;
    private String approvedAt;
    private String refundedAt;

    public ReturnRequest() {}

    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isRefunded() { return STATUS_REFUNDED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
    public boolean isCod()      { return Order.PAYMENT_COD.equals(paymentMethod); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getApprovedAt() { return approvedAt; }
    public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }

    public String getRefundedAt() { return refundedAt; }
    public void setRefundedAt(String refundedAt) { this.refundedAt = refundedAt; }
    // endregion
}
