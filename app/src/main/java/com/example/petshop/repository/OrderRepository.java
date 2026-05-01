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
    private static final String COL_USERS  = "users";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ====== CREATE ORDER (atomic) ======
    public void createOrder(Order order, List<CartItem> cartItems, Callback<String> cb) {
        String orderId   = UUID.randomUUID().toString();
        String orderCode = "ORD-"
                + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date())
                + "-" + orderId.substring(0, 4).toUpperCase();
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
                // 1. COLLECT ALL READS FIRST
                DocumentReference userRef = db.collection(COL_USERS).document(order.getUserId());
                
                // Collect food references
                Map<String, DocumentReference> foodRefs = new HashMap<>();
                for (OrderItem item : orderItems) {
                    if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                        foodRefs.put(item.getProductId(), db.collection(COL_FOODS).document(item.getProductId()));
                    }
                }
                
                // Perform all gets
                DocumentSnapshot userSnap = tx.get(userRef);
                Map<String, DocumentSnapshot> foodSnaps = new HashMap<>();
                for (Map.Entry<String, DocumentReference> entry : foodRefs.entrySet()) {
                    foodSnaps.put(entry.getKey(), tx.get(entry.getValue()));
                }

                // 2. PERFORM ALL WRITES
                // Subtract stock
                for (OrderItem item : orderItems) {
                    if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                        tx.update(db.collection(COL_PETS).document(item.getProductId()), "status", Pet.STATUS_SOLD);
                    } else {
                        DocumentSnapshot snap = foodSnaps.get(item.getProductId());
                        long stock = (snap != null && snap.getLong("stock") != null) ? snap.getLong("stock") : 0;
                        long sold  = (snap != null && snap.getLong("sold") != null) ? snap.getLong("sold") : 0;
                        tx.update(foodRefs.get(item.getProductId()),
                                "stock", Math.max(0, stock - item.getQuantity()),
                                "sold",  sold + item.getQuantity());
                    }
                }

                // Update User stats
                if (userSnap.exists()) {
                    long totalOrders = userSnap.getLong("totalOrders") != null ? userSnap.getLong("totalOrders") : 0;
                    double totalSpent = userSnap.getDouble("totalSpent") != null ? userSnap.getDouble("totalSpent") : 0;
                    tx.update(userRef, "totalOrders", totalOrders + 1, "totalSpent", totalSpent + order.getTotalAmount());
                }
            }
            
            tx.set(db.collection(COL_ORDERS).document(orderId), order);
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(orderId))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private void subtractStockInTx(Transaction tx, List<OrderItem> items) throws Exception {
        Map<String, DocumentReference> foodRefs = new HashMap<>();
        for (OrderItem item : items) {
            if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                foodRefs.put(item.getProductId(), db.collection(COL_FOODS).document(item.getProductId()));
            }
        }

        Map<String, DocumentSnapshot> foodSnaps = new HashMap<>();
        for (Map.Entry<String, DocumentReference> entry : foodRefs.entrySet()) {
            foodSnaps.put(entry.getKey(), tx.get(entry.getValue()));
        }

        for (OrderItem item : items) {
            if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                tx.update(db.collection(COL_PETS).document(item.getProductId()), "status", Pet.STATUS_SOLD);
            } else {
                DocumentSnapshot snap = foodSnaps.get(item.getProductId());
                long stock = (snap != null && snap.getLong("stock") != null) ? snap.getLong("stock") : 0;
                long sold  = (snap != null && snap.getLong("sold") != null) ? snap.getLong("sold") : 0;
                tx.update(foodRefs.get(item.getProductId()),
                        "stock", Math.max(0, stock - item.getQuantity()),
                        "sold",  sold + item.getQuantity());
            }
        }
    }

    private void restoreStockInTx(Transaction tx, List<OrderItem> items) throws Exception {
        Map<String, DocumentReference> foodRefs = new HashMap<>();
        for (OrderItem item : items) {
            if (OrderItem.PRODUCT_TYPE_FOOD.equals(item.getProductType())) {
                foodRefs.put(item.getProductId(), db.collection(COL_FOODS).document(item.getProductId()));
            }
        }

        Map<String, DocumentSnapshot> foodSnaps = new HashMap<>();
        for (Map.Entry<String, DocumentReference> entry : foodRefs.entrySet()) {
            foodSnaps.put(entry.getKey(), tx.get(entry.getValue()));
        }

        for (OrderItem item : items) {
            if (OrderItem.PRODUCT_TYPE_PET.equals(item.getProductType())) {
                tx.update(db.collection(COL_PETS).document(item.getProductId()), "status", Pet.STATUS_AVAILABLE);
            } else {
                DocumentSnapshot snap = foodSnaps.get(item.getProductId());
                long stock = (snap != null && snap.getLong("stock") != null) ? snap.getLong("stock") : 0;
                long sold  = (snap != null && snap.getLong("sold") != null) ? snap.getLong("sold") : 0;
                tx.update(foodRefs.get(item.getProductId()),
                        "stock", stock + item.getQuantity(),
                        "sold",  Math.max(0, sold - item.getQuantity()));
            }
        }
    }

    public void completeVNPayOrder(String orderId, Callback<Void> cb) {
        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentReference orderRef = db.collection(COL_ORDERS).document(orderId);
            DocumentSnapshot orderSnap = tx.get(orderRef);
            Order order = orderSnap.toObject(Order.class);
            
            if (order == null || !Order.STATUS_WAIT_PAY.equals(order.getStatus())) return null;

            try {
                subtractStockInTx(tx, order.getItems());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            DocumentReference userRef = db.collection(COL_USERS).document(order.getUserId());
            DocumentSnapshot userSnap = tx.get(userRef);
            if (userSnap.exists()) {
                long totalOrders = userSnap.getLong("totalOrders") != null ? userSnap.getLong("totalOrders") : 0;
                double totalSpent = userSnap.getDouble("totalSpent") != null ? userSnap.getDouble("totalSpent") : 0;
                tx.update(userRef, "totalOrders", totalOrders + 1, "totalSpent", totalSpent + order.getTotalAmount());
            }

            tx.update(orderRef, 
                    "status", Order.STATUS_PENDING,
                    "paymentStatus", Order.PAY_STATUS_PAID,
                    "updatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
            
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(null))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void cancelOrder(String orderId, String reason, Callback<Void> cb) {
        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentReference orderRef = db.collection(COL_ORDERS).document(orderId);
            DocumentSnapshot orderSnap = tx.get(orderRef);
            Order order = orderSnap.toObject(Order.class);
            
            if (order == null) throw new RuntimeException("Không tìm thấy đơn hàng");
            if (!order.canCancel()) throw new RuntimeException("Không thể hủy đơn hàng này");

            boolean wasSubtracted = !Order.STATUS_WAIT_PAY.equals(order.getStatus());
            
            if (wasSubtracted) {
                // Restore stock
                try {
                    restoreStockInTx(tx, order.getItems());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // Reverse user stats
                DocumentReference userRef = db.collection(COL_USERS).document(order.getUserId());
                DocumentSnapshot userSnap = tx.get(userRef);
                if (userSnap.exists()) {
                    long totalOrders = userSnap.getLong("totalOrders") != null ? userSnap.getLong("totalOrders") : 0;
                    double totalSpent = userSnap.getDouble("totalSpent") != null ? userSnap.getDouble("totalSpent") : 0;
                    tx.update(userRef, 
                            "totalOrders", Math.max(0, totalOrders - 1),
                            "totalSpent", Math.max(0, totalSpent - order.getTotalAmount()));
                }
            }

            tx.update(orderRef, 
                    "status", Order.STATUS_CANCELLED,
                    "cancelReason", reason,
                    "updatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
            
            return null;
        })
        .addOnSuccessListener(v -> cb.onSuccess(null))
        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getAllOrders(Callback<List<Order>> cb) {
        db.collection(COL_ORDERS).orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Order o = doc.toObject(Order.class);
                        if (o != null) { o.setId(doc.getId()); list.add(o); }
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrdersByUser(String userId, Callback<List<Order>> cb) {
        db.collection(COL_ORDERS).whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Order o = doc.toObject(Order.class);
                        if (o != null) { o.setId(doc.getId()); list.add(o); }
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrderById(String orderId, Callback<Order> cb) {
        db.collection(COL_ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> {
                    Order o = doc.toObject(Order.class);
                    if (o != null) o.setId(doc.getId());
                    cb.onSuccess(o);
                }).addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getOrderByCode(String orderCode, Callback<Order> cb) {
        db.collection(COL_ORDERS).whereEqualTo("orderCode", orderCode).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Order o = snap.getDocuments().get(0).toObject(Order.class);
                        if (o != null) o.setId(snap.getDocuments().get(0).getId());
                        cb.onSuccess(o);
                    } else cb.onFailure("Order not found");
                }).addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateStatus(String orderId, String newStatus, String note, Callback<Void> cb) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", now);
        if (Order.STATUS_DELIVERED.equals(newStatus)) updates.put("deliveredAt", now);
        if (Order.PAY_STATUS_PAID.equals(newStatus)) updates.put("paidAt", now);
        if (note != null && !note.isEmpty()) updates.put("adminNote", note);

        db.collection(COL_ORDERS).document(orderId).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

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
