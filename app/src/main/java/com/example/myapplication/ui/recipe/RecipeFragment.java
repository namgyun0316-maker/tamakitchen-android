package com.namgyun.tamakitchen.ui.recipe;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.analytics.AppAnalytics;
import com.namgyun.tamakitchen.network.RecipeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.fusion.FusionFood;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgePrefs;
import com.namgyun.tamakitchen.ui.recipe.adapter.RecipeAdapter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeFragment extends Fragment {

    private static final String TAG = "RecipeFragment";

    private enum Mode { RECOMMEND, ALL }
    private Mode currentMode = Mode.RECOMMEND;

    private enum Cuisine { ALL, KOREAN, JAPANESE, CHINESE, WESTERN, DESSERT, DIET, FAVORITE }
    private Cuisine currentCuisine = Cuisine.ALL;

    private static final String PREFS_FAVORITE = "favorite_prefs";
    private static final String KEY_FAV_PREFIX = "fav_";

    private static final String PREFS_LOGIN = "login_prefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_GUEST = "isGuest";

    private static final String DEFAULT_EMPTY_MESSAGE = "조건에 맞는 레시피가 없어요.";
    private static final String SERVER_ERROR_MESSAGE = "레시피를 불러오지 못했어요. 잠시 후 다시 시도해주세요.";

    private RecyclerView recyclerView;
    private TextView emptyRecipeText;
    private View layoutRecipeLoading;
    private ImageView imgLoadingPet;
    private TextView tvRecipeLoadingText;

    private Button btnRecommend, btnAll, btnAddRecipe;
    private ChipGroup chipCuisineGroup;
    private TextInputEditText etRecipeSearch;

    private String currentKeyword = "";
    private String currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;

    private RecipeAdapter adapter;
    private RecipeApiService service;

    private List<RecipeResponse> originalList = new ArrayList<>();
    private final Map<Long, RecipeResponse> recommendMap = new HashMap<>();

    private List<RecipeResponse> cachedRecommendList = null;
    private List<RecipeResponse> cachedAllList = null;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long SEARCH_DEBOUNCE_MS = 250L;
    private Runnable pendingSearchRunnable;

    private boolean needsReload = true;
    private boolean resumedOnce = false;
    private boolean isLoading = false;

    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recipe, container, false);

        recyclerView = view.findViewById(R.id.recipeRecyclerView);
        emptyRecipeText = view.findViewById(R.id.emptyRecipeText);
        layoutRecipeLoading = view.findViewById(R.id.layoutRecipeLoading);
        imgLoadingPet = view.findViewById(R.id.imgLoadingPet);
        tvRecipeLoadingText = view.findViewById(R.id.tvRecipeLoadingText);

        btnRecommend = view.findViewById(R.id.btnRecommend);
        btnAll = view.findViewById(R.id.btnAll);
        btnAddRecipe = view.findViewById(R.id.btnAddRecipe);

        chipCuisineGroup = view.findViewById(R.id.chipCuisineGroup);
        etRecipeSearch = view.findViewById(R.id.etRecipeSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setItemAnimator(null);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        adapter = new RecipeAdapter(requireContext(), RecipeAdapter.ListType.FRIDGE);
        recyclerView.setAdapter(adapter);
        hookAdapterCallbacks();

        service = RetrofitClient.getRecipeApi();

        pushUserCookToolsToAdapter();

        btnAddRecipe.setOnClickListener(v -> {
            clearRecipeCaches();

            try {
                Intent intent = new Intent(requireContext(), RecipeWriteActivity.class);
                startActivity(intent);
                needsReload = true;
            } catch (Exception e) {
                Log.e(TAG, "open RecipeWriteActivity failed", e);
                AppToast.show(requireActivity(), "레시피 등록 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
            }
        });

        setupSearchListener();

        btnRecommend.setOnClickListener(v -> {
            currentMode = Mode.RECOMMEND;
            adapter.setListType(RecipeAdapter.ListType.FRIDGE);

            updateModeButtonUI();
            clearSearch();

            adapter.forceResort();

            if (cachedRecommendList != null && !cachedRecommendList.isEmpty()) {
                currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
                originalList = new ArrayList<>(cachedRecommendList);
                applyFilterAndUpdate();
            } else {
                needsReload = true;
                reloadIfNeeded();
            }
        });

        btnAll.setOnClickListener(v -> {
            currentMode = Mode.ALL;
            adapter.setListType(RecipeAdapter.ListType.ALL);

            updateModeButtonUI();
            clearSearch();

            adapter.forceResort();

            if (cachedAllList != null && !cachedAllList.isEmpty()) {
                currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
                originalList = new ArrayList<>(cachedAllList);
                applyFilterAndUpdate();
            } else {
                needsReload = true;
                reloadIfNeeded();
            }
        });

        chipCuisineGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentCuisine = cuisineFromChipId(checkedId);
            currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
            applyFilterAndUpdate();
        });

        currentCuisine = cuisineFromChipId(chipCuisineGroup.getCheckedChipId());

        updateModeButtonUI();
        showOnlyEmpty(false);

        return view;
    }

    private void showLoading(boolean show) {
        if (layoutRecipeLoading == null) return;

        if (show) {
            if (layoutRecipeLoading.getVisibility() != View.VISIBLE) {
                setRandomGrayLoadingPet();
            }

            if (tvRecipeLoadingText != null) {
                tvRecipeLoadingText.setText("레시피를 불러오는 중...");
            }

            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (emptyRecipeText != null) emptyRecipeText.setVisibility(View.GONE);

            layoutRecipeLoading.setVisibility(View.VISIBLE);
        } else {
            layoutRecipeLoading.setVisibility(View.GONE);
        }
    }

    private void showOnlyEmpty(boolean show) {
        if (layoutRecipeLoading != null) layoutRecipeLoading.setVisibility(View.GONE);

        if (show) {
            if (emptyRecipeText != null) {
                emptyRecipeText.setText(currentEmptyMessage);
                emptyRecipeText.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (emptyRecipeText != null) emptyRecipeText.setVisibility(View.GONE);
        }
    }

    private void showOnlyList(boolean show) {
        if (layoutRecipeLoading != null) layoutRecipeLoading.setVisibility(View.GONE);
        if (emptyRecipeText != null) emptyRecipeText.setVisibility(View.GONE);

        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void setRandomGrayLoadingPet() {
        if (!isAdded() || imgLoadingPet == null) return;

        List<String> drawableNames = new ArrayList<>();

        for (IngredientCharacter character : IngredientCharacter.values()) {
            if (character != null && !TextUtils.isEmpty(character.getDrawableName())) {
                drawableNames.add(toCatalogDrawableName(character.getDrawableName()));
            }
        }

        for (FusionFood food : FusionFood.values()) {
            if (food != null && !TextUtils.isEmpty(food.getDrawableName())) {
                drawableNames.add(toCatalogDrawableName(food.getDrawableName()));
            }
        }

        if (drawableNames.isEmpty()) {
            imgLoadingPet.setImageDrawable(null);
            return;
        }

        int drawableResId = 0;

        for (int i = 0; i < drawableNames.size(); i++) {
            String drawableName = drawableNames.get(random.nextInt(drawableNames.size()));

            drawableResId = getResources().getIdentifier(
                    drawableName,
                    "drawable",
                    requireContext().getPackageName()
            );

            if (drawableResId != 0) break;
        }

        if (drawableResId == 0) {
            imgLoadingPet.setImageDrawable(null);
            return;
        }

        imgLoadingPet.setImageResource(drawableResId);
        imgLoadingPet.setAlpha(0.62f);

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        imgLoadingPet.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    private String toCatalogDrawableName(String drawableName) {
        if (drawableName == null) return "";

        if (drawableName.endsWith("_body")) {
            return drawableName.replace("_body", "_catalog");
        }

        return drawableName;
    }

    private void clearRecipeCaches() {
        cachedAllList = null;
        cachedRecommendList = null;
        recommendMap.clear();
    }

    private Cuisine cuisineFromChipId(int checkedId) {
        if (checkedId == View.NO_ID) return Cuisine.ALL;

        if (checkedId == R.id.chipCuisineAll) return Cuisine.ALL;
        if (checkedId == R.id.chipCuisineKorean) return Cuisine.KOREAN;
        if (checkedId == R.id.chipCuisineJapanese) return Cuisine.JAPANESE;
        if (checkedId == R.id.chipCuisineChinese) return Cuisine.CHINESE;
        if (checkedId == R.id.chipCuisineWestern) return Cuisine.WESTERN;
        if (checkedId == R.id.chipCuisineDessert) return Cuisine.DESSERT;
        if (checkedId == R.id.chipCuisineDiet) return Cuisine.DIET;
        if (checkedId == R.id.chipCuisineFavorite) return Cuisine.FAVORITE;

        return Cuisine.ALL;
    }

    private void setupSearchListener() {
        if (etRecipeSearch == null) return;

        etRecipeSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = (s == null) ? "" : s.toString();

                if (pendingSearchRunnable != null) uiHandler.removeCallbacks(pendingSearchRunnable);
                pendingSearchRunnable = () -> {
                    if (!isAdded()) return;
                    currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
                    applyFilterAndUpdate();
                };
                uiHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void clearSearch() {
        currentKeyword = "";
        currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;

        if (pendingSearchRunnable != null) uiHandler.removeCallbacks(pendingSearchRunnable);
        if (etRecipeSearch != null) etRecipeSearch.setText("");
    }

    private void hookAdapterCallbacks() {
        adapter.setOnRecipeClickListener(item -> {

            if (item != null) {
                AppAnalytics.logRecipeView(requireContext(), item.getName());
            }

            try {
                Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);

                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, item);

                intent.putStringArrayListExtra(
                        RecipeDetailActivity.EXTRA_USED_INGREDIENTS,
                        toArrayList(item != null ? item.getUsedIngredients() : null)
                );
                intent.putStringArrayListExtra(
                        RecipeDetailActivity.EXTRA_MISSING_INGREDIENTS,
                        toArrayList(item != null ? item.getMissingIngredients() : null)
                );

                intent.putExtra(RecipeDetailActivity.EXTRA_MATCH_PERCENT, item != null ? item.getMatchPercent() : 0);
                intent.putExtra(RecipeDetailActivity.EXTRA_LIST_TYPE, adapterListTypeName());

                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "open RecipeDetailActivity failed", e);
                AppToast.show(requireActivity(), "레시피 상세 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
            }
        });

        adapter.setOnRecipeDeleteListener((item, position) -> {
            if (item == null) {
                AppToast.show(requireActivity(), "레시피 정보를 확인할 수 없습니다.");
                return;
            }

            long loginUserId = AuthPrefs.getUserId(requireContext());
            Long authorId = item.getAuthorId();

            if (loginUserId <= 0 || authorId == null || !authorId.equals(loginUserId)) {
                AppToast.show(requireActivity(), "본인이 작성한 레시피만 삭제할 수 있습니다.");
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("레시피 삭제")
                    .setMessage("'" + item.getName() + "' 레시피를 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        if (item.getId() != null) {
                            deleteRecipe(item.getId(), position);
                        } else {
                            AppToast.show(requireActivity(), "삭제할 레시피 ID가 없습니다.");
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        adapter.setOnFavoriteChangedListener(this::applyFilterAndUpdate);
    }

    private String adapterListTypeName() {
        return (currentMode == Mode.ALL)
                ? RecipeAdapter.ListType.ALL.name()
                : RecipeAdapter.ListType.FRIDGE.name();
    }

    private ArrayList<String> toArrayList(List<String> list) {
        ArrayList<String> out = new ArrayList<>();
        if (list == null) return out;

        for (String s : list) {
            if (s == null) continue;

            String t = s.trim();

            if (!t.isEmpty()) {
                out.add(t);
            }
        }

        return out;
    }

    @Override
    public void onResume() {
        super.onResume();

        AppAnalytics.logScreen(requireContext(), "recipe_screen");

        if (!resumedOnce) {
            resumedOnce = true;
            needsReload = true;
        }

        pushUserCookToolsToAdapter();
        reloadIfNeeded();
    }

    private void reloadIfNeeded() {
        if (!isAdded()) return;
        if (isLoading) return;

        if (!needsReload) {
            applyFilterAndUpdate();
            return;
        }

        needsReload = false;
        reload();
    }

    private void updateModeButtonUI() {
        int selectedBg = Color.parseColor("#5CB6E4");
        int unselectedBg = Color.parseColor("#87CEEB");
        int textColor = Color.WHITE;

        if (currentMode == Mode.RECOMMEND) {
            btnRecommend.setBackgroundTintList(ColorStateList.valueOf(selectedBg));
            btnRecommend.setTextColor(textColor);

            btnAll.setBackgroundTintList(ColorStateList.valueOf(unselectedBg));
            btnAll.setTextColor(textColor);
        } else {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(selectedBg));
            btnAll.setTextColor(textColor);

            btnRecommend.setBackgroundTintList(ColorStateList.valueOf(unselectedBg));
            btnRecommend.setTextColor(textColor);
        }
    }

    @Nullable
    private Long getUserIdOrNullFromLoginPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_LOGIN, android.content.Context.MODE_PRIVATE);

        boolean isGuest = prefs.getBoolean(KEY_IS_GUEST, false);
        long id = prefs.getLong(KEY_USER_ID, -1L);

        Log.d(TAG, "prefs check => isGuest=" + isGuest + ", userId=" + id);

        return (id > 0L) ? id : null;
    }

    private void reload() {
        if (currentMode == Mode.RECOMMEND) {

            if (cachedRecommendList != null && !cachedRecommendList.isEmpty()) {
                currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
                originalList = new ArrayList<>(cachedRecommendList);
                applyFilterAndUpdate();
                return;
            }

            String type = OnboardingPrefs.getFridgeType(requireContext());
            boolean isShared = "SHARED".equalsIgnoreCase(type);

            Long userId = getUserIdOrNullFromLoginPrefs();
            long fridgeId = SharedFridgePrefs.getFridgeId(requireContext());

            Log.d(TAG, "reload() RECOMMEND type=" + type + " userId=" + userId + " fridgeId=" + fridgeId);

            if (isShared) {
                if (fridgeId <= 0) {
                    currentEmptyMessage = "공동 냉장고를 먼저 만들거나 참여해주세요.";
                    originalList = new ArrayList<>();
                    applyFilterAndUpdate();
                    return;
                }

                loadRecommendedRecipes(null, fridgeId);
                return;
            }

            if (userId == null) {
                currentMode = Mode.ALL;
                adapter.setListType(RecipeAdapter.ListType.ALL);
                updateModeButtonUI();

                adapter.forceResort();
                loadAllRecipes();
                return;
            }

            loadRecommendedRecipes(userId, null);

        } else {
            if (cachedAllList != null && !cachedAllList.isEmpty()) {
                currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
                originalList = new ArrayList<>(cachedAllList);
                applyFilterAndUpdate();
                return;
            }

            loadAllRecipes();
        }
    }

    private void loadRecommendedRecipes(@Nullable Long userId, @Nullable Long fridgeId) {
        if (service == null) return;

        isLoading = true;
        currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
        showLoading(true);
        recommendMap.clear();

        service.getRecommendedRecipes(userId, fridgeId).enqueue(new Callback<List<RecipeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecipeResponse>> call,
                                   @NonNull Response<List<RecipeResponse>> response) {
                isLoading = false;
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
                    originalList = response.body();
                    cachedRecommendList = new ArrayList<>(originalList);

                    for (RecipeResponse r : originalList) {
                        if (r != null && r.getId() != null) {
                            recommendMap.put(r.getId(), r);
                        }
                    }

                    applyFilterAndUpdate();
                } else {
                    logHttpError("recommend", response);
                    showError();

                    originalList = new ArrayList<>();
                    applyFilterAndUpdate();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RecipeResponse>> call,
                                  @NonNull Throwable t) {
                isLoading = false;
                showLoading(false);
                if (!isAdded()) return;

                logFailure("recommend", t);
                showNetworkError(t);

                originalList = new ArrayList<>();
                applyFilterAndUpdate();
            }
        });
    }

    private void loadAllRecipes() {
        if (service == null) return;

        isLoading = true;
        currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;
        showLoading(true);

        service.getAllRecipes().enqueue(new Callback<List<RecipeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecipeResponse>> call,
                                   @NonNull Response<List<RecipeResponse>> response) {
                isLoading = false;
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    currentEmptyMessage = DEFAULT_EMPTY_MESSAGE;

                    List<RecipeResponse> allList = response.body();

                    for (RecipeResponse r : allList) {
                        if (r == null) continue;

                        Long id = r.getId();
                        if (id != null && recommendMap.containsKey(id)) {
                            RecipeResponse rec = recommendMap.get(id);
                            if (rec != null) {
                                r.setMatchPercent(rec.getMatchPercent());
                                r.setUsedIngredients(rec.getUsedIngredients());
                                r.setMissingIngredients(rec.getMissingIngredients());
                            }
                        }
                    }

                    originalList = allList;
                    cachedAllList = new ArrayList<>(allList);

                    adapter.forceResort();
                    applyFilterAndUpdate();

                } else {
                    logHttpError("all", response);
                    showError();

                    originalList = new ArrayList<>();
                    applyFilterAndUpdate();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RecipeResponse>> call,
                                  @NonNull Throwable t) {
                isLoading = false;
                showLoading(false);
                if (!isAdded()) return;

                logFailure("all", t);
                showNetworkError(t);

                originalList = new ArrayList<>();
                applyFilterAndUpdate();
            }
        });
    }

    private void applyFilterAndUpdate() {
        if (!isAdded()) return;

        if (isLoading) {
            showLoading(true);
            return;
        }

        if (originalList == null) originalList = new ArrayList<>();

        String q = (currentKeyword == null) ? "" : currentKeyword.trim().toLowerCase(Locale.getDefault());

        List<RecipeResponse> filtered = new ArrayList<>();
        for (RecipeResponse r : originalList) {
            if (r == null) continue;

            if (!matchesCuisine(r, currentCuisine)) continue;

            if (!TextUtils.isEmpty(q)) {
                String name = safeLower(r.getName());
                String summary = safeLower(r.getSummary());
                String ingredients = safeLower(r.getIngredients());
                String used = joinLower(r.getUsedIngredients());
                String missing = joinLower(r.getMissingIngredients());

                boolean match = name.contains(q)
                        || summary.contains(q)
                        || ingredients.contains(q)
                        || used.contains(q)
                        || missing.contains(q);

                if (!match) continue;
            }

            filtered.add(r);
        }

        if (filtered.isEmpty()) {
            adapter.submitItems(new ArrayList<>());
            showOnlyEmpty(true);
        } else {
            adapter.submitItems(filtered);
            showOnlyList(true);
        }
    }

    private void pushUserCookToolsToAdapter() {
        if (!isAdded() || adapter == null) return;

        List<String> tools = readUserCookToolsFromPrefsSafe();
        adapter.setUserCookTools(tools);
    }

    private List<String> readUserCookToolsFromPrefsSafe() {
        try {
            return new ArrayList<>(OnboardingPrefs.getTools(requireContext()));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.getDefault());
    }

    private String joinLower(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return TextUtils.join(", ", list).toLowerCase(Locale.getDefault());
    }

    private SharedPreferences favPrefs() {
        return requireContext().getSharedPreferences(PREFS_FAVORITE, android.content.Context.MODE_PRIVATE);
    }

    private boolean isFavorite(Long recipeId) {
        if (recipeId == null) return false;
        return favPrefs().getBoolean(KEY_FAV_PREFIX + recipeId, false);
    }

    private boolean matchesCuisine(RecipeResponse r, Cuisine c) {
        if (c == Cuisine.ALL) return true;

        if (c == Cuisine.FAVORITE) {
            return isFavorite(r.getId());
        }

        String category = r.getCategory();
        if (category != null) category = category.trim();

        switch (c) {
            case KOREAN:
                return "한식".equals(category);
            case JAPANESE:
                return "일식".equals(category);
            case CHINESE:
                return "중식".equals(category);
            case WESTERN:
                return "양식".equals(category);
            case DESSERT:
                return "디저트".equals(category);
            case DIET:
                return "다이어트".equals(category);
            default:
                return true;
        }
    }

    private void deleteRecipe(Long id, int position) {
        if (service == null) return;

        long loginUserId = AuthPrefs.getUserId(requireContext());
        if (loginUserId <= 0) {
            AppToast.show(requireActivity(), "로그인 후 이용해주세요.");
            return;
        }

        service.deleteRecipe(id, loginUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    AppToast.show(requireActivity(), "삭제되었습니다.");

                    clearRecipeCaches();

                    needsReload = true;
                    adapter.forceResort();
                    reloadIfNeeded();
                } else {
                    logHttpError("delete", response);
                    AppToast.show(requireActivity(), "삭제에 실패했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;

                logFailure("delete", t);
                AppToast.show(requireActivity(), NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    private void showError() {
        currentEmptyMessage = SERVER_ERROR_MESSAGE;
    }

    private void showNetworkError(Throwable t) {
        currentEmptyMessage = NetworkErrorUtil.getUserMessage(t);
    }

    private void logHttpError(String apiName, Response<?> response) {
        String err = "";

        try {
            if (response.errorBody() != null) {
                err = response.errorBody().string();
            }
        } catch (IOException ignored) {
        }

        Log.e(TAG, "❌ HTTP ERROR api=" + apiName + " code=" + response.code() + " errorBody=" + err);
    }

    private void logFailure(String apiName, Throwable t) {
        Log.e(TAG, "❌ FAILURE api=" + apiName + " throwable=" + t.getClass().getName() + " msg=" + t.getMessage(), t);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (pendingSearchRunnable != null) {
            uiHandler.removeCallbacks(pendingSearchRunnable);
        }

        if (imgLoadingPet != null) {
            imgLoadingPet.clearColorFilter();
        }
    }
}