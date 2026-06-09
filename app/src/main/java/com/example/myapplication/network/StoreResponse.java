package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

public class StoreResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    // =========================
    // ✅ 기본 생성자 (Gson 필수)
    // =========================
    public StoreResponse() {}

    // =========================
    // ✅ 생성자 추가 (직접 생성용)
    // =========================
    public StoreResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // =========================
    // ✅ Getter
    // =========================
    public Long getId() { return id; }
    public String getName() { return name; }

    // =========================
    // ✅ Setter (다이얼로그에서 사용)
    // =========================
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}