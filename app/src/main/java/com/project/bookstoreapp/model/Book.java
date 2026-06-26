package com.project.bookstoreapp.model;

import java.io.Serializable;

public class Book implements Serializable {
    private String bookId; // Đổi thành String
    private String title;
    private String author;
    private String category; // Đã map tên danh mục theo seeder
    private String series;
    private double price;
    private double originalPrice;
    private String description;
    private String imageUrl;
    private int stock;
    private int sold;
    private double rating;
    private int reviewCount;
    private String publisher;
    private int publishedYear;
    private boolean isHidden = false; // Chức năng Admin

    public Book() {}

    // Bảo toàn cho code cũ nếu cần
    public Book(String bookId, String title, String author, double price, boolean isHidden) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.isHidden = isHidden;
    }

    // Getters & Setters cơ bản
    public String getId() { return bookId; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }
}