package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.project.bookstoreapp.R;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    // UI Components
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnResetPassword;
    private ProgressBar progressBar;

    // Data
    private String email;
    private String resetToken;

    // Network
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Lấy dữ liệu từ Intent
        email = getIntent().getStringExtra("email");
        resetToken = getIntent().getStringExtra("resetToken");

        if (email == null || resetToken == null) {
            Toast.makeText(this, "Lỗi: thiếu thông tin xác thực", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo Network
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Ánh xạ UI
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);

        // Sự kiện
        btnResetPassword.setOnClickListener(v -> performResetPassword());
    }

    // =============================================
    // ĐỔI MẬT KHẨU
    // =============================================
    private void performResetPassword() {
        // Xóa lỗi cũ
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // Validate
        if (newPassword.isEmpty()) {
            tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            tilNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etNewPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Gọi API
        showLoading();

        HashMap<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("resetToken", resetToken);
        body.put("newPassword", newPassword);

        apiService.resetPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Đổi mật khẩu thành công
                        Toast.makeText(ResetPasswordActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();

                        // Quay về LoginActivity, xóa toàn bộ stack
                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("message")) {
                            String message = errorBody.split("\"message\":\"")[1].split("\"")[0];
                            Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ResetPasswordActivity.this,
                                "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "Lỗi kết nối server:", t);
                Toast.makeText(ResetPasswordActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // LOADING UI
    // =============================================
    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnResetPassword.setEnabled(true);
    }
}
