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
import com.example.petshop.model.entity.Pet;

import java.util.List;

public class PetAdminAdapter extends RecyclerView.Adapter<PetAdminAdapter.VH> {

    public interface OnActionListener {
        void onEdit(Pet pet);
        void onDelete(Pet pet);
        void onChangeStatus(Pet pet, String status);
    }

    private final List<Pet>           list;
    private final OnActionListener    listener;

    public PetAdminAdapter(List<Pet> list, OnActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Pet p = list.get(pos);
        h.tvName.setText(p.getName());
        h.tvSpecies.setText(p.getSpecies() != null ? p.getSpecies() : "—");
        h.tvPrice.setText(String.format("₫%,d", (long)p.getPrice()));
        h.tvStatus.setText(p.getStatus());

        // Status color
        switch (p.getStatus()) {
            case "SOLD": h.tvStatus.setTextColor(0xFFFF3B30); break;
            case "RESERVED": h.tvStatus.setTextColor(0xFFFF9500); break;
            case "INACTIVE": h.tvStatus.setTextColor(0xFF999999); break;
            default: h.tvStatus.setTextColor(0xFF34C759); break;
        }

        // Load first image
        if (p.getThumbnailUrl() != null && !p.getThumbnailUrl().isEmpty()) {
            Glide.with(h.itemView).load(p.getThumbnailUrl()).centerCrop().into(h.ivThumbnail);
        }

        h.btnMore.setOnClickListener(v -> showPopup(v.getContext(), v, p));
        h.itemView.setOnClickListener(v -> listener.onEdit(p));
    }

    private void showPopup(android.content.Context ctx, View anchor, Pet p) {
        PopupMenu popup = new PopupMenu(ctx, anchor);
        popup.getMenu().add(0, 1, 0, "✏️ Sửa");
        popup.getMenu().add(0, 2, 0, p.getStatus().equals("SOLD") ? "✅ Hết hàng" : "📍 Đã bán");
        popup.getMenu().add(0, 3, 0, "🗑 Xoá");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: listener.onEdit(p); break;
                case 2: listener.onChangeStatus(p, p.getStatus().equals("SOLD") ? Pet.STATUS_AVAILABLE : Pet.STATUS_SOLD); break;
                case 3: listener.onDelete(p); break;
            }
            return true;
        });
        popup.show();
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Pet> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView    ivThumbnail;
        TextView     tvName, tvSpecies, tvPrice, tvStatus;
        ImageButton  btnMore;

        VH(View v) {
            super(v);
            ivThumbnail = v.findViewById(R.id.ivThumbnail);
            tvName      = v.findViewById(R.id.tvName);
            tvSpecies   = v.findViewById(R.id.tvSpecies);
            tvPrice     = v.findViewById(R.id.tvPrice);
            tvStatus    = v.findViewById(R.id.tvStatus);
            btnMore     = v.findViewById(R.id.btnMore);
        }
    }
}
