package com.namgyun.tamakitchen.ui.menu;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.RecipeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.mealplan.MealPlanPrefs;
import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteRecipeActivity extends AppCompatActivity {

    private static final String PREFS_FAVORITE = "favorite_prefs";
    private static final String KEY_FAV_PREFIX = "fav_";

    private RecyclerView rvFavorite;
    private TextView tvEmpty;
    private ImageView btnBack;

    private FavoriteRecipeAdapter adapter;
    private RecipeApiService service;

    private final List<RecipeResponse> favoriteRecipes = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_recipe);

        rvFavorite = findViewById(R.id.rvFavoriteRecipes);
        tvEmpty = findViewById(R.id.tvEmptyFavorites);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        adapter = new FavoriteRecipeAdapter(
                this,
                item -> showWeekPickerDialog(item)
        );

        rvFavorite.setLayoutManager(new LinearLayoutManager(this));
        rvFavorite.setAdapter(adapter);

        service = RetrofitClient.getRecipeApi();

        loadFavoriteRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteRecipes();
    }

    private void loadFavoriteRecipes() {
        if (service == null) return;

        service.getAllRecipes().enqueue(new Callback<List<RecipeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecipeResponse>> call,
                                   @NonNull Response<List<RecipeResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showEmpty(true);
                    AppToast.show(FavoriteRecipeActivity.this, "즐겨찾기 레시피를 불러오지 못했어.");
                    return;
                }

                SharedPreferences prefs = getSharedPreferences(PREFS_FAVORITE, MODE_PRIVATE);

                favoriteRecipes.clear();

                for (RecipeResponse recipe : response.body()) {
                    if (recipe == null || recipe.getId() == null) continue;

                    boolean isFavorite = prefs.getBoolean(KEY_FAV_PREFIX + recipe.getId(), false);
                    if (isFavorite) {
                        favoriteRecipes.add(recipe);
                    }
                }

                adapter.submitItems(new ArrayList<>(favoriteRecipes));
                showEmpty(favoriteRecipes.isEmpty());
            }

            @Override
            public void onFailure(@NonNull Call<List<RecipeResponse>> call,
                                  @NonNull Throwable t) {
                showEmpty(true);
                AppToast.show(FavoriteRecipeActivity.this, "네트워크 오류가 발생했어.");
            }
        });
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvFavorite.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showWeekPickerDialog(RecipeResponse recipe) {
        if (recipe == null) return;

        Calendar today = Calendar.getInstance(Locale.KOREA);
        final int year = today.get(Calendar.YEAR);
        final int month = today.get(Calendar.MONTH) + 1;
        final int todayDay = today.get(Calendar.DAY_OF_MONTH);

        List<List<Integer>> weeks = buildVisibleWeeks(year, month);
        if (weeks.isEmpty()) {
            AppToast.show(this, "주차 정보를 만들 수 없어.");
            return;
        }

        String[] weekLabels = new String[weeks.size()];
        int checkedIndex = findWeekIndexForDay(year, month, todayDay) - 1;
        if (checkedIndex < 0) checkedIndex = 0;

        for (int i = 0; i < weeks.size(); i++) {
            List<Integer> days = weeks.get(i);
            int startDay = days.get(0);
            int endDay = days.get(days.size() - 1);
            weekLabels[i] = month + "월 " + (i + 1) + "주차 (" + startDay + "일~" + endDay + "일)";
        }

        final int[] selectedWeekIndex = {checkedIndex};

        new AlertDialog.Builder(this)
                .setTitle("추가할 주차 선택")
                .setSingleChoiceItems(weekLabels, checkedIndex, (dialog, which) -> selectedWeekIndex[0] = which)
                .setNegativeButton("취소", null)
                .setPositiveButton("다음", (dialog, which) -> {
                    int weekOfMonth = selectedWeekIndex[0] + 1;
                    showDayPickerDialog(recipe, year, month, weekOfMonth, weeks.get(selectedWeekIndex[0]));
                })
                .show();
    }

    private void showDayPickerDialog(RecipeResponse recipe,
                                     int year,
                                     int month,
                                     int weekOfMonth,
                                     List<Integer> visibleDays) {
        if (recipe == null || visibleDays == null || visibleDays.isEmpty()) return;

        String[] labels = new String[visibleDays.size()];
        String[] dayKeys = new String[visibleDays.size()];
        String[] dayNames = new String[visibleDays.size()];

        for (int i = 0; i < visibleDays.size(); i++) {
            int dayOfMonth = visibleDays.get(i);

            Calendar cal = Calendar.getInstance(Locale.KOREA);
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            String dayKey = mapDayKey(dayOfWeek);
            String dayLabel = mapDayLabel(dayOfWeek);

            dayKeys[i] = dayKey;
            dayNames[i] = dayLabel;
            labels[i] = dayOfMonth + "일 (" + dayLabel + ")";
        }

        final int[] selectedIndex = {0};

        new AlertDialog.Builder(this)
                .setTitle("요일 선택")
                .setSingleChoiceItems(labels, 0, (dialog, which) -> selectedIndex[0] = which)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String selectedDayKey = dayKeys[selectedIndex[0]];
                    String selectedDayLabel = dayNames[selectedIndex[0]];

                    MealPlanPrefs.saveRecipe(
                            FavoriteRecipeActivity.this,
                            year,
                            month,
                            weekOfMonth,
                            selectedDayKey,
                            recipe
                    );

                    AppToast.show(
                            FavoriteRecipeActivity.this,
                            "'" + safe(recipe.getName()) + "'를 "
                                    + year + "년 "
                                    + month + "월 "
                                    + weekOfMonth + "주차 "
                                    + selectedDayLabel + "요일 식단에 추가했어."
                    );
                })
                .show();
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

    private int findWeekIndexForDay(int year, int month, int dayOfMonth) {
        List<List<Integer>> weeks = buildVisibleWeeks(year, month);
        for (int i = 0; i < weeks.size(); i++) {
            if (weeks.get(i).contains(dayOfMonth)) {
                return i + 1;
            }
        }
        return 1;
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

    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "선택한 레시피";
        }
        return value.trim();
    }
}