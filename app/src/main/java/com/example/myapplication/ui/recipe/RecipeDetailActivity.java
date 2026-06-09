package com.namgyun.tamakitchen.ui.recipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.network.RecipeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.network.ShoppingItemRequest;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.fridge.FridgeItem;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.namgyun.tamakitchen.ui.fridge.GuestFridgeStore;
import com.namgyun.tamakitchen.ui.mealplan.MealPlanPrefs;
import com.namgyun.tamakitchen.ui.recipe.adapter.DetailIngredientLineAdapter;
import com.namgyun.tamakitchen.ui.recipe.adapter.RecipeAdapter;
import com.namgyun.tamakitchen.ui.shopping.IconCatalog;
import com.namgyun.tamakitchen.ui.shopping.IconItem;
import com.namgyun.tamakitchen.ui.shopping.StorePickerDialog;
import com.namgyun.tamakitchen.ui.shopping.StoreSessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE = "extra_recipe";
    public static final String EXTRA_USED_INGREDIENTS = "extra_used_ingredients";
    public static final String EXTRA_MISSING_INGREDIENTS = "extra_missing_ingredients";
    public static final String EXTRA_MATCH_PERCENT = "extra_match_percent";
    public static final String EXTRA_LIST_TYPE = "extra_list_type";

    private ImageView ivDetailImage;
    private TextView tvDetailName;
    private TextView tvDetailSummary;
    private TextView tvDetailUsedIngredients;
    private TextView tvDetailMissingIngredients;

    // ✅ 추가: 작성자 표시 (XML 없어도 동적 lookup이라 컴파일 안전)
    private TextView tvDetailAuthor;

    private RecyclerView rvDetailIngredients;
    private DetailIngredientLineAdapter detailIngredientLineAdapter;

    private TextView tvDetailOptionalIngredients;
    private TextView tvDetailSubstitutions;

    private LinearLayout layoutDetailSteps;

    private RatingBar ratingBarAverage;
    private TextView tvAverageRating;

    private RatingBar ratingBarMy;
    private Button btnSubmitRating;

    private Button btnEditRecipe;
    // ✅ 추가: 삭제 버튼 (XML 없어도 동적 lookup)
    private Button btnDeleteRecipe;

    private Button btnAddMissingToCart;
    private Button btnAddToMealPlan;
    private Button btnFinishCooking;

    private RecipeResponse recipe;

    private RecipeApiService api;
    private com.namgyun.tamakitchen.network.ShoppingApi shoppingApi;
    private FridgeApiService fridgeApi;

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_GUEST = "isGuest";

    private static final String PREFS_RATING = "rating_prefs";
    private static final String KEY_RATED_PREFIX = "rated_";
    private static final String KEY_MY_RATING_PREFIX = "my_rating_";
    private static final String KEY_GUEST_RATED_PREFIX = "guest_rated_";
    private static final String KEY_GUEST_MY_RATING_PREFIX = "guest_my_rating_";
    private static final Map<Long, List<RecipeStepResponse>> STEP_MEMORY_CACHE = new HashMap<>();
    private TextView tvToggleIngredients;

    private static final int INGREDIENT_PREVIEW_LIMIT = 5;
    private boolean isIngredientsExpanded = false;
    private List<String> allDetailIngredientLines = new ArrayList<>();

    private ValueAnimator ingredientAnim;

    private static final String TAG = "RecipeDetailActivity";

    private RequestOptions fastImageOptions;
    private RequestOptions stepImageOptions;
    private RequestOptions mainImageOptions;

    private final FridgeModeManager mode = new FridgeModeManager();
    private Button btnMeasurementConverter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailSummary = findViewById(R.id.tvDetailSummary);
        tvDetailUsedIngredients = findViewById(R.id.tvDetailUsedIngredients);
        tvDetailMissingIngredients = findViewById(R.id.tvDetailMissingIngredients);

        // ✅ XML 없어도 안전하게 찾기
        tvDetailAuthor = findOptionalTextView("tvDetailAuthor");

        rvDetailIngredients = findViewById(R.id.rvDetailIngredients);

        tvDetailOptionalIngredients = findViewById(R.id.tvDetailOptionalIngredients);
        tvDetailSubstitutions = findViewById(R.id.tvDetailSubstitutions);
        layoutDetailSteps = findViewById(R.id.layoutDetailSteps);

        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        tvAverageRating = findViewById(R.id.tvAverageRating);

        ratingBarMy = findViewById(R.id.ratingBarMy);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);

        btnEditRecipe = findViewById(R.id.btnEditRecipe);
        btnDeleteRecipe = findOptionalButton("btnDeleteRecipe");

        btnAddMissingToCart = findViewById(R.id.btnAddMissingToCart);
        btnAddToMealPlan = findViewById(R.id.btnAddToMealPlan);
        btnFinishCooking = findViewById(R.id.btnFinishCooking);

        tvToggleIngredients = findViewById(R.id.tvToggleIngredients);

        detailIngredientLineAdapter = new DetailIngredientLineAdapter();
        rvDetailIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvDetailIngredients.setAdapter(detailIngredientLineAdapter);
        rvDetailIngredients.setNestedScrollingEnabled(false);
        rvDetailIngredients.setHasFixedSize(false);
        rvDetailIngredients.setItemViewCacheSize(8);
        btnMeasurementConverter = findViewById(R.id.btnMeasurementConverter);
        fastImageOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .format(DecodeFormat.PREFER_RGB_565);

        mainImageOptions = fastImageOptions.clone().priority(Priority.HIGH);
        stepImageOptions = fastImageOptions.clone().priority(Priority.LOW);

        Retrofit retrofit = FridgeApi.getClient();
        api = retrofit.create(RecipeApiService.class);
        fridgeApi = retrofit.create(FridgeApiService.class);
        shoppingApi = RetrofitClient.getShoppingApi();

        if (tvToggleIngredients != null) {
            tvToggleIngredients.setOnClickListener(v -> toggleIngredientsWithAnim());
        }

        recipe = (RecipeResponse) getIntent().getSerializableExtra(EXTRA_RECIPE);

        if (recipe == null) {
            AppToast.show(this, "레시피 정보를 불러올 수 없습니다");
            finish();
            return;
        }
        increaseViewCountOnce(recipe.getId());
        Log.d("RECIPE_DETAIL_ID", "id=" + recipe.getId()
                + ", name=" + recipe.getName()
                + ", ingredients=" + recipe.getIngredients());
        loadRecipeDetail(recipe.getId());

        ArrayList<String> usedExtra = getIntent().getStringArrayListExtra(EXTRA_USED_INGREDIENTS);
        ArrayList<String> missingExtra = getIntent().getStringArrayListExtra(EXTRA_MISSING_INGREDIENTS);
        int matchExtra = getIntent().getIntExtra(EXTRA_MATCH_PERCENT, recipe.getMatchPercent());

        if (usedExtra != null) recipe.setUsedIngredients(usedExtra);
        if (missingExtra != null) recipe.setMissingIngredients(missingExtra);
        recipe.setMatchPercent(matchExtra);

        String listTypeName = getIntent().getStringExtra(EXTRA_LIST_TYPE);
        RecipeAdapter.ListType listType = parseListType(listTypeName);
        applyAllModeFallbackIfNeeded(recipe, listType);

        bindBasicInfo(recipe);
        bindAuthorInfo(recipe);
        bindRatingInfo(recipe);
        bindStepsSmart(recipe);
        bindMyRatingFromPrefs();
        updateAddMissingButtonState();
        updateAuthorActionButtons();

        if (btnEditRecipe != null) {
            btnEditRecipe.setOnClickListener(v -> {
                if (!isMyRecipe()) {
                    AppToast.show(this, "본인이 작성한 레시피만 수정할 수 있습니다");
                    return;
                }

                // ✅ 수정 전 step 캐시 제거
                if (recipe != null && recipe.getId() != null) {
                    STEP_MEMORY_CACHE.remove(recipe.getId());
                }

                Intent intent = new Intent(RecipeDetailActivity.this, RecipeWriteActivity.class);
                intent.putExtra(RecipeWriteActivity.EXTRA_RECIPE, recipe);
                startActivity(intent);
            });
        }

        if (btnDeleteRecipe != null) {
            btnDeleteRecipe.setOnClickListener(v -> onClickDeleteRecipe());
        }

        if (btnSubmitRating != null) {
            btnSubmitRating.setOnClickListener(v -> onClickSubmitRating());
        }

        if (btnAddMissingToCart != null) {
            btnAddMissingToCart.setOnClickListener(v -> showSelectMissingDialog());
        }

        if (btnAddToMealPlan != null) {
            btnAddToMealPlan.setOnClickListener(v -> showMealPlanDayDialog());
        }

        if (btnMeasurementConverter != null) {
            btnMeasurementConverter.setOnClickListener(
                    v -> showMeasurementConverterDialog());
        }

        if (btnFinishCooking != null) {
            btnFinishCooking.setOnClickListener(v -> onClickFinishCooking());
        }
    }
    private void loadRecipeDetail(Long recipeId) {
        if (recipeId == null) return;

        final ArrayList<String> oldUsed = recipe != null && recipe.getUsedIngredients() != null
                ? new ArrayList<>(recipe.getUsedIngredients())
                : new ArrayList<>();

        final ArrayList<String> oldMissing = recipe != null && recipe.getMissingIngredients() != null
                ? new ArrayList<>(recipe.getMissingIngredients())
                : new ArrayList<>();

        final int oldMatchPercent = recipe != null ? recipe.getMatchPercent() : 0;

        api.getRecipeById(recipeId).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipeResponse> call,
                                   @NonNull Response<RecipeResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                RecipeResponse fresh = response.body();

                if (!oldUsed.isEmpty()) {
                    fresh.setUsedIngredients(oldUsed);
                }

                if (!oldMissing.isEmpty()) {
                    fresh.setMissingIngredients(oldMissing);
                }

                if (fresh.getMatchPercent() <= 0 && oldMatchPercent > 0) {
                    fresh.setMatchPercent(oldMatchPercent);
                }

                recipe = fresh;

                bindBasicInfo(recipe);
                bindAuthorInfo(recipe);
                bindRatingInfo(recipe);
                bindStepsSmart(recipe);
                bindMyRatingFromPrefs();
                updateAddMissingButtonState();
                updateAuthorActionButtons();
            }

            @Override
            public void onFailure(@NonNull Call<RecipeResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "loadRecipeDetail failed", t);
            }
        });
    }
    private void increaseViewCountOnce(@Nullable Long recipeId) {
        if (recipeId == null || recipeId <= 0) return;
        if (api == null) return;

        api.increaseViewCount(recipeId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "increaseViewCount response=" + response.code() + ", recipeId=" + recipeId);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "increaseViewCount failure. recipeId=" + recipeId + ", error=" + t.getMessage(), t);
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ingredientAnim != null) {
            ingredientAnim.cancel();
            ingredientAnim = null;
        }
        try {
            if (ivDetailImage != null) Glide.with(this).clear(ivDetailImage);
        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // ✅ 작성자/권한 관련
    // =========================================================
    private TextView findOptionalTextView(@NonNull String name) {
        try {
            int id = getResources().getIdentifier(name, "id", getPackageName());
            if (id == 0) return null;
            View v = findViewById(id);
            return (v instanceof TextView) ? (TextView) v : null;
        } catch (Exception e) {
            return null;
        }
    }
    private void showMeasurementConverterDialog() {

        String message =
                "물 / 액체 기준\n\n" +
                        "종이컵 1컵 = 약 180ml\n" +
                        "머그컵 1컵 = 약 250ml\n" +
                        "소주잔 1잔 = 약 50ml\n" +
                        "밥숟가락 1스푼 = 약 15ml\n" +
                        "티스푼 1스푼 = 약 5ml\n\n" +
                        "예시\n\n" +
                        "500ml → 종이컵 약 2.8컵\n" +
                        "500ml → 머그컵 약 2컵\n" +
                        "500ml → 소주잔 약 10잔\n" +
                        "1L → 종이컵 약 5.6컵\n" +
                        "350ml → 종이컵 약 2컵\n" +
                        "15ml → 밥숟가락 약 1스푼\n\n" +
                        "※ g 단위는 재료마다 무게가 달라 정확한 변환이 어려울 수 있습니다.";

        new AlertDialog.Builder(this)
                .setTitle("생활 계량 변환")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }
    private Button findOptionalButton(@NonNull String name) {
        try {
            int id = getResources().getIdentifier(name, "id", getPackageName());
            if (id == 0) return null;
            View v = findViewById(id);
            return (v instanceof Button) ? (Button) v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void bindAuthorInfo(@Nullable RecipeResponse r) {
        if (tvDetailAuthor == null) return;

        String nick = (r == null) ? "" : safe(r.getAuthorNickname()).trim();
        if (nick.isEmpty()) nick = "알 수 없음";

        tvDetailAuthor.setText("작성자: " + nick);
        tvDetailAuthor.setVisibility(View.VISIBLE);
    }

    private boolean isMyRecipe() {
        if (recipe == null) return false;
        Long authorId = recipe.getAuthorId();
        Long loginUserId = getUserIdFromPrefs();

        if (authorId == null || loginUserId == null) return false;
        return authorId.equals(loginUserId);
    }

    private void updateAuthorActionButtons() {
        boolean mine = isMyRecipe();

        if (btnEditRecipe != null) {
            btnEditRecipe.setVisibility(mine ? View.VISIBLE : View.GONE);
        }

        if (btnDeleteRecipe != null) {
            btnDeleteRecipe.setVisibility(mine ? View.VISIBLE : View.GONE);
        }
    }

    private void onClickDeleteRecipe() {
        if (recipe == null || recipe.getId() == null) {
            AppToast.show(this, "삭제할 레시피 정보가 없습니다");
            return;
        }

        Long loginUserId = getUserIdFromPrefs();
        if (loginUserId == null || loginUserId <= 0) {
            AppToast.show(this, "로그인이 필요합니다");
            return;
        }

        if (!isMyRecipe()) {
            AppToast.show(this, "본인이 작성한 레시피만 삭제할 수 있습니다");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("레시피 삭제")
                .setMessage("'" + safe(recipe.getName()) + "' 레시피를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    api.deleteRecipe(recipe.getId(), loginUserId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (isFinishing() || isDestroyed()) return;

                            if (response.isSuccessful()) {
                                AppToast.show(RecipeDetailActivity.this, "삭제되었습니다");
                                finish();
                            } else {
                                AppToast.show(RecipeDetailActivity.this, "삭제 실패: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            if (isFinishing() || isDestroyed()) return;
                            Log.e(TAG, "deleteRecipe failure", t);
                            AppToast.show(RecipeDetailActivity.this, "네트워크 오류가 발생했습니다");
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private RequestListener<Drawable> buildGlideLogListener(final String label) {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource) {
                Log.e("GLIDE_IMG", "FAIL [" + label + "] url=" + model, e);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource,
                                           Object model,
                                           Target<Drawable> target,
                                           DataSource dataSource,
                                           boolean isFirstResource) {
                Log.d("GLIDE_IMG", "SUCCESS [" + label + "] url=" + model + " source=" + dataSource);
                return false;
            }
        };
    }

    private String deriveThumbnailUrlFromMain(String rawMainUrl) {
        String main = resolveAnyImageUrl(rawMainUrl);
        if (TextUtils.isEmpty(main)) return null;

        // ✅ Cloudinary 썸네일 자동 변환
        if (main.startsWith("https://res.cloudinary.com/")) {
            if (main.contains("/image/upload/c_")) {
                return main;
            }

            return main.replace(
                    "/image/upload/",
                    "/image/upload/c_fill,w_480,h_320,q_auto,f_auto/"
            );
        }

        // ✅ 기존 로컬 webp 썸네일
        if (main.contains("_thumb.webp")) return main;

        if (main.endsWith(".webp")) {
            return main.substring(0, main.length() - ".webp".length()) + "_thumb.webp";
        }

        return null;
    }
    private void bindStepsSmart(@NonNull RecipeResponse r) {
        if (layoutDetailSteps != null) {
            layoutDetailSteps.removeAllViews();
        }

        if (r.getId() != null) {
            loadStepsFromServerThenRender(r.getId());
            return;
        }

        bindStepsFromText(r);
    }
    private int getRecipePayloadStepCount(@Nullable RecipeResponse r) {
        if (r == null) return 0;

        try {
            Method m = r.getClass().getMethod("getStepItems");
            Object obj = m.invoke(r);

            if (obj instanceof List<?>) {
                return ((List<?>) obj).size();
            }
        } catch (Exception ignored) {
        }

        return 0;
    }

    private boolean renderStepsFromRecipePayloadIfPossible(@Nullable RecipeResponse r) {
        if (r == null) return false;

        List<RecipeStepResponse> converted = convertRecipePayloadStepItems(r);
        if (converted == null || converted.isEmpty()) {
            return false;
        }

        renderStepsFromList(converted);
        return true;
    }

    private List<RecipeStepResponse> convertRecipePayloadStepItems(@NonNull RecipeResponse r) {
        List<RecipeStepResponse> out = new ArrayList<>();

        try {
            Method m = r.getClass().getMethod("getStepItems");
            Object obj = m.invoke(r);

            if (!(obj instanceof List<?>)) {
                return out;
            }

            List<?> rawList = (List<?>) obj;
            for (Object raw : rawList) {
                if (raw == null) continue;

                if (raw instanceof RecipeStepResponse) {
                    RecipeStepResponse step = (RecipeStepResponse) raw;
                    if (!TextUtils.isEmpty(step.getDescription()) || !TextUtils.isEmpty(step.getImageUrl())) {
                        out.add(step);
                    }
                    continue;
                }

                RecipeStepResponse converted = new RecipeStepResponse();

                Integer stepNo = readIntegerGetter(raw, "getStepNumber");
                String desc = firstNonEmpty(
                        readStringGetter(raw, "getDescription"),
                        readStringGetter(raw, "getText"),
                        readStringGetter(raw, "getContent")
                );
                String imageUrl = readStringGetter(raw, "getImageUrl");

                try {
                    converted.setStepNumber(stepNo);
                } catch (Exception ignored) {
                }

                try {
                    converted.setDescription(desc);
                } catch (Exception ignored) {
                }

                try {
                    converted.setImageUrl(imageUrl);
                } catch (Exception ignored) {
                }

                if (stepNo != null || !TextUtils.isEmpty(desc) || !TextUtils.isEmpty(imageUrl)) {
                    out.add(converted);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "convertRecipePayloadStepItems error", e);
        }

        return out;
    }

    @Nullable
    private Integer readIntegerGetter(@NonNull Object target, @NonNull String getterName) {
        try {
            Method m = target.getClass().getMethod(getterName);
            Object value = m.invoke(target);

            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Number) return ((Number) value).intValue();

            if (value != null) {
                String s = String.valueOf(value).trim();
                if (!s.isEmpty()) return Integer.parseInt(s);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @Nullable
    private String readStringGetter(@NonNull Object target, @NonNull String getterName) {
        try {
            Method m = target.getClass().getMethod(getterName);
            Object value = m.invoke(target);
            if (value == null) return null;

            String s = String.valueOf(value).trim();
            return s.isEmpty() ? null : s;

        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private String firstNonEmpty(@Nullable String... values) {
        if (values == null) return null;

        for (String v : values) {
            if (!TextUtils.isEmpty(v)) return v;
        }

        return null;
    }

    private void loadStepsFromServerThenRender(@Nullable Long recipeId) {
        if (layoutDetailSteps != null) layoutDetailSteps.removeAllViews();

        if (recipeId == null || recipeId <= 0) {
            bindStepsFromText(recipe);
            return;
        }

        List<RecipeStepResponse> cached = STEP_MEMORY_CACHE.get(recipeId);
        if (cached != null && !cached.isEmpty()) {
            Log.d(TAG, "steps loaded from memory cache. recipeId=" + recipeId);
            renderStepsFromList(new ArrayList<>(cached));
            return;
        }

        Log.d(TAG, "request server steps. recipeId=" + recipeId);

        api.getRecipeSteps(recipeId).enqueue(new Callback<List<RecipeStepResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecipeStepResponse>> call,
                                   @NonNull Response<List<RecipeStepResponse>> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<RecipeStepResponse> steps = new ArrayList<>(response.body());
                    STEP_MEMORY_CACHE.put(recipeId, steps);
                    renderStepsFromList(steps);
                } else {
                    Log.e(TAG, "getRecipeSteps empty or not ok. code=" + response.code());
                    bindStepsFromText(recipe);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RecipeStepResponse>> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "getRecipeSteps failure: " + t.getMessage(), t);
                bindStepsFromText(recipe);
            }
        });
    }

    private void renderStepsFromList(@NonNull List<RecipeStepResponse> steps) {
        if (layoutDetailSteps == null) return;
        layoutDetailSteps.removeAllViews();

        steps.sort((a, b) -> {
            int sa = (a == null || a.getStepNumber() == null) ? 0 : a.getStepNumber();
            int sb = (b == null || b.getStepNumber() == null) ? 0 : b.getStepNumber();
            return sa - sb;
        });

        for (RecipeStepResponse s : steps) {
            if (s == null) continue;

            String stepNo = String.valueOf(s.getStepNumber() == null ? "" : s.getStepNumber());
            String desc = (s.getDescription() == null) ? "" : s.getDescription().trim();
            String imageUrl = (s.getImageUrl() == null) ? "" : s.getImageUrl().trim();

            addStepViewFromApi(stepNo, desc, imageUrl);
        }
    }

    private void addStepViewFromApi(String stepNo, String desc, String imageUrlRaw) {
        if (layoutDetailSteps == null) return;

        View v = LayoutInflater.from(this).inflate(R.layout.item_recipe_step, layoutDetailSteps, false);

        TextView tvStepTitle = v.findViewById(R.id.tvStepTitle);
        TextView tvStepText = v.findViewById(R.id.tvStepText);
        ImageView ivStepImage = v.findViewById(R.id.ivStepImage);

        tvStepTitle.setText("[" + stepNo + "단계]");

        if (!TextUtils.isEmpty(desc)) {
            tvStepText.setVisibility(View.VISIBLE);
            tvStepText.setText(desc);
        } else {
            tvStepText.setVisibility(View.GONE);
        }

        String finalUrl = resolveAnyImageUrl(imageUrlRaw);

        Log.d("STEP_IMG_URL", "step=" + stepNo
                + ", raw=" + imageUrlRaw
                + ", final=" + finalUrl);

        String thumbFromRecipe = (recipe != null) ? resolveAnyImageUrl(recipe.getThumbnailUrl()) : null;
        String thumbUrl = !TextUtils.isEmpty(thumbFromRecipe) ? thumbFromRecipe : deriveThumbnailUrlFromMain(finalUrl);

        if (!TextUtils.isEmpty(finalUrl)) {
            ivStepImage.setVisibility(View.VISIBLE);

            int width = getResources().getDisplayMetrics().widthPixels;
            int height = (int) (200 * getResources().getDisplayMetrics().density);

            Object tag = ivStepImage.getTag();
            if (tag != null && String.valueOf(tag).equals(finalUrl)) {
                // no-op
            } else {
                ivStepImage.setTag(finalUrl);

                if (!TextUtils.isEmpty(thumbUrl) && !finalUrl.equals(thumbUrl)) {
                    Glide.with(this)
                            .load(finalUrl)
                            .apply(stepImageOptions)
                            .override(width, height)
                            .thumbnail(
                                    Glide.with(this)
                                            .load(thumbUrl)
                                            .apply(stepImageOptions)
                                            .override(width, height)
                                            .fitCenter()
                                            .dontAnimate()
                            )
                            .listener(buildGlideLogListener("step#" + stepNo))
                            .placeholder(R.drawable.bg_recipe_placeholder)
                            .error(R.drawable.bg_recipe_placeholder)
                            .fitCenter()
                            .dontAnimate()
                            .into(ivStepImage);
                } else {
                    Glide.with(this)
                            .load(finalUrl)
                            .apply(stepImageOptions)
                            .override(width, height)
                            .thumbnail(0.15f)
                            .listener(buildGlideLogListener("step#" + stepNo))
                            .placeholder(R.drawable.bg_recipe_placeholder)
                            .error(R.drawable.bg_recipe_placeholder)
                            .fitCenter()
                            .dontAnimate()
                            .into(ivStepImage);
                }
            }
        } else {
            ivStepImage.setVisibility(View.GONE);
        }

        layoutDetailSteps.addView(v);
    }

    private String resolveAnyImageUrl(String raw) {
        if (raw == null) return null;
        String url = raw.trim();
        if (url.isEmpty()) return null;

        if (url.startsWith("content://")) {
            if (url.startsWith("content://media/picker_get_content")) return null;
            return url;
        }

        if (url.startsWith("/uploads")) {
            return joinBaseUrl(FridgeApi.BASE_URL, url);
        }
        if (url.startsWith("uploads/")) {
            return joinBaseUrl(FridgeApi.BASE_URL, "/" + url);
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (url.contains("/uploads/")) {
                return replaceHostWithBase(url, FridgeApi.BASE_URL);
            }
        }

        return url;
    }

    private String joinBaseUrl(String base, String pathStartingWithSlash) {
        if (TextUtils.isEmpty(base)) return pathStartingWithSlash;
        String b = base.trim();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + pathStartingWithSlash;
    }

    private String replaceHostWithBase(String originalUrl, String baseUrl) {
        try {
            Uri o = Uri.parse(originalUrl);
            Uri b = Uri.parse(baseUrl);

            String scheme = (b.getScheme() != null) ? b.getScheme() : o.getScheme();
            String host = b.getHost();
            int port = (b.getPort() != -1) ? b.getPort() : o.getPort();

            if (host == null) return originalUrl;

            Uri.Builder nb = new Uri.Builder()
                    .scheme(scheme)
                    .encodedAuthority(port == -1 ? host : (host + ":" + port))
                    .encodedPath(o.getEncodedPath());

            if (o.getEncodedQuery() != null) nb.encodedQuery(o.getEncodedQuery());
            return nb.build().toString();
        } catch (Exception e) {
            return originalUrl;
        }
    }

    private void bindStepsFromText(RecipeResponse r) {
        if (layoutDetailSteps != null) layoutDetailSteps.removeAllViews();
        if (r == null) return;

        String stepsText = r.getSteps();
        if (TextUtils.isEmpty(stepsText)) return;

        Pattern p = Pattern.compile("\\[(\\d+)단계\\]");
        Matcher m = p.matcher(stepsText);

        int lastStart = -1;
        String lastStepNo = null;

        while (m.find()) {
            if (lastStart != -1 && lastStepNo != null) {
                String block = stepsText.substring(lastStart, m.start());
                addStepViewFromText(lastStepNo, block);
            }
            lastStepNo = m.group(1);
            lastStart = m.end();
        }

        if (lastStart != -1 && lastStepNo != null) {
            String block = stepsText.substring(lastStart);
            addStepViewFromText(lastStepNo, block);
        }
    }

    private void addStepViewFromText(String stepNo, String rawBlock) {
        if (layoutDetailSteps == null) return;
        if (rawBlock == null) return;
        String block = rawBlock.trim();
        if (block.isEmpty()) return;

        String imageUri = null;
        StringBuilder textOnly = new StringBuilder();

        String[] lines = block.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) continue;
            String t = line.trim();
            if (t.isEmpty()) continue;

            if (t.startsWith("이미지URI:")) {
                imageUri = t.replace("이미지URI:", "").trim();
                continue;
            }

            if (textOnly.length() > 0) textOnly.append("\n");
            textOnly.append(t);
        }

        String title = "";
        String desc = "";
        String allText = textOnly.toString().trim();

        if (!TextUtils.isEmpty(allText)) {
            String[] tLines = allText.split("\\r?\\n", 2);
            title = tLines[0].trim();
            if (tLines.length > 1) desc = tLines[1].trim();
        }

        View v = LayoutInflater.from(this).inflate(R.layout.item_recipe_step, layoutDetailSteps, false);

        TextView tvStepTitle = v.findViewById(R.id.tvStepTitle);
        TextView tvStepText = v.findViewById(R.id.tvStepText);
        ImageView ivStepImage = v.findViewById(R.id.ivStepImage);

        tvStepTitle.setText("[" + stepNo + "단계] " + (TextUtils.isEmpty(title) ? "" : title));

        if (!TextUtils.isEmpty(desc)) {
            tvStepText.setVisibility(View.VISIBLE);
            tvStepText.setText(desc);
        } else {
            tvStepText.setVisibility(View.GONE);
        }

        String finalUrl = resolveAnyImageUrl(imageUri);

        String thumbFromRecipe = (recipe != null) ? resolveAnyImageUrl(recipe.getThumbnailUrl()) : null;
        String thumbUrl = !TextUtils.isEmpty(thumbFromRecipe) ? thumbFromRecipe : deriveThumbnailUrlFromMain(finalUrl);

        if (!TextUtils.isEmpty(finalUrl)) {
            ivStepImage.setVisibility(View.VISIBLE);

            int width = getResources().getDisplayMetrics().widthPixels;
            int height = (int) (200 * getResources().getDisplayMetrics().density);

            if (!TextUtils.isEmpty(thumbUrl) && !finalUrl.equals(thumbUrl)) {
                Glide.with(this)
                        .load(finalUrl)
                        .apply(stepImageOptions)
                        .override(width, height)
                        .thumbnail(
                                Glide.with(this)
                                        .load(thumbUrl)
                                        .apply(stepImageOptions)
                                        .override(width, height)
                                        .fitCenter()
                                        .dontAnimate()
                        )
                        .listener(buildGlideLogListener("textStep#" + stepNo))
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .fitCenter()
                        .dontAnimate()
                        .into(ivStepImage);
            } else {
                Glide.with(this)
                        .load(finalUrl)
                        .apply(stepImageOptions)
                        .override(width, height)
                        .thumbnail(0.15f)
                        .listener(buildGlideLogListener("textStep#" + stepNo))
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .fitCenter()
                        .dontAnimate()
                        .into(ivStepImage);
            }
        } else {
            ivStepImage.setVisibility(View.GONE);
        }

        layoutDetailSteps.addView(v);
    }

    private void toggleIngredientsWithAnim() {
        int startH = rvDetailIngredients.getHeight();
        if (startH <= 0) startH = measureRecyclerTargetHeight();

        isIngredientsExpanded = !isIngredientsExpanded;
        applyDetailIngredientPreview(false);

        int endH = measureRecyclerTargetHeight();
        animateRecyclerHeight(startH, endH, 220);
    }

    private int measureRecyclerTargetHeight() {
        rvDetailIngredients.measure(
                View.MeasureSpec.makeMeasureSpec(rvDetailIngredients.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        return rvDetailIngredients.getMeasuredHeight();
    }

    private void animateRecyclerHeight(int from, int to, long durationMs) {
        if (from == to) return;

        if (ingredientAnim != null) ingredientAnim.cancel();

        ingredientAnim = ValueAnimator.ofInt(from, to);
        ingredientAnim.setDuration(durationMs);
        ingredientAnim.setInterpolator(new DecelerateInterpolator());
        ingredientAnim.addUpdateListener(anim -> {
            int h = (int) anim.getAnimatedValue();
            ViewGroup.LayoutParams lp = rvDetailIngredients.getLayoutParams();
            lp.height = h;
            rvDetailIngredients.setLayoutParams(lp);
        });
        ingredientAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetRecyclerHeightToWrapContent();
            }
        });
        ingredientAnim.start();
    }

    private void resetRecyclerHeightToWrapContent() {
        ViewGroup.LayoutParams lp = rvDetailIngredients.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        rvDetailIngredients.setLayoutParams(lp);
    }

    private void bindBasicInfo(RecipeResponse r) {
        if (r == null) return;

        if (tvDetailName != null) tvDetailName.setText(safe(r.getName()));
        if (tvDetailSummary != null) tvDetailSummary.setText(safe(r.getSummary()));

        String mainUrl = resolveAnyImageUrl(r.getImageUrl());
        String thumbUrl = resolveAnyImageUrl(r.getThumbnailUrl());
        if (TextUtils.isEmpty(thumbUrl)) {
            thumbUrl = deriveThumbnailUrlFromMain(mainUrl);
        }

        if (!TextUtils.isEmpty(mainUrl)) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = (int) (240 * getResources().getDisplayMetrics().density);

            if (!TextUtils.isEmpty(thumbUrl) && !mainUrl.equals(thumbUrl)) {
                Glide.with(this)
                        .load(mainUrl)
                        .apply(mainImageOptions)
                        .override(width, height)
                        .thumbnail(
                                Glide.with(this)
                                        .load(thumbUrl)
                                        .apply(mainImageOptions)
                                        .override(width, height)
                                        .centerCrop()
                                        .dontAnimate()
                        )
                        .listener(buildGlideLogListener("main"))
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .centerCrop()
                        .dontAnimate()
                        .into(ivDetailImage);
            } else {
                Glide.with(this)
                        .load(mainUrl)
                        .apply(mainImageOptions)
                        .override(width, height)
                        .thumbnail(0.2f)
                        .listener(buildGlideLogListener("main"))
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .centerCrop()
                        .dontAnimate()
                        .into(ivDetailImage);
            }
        } else {
            if (ivDetailImage != null) ivDetailImage.setImageResource(R.drawable.bg_recipe_placeholder);
        }

        if (tvDetailUsedIngredients != null) {
            if (r.getUsedIngredients() != null && !r.getUsedIngredients().isEmpty()) {
                tvDetailUsedIngredients.setText("냉장고에 있는 재료: " + TextUtils.join(", ", toIngredientNamesOnly(r.getUsedIngredients())));
            } else {
                tvDetailUsedIngredients.setText("냉장고에 있는 재료: -");
            }
        }

        if (tvDetailMissingIngredients != null) {
            if (r.getMissingIngredients() != null && !r.getMissingIngredients().isEmpty()) {
                tvDetailMissingIngredients.setText("추가로 필요한 재료: " + TextUtils.join(", ", toIngredientNamesOnly(r.getMissingIngredients())));
            } else {
                List<String> requiredNames = new ArrayList<>();

                try {
                    List<RecipeResponse.RecipeIngredientItem> required = r.getRequiredIngredientsItems();
                    if (required != null) {
                        for (RecipeResponse.RecipeIngredientItem item : required) {
                            if (item == null) continue;
                            String name = safe(item.getName()).trim();
                            if (!name.isEmpty()) requiredNames.add(name);
                        }
                    }
                } catch (Exception ignored) {}

                if (!requiredNames.isEmpty()) {
                    tvDetailMissingIngredients.setText("추가로 필요한 재료: " + TextUtils.join(", ", requiredNames));
                } else {
                    tvDetailMissingIngredients.setText("추가로 필요한 재료: -");
                }
            }
        }

        if (tvDetailOptionalIngredients != null) {

            List<String> optionalNames = new ArrayList<>();

            try {
                List<RecipeResponse.RecipeIngredientItem> optionalItems =
                        r.getOptionalIngredientsItems();

                if (optionalItems != null) {
                    for (RecipeResponse.RecipeIngredientItem item : optionalItems) {

                        if (item == null) continue;

                        String name = extractIngredientNameOnly(
                                safe(item.getName()).trim()
                        );

                        if (!name.isEmpty()) {
                            optionalNames.add(name);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (!optionalNames.isEmpty()) {
                tvDetailOptionalIngredients.setVisibility(View.VISIBLE);
                tvDetailOptionalIngredients.setText(
                        "선택재료: " + TextUtils.join(", ", optionalNames)
                );
            } else {
                tvDetailOptionalIngredients.setVisibility(View.GONE);
            }
        }

        if (tvDetailSubstitutions != null) {
            String subsPretty = null;

            try {
                List<String> lines = r.getSubstituteLines();
                if (lines != null && !lines.isEmpty()) {
                    subsPretty = TextUtils.join("\n", lines);
                }
            } catch (Exception ignored) {
            }

            if (TextUtils.isEmpty(subsPretty)) {
                String subsJson = null;
                try {
                    subsJson = r.getSubstitutesJson();
                } catch (Exception ignored) {
                }
                subsPretty = buildSubstitutesPretty(subsJson);
            }

            if (!TextUtils.isEmpty(subsPretty)) {
                tvDetailSubstitutions.setVisibility(View.VISIBLE);
                tvDetailSubstitutions.setText("대체재료:\n" + subsPretty);
            } else {
                tvDetailSubstitutions.setVisibility(View.GONE);
            }
        }

        allDetailIngredientLines = buildDetailIngredientLinesPreferStructured(r);
        if (allDetailIngredientLines == null) allDetailIngredientLines = new ArrayList<>();
        if (allDetailIngredientLines.isEmpty()) allDetailIngredientLines.add("-");

        isIngredientsExpanded = false;
        applyDetailIngredientPreview(true);
        debugLogIngredientPayload(r);
    }

    private void applyDetailIngredientPreview(boolean initialBind) {
        if (allDetailIngredientLines == null) allDetailIngredientLines = new ArrayList<>();

        boolean isDashOnly = (allDetailIngredientLines.size() == 1
                && "-".equals(safe(allDetailIngredientLines.get(0)).trim()));

        if (isDashOnly) {
            if (tvToggleIngredients != null) tvToggleIngredients.setVisibility(View.GONE);
            detailIngredientLineAdapter.submit(allDetailIngredientLines);
            if (initialBind) resetRecyclerHeightToWrapContent();
            return;
        }

        int total = allDetailIngredientLines.size();

        if (total <= INGREDIENT_PREVIEW_LIMIT) {
            if (tvToggleIngredients != null) tvToggleIngredients.setVisibility(View.GONE);
            detailIngredientLineAdapter.submit(allDetailIngredientLines);
            if (initialBind) resetRecyclerHeightToWrapContent();
            return;
        }

        if (tvToggleIngredients != null) {
            tvToggleIngredients.setVisibility(View.VISIBLE);
            tvToggleIngredients.setText(isIngredientsExpanded ? "접기 ▴" : "더보기 ▾");
        }

        List<String> show;
        if (isIngredientsExpanded) {
            show = allDetailIngredientLines;
        } else {
            show = new ArrayList<>(allDetailIngredientLines.subList(0, INGREDIENT_PREVIEW_LIMIT));
        }

        detailIngredientLineAdapter.submit(show);
        if (initialBind) resetRecyclerHeightToWrapContent();
    }

    private List<String> buildDetailIngredientLinesPreferStructured(RecipeResponse r) {
        if (r == null) return new ArrayList<>();

        List<RecipeResponse.RecipeIngredientItem> all = null;
        try {
            all = r.getIngredientItems();
        } catch (Exception ignored) {
        }

        LinkedHashSet<String> required = new LinkedHashSet<>();
        LinkedHashSet<String> optional = new LinkedHashSet<>();

        if (all != null && !all.isEmpty()) {
            for (RecipeResponse.RecipeIngredientItem it : all) {
                if (it == null) continue;

                String type = safe(it.getType()).trim().toUpperCase(Locale.ROOT);
                if ("SUBSTITUTE".equals(type)) continue;

                String line = buildLineWithOptionalMark(it);
                if (TextUtils.isEmpty(line)) continue;

                if ("OPTIONAL".equals(type)) optional.add(line);
                else required.add(line);
            }

            ArrayList<String> out = new ArrayList<>();
            out.addAll(required);
            out.addAll(optional);
            return out;
        }

        LinkedHashSet<String> acc = new LinkedHashSet<>();
        addCsvInto(acc, r.getIngredients());
        addCsvInto(acc, r.getOptionalIngredientsText());
        return new ArrayList<>(acc);
    }

    private String buildLineWithOptionalMark(RecipeResponse.RecipeIngredientItem it) {
        if (it == null) return null;

        String name = safe(it.getName()).trim();
        if (name.isEmpty()) return null;

        String qty = null;
        try {
            qty = it.getPrettyQuantity();
        } catch (Exception ignored) {
        }
        qty = safe(qty).trim();

        if (qty.isEmpty()) {
            String raw = null;
            try {
                raw = it.getQuantityText();
            } catch (Exception ignored) {
            }
            qty = safe(raw).trim();
        }

        String type = safe(it.getType()).trim().toUpperCase(Locale.ROOT);
        boolean isOptional = "OPTIONAL".equals(type);

        String base = qty.isEmpty() ? name : (name + " " + qty);
        if (isOptional) return base + " (선택)";
        return base;
    }

    private String buildSubstitutesPretty(String json) {
        if (TextUtils.isEmpty(json)) return null;
        String raw = json.trim();
        if (raw.isEmpty()) return null;

        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray names = obj.names();
            if (names == null) return null;

            List<String> lines = new ArrayList<>();

            for (int i = 0; i < names.length(); i++) {
                String key = names.optString(i, "").trim();
                if (TextUtils.isEmpty(key)) continue;

                Object v = obj.opt(key);
                List<String> alts = new ArrayList<>();

                if (v instanceof JSONArray) {
                    JSONArray arr = (JSONArray) v;
                    for (int j = 0; j < arr.length(); j++) {
                        String a = arr.optString(j, "").trim();
                        if (!TextUtils.isEmpty(a)) alts.add(a);
                    }
                } else if (v != null) {
                    String a = String.valueOf(v).trim();
                    if (!TextUtils.isEmpty(a)) alts.add(a);
                }

                if (!alts.isEmpty()) {
                    lines.add(key + " → " + TextUtils.join(", ", alts));
                }
            }

            if (lines.isEmpty()) return null;
            return TextUtils.join("\n", lines);

        } catch (Exception e) {
            return null;
        }
    }

    private void addCsvInto(LinkedHashSet<String> acc, String csv) {
        if (acc == null) return;
        if (TextUtils.isEmpty(csv)) return;

        String normalized = csv
                .replace("\\r\\n", ",")
                .replace("\\n", ",")
                .replace("\r\n", ",")
                .replace("\n", ",")
                .replace("|", ",");

        String[] parts = normalized.split(",");

        for (String p : parts) {
            String t = safe(p).trim();
            if (t.isEmpty()) continue;
            if ("-".equals(t)) continue;
            acc.add(t);
        }
    }

    private void debugLogIngredientPayload(RecipeResponse r) {
        try {
            List<RecipeResponse.RecipeIngredientItem> all = null;
            try {
                all = r.getIngredientItems();
            } catch (Exception ignored) {
            }

            Log.d("DETAIL_PAYLOAD",
                    "ingredientItems(all)=" + (all == null ? "null" : all.size())
                            + ", legacyIngredientsCsv=" + (TextUtils.isEmpty(r.getIngredients()) ? "empty" : "has")
                            + ", legacyOptionalCsv=" + (TextUtils.isEmpty(r.getOptionalIngredientsText()) ? "empty" : "has")
            );
        } catch (Exception e) {
            Log.e("DETAIL_PAYLOAD", "debugLogIngredientPayload error", e);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private RecipeAdapter.ListType parseListType(String name) {
        if (TextUtils.isEmpty(name)) return RecipeAdapter.ListType.FRIDGE;
        try {
            return RecipeAdapter.ListType.valueOf(name);
        } catch (Exception e) {
            return RecipeAdapter.ListType.FRIDGE;
        }
    }

    private void applyAllModeFallbackIfNeeded(RecipeResponse r, RecipeAdapter.ListType listType) {
        if (r == null) return;

        boolean hasUsed = r.getUsedIngredients() != null && !r.getUsedIngredients().isEmpty();
        boolean hasMissing = r.getMissingIngredients() != null && !r.getMissingIngredients().isEmpty();

        if (listType == RecipeAdapter.ListType.ALL && !hasUsed && !hasMissing) {
            String ing = r.getIngredients();
            if (!TextUtils.isEmpty(ing)) r.setMissingIngredients(new ArrayList<>(splitIngredientsToList(ing)));
        }
    }

    private List<String> splitIngredientsToList(String ing) {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(ing)) return out;

        String normalized = ing
                .replace("\\r\\n", ",")
                .replace("\\n", ",")
                .replace("\r\n", ",")
                .replace("\n", ",")
                .replace("|", ",");

        String[] parts = normalized.split(",");

        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
    private List<String> toIngredientNamesOnly(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;

        for (String s : list) {
            String name = extractIngredientNameOnly(s);
            if (!TextUtils.isEmpty(name) && !out.contains(name)) {
                out.add(name);
            }
        }

        return out;
    }

    private String extractIngredientNameOnly(String raw) {
        if (raw == null) return "";

        String s = raw.trim();

        s = s.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("|", "\n");

        if (s.contains("\n")) {
            String first = s.split("\\n")[0].trim();
            if (!first.isEmpty()) s = first;
        }

        s = s.replaceAll("\\s+\\d+/\\d+(개|장|컵|스푼|큰술|작은술|줄|꼬집|공기|알|봉|팩|g|ml)$", "");
        s = s.replaceAll("\\s+\\d+(\\.\\d+)?(개|장|컵|스푼|큰술|작은술|줄|꼬집|공기|알|봉|팩|g|ml)$", "");
        s = s.replaceAll("\\s+(한줌|약간|조금|적당히)$", "");

        return s.trim();
    }
    private void bindRatingInfo(RecipeResponse r) {
        if (r == null) return;
        if (ratingBarAverage == null || tvAverageRating == null) return;

        if (isGuestMode()) {
            ratingBarAverage.setVisibility(View.GONE);
            tvAverageRating.setText("게스트 모드에서는 다른 사용자 평점이 보이지 않습니다.");
            return;
        } else {
            ratingBarAverage.setVisibility(View.VISIBLE);
        }

        float avg = r.getRatingAverage();
        int count = r.getRatingCount();

        if (count > 0) {
            ratingBarAverage.setRating(avg);
            tvAverageRating.setText(String.format(Locale.getDefault(), "평점 %.1f점 (%d명)", avg, count));
        } else {
            ratingBarAverage.setRating(0f);
            tvAverageRating.setText("아직 등록된 평점이 없습니다.");
        }
    }

    private Long getUserIdFromPrefs() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            long id = prefs.getLong(KEY_USER_ID, -1L);
            return (id == -1L) ? null : id;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isGuestMode() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            return prefs.getBoolean(KEY_IS_GUEST, false);
        } catch (Exception e) {
            return false;
        }
    }

    private SharedPreferences ratingPrefs() {
        return getSharedPreferences(PREFS_RATING, MODE_PRIVATE);
    }

    private String ratedKeyForCurrentMode(Long recipeId) {
        if (recipeId == null) return "";
        return isGuestMode()
                ? (KEY_GUEST_RATED_PREFIX + recipeId)
                : (KEY_RATED_PREFIX + recipeId);
    }

    private String myRatingKeyForCurrentMode(Long recipeId) {
        if (recipeId == null) return "";
        return isGuestMode()
                ? (KEY_GUEST_MY_RATING_PREFIX + recipeId)
                : (KEY_MY_RATING_PREFIX + recipeId);
    }

    private void bindMyRatingFromPrefs() {
        if (recipe == null || recipe.getId() == null) return;
        if (ratingBarMy == null) return;

        float saved = ratingPrefs().getFloat(myRatingKeyForCurrentMode(recipe.getId()), 0f);
        if (saved > 0f) {
            ratingBarMy.setRating(saved);
        } else {
            ratingBarMy.setRating(0f);
        }
    }

    private void onClickSubmitRating() {
        if (recipe == null || recipe.getId() == null) {
            AppToast.show(this, "레시피 정보가 없습니다");
            return;
        }
        if (ratingBarMy == null) return;

        float myRating = ratingBarMy.getRating();
        if (myRating <= 0f) {
            AppToast.show(this, "별점을 선택해 주세요");
            return;
        }

        boolean guest = isGuestMode();
        boolean alreadyRated = ratingPrefs().getBoolean(ratedKeyForCurrentMode(recipe.getId()), false);

        if (guest) {
            if (alreadyRated) {
                SpannableString msg = new SpannableString("이미 이 레시피에 평점을 남겼어요.\n새 별점으로 수정할까요?");
                msg.setSpan(new StyleSpan(Typeface.BOLD), 0, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                new AlertDialog.Builder(this)
                        .setTitle("평점 수정")
                        .setMessage(msg)
                        .setPositiveButton("수정하기", (d, w) -> submitGuestRating(myRating))
                        .setNegativeButton("취소", null)
                        .show();
            } else {
                SpannableString msg = new SpannableString("선택한 별점으로 평점을 등록할까요?");
                msg.setSpan(new StyleSpan(Typeface.BOLD), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                new AlertDialog.Builder(this)
                        .setTitle("평점 남기기")
                        .setMessage(msg)
                        .setPositiveButton("등록하기", (d, w) -> submitGuestRating(myRating))
                        .setNegativeButton("취소", null)
                        .show();
            }
            return;
        }

        Long userId = getUserIdFromPrefs();
        if (userId == null) {
            AppToast.show(this, "로그인이 필요합니다");
            return;
        }

        if (alreadyRated) {
            SpannableString msg = new SpannableString("이미 이 레시피에 평점을 남겼어요.\n새 별점으로 수정할까요?");
            msg.setSpan(new StyleSpan(Typeface.BOLD), 0, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            new AlertDialog.Builder(this)
                    .setTitle("평점 수정")
                    .setMessage(msg)
                    .setPositiveButton("수정하기", (d, w) -> submitMyRating(userId, myRating))
                    .setNegativeButton("취소", null)
                    .show();
        } else {
            SpannableString msg = new SpannableString("선택한 별점으로 평점을 등록할까요?");
            msg.setSpan(new StyleSpan(Typeface.BOLD), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            new AlertDialog.Builder(this)
                    .setTitle("평점 남기기")
                    .setMessage(msg)
                    .setPositiveButton("등록하기", (d, w) -> submitMyRating(userId, myRating))
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private void submitGuestRating(float myRating) {
        if (recipe == null || recipe.getId() == null) return;

        ratingPrefs().edit()
                .putBoolean(ratedKeyForCurrentMode(recipe.getId()), true)
                .putFloat(myRatingKeyForCurrentMode(recipe.getId()), myRating)
                .apply();

        if (ratingBarMy != null) {
            ratingBarMy.setRating(myRating);
        }

        AppToast.show(this, "평점이 등록되었습니다 ⭐");
    }

    private void submitMyRating(Long userId, float myRating) {
        RatingRequest request = new RatingRequest(myRating, userId);

        api.rateRecipe(recipe.getId(), request).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipe = response.body();
                    bindRatingInfo(recipe);
                    bindAuthorInfo(recipe);
                    updateAuthorActionButtons();

                    ratingPrefs().edit()
                            .putBoolean(ratedKeyForCurrentMode(recipe.getId()), true)
                            .putFloat(myRatingKeyForCurrentMode(recipe.getId()), myRating)
                            .apply();

                    AppToast.show(RecipeDetailActivity.this, "평점이 등록되었습니다 ⭐");
                } else {
                    AppToast.show(RecipeDetailActivity.this, "평점 저장에 실패했습니다");
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Log.e("RATING", "submit rating error", t);
                AppToast.show(RecipeDetailActivity.this, "네트워크 오류가 발생했습니다");
            }
        });
    }

    private void showSelectMissingDialog() {
        if (recipe == null) return;

        Long userId = getUserIdFromPrefs();
        if (userId == null) {
            AppToast.show(this, "로그인이 필요합니다");
            return;
        }

        List<String> missing = normalize(recipe.getMissingIngredients());
        if (missing.isEmpty()) {
            AppToast.show(this, "부족한 재료가 없습니다");
            return;
        }

        SelectMissingDialog dialog = new SelectMissingDialog(this, missing, selected -> {
            if (selected == null || selected.isEmpty()) {
                AppToast.show(this, "재료를 선택해 주세요");
                return;
            }
            showStorePickerAndAddToCart(userId, selected, null);
        });
        dialog.show();
    }

    private void showStorePickerAndAddToCart(@NonNull Long userId,
                                             @NonNull List<String> names,
                                             @Nullable Done done) {
        if (!(this instanceof FragmentActivity)) {
            addListToCart(userId, names, null, done);
            return;
        }

        StorePickerDialog dialog = new StorePickerDialog(userId, (storeId, storeName) -> {
            String finalStoreName = (storeName == null || storeName.trim().isEmpty()) ? "미지정" : storeName.trim();
            StoreSessionManager.setCurrentStore(this, storeId, finalStoreName);
            addListToCart(userId, names, storeId, done);
        });
        dialog.show(getSupportFragmentManager(), "RecipeMissingStorePickerDialog");
    }

    private void updateAddMissingButtonState() {
        if (btnAddMissingToCart == null) return;

        List<String> missing = (recipe != null) ? recipe.getMissingIngredients() : null;
        boolean hasMissing = missing != null && !missing.isEmpty();

        btnAddMissingToCart.setEnabled(hasMissing);
        btnAddMissingToCart.setAlpha(hasMissing ? 1f : 0.5f);
    }

    private void showMealPlanDayDialog() {
        if (recipe == null) {
            AppToast.show(this, "레시피 정보가 없습니다");
            return;
        }

        Calendar cal = Calendar.getInstance(Locale.KOREA);
        cal.setFirstDayOfWeek(Calendar.SUNDAY);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);

        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        String[] keys = {
                MealPlanPrefs.DAY_SUN,
                MealPlanPrefs.DAY_MON,
                MealPlanPrefs.DAY_TUE,
                MealPlanPrefs.DAY_WED,
                MealPlanPrefs.DAY_THU,
                MealPlanPrefs.DAY_FRI,
                MealPlanPrefs.DAY_SAT
        };

        new AlertDialog.Builder(this)
                .setTitle(String.format(Locale.KOREA, "%d년 %d월 %d주차 식단표에 담기", year, month, weekOfMonth))
                .setItems(labels, (dialog, which) -> {
                    MealPlanPrefs.saveRecipe(this, year, month, weekOfMonth, keys[which], recipe);
                    AppToast.show(this, labels[which] + "요일에 담았습니다");
                })
                .show();

    }

    private void onClickFinishCooking() {
        Long userId = getUserIdFromPrefs();

        if (!isGuestMode() && userId == null) {
            AppToast.show(this, "로그인이 필요합니다");
            return;
        }

        List<String> candidates = getIngredientCandidates();
        if (candidates.isEmpty()) {
            AppToast.show(this, "사용한 재료를 찾을 수 없습니다");
            return;
        }

        showFinishCookingDialog(userId, candidates);
    }

    private void showFinishCookingDialog(@Nullable Long userId, @NonNull List<String> candidates) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_recipe_finish_cooking, null);

        RecyclerView rv = view.findViewById(R.id.rvUsedIngredients);
        Button btnSelectAll = view.findViewById(R.id.btnSelectAll);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        UsedIngredientAdapter adapter = new UsedIngredientAdapter(candidates);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        rv.setHasFixedSize(false);
        rv.setNestedScrollingEnabled(true);
        rv.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int dialogWidth = (int) (screenWidth * 0.92f);
                dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            rv.post(() -> {
                rv.requestLayout();
                adapter.notifyDataSetChanged();
            });
        });

        btnSelectAll.setOnClickListener(v -> {
            adapter.toggleSelectAll();
            btnSelectAll.setText(adapter.isAllSelected() ? "전체해제" : "전체선택");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            List<String> selected = adapter.getSelected();

            if (selected.isEmpty()) {
                AppToast.show(this, "사용한 재료를 선택해 주세요");
                return;
            }

            dialog.dismiss();

            removeFromFridge(userId, selected, () -> {
                ChecklistPrefs.markCookDone(RecipeDetailActivity.this, 1);

                // ✅ 요리 완료 EXP 지급
                com.namgyun.tamakitchen.pet.PetExpManager.giveCookCompleteExp(
                        RecipeDetailActivity.this
                );
                List<String> missing = normalize(recipe.getMissingIngredients());

                if (missing.isEmpty()) {
                    AppToast.show(RecipeDetailActivity.this, "요리 완료! 부족한 재료가 없습니다");
                    return;
                }

                new AlertDialog.Builder(RecipeDetailActivity.this)
                        .setTitle("장바구니 추가")
                        .setMessage("부족한 재료를 장바구니에 추가할까요?")
                        .setPositiveButton("추가하기", (d, w) ->
                                showStorePickerAndAddToCart(userId, missing, () ->
                                        AppToast.show(RecipeDetailActivity.this, "요리 완료 처리 끝!")
                                )
                        )
                        .setNegativeButton("닫기", (d, w) ->
                                AppToast.show(RecipeDetailActivity.this, "요리 완료 처리 끝!")
                        )
                        .show();
            });
        });

        dialog.show();
    }

    private List<String> getIngredientCandidates() {
        Set<String> set = new HashSet<>();

        List<RecipeResponse.RecipeIngredientItem> all = null;
        try {
            all = recipe.getIngredientItems();
        } catch (Exception ignored) {
        }

        if (all != null && !all.isEmpty()) {
            for (RecipeResponse.RecipeIngredientItem it : all) {
                if (it == null) continue;
                String type = safe(it.getType()).trim().toUpperCase(Locale.ROOT);
                if ("SUBSTITUTE".equals(type)) continue;
                if ("OPTIONAL".equals(type)) continue;
                String n = safe(it.getName()).trim();
                if (!n.isEmpty()) set.add(n);
            }
        }

        String ing = recipe != null ? recipe.getIngredients() : null;
        if (!TextUtils.isEmpty(ing)) {
            String[] parts = ing.split(",");
            for (String p : parts) {
                if (p == null) continue;
                String t = p.trim();
                if (!t.isEmpty()) set.add(t);
            }
        }

        if (set.isEmpty()) {
            set.addAll(normalize(recipe.getUsedIngredients()));
            set.addAll(normalize(recipe.getMissingIngredients()));
        }

        return new ArrayList<>(set);
    }

    private List<String> normalize(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) {
            if (s == null) continue;
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private interface Done {
        void run();
    }

    private void removeFromFridge(@Nullable Long userId, List<String> selectedNames, Done done) {
        if (mode.isGuestMode(this)) {
            try {
                List<FridgeItem> local = GuestFridgeStore.load(this);
                if (local == null) local = new ArrayList<>();

                boolean changed = false;
                List<FridgeItem> keep = new ArrayList<>();

                for (FridgeItem it : local) {
                    if (it == null) continue;
                    String n = it.getName();
                    if (TextUtils.isEmpty(n)) {
                        keep.add(it);
                        continue;
                    }

                    boolean shouldRemove = false;
                    for (String sel : selectedNames) {
                        if (sel == null) continue;
                        if (n.trim().equals(sel.trim())) {
                            shouldRemove = true;
                            break;
                        }
                    }

                    if (shouldRemove) {
                        changed = true;
                    } else {
                        keep.add(it);
                    }
                }

                if (changed) {
                    GuestFridgeStore.save(this, keep);
                    AppToast.show(this, "냉장고에서 재료를 제거했어요");
                } else {
                    AppToast.show(this, "체크한 재료가 냉장고에 없어요");
                }

                if (done != null) done.run();
            } catch (Exception e) {
                AppToast.show(this, "게스트 냉장고 처리 오류");
            }
            return;
        }

        if (userId == null || userId <= 0) {
            AppToast.show(this, "로그인이 필요합니다");
            return;
        }

        if (mode.isSharedFridgeMode(this)) {
            long fridgeId = mode.getSharedFridgeIdSafe(this);
            if (fridgeId <= 0) {
                mode.handleMissingSharedFridgeId(this, () ->
                        AppToast.show(this, "개인 냉장고로 전환했습니다. 다시 시도해주세요")
                );
                return;
            }

            fridgeApi.getSharedFridgeItems(fridgeId, userId).enqueue(new Callback<List<FridgeItem>>() {
                @Override
                public void onResponse(Call<List<FridgeItem>> call, Response<List<FridgeItem>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        AppToast.show(RecipeDetailActivity.this, "냉장고 불러오기 실패");
                        return;
                    }
                    handleDeleteItemsFromLoadedList(userId, response.body(), selectedNames, done);
                }

                @Override
                public void onFailure(Call<List<FridgeItem>> call, Throwable t) {
                    AppToast.show(RecipeDetailActivity.this, "네트워크 오류가 발생했습니다");
                }
            });

        } else {
            fridgeApi.getFridgeItems(userId).enqueue(new Callback<List<FridgeItem>>() {
                @Override
                public void onResponse(Call<List<FridgeItem>> call, Response<List<FridgeItem>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        AppToast.show(RecipeDetailActivity.this, "냉장고 불러오기 실패");
                        return;
                    }
                    handleDeleteItemsFromLoadedList(userId, response.body(), selectedNames, done);
                }

                @Override
                public void onFailure(Call<List<FridgeItem>> call, Throwable t) {
                    AppToast.show(RecipeDetailActivity.this, "네트워크 오류가 발생했습니다");
                }
            });
        }
    }

    private void handleDeleteItemsFromLoadedList(Long userId,
                                                 @NonNull List<FridgeItem> loaded,
                                                 @NonNull List<String> selectedNames,
                                                 @Nullable Done done) {

        List<Long> deleteIds = new ArrayList<>();
        for (FridgeItem item : loaded) {
            if (item == null) continue;
            String itemName = item.getName();
            if (TextUtils.isEmpty(itemName)) continue;

            for (String sel : selectedNames) {
                if (sel == null) continue;
                if (itemName.trim().equals(sel.trim())) {
                    if (item.getId() != null) deleteIds.add(item.getId());
                    break;
                }
            }
        }

        if (deleteIds.isEmpty()) {
            AppToast.show(RecipeDetailActivity.this, "체크한 재료가 냉장고에 없어요");
            if (done != null) done.run();
            return;
        }

        final int total = deleteIds.size();
        final int[] finished = {0};
        final int[] success = {0};

        for (Long id : deleteIds) {
            fridgeApi.deleteFridgeItem(id, userId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> r) {
                    finished[0]++;
                    if (r.isSuccessful()) success[0]++;
                    if (finished[0] == total) {
                        AppToast.show(RecipeDetailActivity.this,
                                "냉장고에서 " + success[0] + "개 제거했어요");
                        if (done != null) done.run();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    finished[0]++;
                    if (finished[0] == total) {
                        AppToast.show(RecipeDetailActivity.this,
                                "냉장고 제거 완료 (성공 " + success[0] + "/" + total + ")");
                        if (done != null) done.run();
                    }
                }
            });
        }
    }

    private String resolveShoppingIconKey(@Nullable String ingredientName) {
        if (ingredientName == null) return "custom_item";

        String target = ingredientName.trim();
        if (target.isEmpty()) return "custom_item";

        List<IconItem> icons = IconCatalog.getAllIcons();
        if (icons != null) {
            for (IconItem icon : icons) {
                if (icon == null) continue;

                String iconName = icon.getName() == null ? "" : icon.getName().trim();
                if (!iconName.equals(target)) continue;

                String key = icon.getNormalizedKey();
                if (key != null && !key.trim().isEmpty()) {
                    return key.trim();
                }

                key = icon.getSearchKey();
                if (key != null && !key.trim().isEmpty()) {
                    return key.trim();
                }

                break;
            }
        }

        return "custom_item";
    }

    private void addListToCart(Long userId, List<String> names, @Nullable Long storeId, @Nullable Done done) {
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        final int total = names.size();
        final int[] finished = {0};
        final int[] success = {0};

        for (String name : names) {
            String iconKey = resolveShoppingIconKey(name);

            ShoppingItemRequest req = new ShoppingItemRequest(
                    name,
                    1,
                    iconKey,
                    userId,
                    0,
                    dateKey,
                    "개",
                    storeId
            );

            shoppingApi.addItem(req).enqueue(new Callback<com.namgyun.tamakitchen.ui.shopping.ShoppingItem>() {
                @Override
                public void onResponse(Call<com.namgyun.tamakitchen.ui.shopping.ShoppingItem> call,
                                       Response<com.namgyun.tamakitchen.ui.shopping.ShoppingItem> response) {
                    finished[0]++;
                    if (response.isSuccessful() && response.body() != null) success[0]++;
                    if (finished[0] == total) {
                        AppToast.show(RecipeDetailActivity.this,
                                "장바구니에 " + success[0] + "개 담았습니다 🛒");
                        if (done != null) done.run();
                    }
                }

                @Override
                public void onFailure(Call<com.namgyun.tamakitchen.ui.shopping.ShoppingItem> call, Throwable t) {
                    finished[0]++;
                    if (finished[0] == total) {
                        AppToast.show(RecipeDetailActivity.this,
                                "장바구니에 " + success[0] + "개 담았습니다 🛒");
                        if (done != null) done.run();
                    }
                }
            });
        }
    }

    private static class MissingIngredientItem {
        private final String name;
        private boolean checked;

        MissingIngredientItem(String name, boolean checked) {
            this.name = name;
            this.checked = checked;
        }

        String getName() {
            return name;
        }

        boolean isChecked() {
            return checked;
        }

        void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    private static class MissingIngredientAdapter extends RecyclerView.Adapter<MissingIngredientAdapter.VH> {
        private final List<MissingIngredientItem> items = new ArrayList<>();

        MissingIngredientAdapter(List<MissingIngredientItem> initial) {
            if (initial != null) items.addAll(initial);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_checkbox_ingredient, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MissingIngredientItem item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvCount.setText("1개");
            applySelectedUI(holder, item.isChecked());

            View.OnClickListener toggleListener = v -> {
                boolean next = !item.isChecked();
                item.setChecked(next);
                applySelectedUI(holder, next);
            };

            holder.itemView.setOnClickListener(toggleListener);
            holder.ivCheck.setOnClickListener(toggleListener);
            holder.tvName.setOnClickListener(toggleListener);
            holder.tvCount.setOnClickListener(toggleListener);
        }

        private void applySelectedUI(@NonNull VH holder, boolean selected) {
            holder.ivCheck.setSelected(selected);
            holder.ivCheck.refreshDrawableState();
            holder.ivCheck.invalidate();

            holder.itemView.setBackgroundResource(
                    selected ? R.drawable.bg_checkbox_row_selected : R.drawable.bg_item_selectable
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void setAllChecked(boolean checked) {
            for (MissingIngredientItem it : items) {
                it.setChecked(checked);
            }
            notifyDataSetChanged();
        }

        List<String> getSelectedNames() {
            List<String> out = new ArrayList<>();
            for (MissingIngredientItem it : items) {
                if (it.isChecked()) out.add(it.getName());
            }
            return out;
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivCheck;
            TextView tvName;
            TextView tvCount;

            VH(@NonNull View itemView) {
                super(itemView);
                ivCheck = itemView.findViewById(R.id.ivCheck);
                tvName = itemView.findViewById(R.id.tvIngredientName);
                tvCount = itemView.findViewById(R.id.tvIngredientCount);
            }
        }
    }

    private static class SelectMissingDialog extends Dialog {

        interface OnConfirm {
            void onConfirm(List<String> selected);
        }

        private final List<String> names;
        private final OnConfirm onConfirm;
        private boolean allSelected = false;

        SelectMissingDialog(@NonNull RecipeDetailActivity activity,
                            @NonNull List<String> names,
                            @NonNull OnConfirm onConfirm) {
            super(activity);
            this.names = names;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);

            if (getWindow() != null) {
                getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                );
                getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            setContentView(R.layout.dialog_select_missing_ingredients);

            RecyclerView rv = findViewById(R.id.rvIngredients);
            Button btnSelectAll = findViewById(R.id.btnSelectAll);
            Button btnCancel = findViewById(R.id.btnCancel);
            Button btnConfirm = findViewById(R.id.btnConfirm);

            List<MissingIngredientItem> items = new ArrayList<>();
            for (String n : names) {
                if (n == null) continue;
                String t = n.trim();
                if (!t.isEmpty()) items.add(new MissingIngredientItem(t, false));
            }

            MissingIngredientAdapter adapter = new MissingIngredientAdapter(items);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setHasFixedSize(true);
            rv.setNestedScrollingEnabled(true);
            rv.setAdapter(adapter);

            btnSelectAll.setOnClickListener(v -> {
                allSelected = !allSelected;
                adapter.setAllChecked(allSelected);
                btnSelectAll.setText(allSelected ? "전체해제" : "전체선택");
            });

            btnCancel.setOnClickListener(v -> dismiss());

            btnConfirm.setOnClickListener(v -> {
                List<String> selected = adapter.getSelectedNames();
                onConfirm.onConfirm(selected);
                dismiss();
            });
        }
    }
}