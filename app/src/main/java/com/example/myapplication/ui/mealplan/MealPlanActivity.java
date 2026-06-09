package com.namgyun.tamakitchen.ui.mealplan;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.MainActivity;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.recipe.RecipeWriteActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MealPlanActivity extends AppCompatActivity {

    private static final String PREFS_FAVORITE = "favorite_prefs";
    private static final String KEY_FAV_PREFIX = "fav_";

    public static final String EXTRA_OPEN_TAB = "open_tab";
    public static final String TAB_RECIPE = "recipe";

    public static final String EXTRA_YEAR = "extra_year";
    public static final String EXTRA_MONTH = "extra_month";
    public static final String EXTRA_WEEK_OF_MONTH = "extra_week_of_month";

    private RecyclerView rvMealPlan;
    private TextView btnPrevWeek;
    private TextView btnNextWeek;
    private TextView tvWeekTitle;
    private View btnWeeklyMissing;

    private MealPlanDayAdapter adapter;

    private int displayYear;
    private int displayMonth;
    private int displayWeekOfMonth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan);

        rvMealPlan = findViewById(R.id.rvMealPlan);
        btnPrevWeek = findViewById(R.id.btnPrevWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        tvWeekTitle = findViewById(R.id.tvWeekTitle);
        btnWeeklyMissing = findViewById(R.id.btnWeeklyMissing);

        Calendar today = Calendar.getInstance(Locale.KOREA);
        displayYear = today.get(Calendar.YEAR);
        displayMonth = today.get(Calendar.MONTH) + 1;
        displayWeekOfMonth = findWeekIndexForDay(
                displayYear,
                displayMonth,
                today.get(Calendar.DAY_OF_MONTH)
        );

        adapter = new MealPlanDayAdapter(this::onDayClicked);
        rvMealPlan.setLayoutManager(new LinearLayoutManager(this));
        rvMealPlan.setAdapter(adapter);

        btnPrevWeek.setOnClickListener(v -> movePrevVisibleWeek());
        btnNextWeek.setOnClickListener(v -> moveNextVisibleWeek());

        if (btnWeeklyMissing != null) {
            btnWeeklyMissing.setOnClickListener(v -> openWeeklyMissingScreen());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        normalizeDisplayState();
        refreshWeek();
    }

    private void refreshWeek() {
        normalizeDisplayState();

        tvWeekTitle.setText(String.format(
                Locale.KOREA,
                "%d년 %d월 %d주차",
                displayYear,
                displayMonth,
                displayWeekOfMonth
        ));

        List<MealPlanDayItem> items = buildVisibleWeekItems(
                displayYear,
                displayMonth,
                displayWeekOfMonth
        );

        applyFavoriteState(items);
        adapter.submit(items);
    }

    private void movePrevVisibleWeek() {
        normalizeDisplayState();

        if (displayWeekOfMonth > 1) {
            displayWeekOfMonth--;
        } else {
            if (displayMonth == 1) {
                displayYear--;
                displayMonth = 12;
            } else {
                displayMonth--;
            }

            displayWeekOfMonth = getVisibleWeekCount(displayYear, displayMonth);
        }

        refreshWeek();
    }

    private void moveNextVisibleWeek() {
        normalizeDisplayState();

        int maxWeek = getVisibleWeekCount(displayYear, displayMonth);

        if (displayWeekOfMonth < maxWeek) {
            displayWeekOfMonth++;
        } else {
            if (displayMonth == 12) {
                displayYear++;
                displayMonth = 1;
            } else {
                displayMonth++;
            }

            displayWeekOfMonth = 1;
        }

        refreshWeek();
    }

    private void normalizeDisplayState() {
        if (displayMonth < 1) {
            displayMonth = 1;
        } else if (displayMonth > 12) {
            displayMonth = 12;
        }

        int maxWeek = getVisibleWeekCount(displayYear, displayMonth);

        if (displayWeekOfMonth < 1) {
            displayWeekOfMonth = 1;
        }

        if (displayWeekOfMonth > maxWeek) {
            displayWeekOfMonth = maxWeek;
        }
    }

    private int getVisibleWeekCount(int year, int month) {
        return buildVisibleWeeks(year, month).size();
    }

    private int findWeekIndexForDay(int year, int month, int dayOfMonth) {
        List<List<Integer>> weeks = buildVisibleWeeks(year, month);

        for (int i = 0; i < weeks.size(); i++) {
            List<Integer> week = weeks.get(i);

            if (week.contains(dayOfMonth)) {
                return i + 1;
            }
        }

        return 1;
    }

    private List<List<Integer>> buildVisibleWeeks(int year, int month) {
        List<List<Integer>> weeks = new ArrayList<>();

        Calendar cal = Calendar.getInstance(Locale.KOREA);
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.setMinimalDaysInFirstWeek(1);
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<Integer> currentWeek = new ArrayList<>();

        for (int day = 1; day <= lastDay; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            currentWeek.add(day);

            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day == lastDay) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>();
            }
        }

        return weeks;
    }

    private List<MealPlanDayItem> buildVisibleWeekItems(int year, int month, int weekOfMonth) {
        List<MealPlanDayItem> result = new ArrayList<>();
        List<List<Integer>> weeks = buildVisibleWeeks(year, month);

        if (weekOfMonth < 1 || weekOfMonth > weeks.size()) {
            return result;
        }

        List<Integer> days = weeks.get(weekOfMonth - 1);

        for (Integer dayOfMonth : days) {
            Calendar cal = Calendar.getInstance(Locale.KOREA);
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            String dayKey = mapDayKey(dayOfWeek);
            String dayLabel = mapDayLabel(dayOfWeek);

            MealPlanDayItem item = MealPlanPrefs.getDayItem(
                    this,
                    year,
                    month,
                    weekOfMonth,
                    dayKey,
                    dayLabel,
                    dayOfMonth
            );

            result.add(item);
        }

        return result;
    }

    private void applyFavoriteState(List<MealPlanDayItem> items) {
        if (items == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_FAVORITE, MODE_PRIVATE);

        for (MealPlanDayItem item : items) {
            if (item == null || item.getRecipeId() == null) continue;

            boolean favorite = prefs.getBoolean(
                    KEY_FAV_PREFIX + item.getRecipeId(),
                    false
            );

            item.setFavorite(favorite);
        }
    }

    private void onDayClicked(MealPlanDayItem item) {
        if (item == null) return;

        if (!item.hasRecipe()) {
            showCustomDialog(item);
            return;
        }

        showMealPlanMenuDialog(item);
    }

    private void showMealPlanMenuDialog(MealPlanDayItem item) {
        View view = getLayoutInflater().inflate(R.layout.dialog_mealplan_recipe_menu, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvSubtitle);
        View layoutCompare = view.findViewById(R.id.layoutCompare);
        View layoutRemove = view.findViewById(R.id.layoutRemove);
        View layoutClose = view.findViewById(R.id.layoutClose);

        tvTitle.setText(item.getDayLabel() + "요일 식단");

        if (item.getRecipeName() != null && !item.getRecipeName().trim().isEmpty()) {
            tvSubtitle.setText(item.getRecipeName());
        } else {
            tvSubtitle.setText("등록된 메뉴를 관리해보자.");
        }

        layoutCompare.setOnClickListener(v -> {
            openCompareDialog(item);
            dialog.dismiss();
        });

        layoutRemove.setOnClickListener(v -> {
            MealPlanPrefs.removeDay(
                    this,
                    displayYear,
                    displayMonth,
                    displayWeekOfMonth,
                    item.getDayKey()
            );

            refreshWeek();
            AppToast.show(this, "식단에서 제거했어.");
            dialog.dismiss();
        });

        layoutClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showCustomDialog(MealPlanDayItem item) {
        View view = getLayoutInflater().inflate(R.layout.dialog_mealplan_action, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText(item.getDayLabel() + "요일 식단 추가");

        view.findViewById(R.id.btnGoRecipe).setOnClickListener(v -> {
            openRecipeScreen();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnWrite).setOnClickListener(v -> {
            startActivity(new Intent(this, RecipeWriteActivity.class));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openRecipeScreen() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(EXTRA_OPEN_TAB, TAB_RECIPE);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            AppToast.show(this, "레시피 화면 이동 실패");
        }
    }

    private void openCompareDialog(MealPlanDayItem item) {
        Intent intent = new Intent(this, MealPlanRecipeCompareActivity.class);
        intent.putExtra(MealPlanRecipeCompareActivity.EXTRA_DAY_KEY, item.getDayKey());
        intent.putExtra(MealPlanRecipeCompareActivity.EXTRA_DAY_LABEL, item.getDayLabel());
        intent.putExtra(MealPlanRecipeCompareActivity.EXTRA_RECIPE_ID, item.getRecipeId());
        intent.putExtra(MealPlanRecipeCompareActivity.EXTRA_RECIPE_NAME, item.getRecipeName());
        startActivity(intent);
    }

    private void openWeeklyMissingScreen() {
        Intent intent = new Intent(this, MealPlanWeeklyMissingActivity.class);
        intent.putExtra(EXTRA_YEAR, displayYear);
        intent.putExtra(EXTRA_MONTH, displayMonth);
        intent.putExtra(EXTRA_WEEK_OF_MONTH, displayWeekOfMonth);
        startActivity(intent);
    }

    private String mapDayKey(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return MealPlanPrefs.DAY_SUN;

            case Calendar.MONDAY:
                return MealPlanPrefs.DAY_MON;

            case Calendar.TUESDAY:
                return MealPlanPrefs.DAY_TUE;

            case Calendar.WEDNESDAY:
                return MealPlanPrefs.DAY_WED;

            case Calendar.THURSDAY:
                return MealPlanPrefs.DAY_THU;

            case Calendar.FRIDAY:
                return MealPlanPrefs.DAY_FRI;

            case Calendar.SATURDAY:
            default:
                return MealPlanPrefs.DAY_SAT;
        }
    }

    private String mapDayLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "일";

            case Calendar.MONDAY:
                return "월";

            case Calendar.TUESDAY:
                return "화";

            case Calendar.WEDNESDAY:
                return "수";

            case Calendar.THURSDAY:
                return "목";

            case Calendar.FRIDAY:
                return "금";

            case Calendar.SATURDAY:
            default:
                return "토";
        }
    }
}