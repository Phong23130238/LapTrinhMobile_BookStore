package com.project.bookstoreapp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CartItem implements Parcelable {

    private String bookId;
    private String title;
    private String author;
    private String imageUrl;
    private long price;
    private long originalPrice;
    private int quantity;
    private String addedAt;
    private boolean isSelected = false;

    public CartItem() {}

    public CartItem(String bookId, String title, String author,
                    String imageUrl, long price, long originalPrice,
                    int quantity, String addedAt) {
        this.bookId        = bookId;
        this.title         = title;
        this.author        = author;
        this.imageUrl      = imageUrl;
        this.price         = price;
        this.originalPrice = originalPrice;
        this.quantity      = quantity;
        this.addedAt       = addedAt;
    }

    protected CartItem(Parcel in) {
        bookId        = in.readString();
        title         = in.readString();
        author        = in.readString();
        imageUrl      = in.readString();
        price         = in.readLong();
        originalPrice = in.readLong();
        quantity      = in.readInt();
        addedAt       = in.readString();
        isSelected    = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bookId);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(imageUrl);
        dest.writeLong(price);
        dest.writeLong(originalPrice);
        dest.writeInt(quantity);
        dest.writeString(addedAt);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<CartItem> CREATOR = new Creator<CartItem>() {
        @Override
        public CartItem createFromParcel(Parcel in) { return new CartItem(in); }
        @Override
        public CartItem[] newArray(int size) { return new CartItem[size]; }
    };

    public String getBookId()            { return bookId; }
    public void   setBookId(String v)    { this.bookId = v; }
    public String getTitle()             { return title; }
    public void   setTitle(String v)     { this.title = v; }
    public String getAuthor()            { return author; }
    public void   setAuthor(String v)    { this.author = v; }
    public String getImageUrl()          { return imageUrl; }
    public void   setImageUrl(String v)  { this.imageUrl = v; }
    public long   getPrice()             { return price; }
    public void   setPrice(long v)       { this.price = v; }
    public long   getOriginalPrice()     { return originalPrice; }
    public void   setOriginalPrice(long v){ this.originalPrice = v; }
    public int    getQuantity()          { return quantity; }
    public void   setQuantity(int v)     { this.quantity = v; }
    public String getAddedAt()           { return addedAt; }
    public void   setAddedAt(String v)   { this.addedAt = v; }
    public boolean isSelected()          { return isSelected; }
    public void    setSelected(boolean v){ this.isSelected = v; }
    public long   getSubtotal()          { return price * quantity; }
}