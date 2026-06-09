package com.namgyun.tamakitchen.ui.shopping;

import com.google.gson.annotations.SerializedName;

public class FridgeItemRequest {

    @SerializedName("userId")
    private Long userId;

    @SerializedName("name")
    private String name;

    @SerializedName("storage")
    private String storage;

    @SerializedName("quantity")
    private int quantity;

    // 서버가 iconName이면 그대로, iconKey면 @SerializedName("iconKey")로 변경
    @SerializedName("iconName")
    private String iconName;

    public FridgeItemRequest() {}

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getStorage() { return storage; }
    public int getQuantity() { return quantity; }
    public String getIconName() { return iconName; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setStorage(String storage) { this.storage = storage; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}
