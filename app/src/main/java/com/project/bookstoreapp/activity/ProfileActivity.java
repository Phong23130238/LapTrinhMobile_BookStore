package com.project.bookstoreapp.activity;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.User;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;
import com.project.bookstoreapp.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private CircleImageView ivAvatar;
    private FloatingActionButton fabChangeAvatar;
    private TextView tvEmail;
    private EditText etName, etPhone, etAddress;
    private MaterialButton btnSaveProfile, btnChangePassword, btnLogout;
    private ProgressBar progressBarProfile;

    private SessionManager sessionManager;
    private User currentUser;
    private ApiService apiService;
    private Uri selectedImageUri = null;

    // Trình chọn ảnh
    private final ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).into(ivAvatar);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarProfile);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivAvatar = findViewById(R.id.ivAvatar);
        fabChangeAvatar = findViewById(R.id.fabChangeAvatar);
        tvEmail = findViewById(R.id.tvEmail);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        progressBarProfile = findViewById(R.id.progressBarProfile);
    }

    private void loadUserData() {
        tvEmail.setText(currentUser.getEmail());
        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());
        etAddress.setText(currentUser.getAddress());

        String avatarUrl = currentUser.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(ivAvatar);
        }
    }

    private void setupListeners() {
        fabChangeAvatar.setOnClickListener(v -> {
            getContentLauncher.launch("image/*");
        });

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showLoading(boolean isLoading) {
        progressBarProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        fabChangeAvatar.setEnabled(!isLoading);
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Vui lòng nhập họ tên");
            return;
        }

        showLoading(true);

        // Tạo các RequestBody cho form-data
        RequestBody uidPart = RequestBody.create(MediaType.parse("text/plain"), currentUser.getUid());
        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), phone);
        RequestBody addressPart = RequestBody.create(MediaType.parse("text/plain"), address);

        MultipartBody.Part avatarPart = null;

        // Xử lý ảnh nếu có chọn
        if (selectedImageUri != null) {
            File file = getFileFromUri(selectedImageUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                avatarPart = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);
            }
        }

        apiService.updateProfile(uidPart, namePart, phonePart, addressPart, avatarPart)
                .enqueue(new Callback<ApiResponse<User>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isSuccess()) {
                                Toast.makeText(ProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                // Cập nhật Session
                                User updatedUser = response.body().getData();
                                sessionManager.saveUser(updatedUser);
                                currentUser = updatedUser;
                                selectedImageUri = null; // reset
                            } else {
                                Toast.makeText(ProfileActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, "Lỗi cập nhật hồ sơ", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "onFailure: ", t);
                        Toast.makeText(ProfileActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        dialog.setContentView(R.layout.dialog_change_password);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etOldPassword = dialog.findViewById(R.id.etOldPassword);
        EditText etNewPassword = dialog.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialog.findViewById(R.id.etConfirmPassword);
        Button btnCancel = dialog.findViewById(R.id.btnCancelPassword);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitPassword);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarPassword);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
                return;
            }

            if (newPass.length() < 6) {
                etNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);

            HashMap<String, Object> body = new HashMap<>();
            body.put("uid", currentUser.getUid());
            body.put("oldPassword", oldPass);
            body.put("newPassword", newPass);

            apiService.updatePassword(body).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().isSuccess()) {
                            Toast.makeText(ProfileActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(ProfileActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle error body
                        try {
                            String err = response.errorBody().string();
                            if (err.contains("message")) {
                                String msg = err.split("\"message\":\"")[1].split("\"")[0];
                                Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Lỗi khi đổi mật khẩu", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ProfileActivity.this, "Lỗi khi đổi mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // Tiện ích để tạo file tạm từ URI (vì Retrofit Multipart cần File hoặc File path)
    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getCacheDir(), "upload_avatar_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi chuyển đổi URI sang File: ", e);
            return null;
        }
    }
}