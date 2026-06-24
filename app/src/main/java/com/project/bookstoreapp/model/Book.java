package com.project.bookstoreapp.model;

public class Book {
    private int id;
    private String title;
    private String author;
    private double price;
    private int imageResource; // Dùng int để lưu tạm hình ảnh có sẵn trong thư mục drawable/mipmap

    public Book(int id, String title, String author, double price, int imageResource) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.imageResource = imageResource;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public double getPrice() { return price; }
    public int getImageResource() { return imageResource; }
}