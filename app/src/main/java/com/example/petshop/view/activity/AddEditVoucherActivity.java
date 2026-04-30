package com.example.petshop.view.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;

public class AddEditVoucherActivity extends AppCompatActivity {

    public static final String EXTRA_VOUCHER_ID = "voucher_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: implement full add/edit voucher form
        Toast.makeText(this, "Thêm/sửa voucher — sắp hoàn thiện", Toast.LENGTH_SHORT).show();
        finish();
    }
}
