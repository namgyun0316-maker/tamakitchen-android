package com.namgyun.tamakitchen.ui.mealplan;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.network.RecipeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.network.ShoppingItemRequest;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.fridge.FridgeItem;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.namgyun.tamakitchen.ui.fridge.GuestFridgeStore;
import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;
import com.namgyun.tamakitchen.ui.shopping.ShoppingItem;
import com.namgyun.tamakitchen.ui.shopping.StorePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealPlanWeeklyMissingActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvSubTitle;
    private TextView tvWeeklyUsing;
    private TextView tvWeeklyMissing;
    private TextView btnAddAllToShopping;
    private ImageView btnBack;

    private RecipeApiService recipeApi;
    private FridgeApiService fridgeApi;
    private com.namgyun.tamakitchen.network.ShoppingApi shoppingApi;

    private final FridgeModeManager mode = new FridgeModeManager();

    private int year;
    private int month;
    private int weekOfMonth;

    private final List<MealPlanDayItem> weekItems = new ArrayList<>();
    private final List<RecipeResponse> recipeResponses = new ArrayList<>();

    private final List<String> usingIngredients = new ArrayList<>();
    private final List<String> missingIngredients = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan_weekly_missing);

        tvTitle = findViewById(R.id.tvMealPlanWeeklyTitle);
        tvSubTitle = findViewById(R.id.tvMealPlanWeeklySubTitle);
        tvWeeklyUsing = findViewById(R.id.tvMealPlanWeeklyUsingIngredients);
        tvWeeklyMissing = findViewById(R.id.tvMealPlanWeeklyMissingIngredients);
        btnAddAllToShopping = findViewById(R.id.btnMealPlanAddAllMissingToShopping);
        btnBack = findViewById(R.id.btnBack);

        recipeApi = FridgeApi.getClient().create(RecipeApiService.class);
        fridgeApi = FridgeApi.getClient().create(FridgeApiService.class);
        shoppingApi = RetrofitClient.getShoppingApi();

        year = getIntent().getIntExtra(MealPlanActivity.EXTRA_YEAR, -1);
        month = getIntent().getIntExtra(MealPlanActivity.EXTRA_MONTH, -1);
        weekOfMonth = getIntent().getIntExtra(MealPlanActivity.EXTRA_WEEK_OF_MONTH, -1);

        tvTitle.setText("주간 부족 재료");
        tvSubTitle.setText(String.format(Locale.KOREA, "%d년 %d월 %d주차 식단 기준", year, month, weekOfMonth));

        btnBack.setOnClickListener(v -> finish());
        btnAddAllToShopping.setOnClickListener(v -> showStorePickerThenAdd());

        if (year <= 0 || month <= 0 || weekOfMonth <= 0) {
            AppToast.show(this, "주차 정보가 올바르지 않아.");
            finish();
            return;
        }

        loadWeekItems();
    }

    private void loadWeekItems() {
        weekItems.clear();
        recipeResponses.clear();
        usingIngredients.clear();
        missingIngredients.clear();

        weekItems.addAll(buildVisibleWeekItems(year, month, weekOfMonth));

        List<Long> recipeIds = new ArrayList<>();

        for (MealPlanDayItem item : weekItems) {
            if (item != null && item.hasRecipe() && item.getRecipeId() != null) {
                recipeIds.add(item.getRecipeId());
            }
        }

        if (recipeIds.isEmpty()) {
            tvWeeklyUsing.setText("이번 주 등록된 식단이 없어.");
            tvWeeklyMissing.setText("부족한 재료:\n-");
            btnAddAllToShopping.setEnabled(false);
            btnAddAllToShopping.setAlpha(0.5f);
            return;
        }

        loadRecipeDetailsSequentially(recipeIds, 0);
    }

    private void loadRecipeDetailsSequentially(List<Long> recipeIds, int index) {
        if (index >= recipeIds.size()) {
            loadFridgeAndCompare();
            return;
        }

        long recipeId = recipeIds.get(index);

        recipeApi.getRecipeById(recipeId).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipeResponses.add(response.body());
                }

                loadRecipeDetailsSequentially(recipeIds, index + 1);
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                loadRecipeDetailsSequentially(recipeIds, index + 1);
            }
        });
    }

    private void loadFridgeAndCompare() {
        long userId = mode.getUserIdSafe(this);

        if (userId <= 0) {
            AppToast.show(this, "로그인이 필요해.");
            finish();
            return;
        }

        if (mode.isGuestMode(this)) {
            List<FridgeItem> local = GuestFridgeStore.load(this);
            compareWeekly(recipeResponses, local == null ? new ArrayList<>() : local);
            return;
        }

        if (mode.isSharedFridgeMode(this)) {
            long fridgeId = mode.getSharedFridgeIdSafe(this);

            if (fridgeId <= 0) {
                AppToast.show(this, "공동 냉장고 정보를 찾지 못했어.");
                return;
            }

            fridgeApi.getSharedFridgeItems(fridgeId, userId).enqueue(new Callback<List<FridgeItem>>() {
                @Override
                public void onResponse(Call<List<FridgeItem>> call, Response<List<FridgeItem>> response) {
                    compareWeekly(recipeResponses, response.body() == null ? new ArrayList<>() : response.body());
                }

                @Override
                public void onFailure(Call<List<FridgeItem>> call, Throwable t) {
                    AppToast.show(
                            MealPlanWeeklyMissingActivity.this,
                            NetworkErrorUtil.getUserMessage(t)
                    );
                }
            });

        } else {
            fridgeApi.getFridgeItems(userId).enqueue(new Callback<List<FridgeItem>>() {
                @Override
                public void onResponse(Call<List<FridgeItem>> call, Response<List<FridgeItem>> response) {
                    compareWeekly(recipeResponses, response.body() == null ? new ArrayList<>() : response.body());
                }

                @Override
                public void onFailure(Call<List<FridgeItem>> call, Throwable t) {
                    AppToast.show(
                            MealPlanWeeklyMissingActivity.this,
                            NetworkErrorUtil.getUserMessage(t)
                    );
                }
            });
        }
    }

    private void compareWeekly(List<RecipeResponse> recipes, List<FridgeItem> fridgeItems) {
        usingIngredients.clear();
        missingIngredients.clear();

        Set<String> fridgeNames = new LinkedHashSet<>();

        for (FridgeItem item : fridgeItems) {
            if (item == null || TextUtils.isEmpty(item.getName())) continue;
            fridgeNames.add(normalize(item.getName()));
        }

        LinkedHashSet<String> usingSet = new LinkedHashSet<>();
        LinkedHashSet<String> missingSet = new LinkedHashSet<>();

        for (RecipeResponse recipe : recipes) {
            if (recipe == null) continue;

            List<String> names = extractRequiredIngredientNames(recipe);

            for (String name : names) {
                if (TextUtils.isEmpty(name)) continue;

                if (fridgeNames.contains(normalize(name))) {
                    usingSet.add(name);
                } else {
                    missingSet.add(name);
                }
            }
        }

        usingIngredients.addAll(usingSet);
        missingIngredients.addAll(missingSet);

        tvWeeklyUsing.setText(
                usingIngredients.isEmpty()
                        ? "냉장고에 있는 재료:\n-"
                        : "냉장고에 있는 재료:\n" + formatBulletLines(usingIngredients)
        );

        tvWeeklyMissing.setText(
                missingIngredients.isEmpty()
                        ? "부족한 재료:\n-"
                        : "부족한 재료:\n" + formatBulletLines(missingIngredients)
        );

        btnAddAllToShopping.setEnabled(!missingIngredients.isEmpty());
        btnAddAllToShopping.setAlpha(missingIngredients.isEmpty() ? 0.5f : 1f);
    }

    private List<String> extractRequiredIngredientNames(RecipeResponse recipe) {
        List<String> out = new ArrayList<>();

        List<RecipeResponse.RecipeIngredientItem> ingredientItems = recipe.getIngredientItems();

        if (ingredientItems != null && !ingredientItems.isEmpty()) {
            for (RecipeResponse.RecipeIngredientItem ingredient : ingredientItems) {
                if (ingredient == null) continue;

                String type = ingredient.getType() == null
                        ? ""
                        : ingredient.getType().trim().toUpperCase(Locale.ROOT);

                if ("SUBSTITUTE".equals(type) || "OPTIONAL".equals(type)) continue;

                String name = safe(ingredient.getName());

                if (!TextUtils.isEmpty(name)) {
                    out.add(name);
                }
            }

            return out;
        }

        return splitCsv(recipe.getIngredients());
    }

    private void showStorePickerThenAdd() {
        long userId = mode.getUserIdSafe(this);

        if (userId <= 0) {
            AppToast.show(this, "로그인이 필요해.");
            return;
        }

        List<String> rawMissingNames = extractRawMissingNames();

        if (rawMissingNames.isEmpty()) {
            AppToast.show(this, "장보기에 담을 부족 재료가 없어.");
            return;
        }

        StorePickerDialog dialog = new StorePickerDialog(userId, (storeId, storeName) -> {
            addAllMissingToShoppingWithStore(storeId, storeName);
        });

        dialog.show(getSupportFragmentManager(), "StorePickerDialog");
    }

    private void addAllMissingToShoppingWithStore(
            @Nullable Long selectedStoreId,
            @NonNull String selectedStoreName
    ) {
        long userId = mode.getUserIdSafe(this);

        if (userId <= 0) {
            AppToast.show(this, "로그인이 필요해.");
            return;
        }

        List<String> rawMissingNames = extractRawMissingNames();

        if (rawMissingNames.isEmpty()) {
            AppToast.show(this, "장보기에 담을 부족 재료가 없어.");
            return;
        }

        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        final int total = rawMissingNames.size();
        final int[] finished = {0};
        final int[] success = {0};

        final String storeLabel = TextUtils.isEmpty(selectedStoreName)
                ? "미지정"
                : selectedStoreName.trim();

        for (String name : rawMissingNames) {
            ShoppingItemRequest req = new ShoppingItemRequest(
                    name,
                    1,
                    "ic_launcher_foreground",
                    userId,
                    0,
                    dateKey,
                    "개",
                    selectedStoreId
            );

            shoppingApi.addItem(req).enqueue(new Callback<ShoppingItem>() {
                @Override
                public void onResponse(Call<ShoppingItem> call, Response<ShoppingItem> response) {
                    finished[0]++;

                    if (response.isSuccessful()) {
                        success[0]++;
                    }

                    if (finished[0] == total) {
                        AppToast.show(
                                MealPlanWeeklyMissingActivity.this,
                                storeLabel + "에 " + success[0] + "개 담았어."
                        );
                    }
                }

                @Override
                public void onFailure(Call<ShoppingItem> call, Throwable t) {
                    finished[0]++;

                    if (finished[0] == total) {
                        AppToast.show(
                                MealPlanWeeklyMissingActivity.this,
                                success[0] > 0
                                        ? "장보기 담기 완료 (성공 " + success[0] + "/" + total + ")"
                                        : NetworkErrorUtil.getUserMessage(t)
                        );
                    }
                }
            });
        }
    }

    private List<String> extractRawMissingNames() {
        LinkedHashSet<String> missingSet = new LinkedHashSet<>();

        Set<String> fridgeNames = new LinkedHashSet<>();

        if (mode.isGuestMode(this)) {
            List<FridgeItem> localFridge = GuestFridgeStore.load(this);

            if (localFridge != null) {
                for (FridgeItem item : localFridge) {
                    if (item == null || TextUtils.isEmpty(item.getName())) continue;
                    fridgeNames.add(normalize(item.getName()));
                }
            }
        }

        for (RecipeResponse recipe : recipeResponses) {
            if (recipe == null) continue;

            List<String> names = extractRequiredIngredientNames(recipe);

            for (String name : names) {
                if (TextUtils.isEmpty(name)) continue;
                if (!fridgeNames.isEmpty() && fridgeNames.contains(normalize(name))) continue;

                missingSet.add(name);
            }
        }

        return new ArrayList<>(missingSet);
    }

    private String formatBulletLines(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            sb.append("- ").append(items.get(i));

            if (i < items.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
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

    private List<String> splitCsv(String csv) {
        List<String> out = new ArrayList<>();

        if (TextUtils.isEmpty(csv)) return out;

        String[] arr = csv.split(",");

        for (String s : arr) {
            if (s == null) continue;

            String t = s.trim();

            if (!t.isEmpty()) {
                out.add(t);
            }
        }

        return out;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace(" ", "").trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
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