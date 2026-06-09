package com.namgyun.tamakitchen.ui.collection;

import android.content.Context;
import android.content.SharedPreferences;

public class CollectionDisplayPrefs {

    private static final String PREFS_NAME = "collection_display_prefs";
    private static final String KEY_SELECTED_ITEM_KEY = "selected_item_key";

    private CollectionDisplayPrefs() {}

    public static void saveSelectedItem(Context context, String itemKey) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_ITEM_KEY, itemKey).apply();
    }

    public static String getSelectedItemKey(Context context) {
        if (context == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_ITEM_KEY, null);
    }

    public static void clearSelectedItem(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_SELECTED_ITEM_KEY).apply();
    }

    public static boolean isSelected(Context context, String itemKey) {
        String selected = getSelectedItemKey(context);
        return selected != null && selected.equals(itemKey);
    }

    public static void clearSelectedItemIfMatches(Context context, String itemKey) {
        if (context == null || itemKey == null || itemKey.trim().isEmpty()) return;

        String selected = getSelectedItemKey(context);
        if (itemKey.equals(selected)) {
            clearSelectedItem(context);
        }
    }
}