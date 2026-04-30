package com.example.petshop.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Category;

import java.util.List;

public class HomeCategoryAdapter extends RecyclerView.Adapter<HomeCategoryAdapter.VH> {

    public interface OnCategoryClick {
        void onClick(Category category, int position);
    }

    private final List<Category>   list;
    private final OnCategoryClick  listener;
    private int selectedPos = 0;

    public HomeCategoryAdapter(List<Category> list, OnCategoryClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_home, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Category cat = list.get(pos);
        h.tvName.setText(cat.getName());

        Context ctx = h.itemView.getContext();

        // Selected → orange circle, else white
        if (pos == selectedPos) {
            h.vCircleBg.setBackgroundResource(R.drawable.bg_circle_orange);
        } else {
            h.vCircleBg.setBackgroundResource(R.drawable.bg_circle_white);
        }

        // Load category image or show emoji placeholder
        if (cat.getImageUrl() != null && !cat.getImageUrl().isEmpty()) {
            Glide.with(ctx).load(cat.getImageUrl())
                    .circleCrop().into(h.ivImage);
        } else {
            // Placeholder emoji via text trick or default launcher
            h.ivImage.setImageResource(R.mipmap.ic_launcher);
        }

        h.itemView.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = h.getAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(selectedPos);
            listener.onClick(cat, selectedPos);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Category> newList) {
        list.clear();
        list.addAll(newList);
        selectedPos = 0;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        View      vCircleBg;
        ImageView ivImage;
        TextView  tvName;

        VH(View v) {
            super(v);
            vCircleBg = v.findViewById(R.id.vCircleBg);
            ivImage   = v.findViewById(R.id.ivCategoryImage);
            tvName    = v.findViewById(R.id.tvCategoryName);
        }
    }
}
