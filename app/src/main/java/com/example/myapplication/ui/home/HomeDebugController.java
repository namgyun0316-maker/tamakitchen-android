package com.namgyun.tamakitchen.ui.home;

import android.view.HapticFeedbackConstants;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class HomeDebugController {

    public interface Callback {
        void onPauseIdleAnimation();
        void onResumeIdleAnimationIfPossible();
        void onRefreshUi();
        void onSyncHomeDisplayPriority();
        void onPlayHatchIfNeeded();
        void onShowBubbleAction(String message);
        void onPlayLevelUpAnimation();
        boolean isHatchRunning();
    }

    private final Fragment fragment;
    private final HomeStateProvider stateProvider;
    private final Callback callback;

    public HomeDebugController(
            @NonNull Fragment fragment,
            @NonNull HomeStateProvider stateProvider,
            @NonNull Callback callback
    ) {
        this.fragment = fragment;
        this.stateProvider = stateProvider;
        this.callback = callback;
    }

    public void bindButtons(
            MaterialButton btnDebugHungerDown,
            MaterialButton btnDebugFeedPlus,
            MaterialButton btnDebugCoinsPlus,
            MaterialButton btnDebugLevelUp,
            MaterialButton btnDebugLevelDown
    ) {
        bindHungerDown(btnDebugHungerDown);
        bindFeedPlus(btnDebugFeedPlus);
        bindCoinsPlus(btnDebugCoinsPlus);
        bindLevelUp(btnDebugLevelUp);
        bindLevelDown(btnDebugLevelDown);
    }

    private void bindHungerDown(MaterialButton button) {
        if (button == null) return;

        button.setOnClickListener(view -> {
            if (!fragment.isAdded()) return;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
                String key = stateProvider.getSelectedCollectionKey();
                int newHunger = CollectionPetStatePrefs.debugHungerDown(fragment.requireContext(), key, 20);
                callback.onShowBubbleAction("테스트로 포만도를 낮췄다.\n현재 포만도 " + newHunger + "%");
            } else {
                int newHunger = PetPrefs.debugHungerDown(fragment.requireContext(), 20);
                syncMainPetStateToSelectedInstanceIfNeeded();
                callback.onShowBubbleAction("테스트로 포만도를 낮췄다.\n현재 포만도 " + newHunger + "%");
            }

            callback.onSyncHomeDisplayPriority();
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
        });
    }

    private void bindFeedPlus(MaterialButton button) {
        if (button == null) return;

        button.setOnClickListener(view -> {
            if (!fragment.isAdded()) return;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            int newFeed = PetPrefs.debugFeedPlus(fragment.requireContext(), 10);
            callback.onShowBubbleAction("테스트로 먹이를 추가했다.\n현재 먹이 " + newFeed + "개");
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
        });
    }

    private void bindCoinsPlus(MaterialButton button) {
        if (button == null) return;

        button.setOnClickListener(view -> {
            if (!fragment.isAdded()) return;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            PetPrefs.addCoins(fragment.requireContext(), 1000);
            int coins = PetPrefs.getCoins(fragment.requireContext());

            callback.onShowBubbleAction("테스트로 코인을 추가했다.\n현재 코인 " + coins);
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
        });
    }

    private void bindLevelUp(MaterialButton button) {
        if (button == null) return;

        button.setOnClickListener(view -> {
            if (!fragment.isAdded()) return;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            if (callback.isHatchRunning()) return;

            callback.onPauseIdleAnimation();

            int newLevel;

            if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
                String key = stateProvider.getSelectedCollectionKey();
                newLevel = CollectionPetStatePrefs.debugLevelUp(fragment.requireContext(), key);

                callback.onRefreshUi();

                if (CollectionCatalog.KEY_EGG_BASIC.equals(key) && newLevel >= PetPrefs.HATCH_LEVEL) {
                    callback.onPlayLevelUpAnimation();
                    callback.onPlayHatchIfNeeded();
                    return;
                }
            } else {
                newLevel = PetPrefs.debugLevelUp(fragment.requireContext());
                syncMainPetStateToSelectedInstanceIfNeeded();

                callback.onSyncHomeDisplayPriority();
                callback.onRefreshUi();

                if (PetPrefs.shouldPlayHatchAnimation(fragment.requireContext())) {
                    callback.onPlayLevelUpAnimation();
                    callback.onPlayHatchIfNeeded();
                    return;
                }

                PetPrefs.unlockSelectedCharacterIfNeeded(fragment.requireContext());
                syncMainPetStateToSelectedInstanceIfNeeded();
            }

            if (newLevel >= PetPrefs.HATCH_LEVEL
                    && (!stateProvider.isCollectionMode()
                    || CollectionCatalog.KEY_EGG_BASIC.equals(stateProvider.getSelectedCollectionKey()))) {
                callback.onShowBubbleAction("테스트로 레벨을 올렸어.\n현재 MAX");
            } else {
                callback.onShowBubbleAction("테스트로 레벨을 올렸어.\n현재 Lv" + newLevel);
            }

            callback.onPlayLevelUpAnimation();
            callback.onRefreshUi();
        });
    }

    private void bindLevelDown(MaterialButton button) {
        if (button == null) return;

        button.setOnClickListener(view -> {
            if (!fragment.isAdded()) return;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            if (callback.isHatchRunning()) return;

            int newLevel;

            if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
                String key = stateProvider.getSelectedCollectionKey();
                newLevel = CollectionPetStatePrefs.debugLevelDown(fragment.requireContext(), key);
            } else {
                newLevel = PetPrefs.debugLevelDown(fragment.requireContext());
                syncMainPetStateToSelectedInstanceIfNeeded();
            }

            callback.onSyncHomeDisplayPriority();
            callback.onShowBubbleAction("테스트로 레벨을 내렸어.\n현재 Lv" + newLevel);
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
        });
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