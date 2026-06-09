package com.namgyun.tamakitchen.ui.shopping;

import com.google.gson.annotations.SerializedName;

public class ShoppingItem {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    /**
     * ✅ 서버 DTO가 quantity 또는 count로 내려올 수 있으니 하나의 필드로 통합 수신
     * - Gson alternate 사용 (Gson 2.8.6+)
     */
    @SerializedName(value = "quantity", alternate = {"count"})
    private Double quantity;

    @SerializedName("price")
    private Integer price;

    @SerializedName("shoppingDate")
    private String shoppingDate;

    @SerializedName("unit")
    private String unit;

    /**
     * ✅ iconKey / iconName 둘 다 대응
     */
    @SerializedName(value = "iconKey", alternate = {"iconName"})
    private String iconKey;

    // ✅ 케이스1: 서버가 storeId/storeName 직접 내려줌
    @SerializedName("storeId")
    private Long storeId;

    @SerializedName("storeName")
    private String storeName;

    // ✅ 케이스2: 서버가 store 객체로 내려줌 (store: {id, name})
    @SerializedName("store")
    private StoreInfo store;

    public static class StoreInfo {
        @SerializedName("id")
        public Long id;
        @SerializedName("name")
        public String name;
    }

    // UI 상태
    private boolean checked;
    private int iconResId;

    // ===== getters/setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() {
        return quantity == null ? 0d : quantity;
    }

    public void setQuantity(double q) {
        this.quantity = q;
    }

    public int getPrice() { return price == null ? 0 : price; }
    public void setPrice(int price) { this.price = price; }

    public String getShoppingDate() { return shoppingDate; }
    public void setShoppingDate(String shoppingDate) { this.shoppingDate = shoppingDate; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }

    // ✅ storeId (storeId/store 객체 둘 다 지원)
    public Long getStoreId() {
        if (storeId != null) return storeId;
        if (store != null) return store.id;
        return null;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
        if (this.store != null) this.store.id = storeId;
    }

    public String getStoreName() {
        if (storeName != null && !storeName.trim().isEmpty()) return storeName;
        if (store != null) return store.name;
        return null;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
        if (this.store != null) this.store.name = storeName;
    }

    public void setStore(StoreInfo store) {
        this.store = store;
        if (store != null) {
            if (this.storeId == null) this.storeId = store.id;
            if (this.storeName == null || this.storeName.trim().isEmpty()) this.storeName = store.name;
        }
    }

    public StoreInfo getStore() { return store; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
}