package com.namgyun.tamakitchen.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthPrefs {

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_GUEST = "isGuest";
    private static final String KEY_USER_NICKNAME = "userNickname";
    private static final String KEY_EMAIL = "email";

    private AuthPrefs() {}

    public static boolean isGuest(Context context) {
        if (context == null) return true;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getBoolean(KEY_IS_GUEST, false);
    }

    public static boolean isLoggedIn(Context context) {
        return !isGuest(context);
    }

    public static String getEmail(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, null);
    }

    public static long getUserId(Context context) {
        if (context == null) return -1L;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getLong(KEY_USER_ID, -1L);
    }

    public static String getNickname(Context context) {
        if (context == null) return "";
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String nick = p.getString(KEY_USER_NICKNAME, "");
        return nick == null ? "" : nick.trim();
    }

    // ✅🔥 추가 (핵심)
    public static String getUserNickname(Context context) {
        return getNickname(context);
    }

    public static void saveNickname(Context context, String nickname) {
        if (context == null) return;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putString(KEY_USER_NICKNAME, nickname == null ? "" : nickname.trim()).apply();
    }

    public static void saveEmail(Context context, String email) {
        if (context == null) return;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putString(KEY_EMAIL, email == null ? "" : email.trim()).apply();
    }

    public static void setGuestMode(Context context, boolean guest) {
        if (context == null) return;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putBoolean(KEY_IS_GUEST, guest).apply();
    }

    public static void saveLoginUser(Context context, long userId, String nickname, String email) {
        if (context == null) return;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = p.edit();
        ed.putBoolean(KEY_IS_GUEST, false);
        ed.putLong(KEY_USER_ID, userId);
        ed.putString(KEY_USER_NICKNAME, nickname == null ? "" : nickname.trim());
        ed.putString(KEY_EMAIL, email == null ? "" : email.trim());
        ed.apply();
    }

    public static void saveGuestUser(Context context, long userId, String nickname, String email) {
        if (context == null) return;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = p.edit();
        ed.putBoolean(KEY_IS_GUEST, true);
        ed.putLong(KEY_USER_ID, userId);
        ed.putString(KEY_USER_NICKNAME, nickname == null ? "" : nickname.trim());
        ed.putString(KEY_EMAIL, email == null ? "" : email.trim());
        ed.apply();
    }

    public static void clearLogin(Context context) {
        if (context == null) return;
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NICKNAME)
                .remove(KEY_EMAIL)
                .putBoolean(KEY_IS_GUEST, true)
                .apply();
    }
}