package com.project.bookstoreapp.ghn;

import com.google.gson.annotations.SerializedName;

public class District {
    @SerializedName("DistrictID")
    public int DistrictID;

    @SerializedName("ProvinceID")
    public int ProvinceID;

    @SerializedName("DistrictName")
    public String DistrictName;

    @Override
    public String toString() {
        return DistrictName;
    }
}
