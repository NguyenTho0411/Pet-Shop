package com.example.petshop.repository;

import com.example.petshop.model.entity.CartItem;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.OrderItem;
import com.example.petshop.model.entity.Pet;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OrderRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL_ORDERS = "orders";
    private static final String COL_PETS   = "pets";
    private static final String COL_FOODS  = "foods";
    private static final String COL_USERS  = "users";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ====== CREATE ORDER (atomic) ======
    public void createOrder(Order order, List<CartItem> cartItems, Callback<String> cb) {
        String orderId   = UUID.randomUUID().toString();
        String orderCode = "ORD" + new SimpleDateFormat("MMddHHmmss", Locale.getDefault()).format(new Date())
                + orderId.substring(0, 6).toUpperCase();

        order.setId(orderId);
        order.setOrderCode(orderCode);

        boolean isVNPay = Order.PAYMENT_VNPAY.equals(order.getPaymentMethod());
        if (isVNPay) {
            order.setStatus(Order.STATUS_WAIT_PAY);
        } else {
            order.setStatus(Order.STATUS_PENDING);
        }

        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setId(UUID.randomUUID().toString());
            oi.setOrderId(orderId);
            oi.setProductType(ci.getProductType());
            oi.setProductId(ci.getProductId());
            oi.setProductName(ci.getProductName());
            oi.setProductThumbnail(ci.getProductThumbnail());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setSubtotal(ci.getSubtotal());
            orderItems.add(oi);
        }
        order.setItems(orderItems);

        db.runTransaction((Transaction.Function<Void>) tx -> {
                    if (!isVNPay) {
                        Map<String, DocumentReference> foodRefs = new HashMap<>();
                        for (OrderItem item : orderItems) {
                            if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                                foodRefs.put(item.getProductId(), db.collection(COL_FOODS).document(item.getProductId()));
                            }
                        }

                        Map<String, DocumentSnapshot> foodSnaps = new HashMap<>();
                        for (Map.Entry<String, DocumentReference> entry : foodRefs.entrySet()) {
                            foodSnaps.put(entry.getKey(), tx.get(entry.getValue()));
                        }

                        for (OrderItem item : orderItems) {
                            if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                                tx.update(db.collection(COL_PETS).document(item.getProductId()),
                                        "status", Pet.STATUS_RESERVED);
                            } else if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                                DocumentSnapshot snap = foodSnaps.get(item.getProductId());
                                long stock = snap != null && snap.getLong("stock") != null
                                        ? snap.getLong("stock") : 0;
                                tx.update(foodRefs.get(item.getProductId()),
                                        "stock", Math.max(0, stock - item.getQuantity()));
                            }
                        }
                    }

                    tx.set(db.collection(COL_ORDERS).document(orderId), order);
                    return null;
                })
                .addOnSuccessListener(v -> cb.onSuccess(orderId))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void completeVNPayOrder(String orderId, Callback<Void> cb) {
        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentReference orderRef = db.collection(COL_ORDERS).document(orderId);
                    DocumentSnapshot orderSnap = tx.get(orderRef);
                    Order order = orderSnap.toObject(Order.class);

                    if (order == null || !Order.STATUS_WAIT_PAY.equals(order.getStatus())) {
                        return null;
                    }

                    Map<String, DocumentReference> foodRefs = new HashMap<>();
                    for (OrderItem item : order.getItems()) {
                        if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                            foodRefs.put(item.getProductId(), db.collection(COL_FOODS).document(item.getProductId()));
                        }
                    }

                    Map<String, DocumentSnapshot> foodSnaps = new HashMap<>();
                    for (Map.Entry<String, DocumentReference> entry : foodRefs.entrySet()) {
                        foodSnaps.put(entry.getKey(), tx.get(entry.getValue()));
                    }

                    for (OrderItem item : order.getItems()) {
                        if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                            tx.update(db.collection(COL_PETS).document(item.getProductId()),
                                    "status", Pet.STATUS_RESERVED);
                        } else if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                            DocumentSnapshot foodSnap = foodSnaps.get(item.getProductId());
                            long stock = foodSnap != null && foodSnap.getLong("stock") != null
                                    ? foodSnap.getLong("stock") : 0;
                            tx.update(foodRefs.get(item.getProductId()),
                                    "stock", Math.max(0, stock - item.getQuantity()));
                        }
                    }

                    tx.update(orderRef,
                            "status", Order.STATUS_PENDING,
                            "paymentStatus", Order.PAY_STATUS_PAID,
                            "updatedAt", now());

                    return null;
                })
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void cancelOrder(String orderId, String reason, Callback<Void> cb) {
        final String[] cancelledVoucherIds = {null};

        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentReference orderRef = db.collection(COL_ORDERS).document(orderId);
                    DocumentSnapshot orderSnap = tx.get(orderRef);
                    Order order = orderSnap.toObject(Order.class);

                    if (order == null) {
                        throw new RuntimeException("Không tìm thấy đơn hàng");
                    }

                    if (!order.canCancel()) {
                        throw new RuntimeException("Không thể hủy đơn hàng này");
                    }

                    cancelledVoucherIds[0] = order.getVoucherId();

                    boolean wasSubtracted = !Order.STATUS_WAIT_PAY.equals(order.getStatus());

                    List<OrderItem> items = order.getItems() != null
                            ? order.getItems()
                            : new ArrayList<>();

                    Map<String, DocumentReference> foodRefs = new HashMap<>();
                    Map<String, DocumentSnapshot> foodSnaps = new HashMap<>();

                    DocumentReference userRef = null;
                    DocumentSnapshot userSnap = null;

                    if (wasSubtracted) {
                        for (OrderItem item : items) {
                            if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                                foodRefs.put(item.getProductId(),
                                        db.collection(COL_FOODS).document(item.getProductId()));
                            }
                        }

                        for (Map.Entry<String, DocumentReference> entry : foodRefs.entrySet()) {
                            foodSnaps.put(entry.getKey(), tx.get(entry.getValue()));
                        }

                        userRef = db.collection(COL_USERS).document(order.getUserId());
                        userSnap = tx.get(userRef);
                    }

                    if (wasSubtracted) {
                        for (OrderItem item : items) {
                            if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                                tx.update(db.collection(COL_PETS).document(item.getProductId()),
                                        "status", Pet.STATUS_AVAILABLE);
                            } else if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                                DocumentSnapshot snap = foodSnaps.get(item.getProductId());

                                long stock = 0;
                                long sold = 0;

                                if (snap != null) {
                                    Long stockValue = snap.getLong("stock");
                                    Long soldValue = snap.getLong("sold");

                                    stock = stockValue != null ? stockValue : 0;
                                    sold = soldValue != null ? soldValue : 0;
                                }

                                tx.update(foodRefs.get(item.getProductId()),
                                        "stock", stock + item.getQuantity(),
                                        "sold", Math.max(0, sold - item.getQuantity()));
                            }
                        }

                        boolean counted = countsForCustomerStats(
                                order.getStatus(),
                                order.getPaymentStatus(),
                                order.getPaymentMethod()
                        );

                        if (counted && userSnap != null && userSnap.exists() && userRef != null) {
                            long totalOrders = userSnap.getLong("totalOrders") != null
                                    ? userSnap.getLong("totalOrders") : 0;
                            double totalSpent = userSnap.getDouble("totalSpent") != null
                                    ? userSnap.getDouble("totalSpent") : 0;

                            tx.update(userRef,
                                    "totalOrders", Math.max(0, totalOrders - 1),
                                    "totalSpent", Math.max(0, totalSpent - order.getTotalAmount()));
                        }
                    }

                    tx.update(orderRef,
                            "status", Order.STATUS_CANCELLED,
                            "paymentStatus", Order.PAY_STATUS_FAILED,
                            "cancelReason", reason,
                            "updatedAt", now());

                    return null;
                })
                .addOnSuccessListener(v -> {
                    restoreVoucherUsage(cancelledVoucherIds[0]);
                    cb.onSuccess(null);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getAllOrders(Callback<List<Order>> cb) {
        db.collection(COL_ORDERS).orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Order o = doc.toObject(Order.class);
                        if (o != null) {
                            o.setId(doc.getId());
                            list.add(o);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrdersByUser(String userId, Callback<List<Order>> cb) {
        db.collection(COL_ORDERS).whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Order o = doc.toObject(Order.class);
                        if (o != null) {
                            o.setId(doc.getId());
                            list.add(o);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrderById(String orderId, Callback<Order> cb) {
        db.collection(COL_ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> {
                    Order o = doc.toObject(Order.class);
                    if (o != null) o.setId(doc.getId());
                    cb.onSuccess(o);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrderByCode(String orderCode, Callback<Order> cb) {
        db.collection(COL_ORDERS).whereEqualTo("orderCode", orderCode).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Order o = snap.getDocuments().get(0).toObject(Order.class);
                        if (o != null) o.setId(snap.getDocuments().get(0).getId());
                        cb.onSuccess(o);
                    } else {
                        cb.onFailure("Order not found");
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateStatus(String orderId, String newStatus, String note, Callback<Void> cb) {
        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentReference orderRef = db.collection(COL_ORDERS).document(orderId);
                    DocumentSnapshot orderSnap = tx.get(orderRef);
                    Order order = orderSnap.toObject(Order.class);

                    if (order == null) {
                        throw new RuntimeException("Không tìm thấy đơn hàng");
                    }

                    String oldStatus = order.getStatus();
                    String oldPaymentStatus = order.getPaymentStatus();
                    String paymentMethod = order.getPaymentMethod();

                    boolean wasCounted = countsForCustomerStats(oldStatus, oldPaymentStatus, paymentMethod);

                    String nextPaymentStatus = oldPaymentStatus;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", newStatus);
                    updates.put("updatedAt", now());

                    if (Order.STATUS_DELIVERED.equals(newStatus)
                            || Order.STATUS_COMPLETED.equals(newStatus)) {
                        nextPaymentStatus = Order.PAY_STATUS_PAID;
                        updates.put("paymentStatus", Order.PAY_STATUS_PAID);
                        updates.put("paidAt", now());
                    }

                    if (Order.STATUS_DELIVERED.equals(newStatus)) {
                        updates.put("deliveredAt", now());
                    }

                    if (Order.STATUS_REFUNDED.equals(newStatus)) {
                        nextPaymentStatus = Order.PAY_STATUS_REFUNDED;
                        updates.put("paymentStatus", Order.PAY_STATUS_REFUNDED);
                    }

                    if (Order.STATUS_CANCELLED.equals(newStatus)
                            && !Order.PAY_STATUS_PAID.equals(oldPaymentStatus)) {
                        nextPaymentStatus = Order.PAY_STATUS_FAILED;
                        updates.put("paymentStatus", Order.PAY_STATUS_FAILED);
                    }

                    if (note != null && !note.isEmpty()) {
                        updates.put("adminNote", note);
                    }

                    boolean willCount = countsForCustomerStats(newStatus, nextPaymentStatus, paymentMethod);

                    if (wasCounted != willCount) {
                        DocumentReference userRef = db.collection(COL_USERS).document(order.getUserId());
                        DocumentSnapshot userSnap = tx.get(userRef);

                        if (userSnap.exists()) {
                            long totalOrders = userSnap.getLong("totalOrders") != null
                                    ? userSnap.getLong("totalOrders") : 0;
                            double totalSpent = userSnap.getDouble("totalSpent") != null
                                    ? userSnap.getDouble("totalSpent") : 0.0;

                            long deltaOrders = willCount ? 1 : -1;
                            double deltaSpent = willCount ? order.getTotalAmount() : -order.getTotalAmount();

                            tx.update(userRef,
                                    "totalOrders", Math.max(0, totalOrders + deltaOrders),
                                    "totalSpent", Math.max(0, totalSpent + deltaSpent));
                        }
                    }

                    tx.update(orderRef, updates);
                    return null;
                })
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void requestReturn(String orderId, String reason, boolean hasPet, Callback<Void> cb) {
        Map<String, Object> upd = new HashMap<>();
        upd.put("status", Order.STATUS_RETURN_REQUESTED);
        upd.put("cancelReason", reason);
        upd.put("updatedAt", now());

        db.collection(COL_ORDERS).document(orderId).update(upd)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void deleteOrder(String orderId, Callback<Void> cb) {
        db.collection(COL_ORDERS)
                .document(orderId)
                .delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void countPending(Callback<Long> cb) {
        db.collection(COL_ORDERS).whereEqualTo("status", Order.STATUS_PENDING)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener(r -> cb.onSuccess(r.getCount()))
                .addOnFailureListener(e -> cb.onSuccess(0L));
    }

    private boolean countsForCustomerStats(String status, String paymentStatus, String paymentMethod) {
        if (!(Order.STATUS_DELIVERED.equals(status) || Order.STATUS_COMPLETED.equals(status))) {
            return false;
        }

        return Order.PAY_STATUS_PAID.equals(paymentStatus)
                || Order.PAYMENT_COD.equals(paymentMethod);
    }

    private void restoreVoucherUsage(String voucherIds) {
        if (voucherIds == null || voucherIds.trim().isEmpty()) return;

        Set<String> uniqueIds = new HashSet<>();
        String[] ids = voucherIds.split(",");

        for (String rawId : ids) {
            String voucherId = rawId.trim();
            if (!voucherId.isEmpty()) uniqueIds.add(voucherId);
        }

        for (String voucherId : uniqueIds) {
            new VoucherRepository().decrementUsageCount(voucherId, new VoucherRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    android.util.Log.d("OrderRepo", "Voucher usage restored: " + voucherId);
                }

                @Override
                public void onFailure(String error) {
                    new PromotionRepository().decrementUsageCount(voucherId, new PromotionRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            android.util.Log.d("OrderRepo", "Promotion voucher usage restored: " + voucherId);
                        }

                        @Override
                        public void onFailure(String error2) {
                            android.util.Log.e("OrderRepo", "Failed to restore voucher usage: " + error2);
                        }
                    });
                }
            });
        }
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}