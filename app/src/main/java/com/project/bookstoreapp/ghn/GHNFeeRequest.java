package com.project.bookstoreapp.ghn;

public class GHNFeeRequest {
    public int from_district_id;
    public String from_ward_code;
    public int service_id;
    public int service_type_id;
    public int to_district_id;
    public String to_ward_code;
    public int height;
    public int length;
    public int weight;
    public int width;
    public int insurance_value;
    public String coupon;

    public GHNFeeRequest(int from_district_id, String from_ward_code, int to_district_id, String to_ward_code) {
        this.from_district_id = from_district_id;
        this.from_ward_code = from_ward_code;
        this.service_type_id = 2; // Giao chuẩn
        this.to_district_id = to_district_id;
        this.to_ward_code = to_ward_code;
        this.height = 10;
        this.length = 20;
        this.weight = 500;
        this.width = 15;
        this.insurance_value = 0;
        this.coupon = null;
    }
}
