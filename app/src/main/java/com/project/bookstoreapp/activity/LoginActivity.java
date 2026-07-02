package com.project.bookstoreapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.User;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;
import com.project.bookstoreapp.utils.SessionManager;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Web Client ID
    private static final String WEB_CLIENT_ID = "156167272606-ahuk0t1gr5biq7b69a24kh0i9so84vp4.apps.googleusercontent.com";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        setupGoogleSignIn();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        hideLoading();
                        Log.w(TAG, "Google Sign-In bị hủy hoặc thất bại, resultCode=" + result.getResultCode());
                    }
                });

        // ===== SỰ KIỆN =====

        // đăng nhập
        btnLogin.setOnClickListener(v -> performLogin());

        // đăng nhập bằng Google
        btnGoogleLogin.setOnClickListener(v -> performGoogleLogin());

        // đăng ký
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * CẤU HÌNH GOOGLE SIGN-IN
     * Khởi tạo GoogleSignInClient với cấu hình yêu cầu Email và ID Token.
     * ID Token này do Google cấp cho Client, sau đó Client sẽ gửi lên Server
     * Node.js
     * để verify (đảm bảo tính bảo mật, tránh giả mạo token).
     */
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * ĐĂNG NHẬP THƯỜNG (đăng nhập bằng: Email + Password)
     * 1. Validate email và password không được rỗng, email đúng định dạng.
     * 2. Gọi API `apiService.login` truyền lên credentials.
     * 3. Xử lý phản hồi từ server:
     * - Thành công: Lưu thông tin User vào SessionManager, chuyển hướng đến Home
     * (hoặc Admin).
     * - Thất bại (401/400): Hiển thị Toast báo lỗi tương ứng.
     */
    private void performLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

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

        showLoading();

        HashMap<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        apiService.login(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        User user = apiResponse.getData();
                        sessionManager.saveUser(user);
                        navigateToHome(user);
                    } else {
                        Toast.makeText(LoginActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("message")) {
                            String message = errorBody.split("\"message\":\"")[1].split("\"")[0];
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this,
                                "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "Lỗi kết nối server:", t);
                Toast.makeText(LoginActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // ĐĂNG NHẬP BẰNG GOOGLE
    // =============================================
    private void performGoogleLogin() {
        showLoading();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account == null || account.getIdToken() == null) {
                hideLoading();
                Toast.makeText(this, "Không lấy được thông tin Google", Toast.LENGTH_SHORT).show();
                return;
            }

            String idToken = account.getIdToken();
            Log.d(TAG, "Google ID Token nhận được, gửi lên server để verify...");

            sendGoogleTokenToServer(idToken);

        } catch (ApiException e) {
            hideLoading();
            Log.e(TAG, "Google Sign-In thất bại, statusCode=" + e.getStatusCode(), e);
            Toast.makeText(this, "Đăng nhập Google thất bại (mã lỗi: " + e.getStatusCode() + ")",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void sendGoogleTokenToServer(String idToken) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("idToken", idToken);

        apiService.googleLogin(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        User user = apiResponse.getData();
                        sessionManager.saveUser(user);
                        Toast.makeText(LoginActivity.this,
                                "Xin chào, " + user.getName() + "!", Toast.LENGTH_SHORT).show();
                        navigateToHome(user);
                    } else {
                        Toast.makeText(LoginActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Xác thực Google thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "Lỗi kết nối server (Google login):", t);
                Toast.makeText(LoginActivity.this,
                        "Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============================================
    // ĐIỀU HƯỚNG SAU ĐĂNG NHẬP
    // =============================================
    private void navigateToHome(User user) {
        Intent intent;
        if ("admin".equals(user.getRole())) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, HomeActivity.class);
        }
        // Xóa sạch back stack để không bấm Back quay lại Login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // =============================================
    // LOADING UI
    // =============================================
    private void showLoading() {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnGoogleLogin.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null)
            progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        btnGoogleLogin.setEnabled(true);
    }
}