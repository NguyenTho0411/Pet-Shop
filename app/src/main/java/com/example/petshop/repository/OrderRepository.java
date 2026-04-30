package com.example.petshop.repository;

import com.example.petshop.model.entity.CartItem;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.OrderItem;
import com.example.petshop.model.entity.Pet;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class OrderRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL_ORDERS = "orders";
    private static final String COL_PETS   = "pets";
    private static final String COL_FOODS  = "foods";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ====== CREATE ORDER (atomic) ======
    public void createOrder(Order order, List<CartItem> cartItems, Callback<String> cb) {
        String orderId   = UUID.randomUUID().toString();
        String orderCode = "ORD-"
                + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date())
                + "-" + orderId.substring(0, 4).toUpperCase();
        order.setId(orderId);
        order.setOrderCode(orderCode);
        order.setStatus(Order.STATUS_PENDING);
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

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Step 1: Collect all necessary data (Reads)
            Map<String, com.google.firebase.firestore.DocumentSnapshot> foodDocs = new HashMap<>();
            for (CartItem item : cartItems) {
                if (CartItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                    var ref = db.collection(COL_FOODS).document(item.getProductId());
                    foodDocs.put(item.getProductId(), transaction.get(ref));
                }
            }

            // Step 2: Perform updates (Writes)
            for (CartItem item : cartItems) {
                if (CartItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                    // Pet: mark SOLD (permanent)
                    transaction.update(
                            db.collection(COL_PETS).document(item.getProductId()),
                            "status", Pet.STATUS_SOLD);
                } else {
                    // Food: reduce stock
                    var foodRef = db.collection(COL_FOODS).document(item.getProductId());
                    var foodDoc = foodDocs.get(item.getProductId());
                    long stock = foodDoc != null && foodDoc.getLong("stock") != null ? foodDoc.getLong("stock") : 0;
                    long sold  = foodDoc != null && foodDoc.getLong("sold")  != null ? foodDoc.getLong("sold")  : 0;
                    transaction.update(foodRef,
                            "stock", Math.max(0, stock - item.getQuantity()),
                            "sold",  sold + item.getQuantity());
                }
            }
            transaction.set(db.collection(COL_ORDERS).document(orderId), order);
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(orderId))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ====== GET ALL (admin) ======
    public void getAllOrders(Callback<List<Order>> cb) {
        db.collection(COL_ORDERS)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) list.add(doc.toObject(Order.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrdersByStatus(String status, Callback<List<Order>> cb) {
        db.collection(COL_ORDERS).whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) list.add(doc.toObject(Order.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ====== GET BY USER ======
    public void getOrdersByUser(String userId, Callback<List<Order>> cb) {
        db.collection(COL_ORDERS).whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) list.add(doc.toObject(Order.class));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrderById(String orderId, Callback<Order> cb) {
        db.collection(COL_ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> cb.onSuccess(doc.toObject(Order.class)))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ====== UPDATE STATUS (admin) ======
    public void updateStatus(String orderId, String newStatus, String note, Callback<Void> cb) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", now);
        if (Order.STATUS_DELIVERED.equals(newStatus))
            updates.put("deliveredAt", now);
        if (Order.PAY_STATUS_PAID.equals(newStatus))
            updates.put("paidAt", now);
        if (note != null && !note.isEmpty()) updates.put("adminNote", note);

        db.collection(COL_ORDERS).document(orderId).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ====== CANCEL (customer: PENDING or CONFIRMED only) ======
    public void cancelOrder(String orderId, String reason, Callback<Void> cb) {
        db.collection(COL_ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> {
                    Order order = doc.toObject(Order.class);
                    if (order == null) { cb.onFailure("Không tìm thấy đơn hàng"); return; }
                    if (!order.canCancel()) {
                        cb.onFailure("Không thể huỷ đơn ở trạng thái \"" + order.getStatus() + "\"");
                        return;
                    }
                    db.runTransaction((Transaction.Function<Void>) tx -> {
                        // Step 1: Collect reads
                        Map<String, com.google.firebase.firestore.DocumentSnapshot> foodDocs = new HashMap<>();
                        if (order.getItems() != null) {
                            for (OrderItem item : order.getItems()) {
                                if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                                    var fr = db.collection(COL_FOODS).document(item.getProductId());
                                    foodDocs.put(item.getProductId(), tx.get(fr));
                                }
                            }
                        }

                        // Step 2: Perform writes
                        if (order.getItems() != null) {
                            for (OrderItem item : order.getItems()) {
                                if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                                    tx.update(
                                            db.collection(COL_PETS).document(item.getProductId()),
                                            "status", Pet.STATUS_AVAILABLE);
                                } else {
                                    var fr = db.collection(COL_FOODS).document(item.getProductId());
                                    var fd = foodDocs.get(item.getProductId());
                                    long s = fd != null && fd.getLong("stock") != null ? fd.getLong("stock") : 0;
                                    tx.update(fr, "stock", s + item.getQuantity());
                                }
                            }
                        }
                        tx.update(db.collection(COL_ORDERS).document(orderId),
                                "status", Order.STATUS_CANCELLED,
                                "cancelReason", reason,
                                "updatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
                        return null;
                    })
                    .addOnSuccessListener(v -> cb.onSuccess(null))
                    .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // ====== REQUEST RETURN ======
    public void requestReturn(String orderId, String reason, boolean hasPet, Callback<Void> cb) {
        Map<String, Object> upd = new HashMap<>();
        upd.put("status", Order.STATUS_REFUNDED);
        upd.put("cancelReason", reason);
        upd.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
        db.collection(COL_ORDERS).document(orderId).update(upd)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void countPending(Callback<Long> cb) {
        db.collection(COL_ORDERS).whereEqualTo("status", Order.STATUS_PENDING)
                .count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener(r -> cb.onSuccess(r.getCount()))
                .addOnFailureListener(e -> cb.onSuccess(0L));
    }
}
