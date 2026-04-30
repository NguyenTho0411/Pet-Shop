package com.example.petshop.utils;

import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class StorageHelper {

    public interface OnUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }

    public interface OnDeleteCallback {
        void onComplete();
    }

    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    public static void uploadImage(Uri fileUri, String folder, OnUploadCallback callback) {
        String fileName  = folder + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void uploadVideo(Uri fileUri, String folder, OnUploadCallback callback) {
        String fileName  = folder + "/" + UUID.randomUUID().toString() + ".mp4";
        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void deleteFile(String downloadUrl, OnDeleteCallback callback) {
        if (downloadUrl == null || downloadUrl.isEmpty()) { callback.onComplete(); return; }
        try {
            storage.getReferenceFromUrl(downloadUrl).delete()
                    .addOnCompleteListener(t -> callback.onComplete());
        } catch (Exception e) {
            callback.onComplete();
        }
    }
}
