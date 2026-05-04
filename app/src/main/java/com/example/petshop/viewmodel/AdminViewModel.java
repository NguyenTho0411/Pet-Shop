package com.example.petshop.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.petshop.model.entity.Order;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * AdminViewModel - MVVM Pattern cho Admin Dashboard
 * - Real-time updates khi có thay đổi trong Firestore
 * - Tự động cập nhật UI khi data thay đổi
 * - Tối ưu query với Firestore listeners
 */
public class AdminViewModel extends AndroidViewModel {

    private static final String TAG = "AdminViewModel";
    private static final String COL_ORDERS = "orders";
    private static final String COL_USERS = "users";

    // LiveData cho Dashboard Stats
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

    // LiveData cho loading/error states
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // Firestore & Listeners
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration ordersListener;
    private ListenerRegistration usersListener;

    public AdminViewModel(@NonNull Application application) {
        super(application);
    }

    // ==================== GETTERS CHO LIVE DATA ====================

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

    // ==================== REAL-TIME LISTENERS ====================

    /**
     * Bắt đầu lắng nghe real-time changes
     * Gọi trong onStart() của Activity
     */
    public void startListening() {
        Log.d(TAG, "startListening: Starting real-time listeners");
        isLoading.postValue(true);

        // Lắng nghe orders collection
        startOrdersListener();

        // Lắng nghe users collection
        startUsersListener();
    }

    /**
     * Dừng lắng nghe khi Activity bị destroy
     * Gọi trong onStop() của Activity
     */
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
        // Sử dụng listener thay vì one-time fetch
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

                        // Tính toán tất cả stats từ snapshots
                        calculateOrderStats(snapshots);
                        isLoading.postValue(false);
                        Log.d(TAG, "Orders updated: " + snapshots.size() + " documents");
                    }
                });
    }

    private void startUsersListener() {
        usersListener = db.collection(COL_USERS)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "Users listener error: " + e.getMessage());
                            return;
                        }

                        int count = snapshots != null ? snapshots.size() : 0;
                        totalUsers.postValue((long) count);
                        Log.d(TAG, "Users updated: " + count);
                    }
                });
    }

    // ==================== TÍNH TOÁN STATS ====================

    private void calculateOrderStats(QuerySnapshot snapshots) {
        long total = 0;
        long pending = 0;
        long completed = 0;
        long cancelled = 0;
        long preparing = 0;
        long shipping = 0;
        long delivered = 0;
        long revenue = 0;
        long refunded = 0;

        for (var doc : snapshots.getDocuments()) {
            total++;

            String status = doc.getString("status");
            Double amount = doc.getDouble("totalAmount");
            if (amount == null) amount = 0.0;

            // Đếm theo status
            switch (status != null ? status : "") {
                case Order.STATUS_PENDING:
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
                    // DELIVERED vẫn tính doanh thu
                    revenue += amount.longValue();
                    break;
                case Order.STATUS_COMPLETED:
                    completed++;
                    // COMPLETED tính doanh thu
                    revenue += amount.longValue();
                    break;
                case Order.STATUS_CANCELLED:
                    cancelled++;
                    break;
                case Order.STATUS_REFUNDED:
                    refunded += amount.longValue(); // Hoàn tiền
                    break;
            }
        }

        // Cập nhật LiveData
        totalOrders.postValue(total);
        pendingOrders.postValue(pending);
        preparingOrders.postValue(preparing);
        shippingOrders.postValue(shipping);
        deliveredOrders.postValue(delivered);
        completedOrders.postValue(completed);
        cancelledOrders.postValue(cancelled);

        // Doanh thu = COMPLETED + DELIVERED - REFUNDED
        long netRevenue = revenue - refunded;
        totalRevenue.postValue(netRevenue);
        refundedAmount.postValue(refunded);

        Log.d(TAG, "Stats calculated - Revenue: " + netRevenue + ", Pending: " + pending + ", Completed: " + completed);
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

    // ==================== CLEANUP ====================

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
        Log.d(TAG, "onCleared: ViewModel destroyed");
    }
}
