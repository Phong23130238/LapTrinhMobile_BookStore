package com.project.bookstoreapp.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import java.util.Calendar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Voucher;

public class AddEditVoucherActivity extends AppCompatActivity {

    private TextInputEditText etVoucherCode, etDiscountPercent, etMaxDiscount, etMinOrderValue, etExpiredAt;
    private TextInputLayout tilVoucherCode;
    private SwitchMaterial switchIsActive;
    private Button btnSaveVoucher;
    
    private FirebaseFirestore db;
    private boolean isEditMode = false;
    private String originalCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_voucher);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAddEditVoucher);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        initViews();
        checkEditMode();

        btnSaveVoucher.setOnClickListener(v -> saveVoucher());
    }

    private void initViews() {
        etVoucherCode = findViewById(R.id.etVoucherCode);
        etDiscountPercent = findViewById(R.id.etDiscountPercent);
        etMaxDiscount = findViewById(R.id.etMaxDiscount);
        etMinOrderValue = findViewById(R.id.etMinOrderValue);
        etExpiredAt = findViewById(R.id.etExpiredAt);
        switchIsActive = findViewById(R.id.switchIsActive);
        btnSaveVoucher = findViewById(R.id.btnSaveVoucher);
        tilVoucherCode = findViewById(R.id.tilVoucherCode);

        etExpiredAt.setFocusable(false);
        etExpiredAt.setClickable(true);
        etExpiredAt.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etExpiredAt.setText(formattedDate);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("VOUCHER_CODE")) {
            isEditMode = true;
            originalCode = getIntent().getStringExtra("VOUCHER_CODE");
            
            etVoucherCode.setText(originalCode);
            etVoucherCode.setEnabled(false);
            tilVoucherCode.setEnabled(false);
            
            etDiscountPercent.setText(String.valueOf(getIntent().getIntExtra("DISCOUNT_PERCENT", 0)));
            etMaxDiscount.setText(String.valueOf(getIntent().getLongExtra("MAX_DISCOUNT", 0)));
            etMinOrderValue.setText(String.valueOf(getIntent().getLongExtra("MIN_ORDER_VALUE", 0)));
            switchIsActive.setChecked(getIntent().getBooleanExtra("IS_ACTIVE", true));
            
            String expiredAt = getIntent().getStringExtra("EXPIRED_AT");
            if (expiredAt != null) {
                etExpiredAt.setText(expiredAt);
            }

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Sửa Voucher");
            }
        }
    }

    private void saveVoucher() {
        String code = etVoucherCode.getText() != null ? etVoucherCode.getText().toString().trim().toUpperCase() : "";
        String discountStr = etDiscountPercent.getText() != null ? etDiscountPercent.getText().toString().trim() : "";
        String maxDiscStr = etMaxDiscount.getText() != null ? etMaxDiscount.getText().toString().trim() : "";
        String minOrderStr = etMinOrderValue.getText() != null ? etMinOrderValue.getText().toString().trim() : "";
        String expiredAt = etExpiredAt.getText() != null ? etExpiredAt.getText().toString().trim() : "";
        boolean isActive = switchIsActive.isChecked();

        if (code.isEmpty() || discountStr.isEmpty() || maxDiscStr.isEmpty() || minOrderStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        int discountPercent = Integer.parseInt(discountStr);
        long maxDiscount = Long.parseLong(maxDiscStr);
        long minOrderValue = Long.parseLong(minOrderStr);

        if (discountPercent <= 0 || discountPercent > 100) {
            Toast.makeText(this, "Phần trăm giảm phải từ 1 đến 100", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveVoucher.setEnabled(false);
        btnSaveVoucher.setText("ĐANG LƯU...");

        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setDiscountPercent(discountPercent);
        voucher.setMaxDiscount(maxDiscount);
        voucher.setMinOrderValue(minOrderValue);
        voucher.setActive(isActive);
        voucher.setExpiredAt(expiredAt);

        db.collection("vouchers").document(code)
                .set(voucher)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Lưu Voucher thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveVoucher.setEnabled(true);
                    btnSaveVoucher.setText("LƯU VOUCHER");
                    Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
