package com.example.petshop.view.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho việc chọn nhiều ảnh/video.
 * Luôn hiển thị 1 tile "+" ở cuối để thêm media mới.
 */
public class MediaPickerAdapter extends RecyclerView.Adapter<MediaPickerAdapter.VH> {

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_ADD   = 2;

    public interface OnMediaAction {
        void onAddClick();
        void onRemoveClick(int index);
    }

    public static class MediaItem {
        public Uri    uri;
        public String url;        // existing URL from Firestore/Storage
        public String mediaId;    // existing Firestore doc ID
        public int    type;       // TYPE_IMAGE | TYPE_VIDEO
        public boolean isExisting;

        public MediaItem(Uri uri, int type) {
            this.uri  = uri;
            this.type = type;
            this.isExisting = false;
        }
        public MediaItem(String url, String mediaId, int type) {
            this.url     = url;
            this.mediaId = mediaId;
            this.type    = type;
            this.isExisting = true;
        }
    }

    private final List<MediaItem>   items;
    private final OnMediaAction     listener;

    public MediaPickerAdapter(List<MediaItem> items, OnMediaAction listener) {
        this.items    = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_picker, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        if (pos == items.size()) {
            // "+" add tile
            h.ivPreview.setVisibility(View.GONE);
            h.btnRemove.setVisibility(View.GONE);
            h.ivVideoIcon.setVisibility(View.GONE);
            h.llAddNew.setVisibility(View.VISIBLE);
            h.llAddNew.setOnClickListener(v -> listener.onAddClick());
            return;
        }

        MediaItem item = items.get(pos);
        h.llAddNew.setVisibility(View.GONE);
        h.ivPreview.setVisibility(View.VISIBLE);
        h.btnRemove.setVisibility(View.VISIBLE);

        if (item.isExisting) {
            Glide.with(h.itemView).load(item.url).centerCrop().into(h.ivPreview);
        } else {
            Glide.with(h.itemView).load(item.uri).centerCrop().into(h.ivPreview);
        }

        h.ivVideoIcon.setVisibility(item.type == TYPE_VIDEO ? View.VISIBLE : View.GONE);
        h.btnRemove.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_ID) listener.onRemoveClick(p);
        });
    }

    @Override
    public int getItemCount() { return items.size() + 1; } // +1 for "+" button

    public List<MediaItem> getItems() { return items; }

    public void addItem(MediaItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int index) {
        if (index < items.size()) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }

    /** Returns only NEW (not existing) Uri items for upload */
    public List<Uri> getNewUris() {
        List<Uri> uris = new ArrayList<>();
        for (MediaItem m : items) if (!m.isExisting && m.uri != null) uris.add(m.uri);
        return uris;
    }

    public List<String> getNewTypes() {
        List<String> types = new ArrayList<>();
        for (MediaItem m : items) {
            if (!m.isExisting && m.uri != null)
                types.add(m.type == TYPE_VIDEO ? "VIDEO" : "IMAGE");
        }
        return types;
    }

    public List<MediaItem> getExistingItems() {
        List<MediaItem> existing = new ArrayList<>();
        for (MediaItem m : items) if (m.isExisting) existing.add(m);
        return existing;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView    ivPreview, ivVideoIcon;
        ImageButton  btnRemove;
        LinearLayout llAddNew;

        VH(View v) {
            super(v);
            ivPreview  = v.findViewById(R.id.ivPreview);
            ivVideoIcon= v.findViewById(R.id.ivVideoIcon);
            btnRemove  = v.findViewById(R.id.btnRemove);
            llAddNew   = v.findViewById(R.id.llAddNew);
        }
    }
}
