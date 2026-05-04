package com.example.petshop.view.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Food;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.view.activity.FoodDetailActivity;
import com.example.petshop.view.activity.PetDetailActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface OnProductClick { void onClick(Object product); }
    public interface OnAddToCart { void onAddToCart(Object product); }

    private final List<Object> products = new ArrayList<>();
    private final OnProductClick listener;
    private final OnAddToCart cartListener;
    private final boolean isPetCategory;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    public ProductAdapter(List<Object> products, OnProductClick listener) {
        if (products != null) this.products.addAll(products);
        this.listener = listener;
        this.cartListener = null;
        this.isPetCategory = false;
    }

    public ProductAdapter(List<Object> products, OnProductClick listener, OnAddToCart cartListener) {
        if (products != null) this.products.addAll(products);
        this.listener = listener;
        this.cartListener = cartListener;
        this.isPetCategory = false;
    }

    public ProductAdapter(List<Object> products, OnProductClick listener, OnAddToCart cartListener, boolean isPetCategory) {
        if (products != null) this.products.addAll(products);
        this.listener = listener;
        this.cartListener = cartListener;
        this.isPetCategory = isPetCategory;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Object item = products.get(pos);

        if (item instanceof Pet) {
            bindPet(h, (Pet) item);
        } else if (item instanceof Food) {
            bindFood(h, (Food) item);
        }
    }

    private void bindPet(VH h, Pet pet) {
        h.tvName.setText(pet.getName() != null ? pet.getName() : "");
        h.tvBrand.setVisibility(View.GONE);

        double price = pet.getEffectivePrice();
        h.tvPrice.setText(price > 0 ? VND.format((long) price) + "đ" : "Liên hệ");

        if (pet.hasPromotion() && pet.getOriginalPrice() > 0 && pet.getOriginalPrice() > pet.getEffectivePrice()) {
            h.tvOriginalPrice.setVisibility(View.VISIBLE);
            h.tvOriginalPrice.setText(VND.format((long) pet.getOriginalPrice()) + "đ");
            h.tvOriginalPrice.setPaintFlags(h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvSaleBadge.setVisibility(View.VISIBLE);
            int pct = (int) Math.round((1 - pet.getEffectivePrice() / pet.getOriginalPrice()) * 100);
            h.tvSaleBadge.setText("-" + pct + "%");
        } else {
            h.tvOriginalPrice.setVisibility(View.GONE);
            h.tvSaleBadge.setVisibility(View.GONE);
        }

        String thumb = pet.getThumbnailUrl();
        if (thumb != null && !thumb.isEmpty()) {
            Glide.with(h.itemView).load(thumb).centerCrop()
                    .placeholder(R.mipmap.ic_launcher).into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.mipmap.ic_launcher);
        }

        String status = pet.getStatus() != null ? pet.getStatus() : "";
        if (Pet.STATUS_AVAILABLE.equals(status)) {
            h.tvStatusBadge.setVisibility(View.GONE);
        } else {
            h.tvStatusBadge.setVisibility(View.VISIBLE);
            if (Pet.STATUS_SOLD.equals(status)) {
                h.tvStatusBadge.setText("ĐÃ BÁN");
                h.tvStatusBadge.getBackground().setTint(h.itemView.getContext().getColor(R.color.badge_red));
            } else if (Pet.STATUS_RESERVED.equals(status)) {
                h.tvStatusBadge.setText("ĐÃ ĐẶT");
                h.tvStatusBadge.getBackground().setTint(h.itemView.getContext().getColor(R.color.text_secondary));
            } else {
                h.tvStatusBadge.setVisibility(View.GONE);
            }
        }

        h.ivAddToCart.setVisibility(Pet.STATUS_AVAILABLE.equals(status) ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(pet);
        });
        h.ivAddToCart.setOnClickListener(v -> {
            if (cartListener != null) cartListener.onAddToCart(pet);
        });
    }

    private void bindFood(VH h, Food food) {
        h.tvName.setText(food.getName() != null ? food.getName() : "");
        h.tvBrand.setVisibility(food.getBrand() != null && !food.getBrand().isEmpty() ? View.VISIBLE : View.GONE);
        h.tvBrand.setText(food.getBrand() != null ? food.getBrand() : "");

        double price = food.getEffectivePrice();
        h.tvPrice.setText(price > 0 ? VND.format((long) price) + "đ" : "Liên hệ");

        if (food.hasPromotion() && food.getOriginalPrice() > 0 && food.getOriginalPrice() > food.getEffectivePrice()) {
            h.tvOriginalPrice.setVisibility(View.VISIBLE);
            h.tvOriginalPrice.setText(VND.format((long) food.getOriginalPrice()) + "đ");
            h.tvOriginalPrice.setPaintFlags(h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvSaleBadge.setVisibility(View.VISIBLE);
            int pct = (int) Math.round((1 - food.getEffectivePrice() / food.getOriginalPrice()) * 100);
            h.tvSaleBadge.setText("-" + pct + "%");
        } else {
            h.tvOriginalPrice.setVisibility(View.GONE);
            h.tvSaleBadge.setVisibility(View.GONE);
        }

        String thumb = food.getThumbnailUrl();
        if (thumb != null && !thumb.isEmpty()) {
            Glide.with(h.itemView).load(thumb).centerCrop()
                    .placeholder(R.mipmap.ic_launcher).into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.mipmap.ic_launcher);
        }

        h.tvStatusBadge.setVisibility(View.GONE);
        h.ivAddToCart.setVisibility(View.VISIBLE);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(food);
        });
        h.ivAddToCart.setOnClickListener(v -> {
            if (cartListener != null) cartListener.onAddToCart(food);
        });
    }

    @Override public int getItemCount() { return products.size(); }

    public void updateData(List<Object> newProducts) {
        products.clear();
        if (newProducts != null) products.addAll(newProducts);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage, ivAddToCart;
        TextView tvName, tvBrand, tvPrice, tvOriginalPrice, tvSaleBadge, tvStatusBadge;

        VH(View v) {
            super(v);
            ivImage = v.findViewById(R.id.ivProductImage);
            ivAddToCart = v.findViewById(R.id.ivAddToCart);
            tvName = v.findViewById(R.id.tvProductName);
            tvBrand = v.findViewById(R.id.tvProductBrand);
            tvPrice = v.findViewById(R.id.tvProductPrice);
            tvOriginalPrice = v.findViewById(R.id.tvOriginalPrice);
            tvSaleBadge = v.findViewById(R.id.tvSaleBadge);
            tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
        }
    }
}
