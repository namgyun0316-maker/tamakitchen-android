package com.namgyun.tamakitchen.ui.collection;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CollectionInventoryPrefs {

    private static final String PREFS_NAME = "collection_inventory_prefs";
    private static final String KEY_OWNED_SET = "owned_item_set";
    private static final String KEY_INGREDIENT_INSTANCE_SET = "ingredient_instance_set";

    private static final String INSTANCE_PREFIX = "ingredient_instance";

    private CollectionInventoryPrefs() {}

    // =========================
    // Generic single-key inventory (egg, food)
    // =========================
    public static boolean hasItem(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> ownedSet = prefs.getStringSet(KEY_OWNED_SET, new HashSet<>());
        return ownedSet != null && ownedSet.contains(key);
    }

    public static void addItem(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> ownedSet = new HashSet<>(prefs.getStringSet(KEY_OWNED_SET, new HashSet<>()));
        ownedSet.add(key);
        prefs.edit().putStringSet(KEY_OWNED_SET, ownedSet).apply();
    }

    public static void removeItem(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> ownedSet = new HashSet<>(prefs.getStringSet(KEY_OWNED_SET, new HashSet<>()));
        ownedSet.remove(key);
        prefs.edit().putStringSet(KEY_OWNED_SET, ownedSet).apply();
    }

    // =========================
    // Ingredient instance inventory
    // =========================
    public static String addIngredientInstance(Context context, String baseKey) {
        if (baseKey == null || baseKey.trim().isEmpty()) return null;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> raw = new HashSet<>(prefs.getStringSet(KEY_INGREDIENT_INSTANCE_SET, new HashSet<>()));

        String instanceKey = buildInstanceKey(baseKey);
        raw.add(instanceKey);

        prefs.edit().putStringSet(KEY_INGREDIENT_INSTANCE_SET, raw).apply();
        CollectionPetStatePrefs.ensureState(context, instanceKey);

        return instanceKey;
    }

    public static boolean removeIngredientInstance(Context context, String instanceKey) {
        if (!isIngredientInstanceKey(instanceKey)) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> raw = new HashSet<>(prefs.getStringSet(KEY_INGREDIENT_INSTANCE_SET, new HashSet<>()));

        boolean removed = raw.remove(instanceKey);
        if (!removed) return false;

        prefs.edit().putStringSet(KEY_INGREDIENT_INSTANCE_SET, raw).apply();

        if (CollectionDisplayPrefs.isSelected(context, instanceKey)) {
            CollectionDisplayPrefs.clearSelectedItem(context);
        }

        return true;
    }

    public static List<String> getIngredientInstanceKeys(Context context, String baseKey) {
        List<String> result = new ArrayList<>();
        if (baseKey == null || baseKey.trim().isEmpty()) return result;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_INGREDIENT_INSTANCE_SET, new HashSet<>());
        if (raw == null || raw.isEmpty()) return result;

        for (String value : raw) {
            if (value == null) continue;
            String parsedBase = getBaseKeyFromInstanceKey(value);
            if (baseKey.equals(parsedBase)) {
                result.add(value);
            }
        }

        return result;
    }

    public static int getIngredientInstanceCount(Context context, String baseKey) {
        return getIngredientInstanceKeys(context, baseKey).size();
    }

    public static boolean hasIngredientInstance(Context context, String baseKey) {
        return getIngredientInstanceCount(context, baseKey) > 0;
    }

    public static boolean isIngredientInstanceKey(String key) {
        return key != null && key.startsWith(INSTANCE_PREFIX + "|");
    }

    public static String getBaseKeyFromInstanceKey(String instanceKey) {
        if (!isIngredientInstanceKey(instanceKey)) return null;

        String[] parts = instanceKey.split("\\|");
        if (parts.length < 3) return null;

        return parts[1];
    }

    public static List<String> getAllIngredientInstanceKeys(Context context) {
        List<String> result = new ArrayList<>();

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_INGREDIENT_INSTANCE_SET, new HashSet<>());
        if (raw == null || raw.isEmpty()) return result;

        result.addAll(raw);
        return result;
    }

    private static String buildInstanceKey(String baseKey) {
        return INSTANCE_PREFIX
                + "|"
                + baseKey
                + "|"
                + System.currentTimeMillis()
                + "|"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}