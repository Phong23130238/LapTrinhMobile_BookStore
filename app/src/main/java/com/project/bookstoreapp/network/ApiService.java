package com.project.bookstoreapp.network;

import com.project.bookstoreapp.model.Order;
import com.project.bookstoreapp.model.Review;
import com.project.bookstoreapp.model.User;
import java.util.HashMap;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    // ===== AUTH APIs =====

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/auth/register")
    Call<ApiResponse<User>> register(@Body HashMap<String, Object> body);

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/auth/login")
    Call<ApiResponse<User>> login(@Body HashMap<String, Object> body);

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/auth/google")
    Call<ApiResponse<User>> googleLogin(@Body HashMap<String, Object> body);

    // ===== USER PROFILE APIs =====

    @Multipart
    @Headers("Bypass-Tunnel-Reminder: true")
    @PUT("api/users/profile")
    Call<ApiResponse<User>> updateProfile(
            @Part("uid") RequestBody uid,
            @Part("name") RequestBody name,
            @Part("phone") RequestBody phone,
            @Part("address") RequestBody address,
            @Part MultipartBody.Part avatar
    );

    @Headers("Bypass-Tunnel-Reminder: true")
    @PUT("api/users/password")
    Call<ApiResponse<Void>> updatePassword(@Body HashMap<String, Object> body);

    // ===== REVIEW APIs =====

    @Headers("Bypass-Tunnel-Reminder: true")
    @GET("api/reviews/{bookId}")
    Call<ApiResponse<List<Review>>> getReviews(@Path("bookId") String bookId);

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/reviews/check-purchase")
    Call<ApiResponse<Void>> checkPurchase(@Body HashMap<String, Object> body);

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/reviews")
    Call<ApiResponse<Void>> submitReview(@Body HashMap<String, Object> body);

    // ===== ORDER APIs =====

    @Headers("Bypass-Tunnel-Reminder: true")
    @GET("api/orders/{orderId}")
    Call<ApiResponse<Order>> getOrderDetails(@Path("orderId") String orderId);
}
