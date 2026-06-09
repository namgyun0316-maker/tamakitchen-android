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
import com.namgyun.tamakitchen.ui.home.animator.PetHatchAnimator;
import com.namgyun.tamakitchen.ui.home.factory.HomeBubbleTextFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeHatchController {

    public interface Callback {
        void onPauseIdleAnimation();
        void onResumeIdleAnimationIfPossible();
        void onRefreshUi();
    }

    private final Fragment fragment;
    private final PetHatchAnimator hatchAnimator;
    private final HomeBubbleController bubbleController;
    private final HomeStateProvider stateProvider;
    private final HomeDisplayController displayController;
    private final Callback callback;

    public HomeHatchController(
            @NonNull Fragment fragment,
            @NonNull PetHatchAnimator hatchAnimator,
            @NonNull HomeBubbleController bubbleController,
            @NonNull HomeStateProvider stateProvider,
            @NonNull HomeDisplayController displayController,
            @NonNull Callback callback
    ) {
        this.fragment = fragment;
        this.hatchAnimator = hatchAnimator;
        this.bubbleController = bubbleController;
        this.stateProvider = stateProvider;
        this.displayController = displayController;
        this.callback = callback;
    }

    public boolean isRunning() {
        return hatchAnimator != null && hatchAnimator.isRunning();
    }

    public void playIfNeeded() {
        if (!fragment.isAdded()) return;
        if (hatchAnimator == null) return;
        if (hatchAnimator.isRunning()) return;

        if (stateProvider.shouldPlaySelectedEggHatchAnimation()) {
            playCollectionEggHatch();
            return;
        }

        if (!PetPrefs.isMainEggHatchPending(fragment.requireContext())) {
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        playMainPetHatch();
    }

    private void playMainPetHatch() {
        callback.onPauseIdleAnimation();
        CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());

        IngredientCharacter picked = pickHatchableIngredient();
        if (picked == null) {
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        PetPrefs.forceSelectIngredient(fragment.requireContext(), picked);

        bubbleController.hideBubbleEmotion();
        bubbleController.showHatchingMessage();

        final Set<String> beforeInstanceKeys = new HashSet<>(
                CollectionInventoryPrefs.getIngredientInstanceKeys(fragment.requireContext(), picked.name())
        );

        hatchAnimator.play(picked, new PetHatchAnimator.Callback() {
            @Override
            public void onHatchStart() {
                callback.onPauseIdleAnimation();
                bubbleController.hideBubbleEmotion();
                bubbleController.showHatchingMessage();
            }

            @Override
            public void onHatchRevealPrepare() {
                if (!fragment.isAdded()) return;

                PetPrefs.applyPostHatchInitialState(fragment.requireContext());
                PetPrefs.obtainCharacter(fragment.requireContext(), picked);

                // 부화 업적 카운트 증가
                ChecklistPrefs.addHatchCount(fragment.requireContext());

                String newInstanceKey = findNewInstanceKey(picked.name(), beforeInstanceKeys);
                if (newInstanceKey != null) {
                    CollectionPetStatePrefs.setLevel(fragment.requireContext(), newInstanceKey, 1);
                    CollectionPetStatePrefs.setHunger(fragment.requireContext(), newInstanceKey, 100);
                    CollectionPetStatePrefs.setExpPercent(fragment.requireContext(), newInstanceKey, 0);
                }

                stateProvider.initPetState();
                stateProvider.syncHomeDisplayPriority();
                displayController.prepareFaceOnlyForCurrentState();
            }

            @Override
            public void onHatchEnd() {
                if (!fragment.isAdded()) return;

                bubbleController.hideBubbleEmotion();
                bubbleController.showBubbleAction(
                        HomeBubbleTextFactory.buildHatchedMessage(picked.getDisplayName()),
                        2600L
                );

                callback.onRefreshUi();
                callback.onResumeIdleAnimationIfPossible();
            }
        });
    }

    private void playCollectionEggHatch() {
        if (!fragment.isAdded()) return;
        if (hatchAnimator.isRunning()) return;

        callback.onPauseIdleAnimation();

        IngredientCharacter picked = pickHatchableIngredient();
        if (picked == null) {
            callback.onRefreshUi();
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        bubbleController.hideBubbleEmotion();
        bubbleController.showHatchingMessage();

        final Set<String> beforeInstanceKeys = new HashSet<>(
                CollectionInventoryPrefs.getIngredientInstanceKeys(fragment.requireContext(), picked.name())
        );

        hatchAnimator.play(picked, new PetHatchAnimator.Callback() {
            @Override
            public void onHatchStart() {
                callback.onPauseIdleAnimation();
                bubbleController.hideBubbleEmotion();
                bubbleController.showHatchingMessage();
            }

            @Override
            public void onHatchRevealPrepare() {
                if (!fragment.isAdded()) return;

                PetPrefs.obtainCharacter(fragment.requireContext(), picked);

                // 부화 업적 카운트 증가
                ChecklistPrefs.addHatchCount(fragment.requireContext());

                String newInstanceKey = findNewInstanceKey(picked.name(), beforeInstanceKeys);
                if (newInstanceKey != null) {
                    CollectionPetStatePrefs.setLevel(fragment.requireContext(), newInstanceKey, 1);
                    CollectionPetStatePrefs.setHunger(fragment.requireContext(), newInstanceKey, 100);
                    CollectionPetStatePrefs.setExpPercent(fragment.requireContext(), newInstanceKey, 0);
                }

                consumeCollectionEggCompletely();

                if (newInstanceKey != null) {
                    CollectionDisplayPrefs.saveSelectedItem(fragment.requireContext(), newInstanceKey);
                    CollectionPetStatePrefs.ensureState(fragment.requireContext(), newInstanceKey);
                } else {
                    CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());
                }

                stateProvider.initPetState();
                stateProvider.syncHomeDisplayPriority();
                stateProvider.ensureSelectedCollectionStateIfNeeded();
                displayController.prepareFaceOnlyForCurrentState();
            }

            @Override
            public void onHatchEnd() {
                if (!fragment.isAdded()) return;

                bubbleController.hideBubbleEmotion();
                bubbleController.showBubbleAction(
                        HomeBubbleTextFactory.buildHatchedMessage(picked.getDisplayName()),
                        2600L
                );

                callback.onRefreshUi();
                callback.onResumeIdleAnimationIfPossible();
            }
        });
    }

    /**
     * 도감 알 부화 후 알 재고/선택/상태를 완전히 정리
     */
    private void consumeCollectionEggCompletely() {
        if (!fragment.isAdded()) return;

        // 1) 실제 알 재고 차감 시도
        PetPrefs.consumeEgg(fragment.requireContext(), 1);

        // 2) 혹시 남아 있을 수 있으므로 강제로 0으로 맞춤
        PetPrefs.setEggCount(fragment.requireContext(), 0);

        // 3) 도감 인벤토리 알 제거
        CollectionInventoryPrefs.removeItem(fragment.requireContext(), CollectionCatalog.KEY_EGG_BASIC);

        // 4) 도감에서 알이 선택된 상태면 해제
        CollectionDisplayPrefs.clearSelectedItemIfMatches(
                fragment.requireContext(),
                CollectionCatalog.KEY_EGG_BASIC
        );

        // 5) 알 상태 초기화
        CollectionPetStatePrefs.setLevel(
                fragment.requireContext(),
                CollectionCatalog.KEY_EGG_BASIC,
                1
        );
        CollectionPetStatePrefs.setHunger(
                fragment.requireContext(),
                CollectionCatalog.KEY_EGG_BASIC,
                100
        );
        CollectionPetStatePrefs.setExpPercent(
                fragment.requireContext(),
                CollectionCatalog.KEY_EGG_BASIC,
                0
        );
    }

    private IngredientCharacter pickHatchableIngredient() {
        if (!fragment.isAdded()) return null;

        List<IngredientCharacter> candidates = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            IngredientCharacter picked = PetPrefs.getRandomAvailableIngredientForHatch(fragment.requireContext());
            if (picked == null) continue;
            if (hasDrawableFor(picked)) {
                candidates.add(picked);
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }

        for (IngredientCharacter character : IngredientCharacter.values()) {
            if (hasDrawableFor(character)) {
                return character;
            }
        }

        return null;
    }

    private boolean hasDrawableFor(@NonNull IngredientCharacter character) {
        if (!fragment.isAdded()) return false;
        int resId = fragment.getResources().getIdentifier(
                character.getDrawableName(),
                "drawable",
                fragment.requireContext().getPackageName()
        );
        return resId != 0;
    }

    private String findNewInstanceKey(String baseKey, Set<String> beforeKeys) {
        if (!fragment.isAdded()) return null;

        List<String> afterKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(
                fragment.requireContext(),
                baseKey
        );

        for (String key : afterKeys) {
            if (!beforeKeys.contains(key)) {
                return key;
            }
        }

        if (!afterKeys.isEmpty()) {
            return getLatestInstanceKey(afterKeys);
        }

        return null;
    }

    private String getLatestInstanceKey(List<String> keys) {
        if (keys == null || keys.isEmpty()) return null;

        List<String> copied = new ArrayList<>(keys);
        String latest = copied.get(0);

        for (String key : copied) {
            if (key != null && latest != null && key.compareTo(latest) > 0) {
                latest = key;
            }
        }

        return latest;
    }
}