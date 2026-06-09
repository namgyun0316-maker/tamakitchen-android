package com.namgyun.tamakitchen.network;

public class FridgeItemCreateRequest {

    private String name;
    private String storage;
    private Double quantity;
    private String expiryDate;
    private int iconResId;
    private String unit;
    private String iconName;
    private String addedByNickname;

    // ✅ 기본 생성자 추가
    public FridgeItemCreateRequest() {
    }

    public FridgeItemCreateRequest(String name,
                                   String storage,
                                   Double quantity,
                                   String expiryDate,
                                   int iconResId,
                                   String unit,
                                   String iconName,
                                   String addedByNickname) {
        this.name = name;
        this.storage = storage;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.iconResId = iconResId;
        this.unit = unit;
        this.iconName = iconName;
        this.addedByNickname = addedByNickname;
    }

    public String getName() {
        return name;
    }

    public String getStorage() {
        return storage;
    }

    public Double getQuantity() {
        return quantity;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getUnit() {
        return unit;
    }

    public String getIconName() {
        return iconName;
    }

    public String getAddedByNickname() {
        return addedByNickname;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public void setAddedByNickname(String addedByNickname) {
        this.addedByNickname = addedByNickname;
    }
}