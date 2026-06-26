package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.project.bookstoreapp.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ các thành phần từ giao diện XML
        TextView tvRegister = findViewById(R.id.tvRegister);

        // Lưu ý: Bạn cần kiểm tra xem ID của nút đăng nhập trong file XML
        // của bạn có phải là btnLogin không, nếu không hãy sửa lại cho khớp.
        Button btnLogin = findViewById(R.id.btnLogin);

        // 1. Nhấn "Đăng ký ngay" -> Mở màn hình Đăng ký
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 2. Nhấn "Đăng nhập" -> Mở Trang chủ
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);

                // Sau khi đăng nhập thành công, đóng màn hình Login lại
                finish();
            });
        }

    }
}