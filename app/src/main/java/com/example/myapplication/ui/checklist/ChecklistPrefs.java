package com.namgyun.tamakitchen.ui.checklist;

import android.content.Context;
import android.content.SharedPreferences;

import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChecklistPrefs {

    private static final String PREFS_NAME = "checklist_prefs";

    private static final String KEY_DATE = "date";
    private static final String KEY_WEEK_KEY = "week_key";

    // ===== 일일 미션 =====
    private static final String KEY_FEED_DONE = "feed_done";
    private static final String KEY_FRIDGE_ADD_DONE = "fridge_add_done";
    private static final String KEY_EXPIRY_ADD_DONE = "expiry_add_done";
    private static final String KEY_DELETE_DONE = "delete_done";
    private static final String KEY_COOK_DONE = "cook_done";

    private static final String KEY_FEED_CLAIMED = "feed_claimed";
    private static final String KEY_FRIDGE_ADD_CLAIMED = "fridge_add_claimed";
    private static final String KEY_EXPIRY_ADD_CLAIMED = "expiry_add_claimed";
    private static final String KEY_DELETE_CLAIMED = "delete_claimed";
    private static final String KEY_COOK_CLAIMED = "cook_claimed";

    private static final String KEY_TODAY_CLAIMED_COINS = "today_claimed_coins";

    // ===== 주간 미션 누적 =====
    private static final String KEY_WEEKLY_ATTENDANCE_COUNT = "weekly_attendance_count";
    private static final String KEY_WEEKLY_COOK_COUNT = "weekly_cook_count";
    private static final String KEY_WEEKLY_SHOPPING_COUNT = "weekly_shopping_count";
    private static final String KEY_WEEKLY_FEED_COUNT = "weekly_feed_count";

    private static final String KEY_WEEKLY_ATTENDANCE_CLAIMED = "weekly_attendance_claimed";
    private static final String KEY_WEEKLY_COOK_CLAIMED = "weekly_cook_claimed";
    private static final String KEY_WEEKLY_SHOPPING_CLAIMED = "weekly_shopping_claimed";
    private static final String KEY_WEEKLY_FEED_5_CLAIMED = "weekly_feed_5_claimed";
    private static final String KEY_WEEKLY_FEED_10_CLAIMED = "weekly_feed_10_claimed";

    // ===== 누적 업적 카운트 =====
    private static final String KEY_FEED_COUNT = "feed_count";
    private static final String KEY_COOK_COUNT = "cook_count";
    private static final String KEY_RECIPE_CREATE_COUNT = "recipe_create_count";
    private static final String KEY_FUSION_COUNT = "fusion_count";
    private static final String KEY_HATCH_COUNT = "hatch_count";

    // ===== 업적 claim prefix =====
    private static final String PREFIX_CLAIM_COOK = "ach_cook_";
    private static final String PREFIX_CLAIM_RECIPE = "ach_recipe_";
    private static final String PREFIX_CLAIM_FUSION = "ach_fusion_";
    private static final String PREFIX_CLAIM_HATCH = "ach_hatch_";
    private static final String PREFIX_CLAIM_COLLECTION = "ach_collection_";

    // ===== 업적 단계 =====
    private static final int[] COOK_MILESTONES = {1, 5, 10, 20, 40, 70, 100};
    private static final int[] RECIPE_MILESTONES = {1, 5, 10, 15, 20, 25, 30, 35, 40};
    private static final int[] FUSION_MILESTONES = {1, 5, 10, 15, 20, 25, 30, 35, 40};
    private static final int[] HATCH_MILESTONES = {1, 10, 20, 30};
    private static final int[] COLLECTION_MILESTONES = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    // ===== 일일 보상 =====
    public static final int REWARD_FEED = 10;
    public static final int REWARD_FRIDGE_ADD = 10;
    public static final int REWARD_EXPIRY_ADD = 20;
    public static final int REWARD_DELETE = 10;
    public static final int REWARD_COOK = 30;

    // ===== 주간 보상 =====
    public static final int REWARD_WEEKLY_ATTENDANCE = 100;
    public static final int REWARD_WEEKLY_COOK = 100;
    public static final int REWARD_WEEKLY_SHOPPING = 100;
    public static final int REWARD_WEEKLY_FEED_5 = 50;
    public static final int REWARD_WEEKLY_FEED_10 = 100;

    private ChecklistPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    }

    private static String currentWeekKey() {
        return new SimpleDateFormat("yyyyww", Locale.getDefault()).format(new Date());
    }

    private static String buildClaimKey(String prefix, int target) {
        return prefix + target + "_claimed";
    }

    private static boolean isClaimed(Context context, String prefix, int target) {
        return prefs(context).getBoolean(buildClaimKey(prefix, target), false);
    }

    private static void setClaimed(Context context, String prefix, int target, boolean value) {
        prefs(context).edit().putBoolean(buildClaimKey(prefix, target), value).apply();
    }

    private static int getSequentialTarget(Context context, int[] milestones, String prefix) {
        if (milestones == null || milestones.length == 0) return 1;

        for (int milestone : milestones) {
            if (!isClaimed(context, prefix, milestone)) {
                return milestone;
            }
        }
        return milestones[milestones.length - 1];
    }

    private static int getHighestReachedTarget(int count, int[] milestones) {
        if (milestones == null || milestones.length == 0) return 1;

        int highest = milestones[0];
        for (int milestone : milestones) {
            if (count >= milestone) {
                highest = milestone;
            } else {
                break;
            }
        }
        return highest;
    }

    public static void ensureToday(Context context, int currentLevel) {
        SharedPreferences sp = prefs(context);
        String savedDate = sp.getString(KEY_DATE, null);
        String today = today();

        if (!today.equals(savedDate)) {
            sp.edit()
                    .putString(KEY_DATE, today)
                    .putBoolean(KEY_FEED_DONE, false)
                    .putBoolean(KEY_FRIDGE_ADD_DONE, false)
                    .putBoolean(KEY_EXPIRY_ADD_DONE, false)
                    .putBoolean(KEY_DELETE_DONE, false)
                    .putBoolean(KEY_COOK_DONE, false)
                    .putBoolean(KEY_FEED_CLAIMED, false)
                    .putBoolean(KEY_FRIDGE_ADD_CLAIMED, false)
                    .putBoolean(KEY_EXPIRY_ADD_CLAIMED, false)
                    .putBoolean(KEY_DELETE_CLAIMED, false)
                    .putBoolean(KEY_COOK_CLAIMED, false)
                    .putInt(KEY_TODAY_CLAIMED_COINS, 0)
                    .apply();
        }
    }

    public static void ensureWeek(Context context) {
        SharedPreferences sp = prefs(context);
        String savedWeek = sp.getString(KEY_WEEK_KEY, null);
        String currentWeek = currentWeekKey();

        if (!currentWeek.equals(savedWeek)) {
            sp.edit()
                    .putString(KEY_WEEK_KEY, currentWeek)
                    .putInt(KEY_WEEKLY_ATTENDANCE_COUNT, 0)
                    .putInt(KEY_WEEKLY_COOK_COUNT, 0)
                    .putInt(KEY_WEEKLY_SHOPPING_COUNT, 0)
                    .putInt(KEY_WEEKLY_FEED_COUNT, 0)
                    .putBoolean(KEY_WEEKLY_ATTENDANCE_CLAIMED, false)
                    .putBoolean(KEY_WEEKLY_COOK_CLAIMED, false)
                    .putBoolean(KEY_WEEKLY_SHOPPING_CLAIMED, false)
                    .putBoolean(KEY_WEEKLY_FEED_5_CLAIMED, false)
                    .putBoolean(KEY_WEEKLY_FEED_10_CLAIMED, false)
                    .apply();
        }
    }

    // =========================
    // 일일 완료 처리
    // =========================
    public static void markFeedDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        prefs(context).edit().putBoolean(KEY_FEED_DONE, true).apply();
    }

    public static void markFridgeAddDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        prefs(context).edit().putBoolean(KEY_FRIDGE_ADD_DONE, true).apply();
    }

    public static void markExpiryAddDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        prefs(context).edit().putBoolean(KEY_EXPIRY_ADD_DONE, true).apply();
    }

    public static void markDeleteDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        prefs(context).edit().putBoolean(KEY_DELETE_DONE, true).apply();
    }

    public static void markCookDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        ensureWeek(context);

        SharedPreferences sp = prefs(context);
        sp.edit().putBoolean(KEY_COOK_DONE, true).apply();

        int cookCount = sp.getInt(KEY_COOK_COUNT, 0);
        int weeklyCookCount = sp.getInt(KEY_WEEKLY_COOK_COUNT, 0);

        sp.edit()
                .putInt(KEY_COOK_COUNT, cookCount + 1)
                .putInt(KEY_WEEKLY_COOK_COUNT, weeklyCookCount + 1)
                .apply();
    }

    // =========================
    // 누적 카운트
    // =========================
    public static void addFeedCount(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);

        int total = sp.getInt(KEY_FEED_COUNT, 0);
        int weekly = sp.getInt(KEY_WEEKLY_FEED_COUNT, 0);

        sp.edit()
                .putInt(KEY_FEED_COUNT, total + 1)
                .putInt(KEY_WEEKLY_FEED_COUNT, weekly + 1)
                .apply();
    }

    public static void addRecipeCreateCount(Context context) {
        SharedPreferences sp = prefs(context);
        int count = sp.getInt(KEY_RECIPE_CREATE_COUNT, 0);
        sp.edit().putInt(KEY_RECIPE_CREATE_COUNT, count + 1).apply();
    }

    public static void addFusionCount(Context context) {
        SharedPreferences sp = prefs(context);
        int count = sp.getInt(KEY_FUSION_COUNT, 0);
        sp.edit().putInt(KEY_FUSION_COUNT, count + 1).apply();
    }

    public static void addHatchCount(Context context) {
        SharedPreferences sp = prefs(context);
        int count = sp.getInt(KEY_HATCH_COUNT, 0);
        sp.edit().putInt(KEY_HATCH_COUNT, count + 1).apply();
    }

    // =========================
    // 주간 처리
    // =========================
    public static void markWeeklyAttendanceDone(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        int count = sp.getInt(KEY_WEEKLY_ATTENDANCE_COUNT, 0);
        sp.edit().putInt(KEY_WEEKLY_ATTENDANCE_COUNT, count + 1).apply();
    }

    public static void markWeeklyShoppingDone(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        int count = sp.getInt(KEY_WEEKLY_SHOPPING_COUNT, 0);
        sp.edit().putInt(KEY_WEEKLY_SHOPPING_COUNT, count + 1).apply();
    }

    public static int getWeeklyAttendanceCount(Context context) {
        ensureWeek(context);
        return prefs(context).getInt(KEY_WEEKLY_ATTENDANCE_COUNT, 0);
    }

    public static int getWeeklyCookCount(Context context) {
        ensureWeek(context);
        return prefs(context).getInt(KEY_WEEKLY_COOK_COUNT, 0);
    }

    public static int getWeeklyShoppingCount(Context context) {
        ensureWeek(context);
        return prefs(context).getInt(KEY_WEEKLY_SHOPPING_COUNT, 0);
    }

    public static int getWeeklyFeedCount(Context context) {
        ensureWeek(context);
        return prefs(context).getInt(KEY_WEEKLY_FEED_COUNT, 0);
    }

    public static boolean isWeeklyAttendanceClaimed(Context context) {
        ensureWeek(context);
        return prefs(context).getBoolean(KEY_WEEKLY_ATTENDANCE_CLAIMED, false);
    }

    public static boolean isWeeklyCookClaimed(Context context) {
        ensureWeek(context);
        return prefs(context).getBoolean(KEY_WEEKLY_COOK_CLAIMED, false);
    }

    public static boolean isWeeklyShoppingClaimed(Context context) {
        ensureWeek(context);
        return prefs(context).getBoolean(KEY_WEEKLY_SHOPPING_CLAIMED, false);
    }

    public static boolean isWeeklyFeed5Claimed(Context context) {
        ensureWeek(context);
        return prefs(context).getBoolean(KEY_WEEKLY_FEED_5_CLAIMED, false);
    }

    public static boolean isWeeklyFeed10Claimed(Context context) {
        ensureWeek(context);
        return prefs(context).getBoolean(KEY_WEEKLY_FEED_10_CLAIMED, false);
    }

    public static int claimWeeklyAttendanceReward(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        if (sp.getInt(KEY_WEEKLY_ATTENDANCE_COUNT, 0) < 5) return 0;
        if (sp.getBoolean(KEY_WEEKLY_ATTENDANCE_CLAIMED, false)) return 0;

        sp.edit().putBoolean(KEY_WEEKLY_ATTENDANCE_CLAIMED, true).apply();
        return REWARD_WEEKLY_ATTENDANCE;
    }

    public static int claimWeeklyCookReward(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        if (sp.getInt(KEY_WEEKLY_COOK_COUNT, 0) < 2) return 0;
        if (sp.getBoolean(KEY_WEEKLY_COOK_CLAIMED, false)) return 0;

        sp.edit().putBoolean(KEY_WEEKLY_COOK_CLAIMED, true).apply();
        return REWARD_WEEKLY_COOK;
    }

    public static int claimWeeklyShoppingReward(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        if (sp.getInt(KEY_WEEKLY_SHOPPING_COUNT, 0) < 1) return 0;
        if (sp.getBoolean(KEY_WEEKLY_SHOPPING_CLAIMED, false)) return 0;

        sp.edit().putBoolean(KEY_WEEKLY_SHOPPING_CLAIMED, true).apply();
        return REWARD_WEEKLY_SHOPPING;
    }

    public static int claimWeeklyFeed5Reward(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        if (sp.getInt(KEY_WEEKLY_FEED_COUNT, 0) < 5) return 0;
        if (sp.getBoolean(KEY_WEEKLY_FEED_5_CLAIMED, false)) return 0;

        sp.edit().putBoolean(KEY_WEEKLY_FEED_5_CLAIMED, true).apply();
        return REWARD_WEEKLY_FEED_5;
    }

    public static int claimWeeklyFeed10Reward(Context context) {
        ensureWeek(context);
        SharedPreferences sp = prefs(context);
        if (sp.getInt(KEY_WEEKLY_FEED_COUNT, 0) < 10) return 0;
        if (sp.getBoolean(KEY_WEEKLY_FEED_10_CLAIMED, false)) return 0;

        sp.edit().putBoolean(KEY_WEEKLY_FEED_10_CLAIMED, true).apply();
        return REWARD_WEEKLY_FEED_10;
    }

    // =========================
    // 일일 조회
    // =========================
    public static boolean isFeedDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_FEED_DONE, false);
    }

    public static boolean isFridgeAddDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_FRIDGE_ADD_DONE, false);
    }

    public static boolean isExpiryAddDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_EXPIRY_ADD_DONE, false);
    }

    public static boolean isDeleteDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_DELETE_DONE, false);
    }

    public static boolean isCookDone(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_COOK_DONE, false);
    }

    public static boolean isFeedClaimed(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_FEED_CLAIMED, false);
    }

    public static boolean isFridgeAddClaimed(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_FRIDGE_ADD_CLAIMED, false);
    }

    public static boolean isExpiryAddClaimed(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_EXPIRY_ADD_CLAIMED, false);
    }

    public static boolean isDeleteClaimed(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_DELETE_CLAIMED, false);
    }

    public static boolean isCookClaimed(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getBoolean(KEY_COOK_CLAIMED, false);
    }

    // =========================
    // 일일 보상
    // =========================
    public static int claimFeedReward(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        SharedPreferences sp = prefs(context);
        if (!sp.getBoolean(KEY_FEED_DONE, false)) return 0;
        if (sp.getBoolean(KEY_FEED_CLAIMED, false)) return 0;

        addTodayCoins(context, REWARD_FEED);
        sp.edit().putBoolean(KEY_FEED_CLAIMED, true).apply();
        return REWARD_FEED;
    }

    public static int claimFridgeAddReward(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        SharedPreferences sp = prefs(context);
        if (!sp.getBoolean(KEY_FRIDGE_ADD_DONE, false)) return 0;
        if (sp.getBoolean(KEY_FRIDGE_ADD_CLAIMED, false)) return 0;

        addTodayCoins(context, REWARD_FRIDGE_ADD);
        sp.edit().putBoolean(KEY_FRIDGE_ADD_CLAIMED, true).apply();
        return REWARD_FRIDGE_ADD;
    }

    public static int claimExpiryAddReward(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        SharedPreferences sp = prefs(context);
        if (!sp.getBoolean(KEY_EXPIRY_ADD_DONE, false)) return 0;
        if (sp.getBoolean(KEY_EXPIRY_ADD_CLAIMED, false)) return 0;

        addTodayCoins(context, REWARD_EXPIRY_ADD);
        sp.edit().putBoolean(KEY_EXPIRY_ADD_CLAIMED, true).apply();
        return REWARD_EXPIRY_ADD;
    }

    public static int claimDeleteReward(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        SharedPreferences sp = prefs(context);
        if (!sp.getBoolean(KEY_DELETE_DONE, false)) return 0;
        if (sp.getBoolean(KEY_DELETE_CLAIMED, false)) return 0;

        addTodayCoins(context, REWARD_DELETE);
        sp.edit().putBoolean(KEY_DELETE_CLAIMED, true).apply();
        return REWARD_DELETE;
    }

    public static int claimCookReward(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        SharedPreferences sp = prefs(context);
        if (!sp.getBoolean(KEY_COOK_DONE, false)) return 0;
        if (sp.getBoolean(KEY_COOK_CLAIMED, false)) return 0;

        addTodayCoins(context, REWARD_COOK);
        sp.edit().putBoolean(KEY_COOK_CLAIMED, true).apply();
        return REWARD_COOK;
    }

    private static void addTodayCoins(Context context, int amount) {
        SharedPreferences sp = prefs(context);
        int current = sp.getInt(KEY_TODAY_CLAIMED_COINS, 0);
        sp.edit().putInt(KEY_TODAY_CLAIMED_COINS, current + amount).apply();
    }

    public static int getTodayClaimedCoins(Context context, int currentLevel) {
        ensureToday(context, currentLevel);
        return prefs(context).getInt(KEY_TODAY_CLAIMED_COINS, 0);
    }

    // =========================
    // 업적 카운트 조회
    // =========================
    public static int getFeedCount(Context context) {
        return prefs(context).getInt(KEY_FEED_COUNT, 0);
    }

    public static int getCookCount(Context context) {
        return prefs(context).getInt(KEY_COOK_COUNT, 0);
    }

    public static int getRecipeCreateCount(Context context) {
        return prefs(context).getInt(KEY_RECIPE_CREATE_COUNT, 0);
    }

    public static int getFusionCount(Context context) {
        return prefs(context).getInt(KEY_FUSION_COUNT, 0);
    }

    public static int getHatchCount(Context context) {
        return prefs(context).getInt(KEY_HATCH_COUNT, 0);
    }

    public static int getUnlockedCollectionCount(Context context) {
        List<CharacterCollectionItem> items = CollectionCatalog.getAllItems(context);
        if (items == null || items.isEmpty()) return 0;

        int count = 0;
        for (CharacterCollectionItem item : items) {
            if (item == null) continue;
            if (CollectionCatalog.KEY_EGG_BASIC.equals(item.getKey())) continue;
            if (item.isUnlocked()) count++;
        }
        return count;
    }

    // =========================
    // 요리 업적
    // =========================
    public static int getCookAchievementTarget(Context context) {
        return getSequentialTarget(context, COOK_MILESTONES, PREFIX_CLAIM_COOK);
    }

    public static int getCookAchievementTarget(int count) {
        return getHighestReachedTarget(count, COOK_MILESTONES);
    }

    public static String getCookAchievementTitleByTarget(int target) {
        switch (target) {
            case 1: return "요리 초보";
            case 5: return "요리 견습";
            case 10: return "주방 입문자";
            case 20: return "집밥 요리사";
            case 40: return "레시피 숙련가";
            case 70: return "냉장고 셰프";
            case 100: return "전설의 셰프";
            default: return "요리 초보";
        }
    }

    public static String getCookAchievementDescByTarget(int target) {
        switch (target) {
            case 1: return "레시피를 보고 처음으로 요리를 완성해보자.";
            case 5: return "요리를 5번 완성해보자.";
            case 10: return "요리를 10번 완성해보자.";
            case 20: return "요리를 20번 완성해보자.";
            case 40: return "요리를 40번 완성해보자.";
            case 70: return "요리를 70번 완성해보자.";
            case 100: return "요리를 100번 완성해보자.";
            default: return "요리를 완성해보자.";
        }
    }

    public static String getCookAchievementTitle(int count) {
        return getCookAchievementTitleByTarget(getCookAchievementTarget(count));
    }

    public static String getCookAchievementDesc(int count) {
        return getCookAchievementDescByTarget(getCookAchievementTarget(count));
    }

    public static int getCookAchievementReward(int target) {
        switch (target) {
            case 1: return 30;
            case 5: return 50;
            case 10: return 80;
            case 20: return 120;
            case 40: return 180;
            case 70: return 260;
            case 100: return 350;
            default: return 30;
        }
    }

    public static boolean isCookAchievementClaimed(Context context, int target) {
        return isClaimed(context, PREFIX_CLAIM_COOK, target);
    }

    public static int claimCookAchievementReward(Context context, int target) {
        int count = getCookCount(context);
        if (count < target) return 0;
        if (isCookAchievementClaimed(context, target)) return 0;

        int reward = getCookAchievementReward(target);
        setClaimed(context, PREFIX_CLAIM_COOK, target, true);
        return reward;
    }

    // =========================
    // 레시피 작성 업적
    // =========================
    public static int getRecipeAchievementTarget(Context context) {
        return getSequentialTarget(context, RECIPE_MILESTONES, PREFIX_CLAIM_RECIPE);
    }

    public static int getRecipeAchievementTarget(int count) {
        return getHighestReachedTarget(count, RECIPE_MILESTONES);
    }

    public static String getRecipeAchievementTitleByTarget(int target) {
        switch (target) {
            case 1: return "첫 레시피 작성자";
            case 5: return "레시피 기록가";
            case 10: return "메뉴 메모러";
            case 15: return "레시피 연구원";
            case 20: return "홈쿠킹 개발자";
            case 25: return "메뉴 창작자";
            case 30: return "레시피 공방장";
            case 35: return "레시피 장인";
            case 40: return "전설의 레시피북";
            default: return "첫 레시피 작성자";
        }
    }

    public static String getRecipeAchievementDescByTarget(int target) {
        switch (target) {
            case 1: return "레시피를 1개 등록해보자.";
            case 5: return "레시피를 5개 등록해보자.";
            case 10: return "레시피를 10개 등록해보자.";
            case 15: return "레시피를 15개 등록해보자.";
            case 20: return "레시피를 20개 등록해보자.";
            case 25: return "레시피를 25개 등록해보자.";
            case 30: return "레시피를 30개 등록해보자.";
            case 35: return "레시피를 35개 등록해보자.";
            case 40: return "레시피를 40개 등록해보자.";
            default: return "레시피를 등록해보자.";
        }
    }

    public static String getRecipeAchievementTitle(int count) {
        return getRecipeAchievementTitleByTarget(getRecipeAchievementTarget(count));
    }

    public static String getRecipeAchievementDesc(int count) {
        return getRecipeAchievementDescByTarget(getRecipeAchievementTarget(count));
    }

    public static int getRecipeAchievementReward(int target) {
        switch (target) {
            case 1: return 30;
            case 5: return 50;
            case 10: return 80;
            case 15: return 100;
            case 20: return 120;
            case 25: return 140;
            case 30: return 160;
            case 35: return 180;
            case 40: return 200;
            default: return 30;
        }
    }

    public static boolean isRecipeAchievementClaimed(Context context, int target) {
        return isClaimed(context, PREFIX_CLAIM_RECIPE, target);
    }

    public static int claimRecipeAchievementReward(Context context, int target) {
        int count = getRecipeCreateCount(context);
        if (count < target) return 0;
        if (isRecipeAchievementClaimed(context, target)) return 0;

        int reward = getRecipeAchievementReward(target);
        setClaimed(context, PREFIX_CLAIM_RECIPE, target, true);
        return reward;
    }

    // =========================
    // 합성 업적
    // =========================
    public static int getFusionAchievementTarget(Context context) {
        return getSequentialTarget(context, FUSION_MILESTONES, PREFIX_CLAIM_FUSION);
    }

    public static int getFusionAchievementTarget(int count) {
        return getHighestReachedTarget(count, FUSION_MILESTONES);
    }

    public static String getFusionAchievementTitleByTarget(int target) {
        switch (target) {
            case 1: return "첫 합성 성공";
            case 5: return "합성 견습생";
            case 10: return "조합 연구가";
            case 15: return "맛의 실험가";
            case 20: return "레시피 연금술사";
            case 25: return "합성 전문가";
            case 30: return "조합 마에스트로";
            case 35: return "진화 설계자";
            case 40: return "합성 마스터";
            default: return "첫 합성 성공";
        }
    }

    public static String getFusionAchievementDescByTarget(int target) {
        switch (target) {
            case 1: return "펫을 처음 합성해보자.";
            case 5: return "합성을 5번 성공해보자.";
            case 10: return "합성을 10번 성공해보자.";
            case 15: return "합성을 15번 성공해보자.";
            case 20: return "합성을 20번 성공해보자.";
            case 25: return "합성을 25번 성공해보자.";
            case 30: return "합성을 30번 성공해보자.";
            case 35: return "합성을 35번 성공해보자.";
            case 40: return "합성을 40번 성공해보자.";
            default: return "합성을 성공해보자.";
        }
    }

    public static String getFusionAchievementTitle(int count) {
        return getFusionAchievementTitleByTarget(getFusionAchievementTarget(count));
    }

    public static String getFusionAchievementDesc(int count) {
        return getFusionAchievementDescByTarget(getFusionAchievementTarget(count));
    }

    public static int getFusionAchievementReward(int target) {
        switch (target) {
            case 1: return 30;
            case 5: return 70;
            case 10: return 100;
            case 15: return 130;
            case 20: return 160;
            case 25: return 190;
            case 30: return 220;
            case 35: return 260;
            case 40: return 300;
            default: return 30;
        }
    }

    public static boolean isFusionAchievementClaimed(Context context, int target) {
        return isClaimed(context, PREFIX_CLAIM_FUSION, target);
    }

    public static int claimFusionAchievementReward(Context context, int target) {
        int count = getFusionCount(context);
        if (count < target) return 0;
        if (isFusionAchievementClaimed(context, target)) return 0;

        int reward = getFusionAchievementReward(target);
        setClaimed(context, PREFIX_CLAIM_FUSION, target, true);
        return reward;
    }

    // =========================
    // 부화 업적
    // =========================
    public static int getHatchAchievementTarget(Context context) {
        return getSequentialTarget(context, HATCH_MILESTONES, PREFIX_CLAIM_HATCH);
    }

    public static int getHatchAchievementTarget(int count) {
        return getHighestReachedTarget(count, HATCH_MILESTONES);
    }

    public static String getHatchAchievementTitleByTarget(int target) {
        switch (target) {
            case 1: return "첫 부화의 순간";
            case 10: return "부화 입문자";
            case 20: return "알 돌보기 달인";
            case 30: return "부화 마스터";
            default: return "첫 부화의 순간";
        }
    }

    public static String getHatchAchievementDescByTarget(int target) {
        switch (target) {
            case 1: return "알에서 펫을 1번 부화시켜보자.";
            case 10: return "알에서 펫을 10번 부화시켜보자.";
            case 20: return "알에서 펫을 20번 부화시켜보자.";
            case 30: return "알에서 펫을 30번 부화시켜보자.";
            default: return "알에서 펫을 부화시켜보자.";
        }
    }

    public static String getHatchAchievementTitle(int count) {
        return getHatchAchievementTitleByTarget(getHatchAchievementTarget(count));
    }

    public static String getHatchAchievementDesc(int count) {
        return getHatchAchievementDescByTarget(getHatchAchievementTarget(count));
    }

    public static int getHatchAchievementReward(int target) {
        switch (target) {
            case 1: return 30;
            case 10: return 120;
            case 20: return 220;
            case 30: return 350;
            default: return 30;
        }
    }

    public static boolean isHatchAchievementClaimed(Context context, int target) {
        return isClaimed(context, PREFIX_CLAIM_HATCH, target);
    }

    public static int claimHatchAchievementReward(Context context, int target) {
        int count = getHatchCount(context);
        if (count < target) return 0;
        if (isHatchAchievementClaimed(context, target)) return 0;

        int reward = getHatchAchievementReward(target);
        setClaimed(context, PREFIX_CLAIM_HATCH, target, true);
        return reward;
    }

    // =========================
    // 도감 업적
    // =========================
    public static int getCollectionAchievementTarget(Context context) {
        return getSequentialTarget(context, COLLECTION_MILESTONES, PREFIX_CLAIM_COLLECTION);
    }

    public static int getCollectionAchievementTarget(int count) {
        return getHighestReachedTarget(count, COLLECTION_MILESTONES);
    }

    public static String getCollectionAchievementTitleByTarget(int target) {
        switch (target) {
            case 10: return "도감 수집가";
            case 20: return "도감 탐험가";
            case 30: return "도감 애호가";
            case 40: return "도감 숙련가";
            case 50: return "도감 전문가";
            case 60: return "도감 연구가";
            case 70: return "도감 큐레이터";
            case 80: return "도감 헌터";
            case 90: return "도감 마에스트로";
            case 100: return "도감 전설";
            default: return "도감 수집가";
        }
    }

    public static String getCollectionAchievementDescByTarget(int target) {
        switch (target) {
            case 10: return "도감에 펫을 10종 등록해보자.";
            case 20: return "도감에 펫을 20종 등록해보자.";
            case 30: return "도감에 펫을 30종 등록해보자.";
            case 40: return "도감에 펫을 40종 등록해보자.";
            case 50: return "도감에 펫을 50종 등록해보자.";
            case 60: return "도감에 펫을 60종 등록해보자.";
            case 70: return "도감에 펫을 70종 등록해보자.";
            case 80: return "도감에 펫을 80종 등록해보자.";
            case 90: return "도감에 펫을 90종 등록해보자.";
            case 100: return "도감에 펫을 100종 등록해보자.";
            default: return "도감에 새로운 펫을 등록해보자.";
        }
    }

    public static String getCollectionAchievementTitle(int count) {
        return getCollectionAchievementTitleByTarget(getCollectionAchievementTarget(count));
    }

    public static String getCollectionAchievementDesc(int count) {
        return getCollectionAchievementDescByTarget(getCollectionAchievementTarget(count));
    }

    public static int getCollectionAchievementReward(int target) {
        switch (target) {
            case 10: return 80;
            case 20: return 120;
            case 30: return 160;
            case 40: return 200;
            case 50: return 240;
            case 60: return 280;
            case 70: return 320;
            case 80: return 360;
            case 90: return 420;
            case 100: return 500;
            default: return 80;
        }
    }

    public static boolean isCollectionAchievementClaimed(Context context, int target) {
        return isClaimed(context, PREFIX_CLAIM_COLLECTION, target);
    }

    public static int claimCollectionAchievementReward(Context context, int target) {
        int count = getUnlockedCollectionCount(context);
        if (count < target) return 0;
        if (isCollectionAchievementClaimed(context, target)) return 0;

        int reward = getCollectionAchievementReward(target);
        setClaimed(context, PREFIX_CLAIM_COLLECTION, target, true);
        return reward;
    }
}