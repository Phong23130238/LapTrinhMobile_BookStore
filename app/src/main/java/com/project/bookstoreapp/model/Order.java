package com.project.bookstoreapp.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Order implements Serializable {
    private String orderId; // String theo Firebase
    private String userId;  // String theo Firebase
    private String status;
    private double totalPrice;
    private double shippingFee;
    private String shippingAddress;
    private String paymentMethod;
    private String note;
    private String createdAt;
    private String updatedAt;
    // Firebase seeder của bạn có list
    private List<String> bookIds;
    private List<Map<String, Object>> items;

    public Order() {}

    public Order(String orderId, String userId, String status, double totalPrice, double shippingFee, String shippingAddress, String paymentMethod, String note, String createdAt, String updatedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public double getShippingFee() { return shippingFee; }
    public String getShippingAddress() { return shippingAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public List<String> getBookIds() { return bookIds; }
    public List<Map<String, Object>> getItems() { return items; }
}