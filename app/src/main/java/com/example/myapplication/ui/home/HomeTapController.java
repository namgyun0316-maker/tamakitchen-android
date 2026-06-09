package com.namgyun.tamakitchen.ui.home;

import android.os.Handler;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.namgyun.tamakitchen.ui.home.factory.HomeBubbleTextFactory;

public class HomeTapController {

    public interface StateProvider {
        boolean isHatchRunning();
        boolean isCharacterHappyBouncing();
        int getCurrentStage();
        int getCurrentHunger();
        int getLocalTapCount();
    }

    public interface Callback {
        void setLocalTapCount(int value);
        void onPauseIdleAnimation();
        void onResumeIdleAnimationIfPossible();
        void onRefreshUi();
        void showBubbleAction(String message);
    }

    private static final long TAP_COOLDOWN_MS = 180L;

    private final Handler handler;
    private final StateProvider stateProvider;
    private final Callback callback;

    private long lastTapMs = 0L;

    public HomeTapController(
            @NonNull Handler handler,
            @NonNull StateProvider stateProvider,
            @NonNull Callback callback
    ) {
        this.handler = handler;
        this.stateProvider = stateProvider;
        this.callback = callback;
    }

    public void bindMascotTap(View mascotLayer, ImageView ivFridgeMascot, ImageView ivEggSequence) {
        if (mascotLayer == null) return;

        mascotLayer.setOnClickListener(view -> {
            if (stateProvider.isHatchRunning()) return;
            if (stateProvider.isCharacterHappyBouncing()) return;

            long now = SystemClock.elapsedRealtime();
            if (now - lastTapMs < TAP_COOLDOWN_MS) return;
            lastTapMs = now;

            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            callback.onPauseIdleAnimation();
            playPetTapEffect(ivFridgeMascot, ivEggSequence);

            int newTapCount = stateProvider.getLocalTapCount() + 1;
            callback.setLocalTapCount(newTapCount);

            int stage = stateProvider.getCurrentStage();
            int hunger = stateProvider.getCurrentHunger();

            if (newTapCount % 5 == 0) {
                callback.showBubbleAction(
                        HomeBubbleTextFactory.buildTapReactionMessage(stage, hunger)
                );
            } else {
                callback.onRefreshUi();
            }

            handler.postDelayed(callback::onResumeIdleAnimationIfPossible, 260L);
        });
    }

    private void playPetTapEffect(ImageView ivFridgeMascot, ImageView ivEggSequence) {
        View target = null;

        if (ivFridgeMascot != null
                && ivFridgeMascot.getVisibility() == View.VISIBLE
                && ivFridgeMascot.getAlpha() > 0.5f) {
            target = ivFridgeMascot;
        } else if (ivEggSequence != null
                && ivEggSequence.getVisibility() == View.VISIBLE
                && ivEggSequence.getAlpha() > 0.5f) {
            target = ivEggSequence;
        }

        if (target == null) return;

        target.animate()
                .rotationBy(0f)
                .translationXBy(0f)
                .cancel();

        View finalTarget = target;
        target.animate()
                .rotationBy(4f)
                .translationXBy(6f)
                .setDuration(80)
                .withEndAction(() -> finalTarget.animate()
                        .rotation(-4f)
                        .translationX(-6f)
                        .setDuration(80)
                        .withEndAction(() -> finalTarget.animate()
                                .rotation(0f)
                                .translationX(0f)
                                .setDuration(80)
                                .start())
                        .start())
                .start();
    }
}
