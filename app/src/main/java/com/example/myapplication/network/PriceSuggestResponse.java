package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

public class PriceSuggestResponse {

    public static class PriceInfo {
        @SerializedName("storeId")
        public Long storeId;

        @SerializedName("storeName")
        public String storeName;

        @SerializedName("price")
        public Integer price;

        @SerializedName("daysAgo")
        public Integer daysAgo;
    }

    public static class CheaperInfo {
        @SerializedName("storeId")
        public Long storeId;

        @SerializedName("storeName")
        public String storeName;

        @SerializedName("price")
        public Integer price;

        @SerializedName("daysAgo")
        public Integer daysAgo;

        @SerializedName("diff")
        public Integer diff;
    }

    @SerializedName("currentStoreRecent")
    public PriceInfo currentStoreRecent;

    @SerializedName("cheaperElsewhere")
    public CheaperInfo cheaperElsewhere;
}
