package com.project.bookstoreapp.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid; // Đổi thành String theo Firebase
    private String name;
    private String email;
    private String password; // Hash MD5, có thể null (tài khoản Google)
    private String phone;
    private String address;
    private String role;
    private String createdAt;
    private String avatarUrl;
    private boolean isLocked = false; // Thuộc tính riêng cho UI Admin

    public User() {} // Bắt buộc cho Firebase

    public User(String uid, String name, String email, String phone, String address, String role, String createdAt, boolean isLocked) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.createdAt = createdAt;
        this.isLocked = isLocked;
    }

    public User(String uid, String name, String email, String password, String phone, String address, String role,
                String createdAt, String avatarUrl, boolean isLocked) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.createdAt = createdAt;
        this.avatarUrl = avatarUrl;
        this.isLocked = isLocked;
    }



    // Getters & Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}