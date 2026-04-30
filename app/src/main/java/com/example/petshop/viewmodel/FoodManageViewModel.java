package com.example.petshop.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.FoodMedia;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.utils.StorageHelper;

import java.util.ArrayList;
import java.util.List;

public class FoodManageViewModel extends ViewModel {

    private final FoodRepository repo = new FoodRepository();

    private final MutableLiveData<List<Food>>      foods         = new MutableLiveData<>();
    private final MutableLiveData<Food>            currentFood   = new MutableLiveData<>();
    private final MutableLiveData<List<FoodMedia>> mediaList     = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>         isLoading     = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>         isSaved       = new MutableLiveData<>(false);
    private final MutableLiveData<String>          error         = new MutableLiveData<>();
    private final MutableLiveData<String>          success       = new MutableLiveData<>();
    private final MutableLiveData<Integer>         uploadProgress = new MutableLiveData<>(0);

    public LiveData<List<Food>>      getFoods()          { return foods; }
    public LiveData<Food>            getCurrentFood()    { return currentFood; }
    public LiveData<List<FoodMedia>> getMediaList()      { return mediaList; }
    public LiveData<Boolean>         getLoading()        { return isLoading; }
    public LiveData<Boolean>         getIsSaved()        { return isSaved; }
    public LiveData<String>          getError()          { return error; }
    public LiveData<String>          getSuccess()        { return success; }
    public LiveData<Integer>         getUploadProgress() { return uploadProgress; }

    public void loadAll() {
        isLoading.setValue(true);
        repo.getAll(new FoodRepository.Callback<>() {
            public void onSuccess(List<Food> data) { isLoading.postValue(false); foods.postValue(data); }
            public void onFailure(String err)      { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void loadById(String id) {
        isLoading.setValue(true);
        repo.getById(id, new FoodRepository.Callback<>() {
            public void onSuccess(Food food) {
                isLoading.postValue(false);
                currentFood.postValue(food);
                loadMedia(id);
            }
            public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void loadMedia(String foodId) {
        repo.getMedia(foodId, new FoodRepository.Callback<>() {
            public void onSuccess(List<FoodMedia> data) { mediaList.postValue(data); }
            public void onFailure(String err)           { /* ignore */ }
        });
    }

    public void saveFood(Food food, List<Uri> newMediaUris, List<String> mediaTypes) {
        isLoading.setValue(true);
        boolean isNew = food.getId() == null || food.getId().isEmpty();

        FoodRepository.Callback<String> addCb = new FoodRepository.Callback<>() {
            public void onSuccess(String id) { uploadMedia(id, newMediaUris, mediaTypes, 0); }
            public void onFailure(String err){ isLoading.postValue(false); error.postValue(err); }
        };

        FoodRepository.Callback<Void> updateCb = new FoodRepository.Callback<>() {
            public void onSuccess(Void v) {
                if (!newMediaUris.isEmpty()) uploadMedia(food.getId(), newMediaUris, mediaTypes, 0);
                else { isLoading.postValue(false); isSaved.postValue(true); success.postValue("Cập nhật thức ăn thành công"); }
            }
            public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
        };

        if (isNew) repo.add(food, addCb);
        else       repo.update(food, updateCb);
    }

    private void uploadMedia(String foodId, List<Uri> uris, List<String> types, int index) {
        if (index >= uris.size()) {
            isLoading.postValue(false);
            isSaved.postValue(true);
            success.postValue("Lưu thức ăn thành công");
            return;
        }
        uploadProgress.postValue((index * 100) / uris.size());
        Uri uri = uris.get(index);
        String type = types.get(index);
        boolean isVideo = FoodMedia.TYPE_VIDEO.equals(type);

        StorageHelper.OnUploadCallback cb = new StorageHelper.OnUploadCallback() {
            public void onSuccess(String url) {
                FoodMedia media = new FoodMedia(foodId, url, type);
                media.setSortOrder(index);
                repo.addMedia(foodId, media, new FoodRepository.Callback<>() {
                    public void onSuccess(String id) { uploadMedia(foodId, uris, types, index + 1); }
                    public void onFailure(String err){ uploadMedia(foodId, uris, types, index + 1); }
                });
            }
            public void onFailure(String err) { uploadMedia(foodId, uris, types, index + 1); }
        };

        if (isVideo) StorageHelper.uploadVideo(uri, "foods/" + foodId, cb);
        else         StorageHelper.uploadImage(uri, "foods/" + foodId, cb);
    }

    public void deleteMediaItem(String foodId, String mediaId, String mediaUrl) {
        StorageHelper.deleteFile(mediaUrl, () ->
            repo.deleteMedia(foodId, mediaId, new FoodRepository.Callback<>() {
                public void onSuccess(Void v)    { loadMedia(foodId); }
                public void onFailure(String err){ error.postValue(err); }
            })
        );
    }

    public void deleteFood(String foodId) {
        repo.delete(foodId, new FoodRepository.Callback<>() {
            public void onSuccess(Void v)    { success.postValue("Đã xoá thức ăn"); loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }

    public void updateStock(String foodId, int stock) {
        repo.updateStock(foodId, stock, new FoodRepository.Callback<>() {
            public void onSuccess(Void v)    { success.postValue("Cập nhật kho thành công"); loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }
}
