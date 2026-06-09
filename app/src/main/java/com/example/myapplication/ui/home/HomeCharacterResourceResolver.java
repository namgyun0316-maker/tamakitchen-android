package com.namgyun.tamakitchen.ui.home;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;

public class HomeCharacterResourceResolver {

    public interface StateProvider {
        boolean isCollectionMode();
        boolean shouldPrioritizeMainPetHatching();
        String getSelectedCollectionKey();
        int getCurrentStage();
        int getCurrentHunger();
    }

    private static final String FACE_NORMAL = "face_normal";
    private static final String FACE_CLOSED = "face_closed";
    private static final String FACE_HAPPY = "face_happy";
    private static final String FACE_SAD = "face_sad";

    private final Fragment fragment;
    private final StateProvider stateProvider;
    private final int hungryThreshold;

    public HomeCharacterResourceResolver(
            @NonNull Fragment fragment,
            @NonNull StateProvider stateProvider,
            int hungryThreshold
    ) {
        this.fragment = fragment;
        this.stateProvider = stateProvider;
        this.hungryThreshold = hungryThreshold;
    }

    public boolean isCharacterHungryState() {
        if (!fragment.isAdded()) return false;

        int stage = stateProvider.getCurrentStage();
        if (stage == PetPrefs.STAGE_EGG) return false;

        return stateProvider.getCurrentHunger() <= hungryThreshold;
    }

    public int getCurrentCharacterBodyResId() {
        if (!fragment.isAdded()) return 0;

        String drawableName = getCurrentCharacterDrawableBaseName();
        if (drawableName == null || drawableName.isEmpty()) {
            return 0;
        }

        return resolveDrawableRes(drawableName);
    }

    public int getCurrentCharacterFaceResId(
            boolean isShowingCharacterHappyFace,
            boolean shouldRestoreHungryFaceAfterHappy
    ) {
        if (isShowingCharacterHappyFace) {
            int happyResId = getCurrentCharacterHappyFaceResId();
            if (happyResId != 0) {
                return happyResId;
            }
        }

        if (shouldRestoreHungryFaceAfterHappy) {
            int hungryResId = getCurrentCharacterForcedHungryFaceResId();
            if (hungryResId != 0) {
                return hungryResId;
            }
        }

        return getCurrentCharacterNormalOrHungryFaceResId();
    }

    public int getCurrentCharacterBlinkFaceResId(
            boolean isShowingCharacterHappyFace,
            boolean isCharacterHappyBouncing,
            boolean shouldRestoreHungryFaceAfterHappy
    ) {
        if (!fragment.isAdded()) return 0;

        if (isShowingCharacterHappyFace || isCharacterHappyBouncing || shouldRestoreHungryFaceAfterHappy) {
            return 0;
        }

        if (isCharacterHungryState()) {
            return 0;
        }

        return resolveDrawableRes(FACE_CLOSED);
    }

    public int getCurrentCharacterHappyFaceResId() {
        if (!fragment.isAdded()) return 0;
        return resolveDrawableRes(FACE_HAPPY);
    }

    public int getCurrentCharacterForcedHungryFaceResId() {
        if (!fragment.isAdded()) return 0;
        return resolveDrawableRes(FACE_SAD);
    }

    public int getCurrentCharacterNormalOrHungryFaceResId() {
        if (!fragment.isAdded()) return 0;

        if (isCharacterHungryState()) {
            int sadResId = resolveDrawableRes(FACE_SAD);
            if (sadResId != 0) {
                return sadResId;
            }
        }

        return resolveDrawableRes(FACE_NORMAL);
    }

    public String getCurrentCharacterDrawableBaseNameForUi() {
        return getCurrentCharacterDrawableBaseName();
    }

    private CharacterCollectionItem getSelectedCollectionItem() {
        if (!fragment.isAdded()) return null;

        String key = stateProvider.getSelectedCollectionKey();
        if (key == null || key.trim().isEmpty()) return null;

        return CollectionCatalog.findByKey(fragment.requireContext(), key);
    }

    private String getCurrentCharacterDrawableBaseName() {
        if (!fragment.isAdded()) return null;

        if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
            CharacterCollectionItem item = getSelectedCollectionItem();
            if (item == null) return null;
            if (item.getImageResId() == 0) return null;

            return getResourceEntryNameSafely(item.getImageResId());
        }

        IngredientCharacter ingredient = PetPrefs.getSelectedIngredient(fragment.requireContext());
        if (ingredient == null) return null;

        String drawableName = ingredient.getDrawableName();
        if (drawableName == null || drawableName.trim().isEmpty()) return null;

        return drawableName;
    }

    private String getResourceEntryNameSafely(int resId) {
        if (!fragment.isAdded()) return null;
        if (resId == 0) return null;

        try {
            return fragment.getResources().getResourceEntryName(resId);
        } catch (Exception e) {
            return null;
        }
    }

    private int resolveDrawableRes(String drawableName) {
        if (!fragment.isAdded() || drawableName == null || drawableName.isEmpty()) return 0;

        return fragment.getResources().getIdentifier(
                drawableName,
                "drawable",
                fragment.requireContext().getPackageName()
        );
    }
}