package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Order {

    // Trạng thái đơn hàng
    public static final String STATUS_PENDING    = "PENDING";     // chờ xác nhận
    public static final String STATUS_CONFIRMED  = "CONFIRMED";   // đã xác nhận
    public static final String STATUS_PREPARING  = "PREPARING";   // đang chuẩn bị
    public static final String STATUS_SHIPPING   = "SHIPPING";    // đang giao
    public static final String STATUS_DELIVERED  = "DELIVERED";   // đã giao
    public static final String STATUS_COMPLETED  = "COMPLETED";   // hoàn thành
    public static final String STATUS_CANCELLED  = "CANCELLED";   // đã hủy
    public static final String STATUS_REFUNDED   = "REFUNDED";    // hoàn tiền

    // Phương thức thanh toán
    public static final String PAYMENT_COD    = "COD";
    public static final String PAYMENT_VNPAY  = "VNPAY";

    // Trạng thái thanh toán
    public static final String PAY_STATUS_PENDING  = "PENDING";
    public static final String PAY_STATUS_PAID     = "PAID";
    public static final String PAY_STATUS_FAILED   = "FAILED";
    public static final String PAY_STATUS_REFUNDED = "REFUNDED";

    @SerializedName("id")
    private String id;

    @SerializedName("orderCode")
    private String orderCode;            // mã đơn: ORD-20240101-0001

    @SerializedName("userId")
    private String userId;

    @SerializedName("user")
    private User user;

    @SerializedName("items")
    private List<OrderItem> items;

    // Thông tin nhận hàng (snapshot tại thời điểm đặt)
    @SerializedName("receiverName")
    private String receiverName;

    @SerializedName("receiverPhone")
    private String receiverPhone;

    @SerializedName("shippingAddress")
    private String shippingAddress;      // địa chỉ đầy đủ

    @SerializedName("shippingWard")
    private String shippingWard;

    @SerializedName("shippingDistrict")
    private String shippingDistrict;

    @SerializedName("shippingCity")
    private String shippingCity;

    // Tài chính
    @SerializedName("subtotal")
    private double subtotal;             // tổng tiền hàng

    @SerializedName("shippingFee")
    private double shippingFee;

    @SerializedName("promotionDiscount")
    private double promotionDiscount;    // giảm từ promotion sản phẩm

    @SerializedName("voucherDiscount")
    private double voucherDiscount;      // giảm từ voucher

    @SerializedName("totalAmount")
    private double totalAmount;          // thực trả = subtotal + shippingFee - discounts

    @SerializedName("voucherId")
    private String voucherId;

    @SerializedName("voucherCode")
    private String voucherCode;

    // Thanh toán
    @SerializedName("paymentMethod")
    private String paymentMethod;        // COD | VNPAY

    @SerializedName("paymentStatus")
    private String paymentStatus;        // PENDING | PAID | FAILED | REFUNDED

    @SerializedName("paidAt")
    private String paidAt;

    // Trạng thái đơn
    @SerializedName("status")
    private String status;

    @SerializedName("note")
    private String note;                 // ghi chú của khách

    @SerializedName("cancelReason")
    private String cancelReason;

    @SerializedName("estimatedDelivery")
    private String estimatedDelivery;    // ngày giao dự kiến

    @SerializedName("deliveredAt")
    private String deliveredAt;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Order() {}

    public boolean isPending()   { return STATUS_PENDING.equals(status); }
    public boolean isShipping()  { return STATUS_SHIPPING.equals(status); }
    public boolean isCompleted() { return STATUS_COMPLETED.equals(status); }
    public boolean isCancelled() { return STATUS_CANCELLED.equals(status); }

    public boolean canCancel() {
        return STATUS_PENDING.equals(status) || STATUS_CONFIRMED.equals(status);
    }

    public boolean canReview() {
        return STATUS_COMPLETED.equals(status) || STATUS_DELIVERED.equals(status);
    }

    public boolean isCodPayment()   { return PAYMENT_COD.equals(paymentMethod); }
    public boolean isVnpayPayment() { return PAYMENT_VNPAY.equals(paymentMethod); }
    public boolean isPaid()         { return PAY_STATUS_PAID.equals(paymentStatus); }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getShippingWard() { return shippingWard; }
    public void setShippingWard(String shippingWard) { this.shippingWard = shippingWard; }

    public String getShippingDistrict() { return shippingDistrict; }
    public void setShippingDistrict(String shippingDistrict) { this.shippingDistrict = shippingDistrict; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }

    public double getPromotionDiscount() { return promotionDiscount; }
    public void setPromotionDiscount(double promotionDiscount) { this.promotionDiscount = promotionDiscount; }

    public double getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(double voucherDiscount) { this.voucherDiscount = voucherDiscount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getVoucherId() { return voucherId; }
    public void setVoucherId(String voucherId) { this.voucherId = voucherId; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(String deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    // endregion
}
