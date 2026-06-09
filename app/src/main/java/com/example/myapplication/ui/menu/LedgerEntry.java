package com.namgyun.tamakitchen.ui.menu;

import java.io.Serializable;
import java.util.UUID;

public class LedgerEntry implements Serializable {

    public static final String SOURCE_SHOPPING_AUTO = "SHOPPING_AUTO";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_RECEIPT = "RECEIPT";

    private String id;
    private String date;          // yyyy-MM-dd
    private Long storeId;         // nullable
    private String storeName;     // nullable -> "미지정" 처리
    private String itemName;
    private double quantity;
    private String unit;
    private int unitPrice;
    private int totalPrice;
    private String memo;
    private String sourceType;
    private Long sourceShoppingItemId; // 쇼핑 자동등록 원본 id
    private boolean receiptAttached;
    private long createdAt;

    public LedgerEntry() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public static LedgerEntry createManual(
            String date,
            Long storeId,
            String storeName,
            String itemName,
            double quantity,
            String unit,
            int unitPrice,
            String memo
    ) {
        LedgerEntry entry = new LedgerEntry();
        entry.date = date;
        entry.storeId = storeId;
        entry.storeName = storeName;
        entry.itemName = itemName;
        entry.quantity = quantity;
        entry.unit = unit;
        entry.unitPrice = Math.max(0, unitPrice);
        entry.totalPrice = Math.max(0, (int) Math.round(quantity * unitPrice));
        entry.memo = memo;
        entry.sourceType = SOURCE_MANUAL;
        entry.sourceShoppingItemId = null;
        entry.receiptAttached = false;
        return entry;
    }

    public static LedgerEntry createFromShopping(
            Long shoppingItemId,
            String date,
            Long storeId,
            String storeName,
            String itemName,
            double quantity,
            String unit,
            int unitPrice
    ) {
        LedgerEntry entry = new LedgerEntry();
        entry.date = date;
        entry.storeId = storeId;
        entry.storeName = storeName;
        entry.itemName = itemName;
        entry.quantity = quantity;
        entry.unit = unit;
        entry.unitPrice = Math.max(0, unitPrice);
        entry.totalPrice = Math.max(0, (int) Math.round(quantity * unitPrice));
        entry.memo = "";
        entry.sourceType = SOURCE_SHOPPING_AUTO;
        entry.sourceShoppingItemId = shoppingItemId;
        entry.receiptAttached = false;
        return entry;
    }

    public void recalcTotalPrice() {
        this.totalPrice = Math.max(0, (int) Math.round(this.quantity * this.unitPrice));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && !id.trim().isEmpty()) {
            this.id = id.trim();
        }
    }

    public String getDate() {
        return date == null ? "" : date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return (storeName == null || storeName.trim().isEmpty()) ? "미지정" : storeName.trim();
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getItemName() {
        return itemName == null ? "" : itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQuantity() {
        return quantity <= 0d ? 1.0d : quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        recalcTotalPrice();
    }

    public String getUnit() {
        return (unit == null || unit.trim().isEmpty()) ? "개" : unit.trim();
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getUnitPrice() {
        return Math.max(0, unitPrice);
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = Math.max(0, unitPrice);
        recalcTotalPrice();
    }

    public int getTotalPrice() {
        return Math.max(0, totalPrice);
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = Math.max(0, totalPrice);
    }

    public String getMemo() {
        return memo == null ? "" : memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getSourceType() {
        return (sourceType == null || sourceType.trim().isEmpty()) ? SOURCE_MANUAL : sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceShoppingItemId() {
        return sourceShoppingItemId;
    }

    public void setSourceShoppingItemId(Long sourceShoppingItemId) {
        this.sourceShoppingItemId = sourceShoppingItemId;
    }

    public boolean isReceiptAttached() {
        return receiptAttached;
    }

    public void setReceiptAttached(boolean receiptAttached) {
        this.receiptAttached = receiptAttached;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    private Long serverId;

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

}