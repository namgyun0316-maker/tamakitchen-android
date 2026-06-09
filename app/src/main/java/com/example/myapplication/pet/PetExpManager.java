package com.namgyun.tamakitchen.pet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PetExpManager {

    private static final String PREFS = "pet_exp_limit_prefs";

    private static final String KEY_DATE = "date";
    private static final String KEY_FRIDGE_ADD_COUNT = "fridge_add_count";
    private static final String KEY_EXPIRY_ADD_COUNT = "expiry_add_count";
    private static final String KEY_EXPIRED_DELETE_COUNT = "expired_delete_count";
    private static final String KEY_SHOPPING_COMPLETE_COUNT = "shopping_complete_count";
    private static final String KEY_COOK_COMPLETE_COUNT = "cook_complete_count";

    public static final int EXP_FEED = 3;
    public static final int EXP_FRIDGE_ADD = 5;
    public static final int EXP_EXPIRY_MANUAL_ADD = 10;
    public static final int EXP_EXPIRED_DELETE = 5;
    public static final int EXP_SHOPPING_COMPLETE = 20;
    public static final int EXP_RECIPE_CREATE = 20;
    public static final int EXP_COOK_COMPLETE = 15;

    private static final int DAILY_LIMIT_FRIDGE_ADD = 10;
    private static final int DAILY_LIMIT_EXPIRY_ADD = 5;
    private static final int DAILY_LIMIT_EXPIRED_DELETE = 5;
    private static final int DAILY_LIMIT_SHOPPING_COMPLETE = 1;
    private static final int DAILY_LIMIT_COOK_COMPLETE = 3;

    private PetExpManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String todayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date());
    }

    private static void resetIfNewDay(Context context) {
        if (context == null) return;

        SharedPreferences p = prefs(context);
        String today = todayKey();
        String saved = p.getString(KEY_DATE, "");

        if (!today.equals(saved)) {
            p.edit()
                    .putString(KEY_DATE, today)
                    .putInt(KEY_FRIDGE_ADD_COUNT, 0)
                    .putInt(KEY_EXPIRY_ADD_COUNT, 0)
                    .putInt(KEY_EXPIRED_DELETE_COUNT, 0)
                    .putInt(KEY_SHOPPING_COMPLETE_COUNT, 0)
                    .putInt(KEY_COOK_COMPLETE_COUNT, 0)
                    .apply();
        }
    }

    public static boolean giveExp(Activity activity, int exp) {
        if (activity == null || activity.isFinishing()) return false;
        if (exp <= 0) return false;

        PetPrefs.addExpAndMaybeLevelUp(activity, exp);

        String selectedKey = CollectionDisplayPrefs.getSelectedItemKey(activity);
        if (selectedKey != null && !selectedKey.trim().isEmpty()) {
            CollectionPetStatePrefs.ensureState(activity, selectedKey);
            CollectionPetStatePrefs.addExpAndMaybeLevelUp(activity, selectedKey, exp);
        }

        AppToast.show(activity, "EXP " + exp + " 획득");
        return true;
    }

    private static boolean giveLimitedExp(
            Activity activity,
            String countKey,
            int dailyLimit,
            int exp
    ) {
        if (activity == null || activity.isFinishing()) return false;

        resetIfNewDay(activity);

        SharedPreferences p = prefs(activity);
        int count = p.getInt(countKey, 0);

        if (count >= dailyLimit) {
            AppToast.show(activity, "오늘 EXP 획득 가능 횟수를 모두 사용했어요 (" + dailyLimit + "/" + dailyLimit + ")");
            return false;
        }

        PetPrefs.addExpAndMaybeLevelUp(activity, exp);

        String selectedKey = CollectionDisplayPrefs.getSelectedItemKey(activity);
        if (selectedKey != null && !selectedKey.trim().isEmpty()) {
            CollectionPetStatePrefs.ensureState(activity, selectedKey);
            CollectionPetStatePrefs.addExpAndMaybeLevelUp(activity, selectedKey, exp);
        }

        int nextCount = count + 1;
        p.edit().putInt(countKey, nextCount).apply();

        AppToast.show(activity, "EXP " + exp + " 획득 (" + nextCount + "/" + dailyLimit + ")");
        return true;
    }

    public static boolean giveFridgeAddExp(Activity activity) {
        return giveLimitedExp(
                activity,
                KEY_FRIDGE_ADD_COUNT,
                DAILY_LIMIT_FRIDGE_ADD,
                EXP_FRIDGE_ADD
        );
    }

    public static boolean giveExpiryManualAddExp(Activity activity) {
        return giveLimitedExp(
                activity,
                KEY_EXPIRY_ADD_COUNT,
                DAILY_LIMIT_EXPIRY_ADD,
                EXP_EXPIRY_MANUAL_ADD
        );
    }

    public static boolean giveExpiredDeleteExp(Activity activity) {
        return giveLimitedExp(
                activity,
                KEY_EXPIRED_DELETE_COUNT,
                DAILY_LIMIT_EXPIRED_DELETE,
                EXP_EXPIRED_DELETE
        );
    }

    public static boolean giveShoppingCompleteExp(Activity activity) {
        return giveLimitedExp(
                activity,
                KEY_SHOPPING_COMPLETE_COUNT,
                DAILY_LIMIT_SHOPPING_COMPLETE,
                EXP_SHOPPING_COMPLETE
        );
    }

    public static boolean giveCookCompleteExp(Activity activity) {
        return giveLimitedExp(
                activity,
                KEY_COOK_COMPLETE_COUNT,
                DAILY_LIMIT_COOK_COMPLETE,
                EXP_COOK_COMPLETE
        );
    }

    public static boolean giveRecipeCreateExp(Activity activity) {
        return giveExp(activity, EXP_RECIPE_CREATE);
    }
}