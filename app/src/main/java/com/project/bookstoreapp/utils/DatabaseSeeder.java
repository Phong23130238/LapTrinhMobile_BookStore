package com.project.bookstoreapp.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DatabaseSeeder {

    private static final String TAG = "DatabaseSeeder";

    public static void seedData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        seedCategories(db);
        seedBooks(db);
        // Note: You can add seedUsers(), seedOrders() etc. here if needed
    }

    private static void seedCategories(FirebaseFirestore db) {
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("name", "Văn học");
        cat1.put("imageUrl", "");
        cat1.put("bookCount", 2);

        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("name", "Kỹ năng sống");
        cat2.put("imageUrl", "");
        cat2.put("bookCount", 1);

        db.collection("categories").add(cat1);
        db.collection("categories").add(cat2);
        Log.d(TAG, "Categories seeded");
    }

    private static void seedBooks(FirebaseFirestore db) {
        // Book 1: Harry Potter series (Tập 1)
        Map<String, Object> book1 = new HashMap<>();
        book1.put("title", "Harry Potter và Hòn Đá Phù Thủy");
        book1.put("author", "J.K. Rowling");
        book1.put("category", "Văn học");
        book1.put("series", "Harry Potter");
        book1.put("volume", 1);
        book1.put("price", 85000);
        book1.put("originalPrice", 120000);
        book1.put("description", "Tập đầu tiên của bộ truyện phép thuật nổi tiếng...");
        book1.put("imageUrl", "https://upload.wikimedia.org/wikipedia/vi/a/a3/Harry_Potter_v%C3%A0_H%C3%B2n_%C4%91%C3%A1_Ph%C3%B9_th%E1%BB%A7y.jpg");
        book1.put("stock", 50);
        book1.put("sold", 12);
        book1.put("rating", 4.5);
        book1.put("reviewCount", 128);
        book1.put("publisher", "NXB Trẻ");
        book1.put("publishedYear", 2023);
        book1.put("createdAt", new java.util.Date());

        // Book 2: Harry Potter series (Tập 2)
        Map<String, Object> book2 = new HashMap<>();
        book2.put("title", "Harry Potter và Phòng Chứa Bí Mật");
        book2.put("author", "J.K. Rowling");
        book2.put("category", "Văn học");
        book2.put("series", "Harry Potter");
        book2.put("volume", 2);
        book2.put("price", 90000);
        book2.put("originalPrice", 125000);
        book2.put("description", "Tập thứ hai của bộ truyện...");
        book2.put("imageUrl", "https://upload.wikimedia.org/wikipedia/vi/7/7c/Harry_Potter_v%C3%A0_Ph%C3%B2ng_ch%E1%BB%A9a_B%C3%AD_m%E1%BA%ADt.jpg");
        book2.put("stock", 40);
        book2.put("sold", 8);
        book2.put("rating", 4.8);
        book2.put("reviewCount", 95);
        book2.put("publisher", "NXB Trẻ");
        book2.put("publishedYear", 2023);
        book2.put("createdAt", new java.util.Date());

        // Book 3: Standalone book (Không có series)
        Map<String, Object> book3 = new HashMap<>();
        book3.put("title", "Đắc Nhân Tâm");
        book3.put("author", "Dale Carnegie");
        book3.put("category", "Kỹ năng sống");
        // Không thêm series và volume
        book3.put("price", 75000);
        book3.put("originalPrice", 90000);
        book3.put("description", "Cuốn sách về nghệ thuật thu phục lòng người...");
        book3.put("imageUrl", "https://upload.wikimedia.org/wikipedia/vi/3/33/Dacnhantam.jpg");
        book3.put("stock", 100);
        book3.put("sold", 320);
        book3.put("rating", 5.0);
        book3.put("reviewCount", 500);
        book3.put("publisher", "NXB Tổng Hợp");
        book3.put("publishedYear", 2020);
        book3.put("createdAt", new java.util.Date());

        db.collection("books").add(book1);
        db.collection("books").add(book2);
        db.collection("books").add(book3);
        
        Log.d(TAG, "Books seeded with Series!");
    }
}
