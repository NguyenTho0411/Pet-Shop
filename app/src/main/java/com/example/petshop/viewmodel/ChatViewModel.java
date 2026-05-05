package com.example.petshop.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.petshop.BuildConfig;
import com.example.petshop.model.entity.Category;
import com.example.petshop.model.entity.ChatMessage;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.Promotion;
import com.example.petshop.model.entity.Voucher;
import com.example.petshop.repository.CategoryRepository;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.repository.OrderRepository;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.repository.PromotionRepository;
import com.example.petshop.repository.VoucherRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatViewModel extends AndroidViewModel {

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>           isTyping = new MutableLiveData<>(false);
    private final MutableLiveData<String>            selectedImageUri = new MutableLiveData<>(null);
    private final MutableLiveData<List<com.example.petshop.model.entity.ChatSession>> sessions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String>            currentSessionId = new MutableLiveData<>(null);

    private final OkHttpClient                       client   = new OkHttpClient.Builder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final Gson                               gson     = new Gson();

    private static final String PREFS_NAME        = "petshop_prefs";
    private static final String KEY_GUEST_MSGS    = "guest_chat_messages";
    private static final String KEY_GUEST_SESSIONS = "guest_chat_sessions";
    private static final int    GUEST_MSG_LIMIT   = 50;

    private String userContext = "";
    private String currentUserId = "";

    public ChatViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean>           getIsTyping() { return isTyping; }
    public LiveData<String>            getSelectedImageUri() { return selectedImageUri; }
    public LiveData<List<com.example.petshop.model.entity.ChatSession>> getSessions() { return sessions; }
    public LiveData<String>            getCurrentSessionId() { return currentSessionId; }

    public void setSelectedImage(String uri) {
        selectedImageUri.postValue(uri);
    }

    public void addWelcomeMessage() {
        List<ChatMessage> current = messages.getValue();
        if (current == null || current.isEmpty()) {
            ChatMessage welcome = new ChatMessage("Chào bạn! Tôi là trợ lý ảo của PetShop. Tôi có thể giúp gì cho bạn hôm nay?", ChatMessage.TYPE_BOT);
            welcome.setSessionId(currentSessionId.getValue());
            addMessageInternal(welcome);
        }
    }

    public void initContext(String userId) {
        this.currentUserId = userId;
        loadSessions();

        StringBuilder sb = new StringBuilder();
        
        // 1. Fetch Categories
        new CategoryRepository().getAll(new CategoryRepository.Callback<>() {
            @Override
            public void onSuccess(List<Category> data) {
                sb.append("DANH MỤC: ");
                if (data != null) {
                    for (Category c : data) if (c.isActive()) sb.append(c.getName()).append(", ");
                }
                fetchPets(userId, sb);
            }
            @Override public void onFailure(String error) { fetchPets(userId, sb); }
        });
    }

    private void fetchPets(String userId, StringBuilder sb) {
        new PetRepository().getAll(new PetRepository.Callback<>() {
            @Override
            public void onSuccess(List<Pet> data) {
                sb.append("\nTHÚ CƯNG ĐANG BÁN: ");
                if (data != null) {
                    int count = 0;
                    for (Pet p : data) {
                        if (p.isAvailable() && count < 10) {
                            sb.append(p.getName()).append(" (").append(p.getBreed()).append(" - ").append(p.getEffectivePrice()).append("đ), ");
                            count++;
                        }
                    }
                }
                fetchFoods(userId, sb);
            }
            @Override public void onFailure(String error) { fetchFoods(userId, sb); }
        });
    }

    private void fetchFoods(String userId, StringBuilder sb) {
        new FoodRepository().getAll(new FoodRepository.Callback<>() {
            @Override
            public void onSuccess(List<Food> data) {
                sb.append("\nTHỨC ĂN/PHỤ KIỆN: ");
                if (data != null) {
                    int count = 0;
                    for (Food f : data) {
                        if (f.isAvailable() && count < 10) {
                            sb.append(f.getName()).append(" (").append(f.getBrand()).append(" - ").append(f.getEffectivePrice()).append("đ), ");
                            count++;
                        }
                    }
                }
                fetchOrders(userId, sb);
            }
            @Override public void onFailure(String error) { fetchOrders(userId, sb); }
        });
    }

    private void fetchOrders(String userId, StringBuilder sb) {
        new OrderRepository().getOrdersByUser(userId, new OrderRepository.Callback<>() {
            @Override
            public void onSuccess(List<Order> data) {
                sb.append("\nĐƠN HÀNG CỦA BẠN: ");
                if (data == null || data.isEmpty()) {
                    sb.append("Chưa có.");
                } else {
                    for (Order o : data) {
                        sb.append("Mã ").append(o.getOrderCode()).append(" (").append(o.getStatus()).append("), ");
                    }
                }
                fetchPromotions(sb);
            }
            @Override public void onFailure(String error) { fetchPromotions(sb); }
        });
    }

    private void fetchPromotions(StringBuilder sb) {
        new PromotionRepository().getActive(new PromotionRepository.Callback<>() {
            @Override
            public void onSuccess(List<Promotion> data) {
                sb.append("\nKHUYẾN MÃI: ");
                if (data != null) {
                    for (Promotion p : data) sb.append(p.getName()).append(": ").append(p.getDescription()).append("; ");
                }
                fetchVouchers(sb);
            }
            @Override public void onFailure(String error) { fetchVouchers(sb); }
        });
    }

    private void fetchVouchers(StringBuilder sb) {
        new VoucherRepository().getAll(new VoucherRepository.Callback<>() {
            @Override
            public void onSuccess(List<Voucher> data) {
                sb.append("\nVOUCHER: ");
                if (data != null) {
                    for (Voucher v : data) if (v.isActive()) sb.append(v.getCode()).append(" (").append(v.getDescription()).append("), ");
                }
                userContext = sb.toString();
            }
            @Override public void onFailure(String error) { userContext = sb.toString(); }
        });
    }

    private void loadSessions() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            loadGuestSessions();
            return;
        }

        FirebaseHelper.db().collection("users").document(currentUserId)
                .collection("sessions")
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<com.example.petshop.model.entity.ChatSession> sessionList = queryDocumentSnapshots.toObjects(com.example.petshop.model.entity.ChatSession.class);
                    if (sessionList == null) sessionList = new ArrayList<>();
                    
                    sessions.postValue(new ArrayList<>(sessionList));
                    if (!sessionList.isEmpty()) {
                        switchSession(sessionList.get(0).getId());
                    } else {
                        startNewChat();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to guest sessions if firestore fails
                    loadGuestSessions();
                });
    }

    private void loadGuestSessions() {
        String json = getApplication()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_GUEST_SESSIONS, null);
        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<List<com.example.petshop.model.entity.ChatSession>>() {}.getType();
            List<com.example.petshop.model.entity.ChatSession> sessionList = gson.fromJson(json, type);
            sessions.postValue(new ArrayList<>(sessionList));
            if (!sessionList.isEmpty()) {
                switchSession(sessionList.get(0).getId());
                return;
            }
        }
        startNewChat();
    }

    public void switchSession(String sessionId) {
        currentSessionId.postValue(sessionId);
        loadChatHistory(sessionId);
    }

    private void loadChatHistory(String sessionId) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            loadGuestMessages(sessionId);
            return;
        }

        FirebaseHelper.db().collection("users").document(currentUserId)
                .collection("chats")
                .whereEqualTo("sessionId", sessionId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatMessage> history = queryDocumentSnapshots.toObjects(ChatMessage.class);
                    messages.postValue(history != null ? history : new ArrayList<>());
                    if (history == null || history.isEmpty()) {
                        addWelcomeMessage();
                    }
                });
    }

    private void loadGuestMessages(String sessionId) {
        String json = getApplication()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_GUEST_MSGS + "_" + sessionId, null);
        if (json != null && !json.isEmpty()) {
            Type listType = new TypeToken<List<ChatMessage>>() {}.getType();
            List<ChatMessage> history = gson.fromJson(json, listType);
            messages.postValue(history != null ? history : new ArrayList<>());
        } else {
            messages.postValue(new ArrayList<>());
            addWelcomeMessage();
        }
    }

    private void saveGuestMessages() {
        String sid = currentSessionId.getValue();
        if (sid == null) return;

        List<ChatMessage> current = messages.getValue();
        if (current == null) return;
        
        List<ChatMessage> toSave = current.size() > GUEST_MSG_LIMIT
                ? current.subList(current.size() - GUEST_MSG_LIMIT, current.size())
                : current;
        getApplication()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_GUEST_MSGS + "_" + sid, gson.toJson(toSave)).apply();
        
        // Update session timestamp in guest sessions
        List<com.example.petshop.model.entity.ChatSession> currentSessions = sessions.getValue();
        if (currentSessions != null) {
            for (com.example.petshop.model.entity.ChatSession s : currentSessions) {
                if (s.getId().equals(sid)) {
                    s.setLastTimestamp(System.currentTimeMillis());
                    if (current.size() > 1 && (s.getTitle() == null || s.getTitle().startsWith("Cuộc trò chuyện mới"))) {
                        String firstMsg = current.get(1).getText();
                        if (firstMsg.length() > 20) firstMsg = firstMsg.substring(0, 20) + "...";
                        s.setTitle(firstMsg);
                    }
                    break;
            }
        }
            saveGuestSessionsInternal(currentSessions);
        }
    }

    private void saveGuestSessionsInternal(List<com.example.petshop.model.entity.ChatSession> sessionList) {
        getApplication()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_GUEST_SESSIONS, gson.toJson(sessionList)).apply();
        sessions.postValue(new ArrayList<>(sessionList));
    }

    private void saveMessageToFirestore(ChatMessage msg) {
        String sid = currentSessionId.getValue();
        if (sid == null) return;
        msg.setSessionId(sid);

        if (currentUserId == null || currentUserId.isEmpty()) {
            saveGuestMessages();
            return;
        }

        FirebaseHelper.db().collection("users").document(currentUserId)
                .collection("chats")
                .add(msg);

        // Update session lastTimestamp
        FirebaseHelper.db().collection("users").document(currentUserId)
                .collection("sessions").document(sid)
                .update("lastTimestamp", System.currentTimeMillis());
        
        // Cập nhật title nếu là tin nhắn người dùng đầu tiên (sau welcome)
        List<ChatMessage> currentMsgs = messages.getValue();
        if (currentMsgs != null && currentMsgs.size() >= 2 && msg.getType() == ChatMessage.TYPE_USER) {
            String title = msg.getText();
            if (title != null && !title.isEmpty()) {
                if (title.length() > 25) title = title.substring(0, 25) + "...";
                final String finalTitle = title;
                FirebaseHelper.db().collection("users").document(currentUserId)
                        .collection("sessions").document(sid)
                        .update("title", finalTitle)
                        .addOnSuccessListener(aVoid -> {
                            // Cập nhật lại danh sách local
                            List<com.example.petshop.model.entity.ChatSession> currentSessions = sessions.getValue();
                            if (currentSessions != null) {
                                for (com.example.petshop.model.entity.ChatSession s : currentSessions) {
                                    if (s.getId().equals(sid)) {
                                        s.setTitle(finalTitle);
                                        break;
                                    }
                                }
                                sessions.postValue(new ArrayList<>(currentSessions));
                            }
                        });
            }
        }
    }

    public void startNewChat() {
        String newId = String.valueOf(System.currentTimeMillis());
        com.example.petshop.model.entity.ChatSession newSession = new com.example.petshop.model.entity.ChatSession(newId, "Cuộc trò chuyện mới");
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            List<com.example.petshop.model.entity.ChatSession> current = sessions.getValue();
            List<com.example.petshop.model.entity.ChatSession> newList = current != null ? new ArrayList<>(current) : new ArrayList<>();
            newList.add(0, newSession);
            saveGuestSessionsInternal(newList);
            switchSession(newId);
            return;
        }

        FirebaseHelper.db().collection("users").document(currentUserId)
                .collection("sessions").document(newId)
                .set(newSession)
                .addOnSuccessListener(aVoid -> loadSessions())
                .addOnFailureListener(e -> {
                    // Fallback to guest if failed
                    currentUserId = "";
                    startNewChat();
                });
    }

    public void sendMessage(ChatMessage msg) {
        if (msg.getImageUrl() != null) {
            uploadImageAndSend(msg);
        } else {
            addMessageInternal(msg);
            callOpenAI(msg.getText(), null);
        }
    }

    private void uploadImageAndSend(ChatMessage msg) {
        isTyping.postValue(true);
        String localUri = msg.getImageUrl();
        Uri fileUri = Uri.parse(localUri);
        String fileName = "chat_" + System.currentTimeMillis() + ".jpg";
        com.google.firebase.storage.StorageReference ref = 
                com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child("chats/" + fileName);

        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    msg.setImageUrl(uri.toString());
                    addMessageInternal(msg);
                    callOpenAI(msg.getText(), localUri);
                }))
                .addOnFailureListener(e -> {
                    isTyping.postValue(false);
                    addBotMessage("Lỗi tải ảnh lên: " + e.getMessage());
                });
    }

    private void addMessageInternal(ChatMessage msg) {
        List<ChatMessage> current = messages.getValue();
        List<ChatMessage> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        updated.add(msg);
        messages.postValue(updated);
        saveMessageToFirestore(msg);
    }

    private void callOpenAI(String userMsg, String imgUri) {
        isTyping.postValue(true);
        final String sid = currentSessionId.getValue();

        JsonObject body = new JsonObject();
        body.addProperty("model", "gpt-4o-mini");
        body.addProperty("stream", true);

        JsonArray msgs = new JsonArray();
        
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", "Bạn là trợ lý ảo PetShop. " +
                "DỰA VÀO DỮ LIỆU SAU ĐỂ TRẢ LỜI: \n" + userContext + "\n" +
                "QUY TẮC: " +
                "1. CHỈ sử dụng dữ liệu trên để tư vấn sản phẩm, đơn hàng, khuyến mãi. " +
                "2. Nếu khách hỏi sản phẩm không có trong danh sách, hãy nói shop hiện chưa có nhưng sẽ cập nhật sau. " +
                "3. TUYỆT ĐỐI KHÔNG trả lời các câu hỏi ngoài lề (toán, code, chính trị, kiến thức chung không liên quan thú cưng). " +
                "Nếu bị hỏi ngoài lề, đáp: 'Xin lỗi, tôi là trợ lý PetShop, tôi chỉ hỗ trợ các vấn đề về thú cưng và cửa hàng.' " +
                "4. Thân thiện, ngắn gọn, dùng tiếng Việt.");
        msgs.add(sys);

        List<ChatMessage> history = messages.getValue();
        if (history != null) {
            int start = Math.max(0, history.size() - 7); 
            for (int i = start; i < history.size() - 1; i++) { 
                ChatMessage m = history.get(i);
                JsonObject h = new JsonObject();
                h.addProperty("role", m.getType() == ChatMessage.TYPE_USER ? "user" : "assistant");
                h.addProperty("content", m.getText());
                msgs.add(h);
            }
        }

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        
        if (imgUri != null) {
            JsonArray content = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", userMsg);
            content.add(textPart);

            String base64 = uriToBase64(imgUri);
            if (base64 != null) {
                JsonObject imgPart = new JsonObject();
                imgPart.addProperty("type", "image_url");
                JsonObject imgUrl = new JsonObject();
                imgUrl.addProperty("url", "data:image/jpeg;base64," + base64);
                imgPart.add("image_url", imgUrl);
                content.add(imgPart);
            }
            
            user.add("content", content);
            selectedImageUri.postValue(null);
        } else {
            user.addProperty("content", userMsg);
        }
        msgs.add(user);

        body.add("messages", msgs);
        // ... rest of streaming logic remains the same

        RequestBody reqBody = RequestBody.create(gson.toJson(body), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY)
                .post(reqBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isTyping.postValue(false);
                addBotMessage("Lỗi kết nối. Vui lòng thử lại!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    isTyping.postValue(false);
                    addBotMessage("AI hiện không phản hồi. (Lỗi " + response.code() + ")");
                    return;
                }

                // Create placeholder for streaming message
                ChatMessage streamingMsg = new ChatMessage("", ChatMessage.TYPE_BOT);
                streamingMsg.setSessionId(sid);
                final long streamingTimestamp = streamingMsg.getTimestamp();
                addMessageInternalSync(streamingMsg);

                okio.BufferedSource source = response.body().source();
                StringBuilder fullText = new StringBuilder();

                while (true) {
                    String line = source.readUtf8Line();
                    if (line == null) break;
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if (data.equals("[DONE]")) break;

                        try {
                            JsonObject chunk = gson.fromJson(data, JsonObject.class);
                            JsonArray choices = chunk.getAsJsonArray("choices");
                            if (choices.size() > 0) {
                                JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                                if (delta.has("content")) {
                                    String content = delta.get("content").getAsString();
                                    fullText.append(content);
                                    
                                    // Tạo bản sao với text mới nhưng giữ nguyên timestamp để DiffUtil nhận diện là cùng 1 item
                                    ChatMessage updatedMsg = new ChatMessage(fullText.toString(), ChatMessage.TYPE_BOT);
                                    updatedMsg.setSessionId(sid);
                                    updatedMsg.setTimestamp(streamingTimestamp);
                                    
                                    List<ChatMessage> currentList = messages.getValue();
                                    if (currentList != null && !currentList.isEmpty()) {
                                        List<ChatMessage> newList = new ArrayList<>(currentList);
                                        newList.set(newList.size() - 1, updatedMsg);
                                        messages.postValue(newList);
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                isTyping.postValue(false);
                
                // Lưu tin nhắn cuối cùng hoàn chỉnh vào Firestore
                ChatMessage finalMsg = new ChatMessage(fullText.toString(), ChatMessage.TYPE_BOT);
                finalMsg.setSessionId(sid);
                finalMsg.setTimestamp(streamingTimestamp);
                saveMessageToFirestore(finalMsg);
            }
        });
    }

    private void addMessageInternalSync(ChatMessage msg) {
        List<ChatMessage> current = messages.getValue();
        List<ChatMessage> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        updated.add(msg);
        messages.postValue(updated);
    }

    private void addBotMessage(String text) {
        ChatMessage botMsg = new ChatMessage(text, ChatMessage.TYPE_BOT);
        List<ChatMessage> current = messages.getValue();
        List<ChatMessage> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        updated.add(botMsg);
        messages.postValue(updated);
        saveMessageToFirestore(botMsg);
    }

    private String uriToBase64(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            InputStream inputStream = getApplication().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // Resize if too large
            if (bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024) {
                float scale = Math.min(1024f / bitmap.getWidth(), 1024f / bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] bytes = outputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
