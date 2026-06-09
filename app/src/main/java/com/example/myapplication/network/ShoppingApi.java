// File: app/src/main/java/com/example/myapplication/network/ShoppingApi.java
package com.namgyun.tamakitchen.network;

import com.namgyun.tamakitchen.ui.shopping.ShoppingItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ShoppingApi {

    // =========================
    // ✅ Shopping Items
    // =========================
    @GET("shopping/{userId}")
    Call<List<ShoppingItem>> getItems(@Path("userId") Long userId);

    @POST("shopping/add")
    Call<ShoppingItem> addItem(@Body ShoppingItemRequest request);

    @PUT("shopping/{itemId}")
    Call<ShoppingItem> updateItem(
            @Path("itemId") Long itemId,
            @Body ShoppingItemRequest request
    );

    @DELETE("shopping/{itemId}")
    Call<Void> deleteItem(@Path("itemId") Long itemId);

    // =========================
    // ✅ Stores
    // =========================
    @GET("stores/recent")
    Call<List<StoreResponse>> getRecentStores(@Query("userId") Long userId);

    @GET("stores/search")
    Call<List<StoreResponse>> searchStores(
            @Query("userId") Long userId,
            @Query("query") String query
    );

    @POST("stores")
    Call<StoreResponse> createStore(@Body StoreCreateRequest request);

    // ✅ 판매점 삭제 추가
    // 예: DELETE /stores/{storeId}?userId=1
    @DELETE("stores/{storeId}")
    Call<Void> deleteStore(
            @Path("storeId") Long storeId,
            @Query("userId") Long userId
    );

    // =========================
    // ✅ Price Suggest
    // =========================
    @GET("prices/suggest")
    Call<PriceSuggestResponse> suggestPrice(
            @Query("userId") Long userId,
            @Query("name") String name,
            @Query("unit") String unit,
            @Query("storeId") Long storeId
    );
}
