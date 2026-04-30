package com.example.petshop.viewmodel;



import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.PetMedia;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.utils.StorageHelper;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class PetManageViewModel extends ViewModel {

    private final PetRepository repo = new PetRepository();

    private final MutableLiveData<List<Pet>>      pets         = new MutableLiveData<>();
    private final MutableLiveData<Pet>            currentPet   = new MutableLiveData<>();
    private final MutableLiveData<List<PetMedia>> mediaList    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>        isLoading    = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>        isSaved      = new MutableLiveData<>(false);
    private final MutableLiveData<String>         error        = new MutableLiveData<>();
    private final MutableLiveData<String>         success      = new MutableLiveData<>();
    private final MutableLiveData<Integer>        uploadProgress = new MutableLiveData<>(0);

    public LiveData<List<Pet>>      getPets()          { return pets; }
    public LiveData<Pet>            getCurrentPet()    { return currentPet; }
    public LiveData<List<PetMedia>> getMediaList()     { return mediaList; }
    public LiveData<Boolean>        getLoading()       { return isLoading; }
    public LiveData<Boolean>        getIsSaved()       { return isSaved; }
    public LiveData<String>         getError()         { return error; }
    public LiveData<String>         getSuccess()       { return success; }
    public LiveData<Integer>        getUploadProgress(){ return uploadProgress; }

    public void loadAll() {
        isLoading.setValue(true);
        repo.getAll(new PetRepository.Callback<>() {
            public void onSuccess(List<Pet> data) { isLoading.postValue(false); pets.postValue(data); }
            public void onFailure(String err)     { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void loadById(String id) {
        isLoading.setValue(true);
        repo.getById(id, new PetRepository.Callback<>() {
            public void onSuccess(Pet pet) {
                isLoading.postValue(false);
                currentPet.postValue(pet);
                loadMedia(id);
            }
            public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void loadMedia(String petId) {
        repo.getMedia(petId, new PetRepository.Callback<>() {
            public void onSuccess(List<PetMedia> data) { mediaList.postValue(data); }
            public void onFailure(String err)          { /* ignore */ }
        });
    }

    public void savePet(Pet pet, List<Uri> newMediaUris, List<String> mediaTypes) {
        isLoading.setValue(true);
        boolean isNew = pet.getId() == null || pet.getId().isEmpty();

        PetRepository.Callback<String> addCb = new PetRepository.Callback<>() {
            public void onSuccess(String id) {
                uploadMedia(id, newMediaUris, mediaTypes, 0);
            }
            public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
        };

        PetRepository.Callback<Void> updateCb = new PetRepository.Callback<>() {
            public void onSuccess(Void v) {
                if (!newMediaUris.isEmpty()) uploadMedia(pet.getId(), newMediaUris, mediaTypes, 0);
                else { isLoading.postValue(false); isSaved.postValue(true); success.postValue("Cập nhật thú cưng thành công"); }
            }
            public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
        };

        if (isNew) repo.add(pet, addCb);
        else       repo.update(pet, updateCb);
    }

    private void uploadMedia(String petId, List<Uri> uris, List<String> types, int index) {
        if (index >= uris.size()) {
            isLoading.postValue(false);
            isSaved.postValue(true);
            success.postValue("Lưu thú cưng thành công");
            return;
        }
        uploadProgress.postValue((index * 100) / uris.size());
        Uri uri = uris.get(index);
        String type = types.get(index);
        boolean isVideo = PetMedia.TYPE_VIDEO.equals(type);

        StorageHelper.OnUploadCallback cb = new StorageHelper.OnUploadCallback() {
            public void onSuccess(String url) {
                PetMedia media = new PetMedia(petId, url, type);
                media.setSortOrder(index);
                repo.addMedia(petId, media, new PetRepository.Callback<>() {
                    public void onSuccess(String id) {
                        if (index == 0 && !isVideo) {
                            repo.updateThumbnail(petId, url, new PetRepository.Callback<Void>() {
                                public void onSuccess(Void v) { uploadMedia(petId, uris, types, index + 1); }
                                public void onFailure(String err) { uploadMedia(petId, uris, types, index + 1); }
                            });
                        } else {
                            uploadMedia(petId, uris, types, index + 1);
                        }
                    }
                    public void onFailure(String err) { uploadMedia(petId, uris, types, index + 1); }
                });
            }
            public void onFailure(String err) { uploadMedia(petId, uris, types, index + 1); }
        };

        if (isVideo) StorageHelper.uploadVideo(uri, "pets/" + petId, cb);
        else         StorageHelper.uploadImage(uri, "pets/" + petId, cb);
    }

    public void deleteMediaItem(String petId, String mediaId, String mediaUrl) {
        StorageHelper.deleteFile(mediaUrl, () ->
            repo.deleteMedia(petId, mediaId, new PetRepository.Callback<>() {
                public void onSuccess(Void v)    { loadMedia(petId); }
                public void onFailure(String err){ error.postValue(err); }
            })
        );
    }

    public void deletePet(String petId) {
        repo.delete(petId, new PetRepository.Callback<>() {
            public void onSuccess(Void v)    { success.postValue("Đã xoá thú cưng"); loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }

    public void updateStatus(String id, String status) {
        repo.updateStatus(id, status, new PetRepository.Callback<>() {
            public void onSuccess(Void v)    { loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }
}
