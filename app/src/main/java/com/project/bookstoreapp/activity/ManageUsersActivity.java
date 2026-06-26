package com.project.bookstoreapp.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, clickedUser -> {
            String action = clickedUser.isLocked() ? "Mở khóa" : "Khóa";
            Toast.makeText(ManageUsersActivity.this, action + " tài khoản: " + clickedUser.getName(), Toast.LENGTH_SHORT).show();
            // Tương lai: Gọi lệnh db.collection("users").document(clickedUser.getUid()).update("isLocked", !clickedUser.isLocked())
        });
        rvAdminUsers.setAdapter(userAdapter);

        loadUsersFromFirebase();
    }

    private void loadUsersFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    User user = document.toObject(User.class);
                    userList.add(user);
                }
                userAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Lỗi tải Người dùng", Toast.LENGTH_SHORT).show();
                Log.e("Firebase_Error", "Error getting users: ", task.getException());
            }
        });
    }
}