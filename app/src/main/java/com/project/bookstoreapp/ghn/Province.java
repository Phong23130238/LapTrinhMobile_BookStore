package com.project.bookstoreapp.ghn;

import com.google.gson.annotations.SerializedName;

public class Province {
    @SerializedName("ProvinceID")
    public int ProvinceID;

    @SerializedName("ProvinceName")
    public String ProvinceName;

    @Override
    public String toString() {
        return ProvinceName;
    }
}
