package com.project.bookstoreapp.ghn;

import com.google.gson.annotations.SerializedName;

public class Ward {
    @SerializedName("WardCode")
    public String WardCode;

    @SerializedName("DistrictID")
    public int DistrictID;

    @SerializedName("WardName")
    public String WardName;

    @Override
    public String toString() {
        return WardName;
    }
}
