package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.ChatMessage;
import com.example.petshop.view.adapter.ChatAdapter;
import com.example.petshop.view.adapter.ChatSessionAdapter;
import com.example.petshop.viewmodel.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel vm;
    private ChatAdapter   adapter;
    private ChatSessionAdapter historyAdapter;
    private RecyclerView  rv, rvHistory;
    private EditText      etMessage;
    private TextView      tvStatus;
    private ImageButton   btnVoice, btnNewChat, btnAttach, btnClearImage, btnMenu;
    private ImageView     ivPreview;
    private View          flImagePreview;
    private DrawerLayout  drawerLayout;

    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        etMessage.setText(matches.get(0));
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    vm.setSelectedImage(uri.toString());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);
        
        initViews();
        observeViewModel();

        String uid = FirebaseAuth.getInstance().getUid();
        vm.initContext(uid != null ? uid : "");
    }

    private void initViews() {
        rv         = findViewById(R.id.rvChat);
        rvHistory  = findViewById(R.id.rvHistory);
        etMessage  = findViewById(R.id.etMessage);
        tvStatus   = findViewById(R.id.tvBotStatus);
        btnVoice   = findViewById(R.id.btnVoice);
        btnNewChat = findViewById(R.id.btnNewChat);
        btnAttach  = findViewById(R.id.btnAttach);
        btnMenu    = findViewById(R.id.btnMenu);
        ivPreview  = findViewById(R.id.ivPreview);
        flImagePreview = findViewById(R.id.flImagePreview);
        btnClearImage = findViewById(R.id.btnClearImage);
        drawerLayout  = findViewById(R.id.drawerLayout);

        adapter = new ChatAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        historyAdapter = new ChatSessionAdapter(session -> {
            vm.switchSession(session.getId());
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        btnNewChat.setOnClickListener(v -> vm.startNewChat());
        btnVoice.setOnClickListener(v -> startVoiceRecognition());
        btnAttach.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnClearImage.setOnClickListener(v -> vm.setSelectedImage(null));
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        findViewById(R.id.btnBackToHome).setOnClickListener(v -> finish());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói gì đó...");
        
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        vm.getMessages().observe(this, list -> {
            if (list != null) {
                adapter.submitList(new ArrayList<>(list), () -> {
                    if (adapter.getItemCount() > 0) {
                        rv.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                });
            }
        });

        vm.getSessions().observe(this, list -> {
            if (list != null) {
                historyAdapter.submitList(new ArrayList<>(list));
                findViewById(R.id.tvEmptyHistory).setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                findViewById(R.id.tvEmptyHistory).setVisibility(View.VISIBLE);
            }
        });

        vm.getCurrentSessionId().observe(this, id -> {
            historyAdapter.setSelectedSessionId(id);
        });

        vm.getIsTyping().observe(this, isTyping -> {
            tvStatus.setText(isTyping ? "AI đang soạn câu trả lời..." : "Đang hoạt động");
            tvStatus.setTextColor(isTyping ? getColor(R.color.text_secondary) : getColor(R.color.status_success));
        });

        vm.getSelectedImageUri().observe(this, uri -> {
            if (uri != null) {
                flImagePreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).into(ivPreview);
            } else {
                flImagePreview.setVisibility(View.GONE);
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        String imgUri = vm.getSelectedImageUri().getValue();
        
        if (TextUtils.isEmpty(text) && imgUri == null) return;
        
        ChatMessage userMsg = new ChatMessage(text, imgUri, ChatMessage.TYPE_USER);
        vm.sendMessage(userMsg);

        etMessage.setText("");
    }
}
