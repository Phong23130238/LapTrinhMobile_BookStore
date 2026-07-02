package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.project.bookstoreapp.R;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {

    private static final String TAG = "VerifyOtpActivity";

    // UI Components
    private EditText[] otpFields;
    private MaterialButton btnVerifyOtp;
    private TextView tvResendOtp, tvEmailInfo, tvBackToLogin;
    private ProgressBar progressBar;

    // Data
    private String email;
    private CountDownTimer resendTimer;

    // Network
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Lấy email từ Intent
        email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Lỗi: không có email", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo Network
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Ánh xạ UI
        otpFields = new EditText[]{
                findViewById(R.id.etOtp1),
                findViewById(R.id.etOtp2),
                findViewById(R.id.etOtp3),
                findViewById(R.id.etOtp4),
                findViewById(R.id.etOtp5),
                findViewById(R.id.etOtp6)
        };
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        tvEmailInfo = findViewById(R.id.tvEmailInfo);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        // Hiển thị email (che bớt để bảo mật)
        tvEmailInfo.setText("Mã xác thực đã gửi đến\n" + maskEmail(email));

        // Cấu hình auto-focus cho 6 ô OTP
        setupOtpInputs();

        // Bắt đầu đếm ngược gửi lại mã
        startResendTimer();

        // Sự kiện
        btnVerifyOtp.setOnClickListener(v -> performVerifyOtp());

        tvResendOtp.setOnClickListener(v -> {
            if (tvResendOtp.isClickable()) {
                resendOtp();
            }
        });

        tvBackToLogin.setOnClickListener(v -> {
            // Quay về LoginActivity, xóa stack
            Intent intent = new Intent(VerifyOtpActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    // =============================================
    // AUTO-FOCUS OTP INPUTS
    // =============================================
    private void setupOtpInputs() {
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        // Tự động chuyển sang ô kế tiếp
                        otpFields[index + 1].requestFocus();
                    }
                }
            });

            // Xử lý phím Delete/Backspace để quay lại ô trước
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && otpFields[index].getText().toString().isEmpty()
                        && index > 0) {
                    otpFields[index - 1].requestFocus();
                    otpFields[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }

        // Focus vào ô đầu tiên
        otpFields[0].requestFocus();
    }

    // =============================================
    // XÁC THỰC OTP
    // =============================================
    private void performVerifyOtp() {
        // Lấy OTP từ 6 ô
        StringBuilder otpBuilder = new StringBuilder();
        for (EditText field : otpFields) {
            String digit = field.getText().toString().trim();
            if (digit.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số", Toast.LENGTH_SHORT).show();
                field.requestFocus();
                return;
            }
            otpBuilder.append(digit);
        }

        String otp = otpBuilder.toString();

        // Gọi API verify OTP
        showLoading();

        HashMap<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("otp", otp);

        apiService.verifyOtp(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        String resetToken = apiResponse.getResetToken();

                        Toast.makeText(VerifyOtpActivity.this,
                                "Xác thực thành công!", Toast.LENGTH_SHORT).show();

                        // Chuyển sang màn hình đổi mật khẩu
                        Intent intent = new Intent(VerifyOtpActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("resetToken", resetToken);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(VerifyOtpActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("message")) {
                            String message = errorBody.split("\"message\":\"")[1].split("\"")[0];
                            Toast.makeText(VerifyOtpActivity.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(VerifyOtpActivity.this,
                                    "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(VerifyOtpActivity.this,
                                "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "Lỗi kết nối server:", t);
                Toast.makeText(VerifyOtpActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // GỬI LẠI MÃ OTP
    // =============================================
    private void resendOtp() {
        showLoading();

        HashMap<String, Object> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(VerifyOtpActivity.this,
                            "Đã gửi lại mã xác thực!", Toast.LENGTH_SHORT).show();
                    // Xóa OTP cũ
                    for (EditText field : otpFields) {
                        field.setText("");
                    }
                    otpFields[0].requestFocus();
                    // Bắt đầu đếm ngược lại
                    startResendTimer();
                } else {
                    Toast.makeText(VerifyOtpActivity.this,
                            "Gửi lại mã thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                hideLoading();
                Toast.makeText(VerifyOtpActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // COUNTDOWN TIMER
    // =============================================
    private void startResendTimer() {
        tvResendOtp.setClickable(false);
        tvResendOtp.setTextColor(getResources().getColor(R.color.text_secondary, null));

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendOtp.setText("Gửi lại mã (" + (millisUntilFinished / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                tvResendOtp.setText("Gửi lại mã");
                tvResendOtp.setClickable(true);
                tvResendOtp.setTextColor(getResources().getColor(R.color.navy_medium, null));
            }
        }.start();
    }

    // =============================================
    // HELPER: Che email
    // =============================================
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.substring(0, 2) + "***@" + parts[1];
    }

    // =============================================
    // LOADING UI
    // =============================================
    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnVerifyOtp.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnVerifyOtp.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
