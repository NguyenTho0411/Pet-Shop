package com.example.petshop.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.VH> {

    public interface OnCartAction {
        void onRemove(CartItem item);
        void onQtyChange(CartItem item, int newQty);
    }

    private final List<CartItem> list;
    private final OnCartAction   listener;
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    public CartItemAdapter(List<CartItem> list, OnCartAction listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CartItem item = list.get(pos);
        h.tvName.setText(item.getProductName());
        h.tvPrice.setText(VND.format((long) item.getUnitPrice()) + "đ");
        h.tvBadge.setText(item.isPet() ? "THÚ CƯNG" : "ĐỒ ĂN");

        String detail = item.isPet()
                ? "Số lượng: 1 (duy nhất)"
                : "Đơn giá: " + VND.format((long) item.getUnitPrice()) + "đ";
        h.tvDetail.setText(detail);

        if (item.getProductThumbnail() != null) {
            Glide.with(h.itemView).load(item.getProductThumbnail())
                    .centerCrop().placeholder(R.mipmap.ic_launcher).into(h.ivProduct);
        }

        // Quantity control: only for FOOD
        if (item.isFood()) {
            h.llQty.setVisibility(View.VISIBLE);
            h.tvQty.setText(String.valueOf(item.getQuantity()));
            h.btnDec.setOnClickListener(v -> {
                int newQty = item.getQuantity() - 1;
                if (newQty <= 0) listener.onRemove(item);
                else listener.onQtyChange(item, newQty);
            });
            h.btnInc.setOnClickListener(v ->
                    listener.onQtyChange(item, item.getQuantity() + 1));
        } else {
            h.llQty.setVisibility(View.GONE);
        }

        h.btnRemove.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<CartItem> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView   ivProduct;
        TextView    tvName, tvDetail, tvPrice, tvBadge, tvQty, btnDec, btnInc;
        ImageButton btnRemove;
        LinearLayout llQty;

        VH(View v) {
            super(v);
            ivProduct = v.findViewById(R.id.ivProduct);
            tvName    = v.findViewById(R.id.tvProductName);
            tvDetail  = v.findViewById(R.id.tvProductDetail);
            tvPrice   = v.findViewById(R.id.tvUnitPrice);
            tvBadge   = v.findViewById(R.id.tvTypeBadge);
            tvQty     = v.findViewById(R.id.tvQty);
            btnDec    = v.findViewById(R.id.btnDec);
            btnInc    = v.findViewById(R.id.btnInc);
            llQty     = v.findViewById(R.id.llQtyControl);
            btnRemove = v.findViewById(R.id.btnRemove);
        }
    }
}
