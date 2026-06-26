package com.project.bookstoreapp.model;

import java.io.Serializable;

public class Order implements Serializable {
    private String orderId;       // Mã đơn hàng (VD: #DH1001)
    private String customerName;  // Tên khách hàng đặt
    private double totalAmount;   // Tổng tiền đơn hàng
    private String orderDate;     // Ngày đặt hàng
    private String paymentStatus; // Trạng thái thanh toán (Liên quan VNPay: Chưa thanh toán / Đã thanh toán)
    private String shippingStatus;// Trạng thái vận chuyển (Liên quan GHN: Chờ xác nhận / Đang giao / Đã giao)
    private String ghnTrackingCode; // Mã vận đơn của Giao Hàng Nhanh (nếu có)

    public Order(String orderId, String customerName, double totalAmount, String orderDate,
                 String paymentStatus, String shippingStatus, String ghnTrackingCode) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.paymentStatus = paymentStatus;
        this.shippingStatus = shippingStatus;
        this.ghnTrackingCode = ghnTrackingCode;
    }

    // Các hàm Getter/Setter
    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public double getTotalAmount() { return totalAmount; }
    public String getOrderDate() { return orderDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getShippingStatus() { return shippingStatus; }
    public void setShippingStatus(String shippingStatus) { this.shippingStatus = shippingStatus; }
    public String getGhnTrackingCode() { return ghnTrackingCode; }
    public void setGhnTrackingCode(String ghnTrackingCode) { this.ghnTrackingCode = ghnTrackingCode; }
}