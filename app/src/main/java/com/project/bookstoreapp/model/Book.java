package com.project.bookstoreapp.model;

import java.io.Serializable;

public class Book implements Serializable {
    private Object bookId;
    private String title;
    private String author;
    private Object categoryId;
    private Object seriesId;
    private double price;
    private double originalPrice;
    private String description;
    private String imageUrl;
    private double stock;
    private double sold;
    private double rating;
    private double reviewCount;
    private String publisher;
    private int publishedYear;
    private boolean isHidden = false;

    public Book() {}

    public Book(String bookId, String title, String author, double price, boolean isHidden) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.isHidden = isHidden;
    }


    public String getBookId() {
        return bookId != null ? String.valueOf(bookId) : "";
    }
    public void setBookId(Object bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    public Object getCategoryId() { return categoryId; }
    public void setCategoryId(Object categoryId) { this.categoryId = categoryId; }

    public Object getSeriesId() { return seriesId; }
    public void setSeriesId(Object seriesId) { this.seriesId = seriesId; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public double getSold() { return sold; }
    public void setSold(double sold) { this.sold = sold; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public double getReviewCount() { return reviewCount; }
    public void setReviewCount(double reviewCount) { this.reviewCount = reviewCount; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }
}