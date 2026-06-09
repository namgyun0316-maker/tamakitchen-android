package com.namgyun.tamakitchen.ui.collection;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class CollectionUnlockPrefs {

    private static final String PREFS_NAME = "collection_unlock_prefs";
    private static final String KEY_UNLOCKED_SET = "unlocked_collection_set";

    private CollectionUnlockPrefs() {}

    public static boolean isUnlocked(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> unlockedSet = prefs.getStringSet(KEY_UNLOCKED_SET, new HashSet<>());
        return unlockedSet != null && unlockedSet.contains(key);
    }

    public static void unlock(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> unlockedSet = new HashSet<>(prefs.getStringSet(KEY_UNLOCKED_SET, new HashSet<>()));
        unlockedSet.add(key);
        prefs.edit().putStringSet(KEY_UNLOCKED_SET, unlockedSet).apply();
    }

    public static void lock(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> unlockedSet = new HashSet<>(prefs.getStringSet(KEY_UNLOCKED_SET, new HashSet<>()));
        unlockedSet.remove(key);
        prefs.edit().putStringSet(KEY_UNLOCKED_SET, unlockedSet).apply();
    }
}