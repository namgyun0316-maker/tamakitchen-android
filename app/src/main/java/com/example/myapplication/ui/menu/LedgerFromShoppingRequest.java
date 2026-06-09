package com.namgyun.tamakitchen.ui.menu;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class LedgerFromShoppingRequest {

    @SerializedName("userId")
    private Long userId;

    @SerializedName("items")
    private List<LedgerShoppingItemDto> items = new ArrayList<>();

    public LedgerFromShoppingRequest(Long userId, List<LedgerShoppingItemDto> items) {
        this.userId = userId;
        if (items != null) this.items = items;
    }

    public Long getUserId() {
        return userId;
    }

    public List<LedgerShoppingItemDto> getItems() {
        return items;
    }
}