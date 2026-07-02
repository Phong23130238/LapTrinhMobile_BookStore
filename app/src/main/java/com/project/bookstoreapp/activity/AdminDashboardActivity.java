package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.project.bookstoreapp.R;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        MaterialButton btnManageBooks = findViewById(R.id.btnManageBooks);
        MaterialButton btnManageOrders = findViewById(R.id.btnManageOrders);
        MaterialButton btnManageUsers = findViewById(R.id.btnManageUsers);
        MaterialButton btnManageVouchers = findViewById(R.id.btnManageVouchers);
        MaterialButton btnManageInventory = findViewById(R.id.btnManageInventory);
        Button btnAdminLogout = findViewById(R.id.btnAdminLogout);

        // Chuyển sang trang Quản lý sách
        btnManageBooks.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageBooksActivity.class));
        });

         btnManageOrders.setOnClickListener(v -> {
             startActivity(new Intent(AdminDashboardActivity.this, ManageOrdersActivity.class));
         });

         btnManageUsers.setOnClickListener(v -> {
             startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class));
        });

        btnManageVouchers.setOnClickListener(v -> {
             startActivity(new Intent(AdminDashboardActivity.this, ManageVouchersActivity.class));
        });

        findViewById(R.id.btnManageInventory).setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, InventoryActivity.class));
        });

        findViewById(R.id.btnAdminStats).setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminStatisticsActivity.class));
        });

        btnAdminLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa lịch sử trang Admin
            startActivity(intent);
        });
    }
}