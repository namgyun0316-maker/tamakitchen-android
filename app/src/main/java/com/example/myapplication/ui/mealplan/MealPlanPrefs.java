package com.namgyun.tamakitchen.ui.mealplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;

public class MealPlanPrefs {

    private static final String PREFS_NAME = "meal_plan_prefs";

    private static final String KEY_ID_SUFFIX = "_recipe_id";
    private static final String KEY_NAME_SUFFIX = "_recipe_name";
    private static final String KEY_SUMMARY_SUFFIX = "_recipe_summary";
    private static final String KEY_IMAGE_SUFFIX = "_recipe_image";
    private static final String KEY_THUMB_SUFFIX = "_recipe_thumb";

    public static final String DAY_SUN = "SUN";
    public static final String DAY_MON = "MON";
    public static final String DAY_TUE = "TUE";
    public static final String DAY_WED = "WED";
    public static final String DAY_THU = "THU";
    public static final String DAY_FRI = "FRI";
    public static final String DAY_SAT = "SAT";

    private MealPlanPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveRecipe(Context context,
                                  int year,
                                  int month,
                                  int weekOfMonth,
                                  String dayKey,
                                  RecipeResponse recipe) {
        if (context == null || recipe == null || TextUtils.isEmpty(dayKey)) return;

        String prefix = makePrefix(year, month, weekOfMonth, dayKey);

        prefs(context).edit()
                .putLong(prefix + KEY_ID_SUFFIX, recipe.getId() == null ? -1L : recipe.getId())
                .putString(prefix + KEY_NAME_SUFFIX, safe(recipe.getName()))
                .putString(prefix + KEY_SUMMARY_SUFFIX, safe(recipe.getSummary()))
                .putString(prefix + KEY_IMAGE_SUFFIX, safe(recipe.getImageUrl()))
                .putString(prefix + KEY_THUMB_SUFFIX, safe(recipe.getThumbnailUrl()))
                .apply();
    }

    public static MealPlanDayItem getDayItem(Context context,
                                             int year,
                                             int month,
                                             int weekOfMonth,
                                             String dayKey,
                                             String dayLabel,
                                             int dayOfMonth) {
        SharedPreferences p = prefs(context);
        String prefix = makePrefix(year, month, weekOfMonth, dayKey);

        long id = p.getLong(prefix + KEY_ID_SUFFIX, -1L);
        String name = p.getString(prefix + KEY_NAME_SUFFIX, "");
        String summary = p.getString(prefix + KEY_SUMMARY_SUFFIX, "");
        String imageUrl = p.getString(prefix + KEY_IMAGE_SUFFIX, "");
        String thumbUrl = p.getString(prefix + KEY_THUMB_SUFFIX, "");

        return new MealPlanDayItem(
                dayKey,
                dayLabel,
                dayOfMonth,
                id > 0 ? id : null,
                name,
                summary,
                imageUrl,
                thumbUrl
        );
    }

    public static void removeDay(Context context,
                                 int year,
                                 int month,
                                 int weekOfMonth,
                                 String dayKey) {
        if (context == null || TextUtils.isEmpty(dayKey)) return;

        String prefix = makePrefix(year, month, weekOfMonth, dayKey);

        prefs(context).edit()
                .remove(prefix + KEY_ID_SUFFIX)
                .remove(prefix + KEY_NAME_SUFFIX)
                .remove(prefix + KEY_SUMMARY_SUFFIX)
                .remove(prefix + KEY_IMAGE_SUFFIX)
                .remove(prefix + KEY_THUMB_SUFFIX)
                .apply();
    }

    private static String makePrefix(int year, int month, int weekOfMonth, String dayKey) {
        return year + "_" + month + "_" + weekOfMonth + "_" + dayKey;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}