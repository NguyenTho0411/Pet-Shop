package com.example.petshop.view.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.User;
import com.example.petshop.view.adapter.UserAdminAdapter;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.example.petshop.viewmodel.UserManageViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ManageUsersActivity extends AppCompatActivity {

    private UserManageViewModel vm;
    private UserAdminAdapter    adapter;
    private List<User>          allUsers = new ArrayList<>();
    private ProgressBar         progressBar;
    private TextView            tvUserCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        vm = new ViewModelProvider(this).get(UserManageViewModel.class);

        initViews();
        observeViewModel();
        vm.loadAllUsers();
    }

    private void initViews() {
        progressBar  = findViewById(R.id.progressBar);
        tvUserCount  = findViewById(R.id.tvUserCount);

        RecyclerView rv = findViewById(R.id.rvUsers);
        adapter = new UserAdminAdapter(new ArrayList<>(), new UserAdminAdapter.OnActionListener() {
            public void onBan(User u)                        { confirmAction("Khoá tài khoản \"" + u.getFullName() + "\"?", () -> vm.banUser(u.getId())); }
            public void onUnban(User u)                      { vm.unbanUser(u.getId()); }
            public void onChangeRole(User u, String role)    { confirmAction("Đổi role thành " + role + "?", () -> vm.changeRole(u.getId(), role)); }
            public void onDelete(User u)                     { confirmAction("Xoá người dùng \"" + u.getFullName() + "\"? Không thể hoàn tác!", () -> vm.deleteUser(u.getId())); }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Filter chips
        findViewById(R.id.chipAll).setOnClickListener(v      -> filterUsers("ALL"));
        findViewById(R.id.chipCustomer).setOnClickListener(v -> filterUsers(User.ROLE_CUSTOMER));
        findViewById(R.id.chipAdmin).setOnClickListener(v    -> filterUsers(User.ROLE_ADMIN));

        // Search
        ((EditText) findViewById(R.id.etSearch)).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count)    {}
            public void afterTextChanged(Editable s) { searchUsers(s.toString()); }
        });
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getUsers().observe(this, users -> {
            allUsers = users != null ? users : new ArrayList<>();
            adapter.updateList(new ArrayList<>(allUsers));
            tvUserCount.setText(allUsers.size() + " users");
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void filterUsers(String filter) {
        if ("ALL".equals(filter)) {
            adapter.updateList(new ArrayList<>(allUsers));
        } else {
            List<User> filtered = allUsers.stream()
                    .filter(u -> filter.equals(u.getRole()))
                    .collect(Collectors.toList());
            adapter.updateList(filtered);
        }
    }

    private void searchUsers(String query) {
        if (query.isEmpty()) { adapter.updateList(new ArrayList<>(allUsers)); return; }
        String q = query.toLowerCase();
        List<User> filtered = allUsers.stream()
                .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                          || (u.getEmail()    != null && u.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        adapter.updateList(filtered);
    }

    private void confirmAction(String message, Runnable action) {
        DialogUtils.showConfirmDialog(this, message,
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    action.run();
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
