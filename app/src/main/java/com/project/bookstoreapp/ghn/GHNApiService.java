package com.project.bookstoreapp.ghn;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface GHNApiService {
    
    @GET("master-data/province")
    Call<GHNResponse<List<Province>>> getProvinces(@Header("Token") String token);
    
    @GET("master-data/district")
    Call<GHNResponse<List<District>>> getDistricts(
        @Header("Token") String token,
        @Query("province_id") int provinceId
    );
    
    @GET("master-data/ward")
    Call<GHNResponse<List<Ward>>> getWards(
        @Header("Token") String token,
        @Query("district_id") int districtId
    );

    @POST("v2/shipping-order/fee")
    Call<GHNResponse<GHNFeeData>> calculateFee(
        @Header("Token") String token,
        @Header("ShopId") int shopId,
        @retrofit2.http.Body GHNFeeRequest request
    );
}
