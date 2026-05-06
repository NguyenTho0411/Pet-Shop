package com.example.petshop.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Promotion;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.VH> {

    private List<Promotion> list = new ArrayList<>();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    public PromotionAdapter(List<Promotion> list) {
        this.list = list;
    }

    public void updateData(List<Promotion> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class VH extends RecyclerView.ViewHolder {
        private final TextView tvName, tvDescription, tvDiscount, tvApplyFor, tvValidDate, tvBadge, tvVoucherCode;
        private final View btnCopy;
        private final View layoutApplyFor, layoutVoucherCode;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvApplyFor = itemView.findViewById(R.id.tvApplyFor);
            tvValidDate = itemView.findViewById(R.id.tvValidDate);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvVoucherCode = itemView.findViewById(R.id.tvVoucherCode);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            layoutApplyFor = itemView.findViewById(R.id.layoutApplyFor);
            layoutVoucherCode = itemView.findViewById(R.id.layoutVoucherCode);
        }

        void bind(Promotion p) {
            tvName.setText(p.getName() != null ? p.getName() : "");
            tvDescription.setText(p.getDescription() != null ? p.getDescription() : "");

            // Hiển thị voucher code nếu là VOUCHER
            if (p.isVoucher() && p.getVoucherCode() != null && !p.getVoucherCode().isEmpty()) {
                layoutVoucherCode.setVisibility(View.VISIBLE);
                tvVoucherCode.setText("Mã: " + p.getVoucherCode());
                btnCopy.setVisibility(View.VISIBLE);
                btnCopy.setOnClickListener(v -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) v.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Voucher Code", p.getVoucherCode());
                    if (cm != null) cm.setPrimaryClip(clip);
                    android.widget.Toast.makeText(v.getContext(), "Đã copy mã voucher!", android.widget.Toast.LENGTH_SHORT).show();
                });
                // Ẩn phần "Áp dụng cho" khi là VOUCHER
                layoutApplyFor.setVisibility(View.GONE);
            } else {
                layoutVoucherCode.setVisibility(View.GONE);
                // Hiện phần "Áp dụng cho" khi là AUTOMATIC
                layoutApplyFor.setVisibility(View.VISIBLE);
                tvApplyFor.setText(getApplyText(p));
            }

            // Discount display
            if (p.isPercentType()) {
                tvDiscount.setText("-" + (int) p.getDiscountValue() + "%");
                if (p.getMaxDiscountAmount() > 0) {
                    tvBadge.setText("Giảm tối đa " + VND.format((long) p.getMaxDiscountAmount()));
                    tvBadge.setVisibility(View.VISIBLE);
                } else {
                    tvBadge.setVisibility(View.GONE);
                }
            } else {
                tvDiscount.setText("-" + VND.format((long) p.getDiscountValue()));
                tvBadge.setVisibility(View.GONE);
            }

            // Valid date
            String validDate = "";
            if (p.getEndDate() != null && !p.getEndDate().isEmpty()) {
                validDate = "Hết hạn: " + formatDate(p.getEndDate());
            } else {
                validDate = "Không giới hạn";
            }
            if (p.getStartDate() != null && !p.getStartDate().isEmpty()) {
                validDate = "Từ " + formatDate(p.getStartDate()) + " - " + (p.getEndDate() != null ? formatDate(p.getEndDate()) : "...");
            }
            tvValidDate.setText(validDate);
        }

        private String getApplyText(Promotion p) {
            String applyType = p.getApplyType();
            if (applyType == null) return "Tất cả sản phẩm";

            switch (applyType) {
                case Promotion.APPLY_ALL:
                    return "Áp dụng cho tất cả sản phẩm";
                case Promotion.APPLY_CATEGORY:
                    if (Promotion.CATEGORY_PET.equals(p.getApplyCategory())) {
                        return "Chỉ áp dụng cho thú cưng";
                    } else if (Promotion.CATEGORY_FOOD.equals(p.getApplyCategory())) {
                        return "Chỉ áp dụng cho thức ăn";
                    }
                    return "Áp dụng theo danh mục";
                case Promotion.APPLY_SPECIES:
                    if (p.getApplySpecies() != null && !p.getApplySpecies().isEmpty()) {
                        return "Áp dụng cho: " + speciesToVietnamese(p.getApplySpecies());
                    }
                    return "Áp dụng theo giống";
                case Promotion.APPLY_PRODUCT:
                    return "Áp dụng cho sản phẩm cụ thể";
                default:
                    return "Tất cả sản phẩm";
            }
        }

        private String speciesToVietnamese(List<String> species) {
            if (species == null) return "";
            StringBuilder sb = new StringBuilder();
            for (String s : species) {
                if (sb.length() > 0) sb.append(", ");
                switch (s) {
                    case "DOG": sb.append("Chó"); break;
                    case "CAT": sb.append("Mèo"); break;
                    case "FISH": sb.append("Cá"); break;
                    case "BIRD": sb.append("Chim"); break;
                    case "RABBIT": sb.append("Thỏ"); break;
                    case "HAMSTER": sb.append("Hamster"); break;
                    default: sb.append(s); break;
                }
            }
            return sb.toString();
        }

        private String formatDate(String date) {
            if (date == null || date.isEmpty()) return "";
            try {
                java.text.SimpleDateFormat from = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.text.SimpleDateFormat to = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                java.util.Date d = from.parse(date);
                return d != null ? to.format(d) : date;
            } catch (Exception e) {
                return date;
            }
        }
    }
}
