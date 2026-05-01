package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Category;
import com.example.petshop.repository.CategoryRepository;

import java.util.List;

public class CategoryManageViewModel extends ViewModel {

    private final CategoryRepository repo = new CategoryRepository();

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean>        isLoading  = new MutableLiveData<>(false);
    private final MutableLiveData<String>         error      = new MutableLiveData<>();
    private final MutableLiveData<String>         success    = new MutableLiveData<>();

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean>        getLoading()    { return isLoading; }
    public LiveData<String>         getError()      { return error; }
    public LiveData<String>         getSuccess()    { return success; }

    public void loadAll() {
        isLoading.setValue(true);
        repo.getAll(new CategoryRepository.Callback<>() {
            public void onSuccess(List<Category> data) { isLoading.postValue(false); categories.postValue(data); }
            public void onFailure(String err)          { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void add(Category category, android.net.Uri imageUri) {
        isLoading.setValue(true);
        repo.add(category, new CategoryRepository.Callback<>() {
            public void onSuccess(String id) {
                if (imageUri != null) uploadImage(id, imageUri);
                else { isLoading.postValue(false); success.postValue("Thêm danh mục thành công"); loadAll(); }
            }
            public void onFailure(String err){ isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void update(Category category, android.net.Uri imageUri) {
        isLoading.setValue(true);
        repo.update(category, new CategoryRepository.Callback<>() {
            public void onSuccess(Void v)   {
                if (imageUri != null) uploadImage(category.getId(), imageUri);
                else { isLoading.postValue(false); success.postValue("Cập nhật thành công"); loadAll(); }
            }
            public void onFailure(String err){ isLoading.postValue(false); error.postValue(err); }
        });
    }

    private void uploadImage(String catId, android.net.Uri uri) {
        com.example.petshop.utils.StorageHelper.uploadImage(uri, "categories/" + catId, new com.example.petshop.utils.StorageHelper.OnUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Chỉ cập nhật trường imageUrl, không gửi cả object để tránh mất data khác
                repo.updateImageUrl(catId, downloadUrl, new CategoryRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void data) { isLoading.postValue(false); success.postValue("Lưu thành công"); loadAll(); }
                    @Override
                    public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
                });
            }
            @Override
            public void onFailure(String err) { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void delete(String id) {
        repo.delete(id, new CategoryRepository.Callback<>() {
            public void onSuccess(Void v)   { success.postValue("Đã xoá danh mục"); loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }

    public void toggleActive(String id, boolean active) {
        repo.toggleActive(id, active, new CategoryRepository.Callback<>() {
            public void onSuccess(Void v)    { loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }
}
