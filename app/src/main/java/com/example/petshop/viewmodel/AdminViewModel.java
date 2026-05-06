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
import com.google.firebase.firestore.QuerySnapshot;

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
    private final MutableLiveData<Long> completedOrders = new MutableLiveData<>(0L);
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

    public AdminViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Long> getTotalRevenue() { return totalRevenue; }
    public LiveData<Long> getTotalOrders() { return totalOrders; }
    public LiveData<Long> getTotalUsers() { return totalUsers; }
    public LiveData<Long> getPendingOrders() { return pendingOrders; }
    public LiveData<Long> getCompletedOrders() { return completedOrders; }
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
                            resetOrderStats();
                            isLoading.postValue(false);
                            return;
                        }

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

                    int count = snapshots != null ? snapshots.size() : 0;
                    totalUsers.postValue((long) count);
                    Log.d(TAG, "Users updated: " + count);
                });
    }

    private void calculateOrderStats(QuerySnapshot snapshots) {
        long total = 0;
        long pending = 0;
        long completed = 0;
        long cancelled = 0;
        long preparing = 0;
        long shipping = 0;
        long delivered = 0;
        long paidRevenue = 0;
        long refundedOrders = 0;
        long refundedMoney = 0;

        for (var doc : snapshots.getDocuments()) {
            total++;

            String status = doc.getString("status");
            String paymentStatus = doc.getString("paymentStatus");
            String paymentMethod = doc.getString("paymentMethod");
            Double amount = doc.getDouble("totalAmount");
            if (amount == null) amount = 0.0;

            boolean isPaid = Order.PAY_STATUS_PAID.equals(paymentStatus)
                    || (Order.PAYMENT_COD.equals(paymentMethod)
                    && (Order.STATUS_DELIVERED.equals(status) || Order.STATUS_COMPLETED.equals(status)));

            switch (status != null ? status : "") {
                case Order.STATUS_PENDING:
                case Order.STATUS_WAIT_PAY:
                case Order.STATUS_CONFIRMED:
                    pending++;
                    break;

                case Order.STATUS_PREPARING:
                    preparing++;
                    break;

                case Order.STATUS_SHIPPING:
                    shipping++;
                    break;

                case Order.STATUS_DELIVERED:
                    delivered++;
                    if (isPaid) paidRevenue += amount.longValue();
                    break;

                case Order.STATUS_COMPLETED:
                    completed++;
                    if (isPaid) paidRevenue += amount.longValue();
                    break;

                case Order.STATUS_CANCELLED:
                    cancelled++;
                    break;

                case Order.STATUS_RETURN_REQUESTED:
                case Order.STATUS_RETURN_APPROVED:
                    pending++;
                    break;

                case Order.STATUS_REFUNDED:
                    refundedOrders++;
                    refundedMoney += amount.longValue();
                    break;
            }
        }

        long activeOrders = Math.max(0, total - cancelled - refundedOrders);
        long netRevenue = Math.max(0, paidRevenue - refundedMoney);

        totalOrders.postValue(activeOrders);
        pendingOrders.postValue(pending);
        preparingOrders.postValue(preparing);
        shippingOrders.postValue(shipping);
        deliveredOrders.postValue(delivered);
        completedOrders.postValue(completed);
        cancelledOrders.postValue(cancelled);
        totalRevenue.postValue(netRevenue);
        refundedAmount.postValue(refundedMoney);

        Log.d(TAG, "Stats: total=" + total
                + ", active=" + activeOrders
                + ", paidRevenue=" + paidRevenue
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
        completedOrders.postValue(0L);
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