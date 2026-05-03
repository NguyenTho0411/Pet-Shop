package com.example.petshop.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.petshop.model.entity.AppNotification;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.Voucher;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL_ORDERS = "orders";
    private static final String COL_VOUCHERS = "vouchers";
    private static final String PREF_NAME = "petshop_notification_reads";
    private static final String KEY_READ_IDS = "read_ids_";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    public void getNotifications(Context context, String userId, Callback<List<AppNotification>> cb) {
        if (userId == null || userId.isEmpty()) {
            cb.onFailure("Vui lòng đăng nhập");
            return;
        }

        List<AppNotification> result = new ArrayList<>();

        db.collection(COL_ORDERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(orderSnap -> {
                    for (DocumentSnapshot doc : orderSnap.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order == null) continue;

                        order.setId(doc.getId());

                        AppNotification n = buildOrderNotification(order);
                        if (n != null) {
                            result.add(n);
                        }
                    }

                    loadVoucherNotifications(context, userId, result, cb);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private void loadVoucherNotifications(Context context,
                                          String userId,
                                          List<AppNotification> result,
                                          Callback<List<AppNotification>> cb) {
        db.collection(COL_VOUCHERS)
                .get()
                .addOnSuccessListener(voucherSnap -> {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new java.util.Date());

                    for (DocumentSnapshot doc : voucherSnap.getDocuments()) {
                        Voucher voucher = doc.toObject(Voucher.class);
                        if (voucher == null) continue;

                        voucher.setId(doc.getId());

                        if (!voucher.isActive()) continue;
                        if (voucher.isExpired(today)) continue;
                        if (voucher.isUsageLimitReached()) continue;

                        AppNotification n = buildVoucherNotification(voucher);
                        result.add(n);
                    }

                    applyReadStateAndSort(context, userId, result);
                    cb.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    // Nếu voucher lỗi thì vẫn cho hiện thông báo đơn hàng
                    applyReadStateAndSort(context, userId, result);
                    cb.onSuccess(result);
                });
    }

    public void getUnreadCount(Context context, String userId, Callback<Integer> cb) {
        getNotifications(context, userId, new Callback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> data) {
                int count = 0;
                for (AppNotification n : data) {
                    if (!n.isRead()) count++;
                }
                cb.onSuccess(count);
            }

            @Override
            public void onFailure(String error) {
                cb.onFailure(error);
            }
        });
    }

    public void markAllAsRead(Context context, String userId, List<AppNotification> notifications) {
        if (context == null || userId == null || notifications == null) return;

        Set<String> readIds = getReadIds(context, userId);

        for (AppNotification n : notifications) {
            if (n.getId() != null) {
                readIds.add(n.getId());
                n.setRead(true);
            }
        }

        saveReadIds(context, userId, readIds);
    }

    private AppNotification buildOrderNotification(Order order) {
        if (order.getId() == null) return null;

        String status = order.getStatus();
        String code = order.getOrderCode() != null ? order.getOrderCode() : order.getId();
        String time = safeTime(order.getUpdatedAt(), order.getCreatedAt());

        String title;
        String message;

        if (Order.STATUS_WAIT_PAY.equals(status)) {
            title = "Đơn hàng chờ thanh toán";
            message = "Đơn " + code + " đang chờ thanh toán VNPay.";
        } else if (Order.STATUS_PENDING.equals(status)) {
            title = "Đơn hàng chờ xác nhận";
            message = "Đơn " + code + " đã được tạo và đang chờ shop xác nhận.";
        } else if (Order.STATUS_CONFIRMED.equals(status)) {
            title = "Đơn hàng đã được xác nhận";
            message = "Đơn " + code + " đã được shop xác nhận.";
        } else if (Order.STATUS_PREPARING.equals(status)) {
            title = "Đơn hàng đang chuẩn bị";
            message = "Đơn " + code + " đang được chuẩn bị.";
        } else if (Order.STATUS_SHIPPING.equals(status)) {
            title = "Đơn hàng đang giao";
            message = "Đơn " + code + " đang trên đường giao đến bạn.";
        } else if (Order.STATUS_DELIVERED.equals(status)) {
            title = "Đơn hàng đã giao";
            message = "Đơn " + code + " đã được giao thành công.";
        } else if (Order.STATUS_COMPLETED.equals(status)) {
            title = "Đơn hàng hoàn thành";
            message = "Đơn " + code + " đã hoàn thành. Cảm ơn bạn đã mua hàng.";
        } else if (Order.STATUS_CANCELLED.equals(status)) {
            title = "Đơn hàng đã hủy";
            message = "Đơn " + code + " đã được hủy.";
        } else if (Order.STATUS_REFUNDED.equals(status)) {
            title = "Đơn hàng đã hoàn tiền";
            message = "Đơn " + code + " đã được hoàn tiền.";
        } else {
            title = "Cập nhật đơn hàng";
            message = "Đơn " + code + " có trạng thái mới: " + status;
        }

        String id = "order_" + order.getId() + "_" + status + "_" + time;

        return new AppNotification(
                id,
                AppNotification.TYPE_ORDER,
                title,
                message,
                time,
                order.getId(),
                false
        );
    }

    private AppNotification buildVoucherNotification(Voucher voucher) {
        String code = voucher.getCode() != null ? voucher.getCode() : "VOUCHER";
        String title = "Mã giảm giá " + code;

        StringBuilder msg = new StringBuilder();

        if (voucher.getDescription() != null && !voucher.getDescription().isEmpty()) {
            msg.append(voucher.getDescription());
        } else if (voucher.getName() != null && !voucher.getName().isEmpty()) {
            msg.append(voucher.getName());
        } else {
            msg.append("Bạn có mã giảm giá mới có thể sử dụng.");
        }

        if (voucher.getMinOrderAmount() > 0) {
            msg.append(" Áp dụng cho đơn từ ")
                    .append(VND.format((long) voucher.getMinOrderAmount()))
                    .append("đ.");
        }

        if (voucher.getEndDate() != null && !voucher.getEndDate().isEmpty()) {
            msg.append(" Hạn dùng đến ")
                    .append(voucher.getEndDate())
                    .append(".");
        }

        String time = safeTime(voucher.getStartDate(), voucher.getCreatedAt());
        String id = "voucher_" + voucher.getId() + "_" + code + "_" + time;

        return new AppNotification(
                id,
                AppNotification.TYPE_VOUCHER,
                title,
                msg.toString(),
                time,
                null,
                false
        );
    }

    private void applyReadStateAndSort(Context context, String userId, List<AppNotification> list) {
        Set<String> readIds = getReadIds(context, userId);

        for (AppNotification n : list) {
            n.setRead(readIds.contains(n.getId()));
        }

        list.sort((a, b) -> {
            String ta = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String tb = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return tb.compareTo(ta);
        });
    }

    private String safeTime(String primary, String fallback) {
        if (primary != null && !primary.isEmpty()) return primary;
        if (fallback != null && !fallback.isEmpty()) return fallback;
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new java.util.Date());
    }

    private Set<String> getReadIds(Context context, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> oldSet = prefs.getStringSet(KEY_READ_IDS + userId, new HashSet<>());
        return new HashSet<>(oldSet);
    }

    private void saveReadIds(Context context, String userId, Set<String> readIds) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_READ_IDS + userId, new HashSet<>(readIds))
                .apply();
    }
}