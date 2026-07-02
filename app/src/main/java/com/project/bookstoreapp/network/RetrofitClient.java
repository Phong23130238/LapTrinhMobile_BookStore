package com.project.bookstoreapp.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.bookstoreapp.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // IP được đọc từ local.properties của mỗi máy (không commit lên git).
    // Emulator: SERVER_IP=10.0.2.2
    // Điện thoại thật: SERVER_IP=192.168.x.x (IP máy tính trong mạng LAN)
    private static final String BASE_URL = "http://" + BuildConfig.SERVER_IP + ":3000/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}
