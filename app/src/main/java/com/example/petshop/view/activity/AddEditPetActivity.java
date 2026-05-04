package com.example.petshop.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petshop.R;
import com.example.petshop.model.entity.Category;
import com.example.petshop.model.entity.Pet;
import com.example.petshop.model.entity.PetMedia;
import com.example.petshop.repository.CategoryRepository;
import com.example.petshop.view.adapter.MediaPickerAdapter;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;
import com.example.petshop.viewmodel.PetManageViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddEditPetActivity extends AppCompatActivity {

    public static final String EXTRA_PET_ID = "pet_id";

    private PetManageViewModel vm;
    private MediaPickerAdapter mediaAdapter;
    private List<Category>     categories  = new ArrayList<>();
    private String             selectedCategoryId;
    private String             editingPetId;

    // Views
    private TextInputEditText etName, etBreed, etAge, etWeight, etColor,
            etOrigin, etDesc, etCareGuide, etDietInfo, etHealthInfo, etPrice, etOriginalPrice;
    private AutoCompleteTextView actvCategory, actvSpecies, actvAgeUnit,
            actvGender, actvVaccine, actvStatus;
    private MaterialCheckBox cbDewormed, cbMicrochipped, cbCertificate;
    private ProgressBar progressBar;

    // Image/video picker
    private final ActivityResultLauncher<String[]> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null) return;
                for (Uri uri : uris) {
                    String mime = getContentResolver().getType(uri);
                    boolean isVideo = mime != null && mime.startsWith("video/");
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    mediaAdapter.addItem(new MediaPickerAdapter.MediaItem(
                            uri, isVideo ? MediaPickerAdapter.TYPE_VIDEO : MediaPickerAdapter.TYPE_IMAGE));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_pet);

        vm = new ViewModelProvider(this).get(PetManageViewModel.class);
        editingPetId = getIntent().getStringExtra(EXTRA_PET_ID);

        initViews();
        setupDropdowns();
        loadCategories();
        observeViewModel();

        if (editingPetId != null) {
            ((TextView) findViewById(R.id.tvTitle)).setText("Sửa thú cưng");
            vm.loadById(editingPetId);
        }
    }

    private void initViews() {
        etName          = findViewById(R.id.etName);
        etBreed         = findViewById(R.id.etBreed);
        etAge           = findViewById(R.id.etAge);
        etWeight        = findViewById(R.id.etWeight);
        etColor         = findViewById(R.id.etColor);
        etOrigin        = findViewById(R.id.etOrigin);
        etDesc          = findViewById(R.id.etDesc);
        etCareGuide     = findViewById(R.id.etCareGuide);
        etDietInfo      = findViewById(R.id.etDietInfo);
        etHealthInfo    = findViewById(R.id.etHealthInfo);
        etPrice         = findViewById(R.id.etPrice);
        etOriginalPrice = findViewById(R.id.etOriginalPrice);
        actvCategory    = findViewById(R.id.actvCategory);
        actvSpecies     = findViewById(R.id.actvSpecies);
        actvAgeUnit     = findViewById(R.id.actvAgeUnit);
        actvGender      = findViewById(R.id.actvGender);
        actvVaccine     = findViewById(R.id.actvVaccine);
        actvStatus      = findViewById(R.id.actvStatus);
        cbDewormed      = findViewById(R.id.cbDewormed);
        cbMicrochipped  = findViewById(R.id.cbMicrochipped);
        cbCertificate   = findViewById(R.id.cbCertificate);
        progressBar     = findViewById(R.id.progressBar);

        // Media RecyclerView
        RecyclerView rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaAdapter = new MediaPickerAdapter(new ArrayList<>(), new MediaPickerAdapter.OnMediaAction() {
            public void onAddClick() {
                mediaPickerLauncher.launch(new String[]{"image/*", "video/*"});
            }
            public void onRemoveClick(int index) {
                MediaPickerAdapter.MediaItem item = mediaAdapter.getItems().get(index);
                if (item.isExisting && editingPetId != null) {
                    confirmDeleteMedia(index, item);
                } else {
                    mediaAdapter.removeItem(index);
                }
            }
        });
        rvMedia.setAdapter(mediaAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Nút Lưu ở toolbar (top)
        View btnSaveTop = findViewById(R.id.btnSave);
        if (btnSaveTop != null) btnSaveTop.setOnClickListener(v -> savePet());
        // Nút Lưu ở bottom (luôn hiển thị dù cuộn)
        View btnSaveBottom = findViewById(R.id.btnSaveBottom);
        if (btnSaveBottom != null) btnSaveBottom.setOnClickListener(v -> savePet());
    }

    private void setupDropdowns() {
        setAdapter(actvAgeUnit, new String[]{"MONTH", "YEAR"});
        setAdapter(actvGender, new String[]{"MALE", "FEMALE", "UNKNOWN"});
        setAdapter(actvVaccine, new String[]{"FULL", "PARTIAL", "NONE"});
        setAdapter(actvStatus, new String[]{Pet.STATUS_AVAILABLE, Pet.STATUS_SOLD, Pet.STATUS_RESERVED, Pet.STATUS_INACTIVE});
        setAdapter(actvSpecies, new String[]{"DOG", "CAT", "FISH", "BIRD", "RABBIT", "HAMSTER", "REPTILE", "OTHER"});

        actvAgeUnit.setText("MONTH", false);
        actvGender.setText("MALE", false);
        actvVaccine.setText("NONE", false);
        actvStatus.setText(Pet.STATUS_AVAILABLE, false);
    }

    private void loadCategories() {
        new CategoryRepository().getByType(Category.TYPE_PET, new CategoryRepository.Callback<>() {
            public void onSuccess(List<Category> data) {
                categories = data;
                String[] names = new String[data.size()];
                for (int i = 0; i < data.size(); i++) names[i] = data.get(i).getName();
                runOnUiThread(() -> {
                    setAdapter(actvCategory, names);
                    if (editingPetId != null && vm.getCurrentPet().getValue() != null) {
                        fillCategoryField(vm.getCurrentPet().getValue().getCategoryId());
                    }
                });
            }
            public void onFailure(String err) {}
        });
    }

    private void fillCategoryField(String catId) {
        for (Category c : categories) {
            if (c.getId().equals(catId)) {
                actvCategory.setText(c.getName(), false);
                selectedCategoryId = catId;
                break;
            }
        }
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getUploadProgress().observe(this, prog ->
                progressBar.setProgress(prog));

        vm.getCurrentPet().observe(this, pet -> {
            if (pet != null) fillForm(pet);
        });

        vm.getMediaList().observe(this, medias -> {
            if (medias == null) return;
            // Hiển thị existing media từ Firestore
            List<MediaPickerAdapter.MediaItem> items = new ArrayList<>();
            for (PetMedia m : medias) {
                items.add(new MediaPickerAdapter.MediaItem(
                        m.getMediaUrl(), m.getId(),
                        PetMedia.TYPE_VIDEO.equals(m.getMediaType())
                                ? MediaPickerAdapter.TYPE_VIDEO : MediaPickerAdapter.TYPE_IMAGE));
            }
            // Giữ lại các new (chưa upload) items
            List<MediaPickerAdapter.MediaItem> current = new ArrayList<>(mediaAdapter.getItems());
            current.removeIf(i -> i.isExisting);
            items.addAll(current);
            mediaAdapter.getItems().clear();
            mediaAdapter.getItems().addAll(items);
            mediaAdapter.notifyDataSetChanged();
        });

        vm.getIsSaved().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Toast.makeText(this, "Đã lưu thành công", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        vm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    private void fillForm(Pet pet) {
        etName.setText(pet.getName());
        etBreed.setText(pet.getBreed());
        etAge.setText(String.valueOf(pet.getAge()));
        etWeight.setText(pet.getWeight() > 0 ? String.valueOf(pet.getWeight()) : "");
        etColor.setText(pet.getColor());
        etOrigin.setText(pet.getOrigin());
        etDesc.setText(pet.getDescription());
        etCareGuide.setText(pet.getCareGuide());
        etDietInfo.setText(pet.getDietInfo());
        etHealthInfo.setText(pet.getHealthInfo());
        etPrice.setText(pet.getPrice() > 0 ? String.valueOf(pet.getPrice()) : "");
        etOriginalPrice.setText(pet.getOriginalPrice() > 0 ? String.valueOf(pet.getOriginalPrice()) : "");
        if (pet.getSpecies() != null)       actvSpecies.setText(pet.getSpecies(), false);
        if (pet.getAgeUnit() != null)       actvAgeUnit.setText(pet.getAgeUnit(), false);
        if (pet.getGender() != null)        actvGender.setText(pet.getGender(), false);
        if (pet.getVaccineStatus() != null) actvVaccine.setText(pet.getVaccineStatus(), false);
        if (pet.getStatus() != null)        actvStatus.setText(pet.getStatus(), false);
        cbDewormed.setChecked(pet.isDewormed());
        cbMicrochipped.setChecked(pet.isMicrochipped());
        cbCertificate.setChecked(pet.isHasCertificate());
        fillCategoryField(pet.getCategoryId());
    }

    private void savePet() {
        String name = getText(etName);
        if (TextUtils.isEmpty(name)) { etName.setError("Bắt buộc"); return; }
        if (TextUtils.isEmpty(getText(etAge))) { etAge.setError("Bắt buộc"); return; }
        if (TextUtils.isEmpty(getText(etPrice))) { etPrice.setError("Bắt buộc"); return; }

        // Find selected category
        String catName = actvCategory.getText().toString();
        for (Category c : categories) {
            if (c.getName().equals(catName)) { selectedCategoryId = c.getId(); break; }
        }

        Pet pet = new Pet();
        if (editingPetId != null) {
            pet.setId(editingPetId);
            // Giữ lại thumbnail cũ nếu có
            if (vm.getCurrentPet().getValue() != null) {
                pet.setThumbnailUrl(vm.getCurrentPet().getValue().getThumbnailUrl());
            }
        }
        pet.setName(name);
        pet.setCategoryId(selectedCategoryId);
        pet.setSpecies(actvSpecies.getText().toString());
        pet.setBreed(getText(etBreed));
        try { pet.setAge(Integer.parseInt(getText(etAge))); } catch (Exception e) { pet.setAge(0); }
        pet.setAgeUnit(actvAgeUnit.getText().toString());
        pet.setGender(actvGender.getText().toString());
        try { pet.setWeight(Double.parseDouble(getText(etWeight))); } catch (Exception e) {}
        pet.setColor(getText(etColor));
        pet.setOrigin(getText(etOrigin));
        pet.setDescription(getText(etDesc));
        pet.setCareGuide(getText(etCareGuide));
        pet.setDietInfo(getText(etDietInfo));
        pet.setHealthInfo(getText(etHealthInfo));
        pet.setVaccineStatus(actvVaccine.getText().toString());
        pet.setDewormed(cbDewormed.isChecked());
        pet.setMicrochipped(cbMicrochipped.isChecked());
        pet.setHasCertificate(cbCertificate.isChecked());
        try { pet.setPrice(Double.parseDouble(getText(etPrice))); } catch (Exception e) {}
        try { pet.setOriginalPrice(Double.parseDouble(getText(etOriginalPrice))); } catch (Exception e) {}
        pet.setStatus(actvStatus.getText().toString());

        vm.savePet(pet, mediaAdapter.getNewUris(), mediaAdapter.getNewTypes());
    }

    private void setAdapter(AutoCompleteTextView actv, String[] items) {
        actv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items));
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void confirmDeleteMedia(int index, MediaPickerAdapter.MediaItem item) {
        DialogUtils.showConfirmDialog(this, "Xoá media này?",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    vm.deleteMediaItem(editingPetId, item.mediaId, item.url);
                    mediaAdapter.removeItem(index);
                }

                @Override
                public void onCancel() {
                    // Không làm gì
                }
            });
    }
}
