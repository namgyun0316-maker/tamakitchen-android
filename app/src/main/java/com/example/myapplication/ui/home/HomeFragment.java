package com.namgyun.tamakitchen.ui.home;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.analytics.AppAnalytics;
import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.checklist.ChecklistActivity;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionCategory;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.home.animator.PetHatchAnimator;
import com.namgyun.tamakitchen.ui.home.animator.PetIdleAnimator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    private static final long HAPPY_EMOTION_DURATION_MS = 1300L;
    private static final int HUNGRY_CHARACTER_THRESHOLD = 30;
    private static final long CHARACTER_HAPPY_FACE_DURATION_MS = 1200L;
    private static final long FACE_TRANSITION_DURATION_MS = 110L;

    // ✅ 앱을 켜둔 상태에서도 1분마다 포만도 UI 갱신
    private static final long HUNGER_REFRESH_INTERVAL_MS = 60L * 1000L;

    private TextView tvCoins;
    private MaterialButton btnShop;
    private TextView tvHomeBubble;
    private ImageView ivBubbleEmotion;

    private FrameLayout mascotLayer;
    private ImageView ivEggSequence;
    private ImageView ivFridgeMascot;
    private ImageView ivMascotFace;

    private FrameLayout fxContainer;
    private View flashOverlay;

    private ProgressBar pbHunger;
    private TextView tvHungerValue;

    private TextView tvLevel;
    private ProgressBar pbExp;
    private TextView tvExpPercent;

    private MaterialCardView cardFeed;
    private ImageView ivFeed;
    private TextView tvFeedCount;

    private LinearLayout cardCollectionBook;
    private LinearLayout cardAttendance;
    private LinearLayout cardFusion;
    private LinearLayout cardChecklist;

    private final Handler bubbleHandler = new Handler(Looper.getMainLooper());

    private final Runnable hungerRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;

            refreshHomeUi();

            bubbleHandler.postDelayed(this, HUNGER_REFRESH_INTERVAL_MS);
        }
    };

    private PetHatchAnimator hatchAnimator;
    private PetIdleAnimator idleAnimator;

    private HomeStateProvider stateProvider;
    private HomeCharacterResourceResolver characterResolver;
    private HomeBubbleController bubbleController;
    private HomeFaceController faceController;
    private HomeDisplayController displayController;
    private HomeHatchController hatchController;
    private HomeUiBinder uiBinder;
    private HomeFeedController feedController;
    private HomeNavigationController navigationController;
    private HomeTapController tapController;

    private int localTapCount = 0;
    private boolean fusionGuideDialogShowing = false;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        bindViews(v);
        initControllers();
        stateProvider.initPetState();
        bindEvents();

        stateProvider.syncHomeDisplayPriority();
        refreshHomeUi();
        requestHatchIfPending();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppAnalytics.logScreen(requireContext(), "home_screen");
        stateProvider.initPetState();
        stateProvider.syncHomeDisplayPriority();
        stateProvider.ensureSelectedCollectionStateIfNeeded();

        refreshHomeUi();
        requestHatchIfPending();
        startHungerRefreshLoop();

        bubbleHandler.postDelayed(() -> {
            if (!isAdded()) return;
            if (idleAnimator != null
                    && !characterResolver.isCharacterHungryState()
                    && !faceController.isShowingCharacterHappyFace()
                    && !faceController.isCharacterHappyBouncing()
                    && (hatchController == null || !hatchController.isRunning())) {
                idleAnimator.playBlinkNow();
            }
        }, 1000L);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHungerRefreshLoop();
    }

    private void startHungerRefreshLoop() {
        bubbleHandler.removeCallbacks(hungerRefreshRunnable);
        bubbleHandler.postDelayed(hungerRefreshRunnable, HUNGER_REFRESH_INTERVAL_MS);
    }

    private void stopHungerRefreshLoop() {
        bubbleHandler.removeCallbacks(hungerRefreshRunnable);
    }

    private void bindViews(@NonNull View v) {
        tvCoins = v.findViewById(R.id.tvCoins);
        btnShop = v.findViewById(R.id.btnShop);
        tvHomeBubble = v.findViewById(R.id.tvHomeBubble);
        ivBubbleEmotion = v.findViewById(R.id.ivBubbleEmotion);

        mascotLayer = v.findViewById(R.id.mascotLayer);
        ivEggSequence = v.findViewById(R.id.ivEggSequence);
        ivFridgeMascot = v.findViewById(R.id.ivFridgeMascot);
        ivMascotFace = v.findViewById(R.id.ivMascotFace);

        fxContainer = v.findViewById(R.id.fxContainer);
        flashOverlay = v.findViewById(R.id.flashOverlay);

        pbHunger = v.findViewById(R.id.pbHunger);
        tvHungerValue = v.findViewById(R.id.tvHungerValue);

        tvLevel = v.findViewById(R.id.tvLevel);
        pbExp = v.findViewById(R.id.pbExp);
        tvExpPercent = v.findViewById(R.id.tvExpPercent);

        cardFeed = v.findViewById(R.id.cardFeed);
        ivFeed = v.findViewById(R.id.ivFeed);
        tvFeedCount = v.findViewById(R.id.tvFeedCount);

        cardCollectionBook = v.findViewById(R.id.cardCollectionBook);
        cardAttendance = v.findViewById(R.id.cardAttendance);
        cardFusion = v.findViewById(R.id.cardFusion);
        cardChecklist = v.findViewById(R.id.cardChecklist);
    }

    private void initControllers() {
        hatchAnimator = new PetHatchAnimator(
                this,
                mascotLayer,
                ivEggSequence,
                ivFridgeMascot,
                ivMascotFace,
                fxContainer,
                flashOverlay
        );

        idleAnimator = new PetIdleAnimator(ivEggSequence, ivFridgeMascot, ivMascotFace);
        stateProvider = new HomeStateProvider(this);

        characterResolver = new HomeCharacterResourceResolver(
                this,
                new HomeCharacterResourceResolver.StateProvider() {
                    @Override
                    public boolean isCollectionMode() {
                        return stateProvider.isCollectionMode();
                    }

                    @Override
                    public boolean shouldPrioritizeMainPetHatching() {
                        return stateProvider.shouldPrioritizeMainPetHatching();
                    }

                    @Override
                    public String getSelectedCollectionKey() {
                        return stateProvider.getSelectedCollectionKey();
                    }

                    @Override
                    public int getCurrentStage() {
                        return stateProvider.getCurrentStage();
                    }

                    @Override
                    public int getCurrentHunger() {
                        return stateProvider.getCurrentHunger();
                    }
                },
                HUNGRY_CHARACTER_THRESHOLD
        );

        bubbleController = new HomeBubbleController(
                this,
                tvHomeBubble,
                ivBubbleEmotion,
                ivEggSequence,
                bubbleHandler,
                new HomeBubbleController.StateProvider() {
                    @Override
                    public int getCurrentStage() {
                        return stateProvider.getCurrentStage();
                    }

                    @Override
                    public int getCurrentHunger() {
                        return stateProvider.getCurrentHunger();
                    }

                    @Override
                    public int getLocalTapCount() {
                        return localTapCount;
                    }

                    @Override
                    public boolean isHatchRunning() {
                        return hatchController != null && hatchController.isRunning();
                    }
                },
                this::resumeIdleAnimationIfPossible
        );

        faceController = new HomeFaceController(
                this,
                bubbleHandler,
                mascotLayer,
                ivFridgeMascot,
                ivMascotFace,
                stateProvider,
                characterResolver,
                new HomeFaceController.Callback() {
                    @Override
                    public void onPauseIdleAnimation() {
                        pauseIdleAnimation();
                    }

                    @Override
                    public void onResumeIdleAnimationIfPossible() {
                        resumeIdleAnimationIfPossible();
                    }

                    @Override
                    public void onRefreshUi() {
                        refreshHomeUi();
                    }
                },
                FACE_TRANSITION_DURATION_MS,
                CHARACTER_HAPPY_FACE_DURATION_MS
        );

        displayController = new HomeDisplayController(
                this,
                hatchAnimator,
                stateProvider,
                characterResolver,
                new HomeDisplayController.FaceStateProvider() {
                    @Override
                    public boolean isShowingCharacterHappyFace() {
                        return faceController.isShowingCharacterHappyFace();
                    }

                    @Override
                    public boolean shouldRestoreHungryFaceAfterHappy() {
                        return faceController.shouldRestoreHungryFaceAfterHappy();
                    }

                    @Override
                    public boolean isCharacterHappyBouncing() {
                        return faceController.isCharacterHappyBouncing();
                    }
                },
                ivMascotFace
        );

        hatchController = new HomeHatchController(
                this,
                hatchAnimator,
                bubbleController,
                stateProvider,
                displayController,
                new HomeHatchController.Callback() {
                    @Override
                    public void onPauseIdleAnimation() {
                        pauseIdleAnimation();
                    }

                    @Override
                    public void onResumeIdleAnimationIfPossible() {
                        resumeIdleAnimationIfPossible();
                    }

                    @Override
                    public void onRefreshUi() {
                        refreshHomeUi();
                    }
                }
        );

        uiBinder = new HomeUiBinder(
                this,
                stateProvider,
                bubbleController,
                displayController,
                faceController,
                hatchController,
                new HomeUiBinder.Callback() {
                    @Override
                    public void onPauseIdleAnimation() {
                        pauseIdleAnimation();
                    }

                    @Override
                    public void onResumeIdleAnimationIfPossible() {
                        resumeIdleAnimationIfPossible();
                    }

                    @Override
                    public void onPlayHatchIfNeeded() {
                        requestHatchIfPending();
                    }
                },
                tvCoins,
                tvHungerValue,
                tvFeedCount,
                tvLevel,
                tvExpPercent,
                pbHunger,
                pbExp
        );

        feedController = new HomeFeedController(
                this,
                stateProvider,
                faceController,
                new HomeFeedController.Callback() {
                    @Override
                    public void onPauseIdleAnimation() {
                        pauseIdleAnimation();
                    }

                    @Override
                    public void onResumeIdleAnimationIfPossible() {
                        resumeIdleAnimationIfPossible();
                    }

                    @Override
                    public void onRefreshUi() {
                        refreshHomeUi();
                    }

                    @Override
                    public void onSyncHomeDisplayPriority() {
                        stateProvider.syncHomeDisplayPriority();
                    }

                    @Override
                    public void onPlayHatchIfNeeded() {
                        requestHatchIfPending();
                    }

                    @Override
                    public void onShowHappyEggEmotion() {
                        pauseIdleAnimation();
                        bubbleController.showHappyEggEmotion(HAPPY_EMOTION_DURATION_MS);
                    }

                    @Override
                    public void onShowCharacterHappyFaceTemporarily() {
                        faceController.showCharacterHappyFaceTemporarily();
                    }

                    @Override
                    public void onPlayCharacterHappyBounce() {
                        faceController.playCharacterHappyBounce();
                    }

                    @Override
                    public void onPlayCharacterLevelUpAnimation() {
                        playLevelUpAnimation();
                    }

                    @Override
                    public void onShowLevelUpDialog(String petName, int oldLevel, int newLevel, int coinReward) {
                        showLevelUpDialog(petName, oldLevel, newLevel, coinReward);
                    }

                    @Override
                    public void onShowBubbleAction(String message) {
                        bubbleController.showBubbleAction(message);
                    }

                    @Override
                    public void onShowBubbleAction(String message, long durationMs) {
                        bubbleController.showBubbleAction(message, durationMs);
                    }
                },
                HUNGRY_CHARACTER_THRESHOLD
        );

        navigationController = new HomeNavigationController(this);

        tapController = new HomeTapController(
                bubbleHandler,
                new HomeTapController.StateProvider() {
                    @Override
                    public boolean isHatchRunning() {
                        return hatchController.isRunning();
                    }

                    @Override
                    public boolean isCharacterHappyBouncing() {
                        return faceController.isCharacterHappyBouncing();
                    }

                    @Override
                    public int getCurrentStage() {
                        return stateProvider.getCurrentStage();
                    }

                    @Override
                    public int getCurrentHunger() {
                        return stateProvider.getCurrentHunger();
                    }

                    @Override
                    public int getLocalTapCount() {
                        return localTapCount;
                    }
                },
                new HomeTapController.Callback() {
                    @Override
                    public void setLocalTapCount(int value) {
                        localTapCount = value;
                    }

                    @Override
                    public void onPauseIdleAnimation() {
                        pauseIdleAnimation();
                    }

                    @Override
                    public void onResumeIdleAnimationIfPossible() {
                        resumeIdleAnimationIfPossible();
                    }

                    @Override
                    public void onRefreshUi() {
                        refreshHomeUi();
                    }

                    @Override
                    public void showBubbleAction(String message) {
                        bubbleController.showBubbleAction(message);
                    }
                }
        );
    }

    private void bindEvents() {
        navigationController.bindShopButton(btnShop);
        navigationController.bindDefaultNavigation(cardCollectionBook, cardAttendance);

        if (cardFusion != null) {
            cardFusion.setOnClickListener(view -> {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (!isAdded()) return;

                Intent intent = new Intent(requireContext(), com.namgyun.tamakitchen.ui.fusion.FusionActivity.class);
                startActivity(intent);
            });
        }

        if (cardChecklist != null) {
            cardChecklist.setOnClickListener(view -> {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (!isAdded()) return;

                Intent intent = new Intent(requireContext(), ChecklistActivity.class);
                startActivity(intent);
            });
        }

        if (cardFeed != null) {
            cardFeed.setOnClickListener(view -> {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (hatchController.isRunning()) return;
                AppAnalytics.logPetAction(requireContext(), "feed_pet");
                feedController.handleFeedClick();
            });
        }

        tapController.bindMascotTap(mascotLayer, ivFridgeMascot, ivEggSequence);
    }

    private void showLevelUpDialog(String petName, int oldLevel, int newLevel, int coinReward) {
        if (!isAdded()) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_level_up, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvLevelUpTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvLevelUpMessage);
        MaterialButton btnOk = dialogView.findViewById(R.id.btnLevelUpOk);

        if (petName == null || petName.trim().isEmpty()) {
            petName = "펫";
        }

        tvTitle.setText("레벨업");
        tvMessage.setText(
                petName + "가\n"
                        + "Lv" + oldLevel + " → Lv" + newLevel + "로 레벨업했습니다.\n\n"
                        + coinReward + "코인을 얻었습니다."
        );

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            refreshHomeUi();
        });

        dialog.show();
    }

    private void refreshHomeUi() {
        if (!isAdded()) return;
        if (uiBinder == null) return;

        uiBinder.refresh();
        maybeShowFusionGuideIfNeeded();
    }

    private void requestHatchIfPending() {
        bubbleHandler.post(() -> {
            if (!isAdded()) return;
            if (hatchController == null) return;
            if (hatchController.isRunning()) return;

            hatchController.playIfNeeded();

            if (!hatchController.isRunning()) {
                resumeIdleAnimationIfPossible();
            }
        });
    }

    private void playLevelUpAnimation() {
        if (!isAdded()) return;
        if (mascotLayer == null) return;

        final View finalTarget = mascotLayer;

        pauseIdleAnimation();

        finalTarget.animate().cancel();
        finalTarget.setTranslationY(0f);
        finalTarget.setScaleX(1f);
        finalTarget.setScaleY(1f);
        finalTarget.setRotation(0f);

        finalTarget.animate()
                .translationY(-60f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .rotation(-2f)
                .setDuration(220)
                .withEndAction(() -> {
                    if (!isAdded()) return;

                    finalTarget.animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .rotation(0f)
                            .setDuration(240)
                            .withEndAction(this::resumeIdleAnimationIfPossible)
                            .start();
                })
                .start();

        if (flashOverlay != null) {
            flashOverlay.animate().cancel();
            flashOverlay.setAlpha(0f);
            flashOverlay.setVisibility(View.VISIBLE);

            flashOverlay.animate()
                    .alpha(0.18f)
                    .setDuration(90)
                    .withEndAction(() -> flashOverlay.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction(() -> {
                                if (flashOverlay != null) {
                                    flashOverlay.setVisibility(View.GONE);
                                }
                            })
                            .start())
                    .start();
        }
    }

    private void maybeShowFusionGuideIfNeeded() {
        if (!isAdded()) return;
        if (fusionGuideDialogShowing) return;
        if (hatchController != null && hatchController.isRunning()) return;
        if (stateProvider == null) return;

        int stage = stateProvider.getCurrentStage();
        int level = stateProvider.getCurrentLevel();

        if (stage != PetPrefs.STAGE_INGREDIENT) return;
        if (level < PetPrefs.MAX_LEVEL) return;

        String guideKey = resolveFusionGuideKey();
        if (guideKey == null || guideKey.trim().isEmpty()) return;
        if (PetPrefs.hasShownFusionGuide(requireContext(), guideKey)) return;

        String characterName = resolveFusionGuideCharacterName();
        if (characterName == null || characterName.trim().isEmpty()) {
            characterName = "이 재료";
        }

        showFusionGuideDialog(guideKey, characterName);
    }

    private String resolveFusionGuideKey() {
        if (!isAdded()) return null;

        if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
            String selectedKey = stateProvider.getSelectedCollectionKey();
            if (selectedKey == null || selectedKey.trim().isEmpty()) return null;
            if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)) return null;

            CharacterCollectionItem item = CollectionCatalog.findByKey(requireContext(), selectedKey);
            if (item == null) return null;
            if (item.getCategory() != CharacterCollectionCategory.INGREDIENT) return null;

            return "collection:" + selectedKey;
        }

        IngredientCharacter selected = PetPrefs.getSelectedIngredient(requireContext());
        if (selected == null) return null;
        return "main:" + selected.getId();
    }

    private String resolveFusionGuideCharacterName() {
        if (!isAdded()) return null;

        if (stateProvider.isCollectionMode() && !stateProvider.shouldPrioritizeMainPetHatching()) {
            String selectedKey = stateProvider.getSelectedCollectionKey();
            CharacterCollectionItem item = CollectionCatalog.findByKey(requireContext(), selectedKey);
            return item != null ? item.getDisplayName() : null;
        }

        IngredientCharacter selected = PetPrefs.getSelectedIngredient(requireContext());
        return selected != null ? selected.getDisplayName() : null;
    }

    private void showFusionGuideDialog(@NonNull String guideKey, @NonNull String characterName) {
        if (!isAdded()) return;
        if (fusionGuideDialogShowing) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_fusion_guide, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvFusionGuideTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFusionGuideMessage);
        MaterialButton btnLater = dialogView.findViewById(R.id.btnFusionGuideLater);
        MaterialButton btnMove = dialogView.findViewById(R.id.btnFusionGuideMove);

        tvTitle.setText("합성할 수 있어!");
        tvMessage.setText(characterName + "가 MAX에 도달했어.\n이제 합성을 통해 음식 캐릭터로\n진화할 수 있어.");

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        fusionGuideDialogShowing = true;
        PetPrefs.markFusionGuideShown(requireContext(), guideKey);

        dialog.setOnDismissListener(d -> fusionGuideDialogShowing = false);

        btnLater.setOnClickListener(v -> dialog.dismiss());

        btnMove.setOnClickListener(v -> {
            dialog.dismiss();

            if (!isAdded()) return;
            Intent intent = new Intent(requireContext(), com.namgyun.tamakitchen.ui.fusion.FusionActivity.class);
            startActivity(intent);
        });

        dialog.show();
    }

    private void pauseIdleAnimation() {
        if (idleAnimator != null) {
            idleAnimator.stopAll();
        }
    }

    private void resumeIdleAnimationIfPossible() {
        if (!isAdded()) return;
        if (idleAnimator == null) return;
        if (hatchController != null && hatchController.isRunning()) return;
        if (faceController != null && faceController.isCharacterHappyBouncing()) return;
        if (faceController != null && faceController.isShowingCharacterHappyFace()) return;

        boolean showEgg = ivEggSequence != null
                && ivEggSequence.getVisibility() == View.VISIBLE
                && ivEggSequence.getAlpha() > 0.5f;

        boolean showCharacter = ivFridgeMascot != null
                && ivFridgeMascot.getVisibility() == View.VISIBLE
                && ivFridgeMascot.getAlpha() > 0.5f;

        if (showEgg) {
            idleAnimator.startForEgg();
        } else if (showCharacter) {
            int bodyResId = characterResolver.getCurrentCharacterBodyResId();
            int openFaceResId = characterResolver.getCurrentCharacterFaceResId(
                    faceController.isShowingCharacterHappyFace(),
                    faceController.shouldRestoreHungryFaceAfterHappy()
            );
            int blinkFaceResId = characterResolver.getCurrentCharacterBlinkFaceResId(
                    faceController.isShowingCharacterHappyFace(),
                    faceController.isCharacterHappyBouncing(),
                    faceController.shouldRestoreHungryFaceAfterHappy()
            );
            idleAnimator.startForCharacter(bodyResId, openFaceResId, blinkFaceResId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopHungerRefreshLoop();

        if (bubbleController != null) {
            bubbleController.clear();
        }

        if (faceController != null) {
            faceController.clear();
        }

        if (hatchAnimator != null) {
            hatchAnimator.cancel();
        }

        if (idleAnimator != null) {
            idleAnimator.stopAll();
        }

        bubbleHandler.removeCallbacksAndMessages(null);
        fusionGuideDialogShowing = false;

        tvCoins = null;
        btnShop = null;
        tvHomeBubble = null;
        ivBubbleEmotion = null;

        mascotLayer = null;
        ivEggSequence = null;
        ivFridgeMascot = null;
        ivMascotFace = null;

        fxContainer = null;
        flashOverlay = null;

        pbHunger = null;
        tvHungerValue = null;
        tvLevel = null;
        pbExp = null;
        tvExpPercent = null;

        cardFeed = null;
        ivFeed = null;
        tvFeedCount = null;

        cardCollectionBook = null;
        cardAttendance = null;
        cardFusion = null;
        cardChecklist = null;

        hatchAnimator = null;
        idleAnimator = null;

        stateProvider = null;
        characterResolver = null;
        bubbleController = null;
        faceController = null;
        displayController = null;
        hatchController = null;
        uiBinder = null;
        feedController = null;
        navigationController = null;
        tapController = null;
    }
}