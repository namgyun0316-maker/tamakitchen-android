package com.namgyun.tamakitchen.ui.home;

import android.animation.AnimatorSet;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;

public class HomeFaceController {

    public interface Callback {
        void onPauseIdleAnimation();
        void onResumeIdleAnimationIfPossible();
        void onRefreshUi();
    }

    private final Fragment fragment;
    private final Handler handler;
    private final View mascotLayer;
    private final ImageView ivFridgeMascot;
    private final ImageView ivMascotFace;
    private final HomeStateProvider stateProvider;
    private final HomeCharacterResourceResolver characterResolver;
    private final Callback callback;
    private final long faceTransitionDurationMs;
    private final long happyFaceDurationMs;

    private boolean isShowingCharacterHappyFace = false;
    private boolean isCharacterHappyBouncing = false;
    private boolean shouldRestoreHungryFaceAfterHappy = false;

    private AnimatorSet characterFaceAnimator;
    private AnimatorSet characterHappyBounceAnimator;
    private Runnable characterHappyFaceResetRunnable;

    public HomeFaceController(
            @NonNull Fragment fragment,
            @NonNull Handler handler,
            @NonNull View mascotLayer,
            @NonNull ImageView ivFridgeMascot,
            @NonNull ImageView ivMascotFace,
            @NonNull HomeStateProvider stateProvider,
            @NonNull HomeCharacterResourceResolver characterResolver,
            @NonNull Callback callback,
            long faceTransitionDurationMs,
            long happyFaceDurationMs
    ) {
        this.fragment = fragment;
        this.handler = handler;
        this.mascotLayer = mascotLayer;
        this.ivFridgeMascot = ivFridgeMascot;
        this.ivMascotFace = ivMascotFace;
        this.stateProvider = stateProvider;
        this.characterResolver = characterResolver;
        this.callback = callback;
        this.faceTransitionDurationMs = faceTransitionDurationMs;
        this.happyFaceDurationMs = happyFaceDurationMs;
    }

    public boolean isShowingCharacterHappyFace() {
        return isShowingCharacterHappyFace;
    }

    public boolean isCharacterHappyBouncing() {
        return isCharacterHappyBouncing;
    }

    public boolean shouldRestoreHungryFaceAfterHappy() {
        return shouldRestoreHungryFaceAfterHappy;
    }

    public void setShouldRestoreHungryFaceAfterHappy(boolean value) {
        shouldRestoreHungryFaceAfterHappy = value;
    }

    public void showCharacterHappyFaceTemporarily() {
        if (!fragment.isAdded()) return;
        if (ivMascotFace == null) return;
        if (isEggFaceBlocked()) {
            forceHideFace();
            return;
        }

        if (characterHappyFaceResetRunnable != null) {
            handler.removeCallbacks(characterHappyFaceResetRunnable);
            characterHappyFaceResetRunnable = null;
        }

        isShowingCharacterHappyFace = true;
        callback.onPauseIdleAnimation();

        int happyResId = characterResolver.getCurrentCharacterHappyFaceResId();
        if (happyResId == 0) {
            isShowingCharacterHappyFace = false;
            callback.onResumeIdleAnimationIfPossible();
            return;
        }

        swapCharacterFace(happyResId);

        characterHappyFaceResetRunnable = () -> {
            if (!fragment.isAdded()) return;

            if (isEggFaceBlocked()) {
                isShowingCharacterHappyFace = false;
                shouldRestoreHungryFaceAfterHappy = false;
                forceHideFace();

                if (!isCharacterHappyBouncing) {
                    callback.onResumeIdleAnimationIfPossible();
                }
                characterHappyFaceResetRunnable = null;
                return;
            }

            int fallbackResId;

            if (shouldRestoreHungryFaceAfterHappy) {
                fallbackResId = characterResolver.getCurrentCharacterForcedHungryFaceResId();
                if (fallbackResId == 0) {
                    fallbackResId = characterResolver.getCurrentCharacterNormalOrHungryFaceResId();
                }
            } else {
                fallbackResId = characterResolver.getCurrentCharacterNormalOrHungryFaceResId();
            }

            isShowingCharacterHappyFace = false;
            shouldRestoreHungryFaceAfterHappy = false;

            if (fallbackResId != 0) {
                swapCharacterFace(fallbackResId);
            } else {
                forceHideFace();
            }

            if (!isCharacterHappyBouncing) {
                callback.onResumeIdleAnimationIfPossible();
            }
            characterHappyFaceResetRunnable = null;
        };

        handler.postDelayed(characterHappyFaceResetRunnable, happyFaceDurationMs);
    }

    public void playCharacterHappyBounce() {
        if (isEggFaceBlocked()) {
            forceHideFace();
            return;
        }

        if (mascotLayer == null) return;
        if (ivFridgeMascot == null) return;
        if (ivFridgeMascot.getVisibility() != View.VISIBLE || ivFridgeMascot.getAlpha() <= 0.5f) return;

        callback.onPauseIdleAnimation();

        if (characterHappyBounceAnimator != null) {
            characterHappyBounceAnimator.cancel();
            characterHappyBounceAnimator = null;
        }

        isCharacterHappyBouncing = true;

        characterHappyBounceAnimator = HomeMascotMotionHelper.createCharacterHappyBounceAnimator(
                mascotLayer,
                cancelled -> {
                    isCharacterHappyBouncing = false;
                    characterHappyBounceAnimator = null;

                    if (!cancelled && fragment.isAdded()) {
                        if (isEggFaceBlocked()) {
                            forceHideFace();
                        }
                        callback.onRefreshUi();
                        callback.onResumeIdleAnimationIfPossible();
                    }
                }
        );
        characterHappyBounceAnimator.start();
    }

    public void clear() {
        if (characterHappyFaceResetRunnable != null) {
            handler.removeCallbacks(characterHappyFaceResetRunnable);
            characterHappyFaceResetRunnable = null;
        }

        if (characterFaceAnimator != null) {
            characterFaceAnimator.cancel();
            characterFaceAnimator = null;
        }

        if (characterHappyBounceAnimator != null) {
            characterHappyBounceAnimator.cancel();
            characterHappyBounceAnimator = null;
        }

        isShowingCharacterHappyFace = false;
        isCharacterHappyBouncing = false;
        shouldRestoreHungryFaceAfterHappy = false;

        forceHideFace();
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

    private void forceHideFace() {
        if (ivMascotFace == null) return;

        if (characterFaceAnimator != null) {
            characterFaceAnimator.cancel();
            characterFaceAnimator = null;
        }

        ivMascotFace.animate().cancel();
        ivMascotFace.clearAnimation();
        ivMascotFace.setImageDrawable(null);
        ivMascotFace.setVisibility(View.INVISIBLE);
        ivMascotFace.setAlpha(1f);
        ivMascotFace.setScaleX(1f);
        ivMascotFace.setScaleY(1f);
        ivMascotFace.setTranslationX(0f);
        ivMascotFace.setTranslationY(0f);
    }

    private void swapCharacterFace(int newResId) {
        if (ivMascotFace == null || newResId == 0) return;
        if (isEggFaceBlocked()) {
            forceHideFace();
            return;
        }

        if (characterFaceAnimator != null) {
            characterFaceAnimator.cancel();
            characterFaceAnimator = null;
        }

        ivMascotFace.setVisibility(View.VISIBLE);

        characterFaceAnimator = HomeMascotMotionHelper.createFaceSwapAnimator(
                ivMascotFace,
                newResId,
                faceTransitionDurationMs
        );
        characterFaceAnimator.start();
    }
}