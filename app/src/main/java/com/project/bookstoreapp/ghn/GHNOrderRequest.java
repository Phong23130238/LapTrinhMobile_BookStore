package com.project.bookstoreapp.ghn;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GHNOrderRequest {
    @SerializedName("payment_type_id")
    public int paymentTypeId; // 1: Shop trả phí, 2: Khách trả phí
    
    public String note;
    
    @SerializedName("required_note")
    public String requiredNote; // VD: CHOXEMHANGKHONGTHU
    
    @SerializedName("to_name")
    public String toName;
    
    @SerializedName("to_phone")
    public String toPhone;
    
    @SerializedName("to_address")
    public String toAddress;
    
    @SerializedName("to_ward_code")
    public String toWardCode;
    
    @SerializedName("to_district_id")
    public int toDistrictId;
    
    @SerializedName("cod_amount")
    public long codAmount;
    
    public String content;
    public int weight; // gram
    public int length; // cm
    public int width;  // cm
    public int height; // cm
    
    @SerializedName("service_type_id")
    public int serviceTypeId; // 2: Chuẩn
    
    public List<GHNItem> items;
}
