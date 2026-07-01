package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.User;

import java.util.ArrayList;
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

    public void updateList(List<User> newList) {
        this.userList = new ArrayList<>(newList);
        notifyDataSetChanged();
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

        // 1. Gán text thông tin cơ bản (Bảo vệ Null)
        holder.tvUserName.setText(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Chưa cập nhật tên");
        holder.tvUserEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "Chưa có"));
        holder.tvUserPhone.setText("SĐT: " + (user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa có"));

        // 2. Gán ngày tham gia (Cắt chuỗi nếu định dạng ISO quá dài)
        String createdAt = user.getCreatedAt();
        if (createdAt != null && createdAt.length() >= 10) {
            holder.tvUserDate.setText("Tham gia: " + createdAt.substring(0, 10));
        } else {
            holder.tvUserDate.setText("Tham gia: Không rõ");
        }

        // 3. Load Avatar bằng Glide (Cắt hình tròn)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else {
            // Ảnh mặc định nếu user chưa có avatar
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_launcher_foreground) // Đổi thành tên icon mặc định trong drawable của bạn nếu cần
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        }

        // 4. Xử lý màu sắc phân quyền và trạng thái khóa
        if (user.isLocked()) {
            // TÀI KHOẢN BỊ KHÓA: Chuyển toàn bộ màu sang xám, làm mờ thẻ
            holder.tvUserStatus.setText("Đã bị khóa");
            holder.tvUserStatus.setTextColor(0xFF757575);       // Chữ Xám
            holder.tvUserStatus.setBackgroundColor(0xFFE0E0E0); // Nền Xám nhạt

            holder.tvUserRole.setText(user.getRole() != null ? user.getRole() : "Khách hàng");
            holder.tvUserRole.setTextColor(0xFF757575);
            holder.tvUserRole.setBackgroundColor(0xFFE0E0E0);

            holder.itemView.setAlpha(0.6f); // Làm mờ toàn bộ thẻ
        } else {
            // ĐANG HOẠT ĐỘNG
            holder.tvUserStatus.setText("Đang hoạt động");
            holder.tvUserStatus.setTextColor(0xFF388E3C);       // Chữ Xanh lá
            holder.tvUserStatus.setBackgroundColor(0xFFC8E6C9); // Nền Xanh lá nhạt
            holder.itemView.setAlpha(1.0f); // Hiện rõ thẻ

            // Phân quyền (Đỏ cho Admin, Xanh dương cho Khách hàng)
            if ("admin".equalsIgnoreCase(user.getRole())) {
                holder.tvUserRole.setText("Admin");
                holder.tvUserRole.setTextColor(0xFFD32F2F);       // Chữ Đỏ
                holder.tvUserRole.setBackgroundColor(0xFFFFCDD2); // Nền Đỏ nhạt
            } else {
                holder.tvUserRole.setText("Khách hàng");
                holder.tvUserRole.setTextColor(0xFF1976D2);       // Chữ Xanh dương
                holder.tvUserRole.setBackgroundColor(0xFFBBDEFB); // Nền Xanh dương nhạt
            }
        }

        // 5. Sự kiện Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName, tvUserEmail, tvUserPhone, tvUserDate, tvUserRole, tvUserStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
            tvUserDate = itemView.findViewById(R.id.tvUserDate);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
        }
    }
}