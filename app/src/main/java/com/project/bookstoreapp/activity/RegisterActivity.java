package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.User;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI Components
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword, tilOtp;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword, etOtp;
    private MaterialButton btnRegister, btnSendOtp;
    private ProgressBar progressBar;

    // Network
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Network
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Ánh xạ UI
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilOtp = findViewById(R.id.tilOtp);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etOtp = findViewById(R.id.etOtp);
        btnRegister = findViewById(R.id.btnRegister);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        progressBar = findViewById(R.id.progressBarRegister);

        // Ánh xạ dòng chữ quay lại đăng nhập
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Khi nhấn vào, đóng màn hình hiện tại để quay về Login
        tvBackToLogin.setOnClickListener(v -> finish());

        // Nhấn "Tạo tài khoản" → Đăng ký
        btnRegister.setOnClickListener(v -> performRegister());

        // Nhấn "Gửi mã"
        btnSendOtp.setOnClickListener(v -> sendOtp());
    }

    // =============================================
    // GỬI MÃ OTP
    // =============================================
    private void sendOtp() {
        tilEmail.setError(null);
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        // Bắt đầu đếm ngược 60s
        btnSendOtp.setEnabled(false);
        new android.os.CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                btnSendOtp.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Gửi lại");
            }
        }.start();

        // Gọi API gửi OTP
        HashMap<String, Object> body = new HashMap<>();
        body.put("email", email);

        apiService.sendOtp(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(RegisterActivity.this, "Đã gửi mã OTP đến email của bạn", Toast.LENGTH_SHORT).show();
                    etOtp.requestFocus();
                } else {
                    Toast.makeText(RegisterActivity.this, "Lỗi gửi OTP, vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // ĐĂNG KÝ TÀI KHOẢN
    // =============================================
    private void performRegister() {
        // Xóa lỗi cũ
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilOtp.setError(null);

        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String otp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";

        // ===== VALIDATE =====
        if (name.isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        // Kiểm tra mật khẩu phải có chữ hoa, chữ thường, số, ký tự đặc biệt
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{6,}$";
        if (!password.matches(passwordPattern)) {
            tilPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt");
            etPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        if (otp.isEmpty()) {
            tilOtp.setError("Vui lòng nhập mã xác nhận OTP");
            etOtp.requestFocus();
            return;
        }

        // ===== GỌI API ĐĂNG KÝ =====
        showLoading();

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("otp", otp);

        apiService.register(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Đăng ký thành công
                        Toast.makeText(RegisterActivity.this,
                                "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                        finish(); // Quay về màn hình Login
                    } else {
                        // Server trả success=false (VD: email đã tồn tại)
                        Toast.makeText(RegisterActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    // HTTP error (400, 500...)
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("message")) {
                            String message = errorBody.split("\"message\":\"")[1].split("\"")[0];
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this,
                                "Đăng ký thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "Lỗi kết nối server:", t);
                Toast.makeText(RegisterActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // LOADING UI
    // =============================================
    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnRegister.setEnabled(true);
    }
}