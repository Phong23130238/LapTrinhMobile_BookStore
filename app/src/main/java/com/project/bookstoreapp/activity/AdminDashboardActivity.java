package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.project.bookstoreapp.R;

public class AdminDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button btnManageBooks = findViewById(R.id.btnManageBooks);
        Button btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);

        // Chuyển sang trang Quản lý kho sách
        if (btnManageBooks != null) {
            btnManageBooks.setOnClickListener(v -> {
                startActivity(new Intent(AdminDashboardActivity.this, ManageBooksActivity.class));
            });
        }

        // Đăng xuất về lại màn hình Đăng nhập
        if (btnLogoutAdmin != null) {
            btnLogoutAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                // Xóa toàn bộ lịch sử trang để không bấm nút Back quay lại trang Admin được
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }
}