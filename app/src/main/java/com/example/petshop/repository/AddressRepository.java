package com.example.petshop.repository;

import com.example.petshop.model.entity.Address;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddressRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private static final String COL_USERS = "users";
    private static final String COL_ADDR  = "addresses";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getAddresses(String userId, Callback<List<Address>> cb) {
        db.collection(COL_USERS).document(userId)
                .collection(COL_ADDR).get()
                .addOnSuccessListener(snap -> {
                    List<Address> list = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Address addr = doc.toObject(Address.class);
                        if (addr != null) {
                            addr.setId(doc.getId());
                            list.add(addr);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void addAddress(String userId, Address address, Callback<String> cb) {
        String id = UUID.randomUUID().toString();
        address.setId(id);
        address.setUserId(userId);
        address.setCreatedAt(Timestamp.now().toString());
        db.collection(COL_USERS).document(userId)
                .collection(COL_ADDR).document(id).set(address)
                .addOnSuccessListener(v -> cb.onSuccess(id))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void updateAddress(String userId, Address address, Callback<Void> cb) {
        db.collection(COL_USERS).document(userId)
                .collection(COL_ADDR).document(address.getId()).set(address)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void deleteAddress(String userId, String addressId, Callback<Void> cb) {
        db.collection(COL_USERS).document(userId)
                .collection(COL_ADDR).document(addressId).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public void setDefault(String userId, String addressId, Callback<Void> cb) {
        // First reset all to non-default, then set selected to default
        getAddresses(userId, new Callback<List<Address>>() {
            public void onSuccess(List<Address> list) {
                var batch = db.batch();
                for (Address a : list) {
                    var ref = db.collection(COL_USERS).document(userId)
                            .collection(COL_ADDR).document(a.getId());
                    batch.update(ref, "isDefault", a.getId().equals(addressId));
                }
                batch.commit()
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
            }
            public void onFailure(String err) { cb.onFailure(err); }
        });
    }
}
