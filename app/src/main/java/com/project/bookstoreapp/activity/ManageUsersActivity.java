package com.project.bookstoreapp.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.UserAdapter;
import com.project.bookstoreapp.model.User;
import com.project.bookstoreapp.model.UserStats;
import com.project.bookstoreapp.network.ApiResponse;
import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    private ApiService apiService;

    private String currentSearchText = "";
    private String currentRoleFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        initViews();
        setupSearchView();
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

        userAdapter = new UserAdapter(displayUserList, this::showUserDetailsDialog);
        rvAdminUsers.setAdapter(userAdapter);

        RadioGroup rgRoleFilter = findViewById(R.id.rgRoleFilter);
        rgRoleFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAdmin)         currentRoleFilter = "Admin";
            else if (checkedId == R.id.rbCustomer) currentRoleFilter = "Customer";
            else                                   currentRoleFilter = "All";
            applyFilters();
        });
    }

    private void setupSearchView() {
        searchViewUsers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
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
                String name  = user.getName()  != null ? user.getName().toLowerCase()  : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                String phone = user.getPhone() != null ? user.getPhone().toLowerCase() : "";
                if (!name.contains(currentSearchText)
                        && !email.contains(currentSearchText)
                        && !phone.contains(currentSearchText)) {
                    matchText = false;
                }
            }

            if (!currentRoleFilter.equals("All")) {
                String role = user.getRole() != null ? user.getRole() : "";
                if (currentRoleFilter.equals("Admin")    && !role.equalsIgnoreCase("admin"))    matchRole = false;
                if (currentRoleFilter.equals("Customer") &&  role.equalsIgnoreCase("admin"))    matchRole = false;
            }

            if (matchText && matchRole) filteredList.add(user);
        }
        userAdapter.updateList(filteredList);
    }

    private void loadUsersFromApi() {
        progressBarUsers.setVisibility(View.VISIBLE);
        apiService.getAllUsers().enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call,
                                   Response<ApiResponse<List<User>>> response) {
                progressBarUsers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    originalUserList.clear();
                    displayUserList.clear();
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
            }
        });
    }

    // ==========================================================
    //  DIALOG CHI TIẾT NGƯỜI DÙNG (NÂNG CẤP)
    // ==========================================================
    private void showUserDetailsDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // --- Ánh xạ View ---
        ImageView  ivAvatar        = dialogView.findViewById(R.id.ivDialogAvatar);
        TextView   tvStatusDot     = dialogView.findViewById(R.id.tvDialogStatusDot);
        TextView   tvName          = dialogView.findViewById(R.id.tvDialogName);
        TextView   tvEmail         = dialogView.findViewById(R.id.tvDialogEmail);
        TextView   tvRole          = dialogView.findViewById(R.id.tvDialogRole);
        TextView   tvStatus        = dialogView.findViewById(R.id.tvDialogStatus);
        TextView   tvPhone         = dialogView.findViewById(R.id.tvDialogPhone);
        TextView   tvAddress       = dialogView.findViewById(R.id.tvDialogAddress);
        TextView   tvJoinDate      = dialogView.findViewById(R.id.tvDialogJoinDate);
        ProgressBar pbStats        = dialogView.findViewById(R.id.pbDialogStats);
        LinearLayout layoutStats   = dialogView.findViewById(R.id.layoutDialogStats);
        TextView   tvTotalOrders   = dialogView.findViewById(R.id.tvTotalOrders);
        TextView   tvTotalSpent    = dialogView.findViewById(R.id.tvTotalSpent);
        MaterialButton btnToggle   = dialogView.findViewById(R.id.btnDialogToggleLock);
        MaterialButton btnToggleRole = dialogView.findViewById(R.id.btnDialogToggleRole);
        MaterialButton btnClose    = dialogView.findViewById(R.id.btnDialogClose);

        // --- Load Avatar ---
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(ivAvatar);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_launcher_foreground)
                    .transform(new CircleCrop())
                    .into(ivAvatar);
        }

        // --- Thông tin cơ bản ---
        tvName.setText(user.getName() != null && !user.getName().isEmpty()
                ? user.getName() : "Chưa có tên");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa có email");

        // --- SĐT ---
        tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                ? user.getPhone() : "Chưa cập nhật");

        // --- Địa chỉ ---
        tvAddress.setText(user.getAddress() != null && !user.getAddress().isEmpty()
                ? user.getAddress() : "Chưa cập nhật");

        // --- Ngày tham gia (format đẹp) ---
        tvJoinDate.setText(formatJoinDate(user.getCreatedAt()));

        // --- Badge Role ---
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (isAdmin) {
            tvRole.setText("⚙ Admin");
            tvRole.setTextColor(0xFFD32F2F);
            tvRole.setBackgroundColor(0xFFFFCDD2);
        } else {
            tvRole.setText("👤 Khách hàng");
            tvRole.setTextColor(0xFF1976D2);
            tvRole.setBackgroundColor(0xFFBBDEFB);
        }

        // --- Badge Trạng thái + Dot ---
        if (user.isLocked()) {
            tvStatus.setText("Đã bị khóa");
            tvStatus.setTextColor(0xFF757575);
            tvStatus.setBackgroundColor(0xFFEEEEEE);
            tvStatusDot.setBackgroundColor(0xFF9E9E9E);
            tvStatusDot.setText("✕");
        } else {
            tvStatus.setText("Hoạt động");
            tvStatus.setTextColor(0xFF388E3C);
            tvStatus.setBackgroundColor(0xFFC8E6C9);
            tvStatusDot.setBackgroundColor(0xFF388E3C);
            tvStatusDot.setText("✓");
        }

        // --- Nút Khóa / Mở khóa ---
        if (user.isLocked()) {
            btnToggle.setText("Mở khóa tài khoản");
            btnToggle.setBackgroundColor(0xFF388E3C);
        } else {
            btnToggle.setText("Khóa tài khoản");
            btnToggle.setBackgroundColor(0xFFD32F2F);
        }
        
        // --- Nút Cấp quyền ---
        if (isAdmin) {
            btnToggleRole.setText("Hủy quyền Admin");
            btnToggleRole.setBackgroundColor(0xFFF57C00);
        } else {
            btnToggleRole.setText("Cấp quyền Admin");
            btnToggleRole.setBackgroundColor(0xFF388E3C);
        }

        // --- Chặn thao tác với chính bản thân ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && user.getUid() != null && currentUser.getUid().equals(user.getUid())) {
            btnToggle.setEnabled(false);
            btnToggle.setText("Không thể tự khóa mình");
            btnToggle.setBackgroundColor(0xFFBDBDBD);
            
            btnToggleRole.setEnabled(false);
            btnToggleRole.setText("Không thể tự đổi quyền");
            btnToggleRole.setBackgroundColor(0xFFBDBDBD);
        }

        // --- Tải thống kê đơn hàng ---
        if (user.getUid() != null) {
            pbStats.setVisibility(View.VISIBLE);
            layoutStats.setVisibility(View.GONE);

            apiService.getUserStats(user.getUid()).enqueue(new Callback<ApiResponse<UserStats>>() {
                @Override
                public void onResponse(Call<ApiResponse<UserStats>> call,
                                       Response<ApiResponse<UserStats>> response) {
                    pbStats.setVisibility(View.GONE);
                    layoutStats.setVisibility(View.VISIBLE);
                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSuccess() && response.body().getData() != null) {
                        UserStats stats = response.body().getData();
                        tvTotalOrders.setText(String.valueOf(stats.getTotalOrders()));
                        DecimalFormat fmt = new DecimalFormat("###,###,###");
                        tvTotalSpent.setText(fmt.format(stats.getTotalSpent()) + " đ");
                    } else {
                        tvTotalOrders.setText("--");
                        tvTotalSpent.setText("--");
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<UserStats>> call, Throwable t) {
                    pbStats.setVisibility(View.GONE);
                    layoutStats.setVisibility(View.VISIBLE);
                    tvTotalOrders.setText("Lỗi");
                    tvTotalSpent.setText("Lỗi");
                    Log.e("ManageUsers", "getUserStats failed", t);
                }
            });
        } else {
            pbStats.setVisibility(View.GONE);
            layoutStats.setVisibility(View.VISIBLE);
            tvTotalOrders.setText("--");
            tvTotalSpent.setText("--");
        }

        // --- Sự kiện nút ---
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnToggle.setOnClickListener(v -> {
            dialog.dismiss();
            toggleUserLockStatus(user);
        });
        btnToggleRole.setOnClickListener(v -> {
            dialog.dismiss();
            toggleUserRole(user, !isAdmin);
        });

        dialog.show();
    }

    // ==========================================================
    //  KHOÁ / MỞ KHOÁ TÀI KHOẢN
    // ==========================================================
    private void toggleUserLockStatus(User user) {
        if (user.getUid() == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newLockStatus = !user.isLocked();
        progressBarUsers.setVisibility(View.VISIBLE);

        Map<String, Boolean> body = new HashMap<>();
        body.put("isLocked", newLockStatus);

        apiService.toggleUserLock(user.getUid(), body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                progressBarUsers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String msg = newLockStatus ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
                    Toast.makeText(ManageUsersActivity.this, msg, Toast.LENGTH_SHORT).show();
                    loadUsersFromApi(); // Reload danh sách
                } else {
                    Toast.makeText(ManageUsersActivity.this, "Lỗi cập nhật trên Server", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                progressBarUsers.setVisibility(View.GONE);
                Toast.makeText(ManageUsersActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================================
    //  THAY ĐỔI QUYỀN (ROLE)
    // ==========================================================
    private void toggleUserRole(User user, boolean makeAdmin) {
        if (user.getUid() == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        String newRole = makeAdmin ? "admin" : "customer";
        progressBarUsers.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update("role", newRole)
                .addOnCompleteListener(task -> {
                    progressBarUsers.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        String msg = makeAdmin ? "Đã cấp quyền Admin" : "Đã hủy quyền Admin, chuyển về Khách hàng";
                        Toast.makeText(ManageUsersActivity.this, msg, Toast.LENGTH_SHORT).show();
                        loadUsersFromApi(); // Reload
                    } else {
                        Toast.makeText(ManageUsersActivity.this, "Lỗi khi đổi quyền trên Firebase", Toast.LENGTH_SHORT).show();
                        Log.e("ManageUsers", "Update role failed", task.getException());
                    }
                });
    }

    // ==========================================================
    //  TIỆN ÍCH: Format ngày tham gia
    // ==========================================================
    private String formatJoinDate(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return "Không rõ";

        // Thử nhiều format phổ biến
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd"
        };
        for (String fmt : formats) {
            try {
                Date date = new SimpleDateFormat(fmt, Locale.getDefault()).parse(createdAt);
                if (date != null) {
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                }
            } catch (ParseException ignored) { }
        }
        // Fallback: lấy 10 ký tự đầu nếu là ISO string
        return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
    }
}