package com.example.petshop.repository;



import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.PetMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PetRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL       = "pets";
    private static final String COL_MEDIA = "pet_media";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAll(Callback<List<Pet>> cb) {
        db.collection(COL)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Pet> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Pet pet = doc.toObject(Pet.class);
                        if (pet != null) {
                            pet.setId(doc.getId());
                            list.add(pet);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getByCategory(String categoryId, Callback<List<Pet>> cb) {
        db.collection(COL).whereEqualTo("categoryId", categoryId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<Pet> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Pet pet = doc.toObject(Pet.class);
                        if (pet != null) {
                            pet.setId(doc.getId());
                            list.add(pet);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getById(String id, Callback<Pet> cb) {
        db.collection(COL).document(id).get()
                .addOnSuccessListener(doc -> {
                    Pet pet = doc.toObject(Pet.class);
                    if (pet != null) pet.setId(doc.getId());
                    cb.onSuccess(pet);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public void updateThumbnail(String petId, String url, PetRepository.Callback<Void> cb) {
        db.collection(COL).document(petId)
                .update("thumbnailUrl", url, "updatedAt", now())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }


    public void add(Pet pet, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        pet.setId(id);
        pet.setCreatedAt(now());
        pet.setUpdatedAt(now());
        if (pet.getStatus() == null) pet.setStatus(Pet.STATUS_AVAILABLE);
        db.collection(COL).document(id).set(pet)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void update(Pet pet, Callback<Void> cb) {
        pet.setUpdatedAt(now());
        // Sử dụng update thay vì set để không ghi đè mất các trường như thumbnailUrl nếu chúng không có trong object pet
        db.collection(COL).document(pet.getId())
                .set(pet, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void delete(String id, Callback<Void> cb) {
        db.collection(COL).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateStatus(String id, String status, Callback<Void> cb) {
        db.collection(COL).document(id).update(
                "status", status,
                "updatedAt", now())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void addMedia(String petId, PetMedia media, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        media.setId(id);
        media.setPetId(petId);
        db.collection(COL).document(petId)
                .collection(COL_MEDIA).document(id).set(media)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void getMedia(String petId, Callback<List<PetMedia>> cb) {
        db.collection(COL).document(petId)
                .collection(COL_MEDIA).orderBy("sortOrder").get()
                .addOnSuccessListener(snap -> {
                    List<PetMedia> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        PetMedia media = doc.toObject(PetMedia.class);
                        if (media != null) {
                            media.setId(doc.getId());
                            list.add(media);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void deleteMedia(String petId, String mediaId, Callback<Void> cb) {
        db.collection(COL).document(petId)
                .collection(COL_MEDIA).document(mediaId).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /**
     * Cập nhật thông tin khuyến mãi cho pet (discountedPrice, promotionId, promotion).
     * Gọi khi có promotion mới được thêm để đồng bộ giá xuống Firestore.
     */
    public void updatePromotionInfo(String petId, String promoId, double discountedPrice,
                                   com.example.petshop.model.entity.Promotion promo, Callback<Void> cb) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("promotionId", promoId != null ? promoId : "");
        updates.put("discountedPrice", discountedPrice);
        updates.put("updatedAt", now());

        db.collection(COL).document(petId).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /**
     * Xóa thông tin khuyến mãi của pet (khi promotion hết hạn hoặc bị xóa).
     */
    public void clearPromotionInfo(String petId, Callback<Void> cb) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("promotionId", "");
        updates.put("discountedPrice", 0);
        updates.put("updatedAt", now());

        db.collection(COL).document(petId).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }
}
