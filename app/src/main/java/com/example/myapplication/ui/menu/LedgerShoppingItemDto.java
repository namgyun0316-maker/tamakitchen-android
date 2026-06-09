package com.namgyun.tamakitchen.ui.menu;

import com.namgyun.tamakitchen.ui.shopping.ShoppingItem;
import com.google.gson.annotations.SerializedName;

public class LedgerShoppingItemDto {

    @SerializedName("shoppingItemId")
    private Long shoppingItemId;

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

    public static LedgerShoppingItemDto fromShoppingItem(ShoppingItem item) {
        LedgerShoppingItemDto dto = new LedgerShoppingItemDto();
        dto.shoppingItemId = item.getId();
        dto.date = item.getShoppingDate();
        dto.storeId = item.getStoreId();
        dto.storeName = item.getStoreName();
        dto.itemName = item.getName();
        dto.quantity = item.getQuantity() <= 0d ? 1.0d : item.getQuantity();
        dto.unit = toDisplayUnit(item.getUnit());
        dto.unitPrice = item.getPrice();
        dto.totalPrice = Math.max(0, (int) Math.round(item.getPrice() * dto.quantity));
        return dto;
    }

    private static String toDisplayUnit(String serverUnit) {
        if (serverUnit == null || serverUnit.trim().isEmpty()) return "개";

        String u = serverUnit.trim().toUpperCase();

        switch (u) {
            case "EA":
                return "개";
            case "BAG":
                return "봉";
            case "PACK":
                return "팩";
            case "G":
                return "g";
            case "KG":
                return "kg";
            case "ML":
                return "ml";
            case "L":
                return "L";
            default:
                return "개";
        }
    }

    public Long getShoppingItemId() { return shoppingItemId; }
    public String getDate() { return date; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public String getItemName() { return itemName; }
    public Double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Integer getUnitPrice() { return unitPrice; }
    public Integer getTotalPrice() { return totalPrice; }
}