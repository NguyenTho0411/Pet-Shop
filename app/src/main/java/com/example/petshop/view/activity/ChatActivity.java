package com.example.petshop.view.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.ChatMessage;
import com.example.petshop.view.adapter.ChatAdapter;
import com.example.petshop.viewmodel.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel vm;
    private ChatAdapter   adapter;
    private RecyclerView  rv;
    private EditText      etMessage;
    private TextView      tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);
        
        initViews();
        observeViewModel();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) vm.initContext(uid);
    }

    private void initViews() {
        rv        = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        tvStatus  = findViewById(R.id.tvBotStatus);

        adapter = new ChatAdapter(new ArrayList<>());
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
    }

    private void observeViewModel() {
        vm.getMessages().observe(this, list -> {
            // Since we use the same list object in VM, we need to refresh carefully or just update whole
            adapter.notifyDataSetChanged();
            if (adapter.getItemCount() > 0) {
                rv.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        vm.getIsTyping().observe(this, isTyping -> {
            tvStatus.setText(isTyping ? "AI đang trả lời..." : "Đang hoạt động");
            tvStatus.setTextColor(isTyping ? getColor(R.color.text_secondary) : getColor(R.color.status_success));
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        
        vm.sendMessage(text);
        etMessage.setText("");
    }
}
