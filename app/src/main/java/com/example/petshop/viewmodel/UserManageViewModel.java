package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.model.entity.User;
import com.example.petshop.repository.UserRepository;

import java.util.List;

public class UserManageViewModel extends ViewModel {

    private final UserRepository repo = new UserRepository();

    private final MutableLiveData<List<User>> users      = new MutableLiveData<>();
    private final MutableLiveData<Boolean>    isLoading  = new MutableLiveData<>(false);
    private final MutableLiveData<String>     error      = new MutableLiveData<>();
    private final MutableLiveData<String>     success    = new MutableLiveData<>();

    public LiveData<List<User>> getUsers()    { return users; }
    public LiveData<Boolean>    getLoading()  { return isLoading; }
    public LiveData<String>     getError()    { return error; }
    public LiveData<String>     getSuccess()  { return success; }

    public void loadAllUsers() {
        isLoading.setValue(true);
        repo.getAllUsers(new UserRepository.Callback<>() {
            public void onSuccess(List<User> data) { isLoading.postValue(false); users.postValue(data); }
            public void onFailure(String err)      { isLoading.postValue(false); error.postValue(err); }
        });
    }

    public void banUser(String uid) {
        repo.updateUserStatus(uid, User.STATUS_BANNED, new UserRepository.Callback<>() {
            public void onSuccess(Void v) { success.postValue("Đã khoá tài khoản"); loadAllUsers(); }
            public void onFailure(String err) { error.postValue(err); }
        });
    }

    public void unbanUser(String uid) {
        repo.updateUserStatus(uid, User.STATUS_ACTIVE, new UserRepository.Callback<>() {
            public void onSuccess(Void v) { success.postValue("Đã mở khoá tài khoản"); loadAllUsers(); }
            public void onFailure(String err) { error.postValue(err); }
        });
    }

    public void changeRole(String uid, String role) {
        repo.updateUserRole(uid, role, new UserRepository.Callback<>() {
            public void onSuccess(Void v) { success.postValue("Đã cập nhật role"); loadAllUsers(); }
            public void onFailure(String err) { error.postValue(err); }
        });
    }

    public void deleteUser(String uid) {
        repo.deleteUser(uid, new UserRepository.Callback<>() {
            public void onSuccess(Void v) { success.postValue("Đã xoá người dùng"); loadAllUsers(); }
            public void onFailure(String err) { error.postValue(err); }
        });
    }
}
