package com.namgyun.tamakitchen.ui.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.home.factory.HomeBubbleTextFactory;

public class HomeBubbleController {

    public interface StateProvider {
        int getCurrentStage();
        int getCurrentHunger();
        int getLocalTapCount();
        boolean isHatchRunning();
    }

    public interface Callback {
        void onResumeIdleRequested();
    }

    private final Fragment fragment;
    private final TextView tvHomeBubble;
    private final ImageView ivBubbleEmotion;
    private final ImageView ivEggSequence;
    private final Handler handler;
    private final StateProvider stateProvider;
    private final Callback callback;

    private boolean isShowingHappyEmotion = false;
    private Runnable bubbleResetRunnable = null;

    public HomeBubbleController(
            @NonNull Fragment fragment,
            @NonNull TextView tvHomeBubble,
            @NonNull ImageView ivBubbleEmotion,
            @NonNull ImageView ivEggSequence,
            @NonNull Handler handler,
            @NonNull StateProvider stateProvider,
            @NonNull Callback callback
    ) {
        this.fragment = fragment;
        this.tvHomeBubble = tvHomeBubble;
        this.ivBubbleEmotion = ivBubbleEmotion;
        this.ivEggSequence = ivEggSequence;
        this.handler = handler;
        this.stateProvider = stateProvider;
        this.callback = callback;
    }

    public boolean isShowingHappyEmotion() {
        return isShowingHappyEmotion;
    }

    public boolean isActionBubbleShowing() {
        return bubbleResetRunnable != null;
    }

    public void showBubbleAction(String message) {
        showBubbleAction(message, 1500L);
    }

    public void showBubbleAction(String message, long durationMs) {
        if (!fragment.isAdded()) return;

        isShowingHappyEmotion = false;

        if (bubbleResetRunnable != null) {
            handler.removeCallbacks(bubbleResetRunnable);
            bubbleResetRunnable = null;
        }

        hideBubbleEmotion();
        tvHomeBubble.setVisibility(View.VISIBLE);
        tvHomeBubble.setText(message);

        bubbleResetRunnable = () -> {
            bubbleResetRunnable = null;
            if (!fragment.isAdded()) return;

            if (stateProvider.isHatchRunning()) {
                showHatchingMessage();
                return;
            }

            int stage = stateProvider.getCurrentStage();
            int hunger = stateProvider.getCurrentHunger();

            updateBubbleEmotion(stage, hunger);
            showDefaultStatusIfPossible();
            callback.onResumeIdleRequested();
        };

        handler.postDelayed(bubbleResetRunnable, durationMs);
    }

    public void updateBubbleEmotion(int stage, int hunger) {
        if (!fragment.isAdded()) return;

        if (stateProvider.isHatchRunning()) {
            hideBubbleEmotion();
            return;
        }

        if (stage != PetPrefs.STAGE_EGG) {
            hideBubbleEmotion();
            return;
        }

        if (hunger <= 35) {
            applyBubbleStyleEgg();

            tvHomeBubble.setVisibility(View.VISIBLE);
            tvHomeBubble.setText("");

            ivBubbleEmotion.setImageResource(R.drawable.ic_egg_hungry);
            ivBubbleEmotion.setVisibility(View.VISIBLE);
            ivBubbleEmotion.bringToFront();

            ivBubbleEmotion.animate().cancel();
            ivBubbleEmotion.setAlpha(1f);
            ivBubbleEmotion.setScaleX(1f);
            ivBubbleEmotion.setScaleY(1f);
            ivBubbleEmotion.setTranslationY(0f);

            ivBubbleEmotion.animate()
                    .translationY(-8f)
                    .setDuration(420)
                    .withEndAction(() -> ivBubbleEmotion.animate()
                            .translationY(0f)
                            .setDuration(420)
                            .start())
                    .start();
        } else {
            hideBubbleEmotion();
        }
    }

    public void showHappyEggEmotion(long durationMs) {
        if (!fragment.isAdded()) return;

        isShowingHappyEmotion = true;

        if (bubbleResetRunnable != null) {
            handler.removeCallbacks(bubbleResetRunnable);
            bubbleResetRunnable = null;
        }

        applyBubbleStyleEgg();
        tvHomeBubble.setVisibility(View.VISIBLE);
        tvHomeBubble.setText("");

        ivBubbleEmotion.animate().cancel();
        ivBubbleEmotion.setImageResource(R.drawable.ic_egg_happy_heart);
        ivBubbleEmotion.setVisibility(View.VISIBLE);
        ivBubbleEmotion.setAlpha(0f);
        ivBubbleEmotion.setScaleX(1f);
        ivBubbleEmotion.setScaleY(1f);
        ivBubbleEmotion.setTranslationY(10f);
        ivBubbleEmotion.bringToFront();

        ivBubbleEmotion.animate()
                .alpha(1f)
                .translationY(-6f)
                .setDuration(260)
                .withEndAction(() -> ivBubbleEmotion.animate()
                        .translationY(0f)
                        .setDuration(260)
                        .start())
                .start();

        playEggHappyBounce();

        bubbleResetRunnable = () -> {
            isShowingHappyEmotion = false;
            if (!fragment.isAdded()) return;

            hideBubbleEmotion();

            int stage = stateProvider.getCurrentStage();
            int hunger = stateProvider.getCurrentHunger();

            if (stateProvider.isHatchRunning()) {
                showHatchingMessage();
                return;
            }

            updateBubbleEmotion(stage, hunger);
            showDefaultStatusIfPossible();
            callback.onResumeIdleRequested();
        };

        handler.postDelayed(bubbleResetRunnable, durationMs);
    }

    public void showHatchingMessage() {
        if (!fragment.isAdded()) return;
        applyBubbleStyleHatching();
        tvHomeBubble.setVisibility(View.VISIBLE);
        tvHomeBubble.setText(HomeBubbleTextFactory.buildHatchingMessage());
    }

    public void showDefaultStatusIfPossible() {
        if (!fragment.isAdded()) return;
        if (isActionBubbleShowing() || isShowingHappyEmotion) return;

        int stage = stateProvider.getCurrentStage();
        int hunger = stateProvider.getCurrentHunger();

        if (ivBubbleEmotion.getVisibility() == View.VISIBLE) {
            applyBubbleStyleEgg();
            tvHomeBubble.setVisibility(View.VISIBLE);
            tvHomeBubble.setText("");
        } else {
            applyBubbleStyleByStage(stage);
            tvHomeBubble.setVisibility(View.VISIBLE);
            tvHomeBubble.setText(
                    HomeBubbleTextFactory.buildStatusMessage(
                            fragment.requireContext(),
                            stage,
                            hunger,
                            stateProvider.getLocalTapCount()
                    )
            );
        }
    }

    public void hideBubbleEmotion() {
        ivBubbleEmotion.animate().cancel();
        ivBubbleEmotion.setVisibility(View.GONE);
        tvHomeBubble.setVisibility(View.VISIBLE);
    }

    public void clear() {
        if (bubbleResetRunnable != null) {
            handler.removeCallbacks(bubbleResetRunnable);
            bubbleResetRunnable = null;
        }
    }

    private void playEggHappyBounce() {
        if (ivEggSequence == null) return;

        ivEggSequence.animate().cancel();

        ObjectAnimator up1 = ObjectAnimator.ofFloat(ivEggSequence, View.TRANSLATION_Y, 0f, -28f);
        ObjectAnimator down1 = ObjectAnimator.ofFloat(ivEggSequence, View.TRANSLATION_Y, -28f, 0f);
        ObjectAnimator up2 = ObjectAnimator.ofFloat(ivEggSequence, View.TRANSLATION_Y, 0f, -18f);
        ObjectAnimator down2 = ObjectAnimator.ofFloat(ivEggSequence, View.TRANSLATION_Y, -18f, 0f);

        up1.setDuration(130);
        down1.setDuration(150);
        up2.setDuration(110);
        down2.setDuration(130);

        AnimatorSet bounce = new AnimatorSet();
        bounce.playSequentially(up1, down1, up2, down2);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
        bounce.start();

        handler.postDelayed(callback::onResumeIdleRequested, 560L);
    }

    private void applyBubbleStyleByStage(int stage) {
        if (stage == PetPrefs.STAGE_EGG) {
            applyBubbleStyleEgg();
        } else {
            applyBubbleStyleCharacter();
        }
    }

    private void applyBubbleStyleEgg() {
        tvHomeBubble.setBackgroundResource(R.drawable.bg_home_bubble_card);
        tvHomeBubble.setTextColor(0xFF202227);
    }

    private void applyBubbleStyleHatching() {
        tvHomeBubble.setBackgroundResource(R.drawable.bg_home_bubble_card_hatching);
        tvHomeBubble.setTextColor(0xFF1F2430);
    }

    private void applyBubbleStyleCharacter() {
        tvHomeBubble.setBackgroundResource(R.drawable.bg_home_bubble_card_character);
        tvHomeBubble.setTextColor(0xFF1D2230);
    }
}
