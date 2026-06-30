package com.project.bookstoreapp.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
                        db.collection("categories").document(key).set(map, SetOptions.merge());
                        
                        catMap.put(key, cat.optString("name"));
                    }
                    Log.d(TAG, "Categories seeded from JSON");
                }

                // 2. Parse SERIES to a map for easy lookup AND seed to Firestore
                Map<String, String> seriesMap = new HashMap<>();
                if (root.has("SERIES")) {
                    JSONObject series = root.getJSONObject("SERIES");
                    Iterator<String> serKeys = series.keys();
                    while (serKeys.hasNext()) {
                        String key = serKeys.next();
                        JSONObject ser = series.getJSONObject(key);
                        seriesMap.put(key, ser.optString("name"));
                        
                        // Đẩy lên Firestore
                        Map<String, Object> map = new HashMap<>();
                        map.put("seriesId", key);
                        map.put("name", ser.optString("name"));
                        map.put("author", ser.optString("author"));
                        map.put("category", ser.optInt("category"));
                        map.put("totalVolumes", ser.optInt("totalVolumes"));
                        map.put("status", ser.optString("status"));
                        if (ser.has("description")) {
                            map.put("description", ser.optString("description"));
                        }
                        
                        // Parse bookIds sang List<String>
                        String bookIdsStr = ser.optString("bookIds", "[]");
                        List<String> bookIdsList = new ArrayList<>();
                        try {
                            if (!bookIdsStr.startsWith("[")) {
                                bookIdsList.add(bookIdsStr);
                            } else {
                                JSONArray bArray = new JSONArray(bookIdsStr);
                                for (int i = 0; i < bArray.length(); i++) {
                                    bookIdsList.add(String.valueOf(bArray.get(i)));
                                }
                            }
                        } catch (Exception e) {}
                        map.put("bookIds", bookIdsList);
                        
                        db.collection("series").document(key).set(map, SetOptions.merge());
                    }
                    Log.d(TAG, "Series seeded from JSON");
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

                        db.collection("books").document(key).set(map, SetOptions.merge());
                    }
                    Log.d(TAG, "Books seeded from JSON");
                }
                
                // 4. Seed Users
                if (root.has("USERS")) {
                    JSONObject users = root.getJSONObject("USERS");
                    Iterator<String> userKeys = users.keys();
                    while (userKeys.hasNext()) {
                        String key = userKeys.next();
                        JSONObject user = users.getJSONObject(key);
                        Map<String, Object> map = new HashMap<>();
                        map.put("uid", key);
                        map.put("name", user.optString("name"));
                        map.put("email", user.optString("email"));
                        map.put("phone", user.optString("phone")); // optString gets string from number too
                        map.put("address", user.optString("address"));
                        map.put("role", user.optString("role", "customer"));
                        map.put("createdAt", user.optString("createdAt"));
                        if (user.has("avatarUrl")) {
                            map.put("avatarUrl", user.optString("avatarUrl"));
                        }
                        db.collection("users").document(key).set(map, SetOptions.merge());
                    }
                    Log.d(TAG, "Users seeded from JSON");
                }

                // 5. Seed Orders
                if (root.has("ORDERS")) {
                    JSONObject orders = root.getJSONObject("ORDERS");
                    Iterator<String> orderKeys = orders.keys();
                    while (orderKeys.hasNext()) {
                        String key = orderKeys.next();
                        JSONObject order = orders.getJSONObject(key);
                        Map<String, Object> map = new HashMap<>();
                        map.put("orderId", key);
                        map.put("userId", order.optString("userId"));
                        map.put("status", order.optString("status"));
                        map.put("totalPrice", order.optDouble("totalPrice", 0));
                        map.put("shippingFee", order.optDouble("shippingFee", 0));
                        map.put("shippingAddress", order.optString("shippingAddress"));
                        map.put("paymentMethod", order.optString("paymentMethod"));
                        map.put("note", order.optString("note"));
                        map.put("createdAt", order.optString("createdAt"));
                        map.put("updatedAt", order.optString("updatedAt"));
                        
                        // Parse bookIds string to array
                        String bookIdsStr = order.optString("bookIds", "[]");
                        List<String> bookIdsList = new ArrayList<>();
                        try {
                            if (!bookIdsStr.startsWith("[")) {
                                bookIdsList.add(bookIdsStr); // For cases like "58"
                            } else {
                                JSONArray bArray = new JSONArray(bookIdsStr);
                                for (int i = 0; i < bArray.length(); i++) {
                                    bookIdsList.add(String.valueOf(bArray.get(i)));
                                }
                            }
                        } catch (Exception e) {}
                        map.put("bookIds", bookIdsList);
                        
                        // Parse items string to array of maps
                        String itemsStr = order.optString("items", "[]");
                        List<Map<String, Object>> itemsList = new ArrayList<>();
                        try {
                            if (itemsStr.startsWith("[")) {
                                JSONArray iArray = new JSONArray(itemsStr);
                                for (int i = 0; i < iArray.length(); i++) {
                                    JSONObject itemObj = iArray.getJSONObject(i);
                                    Map<String, Object> itemMap = new HashMap<>();
                                    Iterator<String> keys = itemObj.keys();
                                    while (keys.hasNext()) {
                                        String k = keys.next();
                                        itemMap.put(k, itemObj.get(k));
                                    }
                                    if (itemMap.containsKey("bookId")) {
                                        itemMap.put("bookId", String.valueOf(itemMap.get("bookId")));
                                    }
                                    itemsList.add(itemMap);
                                }
                            }
                        } catch (Exception e) {}
                        map.put("items", itemsList);

                        db.collection("orders").document(key).set(map, SetOptions.merge());
                    }
                    Log.d(TAG, "Orders seeded from JSON");
                }
                
                Log.d(TAG, "Full data seeding completed successfully!");

            } catch (Exception e) {
                Log.e(TAG, "Error seeding from JSON", e);
            }
        }).start();
    }
}
