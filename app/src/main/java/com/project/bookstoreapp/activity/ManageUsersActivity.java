package com.project.bookstoreapp.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.UserAdapter;
import com.project.bookstoreapp.model.User;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvAdminUsers;
    private SearchView searchViewUsers;
    private ProgressBar progressBarUsers;
    private UserAdapter userAdapter;

    private List<User> originalUserList;
    private List<User> displayUserList;

    // KHÔNG CÒN DÙNG FIREBASE Ở ĐÂY NỮA
    private ApiService apiService;

    private String currentSearchText = "";
    private String currentRoleFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Khởi tạo Retrofit thay vì Firebase
        apiService = RetrofitClient.getClient().create(ApiService.class);

        initViews();
        setupSearchView();

        // Gọi hàm load API mới
        loadUsersFromApi();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageUsers);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        searchViewUsers = findViewById(R.id.searchViewUsers);
        progressBarUsers = findViewById(R.id.progressBarUsers);
        rvAdminUsers = findViewById(R.id.rvAdminUsers);
        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));

        originalUserList = new ArrayList<>();
        displayUserList = new ArrayList<>();

        userAdapter = new UserAdapter(displayUserList, clickedUser -> {
            showToggleLockDialog(clickedUser);
        });
        rvAdminUsers.setAdapter(userAdapter);

        RadioGroup rgRoleFilter = findViewById(R.id.rgRoleFilter);
        rgRoleFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAdmin) currentRoleFilter = "Admin";
            else if (checkedId == R.id.rbCustomer) currentRoleFilter = "Customer";
            else currentRoleFilter = "All";
            applyFilters();
        });
    }

    private void setupSearchView() {
        searchViewUsers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchText = newText.toLowerCase().trim();
                applyFilters();
                return true;
            }
        });
    }

    private void applyFilters() {
        List<User> filteredList = new ArrayList<>();

        for (User user : originalUserList) {
            boolean matchText = true;
            boolean matchRole = true;

            if (!currentSearchText.isEmpty()) {
                String name = user.getName() != null ? user.getName().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                if (!name.contains(currentSearchText) && !email.contains(currentSearchText)) {
                    matchText = false;
                }
            }

            if (!currentRoleFilter.equals("All")) {
                String role = user.getRole() != null ? user.getRole() : "";
                if (currentRoleFilter.equals("Admin") && !role.equalsIgnoreCase("admin")) matchRole = false;
                if (currentRoleFilter.equals("Customer") && role.equalsIgnoreCase("admin")) matchRole = false;
            }

            if (matchText && matchRole) filteredList.add(user);
        }
        userAdapter.updateList(filteredList);
    }

    // ==========================================
    // GỌI API: LẤY DANH SÁCH USER
    // ==========================================
    private void loadUsersFromApi() {
        progressBarUsers.setVisibility(View.VISIBLE);

        apiService.getAllUsers().enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                progressBarUsers.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    originalUserList.clear();
                    displayUserList.clear();

                    // Lấy dữ liệu từ API nạp vào List
                    originalUserList.addAll(response.body().getData());

                    applyFilters();
                } else {
                    Toast.makeText(ManageUsersActivity.this, "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                progressBarUsers.setVisibility(View.GONE);
                Toast.makeText(ManageUsersActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
                Log.e("ManageUsers", "Lỗi API getAllUsers: ", t);
            }
        });
    }

    private void showToggleLockDialog(User user) {
        String actionStr = user.isLocked() ? "Mở khóa" : "Khóa";

        new AlertDialog.Builder(this)
                .setTitle(actionStr + " tài khoản")
                .setMessage("Bạn có chắc muốn " + actionStr.toLowerCase() + " tài khoản " + user.getName() + "?")
                .setPositiveButton("Xác nhận", (dialog, which) -> toggleUserLockStatus(user))
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==========================================
    // GỌI API: CẬP NHẬT TRẠNG THÁI KHÓA
    // ==========================================
    private void toggleUserLockStatus(User user) {
        if (user.getUid() == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newLockStatus = !user.isLocked();
        progressBarUsers.setVisibility(View.VISIBLE);

        // Tạo body JSON: { "isLocked": true }
        Map<String, Boolean> body = new HashMap<>();
        body.put("isLocked", newLockStatus);

        apiService.toggleUserLock(user.getUid(), body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                progressBarUsers.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ManageUsersActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    // Tải lại danh sách từ Server để UI cập nhật
                    loadUsersFromApi();
                } else {
                    Toast.makeText(ManageUsersActivity.this, "Lỗi cập nhật trên Server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                progressBarUsers.setVisibility(View.GONE);
                Toast.makeText(ManageUsersActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                Log.e("ManageUsers", "Lỗi API toggleUserLock: ", t);
            }
        });
    }
}