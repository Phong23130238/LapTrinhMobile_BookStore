package com.project.bookstoreapp.model;

public class Voucher {

    private String code;
    private int discountPercent;
    private long maxDiscount;
    private long minOrderValue;
    private boolean isActive;
    private String expiredAt;

    public Voucher() {}


    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public long getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(long maxDiscount) { this.maxDiscount = maxDiscount; }

    public long getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(long minOrderValue) { this.minOrderValue = minOrderValue; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getExpiredAt() { return expiredAt; }
    public void setExpiredAt(String expiredAt) { this.expiredAt = expiredAt; }

    public long calculateDiscount(long orderTotal) {
        long discount = (long) (orderTotal * discountPercent / 100.0);
        return Math.min(discount, maxDiscount);
    }
}
