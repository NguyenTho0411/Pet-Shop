package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>           isTyping = new MutableLiveData<>(false);
    private final OkHttpClient                       client   = new OkHttpClient();
    private final Gson                               gson     = new Gson();

    private String userContext = "";

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean>           getIsTyping() { return isTyping; }

    public void addWelcomeMessage() {
        List<ChatMessage> current = messages.getValue();
        if (current == null || current.isEmpty()) {
            addBotMessage("Chào bạn! Tôi là trợ lý ảo của PetShop. Tôi có thể giúp gì cho bạn hôm nay?");
        }
    }

    public void initContext(String userId) {
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

    public void sendMessage(String text) {
        List<ChatMessage> current = messages.getValue();
        List<ChatMessage> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        updated.add(new ChatMessage(text, ChatMessage.TYPE_USER));
        messages.setValue(updated);

        callOpenAI(text);
    }

    private void callOpenAI(String userMsg) {
        isTyping.postValue(true);

        JsonObject body = new JsonObject();
        body.addProperty("model", "gpt-3.5-turbo");

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
        user.addProperty("content", userMsg);
        msgs.add(user);

        body.add("messages", msgs);

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
                isTyping.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject resObj = gson.fromJson(json, JsonObject.class);
                    String reply = resObj.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                    addBotMessage(reply);
                } else {
                    addBotMessage("AI hiện không phản hồi. (Lỗi " + response.code() + ")");
                }
            }
        });
    }

    private void addBotMessage(String text) {
        List<ChatMessage> current = messages.getValue();
        List<ChatMessage> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        updated.add(new ChatMessage(text, ChatMessage.TYPE_BOT));
        messages.postValue(updated);
    }
}
