package com.example.petshop.repository;

import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Promotion;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PromotionRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL = "promotions";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAll(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            list.add(p);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getById(String id, Callback<Promotion> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    Promotion p = doc.toObject(Promotion.class);
                    if (p != null) p.setId(doc.getId());
                    cb.onSuccess(p);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByCode(String code, Callback<Promotion> cb) {
        db.collection(COL)
                .whereEqualTo("voucherCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Promotion p = snap.getDocuments().get(0).toObject(Promotion.class);
                        if (p != null) p.setId(snap.getDocuments().get(0).getId());
                        cb.onSuccess(p);
                    } else {
                        cb.onFailure("Không tìm thấy mã voucher");
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getSystemVouchers(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .whereEqualTo("promotionType", "VOUCHER")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        if (p != null && p.isWithinDateRange()) {
                            p.setId(doc.getId());
                            list.add(p);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getActive(Callback<List<Promotion>> cb) {
        db.collection(COL)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Promotion> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Promotion p = doc.toObject(Promotion.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            if (isNotExpired(p.getEndDate())) {
                                list.add(p);
                            }
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void add(Promotion promotion, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        promotion.setId(id);
        promotion.setCreatedAt(Timestamp.now().toString());
        promotion.setUpdatedAt(Timestamp.now().toString());
        promotion.setUsageCount(0);
        
        android.util.Log.d("PromotionRepo", "add: creating promo id=" + id + ", name=" + promotion.getName() + ", active=" + promotion.isActive());
        
        db.collection(COL).document(id).set(promotion)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("PromotionRepo", "add: promo created successfully, id=" + id);
                    cb.onSuccess(id);
                    // Đồng bộ giá khuyến mãi xuống pets/foods trong Firestore
                    syncPromotionToProducts(promotion, null);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PromotionRepo", "add FAILED: " + e.getMessage());
                    cb.onFailure(e.getMessage());
                });
    }

    private String formatPrice(double price) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format((long) price) + "đ";
    }

    /**
     * Đồng bộ thông tin khuyến mãi xuống Firestore cho tất cả sản phẩm áp dụng.
     * Gọi khi thêm/sửa promotion để cập nhật discountedPrice vào pets/foods.
     */
    public void syncPromotionToProducts(Promotion promotion, Runnable onComplete) {
        if (promotion == null || !promotion.isActive()) {
            android.util.Log.w("PromotionRepo", "syncPromotionToProducts: skipped (promo null or inactive)");
            if (onComplete != null) onComplete.run();
            return;
        }

        android.util.Log.d("PromotionRepo", "syncPromotionToProducts: starting for promo=" + promotion.getName());

        final com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        final String promoId = promotion.getId();
        final int[] updatedPets = {0};
        final int[] updatedFoods = {0};

        // Đếm số task cần hoàn thành
        final int[] pendingTasks = {2}; // pets + foods
        final Runnable checkComplete = () -> {
            synchronized (pendingTasks) {
                pendingTasks[0]--;
                if (pendingTasks[0] <= 0) {
                    android.util.Log.d("PromotionRepo", "syncPromotionToProducts: completed. Updated " + updatedPets[0] + " pets, " + updatedFoods[0] + " foods");
                    if (onComplete != null) onComplete.run();
                }
            }
        };

        // Lấy danh sách pets và cập nhật
        db.collection("pets").get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("PromotionRepo", "syncPromotionToProducts: checking " + snap.size() + " pets");
                    for (var doc : snap.getDocuments()) {
                        Pet pet = doc.toObject(Pet.class);
                        if (pet == null) continue;

                        // Kiểm tra promotion có áp dụng cho pet này không
                        if (promotion.appliesTo(pet)) {
                            double discountedPrice = promotion.applyDiscount(pet.getPrice());
                            db.collection("pets").document(doc.getId())
                                    .update(
                                            "promotionId", promoId,
                                            "discountedPrice", discountedPrice,
                                            "updatedAt", com.google.firebase.Timestamp.now().toString()
                                    );
                            updatedPets[0]++;
                            android.util.Log.d("PromotionRepo", "syncPromotionToProducts: updated pet " + pet.getName() + " price to " + discountedPrice);
                        }
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PromotionRepo", "syncPromotionToProducts pets FAILED: " + e.getMessage());
                    checkComplete.run();
                });

        // Lấy danh sách foods và cập nhật
        db.collection("foods").get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("PromotionRepo", "syncPromotionToProducts: checking " + snap.size() + " foods");
                    for (var doc : snap.getDocuments()) {
                        Food food = doc.toObject(Food.class);
                        if (food == null) continue;

                        // Kiểm tra promotion có áp dụng cho food này không
                        if (promotion.appliesTo(food)) {
                            double discountedPrice = promotion.applyDiscount(food.getPrice());
                            db.collection("foods").document(doc.getId())
                                    .update(
                                            "promotionId", promoId,
                                            "discountedPrice", discountedPrice,
                                            "updatedAt", com.google.firebase.Timestamp.now().toString()
                                    );
                            updatedFoods[0]++;
                            android.util.Log.d("PromotionRepo", "syncPromotionToProducts: updated food " + food.getName() + " price to " + discountedPrice);
                        }
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PromotionRepo", "syncPromotionToProducts foods FAILED: " + e.getMessage());
                    checkComplete.run();
                });
    }

    /**
     * Xóa thông tin khuyến mãi khỏi tất cả sản phẩm.
     * Gọi khi promotion bị xóa hoặc hết hạn.
     */
    public void clearPromotionFromProducts(String promoId, Runnable onComplete) {
        if (promoId == null || promoId.isEmpty()) {
            android.util.Log.w("PromotionRepo", "clearPromotionFromProducts: promoId is null/empty, skip");
            if (onComplete != null) onComplete.run();
            return;
        }

        android.util.Log.d("PromotionRepo", "clearPromotionFromProducts: starting for promoId=" + promoId);

        final com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        final int[] clearedPets = {0};
        final int[] clearedFoods = {0};

        // Đếm số task cần hoàn thành
        final int[] pendingTasks = {2};
        final Runnable checkComplete = () -> {
            synchronized (pendingTasks) {
                pendingTasks[0]--;
                if (pendingTasks[0] <= 0) {
                    android.util.Log.d("PromotionRepo", "clearPromotionFromProducts: completed. Cleared " + clearedPets[0] + " pets, " + clearedFoods[0] + " foods");
                    if (onComplete != null) onComplete.run();
                }
            }
        };

        // Xóa khỏi pets
        db.collection("pets").whereEqualTo("promotionId", promoId).get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("PromotionRepo", "clearPromotionFromProducts: found " + snap.size() + " pets with this promo");
                    for (var doc : snap.getDocuments()) {
                        doc.getReference().update(
                                "promotionId", "",
                                "discountedPrice", 0,
                                "updatedAt", com.google.firebase.Timestamp.now().toString()
                        );
                        clearedPets[0]++;
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PromotionRepo", "clearPromotionFromProducts pets FAILED: " + e.getMessage());
                    checkComplete.run();
                });

        // Xóa khỏi foods
        db.collection("foods").whereEqualTo("promotionId", promoId).get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("PromotionRepo", "clearPromotionFromProducts: found " + snap.size() + " foods with this promo");
                    for (var doc : snap.getDocuments()) {
                        doc.getReference().update(
                                "promotionId", "",
                                "discountedPrice", 0,
                                "updatedAt", com.google.firebase.Timestamp.now().toString()
                        );
                        clearedFoods[0]++;
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PromotionRepo", "clearPromotionFromProducts foods FAILED: " + e.getMessage());
                    checkComplete.run();
                });
    }

    public void update(Promotion promotion, Callback<Void> cb) {
        promotion.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL).document(promotion.getId()).set(promotion)
                .addOnSuccessListener(v -> {
                    cb.onSuccess(null);
                    // Đồng bộ giá khuyến mãi xuống pets/foods
                    if (promotion.isActive()) {
                        syncPromotionToProducts(promotion, null);
                    } else {
                        // Nếu promotion bị tắt, xóa khỏi tất cả sản phẩm
                        clearPromotionFromProducts(promotion.getId(), null);
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void delete(String id, Callback<Void> cb) {
        // Trước khi xóa, lấy promotion để biết id
        getById(id, new Callback<Promotion>() {
            @Override
            public void onSuccess(Promotion data) {
                // Xóa khuyến mãi khỏi tất cả sản phẩm trước
                clearPromotionFromProducts(id, () -> {
                    // Sau đó xóa promotion
                    db.collection(COL).document(id).delete()
                            .addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                });
            }

            @Override
            public void onFailure(String error) {
                // Không tìm thấy promotion, vẫn xóa
                db.collection(COL).document(id).delete()
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
            }
        });
    }

    public void toggleActive(String id, boolean isActive, Callback<Void> cb) {
        // Lấy promotion trước để biết thông tin
        getById(id, new Callback<Promotion>() {
            @Override
            public void onSuccess(Promotion data) {
                db.collection(COL).document(id).update("active", isActive)
                        .addOnSuccessListener(v -> {
                            cb.onSuccess(null);
                            android.util.Log.d("PromotionRepo", "toggleActive: id=" + id + ", isActive=" + isActive + ", promo=" + (data != null ? data.getName() : "null"));
                            
                            if (isActive && data != null) {
                                // Toggle ON: đồng bộ giá
                                data.setActive(true);
                                syncPromotionToProducts(data, null);
                                android.util.Log.d("PromotionRepo", "Toggle ON: synced prices for promo=" + data.getName());
                            } else {
                                // Toggle OFF: xóa khuyến mãi khỏi sản phẩm
                                clearPromotionFromProducts(id, null);
                                android.util.Log.d("PromotionRepo", "Toggle OFF: cleared promotion from products");
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("PromotionRepo", "toggleActive FAILED: " + e.getMessage());
                            cb.onFailure(e.getMessage());
                        });
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("PromotionRepo", "toggleActive getById FAILED: " + error);
                cb.onFailure(error);
            }
        });
    }

    public void incrementUsageCount(String id, Callback<Void> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Promotion p = doc.toObject(Promotion.class);
                        p.setUsageCount(p.getUsageCount() + 1);
                        update(p, cb);
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private boolean isNotExpired(String endDate) {
        if (endDate == null || endDate.isEmpty()) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date end = sdf.parse(endDate);
            return end != null && !end.before(stripTime(new Date()));
        } catch (Exception e) {
            return true;
        }
    }

    private Date stripTime(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.parse(sdf.format(date));
        } catch (Exception e) {
            return date;
        }
    }
}
