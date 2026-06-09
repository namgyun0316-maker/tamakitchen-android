package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SharedFridgeResponse {

    /**
     * ✅ fridgeId에는 절대 "id"를 alternate로 넣지 말 것!
     */
    @SerializedName(value = "sharedFridgeId", alternate = {"fridgeId"})
    private long fridgeId;

    @SerializedName(value = "sharedFridgeName", alternate = {"fridgeName", "name"})
    private String fridgeName;

    @SerializedName(value = "inviteCode", alternate = {"code"})
    private String inviteCode;

    @SerializedName(value = "expiresAt", alternate = {"inviteExpiresAt", "inviteExpiresAtMs", "expireAt"})
    private long expiresAt;

    // 서버가 create/join 응답에 넣어주면 여기로 들어옴
    @SerializedName(value = "myNickname", alternate = {"nickname"})
    private String myNickname;

    @SerializedName(value = "myRole", alternate = {"role"})
    private String myRole;

    // ✅ 서버가 create/join/regenerate 응답에 members까지 넣어주면 여기로 들어옴
    @SerializedName("members")
    private List<Member> members;

    public long getFridgeId() { return fridgeId; }
    public String getFridgeName() { return fridgeName; }
    public String getInviteCode() { return inviteCode; }
    public long getExpiresAt() { return expiresAt; }
    public String getMyNickname() { return myNickname; }
    public String getMyRole() { return myRole; }
    public List<Member> getMembers() { return members; }

    // =========================
    // Member DTO (서버 MemberDto와 맞춤)
    // { userId, nickname, role }
    // =========================
    public static class Member {

        @SerializedName(value = "userId", alternate = {"id"})
        private long userId;

        @SerializedName("nickname")
        private String nickname;

        @SerializedName("role")
        private String role;

        public long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }
    }
}
