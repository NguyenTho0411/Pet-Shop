package com.example.petshop.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnNotificationClick {
        void onClick(AppNotification notification);
    }

    private final List<AppNotification> list = new ArrayList<>();
    private final OnNotificationClick listener;

    public NotificationAdapter(OnNotificationClick listener) {
        this.listener = listener;
    }

    public void updateList(List<AppNotification> data) {
        list.clear();
        if (data != null) list.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppNotification n = list.get(position);

        h.tvTitle.setText(n.getTitle());
        h.tvMessage.setText(n.getMessage());
        h.tvTime.setText(n.getCreatedAt() != null ? n.getCreatedAt() : "");

        if (AppNotification.TYPE_ORDER.equals(n.getType())) {
            h.tvIcon.setText("📦");
        } else if (AppNotification.TYPE_VOUCHER.equals(n.getType())) {
            h.tvIcon.setText("🏷️");
        } else {
            h.tvIcon.setText("🔔");
        }

        h.vUnread.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

        if (n.isRead()) {
            h.llRoot.setBackgroundColor(Color.WHITE);
        } else {
            h.llRoot.setBackgroundColor(Color.parseColor("#FFF3D4"));
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(n);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout llRoot;
        TextView tvIcon, tvTitle, tvMessage, tvTime;
        View vUnread;

        VH(@NonNull View itemView) {
            super(itemView);
            llRoot = itemView.findViewById(R.id.llRoot);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            vUnread = itemView.findViewById(R.id.vUnread);
        }
    }
}