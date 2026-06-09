package com.namgyun.tamakitchen.ui.fridge;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FridgeFilterSorter {

    public enum SortMode { EXPIRY, NAME }

    public static List<FridgeItem> filter(List<FridgeItem> all, String filter) {
        if (all == null) return new ArrayList<>();
        if (TextUtils.isEmpty(filter) || "전체".equals(filter)) return new ArrayList<>(all);

        List<FridgeItem> out = new ArrayList<>();
        for (FridgeItem item : all) {
            if (item != null && filter.equals(item.getStorage())) out.add(item);
        }
        return out;
    }

    public static void sortByExpiry(List<FridgeItem> list) {
        if (list == null) return;
        Collections.sort(list, (a, b) -> {
            String d1 = (a == null) ? "" : a.getExpiryDate();
            String d2 = (b == null) ? "" : b.getExpiryDate();
            if (TextUtils.isEmpty(d1)) return 1;
            if (TextUtils.isEmpty(d2)) return -1;
            return d1.compareTo(d2);
        });
    }

    public static void sortByName(List<FridgeItem> list) {
        if (list == null) return;
        Collections.sort(list, (a, b) -> {
            String n1 = (a == null || a.getName() == null) ? "" : a.getName();
            String n2 = (b == null || b.getName() == null) ? "" : b.getName();
            return n1.compareTo(n2);
        });
    }

    public static List<FridgeItem> expiredOnly(List<FridgeItem> all) {
        List<FridgeItem> out = new ArrayList<>();
        if (all == null) return out;
        for (FridgeItem it : all) {
            if (it == null) continue;
            if ("유통기한 지남".equals(it.getDDayString())) out.add(it);
        }
        return out;
    }
}