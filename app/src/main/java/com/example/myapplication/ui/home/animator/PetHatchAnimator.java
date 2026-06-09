package com.namgyun.tamakitchen.ui.home.animator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.pet.IngredientCharacter;

public class PetHatchAnimator {

    public interface Callback {
        void onHatchStart();
        void onHatchRevealPrepare();
        void onHatchEnd();
    }

    private final Fragment fragment;
    private final FrameLayout mascotLayer;
    private final ImageView ivEggSequence;
    private final ImageView ivFridgeMascot;
    private final ImageView ivMascotFace;
    private final FrameLayout fxContainer;
    private final View flashOverlay;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Animator currentAnimator;
    private boolean running = false;
    private Runnable currentFrameRunnable;

    private static final String BASE_FRAME = "pet_hatch_0000";

    private static final String[] HATCH_FRAMES = new String[]{
            "pet_hatch_0001",
            "pet_hatch_0003",
            "pet_hatch_0005",
            "pet_hatch_0007",
            "pet_hatch_0009",
            "pet_hatch_0010",
            "pet_hatch_0011",
            "pet_hatch_0012"
    };

    private static final long[] FRAME_DURATIONS_MS = new long[]{
            1100,
            1000,
            950,
            900,
            850,
            820,
            850,
            950
    };

    private static final float EGG_IDLE_SCALE = 0.88f;
    private static final float EGG_LAST_FRAME_SCALE = 0.84f;
    private static final float CHARACTER_IDLE_SCALE = 1.08f;

    public PetHatchAnimator(
            @NonNull Fragment fragment,
            @NonNull FrameLayout mascotLayer,
            @NonNull ImageView ivEggSequence,
            @NonNull ImageView ivFridgeMascot,
            @NonNull ImageView ivMascotFace,
            @NonNull FrameLayout fxContainer,
            @NonNull View flashOverlay
    ) {
        this.fragment = fragment;
        this.mascotLayer = mascotLayer;
        this.ivEggSequence = ivEggSequence;
        this.ivFridgeMascot = ivFridgeMascot;
        this.ivMascotFace = ivMascotFace;
        this.fxContainer = fxContainer;
        this.flashOverlay = flashOverlay;
    }

    public boolean isRunning() {
        return running;
    }

    public void cancel() {
        if (currentAnimator != null) {
            currentAnimator.cancel();
            currentAnimator = null;
        }

        if (currentFrameRunnable != null) {
            handler.removeCallbacks(currentFrameRunnable);
            currentFrameRunnable = null;
        }

        ivEggSequence.animate().cancel();
        ivFridgeMascot.animate().cancel();
        if (ivMascotFace != null) {
            ivMascotFace.animate().cancel();
        }
        flashOverlay.animate().cancel();

        running = false;
    }

    public void play(@NonNull IngredientCharacter picked, @NonNull Callback callback) {
        if (!fragment.isAdded() || running) return;

        int ingredientResId = resolveDrawableRes(picked.getDrawableName());
        if (ingredientResId == 0) return;

        running = true;
        callback.onHatchStart();

        showEggOnly();

        ivFridgeMascot.setImageResource(ingredientResId);
        ivFridgeMascot.setVisibility(View.INVISIBLE);
        ivFridgeMascot.setAlpha(0f);
        ivFridgeMascot.setScaleX(0.92f);
        ivFridgeMascot.setScaleY(0.92f);
        ivFridgeMascot.setTranslationX(0f);
        ivFridgeMascot.setTranslationY(28f);
        ivFridgeMascot.setRotation(0f);

        if (ivMascotFace != null) {
            ivMascotFace.setImageDrawable(null);
            ivMascotFace.setVisibility(View.INVISIBLE);
            ivMascotFace.setAlpha(0f);
            ivMascotFace.setScaleX(1f);
            ivMascotFace.setScaleY(1f);
            ivMascotFace.setTranslationX(0f);
            ivMascotFace.setTranslationY(0f);
            ivMascotFace.setRotation(0f);
        }

        playSoundByName("sfx_hatch_start");

        playSelectedFrameSequence(() -> {
            if (!fragment.isAdded()) {
                running = false;
                currentFrameRunnable = null;
                return;
            }
            playBurstAndReveal(callback);
        });
    }

    public void showEggOnly() {
        int baseRes = resolveDrawableRes(BASE_FRAME);
        if (baseRes != 0) {
            ivEggSequence.setImageResource(baseRes);
        }

        ivEggSequence.animate().cancel();
        ivFridgeMascot.animate().cancel();
        if (ivMascotFace != null) {
            ivMascotFace.animate().cancel();
        }

        ivEggSequence.setVisibility(View.VISIBLE);
        ivEggSequence.setAlpha(1f);
        ivEggSequence.setScaleX(EGG_IDLE_SCALE);
        ivEggSequence.setScaleY(EGG_IDLE_SCALE);
        ivEggSequence.setTranslationX(0f);
        ivEggSequence.setTranslationY(0f);
        ivEggSequence.setRotation(0f);

        ivFridgeMascot.setVisibility(View.INVISIBLE);
        ivFridgeMascot.setAlpha(0f);
        ivFridgeMascot.setScaleX(CHARACTER_IDLE_SCALE);
        ivFridgeMascot.setScaleY(CHARACTER_IDLE_SCALE);
        ivFridgeMascot.setTranslationX(0f);
        ivFridgeMascot.setTranslationY(0f);
        ivFridgeMascot.setRotation(0f);

        if (ivMascotFace != null) {
            ivMascotFace.setImageDrawable(null);
            ivMascotFace.setVisibility(View.INVISIBLE);
            ivMascotFace.setAlpha(0f);
            ivMascotFace.setScaleX(1f);
            ivMascotFace.setScaleY(1f);
            ivMascotFace.setTranslationX(0f);
            ivMascotFace.setTranslationY(0f);
            ivMascotFace.setRotation(0f);
        }

        flashOverlay.setAlpha(0f);
        flashOverlay.setVisibility(View.GONE);
    }

    public void showCharacterOnly(int drawableResId) {
        ivEggSequence.animate().cancel();
        ivFridgeMascot.animate().cancel();
        if (ivMascotFace != null) {
            ivMascotFace.animate().cancel();
        }

        ivEggSequence.setVisibility(View.INVISIBLE);
        ivEggSequence.setAlpha(0f);

        ivFridgeMascot.setImageResource(drawableResId);
        ivFridgeMascot.setVisibility(View.VISIBLE);
        ivFridgeMascot.setAlpha(1f);
        ivFridgeMascot.setScaleX(CHARACTER_IDLE_SCALE);
        ivFridgeMascot.setScaleY(CHARACTER_IDLE_SCALE);
        ivFridgeMascot.setTranslationX(0f);
        ivFridgeMascot.setTranslationY(0f);
        ivFridgeMascot.setRotation(0f);

        flashOverlay.setAlpha(0f);
        flashOverlay.setVisibility(View.GONE);
    }

    private void playSelectedFrameSequence(@NonNull Runnable endAction) {
        currentFrameRunnable = new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (!fragment.isAdded()) {
                    running = false;
                    currentFrameRunnable = null;
                    return;
                }

                if (index >= HATCH_FRAMES.length) {
                    currentFrameRunnable = null;
                    endAction.run();
                    return;
                }

                int frameRes = resolveDrawableRes(HATCH_FRAMES[index]);
                if (frameRes != 0) {
                    ivEggSequence.setImageResource(frameRes);
                }

                if (index == HATCH_FRAMES.length - 1) {
                    ivEggSequence.setScaleX(EGG_LAST_FRAME_SCALE);
                    ivEggSequence.setScaleY(EGG_LAST_FRAME_SCALE);
                } else {
                    ivEggSequence.setScaleX(EGG_IDLE_SCALE);
                    ivEggSequence.setScaleY(EGG_IDLE_SCALE);
                }

                playWobbleForStep(index);
                playMiniLift(index);

                if (index == 2 || index == 4) {
                    spawnSparkles(4);
                    playSoundByName("sfx_hatch_crack");
                } else if (index == 5 || index == 6) {
                    spawnSparkles(6);
                    playSoundByName("sfx_hatch_crack");
                }

                if (index >= 6) {
                    playTensionPulse();
                }

                long delay = FRAME_DURATIONS_MS[Math.min(index, FRAME_DURATIONS_MS.length - 1)];
                index++;
                handler.postDelayed(this, delay);
            }
        };

        handler.post(currentFrameRunnable);
    }

    private void playWobbleForStep(int index) {
        float rotation;
        float dx;
        float scale;
        long wobbleDuration;

        switch (index) {
            case 0:
                rotation = 3.5f;
                dx = 5f;
                scale = EGG_IDLE_SCALE;
                wobbleDuration = 420;
                break;
            case 1:
                rotation = 4.2f;
                dx = 6f;
                scale = EGG_IDLE_SCALE * 1.005f;
                wobbleDuration = 430;
                break;
            case 2:
                rotation = 5.0f;
                dx = 7f;
                scale = EGG_IDLE_SCALE * 1.01f;
                wobbleDuration = 430;
                break;
            case 3:
                rotation = 5.8f;
                dx = 8f;
                scale = EGG_IDLE_SCALE * 1.012f;
                wobbleDuration = 420;
                break;
            case 4:
                rotation = 6.5f;
                dx = 9f;
                scale = EGG_IDLE_SCALE * 1.015f;
                wobbleDuration = 410;
                break;
            case 5:
                rotation = 7.0f;
                dx = 10f;
                scale = EGG_IDLE_SCALE * 1.018f;
                wobbleDuration = 400;
                break;
            case 6:
                rotation = 7.5f;
                dx = 10.5f;
                scale = EGG_IDLE_SCALE * 1.022f;
                wobbleDuration = 390;
                break;
            default:
                rotation = 8.0f;
                dx = 11f;
                scale = EGG_LAST_FRAME_SCALE * 1.02f;
                wobbleDuration = 380;
                break;
        }

        AnimatorSet wobble = createEggWobble(wobbleDuration, rotation, dx, scale);
        currentAnimator = wobble;
        wobble.start();
    }

    private void playMiniLift(int index) {
        float up = (index < 4) ? -4f : -6f;

        ivEggSequence.animate()
                .translationY(up)
                .setDuration(120)
                .withEndAction(() -> ivEggSequence.animate()
                        .translationY(0f)
                        .setDuration(180)
                        .start())
                .start();
    }

    private void playTensionPulse() {
        float currentX = ivEggSequence.getScaleX();
        float currentY = ivEggSequence.getScaleY();

        ivEggSequence.animate()
                .scaleX(currentX * 1.015f)
                .scaleY(currentY * 1.015f)
                .setDuration(120)
                .withEndAction(() -> ivEggSequence.animate()
                        .scaleX(currentX * 0.985f)
                        .scaleY(currentY * 0.985f)
                        .setDuration(140)
                        .start())
                .start();
    }

    private void playBurstAndReveal(@NonNull Callback callback) {
        AnimatorSet anticipation = new AnimatorSet();
        anticipation.playTogether(
                ObjectAnimator.ofFloat(
                        ivEggSequence,
                        View.SCALE_X,
                        ivEggSequence.getScaleX(),
                        EGG_LAST_FRAME_SCALE * 0.95f,
                        EGG_IDLE_SCALE * 1.10f
                ),
                ObjectAnimator.ofFloat(
                        ivEggSequence,
                        View.SCALE_Y,
                        ivEggSequence.getScaleY(),
                        EGG_IDLE_SCALE * 1.10f,
                        EGG_LAST_FRAME_SCALE * 0.95f
                ),
                ObjectAnimator.ofFloat(ivEggSequence, View.ROTATION, 0f, -9f, 9f, 0f),
                ObjectAnimator.ofFloat(ivEggSequence, View.TRANSLATION_Y, 0f, -10f, 0f)
        );
        anticipation.setDuration(360);
        anticipation.setInterpolator(new AccelerateDecelerateInterpolator());
        anticipation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mascotLayer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                playSoundByName("sfx_hatch_crack");
                spawnSparkles(10);
            }
        });

        ObjectAnimator tinyHold = ObjectAnimator.ofFloat(ivEggSequence, View.ALPHA, 1f, 1f);
        tinyHold.setDuration(170);

        AnimatorSet burst = new AnimatorSet();
        burst.playTogether(
                ObjectAnimator.ofFloat(ivEggSequence, View.ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(ivEggSequence, View.SCALE_X, EGG_IDLE_SCALE * 1.10f, EGG_IDLE_SCALE * 1.30f),
                ObjectAnimator.ofFloat(ivEggSequence, View.SCALE_Y, EGG_LAST_FRAME_SCALE * 0.95f, EGG_IDLE_SCALE * 1.30f),
                ObjectAnimator.ofFloat(ivEggSequence, View.TRANSLATION_Y, 0f, -24f),
                ObjectAnimator.ofFloat(ivEggSequence, View.ROTATION, 0f, -6f, 6f, 0f)
        );
        burst.setDuration(280);
        burst.setInterpolator(new AccelerateDecelerateInterpolator());
        burst.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                flashOverlay.setAlpha(0f);
                flashOverlay.setVisibility(View.GONE);
                spawnSparkles(28);
                mascotLayer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                vibrateFinalReveal();
                playSoundByName("sfx_hatch_burst");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ivEggSequence.setVisibility(View.INVISIBLE);
                ivEggSequence.setAlpha(0f);
            }
        });

        AnimatorSet sequence = new AnimatorSet();
        sequence.playSequentially(anticipation, tinyHold, burst);
        sequence.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!fragment.isAdded()) {
                    running = false;
                    currentAnimator = null;
                    return;
                }

                callback.onHatchRevealPrepare();

                boolean hasFace = ivMascotFace != null
                        && ivMascotFace.getVisibility() == View.VISIBLE
                        && ivMascotFace.getDrawable() != null;

                ivFridgeMascot.animate().cancel();
                ivFridgeMascot.setVisibility(View.VISIBLE);
                ivFridgeMascot.setAlpha(0f);
                ivFridgeMascot.setScaleX(0.86f);
                ivFridgeMascot.setScaleY(0.86f);
                ivFridgeMascot.setTranslationY(42f);
                ivFridgeMascot.setRotation(0f);

                AnimatorSet reveal = new AnimatorSet();

                if (hasFace) {
                    ivMascotFace.animate().cancel();
                    ivMascotFace.setVisibility(View.VISIBLE);
                    ivMascotFace.setAlpha(0f);
                    ivMascotFace.setScaleX(0.86f);
                    ivMascotFace.setScaleY(0.86f);
                    ivMascotFace.setTranslationY(42f);
                    ivMascotFace.setRotation(0f);

                    reveal.playTogether(
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.ALPHA, 0f, 1f),
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.SCALE_X, 0.86f, 1.20f, 0.98f, CHARACTER_IDLE_SCALE),
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.SCALE_Y, 0.86f, 1.20f, 0.98f, CHARACTER_IDLE_SCALE),
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.TRANSLATION_Y, 42f, -18f, 6f, 0f),

                            ObjectAnimator.ofFloat(ivMascotFace, View.ALPHA, 0f, 1f),
                            ObjectAnimator.ofFloat(ivMascotFace, View.SCALE_X, 0.86f, 1.20f, 0.98f, 1f),
                            ObjectAnimator.ofFloat(ivMascotFace, View.SCALE_Y, 0.86f, 1.20f, 0.98f, 1f),
                            ObjectAnimator.ofFloat(ivMascotFace, View.TRANSLATION_Y, 42f, -18f, 6f, 0f)
                    );
                } else {
                    reveal.playTogether(
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.ALPHA, 0f, 1f),
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.SCALE_X, 0.86f, 1.20f, 0.98f, CHARACTER_IDLE_SCALE),
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.SCALE_Y, 0.86f, 1.20f, 0.98f, CHARACTER_IDLE_SCALE),
                            ObjectAnimator.ofFloat(ivFridgeMascot, View.TRANSLATION_Y, 42f, -18f, 6f, 0f)
                    );
                }

                reveal.setDuration(620);
                reveal.setInterpolator(new DecelerateInterpolator());
                reveal.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        playSoundByName("sfx_hatch_reveal");
                        spawnSparkles(18);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!fragment.isAdded()) {
                            running = false;
                            currentAnimator = null;
                            return;
                        }

                        ivFridgeMascot.setVisibility(View.VISIBLE);
                        ivFridgeMascot.setAlpha(1f);
                        ivFridgeMascot.setScaleX(CHARACTER_IDLE_SCALE);
                        ivFridgeMascot.setScaleY(CHARACTER_IDLE_SCALE);
                        ivFridgeMascot.setTranslationX(0f);
                        ivFridgeMascot.setTranslationY(0f);
                        ivFridgeMascot.setRotation(0f);

                        if (hasFace && ivMascotFace != null) {
                            ivMascotFace.setVisibility(View.VISIBLE);
                            ivMascotFace.setAlpha(1f);
                            ivMascotFace.setScaleX(1f);
                            ivMascotFace.setScaleY(1f);
                            ivMascotFace.setTranslationX(0f);
                            ivMascotFace.setTranslationY(0f);
                            ivMascotFace.setRotation(0f);
                        }

                        playTaDaBounce();

                        running = false;
                        currentAnimator = null;
                        callback.onHatchEnd();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        running = false;
                        currentAnimator = null;
                    }
                });

                currentAnimator = reveal;
                reveal.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                running = false;
                currentAnimator = null;
            }
        });

        currentAnimator = sequence;
        sequence.start();
    }

    private void playTaDaBounce() {
        ivFridgeMascot.animate().cancel();

        AnimatorSet bounce = new AnimatorSet();
        bounce.playTogether(
                ObjectAnimator.ofFloat(
                        ivFridgeMascot,
                        View.SCALE_X,
                        CHARACTER_IDLE_SCALE,
                        CHARACTER_IDLE_SCALE * 1.08f,
                        CHARACTER_IDLE_SCALE * 0.98f,
                        CHARACTER_IDLE_SCALE
                ),
                ObjectAnimator.ofFloat(
                        ivFridgeMascot,
                        View.SCALE_Y,
                        CHARACTER_IDLE_SCALE,
                        CHARACTER_IDLE_SCALE * 1.08f,
                        CHARACTER_IDLE_SCALE * 0.98f,
                        CHARACTER_IDLE_SCALE
                ),
                ObjectAnimator.ofFloat(ivFridgeMascot, View.TRANSLATION_Y, 0f, -10f, 0f)
        );
        bounce.setDuration(360);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
        bounce.start();
    }

    private AnimatorSet createEggWobble(long duration, float rotation, float dx, float scale) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(
                        ivEggSequence,
                        View.ROTATION,
                        0f, -rotation, rotation, -rotation * 0.7f, rotation * 0.7f, 0f
                ),
                ObjectAnimator.ofFloat(
                        ivEggSequence,
                        View.TRANSLATION_X,
                        0f, -dx, dx, -dx * 0.7f, dx * 0.7f, 0f
                ),
                ObjectAnimator.ofFloat(
                        ivEggSequence,
                        View.SCALE_X,
                        ivEggSequence.getScaleX(),
                        scale,
                        ivEggSequence.getScaleX() * 0.995f,
                        ivEggSequence.getScaleX()
                ),
                ObjectAnimator.ofFloat(
                        ivEggSequence,
                        View.SCALE_Y,
                        ivEggSequence.getScaleY(),
                        scale,
                        ivEggSequence.getScaleY() * 0.995f,
                        ivEggSequence.getScaleY()
                )
        );
        set.setDuration(duration);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        return set;
    }

    private void vibrateFinalReveal() {
        if (!fragment.isAdded()) return;

        Context context = fragment.requireContext();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm =
                        (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm == null) return;
                Vibrator vibrator = vm.getDefaultVibrator();
                if (vibrator == null || !vibrator.hasVibrator()) return;

                vibrator.vibrate(
                        VibrationEffect.createOneShot(40, 150)
                );
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator == null || !vibrator.hasVibrator()) return;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                            VibrationEffect.createOneShot(40, 150)
                    );
                } else {
                    vibrator.vibrate(40);
                }
            }
        } catch (Exception ignored) {
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

    private void spawnSparkles(int count) {
        fxContainer.post(() -> {
            int w = fxContainer.getWidth();
            int h = fxContainer.getHeight();
            if (w <= 0 || h <= 0 || !fragment.isAdded()) return;

            int centerX = w / 2;
            int centerY = (int) (h * 0.52f);

            for (int i = 0; i < count; i++) {
                ImageView particle = new ImageView(fragment.requireContext());

                int particleRes = resolveDrawableRes(i % 2 == 0 ? "sparkle_star" : "particle_dot");
                if (particleRes != 0) {
                    particle.setImageResource(particleRes);
                } else {
                    particle.setBackgroundColor(0x66FFFFFF);
                }

                int size = 14 + (int) (Math.random() * 18);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.leftMargin = centerX - size / 2;
                lp.topMargin = centerY - size / 2;
                particle.setLayoutParams(lp);

                fxContainer.addView(particle);

                float dx = -170 + (float) (Math.random() * 341);
                float dy = -170 + (float) (Math.random() * 220);
                float rotate = -180 + (float) (Math.random() * 361);

                ObjectAnimator tx = ObjectAnimator.ofFloat(particle, View.TRANSLATION_X, 0f, dx);
                ObjectAnimator ty = ObjectAnimator.ofFloat(particle, View.TRANSLATION_Y, 0f, dy);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(particle, View.ALPHA, 0f, 1f, 0f);
                ObjectAnimator sx = ObjectAnimator.ofFloat(particle, View.SCALE_X, 0.2f, 1f, 0.3f);
                ObjectAnimator sy = ObjectAnimator.ofFloat(particle, View.SCALE_Y, 0.2f, 1f, 0.3f);
                ObjectAnimator rot = ObjectAnimator.ofFloat(particle, View.ROTATION, 0f, rotate);

                long dur = 420 + (long) (Math.random() * 220);

                AnimatorSet set = new AnimatorSet();
                set.playTogether(tx, ty, alpha, sx, sy, rot);
                set.setDuration(dur);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fxContainer.removeView(particle);
                    }
                });
                set.start();
            }
        });
    }

    private void playSoundByName(String rawName) {
        if (!fragment.isAdded()) return;

        int resId = fragment.getResources().getIdentifier(
                rawName,
                "raw",
                fragment.requireContext().getPackageName()
        );
        if (resId == 0) return;

        MediaPlayer mp = MediaPlayer.create(fragment.requireContext(), resId);
        if (mp == null) return;

        mp.setOnCompletionListener(player -> {
            player.reset();
            player.release();
        });
        mp.start();
    }
}