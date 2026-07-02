package com.project.bookstoreapp.ghn;

import com.google.gson.annotations.SerializedName;

public class GHNItem {
    public String name;
    public String code;
    public int quantity;
    public int price;
    public int length;
    public int width;
    public int height;
    public int weight;
    
    public GHNItem(String name, String code, int quantity, int price, int weight) {
        this.name = name;
        this.code = code;
        this.quantity = quantity;
        this.price = price;
        this.weight = weight;
        this.length = 20; // Default
        this.width = 15;
        this.height = 10;
    }
}
