package com.example.homerent.model; // Đảm bảo đúng package

import com.google.firebase.Timestamp; // Nếu bạn muốn lưu thời gian tạo

public class User {
    private String userId; // ID của người dùng (thường là UID từ Firebase Auth)
    private String name;   // Tên hiển thị
    private String email;  // Email đăng nhập
    private String avatarUrl; // URL ảnh đại diện (từ Firebase Storage)
    private String phoneNumber; // Số điện thoại (Tùy chọn)
    private String role; // Vai trò: "landlord" hoặc "tenant" (Tùy chọn, hữu ích cho phân quyền)
    private Timestamp createdAt; // Thời gian tạo tài khoản (Tùy chọn)

    // Constructor mặc định - BẮT BUỘC cho Firebase Firestore
    public User() {
    }

    public User(String userId, String name, String email, String avatarUrl, String phoneNumber, String role, Timestamp createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.createdAt = createdAt;
    }

// Getters and Setters - BẮT BUỘC cho Firebase Firestore

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}