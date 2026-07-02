package com.project.bookstoreapp.model;

public class UploadResponse {
    private boolean success;
    private String message;
    private String imageUrl;

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getImageUrl() { return imageUrl; }
}