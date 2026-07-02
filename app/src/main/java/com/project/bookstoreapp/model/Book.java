package com.project.bookstoreapp.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class Book implements Serializable {
    private String bookId;
    private String title;
    private String author;
    private String categoryId; // Đổi sang String
    private String seriesId;   // Đổi sang String
    private long price;        // Đổi sang long (phù hợp với VNĐ)
    private long originalPrice;// Đổi sang long
    private String description;
    private String imageUrl;
    private int stock;         // Đổi sang int (số lượng tồn kho)
    private int sold;          // Đổi sang int (số lượng đã bán)
    private double rating;
    private int reviewCount;   // Đổi sang int
    private String publisher;
    private int publishedYear;
    private boolean isHidden = false;

    // Annotation này giúp Firebase tự động lấy giờ máy chủ khi tạo/lưu tài liệu
    @ServerTimestamp
    private Date createdAt;

    public Book() {}

    // Constructor rút gọn
    public Book(String bookId, String title, String author, long price, boolean isHidden) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.isHidden = isHidden;
    }

    // Các Getters và Setters
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSeriesId() { return seriesId; }
    public void setSeriesId(String seriesId) { this.seriesId = seriesId; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}