package com.namgyun.tamakitchen.ui.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.namgyun.tamakitchen.ui.shopping.ShoppingItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LedgerLocalStore {

    private static final String PREF_NAME = "ledger_local_store";
    private static final String KEY_ENTRIES = "entries_json";

    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<LedgerEntry>>() {}.getType();

    private LedgerLocalStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static List<LedgerEntry> getAll(Context context) {
        if (context == null) return new ArrayList<>();

        String json = prefs(context).getString(KEY_ENTRIES, "[]");
        List<LedgerEntry> list;
        try {
            list = gson.fromJson(json, LIST_TYPE);
        } catch (Exception e) {
            list = new ArrayList<>();
        }

        if (list == null) list = new ArrayList<>();
        sortDesc(list);
        return list;
    }

    public static void saveAll(Context context, List<LedgerEntry> entries) {
        if (context == null) return;
        List<LedgerEntry> safe = (entries == null) ? new ArrayList<>() : new ArrayList<>(entries);
        sortDesc(safe);
        prefs(context).edit().putString(KEY_ENTRIES, gson.toJson(safe)).apply();
    }

    public static void addEntry(Context context, LedgerEntry entry) {
        if (context == null || entry == null) return;
        List<LedgerEntry> list = getAll(context);
        list.add(entry);
        saveAll(context, list);
    }

    public static void updateEntry(Context context, LedgerEntry entry) {
        if (context == null || entry == null || TextUtils.isEmpty(entry.getId())) return;

        List<LedgerEntry> list = getAll(context);
        for (int i = 0; i < list.size(); i++) {
            LedgerEntry old = list.get(i);
            if (old != null && entry.getId().equals(old.getId())) {
                list.set(i, entry);
                saveAll(context, list);
                return;
            }
        }

        list.add(entry);
        saveAll(context, list);
    }

    public static void deleteEntry(Context context, String entryId) {
        if (context == null || TextUtils.isEmpty(entryId)) return;

        List<LedgerEntry> list = getAll(context);
        List<LedgerEntry> next = new ArrayList<>();

        for (LedgerEntry item : list) {
            if (item == null) continue;
            if (!entryId.equals(item.getId())) {
                next.add(item);
            }
        }

        saveAll(context, next);
    }

    public static boolean containsShoppingSource(Context context, Long shoppingItemId) {
        if (context == null || shoppingItemId == null) return false;
        List<LedgerEntry> list = getAll(context);
        for (LedgerEntry item : list) {
            if (item == null) continue;
            if (LedgerEntry.SOURCE_SHOPPING_AUTO.equals(item.getSourceType())
                    && shoppingItemId.equals(item.getSourceShoppingItemId())) {
                return true;
            }
        }
        return false;
    }

    public static int addShoppingEntries(Context context, List<ShoppingItem> shoppingItems) {
        if (context == null || shoppingItems == null || shoppingItems.isEmpty()) return 0;

        List<LedgerEntry> list = getAll(context);
        int added = 0;

        for (ShoppingItem item : shoppingItems) {
            if (item == null) continue;
            if (TextUtils.isEmpty(item.getName())) continue;

            Long shoppingId = item.getId();
            if (shoppingId != null && containsShoppingSource(context, shoppingId)) {
                continue;
            }

            LedgerEntry entry = LedgerEntry.createFromShopping(
                    item.getId(),
                    safeDate(item.getShoppingDate()),
                    item.getStoreId(),
                    safeStore(item.getStoreName()),
                    safe(item.getName()),
                    item.getQuantity() <= 0d ? 1.0d : item.getQuantity(),
                    safeUnit(item.getUnit()),
                    item.getPrice()
            );

            list.add(entry);
            added++;
        }

        saveAll(context, list);
        return added;
    }

    public static List<LedgerEntry> getEntriesForMonth(Context context, int year, int month1to12) {
        List<LedgerEntry> all = getAll(context);
        List<LedgerEntry> out = new ArrayList<>();
        String prefix = String.format("%04d-%02d-", year, month1to12);

        for (LedgerEntry item : all) {
            if (item == null) continue;
            if (item.getDate().startsWith(prefix)) {
                out.add(item);
            }
        }

        sortDesc(out);
        return out;
    }

    private static void sortDesc(List<LedgerEntry> list) {
        Collections.sort(list, new Comparator<LedgerEntry>() {
            @Override
            public int compare(LedgerEntry a, LedgerEntry b) {
                if (a == null && b == null) return 0;
                if (a == null) return 1;
                if (b == null) return -1;

                int byDate = b.getDate().compareTo(a.getDate());
                if (byDate != 0) return byDate;

                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safeDate(String s) {
        if (s == null || s.trim().isEmpty()) return "1970-01-01";
        return s.trim();
    }

    private static String safeStore(String s) {
        if (s == null || s.trim().isEmpty()) return "미지정";
        return s.trim();
    }

    private static String safeUnit(String s) {
        if (s == null || s.trim().isEmpty()) return "개";
        return s.trim();
    }
}