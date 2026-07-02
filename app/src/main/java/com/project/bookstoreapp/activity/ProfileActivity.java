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
import com.google.android.material.textfield.TextInputLayout;
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
import java.util.ArrayList;
import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.project.bookstoreapp.ghn.District;
import com.project.bookstoreapp.ghn.GHNApiService;
import com.project.bookstoreapp.ghn.GHNResponse;
import com.project.bookstoreapp.ghn.Province;
import com.project.bookstoreapp.ghn.RetrofitClientGHN;
import com.project.bookstoreapp.ghn.Ward;

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
    private TextInputLayout tilPhone;
    private EditText etName, etPhone, etAddress;
    private MaterialButton btnSaveProfile, btnChangePassword, btnLogout;
    private ProgressBar progressBarProfile;

    private SessionManager sessionManager;
    private User currentUser;
    private ApiService apiService;
    private Uri selectedImageUri = null;

    private AutoCompleteTextView spinProvince, spinDistrict, spinWard;
    private List<Province> provinceList = new ArrayList<>();
    private List<District> districtList = new ArrayList<>();
    private List<Ward> wardList = new ArrayList<>();
    private Province selectedProvince = null;
    private District selectedDistrict = null;
    private Ward selectedWard = null;
    private final String GHN_TOKEN = "dffec2e1-6725-11f1-a973-aee5264794df";

    // 1. Khai báo Launcher để nhận kết quả từ Map
    private ActivityResultLauncher<Intent> mapLauncher;

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

    // 5.1 Khởi tạo giao diện (onCreate, loadUserData, parseAndAutoFillAddress)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 2. Khởi tạo mapLauncher ngay trong onCreate
        mapLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Nhận địa chỉ dạng chuỗi văn bản từ Map trả về
                        String selectedAddress = result.getData().getStringExtra("SELECTED_ADDRESS");

                        // Gán vào EditText địa chỉ
                        if (etAddress != null) {
                            etAddress.setText(selectedAddress);
                            // Bạn có thể yêu cầu người dùng tự chọn lại Phường/Xã/Quận cho đúng chuẩn GHN
                            Toast.makeText(this, "Vui lòng kiểm tra và chọn lại Tỉnh/Quận/Phường nếu cần", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        apiService = RetrofitClient.getClient().create(ApiService.class);
        // 5.1.1 Gọi SessionManager kiểm tra đăng nhập. Nếu OK, gọi initViews và loadUserData().
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
        tilPhone = findViewById(R.id.tilPhone);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        spinProvince = findViewById(R.id.spinProvince);
        spinDistrict = findViewById(R.id.spinDistrict);
        spinWard = findViewById(R.id.spinWard);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        progressBarProfile = findViewById(R.id.progressBarProfile);
    }

    // 5.1.2 Đẩy dữ liệu currentUser lên View. Gọi hàm parseAndAutoFillAddress tách chuỗi (Address text -> Array split bằng phẩy) để định hình lại dropdown GHN.
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

        if (currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
            parseAndAutoFillAddress(currentUser.getAddress());
        }
    }

    private String targetProvinceName = null;
    private String targetDistrictName = null;
    private String targetWardName = null;

    private void parseAndAutoFillAddress(String fullAddress) {
        if (fullAddress == null || fullAddress.isEmpty()) return;
        String[] parts = fullAddress.split(", ");
        if (parts.length >= 4) {
            targetProvinceName = parts[parts.length - 1].trim();
            targetDistrictName = parts[parts.length - 2].trim();
            targetWardName = parts[parts.length - 3].trim();

            StringBuilder street = new StringBuilder();
            for (int i = 0; i < parts.length - 3; i++) {
                street.append(parts[i]);
                if (i < parts.length - 4) street.append(", ");
            }
            etAddress.setText(street.toString());
        } else {
            etAddress.setText(fullAddress);
        }
    }

    private void setupListeners() {
        // 3. Ánh xạ TextInputLayout và bắt sự kiện bấm icon Bản đồ
        TextInputLayout tilAddress = findViewById(R.id.tilAddress);
        if (tilAddress != null) {
            tilAddress.setEndIconOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, MapAddressActivity.class);
                mapLauncher.launch(intent);
            });
        }

        // 5.2 Xử lý ảnh đại diện (setupListeners, getFileFromUri)
        // 5.2.1 Khởi tạo ActivityResultLauncher (GetContent) để mở thư viện, lọc MIME type 'image/*'. Lấy URI khi trả về.
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

        // 6.2.1 Bắt sự kiện spinProvince.setOnItemClickListener, lấy biến ProvinceID và làm trống các Dropdown dưới.
        spinProvince.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvince = provinceList.get(position);
            spinDistrict.setText("");
            spinWard.setText("");
            selectedDistrict = null;
            selectedWard = null;
            loadDistricts(selectedProvince.ProvinceID);
        });

        // 6.3.1 Bắt sự kiện spinDistrict.setOnItemClickListener, lấy DistrictID. Gọi hàm getWards() tải mảng Ward.
        spinDistrict.setOnItemClickListener((parent, view, position, id) -> {
            selectedDistrict = districtList.get(position);
            spinWard.setText("");
            selectedWard = null;
            loadWards(selectedDistrict.DistrictID);
        });

        spinWard.setOnItemClickListener((parent, view, position, id) -> {
            selectedWard = wardList.get(position);
        });

        loadProvinces();
    }

    // 6.1 Tải danh sách Tỉnh/Thành phố (loadProvinces)
    private void loadProvinces() {
        // 6.1.1 Dùng RetrofitClientGHN.getApiService().getProvinces() lấy toàn bộ tỉnh qua Header token GHN.
        RetrofitClientGHN.getApiService().getProvinces(GHN_TOKEN).enqueue(new Callback<GHNResponse<List<Province>>>() {
            @Override
            public void onResponse(Call<GHNResponse<List<Province>>> call, Response<GHNResponse<List<Province>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    // 6.1.2 Đổ Json List vào provinceList, binding lên AutoCompleteTextView spinProvince thông qua ArrayAdapter.
                    provinceList = response.body().data;
                    ArrayAdapter<Province> adapter = new ArrayAdapter<>(ProfileActivity.this, android.R.layout.simple_list_item_1, provinceList);
                    spinProvince.setAdapter(adapter);

                    
                    // 6.1.3 Quét mảng tìm Tỉnh trùng với targetProvinceName (được parse từ hàm parseAndAutoFillAddress). Gọi hàm loadDistricts().
                    if (targetProvinceName != null && !targetProvinceName.isEmpty()) {
                        for (Province p : provinceList) {
                            if (p.ProvinceName != null && p.ProvinceName.equalsIgnoreCase(targetProvinceName)) {
                                selectedProvince = p;
                                spinProvince.setText(p.ProvinceName, false);
                                loadDistricts(p.ProvinceID);
                                break;
                            }
                        }
                        targetProvinceName = null;
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi tải Tỉnh: API trả về thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GHNResponse<List<Province>>> call, Throwable t) {
                Log.e(TAG, "Lỗi tải Tỉnh: ", t);
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối GHN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 6.2 Lải danh sách Quận/Huyện (loadDistricts)
    private void loadDistricts(int provinceId) {
        RetrofitClientGHN.getApiService().getDistricts(GHN_TOKEN, provinceId).enqueue(new Callback<GHNResponse<List<District>>>() {
            @Override
            public void onResponse(Call<GHNResponse<List<District>>> call, Response<GHNResponse<List<District>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    // 6.2.2 Gọi API getDistricts truyền provinceId. Lọc tương tự tự động điền targetDistrictName rồi gọi loadWards().
                    districtList = response.body().data;
                    ArrayAdapter<District> adapter = new ArrayAdapter<>(ProfileActivity.this, android.R.layout.simple_list_item_1, districtList);
                    spinDistrict.setAdapter(adapter);

                    if (targetDistrictName != null && !targetDistrictName.isEmpty()) {
                        for (District d : districtList) {
                            if (d.DistrictName != null && d.DistrictName.equalsIgnoreCase(targetDistrictName)) {
                                selectedDistrict = d;
                                spinDistrict.setText(d.DistrictName, false);
                                loadWards(d.DistrictID);
                                break;
                            }
                        }
                        targetDistrictName = null;
                    }
                }
            }
            @Override
            public void onFailure(Call<GHNResponse<List<District>>> call, Throwable t) {
                Log.e(TAG, "Lỗi tải Quận/Huyện: ", t);
            }
        });
    }

    // 6.3 Tải danh sách Phường/Xã (loadWards)
    private void loadWards(int districtId) {
        RetrofitClientGHN.getApiService().getWards(GHN_TOKEN, districtId).enqueue(new Callback<GHNResponse<List<Ward>>>() {
            @Override
            public void onResponse(Call<GHNResponse<List<Ward>>> call, Response<GHNResponse<List<Ward>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    wardList = response.body().data;
                    ArrayAdapter<Ward> adapter = new ArrayAdapter<>(ProfileActivity.this, android.R.layout.simple_list_item_1, wardList);
                    spinWard.setAdapter(adapter);

                    
                    // 6.3.2 Tự động match với targetWardName, nếu đúng cập nhật spinWard và chọn biến tham chiếu selectedWard.
                    if (targetWardName != null && !targetWardName.isEmpty()) {
                        for (Ward w : wardList) {
                            if (w.WardName != null && w.WardName.equalsIgnoreCase(targetWardName)) {
                                selectedWard = w;
                                spinWard.setText(w.WardName, false);
                                break;
                            }
                        }
                        targetWardName = null;
                    }
                }
            }
            @Override
            public void onFailure(Call<GHNResponse<List<Ward>>> call, Throwable t) {
                Log.e(TAG, "Lỗi tải Phường/Xã: ", t);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBarProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        fabChangeAvatar.setEnabled(!isLoading);
    }

    // 5.3 Lưu thông tin hồ sơ (saveProfile)
    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        // Kiểm tra rỗng
        if (phone.isEmpty()) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            return; // Dừng lại không gọi API nữa
        }

// Kiểm tra định dạng hợp lệ
        if (!phone.matches("^0[35789][0-9]{8}$")) {
            tilPhone.setError("Số điện thoại không hợp lệ (VD: 0912345678)");
            return;
        }

        tilPhone.setError(null); // Xóa lỗi nếu đã nhập đúng
        String addressStreet = etAddress.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (selectedProvince == null || selectedDistrict == null || selectedWard == null || addressStreet.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }

        String address = addressStreet + ", " + selectedWard.WardName + ", " + selectedDistrict.DistrictName + ", " + selectedProvince.ProvinceName;

        showLoading(true);

        if (currentUser.getUid() == null) {
            Toast.makeText(this, "Lỗi dữ liệu người dùng, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        RequestBody uidPart = RequestBody.create(MediaType.parse("text/plain"), currentUser.getUid());
        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), phone);
        RequestBody addressPart = RequestBody.create(MediaType.parse("text/plain"), address);

        MultipartBody.Part avatarPart = null;

        // 5.3.2 Tạo MultipartBody.Part từ file ảnh tạm (nếu có). Gọi apiService.updateProfile() đẩy file qua HTTP.
        if (selectedImageUri != null) {
            File file = getFileFromUri(selectedImageUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                avatarPart = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);
            }
        }

        Call<ApiResponse<User>> call;
        if (avatarPart != null) {
            call = apiService.updateProfile(uidPart, namePart, phonePart, addressPart, avatarPart);
        } else {
            call = apiService.updateProfileWithoutAvatar(uidPart, namePart, phonePart, addressPart);
        }

        call.enqueue(new Callback<ApiResponse<User>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isSuccess()) {
                                Toast.makeText(ProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                // 5.3.3 Nếu server báo thành công, ghi đè object User mới lên SessionManager.
                                User updatedUser = response.body().getData();
                                sessionManager.saveUser(updatedUser);
                                currentUser = updatedUser;
                                selectedImageUri = null;
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

    // 5.4 Đổi mật khẩu (showChangePasswordDialog)
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

            // 5.4.1 Mở popup đổi mật khẩu, kiểm tra đầu vào mật khẩu cũ, mật khẩu mới (khớp nhau, > 6 ký tự).
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

            // 5.4.2 Đóng gói tham số, gọi apiService.updatePassword(body). Try-catch parse chuỗi lỗi từ errorBody.string() nếu HTTP Code 400.
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
    // 5.2.2 Khi bấm lưu Profile, gọi getFileFromUri dùng luồng Byte (InputStream) copy nội dung file từ Storage vào thư mục Cache thành File tạm, để chuyển lên Retrofit Multipart.
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