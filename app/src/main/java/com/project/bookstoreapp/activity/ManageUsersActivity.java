package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.UserAdapter;
import com.project.bookstoreapp.model.User;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvAdminUsers;
    private UserAdapter userAdapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        rvAdminUsers = findViewById(R.id.rvAdminUsers);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManageUsers);

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Mock Data người dùng
        userList = new ArrayList<>();
        userList.add(new User(1, "Quản Trị Viên", "admin@bookstore.com", "0999999999", "Admin", false));
        userList.add(new User(2, "Nguyễn Văn A", "nva@gmail.com", "0901111222", "Khách hàng", false));
        userList.add(new User(3, "Trần Thị B", "ttb_spam@gmail.com", "0903333444", "Khách hàng", true)); // User bị khóa

        // Gắn sự kiện click để Admin xử lý khóa/mở khóa tài khoản
        userAdapter = new UserAdapter(userList, clickedUser -> {
            String action = clickedUser.isLocked() ? "Mở khóa" : "Khóa";
            Toast.makeText(ManageUsersActivity.this, action + " tài khoản: " + clickedUser.getFullName(), Toast.LENGTH_SHORT).show();
            // Tương lai: Bật một Dialog xác nhận, nếu OK thì gọi API Node.js/Firebase cập nhật trạng thái
        });

        rvAdminUsers.setAdapter(userAdapter);
    }
}