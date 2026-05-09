package com.example.petshop.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.petshop.model.entity.Order;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;

/**
 * AdminViewModel - MVVM Pattern cho Admin Dashboard
 * - Real-time updates khi có thay đổi trong Firestore
 * - Tự động cập nhật UI khi data thay đổi
 * - Tính doanh thu và số đơn dựa trên trạng thái đơn hàng hiện tại
 */
public class AdminViewModel extends AndroidViewModel {

    private static final String TAG = "AdminViewModel";
    private static final String COL_ORDERS = "orders";
    private static final String COL_USERS = "users";

    private final MutableLiveData<Long> totalRevenue = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalOrders = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalUsers = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> pendingOrders = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> cancelledOrders = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> refundedAmount = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> preparingOrders = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> shippingOrders = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> deliveredOrders = new MutableLiveData<>(0L);

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration ordersListener;
    private ListenerRegistration usersListener;
    private QuerySnapshot lastOrderSnapshots;
    private final Set<String> activeCustomerIds = new HashSet<>();

    public AdminViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Long> getTotalRevenue() { return totalRevenue; }
    public LiveData<Long> getTotalOrders() { return totalOrders; }
    public LiveData<Long> getTotalUsers() { return totalUsers; }
    public LiveData<Long> getPendingOrders() { return pendingOrders; }
    public LiveData<Long> getCancelledOrders() { return cancelledOrders; }
    public LiveData<Long> getRefundedAmount() { return refundedAmount; }
    public LiveData<Long> getPreparingOrders() { return preparingOrders; }
    public LiveData<Long> getShippingOrders() { return shippingOrders; }
    public LiveData<Long> getDeliveredOrders() { return deliveredOrders; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void startListening() {
        Log.d(TAG, "startListening: Starting real-time listeners");
        isLoading.postValue(true);
        startOrdersListener();
        startUsersListener();
    }

    public void stopListening() {
        Log.d(TAG, "stopListening: Removing all listeners");
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }

    private void startOrdersListener() {
        ordersListener = db.collection(COL_ORDERS)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "Orders listener error: " + e.getMessage());
                            error.postValue("Lỗi tải đơn hàng: " + e.getMessage());
                            isLoading.postValue(false);
                            return;
                        }

                        if (snapshots == null || snapshots.isEmpty()) {
                            lastOrderSnapshots = null;
                            resetOrderStats();
                            isLoading.postValue(false);
                            return;
                        }

                        lastOrderSnapshots = snapshots;
                        calculateOrderStats(snapshots);
                        isLoading.postValue(false);
                        Log.d(TAG, "Orders updated: " + snapshots.size() + " documents");
                    }
                });
    }

    private void startUsersListener() {
        usersListener = db.collection(COL_USERS)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Users listener error: " + e.getMessage());
                        return;
                    }

                    if (snapshots == null) {
                        totalUsers.postValue(0L);
                        activeCustomerIds.clear();
                        if (lastOrderSnapshots != null) calculateOrderStats(lastOrderSnapshots);
                        return;
                    }

                    totalUsers.postValue((long) snapshots.size());

                    activeCustomerIds.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String role = doc.getString("role");
                        String status = doc.getString("status");
                        if ("CUSTOMER".equals(role) && "ACTIVE".equals(status)) {
                            activeCustomerIds.add(doc.getId());
                        }
                    }

                    // Recalculate order stats when user activity changes
                    if (lastOrderSnapshots != null) calculateOrderStats(lastOrderSnapshots);
                    Log.d(TAG, "Users updated: total=" + snapshots.size() + ", activeCustomers=" + activeCustomerIds.size());
                });
    }

    private void calculateOrderStats(QuerySnapshot snapshots) {
        long totalConsidered = 0;   // orders of ACTIVE customers only
        long pending = 0;           // all non-final states
        long cancelled = 0;
        long preparing = 0;
        long shipping = 0;
        long delivered = 0;         // delivered-like for UI tiles
        long countedOrders = 0;     // "Tổng đơn hàng" theo rule COD/VNPAY
        long revenue = 0;           // doanh thu theo rule COD/VNPAY
        long refundedOrders = 0;
        long refundedMoney = 0;

        for (var doc : snapshots.getDocuments()) {
            String status = doc.getString("status");
            String paymentStatus = doc.getString("paymentStatus");
            String paymentMethod = doc.getString("paymentMethod");
            Double amount = doc.getDouble("totalAmount");
            if (amount == null) amount = 0.0;

            String userId = doc.getString("userId");
            // Only count orders of ACTIVE customers
            if (userId == null || !activeCustomerIds.contains(userId)) {
                continue;
            }

            totalConsidered++;

            boolean isCancelled = Order.STATUS_CANCELLED.equals(status);
            boolean isRefunded = Order.STATUS_REFUNDED.equals(status)
                    || Order.PAY_STATUS_REFUNDED.equals(paymentStatus);

            boolean isCountedByRule =
                    (!isCancelled && !isRefunded) && (
                            // COD: only counted when delivered/completed
                            (Order.PAYMENT_COD.equals(paymentMethod)
                                    && (Order.STATUS_DELIVERED.equals(status) || Order.STATUS_COMPLETED.equals(status)))
                                    ||
                            // VNPAY: counted when payment succeeded (count once, regardless of later delivered)
                            (Order.PAYMENT_VNPAY.equals(paymentMethod) && Order.PAY_STATUS_PAID.equals(paymentStatus))
                    );

            if (isCountedByRule) {
                countedOrders++;
                revenue += amount.longValue();
            }

            switch (status != null ? status : "") {
                case Order.STATUS_CANCELLED:
                    cancelled++;
                    break;

                case Order.STATUS_REFUNDED:
                    refundedOrders++;
                    refundedMoney += amount.longValue();
                    break;

                case Order.STATUS_CONFIRMED:
                case Order.STATUS_PREPARING:
                    preparing++;
                    break;

                case Order.STATUS_SHIPPING:
                    shipping++;
                    break;

                case Order.STATUS_DELIVERED:
                case Order.STATUS_COMPLETED:
                case Order.STATUS_RETURN_REQUESTED:
                case Order.STATUS_RETURN_APPROVED:
                    delivered++;
                    break;

                default:
                    // "Chờ xử lý": tất cả đơn chưa có nhãn đã giao/hoàn thành/đã huỷ/hoàn tiền
                    // (bao gồm PENDING, WAITING_PAYMENT, CONFIRMED,...)
                    break;
            }

            // Pending rule: all orders not in final states
            boolean isFinal = Order.STATUS_DELIVERED.equals(status)
                    || Order.STATUS_COMPLETED.equals(status)
                    || Order.STATUS_CANCELLED.equals(status)
                    || Order.STATUS_REFUNDED.equals(status);
            if (!isFinal) {
                pending++;
            }
        }

        long netRevenue = Math.max(0, revenue - refundedMoney);

        totalOrders.postValue(countedOrders);
        pendingOrders.postValue(pending);
        preparingOrders.postValue(preparing);
        shippingOrders.postValue(shipping);
        deliveredOrders.postValue(delivered);
        cancelledOrders.postValue(cancelled);
        totalRevenue.postValue(netRevenue);
        refundedAmount.postValue(refundedMoney);

        Log.d(TAG, "Stats: considered=" + totalConsidered
                + ", countedOrders=" + countedOrders
                + ", revenue=" + revenue
                + ", refundedOrders=" + refundedOrders
                + ", refundedMoney=" + refundedMoney
                + ", netRevenue=" + netRevenue);
    }

    private void resetOrderStats() {
        totalOrders.postValue(0L);
        pendingOrders.postValue(0L);
        preparingOrders.postValue(0L);
        shippingOrders.postValue(0L);
        deliveredOrders.postValue(0L);
        cancelledOrders.postValue(0L);
        totalRevenue.postValue(0L);
        refundedAmount.postValue(0L);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
        Log.d(TAG, "onCleared: ViewModel destroyed");
    }
}