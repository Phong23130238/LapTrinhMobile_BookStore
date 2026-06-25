package com.project.bookstoreapp.model;

import java.io.Serializable;

public class User implements Serializable {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String role; // Ví dụ: "Admin" hoặc "Khách hàng"
    private boolean isLocked; // Trạng thái khóa tài khoản: true (bị khóa), false (hoạt động)

    public User(int userId, String fullName, String email, String phone, String role, boolean isLocked) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.isLocked = isLocked;
    }

    // Các hàm Getter/Setter
    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}