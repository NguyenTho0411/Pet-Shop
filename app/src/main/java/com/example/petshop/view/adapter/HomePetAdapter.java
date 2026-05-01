package com.example.petshop.view.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Pet;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HomePetAdapter extends RecyclerView.Adapter<HomePetAdapter.VH> {

    public interface OnPetClick { void onClick(Pet pet); }
    public interface OnCartClick { void onAddToCart(Pet pet); }

    private final List<Pet>   list;
    private final OnPetClick  listener;
    private final OnCartClick cartListener;

    private static final NumberFormat VND =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    public HomePetAdapter(List<Pet> list, OnPetClick listener, OnCartClick cartListener) {
        this.list         = list;
        this.listener     = listener;
        this.cartListener = cartListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Pet pet = list.get(pos);

        // Pet name
        h.tvPetName.setText("Pet: " + pet.getName());

        // Price
        double price = pet.getEffectivePrice();
        h.tvPrice.setText(price > 0 ? VND.format((long) price) + "đ" : "Liên hệ");

        // Sale badge
        if (pet.hasPromotion() && pet.getOriginalPrice() > 0 && pet.getOriginalPrice() > pet.getEffectivePrice()) {
            h.tvSaleBadge.setVisibility(View.VISIBLE);
            int pct = (int) Math.round((1 - pet.getEffectivePrice() / pet.getOriginalPrice()) * 100);
            h.tvSaleBadge.setText("-" + pct + "%");
        } else {
            h.tvSaleBadge.setVisibility(View.GONE);
        }

        // Breed info line: "Species · AgeUnit · Gender"
        String ageStr = pet.getAge() > 0 ? pet.getAge() + " "
                + ("MONTH".equals(pet.getAgeUnit()) ? "tháng" : "năm") : "";
        String breedInfo = join(" · ",
                pet.getBreed()   != null && !pet.getBreed().isEmpty()   ? pet.getBreed() : "",
                ageStr,
                genderVi(pet.getGender()));
        h.tvBreedInfo.setText(breedInfo);

        // Tags — use species + vaccineStatus + health keywords
        h.tvTag1.setText(pet.getSpecies() != null ? speciesVi(pet.getSpecies()) : "—");
        h.tvTag2.setText(pet.isDewormed() ? "Đã tẩy giun" : "Khoẻ mạnh");
        h.tvTag3.setText(pet.isHasCertificate() ? "Có giấy tờ" : "Thuần chủng");

        // Image
        String thumb = pet.getThumbnailUrl();
        if (thumb != null && !thumb.isEmpty()) {
            Glide.with(h.itemView).load(thumb).centerCrop()
                    .placeholder(R.mipmap.ic_launcher).into(h.ivPet);
        } else {
            h.ivPet.setImageResource(R.mipmap.ic_launcher);
        }

        // Status badge & add to cart visibility
        String status = pet.getStatus() != null ? pet.getStatus() : "";
        if (Pet.STATUS_AVAILABLE.equals(status)) {
            h.tvStatusBadge.setVisibility(View.GONE);
            h.ivAddToCart.setVisibility(View.VISIBLE);
        } else {
            h.tvStatusBadge.setVisibility(View.VISIBLE);
            h.ivAddToCart.setVisibility(View.GONE);
            if (Pet.STATUS_SOLD.equals(status)) {
                h.tvStatusBadge.setText("ĐÃ BÁN");
                h.tvStatusBadge.getBackground().setTint(h.itemView.getContext().getColor(R.color.badge_red));
            } else if (Pet.STATUS_RESERVED.equals(status)) {
                h.tvStatusBadge.setText("ĐÃ ĐẶT");
                h.tvStatusBadge.getBackground().setTint(h.itemView.getContext().getColor(R.color.text_secondary));
            }
        }

        h.itemView.setOnClickListener(v -> listener.onClick(pet));
        h.ivAddToCart.setOnClickListener(v -> cartListener.onAddToCart(pet));
    }

    @Override public int getItemCount() { return list.size(); }

    public void updateList(List<Pet> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    private String genderVi(String g) {
        if (g == null) return "";
        switch (g) {
            case "MALE":   return "Đực";
            case "FEMALE": return "Cái";
            default:        return "";
        }
    }

    private String speciesVi(String s) {
        switch (s) {
            case "DOG":    return "Chó";
            case "CAT":    return "Mèo";
            case "FISH":   return "Cá";
            case "BIRD":   return "Chim";
            case "RABBIT": return "Thỏ";
            default:       return s;
        }
    }

    private String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isEmpty()) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPet, ivAddToCart;
        TextView  tvPetName, tvPrice, tvBreedInfo, tvTag1, tvTag2, tvTag3, tvDistance, tvStatusBadge, tvSaleBadge;

        VH(View v) {
            super(v);
            ivPet       = v.findViewById(R.id.ivPetImage);
            ivAddToCart = v.findViewById(R.id.ivAddToCart);
            tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
            tvSaleBadge = v.findViewById(R.id.tvSaleBadge);
            tvPetName   = v.findViewById(R.id.tvPetName);
            tvPrice     = v.findViewById(R.id.tvPrice);
            tvBreedInfo = v.findViewById(R.id.tvBreedInfo);
            tvTag1      = v.findViewById(R.id.tvTag1);
            tvTag2      = v.findViewById(R.id.tvTag2);
            tvTag3      = v.findViewById(R.id.tvTag3);
            tvDistance  = v.findViewById(R.id.tvDistance);
        }
    }
}
