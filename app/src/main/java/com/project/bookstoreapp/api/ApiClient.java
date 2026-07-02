package com.project.bookstoreapp.api;

import com.project.bookstoreapp.network.ApiService;
import com.project.bookstoreapp.network.RetrofitClient;

public class ApiClient {
    private static ApiService apiService;

    public static ApiService getApiService() {
        if (apiService == null) {
            // Dùng RetrofitClient của bạn để build, sau đó tạo service
            apiService = RetrofitClient.getClient().create(ApiService.class);
        }
        return apiService;
    }
}