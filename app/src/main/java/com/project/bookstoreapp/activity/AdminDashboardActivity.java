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
        Button btnAdminLogout = findViewById(R.id.btnAdminLogout);

        // Chuyển sang trang Quản lý sách
        btnManageBooks.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageBooksActivity.class));
        });

        // Chuyển sang trang Quản lý đơn hàng (Bỏ comment khi bạn tạo file ManageOrdersActivity)
        // btnManageOrders.setOnClickListener(v -> {
        //     startActivity(new Intent(AdminDashboardActivity.this, ManageOrdersActivity.class));
        // });

        // Chuyển sang trang Quản lý người dùng (Bỏ comment khi bạn tạo file ManageUsersActivity)
        // btnManageUsers.setOnClickListener(v -> {
        //     startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class));
        // });

        // Đăng xuất quay về Login
        btnAdminLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa lịch sử trang Admin
            startActivity(intent);
        });
    }
}