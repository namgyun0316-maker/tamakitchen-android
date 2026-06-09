package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

public class ShoppingItemRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("count")
    private double count;

    // 서버는 iconName으로 받음
    @SerializedName("iconName")
    private String iconName;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("price")
    private int price;

    @SerializedName("shoppingDate")
    private String shoppingDate;

    @SerializedName("unit")
    private String unit;

    // ✅ 판매점
    @SerializedName("storeId")
    private Long storeId;

    public ShoppingItemRequest() {}

    // ✅ 풀 생성자
    public ShoppingItemRequest(
            String name,
            double count,
            String iconName,
            Long userId,
            int price,
            String shoppingDate,
            String unit,
            Long storeId
    ) {
        this.name = name;
        this.count = count;
        this.iconName = iconName;
        this.userId = userId;
        this.price = price;
        this.shoppingDate = shoppingDate;
        this.unit = normalizeUnitForServer(unit);
        this.storeId = storeId;
    }

    // ✅ 기존 호환 생성자 (storeId=null)
    public ShoppingItemRequest(
            String name,
            double count,
            String iconName,
            Long userId,
            int price,
            String shoppingDate,
            String unit
    ) {
        this(name, count, iconName, userId, price, shoppingDate, unit, null);
    }

    // ✅ 기존 코드 호환(단위 기본 "개")
    public ShoppingItemRequest(
            String name,
            double count,
            String iconName,
            Long userId,
            int price,
            String shoppingDate
    ) {
        this(name, count, iconName, userId, price, shoppingDate, "개", null);
    }

    // ========= getters =========
    public String getName() { return name; }
    public double getCount() { return count; }
    public String getIconName() { return iconName; }
    public Long getUserId() { return userId; }
    public int getPrice() { return price; }
    public String getShoppingDate() { return shoppingDate; }
    public String getUnit() { return unit; }
    public Long getStoreId() { return storeId; }

    // ========= setters =========
    public void setName(String name) { this.name = name; }
    public void setCount(double count) { this.count = count; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setPrice(int price) { this.price = price; }
    public void setShoppingDate(String shoppingDate) { this.shoppingDate = shoppingDate; }
    public void setUnit(String unit) { this.unit = normalizeUnitForServer(unit); }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    private String normalizeUnitForServer(String rawUnit) {
        if (rawUnit == null) return "EA";

        String unit = rawUnit.trim();
        if (unit.isEmpty()) return "EA";

        String lower = unit.toLowerCase();

        if (lower.equals("개") || lower.equals("ea")) return "EA";
        if (lower.equals("봉") || lower.equals("bag")) return "BAG";
        if (lower.equals("팩") || lower.equals("pack")) return "PACK";
        if (lower.equals("g") || lower.equals("그램")) return "G";
        if (lower.equals("kg") || lower.equals("킬로") || lower.equals("킬로그램")) return "KG";
        if (lower.equals("ml")) return "ML";
        if (lower.equals("l") || lower.equals("리터")) return "L";

        return "EA";
    }
}