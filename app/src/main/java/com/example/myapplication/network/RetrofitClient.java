package com.namgyun.tamakitchen.network;

import retrofit2.Retrofit;

public class RetrofitClient {

    private RetrofitClient() {}

    public static Retrofit getRetrofit() {
        return FridgeApi.getClient();
    }

    public static ShoppingApi getShoppingApi() {
        return getRetrofit().create(ShoppingApi.class);
    }

    public static UploadApiService getUploadApi() {
        return getRetrofit().create(UploadApiService.class);
    }

    public static FridgeApiService getFridgeApi() {
        return getRetrofit().create(FridgeApiService.class);
    }

    public static RecipeApiService getRecipeApi() {
        return getRetrofit().create(RecipeApiService.class);
    }

    public static SupportApiService getSupportApi() {
        return getRetrofit().create(SupportApiService.class);
    }

    public static LedgerApi getLedgerApi() {
        return getRetrofit().create(LedgerApi.class);
    }
}