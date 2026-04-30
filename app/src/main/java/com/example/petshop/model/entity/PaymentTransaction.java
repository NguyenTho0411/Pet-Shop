package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class PaymentTransaction {

    public static final String METHOD_COD   = "COD";
    public static final String METHOD_VNPAY = "VNPAY";

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_SUCCESS  = "SUCCESS";
    public static final String STATUS_FAILED   = "FAILED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    @SerializedName("id")
    private String id;

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("orderCode")
    private String orderCode;

    @SerializedName("userId")
    private String userId;

    @SerializedName("method")
    private String method;               // COD | VNPAY

    @SerializedName("amount")
    private double amount;

    @SerializedName("status")
    private String status;               // PENDING | SUCCESS | FAILED | REFUNDED

    // VNPay specific fields
    @SerializedName("vnpTxnRef")
    private String vnpTxnRef;            // mã giao dịch VNPay

    @SerializedName("vnpTransactionNo")
    private String vnpTransactionNo;     // mã giao dịch phía ngân hàng

    @SerializedName("vnpBankCode")
    private String vnpBankCode;          // mã ngân hàng

    @SerializedName("vnpCardType")
    private String vnpCardType;          // ATM | QRCODE | VISA...

    @SerializedName("vnpResponseCode")
    private String vnpResponseCode;      // 00 = thành công

    @SerializedName("vnpPayDate")
    private String vnpPayDate;           // yyyyMMddHHmmss

    @SerializedName("vnpSecureHash")
    private String vnpSecureHash;

    @SerializedName("failureReason")
    private String failureReason;

    @SerializedName("paidAt")
    private String paidAt;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public PaymentTransaction() {}

    public boolean isSuccess()  { return STATUS_SUCCESS.equals(status); }
    public boolean isFailed()   { return STATUS_FAILED.equals(status); }
    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isVnpay()    { return METHOD_VNPAY.equals(method); }
    public boolean isCod()      { return METHOD_COD.equals(method); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVnpTxnRef() { return vnpTxnRef; }
    public void setVnpTxnRef(String vnpTxnRef) { this.vnpTxnRef = vnpTxnRef; }

    public String getVnpTransactionNo() { return vnpTransactionNo; }
    public void setVnpTransactionNo(String vnpTransactionNo) { this.vnpTransactionNo = vnpTransactionNo; }

    public String getVnpBankCode() { return vnpBankCode; }
    public void setVnpBankCode(String vnpBankCode) { this.vnpBankCode = vnpBankCode; }

    public String getVnpCardType() { return vnpCardType; }
    public void setVnpCardType(String vnpCardType) { this.vnpCardType = vnpCardType; }

    public String getVnpResponseCode() { return vnpResponseCode; }
    public void setVnpResponseCode(String vnpResponseCode) { this.vnpResponseCode = vnpResponseCode; }

    public String getVnpPayDate() { return vnpPayDate; }
    public void setVnpPayDate(String vnpPayDate) { this.vnpPayDate = vnpPayDate; }

    public String getVnpSecureHash() { return vnpSecureHash; }
    public void setVnpSecureHash(String vnpSecureHash) { this.vnpSecureHash = vnpSecureHash; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
