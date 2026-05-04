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
        void onToggle(Promotion promo, boolean isActive);
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

        // Hiển thị badge loại VOUCHER / AUTOMATIC
        if (p.isVoucher()) {
            h.tvType.setText("🎫 VOUCHER");
            h.tvType.setBackgroundResource(R.drawable.bg_discount_badge);
        } else {
            h.tvType.setText("⚡ TỰ ĐỘNG");
            h.tvType.setBackgroundResource(R.drawable.bg_orange_pill);
        }

        h.tvApplyTo.setText("Áp dụng: " + formatApplyTo(p));
        h.tvDateRange.setText(String.format("📅 %s → %s", 
                p.getStartDate() != null ? p.getStartDate() : "N/A", 
                p.getEndDate() != null ? p.getEndDate() : "N/A"));
        h.tvUsage.setText(String.format("Đã dùng: %d", p.getUsageCount()));
        
        h.switchActive.setOnCheckedChangeListener(null); // clear trước để tránh fire khi setChecked
        h.switchActive.setChecked(p.isActive());
        h.switchActive.setOnCheckedChangeListener((btn, checked) -> {
            if (btn.isPressed()) listener.onToggle(p, checked);
        });

        h.btnMore.setOnClickListener(v -> showPopup(v.getContext(), v, p));
    }

    private String formatApplyTo(Promotion p) {
        String applyType = p.getApplyType();
        
        if (applyType == null || Promotion.APPLY_ALL.equals(applyType)) {
            return "Tất cả sản phẩm";
        }
        
        if (Promotion.APPLY_CATEGORY.equals(applyType)) {
            String cat = p.getApplyCategory();
            if (Promotion.CATEGORY_PET.equals(cat)) return "🐾 Thú cưng";
            if (Promotion.CATEGORY_FOOD.equals(cat)) return "🍖 Thức ăn";
            return "📂 " + cat;
        }
        
        if (Promotion.APPLY_SPECIES.equals(applyType)) {
            List<String> species = p.getApplySpecies();
            if (species == null || species.isEmpty()) return "🐾 Các giống";
            
            StringBuilder sb = new StringBuilder();
            for (String s : species) {
                switch (s) {
                    case Promotion.SPECIES_DOG: sb.append("🐕 Chó "); break;
                    case Promotion.SPECIES_CAT: sb.append("🐈 Mèo "); break;
                    case Promotion.SPECIES_FISH: sb.append("🐟 Cá "); break;
                    case Promotion.SPECIES_BIRD: sb.append("🐦 Chim "); break;
                    case Promotion.SPECIES_RABBIT: sb.append("🐰 Thỏ "); break;
                    case Promotion.SPECIES_HAMSTER: sb.append("🐹 Hamster "); break;
                    default: sb.append(s).append(" ");
                }
            }
            return sb.toString().trim();
        }
        
        if (Promotion.APPLY_PRODUCT.equals(applyType)) {
            List<String> ids = p.getProductIds();
            if (ids == null || ids.isEmpty()) return "📦 Sản phẩm cụ thể";
            return "📦 " + ids.size() + " sản phẩm";
        }
        
        return "Tất cả";
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
        TextView tvName, tvDesc, tvDiscount, tvType, tvApplyTo, tvDateRange, tvUsage;
        SwitchMaterial switchActive;
        ImageButton btnMore;

        VH(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvName);
            tvDesc      = v.findViewById(R.id.tvDesc);
            tvDiscount  = v.findViewById(R.id.tvDiscount);
            tvType      = v.findViewById(R.id.tvType);
            tvApplyTo   = v.findViewById(R.id.tvApplyTo);
            tvDateRange = v.findViewById(R.id.tvDateRange);
            tvUsage     = v.findViewById(R.id.tvUsage);
            switchActive= v.findViewById(R.id.switchActive);
            btnMore     = v.findViewById(R.id.btnMore);
        }
    }
}
