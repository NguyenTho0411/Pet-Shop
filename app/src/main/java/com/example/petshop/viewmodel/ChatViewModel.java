package com.example.petshop.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.petshop.BuildConfig;
import com.example.petshop.model.entity.ChatMessage;
import com.example.petshop.model.entity.Order;
import com.example.petshop.repository.OrderRepository;
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

    public void initContext(String userId) {
        new OrderRepository().getOrdersByUser(userId, new OrderRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> data) {
                StringBuilder sb = new StringBuilder();
                sb.append("Dưới đây là danh sách đơn hàng của khách hàng này để bạn tham khảo:\n");
                for (Order o : data) {
                    sb.append("- Đơn hàng: ").append(o.getOrderCode())
                      .append(", Trạng thái: ").append(o.getStatus())
                      .append(", Tổng tiền: ").append(o.getTotalAmount()).append("đ")
                      .append(", Ngày đặt: ").append(o.getCreatedAt()).append("\n");
                }
                userContext = sb.toString();
            }
            @Override public void onFailure(String error) { userContext = "Không thể lấy thông tin đơn hàng."; }
        });
    }

    public void sendMessage(String text) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(new ChatMessage(text, ChatMessage.TYPE_USER));
        messages.setValue(current);

        callOpenAI(text);
    }

    private void callOpenAI(String userMsg) {
        isTyping.postValue(true);

        JsonObject body = new JsonObject();
        body.addProperty("model", "gpt-3.5-turbo");

        JsonArray msgs = new JsonArray();
        
        // System prompt
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", "Bạn là nhân viên tư vấn của cửa hàng PetShop. " +
                "Chỉ trả lời các câu hỏi về thú cưng, cách chăm sóc, và các sản phẩm đồ ăn thú cưng tại shop. " +
                "Bạn cũng có thể hỗ trợ khách tra cứu đơn hàng của họ dựa trên dữ liệu sau: " + userContext +
                " Nếu khách hỏi về các chủ đề không liên quan, hãy từ chối lịch sự và nhắc họ rằng bạn chỉ hỗ trợ về dịch vụ của PetShop.");
        msgs.add(sys);

        // History (optional, let's just send the last few for simplicity)
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMsg);
        msgs.add(user);

        body.add("messages", msgs);

        RequestBody reqBody = RequestBody.create(
                gson.toJson(body),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY)
                .post(reqBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isTyping.postValue(false);
                addBotMessage("Xin lỗi, tôi đang gặp trục trặc kỹ thuật. Hãy thử lại sau!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isTyping.postValue(false);
                if (!response.isSuccessful()) {
                    addBotMessage("Lỗi kết nối với AI (Mã lỗi: " + response.code() + ")");
                    return;
                }
                String json = response.body().string();
                JsonObject resObj = gson.fromJson(json, JsonObject.class);
                String reply = resObj.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                addBotMessage(reply);
            }
        });
    }

    private void addBotMessage(String text) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(new ChatMessage(text, ChatMessage.TYPE_BOT));
        messages.postValue(current);
    }


}
