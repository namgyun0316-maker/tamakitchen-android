// File: app/src/main/java/com/example/myapplication/ui/shopping/ShoppingUtils.java
package com.namgyun.tamakitchen.ui.shopping;

import android.text.TextUtils;

import java.text.DecimalFormat;

public class ShoppingUtils {

    private ShoppingUtils() {}

    public static String safeUnit(String unit) {
        if (unit == null) return "개";
        String u = unit.trim();
        return u.isEmpty() ? "개" : u;
    }

    public static String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) {
            return String.valueOf((long) Math.round(q));
        }
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(q);
    }

    public static double parseQty(String text) {
        if (text == null) return 1.0d;
        String t = text.trim();
        if (t.isEmpty()) return 1.0d;
        if (t.equals(".") || t.equals(",")) return 1.0d;
        t = t.replace(",", ".");
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return 1.0d; }
    }

    public static double parseQtyStrict(String text) throws NumberFormatException {
        if (text == null) throw new NumberFormatException("null");
        String t = text.trim();
        if (t.isEmpty()) throw new NumberFormatException("empty");
        if (t.equals(".") || t.equals(",")) throw new NumberFormatException("dot only");
        t = t.replace(",", ".");
        return Double.parseDouble(t);
    }

    public static int calcLineTotal(ShoppingItem it) {
        if (it == null) return 0;
        return (int) Math.round(it.getPrice() * it.getQuantity());
    }

    public static String makeKey(String dateKey, Long storeId) {
        String d = (dateKey == null) ? "" : dateKey.trim();
        String sid = (storeId == null) ? "ALL" : String.valueOf(storeId);
        return d + "|" + sid;
    }

    public static String safe(String s) { return s == null ? "" : s.trim(); }

    public static boolean isSameKey(String a, String b) {
        return !TextUtils.isEmpty(a) && a.equals(b);
    }
}
