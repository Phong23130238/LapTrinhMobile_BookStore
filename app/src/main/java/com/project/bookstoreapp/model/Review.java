package com.project.bookstoreapp.model;

public class Review {
    private String reviewId;
    private String bookId;
    private String userId;
    private String orderId;
    private String userName;
    private float rating;
    private String comment;
    // API trả về string ISO: "2026-06-26T08:50:00.000Z"
    private String createdAt;

    public Review() {
    }

    public Review(String reviewId, String bookId, String userId, String orderId, String userName, float rating, String comment, String createdAt) {
        this.reviewId = reviewId;
        this.bookId = bookId;
        this.userId = userId;
        this.orderId = orderId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
