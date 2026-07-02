package com.project.bookstoreapp.ghn;

import com.google.gson.annotations.SerializedName;

public class GHNOrderData {
    @SerializedName("order_code")
    public String orderCode;
    
    @SerializedName("expected_delivery_time")
    public String expectedDeliveryTime;
}
