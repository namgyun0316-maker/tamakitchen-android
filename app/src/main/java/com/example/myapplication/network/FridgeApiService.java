package com.namgyun.tamakitchen.network;

import com.namgyun.tamakitchen.ui.fridge.FridgeItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FridgeApiService {

    // =========================================================
    // ✅ 개인 냉장고
    // - GET  /api/fridge/{userId}
    // - POST /api/fridge/{userId}
    // =========================================================
    @GET("api/fridge/{userId}")
    Call<List<FridgeItem>> getFridgeItems(@Path("userId") Long userId);

    // 호환 alias
    @GET("api/fridge/{userId}")
    Call<List<FridgeItem>> getPersonalItems(@Path("userId") Long userId);

    // (기존) FridgeItem 바디로 추가
    @POST("api/fridge/{userId}")
    Call<FridgeItem> addFridgeItem(
            @Path("userId") Long userId,
            @Body FridgeItem item
    );

    // (기존) 호환 alias - FridgeItem 바디
    @POST("api/fridge/{userId}")
    Call<FridgeItem> addPersonalItem(
            @Path("userId") Long userId,
            @Body FridgeItem item
    );

    // ✅✅✅ (추가) FridgeItemCreateRequest 바디로 추가 (영수증 등록용)
    @POST("api/fridge/{userId}")
    Call<FridgeItem> addPersonalItemReq(
            @Path("userId") Long userId,
            @Body FridgeItemCreateRequest req
    );

    // =========================================================
    // ✅ 공동 냉장고
    // - GET  /api/fridge/shared/{fridgeId}?userId=
    // - POST /api/fridge/shared/{fridgeId}?userId=
    // =========================================================
    @GET("api/fridge/shared/{fridgeId}")
    Call<List<FridgeItem>> getSharedFridgeItems(
            @Path("fridgeId") Long fridgeId,
            @Query("userId") Long userId
    );

    // (기존) FridgeItem 바디로 추가
    @POST("api/fridge/shared/{fridgeId}")
    Call<FridgeItem> addSharedFridgeItem(
            @Path("fridgeId") Long fridgeId,
            @Query("userId") Long userId,
            @Body FridgeItem item
    );

    // 호환 alias
    @GET("api/fridge/shared/{fridgeId}")
    Call<List<FridgeItem>> getSharedItems(
            @Path("fridgeId") Long fridgeId,
            @Query("userId") Long userId
    );

    // (기존) 호환 alias - FridgeItem 바디
    @POST("api/fridge/shared/{fridgeId}")
    Call<FridgeItem> addSharedItem(
            @Path("fridgeId") Long fridgeId,
            @Query("userId") Long userId,
            @Body FridgeItem item
    );

    // ✅✅✅ (추가) FridgeItemCreateRequest 바디로 추가 (영수증 등록용)
    @POST("api/fridge/shared/{fridgeId}")
    Call<FridgeItem> addSharedItemReq(
            @Path("fridgeId") Long fridgeId,
            @Query("userId") Long userId,
            @Body FridgeItemCreateRequest req
    );

    // =========================================================
    // ✅ 수정/삭제
    // - PUT    /api/fridge/{itemId}?userId=
    // - DELETE /api/fridge/{itemId}?userId=
    // =========================================================
    @PUT("api/fridge/{itemId}")
    Call<FridgeItem> updateFridgeItem(
            @Path("itemId") Long itemId,
            @Query("userId") Long userId,
            @Body FridgeItem item
    );

    @DELETE("api/fridge/{itemId}")
    Call<Void> deleteFridgeItem(
            @Path("itemId") Long itemId,
            @Query("userId") Long userId
    );

    // 호환 alias
    @DELETE("api/fridge/{itemId}")
    Call<Void> deleteItem(
            @Path("itemId") Long itemId,
            @Query("userId") Long userId
    );

    // =========================================================
    // ✅ userId 없이 호출하던 과거 코드 호환
    // =========================================================
    @PUT("api/fridge/{itemId}")
    Call<FridgeItem> updateFridgeItem(
            @Path("itemId") Long itemId,
            @Body FridgeItem item
    );

    @DELETE("api/fridge/{itemId}")
    Call<Void> deleteFridgeItem(
            @Path("itemId") Long itemId
    );

    // =========================================================
    // ✅ shared-fridge meta
    // =========================================================
    @POST("api/shared-fridge/create")
    Call<SharedFridgeResponse> createSharedFridge(@Body Map<String, Object> body);

    @POST("api/shared-fridge/join")
    Call<SharedFridgeResponse> joinSharedFridge(@Body Map<String, Object> body);

    @POST("api/shared-fridge/{fridgeId}/regenerate")
    Call<SharedFridgeResponse> regenerateSharedFridgeInvite(
            @Path("fridgeId") Long fridgeId,
            @Body Map<String, Object> body
    );

    @GET("api/shared-fridge/{fridgeId}/members")
    Call<List<SharedFridgeMemberDto>> getSharedFridgeMembers(
            @Path("fridgeId") Long fridgeId,
            @Query("userId") Long userId
    );

    @POST("api/shared-fridge/{fridgeId}/transfer-owner")
    Call<Map<String, Object>> transferOwner(
            @Path("fridgeId") Long fridgeId,
            @Body Map<String, Object> body
    );

    @POST("api/shared-fridge/{fridgeId}/leave")
    Call<Map<String, Object>> leaveSharedFridge(
            @Path("fridgeId") Long fridgeId,
            @Body Map<String, Object> body
    );

    @POST("api/shared-fridge/{fridgeId}/kick")
    Call<Map<String, Object>> kickMember(
            @Path("fridgeId") Long fridgeId,
            @Body Map<String, Object> body
    );

    // =========================================================
    // ✅ 유저 탈퇴
    // - DELETE /api/users/{userId}
    // =========================================================
    @DELETE("api/users/{userId}")
    Call<Void> deleteUser(@Path("userId") Long userId);
}