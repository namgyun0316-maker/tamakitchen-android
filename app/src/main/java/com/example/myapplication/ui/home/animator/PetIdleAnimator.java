package com.namgyun.tamakitchen.ui.home.animator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.Random;

public class PetIdleAnimator {

    private static final long EGG_IDLE_DURATION = 2200L;
    private static final long CHARACTER_IDLE_DURATION = 2400L;

    private static final long BLINK_CLOSE_DURATION = 180L;
    private static final long BLINK_HOLD_DURATION = 140L;
    private static final long BLINK_DOUBLE_GAP_DURATION = 160L;

    private final ImageView eggView;
    private final ImageView characterBodyView;
    private final ImageView characterFaceView;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private AnimatorSet eggIdleSet;
    private AnimatorSet characterIdleSet;

    private boolean stopped = true;
    private boolean eggMode = false;
    private boolean characterMode = false;
    private boolean blinking = false;

    private int currentCharacterBodyResId = 0;
    private int currentCharacterOpenFaceResId = 0;
    private int currentCharacterBlinkFaceResId = 0;

    private float baseBodyTranslationX = 0f;
    private float baseBodyTranslationY = 0f;
    private float baseFaceTranslationX = 0f;
    private float baseFaceTranslationY = 0f;

    private Runnable blinkRunnable;

    public PetIdleAnimator(
            @Nullable ImageView eggView,
            @Nullable ImageView characterBodyView,
            @Nullable ImageView characterFaceView
    ) {
        this.eggView = eggView;
        this.characterBodyView = characterBodyView;
        this.characterFaceView = characterFaceView;
    }

    public void startForEgg() {
        stopAll();

        stopped = false;
        eggMode = true;
        characterMode = false;
        blinking = false;

        clearCharacterFaceState();
        forceHideCharacterFace();

        if (eggView == null) return;
        if (eggView.getVisibility() != android.view.View.VISIBLE) return;

        startEggIdleLoop();
    }

    public void startForCharacter(int bodyResId, int openFaceResId, int blinkFaceResId) {
        stopAll();

        stopped = false;
        eggMode = false;
        characterMode = true;
        blinking = false;

        currentCharacterBodyResId = bodyResId;
        currentCharacterOpenFaceResId = openFaceResId;
        currentCharacterBlinkFaceResId = blinkFaceResId;

        if (characterBodyView == null) return;
        if (characterBodyView.getVisibility() != android.view.View.VISIBLE) return;

        if (currentCharacterBodyResId != 0) {
            characterBodyView.setImageResource(currentCharacterBodyResId);
        }

        if (characterFaceView != null) {
            if (currentCharacterOpenFaceResId != 0) {
                characterFaceView.setVisibility(android.view.View.VISIBLE);
                characterFaceView.setAlpha(1f);
                characterFaceView.setScaleX(1f);
                characterFaceView.setScaleY(1f);
                characterFaceView.setImageResource(currentCharacterOpenFaceResId);
            } else {
                forceHideCharacterFace();
            }
        }

        captureCurrentBaseTranslations();
        startCharacterIdleLoop();

        if (currentCharacterOpenFaceResId != 0 && currentCharacterBlinkFaceResId != 0) {
            scheduleNextBlink();
        }
    }

    public void stopAll() {
        stopped = true;
        eggMode = false;
        characterMode = false;
        blinking = false;

        cancelAnimator(eggIdleSet);
        cancelAnimator(characterIdleSet);

        eggIdleSet = null;
        characterIdleSet = null;

        if (blinkRunnable != null) {
            handler.removeCallbacks(blinkRunnable);
            blinkRunnable = null;
        }

        handler.removeCallbacksAndMessages(null);

        resetEggBaseState();
        resetCharacterBaseState();

        if (characterBodyView != null && currentCharacterBodyResId != 0) {
            characterBodyView.setImageResource(currentCharacterBodyResId);
        }

        if (characterFaceView != null) {
            if (currentCharacterOpenFaceResId != 0) {
                characterFaceView.setVisibility(android.view.View.VISIBLE);
                characterFaceView.setAlpha(1f);
                characterFaceView.setScaleX(1f);
                characterFaceView.setScaleY(1f);
                characterFaceView.setImageResource(currentCharacterOpenFaceResId);
            } else {
                forceHideCharacterFace();
            }
        }
    }

    public boolean isBlinking() {
        return blinking;
    }

    public void playBlinkNow() {
        if (stopped || !characterMode) return;
        if (characterFaceView == null) return;
        if (currentCharacterOpenFaceResId == 0 || currentCharacterBlinkFaceResId == 0) return;
        if (characterFaceView.getVisibility() != android.view.View.VISIBLE) return;

        if (blinkRunnable != null) {
            handler.removeCallbacks(blinkRunnable);
            blinkRunnable = null;
        }

        playBlinkSequence();
    }

    private void captureCurrentBaseTranslations() {
        if (characterBodyView != null) {
            baseBodyTranslationX = characterBodyView.getTranslationX();
            baseBodyTranslationY = characterBodyView.getTranslationY();
        }

        if (characterFaceView != null) {
            baseFaceTranslationX = characterFaceView.getTranslationX();
            baseFaceTranslationY = characterFaceView.getTranslationY();
        }
    }

    private void startEggIdleLoop() {
        if (stopped || !eggMode || eggView == null) return;
        if (eggView.getVisibility() != android.view.View.VISIBLE) return;

        forceHideCharacterFace();

        cancelAnimator(eggIdleSet);
        resetEggBaseState();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(eggView, android.view.View.SCALE_X, 1f, 1.018f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(eggView, android.view.View.SCALE_Y, 1f, 1.018f, 1f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(eggView, android.view.View.TRANSLATION_Y, 0f, -4f, 0f);

        scaleX.setDuration(EGG_IDLE_DURATION);
        scaleY.setDuration(EGG_IDLE_DURATION);
        translateY.setDuration(EGG_IDLE_DURATION);

        eggIdleSet = new AnimatorSet();
        eggIdleSet.playTogether(scaleX, scaleY, translateY);
        eggIdleSet.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled && !stopped && eggMode) {
                    startEggIdleLoop();
                }
            }
        });
        eggIdleSet.start();
    }

    private void startCharacterIdleLoop() {
        if (stopped || !characterMode || characterBodyView == null) return;
        if (characterBodyView.getVisibility() != android.view.View.VISIBLE) return;

        cancelAnimator(characterIdleSet);
        captureCurrentBaseTranslations();
        resetCharacterBaseState();

        ObjectAnimator bodyScaleX = ObjectAnimator.ofFloat(characterBodyView, android.view.View.SCALE_X, 1f, 1.015f, 1f);
        ObjectAnimator bodyScaleY = ObjectAnimator.ofFloat(characterBodyView, android.view.View.SCALE_Y, 1f, 1.015f, 1f);
        ObjectAnimator bodyTranslateY = ObjectAnimator.ofFloat(
                characterBodyView,
                android.view.View.TRANSLATION_Y,
                baseBodyTranslationY,
                baseBodyTranslationY - 3f,
                baseBodyTranslationY
        );

        bodyScaleX.setDuration(CHARACTER_IDLE_DURATION);
        bodyScaleY.setDuration(CHARACTER_IDLE_DURATION);
        bodyTranslateY.setDuration(CHARACTER_IDLE_DURATION);

        characterIdleSet = new AnimatorSet();

        if (characterFaceView != null && characterFaceView.getVisibility() == android.view.View.VISIBLE) {
            ObjectAnimator faceScaleX = ObjectAnimator.ofFloat(characterFaceView, android.view.View.SCALE_X, 1f, 1.015f, 1f);
            ObjectAnimator faceScaleY = ObjectAnimator.ofFloat(characterFaceView, android.view.View.SCALE_Y, 1f, 1.015f, 1f);
            ObjectAnimator faceTranslateY = ObjectAnimator.ofFloat(
                    characterFaceView,
                    android.view.View.TRANSLATION_Y,
                    baseFaceTranslationY,
                    baseFaceTranslationY - 3f,
                    baseFaceTranslationY
            );

            faceScaleX.setDuration(CHARACTER_IDLE_DURATION);
            faceScaleY.setDuration(CHARACTER_IDLE_DURATION);
            faceTranslateY.setDuration(CHARACTER_IDLE_DURATION);

            characterIdleSet.playTogether(
                    bodyScaleX, bodyScaleY, bodyTranslateY,
                    faceScaleX, faceScaleY, faceTranslateY
            );
        } else {
            characterIdleSet.playTogether(bodyScaleX, bodyScaleY, bodyTranslateY);
        }

        characterIdleSet.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled && !stopped && characterMode) {
                    startCharacterIdleLoop();
                }
            }
        });
        characterIdleSet.start();
    }

    private void scheduleNextBlink() {
        if (stopped || !characterMode || characterFaceView == null) return;
        if (currentCharacterBlinkFaceResId == 0 || currentCharacterOpenFaceResId == 0) return;

        if (blinkRunnable != null) {
            handler.removeCallbacks(blinkRunnable);
        }

        long delay = 2300L + random.nextInt(2600);

        blinkRunnable = () -> {
            if (stopped || !characterMode || characterFaceView == null) return;
            if (characterFaceView.getVisibility() != android.view.View.VISIBLE) {
                scheduleNextBlink();
                return;
            }
            playBlinkSequence();
        };

        handler.postDelayed(blinkRunnable, delay);
    }

    private void playBlinkSequence() {
        if (characterFaceView == null) {
            scheduleNextBlink();
            return;
        }

        if (!characterMode || currentCharacterBlinkFaceResId == 0 || currentCharacterOpenFaceResId == 0) {
            blinking = false;
            return;
        }

        blinking = true;
        boolean doubleBlink = random.nextInt(100) < 24;

        characterFaceView.setImageResource(currentCharacterBlinkFaceResId);

        handler.postDelayed(() -> {
            if (stopped || !characterMode || characterFaceView == null) {
                blinking = false;
                return;
            }

            characterFaceView.setImageResource(currentCharacterOpenFaceResId);

            if (!doubleBlink) {
                blinking = false;
                scheduleNextBlink();
                return;
            }

            handler.postDelayed(() -> {
                if (stopped || !characterMode || characterFaceView == null) {
                    blinking = false;
                    return;
                }

                characterFaceView.setImageResource(currentCharacterBlinkFaceResId);

                handler.postDelayed(() -> {
                    if (stopped || !characterMode || characterFaceView == null) {
                        blinking = false;
                        return;
                    }

                    characterFaceView.setImageResource(currentCharacterOpenFaceResId);
                    blinking = false;
                    scheduleNextBlink();
                }, BLINK_CLOSE_DURATION + BLINK_HOLD_DURATION);

            }, BLINK_DOUBLE_GAP_DURATION);

        }, BLINK_CLOSE_DURATION + BLINK_HOLD_DURATION);
    }

    private void clearCharacterFaceState() {
        currentCharacterOpenFaceResId = 0;
        currentCharacterBlinkFaceResId = 0;
    }

    private void forceHideCharacterFace() {
        if (characterFaceView == null) return;

        characterFaceView.animate().cancel();
        characterFaceView.clearAnimation();
        characterFaceView.setImageDrawable(null);
        characterFaceView.setVisibility(android.view.View.INVISIBLE);
        characterFaceView.setAlpha(1f);
        characterFaceView.setScaleX(1f);
        characterFaceView.setScaleY(1f);
        characterFaceView.setTranslationX(baseFaceTranslationX);
        characterFaceView.setTranslationY(baseFaceTranslationY);
    }

    private void cancelAnimator(@Nullable AnimatorSet animatorSet) {
        if (animatorSet != null) {
            animatorSet.cancel();
        }
    }

    private void resetEggBaseState() {
        if (eggView == null) return;
        eggView.animate().cancel();
        eggView.clearAnimation();
        eggView.setScaleX(1f);
        eggView.setScaleY(1f);
        eggView.setTranslationY(0f);
    }

    private void resetCharacterBaseState() {
        if (characterBodyView != null) {
            characterBodyView.animate().cancel();
            characterBodyView.clearAnimation();
            characterBodyView.setScaleX(1f);
            characterBodyView.setScaleY(1f);
            characterBodyView.setTranslationX(baseBodyTranslationX);
            characterBodyView.setTranslationY(baseBodyTranslationY);
        }

        if (characterFaceView != null) {
            characterFaceView.animate().cancel();
            characterFaceView.clearAnimation();
            characterFaceView.setScaleX(1f);
            characterFaceView.setScaleY(1f);
            characterFaceView.setTranslationX(baseFaceTranslationX);
            characterFaceView.setTranslationY(baseFaceTranslationY);
        }
    }
}