package com.namgyun.tamakitchen.ui.home;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.PetPrefs;

public class HomeUiBinder {

    public interface Callback {
        void onPauseIdleAnimation();
        void onResumeIdleAnimationIfPossible();
        void onPlayHatchIfNeeded();
    }

    private final Fragment fragment;
    private final HomeStateProvider stateProvider;
    private final HomeBubbleController bubbleController;
    private final HomeDisplayController displayController;
    private final HomeFaceController faceController;
    private final HomeHatchController hatchController;
    private final Callback callback;

    private final TextView tvCoins;
    private final TextView tvHungerValue;
    private final TextView tvFeedCount;
    private final TextView tvLevel;
    private final TextView tvExpPercent;
    private final ProgressBar pbHunger;
    private final ProgressBar pbExp;

    public HomeUiBinder(
            @NonNull Fragment fragment,
            @NonNull HomeStateProvider stateProvider,
            @NonNull HomeBubbleController bubbleController,
            @NonNull HomeDisplayController displayController,
            @NonNull HomeFaceController faceController,
            @NonNull HomeHatchController hatchController,
            @NonNull Callback callback,
            @NonNull TextView tvCoins,
            @NonNull TextView tvHungerValue,
            @NonNull TextView tvFeedCount,
            @NonNull TextView tvLevel,
            @NonNull TextView tvExpPercent,
            @NonNull ProgressBar pbHunger,
            @NonNull ProgressBar pbExp
    ) {
        this.fragment = fragment;
        this.stateProvider = stateProvider;
        this.bubbleController = bubbleController;
        this.displayController = displayController;
        this.faceController = faceController;
        this.hatchController = hatchController;
        this.callback = callback;
        this.tvCoins = tvCoins;
        this.tvHungerValue = tvHungerValue;
        this.tvFeedCount = tvFeedCount;
        this.tvLevel = tvLevel;
        this.tvExpPercent = tvExpPercent;
        this.pbHunger = pbHunger;
        this.pbExp = pbExp;
    }

    public void refresh() {
        if (!fragment.isAdded()) return;

        PetPrefs.applyScheduledHunger(fragment.requireContext());
        stateProvider.syncHomeDisplayPriority();

        int coins = stateProvider.getCoins();
        int hunger = stateProvider.getCurrentHunger();
        int feedCount = stateProvider.getFeedCount();
        int stage = stateProvider.getCurrentStage();
        int level = stateProvider.getCurrentLevel();
        int expPercent = stateProvider.getCurrentExpPercent();

        boolean isMainEggHatchPending =
                !stateProvider.isCollectionMode()
                        && PetPrefs.isMainEggHatchPending(fragment.requireContext());

        boolean isSelectedEggHatchPending =
                stateProvider.shouldPlaySelectedEggHatchAnimation();

        boolean shouldShowEggMaxUi = isMainEggHatchPending || isSelectedEggHatchPending;
        boolean isMaxLevel = level >= PetPrefs.MAX_LEVEL;

        tvCoins.setText(String.valueOf(coins));
        tvHungerValue.setText(hunger + "%");
        tvFeedCount.setText(String.valueOf(feedCount));

        if (shouldShowEggMaxUi) {
            tvLevel.setText("MAX");
            tvExpPercent.setText("MAX");
            pbExp.setMax(100);
            pbExp.setProgress(100);
        } else if (isMaxLevel) {
            tvLevel.setText("MAX");
            tvExpPercent.setText("MAX");
            pbExp.setMax(100);
            pbExp.setProgress(100);
        } else {
            tvLevel.setText("Lv" + level);
            tvExpPercent.setText(expPercent + "%");
            pbExp.setMax(100);
            pbExp.setProgress(expPercent);
        }

        pbHunger.setMax(100);
        pbHunger.setProgress(hunger);

        if (!stateProvider.isCollectionMode() && stage != PetPrefs.STAGE_EGG) {
            PetPrefs.unlockSelectedCharacterIfNeeded(fragment.requireContext());
        }

        if (!hatchController.isRunning()
                && !faceController.isCharacterHappyBouncing()
                && !faceController.isShowingCharacterHappyFace()) {
            displayController.updateMascotImageByStage(stage);
        }

        if (!bubbleController.isShowingHappyEmotion()) {
            bubbleController.updateBubbleEmotion(stage, hunger);
        }

        if (hatchController.isRunning()) {
            callback.onPauseIdleAnimation();
            bubbleController.showHatchingMessage();
            return;
        }

        if (isMainEggHatchPending || isSelectedEggHatchPending) {
            callback.onPauseIdleAnimation();
            callback.onPlayHatchIfNeeded();
            return;
        }

        bubbleController.showDefaultStatusIfPossible();

        if (!faceController.isCharacterHappyBouncing()) {
            callback.onResumeIdleAnimationIfPossible();
        }
    }
}