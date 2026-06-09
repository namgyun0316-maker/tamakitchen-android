package com.namgyun.tamakitchen.ui.menu;

import com.google.gson.annotations.SerializedName;

public class LedgerEntryDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("date")
    private String date;

    @SerializedName("storeId")
    private Long storeId;

    @SerializedName("storeName")
    private String storeName;

    @SerializedName("itemName")
    private String itemName;

    @SerializedName("quantity")
    private Double quantity;

    @SerializedName("unit")
    private String unit;

    @SerializedName("unitPrice")
    private Integer unitPrice;

    @SerializedName("totalPrice")
    private Integer totalPrice;

    @SerializedName("memo")
    private String memo;

    @SerializedName("sourceType")
    private String sourceType;

    @SerializedName("sourceShoppingItemId")
    private Long sourceShoppingItemId;

    @SerializedName("receiptAttached")
    private Boolean receiptAttached;

    @SerializedName("createdAt")
    private String createdAt;

    public static LedgerEntryDto fromLocal(Long userId, LedgerEntry entry) {
        LedgerEntryDto dto = new LedgerEntryDto();
        dto.userId = userId;
        dto.date = entry.getDate();
        dto.storeId = entry.getStoreId();
        dto.storeName = entry.getStoreName();
        dto.itemName = entry.getItemName();
        dto.quantity = entry.getQuantity();
        dto.unit = entry.getUnit();
        dto.unitPrice = entry.getUnitPrice();
        dto.totalPrice = entry.getTotalPrice();
        dto.memo = entry.getMemo();
        dto.sourceType = entry.getSourceType();
        dto.sourceShoppingItemId = entry.getSourceShoppingItemId();
        dto.receiptAttached = entry.isReceiptAttached();
        return dto;
    }

    public LedgerEntry toLocal() {
        LedgerEntry entry = new LedgerEntry();
        if (id != null) entry.setServerId(id);
        entry.setDate(date);
        entry.setStoreId(storeId);
        entry.setStoreName(storeName);
        entry.setItemName(itemName);
        entry.setQuantity(quantity == null ? 1.0d : quantity);
        entry.setUnit(unit);
        entry.setUnitPrice(unitPrice == null ? 0 : unitPrice);
        entry.setTotalPrice(totalPrice == null ? 0 : totalPrice);
        entry.setMemo(memo);
        entry.setSourceType(sourceType);
        entry.setSourceShoppingItemId(sourceShoppingItemId);
        entry.setReceiptAttached(receiptAttached != null && receiptAttached);
        return entry;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getDate() { return date; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public String getItemName() { return itemName; }
    public Double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Integer getUnitPrice() { return unitPrice; }
    public Integer getTotalPrice() { return totalPrice; }
    public String getMemo() { return memo; }
    public String getSourceType() { return sourceType; }
    public Long getSourceShoppingItemId() { return sourceShoppingItemId; }
    public Boolean getReceiptAttached() { return receiptAttached; }
    public String getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setDate(String date) { this.date = date; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }
    public void setMemo(String memo) { this.memo = memo; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setSourceShoppingItemId(Long sourceShoppingItemId) { this.sourceShoppingItemId = sourceShoppingItemId; }
    public void setReceiptAttached(Boolean receiptAttached) { this.receiptAttached = receiptAttached; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}