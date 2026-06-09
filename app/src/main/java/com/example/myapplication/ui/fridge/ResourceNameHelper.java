package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.namgyun.tamakitchen.MyApplication;

public class ResourceNameHelper {

    private static final String TAG = "ResourceNameHelper";

    private ResourceNameHelper() {}

    /**
     * resId -> 리소스 엔트리 이름 반환 (예: item_tomato)
     */
    public static String getNameFromResId(int resId) {
        if (resId == 0) return "";

        Context ctx = MyApplication.getContext();
        if (ctx == null) return "";

        try {
            return ctx.getResources().getResourceEntryName(resId);
        } catch (Exception e) {
            Log.w(TAG, "getNameFromResId failed. resId=" + resId, e);
            return "";
        }
    }

    /**
     * name -> drawable resId 반환
     * - null/빈값이면 0 반환(절대 크래시 X)
     * - "R.drawable.xxx", "@drawable/xxx", "drawable/xxx", "xxx.png" 같은 입력도 정규화해서 처리
     */
    public static int getResIdFromName(String name) {
        Context ctx = MyApplication.getContext();
        if (ctx == null) return 0;

        String normalized = normalizeDrawableName(name);
        if (TextUtils.isEmpty(normalized)) return 0;

        try {
            return ctx.getResources().getIdentifier(normalized, "drawable", ctx.getPackageName());
        } catch (Exception e) {
            Log.w(TAG, "getResIdFromName failed. name=" + name + " normalized=" + normalized, e);
            return 0;
        }
    }

    /**
     * 입력값을 drawable 리소스 이름 형태로 정규화
     * 예)
     * - null -> ""
     * - "R.drawable.item_tomato" -> "item_tomato"
     * - "@drawable/item_tomato"  -> "item_tomato"
     * - "drawable/item_tomato"   -> "item_tomato"
     * - "item_tomato.png"        -> "item_tomato"
     */
    private static String normalizeDrawableName(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";

        if (s.startsWith("R.drawable.")) {
            s = s.substring("R.drawable.".length());
        }

        if (s.startsWith("@drawable/")) {
            s = s.substring("@drawable/".length());
        }

        if (s.startsWith("drawable/")) {
            s = s.substring("drawable/".length());
        }

        int dot = s.lastIndexOf('.');
        if (dot > 0) {
            s = s.substring(0, dot);
        }

        s = s.trim().toLowerCase();
        s = s.replaceAll("\\s+", "_");

        return s;
    }
}
