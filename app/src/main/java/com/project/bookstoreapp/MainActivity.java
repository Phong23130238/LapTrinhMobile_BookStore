package com.project.bookstoreapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.project.bookstoreapp.activity.AdminDashboardActivity;
import com.project.bookstoreapp.activity.HomeActivity;
import com.project.bookstoreapp.activity.LoginActivity;
import com.project.bookstoreapp.utils.DatabaseSeeder;
import com.project.bookstoreapp.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. KÍCH HOẠT SEED DỮ LIỆU (CHỈ DÙNG 1 LẦN)
        // LƯU Ý: Mở comment dòng dưới đây để chạy đẩy dữ liệu 1 lần.
        // Lần sau mở app, BẮT BUỘC phải comment nó lại (thêm // ở đầu) để không ghi đè dữ liệu mới!
         DatabaseSeeder.seedDataFromJson(this);

        // 2. Kiểm tra session đăng nhập bằng SharedPreferences
        SessionManager sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            // Đã đăng nhập → kiểm tra role để điều hướng
            String role = sessionManager.getRole();
            if ("admin".equals(role)) {
                startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
            }
        } else {
            // Chưa đăng nhập → vào Login
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        finish(); // Đóng MainActivity để người dùng không bấm nút Back quay lại màn hình trắng được
    }
}