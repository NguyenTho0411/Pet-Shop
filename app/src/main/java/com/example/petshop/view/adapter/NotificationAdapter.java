package com.example.petshop.view.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> list = new ArrayList<>();

    public NotificationAdapter(List<Notification> list) {
        this.list = list;
    }

    public void updateData(List<Notification> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvTitle, tvMessage, tvTime;
        private final View unreadIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        void bind(Notification n) {
            tvTitle.setText(n.getTitle() != null ? n.getTitle() : "");
            tvMessage.setText(n.getMessage() != null ? n.getMessage() : "");
            tvTime.setText(formatTime(n.getCreatedAt()));

            // Set icon based on type
            int iconRes;
            if ("ORDER".equals(n.getType())) {
                iconRes = R.drawable.ic_orders;
            } else if ("PROMO".equals(n.getType())) {
                iconRes = R.drawable.ic_promo;
            } else {
                iconRes = R.drawable.ic_notification;
            }
            ivIcon.setImageResource(iconRes);

            // Unread indicator
            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);
            }

            // Title bold if unread
            if (n.isRead()) {
                tvTitle.setTypeface(null, Typeface.NORMAL);
            } else {
                tvTitle.setTypeface(null, Typeface.BOLD);
            }

            // Background tint
            if (!n.isRead()) {
                itemView.setBackgroundColor(itemView.getContext().getResources()
                        .getColor(R.color.bg_card, null));
            } else {
                itemView.setBackgroundColor(itemView.getContext().getResources()
                        .getColor(android.R.color.transparent, null));
            }
        }

        private String formatTime(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) return "";
            try {
                // Handle Firestore Timestamp
                if (timestamp.contains("Timestamp")) {
                    long seconds = Long.parseLong(timestamp.replaceAll("[^0-9]", "").substring(0, 10));
                    Date d = new Date(seconds * 1000);
                    return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
                }
                // Handle string date
                Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .parse(timestamp.replace("T", " ").split("\\.")[0]);
                if (d != null) {
                    return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
                }
            } catch (Exception e) {
                // Try alternative format
                try {
                    Date d = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .parse(timestamp);
                    if (d != null) return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
                } catch (Exception ignored) {}
            }
            return timestamp;
        }
    }
}
