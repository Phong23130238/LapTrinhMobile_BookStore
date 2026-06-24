package com.project.bookstoreapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.project.bookstoreapp.activity.LoginActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Không cần gọi giao diện activity_main nữa
        // setContentView(R.layout.activity_main);

        // Chuyển hướng ngay sang LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);

        // Gọi finish() để đóng hoàn toàn MainActivity,
        // ngăn người dùng bấm nút Back trên điện thoại quay lại màn hình trống này.
        finish();
    }
}