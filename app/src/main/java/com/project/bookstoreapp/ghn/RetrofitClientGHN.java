package com.project.bookstoreapp.ghn;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientGHN {
    private static final String BASE_URL = "https://dev-online-gateway.ghn.vn/shiip/public-api/";
    private static Retrofit retrofit = null;

    public static GHNApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(GHNApiService.class);
    }
}
