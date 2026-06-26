package com.project.bookstoreapp.network;

import com.project.bookstoreapp.model.Review;
import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    // Thuộc tính riêng cho check-purchase
    private boolean canReview;
    private String orderId;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public boolean isCanReview() { return canReview; }
    public String getOrderId() { return orderId; }
}
