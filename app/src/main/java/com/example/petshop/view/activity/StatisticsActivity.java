package com.example.petshop.view.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Order;
import com.example.petshop.model.entity.OrderItem;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.ReturnRequest;
import com.example.petshop.model.entity.User;
import com.example.petshop.repository.FoodRepository;
import com.example.petshop.repository.OrderRepository;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.repository.ReturnRepository;
import com.example.petshop.repository.UserRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private LinearLayout chartContainer, chartLabels, topProductsContainer;
    private TextView tvTotalRevenue, tvTotalOrders, tvTotalPets, tvTotalFoods;
    private TextView tvTotalUsers, tvTotalReturns;
    private TextView tvPendingOrders, tvProcessingOrders, tvCompletedToday;
    private ProgressBar progressBar;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private int loadedCount = 0;
    private int totalRequests = 6;

    private double totalRevenue = 0;
    private int totalOrders = 0;
    private int pendingOrders = 0;
    private int processingOrders = 0;
    private int completedToday = 0;
    private int totalPets = 0;
    private int totalFoods = 0;
    private int totalUsers = 0;
    private int totalReturns = 0;
    private Map<String, Integer> topProducts = new HashMap<>();
    private List<Long> dailyRevenue = new ArrayList<>();
    private List<String> dailyLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initViews();
        loadAllStats();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        chartContainer = findViewById(R.id.chartContainer);
        chartLabels = findViewById(R.id.chartLabels);
        topProductsContainer = findViewById(R.id.topProductsContainer);

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvTotalPets = findViewById(R.id.tvTotalPets);
        tvTotalFoods = findViewById(R.id.tvTotalFoods);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalReturns = findViewById(R.id.tvTotalReturns);

        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        tvProcessingOrders = findViewById(R.id.tvProcessingOrders);
        tvCompletedToday = findViewById(R.id.tvCompletedToday);
        progressBar = findViewById(R.id.progressBar);

        // Initialize daily revenue for last 7 days
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) cal.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            dailyRevenue.add(0L);
            dailyLabels.add(DATE_FORMAT.format(day.getTime())); // Use yyyy-MM-dd to match Firestore format
        }
    }

    private void loadAllStats() {
        progressBar.setVisibility(View.VISIBLE);

        loadOrders();
        loadPets();
        loadFoods();
        loadUsers();
        loadReturns();
    }

    private void checkAllLoaded() {
        loadedCount++;
        if (loadedCount >= totalRequests) {
            progressBar.setVisibility(View.GONE);
            updateUI();
        }
    }

    private void updateUI() {
        tvTotalRevenue.setText(VND.format((long) totalRevenue));
        tvTotalOrders.setText(String.valueOf(totalOrders));
        tvTotalPets.setText(String.valueOf(totalPets));
        tvTotalFoods.setText(String.valueOf(totalFoods));
        tvTotalUsers.setText(String.valueOf(totalUsers));
        tvTotalReturns.setText(String.valueOf(totalReturns));

        tvPendingOrders.setText(String.valueOf(pendingOrders));
        tvProcessingOrders.setText(String.valueOf(processingOrders));
        tvCompletedToday.setText(String.valueOf(completedToday));

        drawChart();
        drawTopProducts();
    }

    private void drawChart() {
        chartContainer.removeAllViews();
        chartLabels.removeAllViews();

        if (dailyRevenue.isEmpty()) return;

        long maxRevenue = 0;
        for (Long rev : dailyRevenue) {
            if (rev > maxRevenue) maxRevenue = rev;
        }
        if (maxRevenue == 0) maxRevenue = 1000000;

        int[] colors = {
                Color.parseColor("#4CAF50"),
                Color.parseColor("#8BC34A"),
                Color.parseColor("#CDDC39"),
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#FF5722")
        };

        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < dailyRevenue.size(); i++) {
            long revenue = dailyRevenue.get(i);

            LinearLayout barContainer = new LinearLayout(this);
            barContainer.setOrientation(LinearLayout.VERTICAL);
            barContainer.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            barParams.setMargins(4, 0, 4, 0);
            barContainer.setLayoutParams(barParams);

            View bar = new View(this);
            int height = (int) ((revenue * 120.0) / maxRevenue);
            if (height < 4) height = 4;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(30, height);
            bar.setLayoutParams(params);
            bar.setBackgroundColor(colors[i % colors.length]);

            TextView amountLabel = new TextView(this);
            amountLabel.setText(VND.format(revenue / 1000) + "k");
            amountLabel.setTextSize(8);
            amountLabel.setTextColor(Color.parseColor("#666666"));
            amountLabel.setGravity(Gravity.CENTER);

            barContainer.addView(amountLabel);
            barContainer.addView(bar);

            chartContainer.addView(barContainer);

            TextView label = new TextView(this);
            try {
                Date date = parseFormat.parse(dailyLabels.get(i));
                label.setText(displayFormat.format(date));
            } catch (Exception e) {
                label.setText(dailyLabels.get(i));
            }
            label.setTextSize(10);
            label.setTextColor(Color.parseColor("#888888"));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            labelParams.setMargins(4, 8, 4, 0);
            label.setLayoutParams(labelParams);
            chartLabels.addView(label);
        }
    }

    private void drawTopProducts() {
        topProductsContainer.removeAllViews();

        if (topProducts.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có dữ liệu");
            tv.setTextSize(14);
            tv.setTextColor(getColor(R.color.text_hint));
            topProductsContainer.addView(tv);
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(topProducts.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int count = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (count >= 5) break;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView rank = new TextView(this);
            rank.setText("#" + (count + 1));
            rank.setTextSize(14);
            rank.setTextColor(Color.parseColor("#F5A623"));
            rank.setTypeface(null, android.graphics.Typeface.BOLD);
            rank.setLayoutParams(new LinearLayout.LayoutParams(40, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView name = new TextView(this);
            name.setText(entry.getKey());
            name.setTextSize(14);
            name.setTextColor(getColor(R.color.text_primary));
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView sold = new TextView(this);
            sold.setText(entry.getValue() + " đã bán");
            sold.setTextSize(12);
            sold.setTextColor(getColor(R.color.text_secondary));

            row.addView(rank);
            row.addView(name);
            row.addView(sold);
            topProductsContainer.addView(row);

            if (count < sorted.size() - 1 && count < 4) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                topProductsContainer.addView(divider);
            }

            count++;
        }
    }

    private void loadOrders() {
        db.collection("orders")
                .get()
                .addOnSuccessListener(snap -> {
                    totalOrders = snap.size();
                    String today = DATE_FORMAT.format(new Date());

                    for (var doc : snap.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            String status = order.getStatus();
                            boolean isCompleted = Order.STATUS_COMPLETED.equals(status) || Order.STATUS_DELIVERED.equals(status);

                            if (isCompleted) {
                                totalRevenue += order.getTotalAmount();
                            }

                            if (Order.STATUS_PENDING.equals(status)) pendingOrders++;
                            if (Order.STATUS_CONFIRMED.equals(status) ||
                                Order.STATUS_PREPARING.equals(status) ||
                                Order.STATUS_SHIPPING.equals(status)) {
                                processingOrders++;
                            }

                            String createdAt = order.getCreatedAt();
                            if (createdAt != null && createdAt.length() >= 10) {
                                String orderDate = createdAt.substring(0, 10);

                                if (orderDate.equals(today) && isCompleted) {
                                    completedToday++;
                                }

                                for (int i = 0; i < dailyLabels.size(); i++) {
                                    if (orderDate.equals(dailyLabels.get(i))) {
                                        if (isCompleted) {
                                            long current = dailyRevenue.get(i);
                                            dailyRevenue.set(i, current + (long) order.getTotalAmount());
                                        }
                                        break;
                                    }
                                }
                            }

                            countOrderProducts(order);
                        }
                    }
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    checkAllLoaded();
                });
    }

    private void countOrderProducts(Order order) {
        if (order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            String name = item.getProductName();
            int qty = item.getQuantity();
            topProducts.put(name, topProducts.getOrDefault(name, 0) + qty);
        }
    }

    private void loadPets() {
        db.collection("pets")
                .get()
                .addOnSuccessListener(snap -> {
                    totalPets = snap.size();
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thú cưng", Toast.LENGTH_SHORT).show();
                    checkAllLoaded();
                });
    }

    private void loadFoods() {
        db.collection("foods")
                .get()
                .addOnSuccessListener(snap -> {
                    totalFoods = snap.size();
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thức ăn", Toast.LENGTH_SHORT).show();
                    checkAllLoaded();
                });
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(snap -> {
                    totalUsers = snap.size();
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải người dùng", Toast.LENGTH_SHORT).show();
                    checkAllLoaded();
                });
    }

    private void loadReturns() {
        db.collection("returns")
                .get()
                .addOnSuccessListener(snap -> {
                    totalReturns = snap.size();
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải hoàn trả", Toast.LENGTH_SHORT).show();
                    checkAllLoaded();
                });
    }
}
