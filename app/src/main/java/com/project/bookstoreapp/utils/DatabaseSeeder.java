package com.project.bookstoreapp.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DatabaseSeeder {

    private static final String TAG = "DatabaseSeeder";

    public static void seedDataFromJson(Context context) {
        new Thread(() -> {
            try {
                InputStream is = context.getAssets().open("db_firebase.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String jsonStr = new String(buffer, "UTF-8");

                JSONObject root = new JSONObject(jsonStr);
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 1. Seed Categories
                Map<String, String> catMap = new HashMap<>();
                if (root.has("CATEGORIES")) {
                    JSONObject categories = root.getJSONObject("CATEGORIES");
                    Iterator<String> catKeys = categories.keys();
                    while (catKeys.hasNext()) {
                        String key = catKeys.next();
                        JSONObject cat = categories.getJSONObject(key);
                        Map<String, Object> map = new HashMap<>();
                        map.put("categoryId", key);
                        map.put("name", cat.optString("name"));
                        map.put("bookCount", cat.optInt("bookCount", 0));
                        if (cat.has("imageUrl")) {
                            map.put("imageUrl", cat.optString("imageUrl"));
                        }
                        db.collection("categories").document(key).set(map);
                        
                        catMap.put(key, cat.optString("name"));
                    }
                    Log.d(TAG, "Categories seeded from JSON");
                }

                // 2. Parse SERIES to a map for easy lookup
                Map<String, String> seriesMap = new HashMap<>();
                if (root.has("SERIES")) {
                    JSONObject series = root.getJSONObject("SERIES");
                    Iterator<String> serKeys = series.keys();
                    while (serKeys.hasNext()) {
                        String key = serKeys.next();
                        JSONObject ser = series.getJSONObject(key);
                        seriesMap.put(key, ser.optString("name"));
                    }
                }

                // 3. Seed Books
                if (root.has("BOOKS")) {
                    JSONObject books = root.getJSONObject("BOOKS");
                    Iterator<String> bookKeys = books.keys();
                    while (bookKeys.hasNext()) {
                        String key = bookKeys.next();
                        JSONObject book = books.getJSONObject(key);
                        Map<String, Object> map = new HashMap<>();
                        map.put("bookId", key);
                        map.put("title", book.optString("title"));
                        map.put("author", book.optString("author"));
                        map.put("price", book.optDouble("price", 0));
                        map.put("originalPrice", book.optDouble("originalPrice", 0));
                        map.put("description", book.optString("description"));
                        map.put("imageUrl", book.optString("imageUrl"));
                        map.put("stock", book.optInt("stock", 0));
                        map.put("sold", book.optInt("sold", 0));
                        map.put("rating", book.optDouble("rating", 0));
                        map.put("reviewCount", book.optInt("reviewCount", 0));
                        if (book.has("publisher")) {
                            map.put("publisher", book.optString("publisher"));
                        }
                        if (book.has("publishedYear")) {
                            map.put("publishedYear", book.optInt("publishedYear"));
                        }
                        
                        // category string lookup
                        String catId = String.valueOf(book.optInt("categoryId"));
                        if (catMap.containsKey(catId)) {
                            map.put("category", catMap.get(catId));
                        } else {
                            map.put("category", "");
                        }

                        // series string lookup
                        if (book.has("seriesId")) {
                            String serId = String.valueOf((int)book.optDouble("seriesId")); 
                            if (seriesMap.containsKey(serId)) {
                                map.put("series", seriesMap.get(serId));
                            }
                        }
                        if (book.has("volume")) {
                            map.put("volume", book.optInt("volume"));
                        }
                        
                        map.put("createdAt", new java.util.Date());

                        db.collection("books").document(key).set(map);
                    }
                    Log.d(TAG, "Books seeded from JSON");
                }
                
                Log.d(TAG, "Full data seeding completed successfully!");

            } catch (Exception e) {
                Log.e(TAG, "Error seeding from JSON", e);
            }
        }).start();
    }
}
