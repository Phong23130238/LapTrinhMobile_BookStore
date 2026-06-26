package com.project.bookstoreapp.model;

import java.io.Serializable;

// Thêm implements Serializable để có thể gửi toàn bộ cuốn sách qua Intent
public class Book implements Serializable {
    private int id;
    private String title;
    private String author;
    private double price;
    private int imageResource;
    private boolean isHidden; // Trạng thái Ẩn/Hiện

    public Book(int id, String title, String author, double price, int imageResource, boolean isHidden) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.imageResource = imageResource;
        this.isHidden = isHidden;
    }

    // Các Getter hiện có
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public double getPrice() { return price; }
    public int getImageResource() { return imageResource; }

    // Thêm Getter/Setter cho trạng thái Ẩn
    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }
}