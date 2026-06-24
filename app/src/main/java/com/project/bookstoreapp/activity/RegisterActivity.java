package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.project.bookstoreapp.R;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ dòng chữ quay lại đăng nhập
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Khi nhấn vào, ta không tạo Intent mới mà chỉ cần đóng màn hình hiện tại
        // Hệ thống sẽ tự động rút màn hình này khỏi ngăn xếp, lộ ra màn hình Login bên dưới.
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });
    }
}