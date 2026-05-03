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

    private final List<Category> list;
    private final OnCategoryClick listener;

    // KHÔNG chọn sẵn item nào khi mới vào
    private int selectedPos = RecyclerView.NO_POSITION;

    public HomeCategoryAdapter(List<Category> list, OnCategoryClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
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

        // Chỉ item đang chọn mới có viền vàng
        if (pos == selectedPos) {
            h.vCircleBg.setBackgroundResource(R.drawable.bg_circle_orange);
        } else {
            h.vCircleBg.setBackgroundResource(R.drawable.bg_circle_white);
        }

        if (cat.getImageUrl() != null && !cat.getImageUrl().isEmpty()) {
            Glide.with(ctx)
                    .load(cat.getImageUrl())
                    .circleCrop()
                    .into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.mipmap.ic_launcher);
        }

        h.itemView.setOnClickListener(v -> {
            int newPos = h.getBindingAdapterPosition();
            if (newPos == RecyclerView.NO_POSITION) return;

            int oldPos = selectedPos;
            selectedPos = newPos;

            if (oldPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPos);

            if (listener != null) {
                listener.onClick(cat, selectedPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateList(List<Category> newList) {
        list.clear();
        list.addAll(newList);

        // load lại list thì không chọn sẵn item nào
        selectedPos = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    // Nếu sau này bạn muốn bỏ chọn tất cả
    public void clearSelection() {
        int oldPos = selectedPos;
        selectedPos = RecyclerView.NO_POSITION;
        if (oldPos != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPos);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        View vCircleBg;
        ImageView ivImage;
        TextView tvName;

        VH(View v) {
            super(v);
            vCircleBg = v.findViewById(R.id.vCircleBg);
            ivImage = v.findViewById(R.id.ivCategoryImage);
            tvName = v.findViewById(R.id.tvCategoryName);
        }
    }
}