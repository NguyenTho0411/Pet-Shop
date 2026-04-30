package com.example.petshop.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Voucher;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class VoucherAdminAdapter extends RecyclerView.Adapter<VoucherAdminAdapter.VH> {

    public interface OnActionListener {
        void onEdit(Voucher voucher);
        void onDelete(Voucher voucher);
    }

    private final List<Voucher>        list;
    private final OnActionListener     listener;

    public VoucherAdminAdapter(List<Voucher> list, OnActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Voucher v = list.get(pos);
        h.tvCode.setText(v.getCode());
        h.tvName.setText(v.getName());
        
        String discountText = Voucher.TYPE_PERCENT.equals(v.getType()) 
            ? String.format("%d%%", (int)v.getDiscountValue())
            : Voucher.TYPE_FREESHIP.equals(v.getType())
            ? "Miễn phí ship"
            : String.format("₫%,d", (long)v.getDiscountValue());
        h.tvDiscount.setText(discountText);
        
        h.tvUsage.setText(String.format("Đã dùng: %d / %d", v.getUsedCount(), v.getUsageLimit()));
        int usagePercent = v.getUsageLimit() > 0 ? (v.getUsedCount() * 100) / v.getUsageLimit() : 0;
        h.tvUsagePercent.setText(String.format("%d%%", usagePercent));
        
        // Color usage by percentage
        int color = usagePercent < 50 ? 0xFF34C759 : usagePercent < 80 ? 0xFFFF9500 : 0xFFFF3B30;
        h.tvUsagePercent.setTextColor(color);
        
        h.tvDateRange.setText(String.format("📅 %s → %s", v.getStartDate(), v.getEndDate()));
        h.tvMinOrder.setText(v.getMinOrderAmount() > 0 ? String.format("Đơn tối thiểu: ₫%,d", (long)v.getMinOrderAmount()) : "Không có yêu cầu");
        
        h.switchActive.setChecked(v.isActive());
        h.switchActive.setOnCheckedChangeListener((btn, checked) ->
                listener.onEdit(v));

        h.btnMore.setOnClickListener(x -> showPopup(x.getContext(), x, v));
    }

    private void showPopup(android.content.Context ctx, View anchor, Voucher v) {
        PopupMenu popup = new PopupMenu(ctx, anchor);
        popup.getMenu().add(0, 1, 0, "✏️ Sửa");
        popup.getMenu().add(0, 2, 0, "📋 Copy code");
        popup.getMenu().add(0, 3, 0, "🗑 Xoá");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: listener.onEdit(v); break;
                case 2: copyToClipboard(ctx, v.getCode()); break;
                case 3: listener.onDelete(v); break;
            }
            return true;
        });
        popup.show();
    }

    private void copyToClipboard(android.content.Context ctx, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Voucher code", text);
        clipboard.setPrimaryClip(clip);
        android.widget.Toast.makeText(ctx, "Đã copy: " + text, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Voucher> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvName, tvDiscount, tvUsage, tvUsagePercent, tvDateRange, tvMinOrder;
        SwitchMaterial switchActive;
        ImageButton btnMore;

        VH(View v) {
            super(v);
            tvCode      = v.findViewById(R.id.tvCode);
            tvName      = v.findViewById(R.id.tvName);
            tvDiscount  = v.findViewById(R.id.tvDiscount);
            tvUsage     = v.findViewById(R.id.tvUsage);
            tvUsagePercent = v.findViewById(R.id.tvUsagePercent);
            tvDateRange = v.findViewById(R.id.tvDateRange);
            tvMinOrder  = v.findViewById(R.id.tvMinOrder);
            switchActive= v.findViewById(R.id.switchActive);
            btnMore     = v.findViewById(R.id.btnMore);
        }
    }
}
