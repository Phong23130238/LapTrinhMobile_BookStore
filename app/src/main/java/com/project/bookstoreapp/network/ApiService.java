package com.project.bookstoreapp.network;

import com.project.bookstoreapp.model.Review;
import java.util.HashMap;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @Headers("Bypass-Tunnel-Reminder: true")
    @GET("api/reviews/{bookId}")
    Call<ApiResponse<List<Review>>> getReviews(@Path("bookId") String bookId);

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/reviews/check-purchase")
    Call<ApiResponse<Void>> checkPurchase(@Body HashMap<String, Object> body);

    @Headers("Bypass-Tunnel-Reminder: true")
    @POST("api/reviews")
    Call<ApiResponse<Void>> submitReview(@Body HashMap<String, Object> body);
}
