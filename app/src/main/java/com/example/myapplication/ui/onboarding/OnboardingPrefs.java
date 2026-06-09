// File: app/src/main/java/com/example/myapplication/ui/onboarding/OnboardingPrefs.java
package com.namgyun.tamakitchen.ui.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingPrefs {

    private static final String PREFS = "onboarding_prefs";

    private static final String KEY_DONE = "done";
    private static final String KEY_NICK = "nickname";
    private static final String KEY_FRIDGE_TYPE = "fridgeType";
    private static final String KEY_FRIDGE_ID = "fridgeId";
    private static final String KEY_TOOLS = "tools";

    // ✅ 실제 서비스에서는 false 유지
    private static final boolean FORCE_SHOW_EVERY_TIME = false;

    private OnboardingPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ----------------------------
    // 온보딩 완료 여부
    // ----------------------------
    public static boolean isDone(Context context) {
        if (FORCE_SHOW_EVERY_TIME) return false;
        return prefs(context).getBoolean(KEY_DONE, false);
    }

    // ✅ LoginActivity에서 쓰기 좋은 별칭
    public static boolean isOnboardingDone(Context context) {
        return isDone(context);
    }

    public static boolean shouldShowOnboarding(Context context) {
        return !isDone(context);
    }

    public static void setDone(Context context, boolean done) {
        prefs(context).edit()
                .putBoolean(KEY_DONE, done)
                .apply();
    }

    public static void markDone(Context context) {
        setDone(context, true);
    }

    public static void markNotDone(Context context) {
        setDone(context, false);
    }

    // ----------------------------
    // 닉네임
    // ----------------------------
    public static void saveNickname(Context context, String nickname) {
        prefs(context).edit()
                .putString(KEY_NICK, nickname == null ? "" : nickname.trim())
                .apply();
    }

    public static String getNickname(Context context) {
        return prefs(context).getString(KEY_NICK, "");
    }

    // ----------------------------
    // 냉장고 타입
    // ----------------------------
    public static void saveFridgeType(Context context, String type) {
        String safeType = (type == null || type.trim().isEmpty())
                ? "PERSONAL"
                : type.trim();

        prefs(context).edit()
                .putString(KEY_FRIDGE_TYPE, safeType)
                .apply();
    }

    public static String getFridgeType(Context context) {
        return prefs(context).getString(KEY_FRIDGE_TYPE, "PERSONAL");
    }

    // ----------------------------
    // 냉장고 ID
    // ----------------------------
    public static void saveFridgeId(Context context, long fridgeId) {
        prefs(context).edit()
                .putLong(KEY_FRIDGE_ID, fridgeId)
                .apply();
    }

    public static long getFridgeId(Context context) {
        return prefs(context).getLong(KEY_FRIDGE_ID, -1L);
    }

    // ----------------------------
    // 조리도구 저장
    // ----------------------------
    public static void saveTools(Context context, Set<String> tools) {
        Set<String> safe = (tools == null) ? new HashSet<>() : new HashSet<>(tools);

        prefs(context).edit()
                .putStringSet(KEY_TOOLS, safe)
                .apply();
    }

    public static Set<String> getTools(Context context) {
        Set<String> set = prefs(context).getStringSet(KEY_TOOLS, new HashSet<>());
        if (set == null) return new HashSet<>();
        return new HashSet<>(set);
    }

    public static List<String> getToolsAsList(Context context) {
        return new ArrayList<>(getTools(context));
    }

    public static boolean hasAnyTool(Context context) {
        return !getTools(context).isEmpty();
    }

    // ----------------------------
    // 초기화
    // ----------------------------
    public static void reset(Context context) {
        prefs(context).edit().clear().apply();
    }
}