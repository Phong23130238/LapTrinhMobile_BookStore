package com.project.bookstoreapp.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Order implements Serializable {
    private String orderId; // String theo Firebase
    private String displayId; // Thêm trường này
    private String userId;  // String theo Firebase
    private String status;
    private double totalPrice;
    private double shippingFee;
    private String ghnOrderCode;
    private String shippingAddress;
    private String paymentMethod;
    private String note;
    private String createdAt;
    private String updatedAt;
    private String voucherCode;
    private double discountAmount;
    // Firebase seeder của bạn có list
    private List<String> bookIds;
    private List<Map<String, Object>> items;

    public Order() {}

    public Order(String orderId, String displayId, String userId, String status, double totalPrice, double shippingFee, String ghnOrderCode, String shippingAddress, String paymentMethod, String note, String createdAt, String updatedAt) {
        this.orderId = orderId;
        this.displayId = displayId;
        this.userId = userId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.ghnOrderCode = ghnOrderCode;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getBookIds() {
        return bookIds;
    }

    public void setBookIds(List<String> bookIds) {
        this.bookIds = bookIds;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public String getGhnOrderCode() { return ghnOrderCode; }
    public void setGhnOrderCode(String ghnOrderCode) { this.ghnOrderCode = ghnOrderCode; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
}