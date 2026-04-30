package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.Voucher;
import com.example.petshop.repository.VoucherRepository;

import java.util.List;

public class VoucherManageViewModel extends ViewModel {

    private final VoucherRepository repo = new VoucherRepository();

    private final MutableLiveData<List<Voucher>> vouchers   = new MutableLiveData<>();
    private final MutableLiveData<Boolean>       isLoading  = new MutableLiveData<>(false);
    private final MutableLiveData<String>        error      = new MutableLiveData<>();
    private final MutableLiveData<String>        success    = new MutableLiveData<>();

    public LiveData<List<Voucher>> getVouchers() { return vouchers; }
    public LiveData<Boolean>       getLoading()  { return isLoading; }
    public LiveData<String>        getError()    { return error; }
    public LiveData<String>        getSuccess()  { return success; }

    public void loadAll() {
        isLoading.setValue(true);
        repo.getAll(new VoucherRepository.Callback<>() {
            public void onSuccess(List<Voucher> data) { isLoading.postValue(false); vouchers.postValue(data); }
            public void onFailure(String err)         { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void add(Voucher voucher) {
        isLoading.setValue(true);
        repo.add(voucher, new VoucherRepository.Callback<>() {
            public void onSuccess(String id) { isLoading.postValue(false); success.postValue("Thêm voucher thành công"); loadAll(); }
            public void onFailure(String err){ isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void update(Voucher voucher) {
        isLoading.setValue(true);
        repo.update(voucher, new VoucherRepository.Callback<>() {
            public void onSuccess(Void v)   { isLoading.postValue(false); success.postValue("Cập nhật thành công"); loadAll(); }
            public void onFailure(String err){ isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void delete(String id) {
        repo.delete(id, new VoucherRepository.Callback<>() {
            public void onSuccess(Void v)   { success.postValue("Đã xoá voucher"); loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }

    public void toggleActive(String id, boolean isActive) {
        repo.toggleActive(id, isActive, new VoucherRepository.Callback<>() {
            public void onSuccess(Void v)    { loadAll(); }
            public void onFailure(String err){ error.postValue(err); }
        });
    }
}
