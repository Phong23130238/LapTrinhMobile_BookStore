package com.project.bookstoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.project.bookstoreapp.activity.LoginActivity;
import com.project.bookstoreapp.utils.DatabaseSeeder;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. KÍCH HOẠT SEED DỮ LIỆU (CHỈ DÙNG 1 LẦN)
        // LƯU Ý: Mở comment dòng dưới đây để chạy đẩy dữ liệu 1 lần.
        // Lần sau mở app, BẮT BUỘC phải comment nó lại (thêm // ở đầu) để không ghi đè dữ liệu mới!
         DatabaseSeeder.seedDataFromJson(this);

        mAuth = FirebaseAuth.getInstance();

        // 2. Kiểm tra xem đã đăng nhập chưa
        if (mAuth.getCurrentUser() != null) {
            // Tương lai: Bạn có thể chèn thêm lệnh kiểm tra Role ở đây.
            // Nếu Role = Admin -> Mở AdminDashboardActivity.
            // Nếu Role = Customer -> Mở HomeActivity.
            // Hiện tại cứ cho vào Home trước.
            startActivity(new Intent(MainActivity.this, com.project.bookstoreapp.activity.HomeActivity.class));
        } else {
            // Nếu chưa, vào Login
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        finish(); // Đóng MainActivity để người dùng không bấm nút Back quay lại màn hình trắng được
    }
}