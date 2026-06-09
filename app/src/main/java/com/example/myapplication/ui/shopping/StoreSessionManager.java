package com.namgyun.tamakitchen.ui.shopping;

import android.content.Context;
import android.content.SharedPreferences;

public class StoreSessionManager {

    private static final String PREF = "shopping_store_pref";
    private static final String KEY_STORE_ID = "store_id";
    private static final String KEY_STORE_NAME = "store_name";

    public static void setCurrentStore(Context ctx, Long storeId, String storeName) {
        if (ctx == null) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit()
                .putLong(KEY_STORE_ID, storeId == null ? -1L : storeId)
                .putString(KEY_STORE_NAME, storeName == null ? "" : storeName)
                .apply();
    }

    public static Long getCurrentStoreId(Context ctx) {
        if (ctx == null) return null;
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        long v = sp.getLong(KEY_STORE_ID, -1L);
        return (v <= 0L) ? null : v;
    }

    public static String getCurrentStoreName(Context ctx) {
        if (ctx == null) return "";
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getString(KEY_STORE_NAME, "");
    }

    public static void clear(Context ctx) {
        if (ctx == null) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }
}
