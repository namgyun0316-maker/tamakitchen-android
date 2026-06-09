package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

public class StoreCreateRequest {

    @SerializedName("userId")
    private Long userId;

    @SerializedName("name")
    private String name;

    public StoreCreateRequest(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
}
