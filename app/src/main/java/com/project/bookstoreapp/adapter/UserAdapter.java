package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.User;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;

        // 1. BẢO VỆ NULL: Tên người dùng
        String name = user.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Chưa cập nhật tên";
        }
        holder.tvUserName.setText(name);

        // 2. BẢO VỆ NULL: Email
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = "Chưa có email";
        }
        holder.tvUserEmail.setText(email);

        // 3. BẢO VỆ NULL: Số điện thoại
        String phone = user.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            holder.tvUserPhone.setText("SĐT: Chưa cập nhật");
        } else {
            holder.tvUserPhone.setText("SĐT: " + phone);
        }

        // Lệnh check "admin".equalsIgnoreCase(user.getRole()) của bạn đã
        // rất an toàn với NullPointerException rồi, giữ nguyên!
        if ("admin".equalsIgnoreCase(user.getRole())) {
            holder.tvUserRole.setText("Quản trị viên");
            holder.tvUserRole.setBackgroundColor(0xFF2196F3);
            holder.tvUserRole.setTextColor(0xFFFFFFFF);
        } else {
            holder.tvUserRole.setText("Khách hàng");
            holder.tvUserRole.setBackgroundColor(0xFFFFC107);
            holder.tvUserRole.setTextColor(0xFF000000);
        }

        // Trạng thái Khóa
        if (user.isLocked()) {
            holder.tvUserStatus.setText("Đã bị khóa");
            holder.tvUserStatus.setBackgroundColor(0xFFD32F2F);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.tvUserStatus.setText("Đang hoạt động");
            holder.tvUserStatus.setBackgroundColor(0xFF388E3C);
            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvUserPhone, tvUserRole, tvUserStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
        }
    }
}