package com.namgyun.tamakitchen.game;

import android.content.Context;
import android.content.SharedPreferences;

public class GamePrefs {

    private static final String PREF = "game_prefs";

    private static final String KEY_COINS = "coins";
    private static final String KEY_EXP = "pet_exp";
    private static final String KEY_LEVEL = "pet_level";
    private static final String KEY_HUNGER = "pet_hunger";
    private static final String KEY_LAST_DECAY_MS = "last_decay_ms";

    // 기본값
    private static final int DEFAULT_COINS = 0;
    private static final int DEFAULT_LEVEL = 1;
    private static final int DEFAULT_EXP = 0;
    private static final int DEFAULT_HUNGER = 80;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static int getCoins(Context c) {
        return sp(c).getInt(KEY_COINS, DEFAULT_COINS);
    }

    public static int getLevel(Context c) {
        return sp(c).getInt(KEY_LEVEL, DEFAULT_LEVEL);
    }

    public static int getExp(Context c) {
        return sp(c).getInt(KEY_EXP, DEFAULT_EXP);
    }

    public static int getHunger(Context c) {
        return sp(c).getInt(KEY_HUNGER, DEFAULT_HUNGER);
    }

    public static long getLastDecayMs(Context c) {
        return sp(c).getLong(KEY_LAST_DECAY_MS, 0L);
    }

    public static void setLastDecayMs(Context c, long ms) {
        sp(c).edit().putLong(KEY_LAST_DECAY_MS, ms).apply();
    }

    public static void addCoins(Context c, int delta) {
        int v = Math.max(0, getCoins(c) + delta);
        sp(c).edit().putInt(KEY_COINS, v).apply();
    }

    public static void addExp(Context c, int delta) {
        int exp = Math.max(0, getExp(c) + delta);
        int level = getLevel(c);

        // 레벨업 규칙(아주 단순): 다음 레벨 필요 경험치 = level * 100
        while (exp >= level * 100) {
            exp -= level * 100;
            level += 1;
        }

        sp(c).edit()
                .putInt(KEY_EXP, exp)
                .putInt(KEY_LEVEL, level)
                .apply();
    }

    public static void addHunger(Context c, int delta) {
        int v = clamp(getHunger(c) + delta, 0, 100);
        sp(c).edit().putInt(KEY_HUNGER, v).apply();
    }

    public static boolean spendCoins(Context c, int cost) {
        int coins = getCoins(c);
        if (coins < cost) return false;
        sp(c).edit().putInt(KEY_COINS, coins - cost).apply();
        return true;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}