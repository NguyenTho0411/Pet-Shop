package com.example.petshop.view.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.petshop.R;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.PetMedia;
import com.example.petshop.repository.PetRepository;
import com.example.petshop.utils.FirebaseHelper;
import com.example.petshop.view.adapter.ImageSliderAdapter;
import com.example.petshop.viewmodel.CartViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PetDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PET_ID = "pet_id";

    private final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    // Views
    private ImageView ivHero;
    private ViewPager2 vpPetImages;
    private LinearLayout llDotIndicator;
    private ImageSliderAdapter sliderAdapter;
    private TextView  tvPetName, tvStatusBadge, tvSpeciesBreed, tvSaleBadge;
    private TextView  tvAttrGenderVal, tvAttrAgeVal, tvAttrBreedVal;
    private TextView  tvPrice, tvOriginalPrice, tvWeight, tvOrigin;
    private TextView  tvAboutTitle, tvDescription, tvCareGuide, tvDietInfo;
    private TextView  tvVaccine, tvDewormed, tvMicrochip, tvCertificate;

    private Pet     currentPet;
    private CartViewModel cartVm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        cartVm = new ViewModelProvider(this).get(CartViewModel.class);

        String petId = getIntent().getStringExtra(EXTRA_PET_ID);
        if (petId == null) { finish(); return; }

        bindViews();
        observeViewModel();
        loadPet(petId);
    }

    private void observeViewModel() {
        cartVm.getSuccess().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        cartVm.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    private void bindViews() {
        ivHero           = findViewById(R.id.ivPetHero);
        vpPetImages      = findViewById(R.id.vpPetImages);
        llDotIndicator   = findViewById(R.id.llDotIndicator);

        // Setup ViewPager2 slider
        sliderAdapter = new ImageSliderAdapter();
        vpPetImages.setAdapter(sliderAdapter);
        tvPetName        = findViewById(R.id.tvPetName);
        tvStatusBadge    = findViewById(R.id.tvStatusBadge);
        tvSpeciesBreed   = findViewById(R.id.tvSpeciesBreed);
        tvSaleBadge      = findViewById(R.id.tvSaleBadge);
        tvAttrGenderVal  = findViewById(R.id.tvAttrGenderVal);
        tvAttrAgeVal     = findViewById(R.id.tvAttrAgeVal);
        tvAttrBreedVal   = findViewById(R.id.tvAttrBreedVal);
        tvPrice          = findViewById(R.id.tvPrice);
        tvOriginalPrice  = findViewById(R.id.tvOriginalPrice);
        tvWeight         = findViewById(R.id.tvWeight);
        tvOrigin         = findViewById(R.id.tvOrigin);
        tvAboutTitle     = findViewById(R.id.tvAboutTitle);
        tvDescription    = findViewById(R.id.tvDescription);
        tvCareGuide      = findViewById(R.id.tvCareGuide);
        tvDietInfo       = findViewById(R.id.tvDietInfo);
        tvVaccine        = findViewById(R.id.tvVaccine);
        tvDewormed       = findViewById(R.id.tvDewormed);
        tvMicrochip      = findViewById(R.id.tvMicrochip);
        tvCertificate    = findViewById(R.id.tvCertificate);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnCall).setOnClickListener(v -> showCallDialog());

        findViewById(R.id.btnChat).setOnClickListener(v -> {
            if (FirebaseHelper.getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                startActivity(new Intent(this, ChatActivity.class));
            }
        });

        findViewById(R.id.btnBuy).setOnClickListener(v -> {
            if (FirebaseHelper.getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (currentPet != null) {
                cartVm.addPet(currentPet);
            }
        });
    }

    private void showCallDialog() {
        String phone = "0933623348";
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_call_bottom_sheet, null);
        
        view.findViewById(R.id.btnConfirmCall).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
            dialog.dismiss();
        });
        
        view.findViewById(R.id.btnCancelCall).setOnClickListener(v -> dialog.dismiss());
        
        dialog.setContentView(view);
        dialog.show();
    }

    private void loadPet(String petId) {
        new PetRepository().getById(petId, new PetRepository.Callback<>() {
            public void onSuccess(Pet pet) {
                if (pet == null) { finish(); return; }
                
                // Áp dụng khuyến mãi nếu có
                new com.example.petshop.repository.PromotionRepository().getActive(new com.example.petshop.repository.PromotionRepository.Callback<>() {
                    @Override
                    public void onSuccess(java.util.List<com.example.petshop.model.entity.Promotion> data) {
                        java.util.List<Pet> list = new java.util.ArrayList<>();
                        list.add(pet);
                        com.example.petshop.utils.PromotionManager.applyPromotions(list, null, data);
                        currentPet = pet;
                        runOnUiThread(() -> bindPet(pet));
                    }
                    @Override public void onFailure(String error) {
                        currentPet = pet;
                        runOnUiThread(() -> bindPet(pet));
                    }
                });
            }
            public void onFailure(String err) {
                runOnUiThread(() -> {
                    Toast.makeText(PetDetailActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void bindPet(Pet pet) {
        // Hero image (fallback thumbnail)
        if (pet.getThumbnailUrl() != null && !pet.getThumbnailUrl().isEmpty()) {
            Glide.with(this).load(pet.getThumbnailUrl()).centerCrop().into(ivHero);
        }

        // Load all media from subcollection for gallery
        loadMediaGallery(pet);

        // Name + About title
        tvPetName.setText("Pet Name:  " + pet.getName());
        tvAboutTitle.setText("About " + pet.getName());

        // Status badge
        switch (pet.getStatus() != null ? pet.getStatus() : "") {
            case Pet.STATUS_AVAILABLE: tvStatusBadge.setText("Còn hàng"); break;
            case Pet.STATUS_SOLD:      tvStatusBadge.setText("Đã bán"); break;
            case Pet.STATUS_RESERVED:  tvStatusBadge.setText("Đã đặt"); break;
            default:                   tvStatusBadge.setVisibility(View.GONE);
        }

        // Species · breed · age
        String ageStr = pet.getAge() > 0 ? pet.getAge() + ("YEAR".equals(pet.getAgeUnit()) ? " năm" : " tháng") : "";
        tvSpeciesBreed.setText(join(" · ", speciesVi(pet.getSpecies()), ageStr, genderVi(pet.getGender())));

        // Attribute chips
        tvAttrGenderVal.setText(genderVi(pet.getGender()));
        tvAttrAgeVal.setText(pet.getAge() > 0 ? pet.getAge() + ("YEAR".equals(pet.getAgeUnit()) ? " năm" : " tháng") : "—");
        tvAttrBreedVal.setText(pet.getBreed() != null && !pet.getBreed().isEmpty() ? pet.getBreed() : speciesVi(pet.getSpecies()));

        // Price
        double price = pet.getEffectivePrice();
        tvPrice.setText(price > 0 ? VND.format((long) price) + "đ" : "Liên hệ");

        if (pet.hasPromotion() && pet.getOriginalPrice() > 0 && pet.getOriginalPrice() > pet.getEffectivePrice()) {
            // Hiển thị giá gốc gạch ngang
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(VND.format((long) pet.getOriginalPrice()) + "đ");
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            
            // Hiển thị badge giảm giá
            if (tvSaleBadge != null) {
                int pct = (int) Math.round((1 - pet.getEffectivePrice() / pet.getOriginalPrice()) * 100);
                tvSaleBadge.setText(" GIẢM " + pct + "% ");
                tvSaleBadge.setVisibility(View.VISIBLE);
            }
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
            if (tvSaleBadge != null) tvSaleBadge.setVisibility(View.GONE);
        }

        // Weight & origin
        tvWeight.setText(pet.getWeight() > 0 ? pet.getWeight() + " kg" : "");
        tvOrigin.setText(pet.getOrigin() != null ? pet.getOrigin() : "");

        // About / description
        tvDescription.setText(pet.getDescription() != null ? pet.getDescription() : "");
        tvCareGuide.setText(pet.getCareGuide()   != null ? "🌿 " + pet.getCareGuide() : "");
        tvDietInfo.setText(pet.getDietInfo()     != null ? "🍖 " + pet.getDietInfo()  : "");

        // Health
        tvVaccine.setText(vaccineText(pet.getVaccineStatus()));
        tvDewormed.setText(pet.isDewormed()      ? "✅ Đã tẩy giun"     : "❌ Chưa tẩy giun");
        tvMicrochip.setText(pet.isMicrochipped() ? "📡 Có chip điện tử" : "❌ Chưa gắn chip");
        tvCertificate.setText(pet.isHasCertificate() ? "📄 Có giấy tờ" : "❌ Không có giấy");
    }

    private void toggleFavorite() {
        if (FirebaseHelper.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
    }

    private String genderVi(String g) {
        if (g == null) return "—";
        switch (g) { case "MALE": return "Đực"; case "FEMALE": return "Cái"; default: return "—"; }
    }

    private String speciesVi(String s) {
        if (s == null) return "—";
        switch (s) {
            case "DOG":    return "Chó";   case "CAT":    return "Mèo";
            case "FISH":   return "Cá";    case "BIRD":   return "Chim";
            case "RABBIT": return "Thỏ";   default:       return s;
        }
    }

    private String vaccineText(String v) {
        if (v == null) return "❓ Chưa rõ vaccine";
        switch (v) {
            case "FULL":    return "💉 Vaccine đầy đủ";
            case "PARTIAL": return "⚠️ Vaccine một phần";
            default:        return "❌ Chưa tiêm vaccine";
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

    private void loadMediaGallery(Pet pet) {
        new PetRepository().getMedia(pet.getId(), new PetRepository.Callback<List<PetMedia>>() {
            @Override
            public void onSuccess(List<PetMedia> data) {
                List<String> imageUrls = new ArrayList<>();
                if (data != null) {
                    for (PetMedia m : data) {
                        // Chỉ lấy ảnh, bỏ qua video
                        if (!PetMedia.TYPE_VIDEO.equals(m.getMediaType()) && m.getMediaUrl() != null) {
                            imageUrls.add(m.getMediaUrl());
                        }
                    }
                }

                // Nếu subcollection không có ảnh, dùng thumbnail làm ảnh duy nhất
                if (imageUrls.isEmpty() && pet.getThumbnailUrl() != null && !pet.getThumbnailUrl().isEmpty()) {
                    imageUrls.add(pet.getThumbnailUrl());
                }

                if (imageUrls.size() > 1) {
                    // Có nhiều ảnh → hiện ViewPager, ẩn ImageView đơn
                    runOnUiThread(() -> {
                        ivHero.setVisibility(View.GONE);
                        vpPetImages.setVisibility(View.VISIBLE);
                        sliderAdapter.setImages(imageUrls);
                        setupDotIndicator(imageUrls.size());
                    });
                } else if (imageUrls.size() == 1) {
                    // Chỉ 1 ảnh → dùng ImageView đơn cho đẹp
                    runOnUiThread(() -> {
                        vpPetImages.setVisibility(View.GONE);
                        ivHero.setVisibility(View.VISIBLE);
                        Glide.with(PetDetailActivity.this).load(imageUrls.get(0)).centerCrop().into(ivHero);
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                // Giữ nguyên thumbnail đã load ở trên
            }
        });
    }

    private void setupDotIndicator(int count) {
        llDotIndicator.removeAllViews();
        llDotIndicator.setVisibility(View.VISIBLE);

        View[] dots = new View[count];
        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (8 * density);
        int dotMargin = (int) (4 * density);

        for (int i = 0; i < count; i++) {
            dots[i] = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dotSize, dotSize);
            lp.setMargins(dotMargin, 0, dotMargin, 0);
            dots[i].setLayoutParams(lp);
            dots[i].setBackgroundResource(R.drawable.bg_circle_white);
            dots[i].setAlpha(i == 0 ? 1f : 0.4f);
            llDotIndicator.addView(dots[i]);
        }

        vpPetImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < count; i++) {
                    dots[i].setAlpha(i == position ? 1f : 0.4f);
                }
            }
        });
    }
}
