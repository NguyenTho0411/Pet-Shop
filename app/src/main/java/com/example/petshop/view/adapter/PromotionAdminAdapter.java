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
import com.example.petshop.model.entity.Promotion;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class PromotionAdminAdapter extends RecyclerView.Adapter<PromotionAdminAdapter.VH> {

    public interface OnActionListener {
        void onEdit(Promotion promo);
        void onDelete(Promotion promo);
    }

    private final List<Promotion>      list;
    private final OnActionListener     listener;

    public PromotionAdminAdapter(List<Promotion> list, OnActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Promotion p = list.get(pos);
        h.tvName.setText(p.getName());
        h.tvDesc.setText(p.getDescription());
        
        String discountText = Promotion.TYPE_PERCENT.equals(p.getDiscountType()) 
            ? String.format("%d%%", (int)p.getDiscountValue())
            : String.format("₫%,d", (long)p.getDiscountValue());
        h.tvDiscount.setText("Giảm: " + discountText);
        
        h.tvApplyTo.setText("Áp dụng: " + formatApplyTo(p.getApplyTo()));
        h.tvDateRange.setText(String.format("📅 %s → %s", p.getStartDate(), p.getEndDate()));
        h.tvUsage.setText(String.format("Đã dùng: %d", p.getUsageCount()));
        
        h.switchActive.setChecked(p.isActive());
        h.switchActive.setOnCheckedChangeListener((btn, checked) ->
                listener.onEdit(p)); // Toggle via edit

        h.btnMore.setOnClickListener(v -> showPopup(v.getContext(), v, p));
    }

    private String formatApplyTo(String applyTo) {
        switch (applyTo) {
            case "PET": return "🐾 Thú cưng";
            case "FOOD": return "🍖 Thức ăn";
            case "SPECIFIC": return "📦 Sản phẩm cụ thể";
            default: return "Tất cả";
        }
    }

    private void showPopup(android.content.Context ctx, View anchor, Promotion p) {
        PopupMenu popup = new PopupMenu(ctx, anchor);
        popup.getMenu().add(0, 1, 0, "✏️ Sửa");
        popup.getMenu().add(0, 2, 0, "🗑 Xoá");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: listener.onEdit(p); break;
                case 2: listener.onDelete(p); break;
            }
            return true;
        });
        popup.show();
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Promotion> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvDiscount, tvApplyTo, tvDateRange, tvUsage;
        SwitchMaterial switchActive;
        ImageButton btnMore;

        VH(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvName);
            tvDesc      = v.findViewById(R.id.tvDesc);
            tvDiscount  = v.findViewById(R.id.tvDiscount);
            tvApplyTo   = v.findViewById(R.id.tvApplyTo);
            tvDateRange = v.findViewById(R.id.tvDateRange);
            tvUsage     = v.findViewById(R.id.tvUsage);
            switchActive= v.findViewById(R.id.switchActive);
            btnMore     = v.findViewById(R.id.btnMore);
        }
    }
}
