package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

public class SharedFridgeMemberDto {

    @SerializedName("id")
    public long id;

    // ✅ 서버가 DTO로 바꾸면 아래 userId를 포함해서 내려주도록 할 것
    @SerializedName(value = "userId", alternate = {"user_id"})
    public long userId;

    @SerializedName("nickname")
    public String nickname;

    @SerializedName("role")
    public String role;
}
