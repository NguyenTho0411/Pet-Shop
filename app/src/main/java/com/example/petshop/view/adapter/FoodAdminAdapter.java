package com.example.petshop.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Food;

import java.util.List;

public class FoodAdminAdapter extends RecyclerView.Adapter<FoodAdminAdapter.VH> {

    public interface OnActionListener {
        void onEdit(Food food);
        void onDelete(Food food);
        void onChangeStock(Food food);
    }

    private final List<Food>         list;
    private final OnActionListener   listener;

    public FoodAdminAdapter(List<Food> list, OnActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Food f = list.get(pos);
        h.tvName.setText(f.getName());
        h.tvType.setText(f.getFoodType() != null ? f.getFoodType() : "—");
        h.tvPrice.setText(String.format("₫%,d", (long)f.getPrice()));
        h.tvStock.setText(String.format("Kho: %d", f.getStock()));

        // Stock status color
        int stockColor = f.getStock() > 10 ? 0xFF34C759 
                       : f.getStock() > 0  ? 0xFFFF9500 
                       : 0xFFFF3B30;
        h.tvStock.setTextColor(stockColor);

        if (f.getThumbnailUrl() != null && !f.getThumbnailUrl().isEmpty()) {
            Glide.with(h.itemView).load(f.getThumbnailUrl()).centerCrop().into(h.ivThumbnail);
        }

        h.btnMore.setOnClickListener(v -> showPopup(v.getContext(), v, f));
        h.itemView.setOnClickListener(v -> listener.onEdit(f));
    }

    private void showPopup(android.content.Context ctx, View anchor, Food f) {
        PopupMenu popup = new PopupMenu(ctx, anchor);
        popup.getMenu().add(0, 1, 0, "✏️ Sửa");
        popup.getMenu().add(0, 2, 0, "📦 Cập nhật kho");
        popup.getMenu().add(0, 3, 0, "🗑 Xoá");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: listener.onEdit(f); break;
                case 2: listener.onChangeStock(f); break;
                case 3: listener.onDelete(f); break;
            }
            return true;
        });
        popup.show();
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Food> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView   ivThumbnail;
        TextView    tvName, tvType, tvPrice, tvStock;
        ImageButton btnMore;

        VH(View v) {
            super(v);
            ivThumbnail = v.findViewById(R.id.ivThumbnail);
            tvName      = v.findViewById(R.id.tvName);
            tvType      = v.findViewById(R.id.tvType);
            tvPrice     = v.findViewById(R.id.tvPrice);
            tvStock     = v.findViewById(R.id.tvStock);
            btnMore     = v.findViewById(R.id.btnMore);
        }
    }
}
