package com.project.bookstoreapp.model;

import com.google.gson.annotations.SerializedName;

public class BookStat {
    @SerializedName("bookId")
    private String bookId;

    @SerializedName("title")
    private String title;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("price")
    private long price;

    @SerializedName("stock")
    private int stock;

    @SerializedName("soldInMonth")
    private int soldInMonth;

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getSoldInMonth() {
        return soldInMonth;
    }

    public void setSoldInMonth(int soldInMonth) {
        this.soldInMonth = soldInMonth;
    }
}
