package com.project.bookstoreapp.activity;

import android.content.Intent;
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
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    // UI Components
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendOtp;
    private ProgressBar progressBar;
    private TextView tvBackToLogin;

    // Network
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Khởi tạo Network
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Ánh xạ UI
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        progressBar = findViewById(R.id.progressBar);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Sự kiện
        btnSendOtp.setOnClickListener(v -> performSendOtp());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    // =============================================
    // GỬI OTP
    // =============================================
    private void performSendOtp() {
        tilEmail.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        // Validate
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

        // Gọi API
        showLoading();

        HashMap<String, Object> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Gửi OTP thành công → chuyển sang màn hình nhập OTP
                        Toast.makeText(ForgotPasswordActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    // HTTP error
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("message")) {
                            String message = errorBody.split("\"message\":\"")[1].split("\"")[0];
                            Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "Lỗi kết nối server:", t);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // LOADING UI
    // =============================================
    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnSendOtp.setEnabled(true);
    }
}
