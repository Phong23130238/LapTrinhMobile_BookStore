package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.VoucherAdapter;
import com.project.bookstoreapp.model.Voucher;

import java.util.ArrayList;
import java.util.List;

public class ManageVouchersActivity extends AppCompatActivity {

    private RecyclerView rvVouchers;
    private TextView tvEmptyVouchers;
    private FloatingActionButton fabAddVoucher;
    private VoucherAdapter adapter;
    private List<Voucher> voucherList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_vouchers);

        MaterialToolbar toolbar = findViewById(R.id.toolbarManageVouchers);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvVouchers = findViewById(R.id.rvVouchers);
        tvEmptyVouchers = findViewById(R.id.tvEmptyVouchers);
        fabAddVoucher = findViewById(R.id.fabAddVoucher);

        db = FirebaseFirestore.getInstance();
        voucherList = new ArrayList<>();
        adapter = new VoucherAdapter(this, voucherList);
        rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        rvVouchers.setAdapter(adapter);

        fabAddVoucher.setOnClickListener(v -> {
            startActivity(new Intent(ManageVouchersActivity.this, AddEditVoucherActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVouchers();
    }

    private void loadVouchers() {
        db.collection("vouchers").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    voucherList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Voucher voucher = new Voucher();
                        voucher.setCode(doc.getId());
                        
                        if (doc.contains("discountPercent")) {
                            Number dp = (Number) doc.get("discountPercent");
                            voucher.setDiscountPercent(dp != null ? dp.intValue() : 0);
                        }
                        if (doc.contains("maxDiscount")) {
                            Number md = (Number) doc.get("maxDiscount");
                            voucher.setMaxDiscount(md != null ? md.longValue() : 0L);
                        }
                        if (doc.contains("minOrderValue")) {
                            Number mo = (Number) doc.get("minOrderValue");
                            voucher.setMinOrderValue(mo != null ? mo.longValue() : 0L);
                        }
                        if (doc.contains("isActive")) {
                            Boolean active = doc.getBoolean("isActive");
                            voucher.setActive(active != null ? active : false);
                        } else if (doc.contains("active")) {
                            Boolean active = doc.getBoolean("active");
                            voucher.setActive(active != null ? active : false);
                        }

                        Object expiredObj = doc.get("expiredAt");
                        if (expiredObj instanceof com.google.firebase.Timestamp) {
                            java.util.Date date = ((com.google.firebase.Timestamp) expiredObj).toDate();
                            voucher.setExpiredAt(new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date));
                        } else if (expiredObj != null) {
                            voucher.setExpiredAt(expiredObj.toString());
                        } else {
                            voucher.setExpiredAt("");
                        }
                        
                        voucherList.add(voucher);
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (voucherList.isEmpty()) {
                        tvEmptyVouchers.setVisibility(View.VISIBLE);
                        rvVouchers.setVisibility(View.GONE);
                    } else {
                        tvEmptyVouchers.setVisibility(View.GONE);
                        rvVouchers.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải danh sách Voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
