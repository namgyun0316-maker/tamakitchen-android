package com.namgyun.tamakitchen.ui.mealplan;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealPlanRecipeCompareActivity extends AppCompatActivity {

    public static final String EXTRA_DAY_KEY = "extra_day_key";
    public static final String EXTRA_DAY_LABEL = "extra_day_label";
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";
    public static final String EXTRA_RECIPE_NAME = "extra_recipe_name";

    private TextView tvTitle;
    private TextView tvUsing;
    private TextView tvMissing;
    private Button btnAddMissingToShopping;
    private ImageView btnBack;

    private RecipeApiService recipeApi;
    private FridgeApiService fridgeApi;
    private com.namgyun.tamakitchen.network.ShoppingApi shoppingApi;
    private final FridgeModeManager mode = new FridgeModeManager();

    private long recipeId;
    private String recipeName;

    private final List<String> usingIngredients = new ArrayList<>();
    private final List<String> missingIngredients = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan_compare);

        tvTitle = findViewById(R.id.tvMealPlanCompareTitle);
        tvUsing = findViewById(R.id.tvMealPlanUsingIngredients);
        tvMissing = findViewById(R.id.tvMealPlanMissingIngredients);
        btnAddMissingToShopping = findViewById(R.id.btnMealPlanAddMissingToShopping);
        btnBack = findViewById(R.id.btnBack);

        recipeApi = FridgeApi.getClient().create(RecipeApiService.class);
        fridgeApi = FridgeApi.getClient().create(FridgeApiService.class);
        shoppingApi = RetrofitClient.getShoppingApi();

        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        recipeName = getIntent().getStringExtra(EXTRA_RECIPE_NAME);

        tvTitle.setText(TextUtils.isEmpty(recipeName) ? "식단 재료 비교" : recipeName);

        btnBack.setOnClickListener(v -> finish());
        btnAddMissingToShopping.setOnClickListener(v -> addMissingToShopping());

        if (recipeId <= 0L) {
            AppToast.show(this, "레시피 정보가 없습니다.");
            finish();
            return;
        }

        loadRecipeDetail();
    }

    private void loadRecipeDetail() {
        recipeApi.getRecipeById(recipeId).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    AppToast.show(MealPlanRecipeCompareActivity.this, "레시피를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                RecipeResponse recipe = response.body();
                loadFridgeAndCompare(recipe);
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                AppToast.show(
                        MealPlanRecipeCompareActivity.this,
                        NetworkErrorUtil.getUserMessage(t)
                );
            }
        });
    }

    private void loadFridgeAndCompare(RecipeResponse recipe) {
        long userId = mode.getUserIdSafe(this);

        if (userId <= 0) {
            AppToast.show(this, "로그인이 필요해요.");
            return;
        }

        if (mode.isGuestMode(this)) {
            List<FridgeItem> local = GuestFridgeStore.load(this);
            compareRecipeWithFridge(recipe, local == null ? new ArrayList<>() : local);
            return;
        }

        if (mode.isSharedFridgeMode(this)) {
            long fridgeId = mode.getSharedFridgeIdSafe(this);

            if (fridgeId <= 0) {
                AppToast.show(this, "공동 냉장고 정보를 찾지 못했어요.");
                return;
            }

            fridgeApi.getSharedFridgeItems(fridgeId, userId)
                    .enqueue(new Callback<List<FridgeItem>>() {
                        @Override
                        public void onResponse(
                                Call<List<FridgeItem>> call,
                                Response<List<FridgeItem>> response
                        ) {
                            if (!response.isSuccessful()) {
                                AppToast.show(
                                        MealPlanRecipeCompareActivity.this,
                                        "냉장고 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요."
                                );
                                return;
                            }

                            compareRecipeWithFridge(
                                    recipe,
                                    response.body() == null
                                            ? new ArrayList<>()
                                            : response.body()
                            );
                        }

                        @Override
                        public void onFailure(
                                Call<List<FridgeItem>> call,
                                Throwable t
                        ) {
                            AppToast.show(
                                    MealPlanRecipeCompareActivity.this,
                                    NetworkErrorUtil.getUserMessage(t)
                            );
                        }
                    });

        } else {
            fridgeApi.getFridgeItems(userId)
                    .enqueue(new Callback<List<FridgeItem>>() {
                        @Override
                        public void onResponse(
                                Call<List<FridgeItem>> call,
                                Response<List<FridgeItem>> response
                        ) {
                            if (!response.isSuccessful()) {
                                AppToast.show(
                                        MealPlanRecipeCompareActivity.this,
                                        "냉장고 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요."
                                );
                                return;
                            }

                            compareRecipeWithFridge(
                                    recipe,
                                    response.body() == null
                                            ? new ArrayList<>()
                                            : response.body()
                            );
                        }

                        @Override
                        public void onFailure(
                                Call<List<FridgeItem>> call,
                                Throwable t
                        ) {
                            AppToast.show(
                                    MealPlanRecipeCompareActivity.this,
                                    NetworkErrorUtil.getUserMessage(t)
                            );
                        }
                    });
        }
    }

    private void compareRecipeWithFridge(
            RecipeResponse recipe,
            List<FridgeItem> fridgeItems
    ) {
        usingIngredients.clear();
        missingIngredients.clear();

        Set<String> fridgeNames = new LinkedHashSet<>();

        for (FridgeItem item : fridgeItems) {
            if (item == null || TextUtils.isEmpty(item.getName())) continue;

            fridgeNames.add(normalize(item.getName()));
        }

        List<RecipeResponse.RecipeIngredientItem> ingredientItems =
                recipe.getIngredientItems();

        if (ingredientItems != null && !ingredientItems.isEmpty()) {
            for (RecipeResponse.RecipeIngredientItem ingredient : ingredientItems) {
                if (ingredient == null) continue;

                String type = ingredient.getType() == null
                        ? ""
                        : ingredient.getType().trim().toUpperCase(Locale.ROOT);

                if ("SUBSTITUTE".equals(type)
                        || "OPTIONAL".equals(type)) continue;

                String name = safe(ingredient.getName());

                if (TextUtils.isEmpty(name)) continue;

                if (fridgeNames.contains(normalize(name))) {
                    usingIngredients.add(name);
                } else {
                    missingIngredients.add(name);
                }
            }

        } else {
            List<String> fallback = splitCsv(recipe.getIngredients());

            for (String name : fallback) {
                if (fridgeNames.contains(normalize(name))) {
                    usingIngredients.add(name);
                } else {
                    missingIngredients.add(name);
                }
            }
        }

        tvUsing.setText(
                usingIngredients.isEmpty()
                        ? "내 냉장고에 있는 재료: -"
                        : "내 냉장고에 있는 재료: "
                        + TextUtils.join(", ", usingIngredients)
        );

        tvMissing.setText(
                missingIngredients.isEmpty()
                        ? "부족한 재료: -"
                        : "부족한 재료: "
                        + TextUtils.join(", ", missingIngredients)
        );

        btnAddMissingToShopping.setEnabled(!missingIngredients.isEmpty());
        btnAddMissingToShopping.setAlpha(
                missingIngredients.isEmpty() ? 0.5f : 1f
        );
    }

    private void addMissingToShopping() {
        long userId = mode.getUserIdSafe(this);

        if (userId <= 0) {
            AppToast.show(this, "로그인이 필요해요.");
            return;
        }

        if (missingIngredients.isEmpty()) {
            AppToast.show(this, "장보기에 담을 부족 재료가 없어요.");
            return;
        }

        String dateKey = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.KOREA
        ).format(new Date());

        final int total = missingIngredients.size();
        final int[] finished = {0};
        final int[] success = {0};
        final boolean[] hasNetworkError = {false};
        final String[] networkMessage = {null};

        for (String name : missingIngredients) {
            ShoppingItemRequest req = new ShoppingItemRequest(
                    name,
                    1,
                    "ic_launcher_foreground",
                    userId,
                    0,
                    dateKey,
                    "개",
                    null
            );

            shoppingApi.addItem(req)
                    .enqueue(new Callback<com.namgyun.tamakitchen.ui.shopping.ShoppingItem>() {
                        @Override
                        public void onResponse(
                                Call<com.namgyun.tamakitchen.ui.shopping.ShoppingItem> call,
                                Response<com.namgyun.tamakitchen.ui.shopping.ShoppingItem> response
                        ) {
                            finished[0]++;

                            if (response.isSuccessful()) {
                                success[0]++;
                            }

                            showShoppingResultIfFinished(total, finished[0], success[0], hasNetworkError[0], networkMessage[0]);
                        }

                        @Override
                        public void onFailure(
                                Call<com.namgyun.tamakitchen.ui.shopping.ShoppingItem> call,
                                Throwable t
                        ) {
                            finished[0]++;
                            hasNetworkError[0] = true;
                            networkMessage[0] = NetworkErrorUtil.getUserMessage(t);

                            showShoppingResultIfFinished(total, finished[0], success[0], hasNetworkError[0], networkMessage[0]);
                        }
                    });
        }
    }

    private void showShoppingResultIfFinished(
            int total,
            int finished,
            int success,
            boolean hasNetworkError,
            @Nullable String networkMessage
    ) {
        if (finished != total) return;

        if (success == total) {
            AppToast.show(
                    MealPlanRecipeCompareActivity.this,
                    "장보기에 " + success + "개 담았어요."
            );
            return;
        }

        if (success > 0) {
            AppToast.show(
                    MealPlanRecipeCompareActivity.this,
                    "장보기에 " + success + "개 담았어요. 일부 재료는 다시 시도해주세요."
            );
            return;
        }

        if (hasNetworkError) {
            AppToast.show(
                    MealPlanRecipeCompareActivity.this,
                    TextUtils.isEmpty(networkMessage)
                            ? NetworkErrorUtil.getNetworkMessage()
                            : networkMessage
            );
            return;
        }

        AppToast.show(
                MealPlanRecipeCompareActivity.this,
                "장보기에 담지 못했어요. 잠시 후 다시 시도해주세요."
        );
    }

    private List<String> splitCsv(String csv) {
        List<String> out = new ArrayList<>();

        if (TextUtils.isEmpty(csv)) {
            return out;
        }

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
        if (s == null) {
            return "";
        }

        return s.replace(" ", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}