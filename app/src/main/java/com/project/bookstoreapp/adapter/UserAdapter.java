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

        holder.tvUserName.setText(user.getFullName());
        holder.tvUserEmail.setText(user.getEmail());
        holder.tvUserPhone.setText("SĐT: " + user.getPhone());
        holder.tvUserRole.setText(user.getRole());

        // Thay đổi màu sắc dựa trên quyền
        if (user.getRole().equalsIgnoreCase("Admin")) {
            holder.tvUserRole.setBackgroundColor(0xFF2196F3); // Màu xanh dương cho Admin
            holder.tvUserRole.setTextColor(0xFFFFFFFF);
        } else {
            // Giữ màu vàng gold cho khách hàng
            holder.tvUserRole.setBackgroundColor(0xFFFFC107);
            holder.tvUserRole.setTextColor(0xFF000000);
        }

        // Thay đổi màu sắc và chữ dựa trên trạng thái Khóa
        if (user.isLocked()) {
            holder.tvUserStatus.setText("Đã bị khóa");
            holder.tvUserStatus.setBackgroundColor(0xFFD32F2F); // Đỏ cảnh báo
            holder.itemView.setAlpha(0.6f); // Làm mờ thẻ một chút
        } else {
            holder.tvUserStatus.setText("Đang hoạt động");
            holder.tvUserStatus.setBackgroundColor(0xFF388E3C); // Xanh lá
            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
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