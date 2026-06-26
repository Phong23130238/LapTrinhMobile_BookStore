package com.project.bookstoreapp.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid; // Đổi thành String theo Firebase
    private String name;
    private String email;
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

    // Getters & Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}