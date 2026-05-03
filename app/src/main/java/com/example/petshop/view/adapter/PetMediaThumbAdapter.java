package com.example.petshop.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.PetMedia;

import java.util.ArrayList;
import java.util.List;

public class PetMediaThumbAdapter extends RecyclerView.Adapter<PetMediaThumbAdapter.VH> {

    public interface OnMediaClick {
        void onClick(PetMedia media);
    }

    private final List<PetMedia> list = new ArrayList<>();
    private final OnMediaClick listener;
    private int selectedPos = 0;

    public PetMediaThumbAdapter(OnMediaClick listener) {
        this.listener = listener;
    }

    public void updateList(List<PetMedia> data) {
        list.clear();
        if (data != null) list.addAll(data);
        selectedPos = 0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet_media_thumb, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PetMedia media = list.get(position);

        Glide.with(h.itemView)
                .load(media.getMediaUrl())
                .centerCrop()
                .into(h.ivThumb);

        h.itemView.setBackgroundColor(position == selectedPos
                ? Color.parseColor("#F5A623")
                : Color.TRANSPARENT);

        h.itemView.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = h.getBindingAdapterPosition();

            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            if (selectedPos != RecyclerView.NO_POSITION) notifyItemChanged(selectedPos);

            if (listener != null) listener.onClick(media);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
        }
    }
}