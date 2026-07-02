package com.project.bookstoreapp.model;

public class Category {
    private String categoryId;
    private String name;
    private int bookCount;
    private String imageUrl; // Có thể null theo DB của bạn

    public Category() {}

    public Category(String categoryId, String name, int bookCount) {
        this.categoryId = categoryId;
        this.name = name;
        this.bookCount = bookCount;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}