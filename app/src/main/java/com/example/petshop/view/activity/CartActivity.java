package com.example.petshop.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Cart;
import com.example.petshop.model.entity.CartItem;
import com.example.petshop.view.adapter.CartItemAdapter;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.example.petshop.viewmodel.CartViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private CartViewModel    vm;
    private CartItemAdapter  adapter;
    private LinearLayout     llEmpty, llCheckoutBar;
    private RecyclerView     rvItems;
    private TextView         tvItemCount, tvSubtotal, tvTotal;
    private ProgressBar      progressBar;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        vm = new ViewModelProvider(this).get(CartViewModel.class);
        initViews();
        observeViewModel();
        vm.loadCart();
    }

    private void initViews() {
        progressBar    = findViewById(R.id.progressBar);
        llEmpty        = findViewById(R.id.llEmpty);
        llCheckoutBar  = findViewById(R.id.llCheckoutBar);
        rvItems        = findViewById(R.id.rvCartItems);
        tvItemCount    = findViewById(R.id.tvItemCount);
        tvSubtotal     = findViewById(R.id.tvSubtotal);
        tvTotal        = findViewById(R.id.tvTotal);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartItemAdapter(new ArrayList<>(), new CartItemAdapter.OnCartAction() {
            public void onRemove(CartItem item) { confirmRemove(item); }
            public void onQtyChange(CartItem item, int newQty) { vm.updateFoodQty(item.getId(), newQty); }
        });
        rvItems.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGoShop).setOnClickListener(v -> finish());
        ((Button) findViewById(R.id.btnCheckout)).setOnClickListener(v ->
                startActivity(new Intent(this, CheckoutActivity.class)));
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getCart().observe(this, cart -> {
            if (cart == null || cart.isEmpty()) {
                llEmpty.setVisibility(View.VISIBLE);
                rvItems.setVisibility(View.GONE);
                llCheckoutBar.setVisibility(View.GONE);
                tvItemCount.setText("0 sản phẩm");
            } else {
                llEmpty.setVisibility(View.GONE);
                rvItems.setVisibility(View.VISIBLE);
                llCheckoutBar.setVisibility(View.VISIBLE);
                adapter.updateList(cart.getItems());
                int count = cart.calculateTotalItems();
                tvItemCount.setText(count + " sản phẩm");
                double sub = cart.calculateSubtotal();
                tvSubtotal.setText(VND.format((long) sub) + "đ");
                tvTotal.setText(VND.format((long) sub) + "đ");
            }
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });

        vm.getSuccess().observe(this, msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void confirmRemove(CartItem item) {
        DialogUtils.showRemoveFromCartDialog(this, item.getProductName(),
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    vm.removeItem(item.getId());
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
