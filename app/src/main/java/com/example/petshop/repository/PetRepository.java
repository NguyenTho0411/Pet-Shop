package com.example.petshop.repository;



import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.PetMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
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
    public void updateThumbnail(String petId, String url, PetRepository.Callback<Void> cb) {
        db.collection(COL).document(petId)
                .update("thumbnailUrl", url, "updatedAt", Timestamp.now().toString())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }


    public void add(Pet pet, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        pet.setId(id);
        pet.setCreatedAt(Timestamp.now().toString());
        pet.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL).document(id).set(pet)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void update(Pet pet, Callback<Void> cb) {
        pet.setUpdatedAt(Timestamp.now().toString());
        db.collection(COL).document(pet.getId()).set(pet)
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
                "updatedAt", Timestamp.now().toString())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void addMedia(String petId, PetMedia media, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
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
                    for (var doc : snap.getDocuments())
                        list.add(doc.toObject(PetMedia.class));
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
}
