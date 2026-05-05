package com.example.petshop.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.ChatSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatSessionAdapter extends ListAdapter<ChatSession, ChatSessionAdapter.SessionViewHolder> {

    private final OnSessionClickListener listener;
    private String selectedSessionId = "";

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
    }

    public ChatSessionAdapter(OnSessionClickListener listener) {
        super(new DiffUtil.ItemCallback<ChatSession>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatSession oldItem, @NonNull ChatSession newItem) {
                if (oldItem.getId() == null || newItem.getId() == null) return false;
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatSession oldItem, @NonNull ChatSession newItem) {
                String oldTitle = oldItem.getTitle() != null ? oldItem.getTitle() : "";
                String newTitle = newItem.getTitle() != null ? newItem.getTitle() : "";
                return oldTitle.equals(newTitle) &&
                        oldItem.getLastTimestamp() == newItem.getLastTimestamp();
            }
        });
        this.listener = listener;
    }

    public void setSelectedSessionId(String id) {
        this.selectedSessionId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = getItem(position);
        boolean isSelected = session.getId() != null && session.getId().equals(selectedSessionId);
        holder.bind(session, listener, isSelected);
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvTime;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvTime  = itemView.findViewById(R.id.tvSessionTime);
        }

        public void bind(ChatSession session, OnSessionClickListener listener, boolean isSelected) {
            tvTitle.setText(session.getTitle());
            tvTime.setText(sdf.format(new Date(session.getLastTimestamp())));
            
            itemView.setBackgroundColor(isSelected ? 0xFFF0F0F0 : 0x00000000);
            itemView.setOnClickListener(v -> listener.onSessionClick(session));
        }
    }
}
