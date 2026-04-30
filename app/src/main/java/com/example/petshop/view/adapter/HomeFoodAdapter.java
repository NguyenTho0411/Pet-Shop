package com.example.petshop.view.adapter;

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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HomeFoodAdapter extends RecyclerView.Adapter<HomeFoodAdapter.VH> {

    public interface OnFoodClick { void onClick(Food food); }
    public interface OnCartClick { void onAddToCart(Food food); }

    private final List<Food>  list;
    private final OnFoodClick clickListener;
    private final OnCartClick cartListener;

    private static final NumberFormat VND =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    public HomeFoodAdapter(List<Food> list, OnFoodClick click, OnCartClick cart) {
        this.list          = list;
        this.clickListener = click;
        this.cartListener  = cart;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Food food = list.get(pos);

        h.tvName.setText(food.getName());
        h.tvBrand.setText(food.getBrand() != null ? food.getBrand() : "");

        // Weight
        String wt = food.getWeightGram() > 0
                ? (food.getWeightGram() >= 1000
                        ? (food.getWeightGram() / 1000.0) + " kg"
                        : food.getWeightGram() + " g")
                : (food.getUnit() != null ? food.getUnit() : "");
        h.tvWeight.setText(wt);

        // Price
        double price = food.getEffectivePrice();
        h.tvPrice.setText(price > 0 ? VND.format((long) price) + "đ" : "Liên hệ");

        // Sale badge
        if (food.hasPromotion() && food.getDiscountedPrice() > 0) {
            h.tvSaleBadge.setVisibility(View.VISIBLE);
        } else {
            h.tvSaleBadge.setVisibility(View.GONE);
        }

        // Image
        if (food.getThumbnailUrl() != null && !food.getThumbnailUrl().isEmpty()) {
            Glide.with(h.itemView).load(food.getThumbnailUrl())
                    .centerCrop().placeholder(R.mipmap.ic_launcher).into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.mipmap.ic_launcher);
        }

        h.itemView.setOnClickListener(v -> clickListener.onClick(food));
        h.ivAddToCart.setOnClickListener(v -> cartListener.onAddToCart(food));
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Food> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage, ivAddToCart;
        TextView  tvName, tvBrand, tvWeight, tvPrice, tvSaleBadge;

        VH(View v) {
            super(v);
            ivImage     = v.findViewById(R.id.ivFoodImage);
            ivAddToCart = v.findViewById(R.id.ivAddToCart);
            tvName      = v.findViewById(R.id.tvFoodName);
            tvBrand     = v.findViewById(R.id.tvBrand);
            tvWeight    = v.findViewById(R.id.tvWeight);
            tvPrice     = v.findViewById(R.id.tvFoodPrice);
            tvSaleBadge = v.findViewById(R.id.tvSaleBadge);
        }
    }
}
