package com.namgyun.tamakitchen.analytics;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AppAnalytics {

    private static FirebaseAnalytics analytics;

    private AppAnalytics() {
    }

    private static FirebaseAnalytics get(Context context) {
        if (analytics == null && context != null) {
            analytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
        }
        return analytics;
    }

    public static void logScreen(Context context, String screenName) {
        FirebaseAnalytics firebaseAnalytics = get(context);
        if (firebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName);

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logClick(Context context, String buttonName) {
        FirebaseAnalytics firebaseAnalytics = get(context);
        if (firebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("button_name", buttonName);

        firebaseAnalytics.logEvent("button_click", bundle);
    }

    public static void logRecipeView(Context context, String recipeName) {
        FirebaseAnalytics firebaseAnalytics = get(context);
        if (firebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("recipe_name", recipeName == null ? "" : recipeName);

        firebaseAnalytics.logEvent("recipe_view", bundle);
    }

    public static void logFridgeAdd(Context context, String ingredientName) {
        FirebaseAnalytics firebaseAnalytics = get(context);
        if (firebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("ingredient_name", ingredientName == null ? "" : ingredientName);

        firebaseAnalytics.logEvent("fridge_add_ingredient", bundle);
    }

    public static void logPetAction(Context context, String actionName) {
        FirebaseAnalytics firebaseAnalytics = get(context);
        if (firebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("action_name", actionName == null ? "" : actionName);

        firebaseAnalytics.logEvent("pet_action", bundle);
    }
}