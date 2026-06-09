package com.namgyun.tamakitchen.ui.fusion;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionCategory;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FusionActivity extends AppCompatActivity {

    private static final String TAG = "FusionUi";
    private static final int REQUIRED_LEVEL = PetPrefs.MAX_LEVEL;
    private static final int GRID_COLUMN_COUNT = 3;

    private LinearLayout layoutRecipeEntry;
    private ImageView ivRecipeBookIcon;
    private TextView tvRecipeEntry;

    private ImageView ivSlot1;
    private ImageView ivSlot2;
    private TextView tvSlot1Name;
    private TextView tvSlot2Name;

    private TextView tvFusionGuide;
    private TextView tvFusionResult;

    private RecyclerView recyclerFusionIngredients;
    private TextView tvFusionIngredientEmpty;

    private MaterialButton btnFusionAction;
    private MaterialButton btnResetSelection;

    @Nullable
    private IngredientCharacter selectedIngredient1;

    @Nullable
    private IngredientCharacter selectedIngredient2;

    @Nullable
    private FusionFood selectedResultFood;

    private boolean isFusing = false;

    private FusionIngredientAdapter adapter;
    private final List<IngredientCharacter> ingredientItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fusion);

        PetPrefs.migrateLegacyUnlockedCharacterCountsIfNeeded(this);

        bindViews();
        setupRecyclerView();
        bindEvents();

        ensureEligibleIngredientsMaterializedFromCatalog();
        refreshUi();
        FusionDebugReporter.print(this);
        logRecyclerState("onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();

        PetPrefs.migrateLegacyUnlockedCharacterCountsIfNeeded(this);
        ensureEligibleIngredientsMaterializedFromCatalog();
        refreshUi();
        FusionDebugReporter.print(this);
        logRecyclerState("onResume");
    }

    private void bindViews() {
        layoutRecipeEntry = findViewById(R.id.layoutRecipeEntry);
        ivRecipeBookIcon = findViewById(R.id.ivRecipeBookIcon);
        tvRecipeEntry = findViewById(R.id.tvRecipeEntry);

        ivSlot1 = findViewById(R.id.ivFusionSlot1);
        ivSlot2 = findViewById(R.id.ivFusionSlot2);
        tvSlot1Name = findViewById(R.id.tvFusionSlot1Name);
        tvSlot2Name = findViewById(R.id.tvFusionSlot2Name);

        tvFusionGuide = findViewById(R.id.tvFusionGuide);
        tvFusionResult = findViewById(R.id.tvFusionResult);

        recyclerFusionIngredients = findViewById(R.id.recyclerFusionIngredients);
        tvFusionIngredientEmpty = findViewById(R.id.tvFusionIngredientEmpty);

        btnFusionAction = findViewById(R.id.btnFusionAction);
        btnResetSelection = findViewById(R.id.btnResetSelection);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_COLUMN_COUNT);
        recyclerFusionIngredients.setLayoutManager(layoutManager);

        recyclerFusionIngredients.setHasFixedSize(false);
        recyclerFusionIngredients.setNestedScrollingEnabled(true);
        recyclerFusionIngredients.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        adapter = new FusionIngredientAdapter(
                this,
                ingredientItems,
                new FusionIngredientAdapter.OnIngredientClickListener() {
                    @Override
                    public void onIngredientClick(@NonNull IngredientCharacter character) {
                        if (isFusing) return;
                        handleIngredientSelect(character);
                    }
                }
        );

        recyclerFusionIngredients.setAdapter(adapter);
    }

    private void bindEvents() {
        if (layoutRecipeEntry != null) {
            layoutRecipeEntry.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                Intent intent = new Intent(FusionActivity.this, FusionRecipeBookActivity.class);
                startActivity(intent);
            });
        }

        btnFusionAction.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            tryFusion();
        });

        btnResetSelection.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (isFusing) return;

            clearSelection();
            refreshUi();
        });
    }

    private void refreshUi() {
        ensureEligibleIngredientsMaterializedFromCatalog();
        refreshIngredientList();
        refreshSelectionUi();
        logRecyclerState("refreshUi");
    }

    private void ensureEligibleIngredientsMaterializedFromCatalog() {
        List<CharacterCollectionItem> allItems = CollectionCatalog.getAllItems(this);
        if (allItems == null || allItems.isEmpty()) return;

        for (CharacterCollectionItem item : allItems) {
            if (item == null) continue;
            if (item.getCategory() != CharacterCollectionCategory.INGREDIENT) continue;

            IngredientCharacter ingredient = parseIngredient(item.getKey());
            if (ingredient == null) continue;

            int currentEligibleInstances = getEligibleInstanceCountOnly(ingredient);
            if (currentEligibleInstances > 0) continue;

            if (!isCatalogItemEligible(item)) continue;

            materializeIngredientIfNeeded(ingredient);
        }
    }

    private void refreshIngredientList() {
        ingredientItems.clear();

        List<CharacterCollectionItem> allItems = CollectionCatalog.getAllItems(this);
        if (allItems != null) {
            for (CharacterCollectionItem item : allItems) {
                if (item == null) continue;
                if (item.getCategory() != CharacterCollectionCategory.INGREDIENT) continue;

                IngredientCharacter ingredient = parseIngredient(item.getKey());
                if (ingredient == null) continue;

                if (isCatalogItemEligible(item) || getEligibleIngredientCount(ingredient) > 0) {
                    ingredientItems.add(ingredient);
                }
            }
        }

        adapter.notifyDataSetChanged();

        Log.d(TAG, "ingredientItems.size=" + ingredientItems.size());

        for (IngredientCharacter item : ingredientItems) {
            Log.d(TAG, "visibleItem=" + item.name() + ", level=" + formatEligibleLevelText(item));
        }

        if (ingredientItems.isEmpty()) {
            recyclerFusionIngredients.setVisibility(View.GONE);

            if (tvFusionIngredientEmpty != null) {
                tvFusionIngredientEmpty.setVisibility(View.VISIBLE);
            }

        } else {
            recyclerFusionIngredients.setVisibility(View.VISIBLE);

            if (tvFusionIngredientEmpty != null) {
                tvFusionIngredientEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void handleIngredientSelect(@NonNull IngredientCharacter character) {
        if (selectedIngredient1 == null) {
            selectedIngredient1 = character;

        } else if (selectedIngredient2 == null) {
            selectedIngredient2 = character;

        } else {
            selectedIngredient1 = character;
            selectedIngredient2 = null;
        }

        selectedResultFood = FusionRecipe.getResult(selectedIngredient1, selectedIngredient2);
        refreshUi();
    }

    private void refreshSelectionUi() {
        updateSlot(ivSlot1, tvSlot1Name, selectedIngredient1, "재료 1");
        updateSlot(ivSlot2, tvSlot2Name, selectedIngredient2, "재료 2");

        selectedResultFood = FusionRecipe.getResult(selectedIngredient1, selectedIngredient2);

        if (selectedIngredient1 == null && selectedIngredient2 == null) {
            tvFusionGuide.setText("MAX 재료 2개를 선택하면 합성할 수 있어.");
            tvFusionResult.setText("결과 음식: -");
            btnFusionAction.setEnabled(false);
            btnFusionAction.setAlpha(0.55f);
            btnResetSelection.setEnabled(true);
            btnResetSelection.setAlpha(1f);
            return;
        }

        if (selectedIngredient1 != null && selectedIngredient2 == null) {
            tvFusionGuide.setText("두 번째 MAX 재료를 선택해줘.");
            tvFusionResult.setText("결과 음식: -");
            btnFusionAction.setEnabled(false);
            btnFusionAction.setAlpha(0.55f);
            btnResetSelection.setEnabled(true);
            btnResetSelection.setAlpha(1f);
            return;
        }

        if (selectedIngredient1 == null || selectedIngredient2 == null) {
            tvFusionGuide.setText("MAX 재료 2개를 선택해줘.");
            tvFusionResult.setText("결과 음식: -");
            btnFusionAction.setEnabled(false);
            btnFusionAction.setAlpha(0.55f);
            btnResetSelection.setEnabled(true);
            btnResetSelection.setAlpha(1f);
            return;
        }

        if (!isIngredientEligible(selectedIngredient1) || !isIngredientEligible(selectedIngredient2)) {
            tvFusionGuide.setText("합성은 MAX 재료만 가능해.");
            tvFusionResult.setText("결과 음식: 없음");
            btnFusionAction.setEnabled(false);
            btnFusionAction.setAlpha(0.55f);
            btnResetSelection.setEnabled(true);
            btnResetSelection.setAlpha(1f);
            return;
        }

        if (selectedResultFood != null) {
            tvFusionGuide.setText(
                    selectedIngredient1.getDisplayName() + " + "
                            + selectedIngredient2.getDisplayName()
                            + " 조합으로 합성할 수 있어."
            );

            tvFusionResult.setText("결과 음식: " + selectedResultFood.getDisplayName());
            btnFusionAction.setEnabled(canFuseNow() && !isFusing);
            btnFusionAction.setAlpha((canFuseNow() && !isFusing) ? 1f : 0.55f);
            btnResetSelection.setEnabled(!isFusing);
            btnResetSelection.setAlpha(!isFusing ? 1f : 0.55f);

        } else {
            tvFusionGuide.setText("이 조합으로는 아직 만들 수 있는 요리가 없어.");
            tvFusionResult.setText("결과 음식: 없음");
            btnFusionAction.setEnabled(false);
            btnFusionAction.setAlpha(0.55f);
            btnResetSelection.setEnabled(true);
            btnResetSelection.setAlpha(1f);
        }
    }

    public boolean isIngredientEligible(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return false;
        return getEligibleIngredientCount(ingredient) > 0;
    }

    public int getEligibleIngredientCount(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return 0;

        int count = getEligibleInstanceCountOnly(ingredient);
        if (count > 0) return count;

        CharacterCollectionItem item = CollectionCatalog.findByKey(this, ingredient.name());
        if (item != null && isCatalogItemEligible(item)) {
            return 1;
        }

        if (isEligibleFromMainPet(ingredient)) {
            return 1;
        }

        return 0;
    }

    private int getEligibleInstanceCountOnly(@NonNull IngredientCharacter ingredient) {
        int count = 0;

        List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(this, ingredient.name());
        if (instanceKeys != null) {
            for (String key : instanceKeys) {
                int lv = CollectionPetStatePrefs.getLevel(this, key);
                if (lv >= REQUIRED_LEVEL) {
                    count++;
                }
            }
        }

        return count;
    }

    private boolean isCatalogItemEligible(@Nullable CharacterCollectionItem item) {
        if (item == null) return false;
        if (!item.isUnlocked()) return false;
        if (item.getCategory() != CharacterCollectionCategory.INGREDIENT) return false;

        int level = parseLevelText(item.getLevelText());
        return level >= REQUIRED_LEVEL;
    }

    private int parseLevelText(@Nullable String levelText) {
        if (levelText == null) return 0;

        String t = levelText.trim();
        if (t.isEmpty()) return 0;

        if ("MAX".equalsIgnoreCase(t) || t.contains("MAX")) {
            return PetPrefs.MAX_LEVEL;
        }

        if (t.startsWith("Lv")) {
            try {
                return Integer.parseInt(t.substring(2).trim());
            } catch (Exception ignored) {
                return 0;
            }
        }

        if (t.startsWith("최고 Lv")) {
            try {
                return Integer.parseInt(t.substring("최고 Lv".length()).trim());
            } catch (Exception ignored) {
                return 0;
            }
        }

        return 0;
    }

    @Nullable
    private IngredientCharacter parseIngredient(@Nullable String key) {
        if (key == null || key.trim().isEmpty()) return null;

        try {
            return IngredientCharacter.valueOf(key);
        } catch (Exception ignored) {
        }

        for (IngredientCharacter c : IngredientCharacter.values()) {
            if (key.equals(c.getId())) {
                return c;
            }
        }

        return null;
    }

    private boolean isEligibleFromMainPet(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return false;

        IngredientCharacter selectedMainIngredient = PetPrefs.getSelectedIngredient(this);

        return selectedMainIngredient == ingredient
                && PetPrefs.getStage(this) == PetPrefs.STAGE_INGREDIENT
                && PetPrefs.getLevel(this) >= REQUIRED_LEVEL;
    }

    private void materializeIngredientIfNeeded(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return;
        if (getEligibleInstanceCountOnly(ingredient) > 0) return;

        String newInstanceKey = CollectionInventoryPrefs.addIngredientInstance(this, ingredient.name());
        if (newInstanceKey == null) return;

        String nameKey = ingredient.name();
        String idKey = ingredient.getId();

        boolean hasNameState = CollectionPetStatePrefs.hasState(this, nameKey);
        boolean hasIdState = CollectionPetStatePrefs.hasState(this, idKey);

        if (hasNameState && hasIdState) {
            int nameLevel = CollectionPetStatePrefs.getLevel(this, nameKey);
            int idLevel = CollectionPetStatePrefs.getLevel(this, idKey);

            if (nameLevel >= idLevel) {
                CollectionPetStatePrefs.copyState(this, nameKey, newInstanceKey);
            } else {
                CollectionPetStatePrefs.copyState(this, idKey, newInstanceKey);
            }

        } else if (hasNameState) {
            CollectionPetStatePrefs.copyState(this, nameKey, newInstanceKey);

        } else if (hasIdState) {
            CollectionPetStatePrefs.copyState(this, idKey, newInstanceKey);

        } else if (isEligibleFromMainPet(ingredient)) {
            CollectionPetStatePrefs.setLevel(this, newInstanceKey, PetPrefs.getLevel(this));
            CollectionPetStatePrefs.setHunger(this, newInstanceKey, PetPrefs.getHunger(this));
            CollectionPetStatePrefs.setExpPercent(
                    this,
                    newInstanceKey,
                    PetPrefs.getCurrentLevelExpPercent(this)
            );

        } else {
            CollectionPetStatePrefs.setLevel(this, newInstanceKey, PetPrefs.MAX_LEVEL);
            CollectionPetStatePrefs.setExpPercent(this, newInstanceKey, 100);
        }

        PetPrefs.unlockCharacter(this, ingredient);
    }

    public int getHighestEligibleIngredientLevel(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return 0;

        int highest = 0;

        List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(this, ingredient.name());
        if (instanceKeys != null) {
            for (String key : instanceKeys) {
                int lv = CollectionPetStatePrefs.getLevel(this, key);
                if (lv >= REQUIRED_LEVEL) {
                    highest = Math.max(highest, lv);
                }
            }
        }

        CharacterCollectionItem item = CollectionCatalog.findByKey(this, ingredient.name());
        if (item != null) {
            highest = Math.max(highest, parseLevelText(item.getLevelText()));
        }

        if (isEligibleFromMainPet(ingredient)) {
            highest = Math.max(highest, PetPrefs.getLevel(this));
        }

        return highest;
    }

    public String formatEligibleLevelText(@Nullable IngredientCharacter ingredient) {
        int highest = getHighestEligibleIngredientLevel(ingredient);
        if (highest <= 0) return "Lv?";
        return highest >= PetPrefs.MAX_LEVEL ? "MAX" : "Lv" + highest;
    }

    private boolean canFuseNow() {
        if (selectedIngredient1 == null || selectedIngredient2 == null) return false;
        if (selectedResultFood == null) return false;
        if (!isIngredientEligible(selectedIngredient1) || !isIngredientEligible(selectedIngredient2)) return false;

        if (selectedIngredient1 == selectedIngredient2) {
            return getEligibleIngredientCount(selectedIngredient1) >= 2;
        }

        return getEligibleIngredientCount(selectedIngredient1) >= 1
                && getEligibleIngredientCount(selectedIngredient2) >= 1;
    }

    private void tryFusion() {
        if (isFusing) return;

        if (selectedIngredient1 == null || selectedIngredient2 == null) {
            AppToast.show(this, "재료 2개를 먼저 선택해줘.");
            return;
        }

        if (!isIngredientEligible(selectedIngredient1) || !isIngredientEligible(selectedIngredient2)) {
            AppToast.show(this, "MAX 재료만 합성할 수 있어.");
            return;
        }

        if (selectedResultFood == null) {
            AppToast.show(this, "이 조합은 아직 레시피가 없어.");
            return;
        }

        if (!canFuseNow()) {
            AppToast.show(this, "합성 가능한 재료 개수가 부족해.");
            return;
        }

        final IngredientCharacter ingredient1 = selectedIngredient1;
        final IngredientCharacter ingredient2 = selectedIngredient2;
        final FusionFood resultFood = selectedResultFood;

        isFusing = true;

        btnFusionAction.setEnabled(false);
        btnFusionAction.setAlpha(0.55f);
        btnResetSelection.setEnabled(false);
        btnResetSelection.setAlpha(0.55f);

        tvFusionGuide.setText("합성 중이야...");
        tvFusionResult.setText("결과 음식: " + resultFood.getDisplayName());

        btnFusionAction.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        materializeIngredientIfNeeded(ingredient1);
        materializeIngredientIfNeeded(ingredient2);

        boolean consumed;

        if (ingredient1 == ingredient2) {
            consumed = PetPrefs.consumeCharacter(this, ingredient1, 2);

        } else {
            boolean consumedFirst = PetPrefs.consumeCharacter(this, ingredient1, 1);
            boolean consumedSecond = PetPrefs.consumeCharacter(this, ingredient2, 1);
            consumed = consumedFirst && consumedSecond;
        }

        if (!consumed) {
            isFusing = false;
            AppToast.show(this, "재료 차감 중 문제가 생겼어.");
            btnResetSelection.setEnabled(true);
            btnResetSelection.setAlpha(1f);
            refreshUi();
            return;
        }

        CollectionInventoryPrefs.addItem(this, resultFood.getCollectionKey());
        CollectionPetStatePrefs.ensureState(this, resultFood.getCollectionKey());
        CollectionDisplayPrefs.saveSelectedItem(this, resultFood.getCollectionKey());

        ChecklistPrefs.addFusionCount(this);

        playFusionSlotFadeOutAndShowDialog(resultFood);
    }

    private void playFusionSlotFadeOutAndShowDialog(@NonNull FusionFood resultFood) {
        ivSlot1.animate().cancel();
        ivSlot2.animate().cancel();

        AnimatorSet ingredientDisappear = new AnimatorSet();

        ingredientDisappear.playTogether(
                ObjectAnimator.ofFloat(ivSlot1, View.SCALE_X, 1f, 0.88f),
                ObjectAnimator.ofFloat(ivSlot1, View.SCALE_Y, 1f, 0.88f),
                ObjectAnimator.ofFloat(ivSlot1, View.ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(ivSlot2, View.SCALE_X, 1f, 0.88f),
                ObjectAnimator.ofFloat(ivSlot2, View.SCALE_Y, 1f, 0.88f),
                ObjectAnimator.ofFloat(ivSlot2, View.ALPHA, 1f, 0f)
        );

        ingredientDisappear.setDuration(220);
        ingredientDisappear.setInterpolator(new AccelerateDecelerateInterpolator());

        ingredientDisappear.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                btnFusionAction.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFinishing() || isDestroyed()) {
                    isFusing = false;
                    return;
                }

                ivSlot1.setAlpha(1f);
                ivSlot2.setAlpha(1f);
                ivSlot1.setScaleX(1f);
                ivSlot1.setScaleY(1f);
                ivSlot2.setScaleX(1f);
                ivSlot2.setScaleY(1f);

                clearSelection();

                tvFusionGuide.setText(resultFood.getDisplayName() + "를 획득했어!");
                tvFusionResult.setText("결과 음식: " + resultFood.getDisplayName());

                refreshIngredientList();
                FusionDebugReporter.print(FusionActivity.this);

                showFusionRewardDialog(resultFood);
            }
        });

        ingredientDisappear.start();
    }

    private void showFusionRewardDialog(@NonNull FusionFood resultFood) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_fusion_reward, null, false);

        View halo = dialogView.findViewById(R.id.viewRewardHalo);
        ImageView ivReward = dialogView.findViewById(R.id.ivFusionRewardCharacter);
        TextView tvTitle = dialogView.findViewById(R.id.tvFusionRewardTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFusionRewardMessage);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnFusionRewardConfirm);

        int resultResId = resolveFusionFoodImageResId(resultFood);

        if (resultResId != 0) {
            ivReward.setImageResource(resultResId);
        }

        tvTitle.setText(resultFood.getDisplayName());
        tvMessage.setText(resultFood.getDisplayName() + "를 얻었습니다!");

        halo.setAlpha(0f);
        halo.setScaleX(0.45f);
        halo.setScaleY(0.45f);

        ivReward.setAlpha(0f);
        ivReward.setScaleX(0.45f);
        ivReward.setScaleY(0.45f);
        ivReward.setTranslationY(18f);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        btnConfirm.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            dialog.dismiss();

            isFusing = false;
            refreshUi();
        });

        dialog.setOnShowListener(d -> {
            AnimatorSet haloReveal = new AnimatorSet();

            haloReveal.playTogether(
                    ObjectAnimator.ofFloat(halo, View.ALPHA, 0f, 0.95f, 0.78f),
                    ObjectAnimator.ofFloat(halo, View.SCALE_X, 0.45f, 1.34f, 1.10f),
                    ObjectAnimator.ofFloat(halo, View.SCALE_Y, 0.45f, 1.34f, 1.10f)
            );

            haloReveal.setDuration(620);
            haloReveal.setInterpolator(new OvershootInterpolator(1.2f));

            AnimatorSet rewardPop = new AnimatorSet();

            rewardPop.playTogether(
                    ObjectAnimator.ofFloat(ivReward, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(ivReward, View.SCALE_X, 0.45f, 1.24f, 0.92f, 1.08f, 1f),
                    ObjectAnimator.ofFloat(ivReward, View.SCALE_Y, 0.45f, 1.24f, 0.92f, 1.08f, 1f),
                    ObjectAnimator.ofFloat(ivReward, View.TRANSLATION_Y, 22f, -10f, 4f, -3f, 0f)
            );

            rewardPop.setDuration(820);
            rewardPop.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet all = new AnimatorSet();
            all.playTogether(haloReveal, rewardPop);
            all.start();
        });

        dialog.show();
    }

    private void clearSelection() {
        selectedIngredient1 = null;
        selectedIngredient2 = null;
        selectedResultFood = null;
    }

    private void updateSlot(
            ImageView imageView,
            TextView textView,
            @Nullable IngredientCharacter ingredient,
            String emptyLabel
    ) {
        if (ingredient == null) {
            imageView.setImageDrawable(null);
            textView.setText(emptyLabel);
            return;
        }

        int ingredientResId = resolveFusionDisplayImageResId(ingredient);

        if (ingredientResId != 0) {
            imageView.setImageResource(ingredientResId);
        } else {
            imageView.setImageDrawable(null);
        }

        textView.setText(ingredient.getDisplayName() + "  " + formatEligibleLevelText(ingredient));
    }

    @DrawableRes
    public int resolveFusionDisplayImageResId(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return 0;

        String originalName = ingredient.getDrawableName();
        if (originalName == null || originalName.trim().isEmpty()) return 0;

        String trimmedBodyName = originalName;

        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[]{
                trimmedBodyName + "_catalog",
                originalName + "_catalog",
                originalName
        };

        for (String candidate : candidates) {
            int resId = getResources().getIdentifier(candidate, "drawable", getPackageName());

            if (resId != 0) {
                return resId;
            }
        }

        return 0;
    }

    @DrawableRes
    private int resolveFusionFoodImageResId(@Nullable FusionFood food) {
        if (food == null) return 0;

        String originalName = food.getDrawableName();
        if (originalName == null || originalName.trim().isEmpty()) return 0;

        String trimmedBodyName = originalName;

        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[]{
                trimmedBodyName + "_catalog",
                originalName + "_catalog",
                originalName
        };

        for (String candidate : candidates) {
            int resId = getResources().getIdentifier(candidate, "drawable", getPackageName());

            if (resId != 0) {
                return resId;
            }
        }

        return 0;
    }

    public boolean isSelectedIngredient(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return false;
        return ingredient == selectedIngredient1 || ingredient == selectedIngredient2;
    }

    private void logRecyclerState(@NonNull String from) {
        recyclerFusionIngredients.post(() -> {
            if (recyclerFusionIngredients == null) return;

            RecyclerView.LayoutManager lm = recyclerFusionIngredients.getLayoutManager();
            int childCount = recyclerFusionIngredients.getChildCount();
            int adapterCount = adapter != null ? adapter.getItemCount() : -1;
            int height = recyclerFusionIngredients.getHeight();

            Log.d(
                    TAG,
                    "from=" + from
                            + ", adapterCount=" + adapterCount
                            + ", childCount=" + childCount
                            + ", recyclerHeight=" + height
                            + ", visibility=" + recyclerFusionIngredients.getVisibility()
                            + ", layoutManager=" + (lm == null ? "null" : lm.getClass().getSimpleName())
            );
        });
    }
}