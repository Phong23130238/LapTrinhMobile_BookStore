package com.project.bookstoreapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminStatsResponse {
    @SerializedName("totalSoldQty")
    private int totalSoldQty;

    @SerializedName("totalRevenue")
    private long totalRevenue;

    @SerializedName("totalInventory")
    private int totalInventory;

    @SerializedName("topBooks")
    private List<BookStat> topBooks;

    @SerializedName("unsoldBooks")
    private List<BookStat> unsoldBooks;

    public int getTotalSoldQty() {
        return totalSoldQty;
    }

    public void setTotalSoldQty(int totalSoldQty) {
        this.totalSoldQty = totalSoldQty;
    }

    public long getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(long totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalInventory() {
        return totalInventory;
    }

    public void setTotalInventory(int totalInventory) {
        this.totalInventory = totalInventory;
    }

    public List<BookStat> getTopBooks() {
        return topBooks;
    }

    public void setTopBooks(List<BookStat> topBooks) {
        this.topBooks = topBooks;
    }

    public List<BookStat> getUnsoldBooks() {
        return unsoldBooks;
    }

    public void setUnsoldBooks(List<BookStat> unsoldBooks) {
        this.unsoldBooks = unsoldBooks;
    }
}
