package com.example.petshop.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.PetMedia;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.utils.StorageHelper;

import java.util.ArrayList;
import java.util.List;

public class PetManageViewModel extends ViewModel {

    private final PetRepository repo = new PetRepository();

    private final MutableLiveData<List<Pet>> pets = new MutableLiveData<>();
    private final MutableLiveData<Pet> currentPet = new MutableLiveData<>();
    private final MutableLiveData<List<PetMedia>> mediaList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSaved = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> success = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>(0);

    private boolean isSaving = false;

    public LiveData<List<Pet>> getPets() {
        return pets;
    }

    public LiveData<Pet> getCurrentPet() {
        return currentPet;
    }

    public LiveData<List<PetMedia>> getMediaList() {
        return mediaList;
    }

    public LiveData<Boolean> getLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsSaved() {
        return isSaved;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getSuccess() {
        return success;
    }

    public LiveData<Integer> getUploadProgress() {
        return uploadProgress;
    }

    public void loadAll() {
        isLoading.setValue(true);

        repo.getAll(new PetRepository.Callback<List<Pet>>() {
            @Override
            public void onSuccess(List<Pet> data) {
                isLoading.postValue(false);
                pets.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onFailure(String err) {
                isLoading.postValue(false);
                error.postValue(err);
            }
        });
    }

    public void loadById(String id) {
        isLoading.setValue(true);

        repo.getById(id, new PetRepository.Callback<Pet>() {
            @Override
            public void onSuccess(Pet pet) {
                isLoading.postValue(false);
                currentPet.postValue(pet);
                loadMedia(id);
            }

            @Override
            public void onFailure(String err) {
                isLoading.postValue(false);
                error.postValue(err);
            }
        });
    }

    public void loadMedia(String petId) {
        repo.getMedia(petId, new PetRepository.Callback<List<PetMedia>>() {
            @Override
            public void onSuccess(List<PetMedia> data) {
                mediaList.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onFailure(String err) {
                mediaList.postValue(new ArrayList<>());
            }
        });
    }

    public void savePet(Pet pet, List<Uri> newMediaUris, List<String> mediaTypes) {
        if (isSaving) {
            return;
        }

        if (pet == null) {
            error.setValue("Dữ liệu thú cưng không hợp lệ");
            return;
        }

        isSaving = true;
        isLoading.setValue(true);
        isSaved.setValue(false);
        uploadProgress.setValue(0);

        List<Uri> safeUris = newMediaUris != null ? new ArrayList<>(newMediaUris) : new ArrayList<>();
        List<String> safeTypes = mediaTypes != null ? new ArrayList<>(mediaTypes) : new ArrayList<>();

        boolean isNew = pet.getId() == null || pet.getId().isEmpty();

        if (isNew) {
            repo.add(pet, new PetRepository.Callback<String>() {
                @Override
                public void onSuccess(String id) {
                    pet.setId(id);
                    uploadMedia(id, safeUris, safeTypes, 0, true);
                }

                @Override
                public void onFailure(String err) {
                    finishSavingWithError(err);
                }
            });
        } else {
            repo.update(pet, new PetRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void v) {
                    uploadMedia(pet.getId(), safeUris, safeTypes, 0, false);
                }

                @Override
                public void onFailure(String err) {
                    finishSavingWithError(err);
                }
            });
        }
    }

    private void uploadMedia(String petId,
                             List<Uri> uris,
                             List<String> types,
                             int index,
                             boolean isNewPet) {
        if (uris == null || uris.isEmpty() || index >= uris.size()) {
            finishSavingSuccessfully(isNewPet);
            return;
        }

        if (types == null || index >= types.size()) {
            finishSavingWithError("Danh sách loại media không hợp lệ");
            return;
        }

        uploadProgress.postValue((index * 100) / uris.size());

        Uri uri = uris.get(index);
        String type = types.get(index);

        boolean isVideo = PetMedia.TYPE_VIDEO.equals(type);

        StorageHelper.OnUploadCallback cb = new StorageHelper.OnUploadCallback() {
            @Override
            public void onSuccess(String url) {
                PetMedia media = new PetMedia(petId, url, type);
                media.setSortOrder(index);

                repo.addMedia(petId, media, new PetRepository.Callback<String>() {
                    @Override
                    public void onSuccess(String mediaId) {
                        if (index == 0 && !isVideo) {
                            repo.updateThumbnail(petId, url, new PetRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void v) {
                                    uploadProgress.postValue(((index + 1) * 100) / uris.size());
                                    uploadMedia(petId, uris, types, index + 1, isNewPet);
                                }

                                @Override
                                public void onFailure(String err) {
                                    uploadProgress.postValue(((index + 1) * 100) / uris.size());
                                    uploadMedia(petId, uris, types, index + 1, isNewPet);
                                }
                            });
                        } else {
                            uploadProgress.postValue(((index + 1) * 100) / uris.size());
                            uploadMedia(petId, uris, types, index + 1, isNewPet);
                        }
                    }

                    @Override
                    public void onFailure(String err) {
                        uploadProgress.postValue(((index + 1) * 100) / uris.size());
                        uploadMedia(petId, uris, types, index + 1, isNewPet);
                    }
                });
            }

            @Override
            public void onFailure(String err) {
                uploadProgress.postValue(((index + 1) * 100) / uris.size());
                uploadMedia(petId, uris, types, index + 1, isNewPet);
            }
        };

        if (isVideo) {
            StorageHelper.uploadVideo(uri, "pets/" + petId, cb);
        } else {
            StorageHelper.uploadImage(uri, "pets/" + petId, cb);
        }
    }

    private void finishSavingSuccessfully(boolean isNewPet) {
        isSaving = false;
        isLoading.postValue(false);
        uploadProgress.postValue(100);
        isSaved.postValue(true);

        if (isNewPet) {
            success.postValue("Lưu thú cưng thành công");
        } else {
            success.postValue("Cập nhật thú cưng thành công");
        }
    }

    private void finishSavingWithError(String err) {
        isSaving = false;
        isLoading.postValue(false);
        error.postValue(err != null ? err : "Có lỗi xảy ra khi lưu thú cưng");
    }

    public void deleteMediaItem(String petId, String mediaId, String mediaUrl) {
        StorageHelper.deleteFile(mediaUrl, () ->
                repo.deleteMedia(petId, mediaId, new PetRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        loadMedia(petId);
                    }

                    @Override
                    public void onFailure(String err) {
                        error.postValue(err);
                    }
                })
        );
    }

    public void deletePet(String petId) {
        repo.delete(petId, new PetRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void v) {
                success.postValue("Đã xoá thú cưng");
                loadAll();
            }

            @Override
            public void onFailure(String err) {
                error.postValue(err);
            }
        });
    }

    public void updateStatus(String id, String status) {
        repo.updateStatus(id, status, new PetRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void v) {
                loadAll();
            }

            @Override
            public void onFailure(String err) {
                error.postValue(err);
            }
        });
    }
}