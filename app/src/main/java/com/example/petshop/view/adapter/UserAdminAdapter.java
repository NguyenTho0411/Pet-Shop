package com.example.petshop.view.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.VH> {

    public interface OnActionListener {
        void onBan(User user);
        void onUnban(User user);
        void onChangeRole(User user, String newRole);
        void onDelete(User user);
    }

    private final List<User>         list;
    private final OnActionListener   listener;

    public UserAdminAdapter(List<User> list, OnActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = list.get(pos);
        h.tvName.setText(u.getFullName() != null ? u.getFullName() : "—");
        h.tvEmail.setText(u.getEmail());
        h.tvRole.setText(u.getRole());
        h.tvStatus.setText(u.getStatus());

        // Role color
        h.tvRole.setBackgroundResource(
                User.ROLE_ADMIN.equals(u.getRole()) ? R.drawable.bg_orange_pill : R.drawable.bg_tag_role);

        // Status color
        int statusColor = User.STATUS_ACTIVE.equals(u.getStatus()) ? Color.parseColor("#34C759")
                : User.STATUS_BANNED.equals(u.getStatus()) ? Color.parseColor("#FF3B30")
                : Color.GRAY;
        h.tvStatus.getBackground().setTint(statusColor);

        if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) {
            Glide.with(h.itemView).load(u.getAvatarUrl()).into(h.ivAvatar);
        }

        h.btnMore.setOnClickListener(v -> showPopup(v.getContext(), v, u));
    }

    private void showPopup(Context ctx, View anchor, User u) {
        PopupMenu popup = new PopupMenu(ctx, anchor);
        popup.getMenu().add(0, 1, 0, "Đổi role → " + (User.ROLE_ADMIN.equals(u.getRole()) ? "CUSTOMER" : "ADMIN"));
        if (User.STATUS_BANNED.equals(u.getStatus())) {
            popup.getMenu().add(0, 2, 0, "✅ Mở khoá");
        } else {
            popup.getMenu().add(0, 3, 0, "🚫 Khoá tài khoản");
        }
        popup.getMenu().add(0, 4, 0, "🗑 Xoá");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: listener.onChangeRole(u, User.ROLE_ADMIN.equals(u.getRole()) ? User.ROLE_CUSTOMER : User.ROLE_ADMIN); break;
                case 2: listener.onUnban(u); break;
                case 3: listener.onBan(u); break;
                case 4: listener.onDelete(u); break;
            }
            return true;
        });
        popup.show();
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<User> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        de.hdodenhof.circleimageview.CircleImageView ivAvatar;
        TextView tvName, tvEmail, tvRole, tvStatus;
        ImageButton btnMore;

        VH(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            tvName   = v.findViewById(R.id.tvName);
            tvEmail  = v.findViewById(R.id.tvEmail);
            tvRole   = v.findViewById(R.id.tvRole);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnMore  = v.findViewById(R.id.btnMore);
        }
    }
}
