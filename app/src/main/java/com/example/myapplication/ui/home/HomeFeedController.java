package com.namgyun.tamakitchen.ui.home;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.home.factory.HomeBubbleTextFactory;

import java.util.List;

public class HomeFeedController {

    private static final int FEED_EXP_REWARD = 3;

    public interface Callback {
        void onPauseIdleAnimation();
        void onResumeIdleAnimationIfPossible();
        void onRefreshUi();
        void onSyncHomeDisplayPriority();
        void onPlayHatchIfNeeded();
        void onShowHappyEggEmotion();
        void onShowCharacterHappyFaceTemporarily();
        void onPlayCharacterHappyBounce();
        void onPlayCharacterLevelUpAnimation();
        void onShowLevelUpDialog(String petName, int oldLevel, int newLevel, int coinReward);
        void onShowBubbleAction(String message);
        void onShowBubbleAction(String message, long durationMs);
    }

    private final Fragment fragment;
    private final HomeStateProvider stateProvider;
    private final HomeFaceController faceController;
    private final Callback callback;
    private final int hungryThreshold;

    public HomeFeedController(
            @NonNull Fragment fragment,
            @NonNull HomeStateProvider stateProvider,
            @NonNull HomeFaceController faceController,
            @NonNull Callback callback,
            int hungryThreshold
    ) {
        this.fragment = fragment;
        this.stateProvider = stateProvider;
        this.faceController = faceController;
        this.callback = callback;
        this.hungryThreshold = hungryThreshold;
    }

    public void handleFeedClick() {
        if (!fragment.isAdded()) return;

        callback.onPauseIdleAnimation();

        if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
            handleFeedForCollectionMode();
            return;
        }

        PetPrefs.applyScheduledHunger(fragment.requireContext());
        int currentHunger = PetPrefs.getHunger(fragment.requireContext());

        if (currentHunger >= 100) {
            callback.onShowBubbleAction(HomeBubbleTextFactory.fullMessage());
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        boolean ok = PetPrefs.consumeFeed(fragment.requireContext(), 1);
        if (!ok) {
            callback.onShowBubbleAction(HomeBubbleTextFactory.noFeedMessage());
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        boolean fed = PetPrefs.feedPlus20(fragment.requireContext());
        if (!fed) {
            PetPrefs.addFeed(fragment.requireContext(), 1);
            callback.onShowBubbleAction(HomeBubbleTextFactory.fullMessage());
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        ChecklistPrefs.markFeedDone(
                fragment.requireContext(),
                PetPrefs.getLevel(fragment.requireContext())
        );
        ChecklistPrefs.addFeedCount(fragment.requireContext());

        int oldLevel = PetPrefs.getLevel(fragment.requireContext());

        boolean leveledUp = PetPrefs.addExpAndMaybeLevelUp(
                fragment.requireContext(),
                FEED_EXP_REWARD
        );

        int newLevel = PetPrefs.getLevel(fragment.requireContext());

        AppToast.show(
                fragment.requireActivity(),
                "EXP " + FEED_EXP_REWARD + " 획득"
        );

        if (leveledUp) {
            int coinReward = PetPrefs.getLevelUpCoinReward(newLevel);
            PetPrefs.addCoins(fragment.requireContext(), coinReward);

            callback.onPlayCharacterLevelUpAnimation();
            callback.onShowLevelUpDialog(
                    resolveCurrentPetName(),
                    oldLevel,
                    newLevel,
                    coinReward
            );
        }

        syncMainPetStateToSelectedInstanceIfNeeded();

        callback.onSyncHomeDisplayPriority();
        callback.onRefreshUi();

        if (PetPrefs.isMainEggHatchPending(fragment.requireContext())) {
            callback.onPlayHatchIfNeeded();
            return;
        }

        int stage = PetPrefs.getStage(fragment.requireContext());

        if (stage == PetPrefs.STAGE_EGG) {
            callback.onRefreshUi();
            callback.onShowHappyEggEmotion();
            return;
        }

        PetPrefs.unlockSelectedCharacterIfNeeded(fragment.requireContext());
        syncMainPetStateToSelectedInstanceIfNeeded();

        int hungerAfterFeed = PetPrefs.getHunger(fragment.requireContext());
        faceController.setShouldRestoreHungryFaceAfterHappy(
                hungerAfterFeed <= hungryThreshold
        );

        callback.onShowCharacterHappyFaceTemporarily();
        callback.onPlayCharacterHappyBounce();
        callback.onRefreshUi();

        if (leveledUp) {
            callback.onShowBubbleAction(
                    HomeBubbleTextFactory.buildLikedFeedLevelUpMessage(fragment.requireContext(), stage),
                    1900L
            );
        } else {
            callback.onShowBubbleAction(
                    HomeBubbleTextFactory.buildLikedFeedMessage(fragment.requireContext(), stage),
                    1900L
            );
        }

        callback.onRefreshUi();
    }

    private void handleFeedForCollectionMode() {
        String selectedKey = stateProvider.getSelectedCollectionKey();
        if (selectedKey == null) {
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        int currentHunger = CollectionPetStatePrefs.getHunger(fragment.requireContext(), selectedKey);

        if (currentHunger >= 100) {
            callback.onShowBubbleAction(HomeBubbleTextFactory.fullMessage());
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        boolean ok = PetPrefs.consumeFeed(fragment.requireContext(), 1);
        if (!ok) {
            callback.onShowBubbleAction(HomeBubbleTextFactory.noFeedMessage());
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        boolean fed = CollectionPetStatePrefs.feedPlus20(fragment.requireContext(), selectedKey);
        if (!fed) {
            PetPrefs.addFeed(fragment.requireContext(), 1);
            callback.onShowBubbleAction(HomeBubbleTextFactory.fullMessage());
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        ChecklistPrefs.markFeedDone(
                fragment.requireContext(),
                CollectionPetStatePrefs.getLevel(fragment.requireContext(), selectedKey)
        );
        ChecklistPrefs.addFeedCount(fragment.requireContext());

        int oldLevel = CollectionPetStatePrefs.getLevel(fragment.requireContext(), selectedKey);

        boolean leveledUp = CollectionPetStatePrefs.addExpAndMaybeLevelUp(
                fragment.requireContext(),
                selectedKey,
                FEED_EXP_REWARD
        );

        int newLevel = CollectionPetStatePrefs.getLevel(fragment.requireContext(), selectedKey);

        AppToast.show(
                fragment.requireActivity(),
                "EXP " + FEED_EXP_REWARD + " 획득"
        );

        if (leveledUp) {
            int coinReward = PetPrefs.getLevelUpCoinReward(newLevel);
            PetPrefs.addCoins(fragment.requireContext(), coinReward);

            callback.onPlayCharacterLevelUpAnimation();
            callback.onShowLevelUpDialog(
                    resolveCollectionPetName(selectedKey),
                    oldLevel,
                    newLevel,
                    coinReward
            );
        }
        callback.onRefreshUi();

        if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)
                && CollectionPetStatePrefs.getLevel(fragment.requireContext(), selectedKey) >= PetPrefs.HATCH_LEVEL) {
            callback.onPlayHatchIfNeeded();
            return;
        }

        int stage = stateProvider.getCurrentStage();

        if (stage == PetPrefs.STAGE_EGG) {
            callback.onRefreshUi();
            callback.onShowHappyEggEmotion();
            return;
        }

        int hungerAfterFeed = CollectionPetStatePrefs.getHunger(fragment.requireContext(), selectedKey);
        faceController.setShouldRestoreHungryFaceAfterHappy(
                hungerAfterFeed <= hungryThreshold
        );

        callback.onShowCharacterHappyFaceTemporarily();
        callback.onPlayCharacterHappyBounce();
        callback.onRefreshUi();

        if (leveledUp) {
            callback.onShowBubbleAction(
                    HomeBubbleTextFactory.buildLikedFeedLevelUpMessage(fragment.requireContext(), stage),
                    1900L
            );
        } else {
            callback.onShowBubbleAction(
                    HomeBubbleTextFactory.buildLikedFeedMessage(fragment.requireContext(), stage),
                    1900L
            );
        }

        callback.onRefreshUi();
    }

    private String resolveCurrentPetName() {
        if (!fragment.isAdded()) return "펫";

        IngredientCharacter selected = PetPrefs.getSelectedIngredient(fragment.requireContext());
        if (selected != null) return selected.getDisplayName();

        int stage = PetPrefs.getStage(fragment.requireContext());
        if (stage == PetPrefs.STAGE_EGG) return "알";

        return "펫";
    }

    private String resolveCollectionPetName(String selectedKey) {
        if (!fragment.isAdded()) return "펫";

        if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)) {
            return "알";
        }

        IngredientCharacter character = parseIngredientCharacterFromCollectionKey(selectedKey);
        if (character != null) {
            return character.getDisplayName();
        }

        try {
            com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem item =
                    CollectionCatalog.findByKey(fragment.requireContext(), selectedKey);
            if (item != null && item.getDisplayName() != null && !item.getDisplayName().trim().isEmpty()) {
                return item.getDisplayName().trim();
            }
        } catch (Exception ignored) {
        }

        return "펫";
    }

    private IngredientCharacter parseIngredientCharacterFromCollectionKey(String selectedKey) {
        if (selectedKey == null || selectedKey.trim().isEmpty()) return null;

        String baseKey = selectedKey;

        try {
            if (CollectionInventoryPrefs.isIngredientInstanceKey(selectedKey)) {
                baseKey = CollectionInventoryPrefs.getBaseKeyFromInstanceKey(selectedKey);
            }
        } catch (Exception ignored) {
        }

        if (baseKey == null || baseKey.trim().isEmpty()) return null;

        try {
            return IngredientCharacter.valueOf(baseKey);
        } catch (Exception ignored) {
        }

        for (IngredientCharacter value : IngredientCharacter.values()) {
            if (baseKey.equals(value.getId())) {
                return value;
            }
        }

        return null;
    }

    private void syncMainPetStateToSelectedInstanceIfNeeded() {
        if (!fragment.isAdded()) return;
        if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) return;
        if (PetPrefs.getStage(fragment.requireContext()) != PetPrefs.STAGE_INGREDIENT) return;

        IngredientCharacter selected = PetPrefs.getSelectedIngredient(fragment.requireContext());
        if (selected == null) return;

        PetPrefs.unlockCharacter(fragment.requireContext(), selected);

        String targetKey = findOrCreateBestInstanceKey(selected);
        if (targetKey == null) return;

        CollectionPetStatePrefs.setLevel(
                fragment.requireContext(),
                targetKey,
                PetPrefs.getLevel(fragment.requireContext())
        );
        CollectionPetStatePrefs.setHunger(
                fragment.requireContext(),
                targetKey,
                PetPrefs.getHunger(fragment.requireContext())
        );
        CollectionPetStatePrefs.setExpPercent(
                fragment.requireContext(),
                targetKey,
                PetPrefs.getCurrentLevelExpPercent(fragment.requireContext())
        );

        String selectedDisplayKey = CollectionDisplayPrefs.getSelectedItemKey(fragment.requireContext());
        if (selectedDisplayKey != null
                && CollectionInventoryPrefs.isIngredientInstanceKey(selectedDisplayKey)
                && selected.name().equals(CollectionInventoryPrefs.getBaseKeyFromInstanceKey(selectedDisplayKey))) {

            CollectionPetStatePrefs.setLevel(
                    fragment.requireContext(),
                    selectedDisplayKey,
                    PetPrefs.getLevel(fragment.requireContext())
            );
            CollectionPetStatePrefs.setHunger(
                    fragment.requireContext(),
                    selectedDisplayKey,
                    PetPrefs.getHunger(fragment.requireContext())
            );
            CollectionPetStatePrefs.setExpPercent(
                    fragment.requireContext(),
                    selectedDisplayKey,
                    PetPrefs.getCurrentLevelExpPercent(fragment.requireContext())
            );
        }
    }

    private String findOrCreateBestInstanceKey(@NonNull IngredientCharacter character) {
        List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(
                fragment.requireContext(),
                character.name()
        );

        if (instanceKeys == null || instanceKeys.isEmpty()) {
            return CollectionInventoryPrefs.addIngredientInstance(
                    fragment.requireContext(),
                    character.name()
            );
        }

        String bestKey = instanceKeys.get(0);
        int bestLevel = CollectionPetStatePrefs.getLevel(fragment.requireContext(), bestKey);

        for (String key : instanceKeys) {
            int level = CollectionPetStatePrefs.getLevel(fragment.requireContext(), key);
            if (level > bestLevel) {
                bestLevel = level;
                bestKey = key;
            }
        }

        return bestKey;
    }
}