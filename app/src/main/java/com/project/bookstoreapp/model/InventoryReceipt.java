package com.project.bookstoreapp.model;

import java.io.Serializable;

public class InventoryReceipt implements Serializable {
    private String receiptId;
    private String receiptDisplayId; // VD: PN-001
    private String bookId;
    private String bookTitle;
    private String bookImageUrl;
    private int quantity;
    private long costPrice;    // Giá vốn / cuốn
    private long totalCost;    // costPrice × quantity
    private String importDate; // "yyyy-MM-dd"
    private String note;
    private String createdAt;  // ISO timestamp
    private String type;       // "import"

    public InventoryReceipt() {}

    public InventoryReceipt(String bookId, String bookTitle, String bookImageUrl,
                            int quantity, long costPrice, String importDate, String note) {
        this.bookId       = bookId;
        this.bookTitle    = bookTitle;
        this.bookImageUrl = bookImageUrl;
        this.quantity     = quantity;
        this.costPrice    = costPrice;
        this.totalCost    = costPrice * quantity;
        this.importDate   = importDate;
        this.note         = note;
        this.type         = "import";
        this.createdAt    = new java.util.Date().toInstant().toString();
    }

    // Getters & Setters
    public String getReceiptId()                         { return receiptId; }
    public void   setReceiptId(String receiptId)         { this.receiptId = receiptId; }
    public String getReceiptDisplayId()                  { return receiptDisplayId; }
    public void   setReceiptDisplayId(String id)         { this.receiptDisplayId = id; }
    public String getBookId()                            { return bookId; }
    public void   setBookId(String bookId)               { this.bookId = bookId; }
    public String getBookTitle()                         { return bookTitle; }
    public void   setBookTitle(String bookTitle)         { this.bookTitle = bookTitle; }
    public String getBookImageUrl()                      { return bookImageUrl; }
    public void   setBookImageUrl(String bookImageUrl)   { this.bookImageUrl = bookImageUrl; }
    public int    getQuantity()                          { return quantity; }
    public void   setQuantity(int quantity)              { this.quantity = quantity; }
    public long   getCostPrice()                         { return costPrice; }
    public void   setCostPrice(long costPrice)           { this.costPrice = costPrice; }
    public long   getTotalCost()                         { return totalCost; }
    public void   setTotalCost(long totalCost)           { this.totalCost = totalCost; }
    public String getImportDate()                        { return importDate; }
    public void   setImportDate(String importDate)       { this.importDate = importDate; }
    public String getNote()                              { return note; }
    public void   setNote(String note)                   { this.note = note; }
    public String getCreatedAt()                         { return createdAt; }
    public void   setCreatedAt(String createdAt)         { this.createdAt = createdAt; }
    public String getType()                              { return type; }
    public void   setType(String type)                   { this.type = type; }
}
