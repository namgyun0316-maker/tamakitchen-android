package com.namgyun.tamakitchen.ui.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public final class HomeMascotMotionHelper {

    public interface BounceEndListener {
        void onEnd(boolean cancelled);
    }

    private HomeMascotMotionHelper() {}

    public static AnimatorSet createFaceSwapAnimator(
            @NonNull ImageView target,
            int newResId,
            long durationMs
    ) {
        target.animate().cancel();
        target.clearAnimation();
        target.setVisibility(View.VISIBLE);
        target.setAlpha(1f);
        target.setScaleX(1f);
        target.setScaleY(1f);

        ObjectAnimator shrinkX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 0.96f);
        ObjectAnimator shrinkY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.96f);
        shrinkX.setDuration(durationMs);
        shrinkY.setDuration(durationMs);

        AnimatorSet shrinkSet = new AnimatorSet();
        shrinkSet.playTogether(shrinkX, shrinkY);

        ObjectAnimator expandX = ObjectAnimator.ofFloat(target, View.SCALE_X, 0.96f, 1f);
        ObjectAnimator expandY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.96f, 1f);
        expandX.setDuration(durationMs);
        expandY.setDuration(durationMs);

        AnimatorSet expandSet = new AnimatorSet();
        expandSet.playTogether(expandX, expandY);

        AnimatorSet animator = new AnimatorSet();
        animator.playSequentially(shrinkSet, expandSet);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        shrinkSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                target.setImageResource(newResId);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                target.setScaleX(1f);
                target.setScaleY(1f);
                target.setAlpha(1f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                target.setScaleX(1f);
                target.setScaleY(1f);
                target.setAlpha(1f);
            }
        });

        return animator;
    }

    public static AnimatorSet createCharacterHappyBounceAnimator(
            @NonNull View target,
            @NonNull BounceEndListener listener
    ) {
        target.setTranslationY(0f);
        target.setScaleX(1f);
        target.setScaleY(1f);
        target.setRotation(0f);

        ObjectAnimator squashDownY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.92f);
        ObjectAnimator squashDownX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.08f);
        ObjectAnimator prepDown = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, 8f);

        squashDownY.setDuration(90);
        squashDownX.setDuration(90);
        prepDown.setDuration(90);

        AnimatorSet prepSet = new AnimatorSet();
        prepSet.playTogether(squashDownY, squashDownX, prepDown);

        ObjectAnimator jump1Y = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 8f, -34f);
        ObjectAnimator jump1ScaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1.08f, 0.95f);
        ObjectAnimator jump1ScaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.92f, 1.10f);

        jump1Y.setDuration(160);
        jump1ScaleX.setDuration(160);
        jump1ScaleY.setDuration(160);

        AnimatorSet jump1 = new AnimatorSet();
        jump1.playTogether(jump1Y, jump1ScaleX, jump1ScaleY);

        ObjectAnimator land1Y = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, -34f, 0f);
        ObjectAnimator land1ScaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 0.95f, 1.06f);
        ObjectAnimator land1ScaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1.10f, 0.94f);

        land1Y.setDuration(150);
        land1ScaleX.setDuration(150);
        land1ScaleY.setDuration(150);

        AnimatorSet land1 = new AnimatorSet();
        land1.playTogether(land1Y, land1ScaleX, land1ScaleY);

        ObjectAnimator jump2Y = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -20f);
        ObjectAnimator jump2ScaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1.06f, 0.97f);
        ObjectAnimator jump2ScaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.94f, 1.06f);

        jump2Y.setDuration(120);
        jump2ScaleX.setDuration(120);
        jump2ScaleY.setDuration(120);

        AnimatorSet jump2 = new AnimatorSet();
        jump2.playTogether(jump2Y, jump2ScaleX, jump2ScaleY);

        ObjectAnimator land2Y = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, -20f, 0f);
        ObjectAnimator land2ScaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 0.97f, 1f);
        ObjectAnimator land2ScaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1.06f, 1f);

        land2Y.setDuration(130);
        land2ScaleX.setDuration(130);
        land2ScaleY.setDuration(130);

        AnimatorSet land2 = new AnimatorSet();
        land2.playTogether(land2Y, land2ScaleX, land2ScaleY);

        AnimatorSet animator = new AnimatorSet();
        animator.playSequentially(prepSet, jump1, land1, jump2, land2);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                target.setTranslationY(0f);
                target.setScaleX(1f);
                target.setScaleY(1f);
                target.setRotation(0f);
                listener.onEnd(cancelled);
            }
        });

        return animator;
    }
}
