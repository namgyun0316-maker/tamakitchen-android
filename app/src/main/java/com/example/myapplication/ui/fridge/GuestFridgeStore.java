package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GuestFridgeStore {

    private static final String PREFS_NAME = "guest_local_store";
    private static final String KEY_GUEST_FRIDGE_ITEMS = "guest_fridge_items_json";

    private static final Gson gson = new GsonBuilder().create();
    private static final Type LIST_TYPE = new TypeToken<List<FridgeItem>>() {}.getType();

    private GuestFridgeStore() {}

    public static List<FridgeItem> load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = p.getString(KEY_GUEST_FRIDGE_ITEMS, "");
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<FridgeItem> list = gson.fromJson(json, LIST_TYPE);
            return (list != null) ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void save(Context context, List<FridgeItem> items) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(items != null ? items : new ArrayList<>());
        p.edit().putString(KEY_GUEST_FRIDGE_ITEMS, json).apply();
    }

    public static void clear(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().remove(KEY_GUEST_FRIDGE_ITEMS).apply();
    }
}
