package com.namgyun.tamakitchen.ui.home;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.home.animator.PetHatchAnimator;

public class HomeDisplayController {

    public interface FaceStateProvider {
        boolean isShowingCharacterHappyFace();
        boolean shouldRestoreHungryFaceAfterHappy();
        boolean isCharacterHappyBouncing();
    }

    private final Fragment fragment;
    private final PetHatchAnimator hatchAnimator;
    private final HomeStateProvider stateProvider;
    private final HomeCharacterResourceResolver characterResolver;
    private final FaceStateProvider faceStateProvider;
    private final ImageView ivMascotFace;

    public HomeDisplayController(
            @NonNull Fragment fragment,
            @NonNull PetHatchAnimator hatchAnimator,
            @NonNull HomeStateProvider stateProvider,
            @NonNull HomeCharacterResourceResolver characterResolver,
            @NonNull FaceStateProvider faceStateProvider,
            @NonNull ImageView ivMascotFace
    ) {
        this.fragment = fragment;
        this.hatchAnimator = hatchAnimator;
        this.stateProvider = stateProvider;
        this.characterResolver = characterResolver;
        this.faceStateProvider = faceStateProvider;
        this.ivMascotFace = ivMascotFace;
    }

    public void updateMascotImageByStage(int stage) {
        if (!fragment.isAdded()) return;
        if (hatchAnimator == null) return;

        if (PetPrefs.shouldPlayHatchAnimation(fragment.requireContext())) {
            hideFace();
            hatchAnimator.showEggOnly();
            return;
        }

        if (showSelectedCollectionMascotIfPossible()) {
            return;
        }

        if (stage == PetPrefs.STAGE_EGG) {
            hideFace();
            hatchAnimator.showEggOnly();
            return;
        }

        if (stage == PetPrefs.STAGE_INGREDIENT || stage == PetPrefs.STAGE_FOOD) {
            int bodyResId = characterResolver.getCurrentCharacterBodyResId();
            int faceResId = characterResolver.getCurrentCharacterFaceResId(
                    faceStateProvider.isShowingCharacterHappyFace(),
                    faceStateProvider.shouldRestoreHungryFaceAfterHappy()
            );

            if (bodyResId != 0) {
                hatchAnimator.showCharacterOnly(bodyResId);
                showFace(faceResId);
            } else {
                hideFace();
                hatchAnimator.showEggOnly();
            }
        }
    }

    public boolean showSelectedCollectionMascotIfPossible() {
        if (!fragment.isAdded()) return false;

        if (stateProvider.shouldPrioritizeMainPetHatching()) {
            return false;
        }

        String selectedKey = stateProvider.getSelectedCollectionKey();
        if (selectedKey == null || selectedKey.trim().isEmpty()) {
            return false;
        }

        CharacterCollectionItem selectedItem = CollectionCatalog.findByKey(fragment.requireContext(), selectedKey);
        if (selectedItem == null) {
            CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());
            return false;
        }

        if (!selectedItem.isUnlocked() || !selectedItem.isAvailableToUse()) {
            CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());
            return false;
        }

        if (selectedItem.getImageResId() == 0) {
            CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());
            return false;
        }

        if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedItem.getKey())) {
            hideFace();
            hatchAnimator.showEggOnly();
        } else {
            int bodyResId = characterResolver.getCurrentCharacterBodyResId();
            int faceResId = characterResolver.getCurrentCharacterFaceResId(
                    faceStateProvider.isShowingCharacterHappyFace(),
                    faceStateProvider.shouldRestoreHungryFaceAfterHappy()
            );

            if (bodyResId != 0) {
                hatchAnimator.showCharacterOnly(bodyResId);
            } else {
                hatchAnimator.showCharacterOnly(selectedItem.getImageResId());
            }

            showFace(faceResId);
        }
        return true;
    }

    public void prepareFaceOnlyForCurrentState() {
        if (!fragment.isAdded()) return;
        if (ivMascotFace == null) return;

        if (isEggFaceBlocked()) {
            hideFace();
            return;
        }

        int faceResId = characterResolver.getCurrentCharacterFaceResId(
                faceStateProvider.isShowingCharacterHappyFace(),
                faceStateProvider.shouldRestoreHungryFaceAfterHappy()
        );

        if (faceResId == 0) {
            hideFace();
            return;
        }

        ivMascotFace.animate().cancel();
        ivMascotFace.clearAnimation();
        ivMascotFace.setImageResource(faceResId);
        ivMascotFace.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivMascotFace.setVisibility(View.VISIBLE);
        ivMascotFace.setAlpha(0f);
        ivMascotFace.setScaleX(1f);
        ivMascotFace.setScaleY(1f);
        ivMascotFace.setTranslationX(0f);
        ivMascotFace.setTranslationY(0f);

        applyFacePlacementForCurrentCharacter();
    }

    private boolean isEggFaceBlocked() {
        if (!fragment.isAdded()) return true;

        if (stateProvider.getCurrentStage() == PetPrefs.STAGE_EGG) {
            return true;
        }

        if (!stateProvider.shouldPrioritizeMainPetHatching()) {
            String selectedKey = stateProvider.getSelectedCollectionKey();
            if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)) {
                return true;
            }
        }

        return false;
    }

    private void showFace(int faceResId) {
        if (ivMascotFace == null) return;

        if (isEggFaceBlocked()) {
            hideFace();
            return;
        }

        if (faceResId == 0) {
            hideFace();
            return;
        }

        ivMascotFace.setVisibility(View.VISIBLE);
        ivMascotFace.setAlpha(1f);
        ivMascotFace.setScaleX(1f);
        ivMascotFace.setScaleY(1f);
        ivMascotFace.setImageResource(faceResId);
        ivMascotFace.setScaleType(ImageView.ScaleType.FIT_CENTER);

        applyFacePlacementForCurrentCharacter();
    }

    private void hideFace() {
        if (ivMascotFace == null) return;

        ivMascotFace.animate().cancel();
        ivMascotFace.clearAnimation();
        ivMascotFace.setImageDrawable(null);
        ivMascotFace.setVisibility(View.INVISIBLE);
        ivMascotFace.setAlpha(1f);
        ivMascotFace.setScaleX(1f);
        ivMascotFace.setScaleY(1f);
        ivMascotFace.setTranslationX(0f);
        ivMascotFace.setTranslationY(0f);

        ViewGroup.LayoutParams params = ivMascotFace.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.gravity = Gravity.CENTER;
            lp.leftMargin = 0;
            lp.topMargin = 0;
            lp.rightMargin = 0;
            lp.bottomMargin = 0;
            ivMascotFace.setLayoutParams(lp);
        }
    }

    private void applyFacePlacementForCurrentCharacter() {
        if (!fragment.isAdded()) return;
        if (ivMascotFace == null) return;
        if (isEggFaceBlocked()) {
            hideFace();
            return;
        }

        String bodyName = characterResolver.getCurrentCharacterDrawableBaseNameForUi();
        FacePlacement placement = getFacePlacement(bodyName);

        ViewGroup.LayoutParams params = ivMascotFace.getLayoutParams();
        if (!(params instanceof FrameLayout.LayoutParams)) return;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
        lp.gravity = Gravity.CENTER;
        lp.width = dpToPxInt(placement.getWidthDp());
        lp.height = dpToPxInt(placement.getHeightDp());

        int offsetX = dpToPxInt(placement.getOffsetXDp());
        int offsetY = dpToPxInt(placement.getOffsetYDp());

        lp.leftMargin = offsetX;
        lp.topMargin = offsetY;
        lp.rightMargin = 0;
        lp.bottomMargin = 0;

        ivMascotFace.setLayoutParams(lp);
        ivMascotFace.requestLayout();
        ivMascotFace.invalidate();
    }

    private FacePlacement getFacePlacement(String bodyName) {
        if (bodyName == null || bodyName.trim().isEmpty()) {
            return FacePlacement.defaultValue();
        }

        switch (bodyName) {
            case "green_grape_body":
                return new FacePlacement(350, 150, 0, -6);

            case "sweet_potato_body":
                return new FacePlacement(350, 150, 0, -10);

            case "strawberry_body":
                return new FacePlacement(400, 200, 0,  0);

            case "banana_body":
                return new FacePlacement(230, 135, 12, -10);

            case "onion_body":
                return new FacePlacement(350, 200, 0, -5);

            case "green_onion_body":
                return new FacePlacement(150, 150, 0, 10);

            case "walnut_body":
                return new FacePlacement(300, 300, 0, -5);

            case "pistachio_body":
                return new FacePlacement(250, 250, 0, -3);

            case "tomato_body":
                return new FacePlacement(300, 300, 0, 4);

            case "blueberry_body":
                return new FacePlacement(400, 400, 0, 0);

            case "strawberry_cake_body":
                return new FacePlacement(250, 250, 0, 7);
            case "sugar_cube_body":
                return new FacePlacement(200, 200, 0, -8);
            case "flour_body":
                return new FacePlacement(250, 250, 0, -4);
            case "pasta_noodle_body":
                return new FacePlacement(350, 350, 0, 12);
            case "sparkling_water_body":
                return new FacePlacement(270, 270, 0, 10);

            case "coffee_bean_body":
                return new FacePlacement(400, 400, 0, -10);
            case "chocolate_body":
                return new FacePlacement(350, 350, 0, 5);
            case "chestnut_body":
                return new FacePlacement(400, 400, 0, 20);
            case "milk_body":
               return new FacePlacement(300, 300, 0, -20);
            case "doojjon_cookie_body":
                return new FacePlacement(250, 250, 0, 5);
            case "chestnut_tiramisu_body":
                return new FacePlacement(350, 200, 0, 29);
            case "strawberry_shake_body":
                return new FacePlacement(250, 250, 0, 30);
            case "green_grape_ade_body":
                return new FacePlacement(250, 250, 0, -10);
            case "potato_body":
                return new FacePlacement(350, 250, 0, 0);
            case "green_tea_body":
                return new FacePlacement(250, 250, 0, 25);
            case "butter_body":
                return new FacePlacement(300, 300, 0, 20);
            case "chocolate_milk_body":
                return new FacePlacement(350, 350, 0, 25);
            case "lemon_body":
                return new FacePlacement(350, 350, 0, 0);
            case "apple_body":
                return new FacePlacement(350, 350, 0, 10);
            case "cookie_body":
                return new FacePlacement(350, 350, 0, 0);
            case "sweet_potato_mattang_body":
                return new FacePlacement(300, 300, 0, 0);
            case "tomato_spaghetti_body":
                return new FacePlacement(270, 270, 0, 25);
            case "hash_brown_body":
                return new FacePlacement(300, 300, 0, -25);
            case "green_onion_pancake_body":
                return new FacePlacement(350, 350, 0, 0);
            case "banana_shake_body":
                return new FacePlacement(300, 300, 0, 22);
            case "strawberry_jam_body":
                return new FacePlacement(400, 400, 0, 33);
            case "walnut_gangjeong_body":
                return new FacePlacement(400, 400, 0, 5);
            case "grape_jam_body":
                return new FacePlacement(400, 400, 0, 33);
            case "roasted_chestnut_body":
                return new FacePlacement(300, 300, 0, 10);
            case "choco_banana_body":
                return new FacePlacement(300, 300, 0, 40);
            case "cafe_latte_body":
                return new FacePlacement(300, 300, 0, 10);
            case "matcha_latte_body":
                return new FacePlacement(400, 400, 0, 30);
            case "lemon_ade_body":
                return new FacePlacement(350, 350, 0, 40);
            case "blueberry_muffin_body":
                return new FacePlacement(400, 400, 0, 40);
            case "apple_jam_body":
                return new FacePlacement(400, 400, 0, 33);
            case "onion_cream_soup_body":
                return new FacePlacement(250, 250, 0, 10);
            case "baguette_body":
                return new FacePlacement(250, 250, 0, 10);
            case "pancake_body":
                return new FacePlacement(400, 400, 0, 35);
            case "strawberry_ade_body":
                return new FacePlacement(350, 350, 0, 40);
            case "brownie_body":
                return new FacePlacement(400, 400, 5, 30);
            default:
                return FacePlacement.defaultValue();
        }
    }

    private int dpToPxInt(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                fragment.requireContext().getResources().getDisplayMetrics()
        );
    }
}