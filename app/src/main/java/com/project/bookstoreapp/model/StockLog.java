package com.project.bookstoreapp.model;

import java.io.Serializable;

public class StockLog implements Serializable {
    private String logId;
    private String bookId;
    private String bookTitle;
    // "import" | "sold" | "returned"
    private String changeType;
    private int    delta;          // +N hoặc -N
    // "receipt" | "order"
    private String sourceType;
    private String sourceId;       // PN-001 hoặc mã đơn hàng
    private String sourceDisplay;  // "Phiếu nhập kho #PN-001"
    private String createdAt;      // ISO timestamp

    public StockLog() {}

    public StockLog(String bookId, String bookTitle, String changeType,
                    int delta, String sourceType, String sourceId, String sourceDisplay) {
        this.bookId        = bookId;
        this.bookTitle     = bookTitle;
        this.changeType    = changeType;
        this.delta         = delta;
        this.sourceType    = sourceType;
        this.sourceId      = sourceId;
        this.sourceDisplay = sourceDisplay;
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
        this.createdAt = sdf.format(new java.util.Date());
    }

    // Getters & Setters
    public String getLogId()                           { return logId; }
    public void   setLogId(String logId)               { this.logId = logId; }
    public String getBookId()                          { return bookId; }
    public void   setBookId(String bookId)             { this.bookId = bookId; }
    public String getBookTitle()                       { return bookTitle; }
    public void   setBookTitle(String bookTitle)       { this.bookTitle = bookTitle; }
    public String getChangeType()                      { return changeType; }
    public void   setChangeType(String changeType)     { this.changeType = changeType; }
    public int    getDelta()                           { return delta; }
    public void   setDelta(int delta)                  { this.delta = delta; }
    public String getSourceType()                      { return sourceType; }
    public void   setSourceType(String sourceType)     { this.sourceType = sourceType; }
    public String getSourceId()                        { return sourceId; }
    public void   setSourceId(String sourceId)         { this.sourceId = sourceId; }
    public String getSourceDisplay()                   { return sourceDisplay; }
    public void   setSourceDisplay(String sourceDisplay){ this.sourceDisplay = sourceDisplay; }
    public String getCreatedAt()                       { return createdAt; }
    public void   setCreatedAt(String createdAt)       { this.createdAt = createdAt; }
}
