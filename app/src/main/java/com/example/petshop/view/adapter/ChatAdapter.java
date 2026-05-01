package com.example.petshop.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.ChatMessage;

import java.util.List;

public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.VH> {

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return oldItem.getTimestamp() == newItem.getTimestamp();
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return oldItem.getText().equals(newItem.getText()) && oldItem.getType() == newItem.getType();
            }
        });
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_msg, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage msg = getItem(position);
        if (msg.getType() == ChatMessage.TYPE_USER) {
            h.llUser.setVisibility(View.VISIBLE);
            h.llBot.setVisibility(View.GONE);
            h.tvUser.setText(msg.getText());
        } else {
            h.llUser.setVisibility(View.GONE);
            h.llBot.setVisibility(View.VISIBLE);
            h.tvBot.setText(msg.getText());
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout llUser, llBot;
        TextView tvUser, tvBot;
        VH(View v) {
            super(v);
            llUser = v.findViewById(R.id.llUserMsg);
            llBot  = v.findViewById(R.id.llBotMsg);
            tvUser = v.findViewById(R.id.tvUserMsg);
            tvBot  = v.findViewById(R.id.tvBotMsg);
        }
    }
}
