package com.namgyun.tamakitchen.ui.checklist;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;

import java.lang.reflect.Method;

public class ChecklistActivity extends AppCompatActivity {

    private TextView tabDaily;
    private TextView tabWeekly;
    private TextView tabAchievement;

    private LinearLayout sectionDaily;
    private LinearLayout sectionWeekly;
    private LinearLayout sectionAchievement;

    private LinearLayout rewardFeed;
    private LinearLayout rewardFridgeAdd;
    private LinearLayout rewardExpiryAdd;
    private LinearLayout rewardDeleteUsed;
    private LinearLayout rewardCook;

    private TextView rewardFeedText;
    private TextView rewardFridgeAddText;
    private TextView rewardExpiryAddText;
    private TextView rewardDeleteUsedText;
    private TextView rewardCookText;

    private ImageView rewardFeedCoin;
    private ImageView rewardFridgeAddCoin;
    private ImageView rewardExpiryAddCoin;
    private ImageView rewardDeleteUsedCoin;
    private ImageView rewardCookCoin;

    private TextView tvTodayRewardValue;
    private ImageView ivTodayRewardCoin;
    private LinearLayout todayRewardWrap;
    private TextView tvTodayRewardDesc;

    private TextView tvFeedProgress;
    private TextView tvFridgeAddProgress;
    private TextView tvExpiryAddProgress;
    private TextView tvDeleteUsedProgress;
    private TextView tvCookProgress;

    private LinearLayout rewardWeeklyAttendance;
    private LinearLayout rewardWeeklyCook;
    private LinearLayout rewardWeeklyShopping;
    private LinearLayout rewardWeeklyFeed5;
    private LinearLayout rewardWeeklyFeed10;

    private TextView rewardWeeklyAttendanceText;
    private TextView rewardWeeklyCookText;
    private TextView rewardWeeklyShoppingText;
    private TextView rewardWeeklyFeed5Text;
    private TextView rewardWeeklyFeed10Text;

    private ImageView rewardWeeklyAttendanceCoin;
    private ImageView rewardWeeklyCookCoin;
    private ImageView rewardWeeklyShoppingCoin;
    private ImageView rewardWeeklyFeed5Coin;
    private ImageView rewardWeeklyFeed10Coin;

    private TextView tvWeeklyAttendanceProgress;
    private TextView tvWeeklyCookProgress;
    private TextView tvWeeklyShoppingProgress;
    private TextView tvWeeklyFeed5Progress;
    private TextView tvWeeklyFeed10Progress;

    private TextView tvCookAchievementTitle;
    private TextView tvCookAchievementDesc;
    private TextView tvCookAchievementProgress;
    private LinearLayout rewardCookAchievement;
    private TextView rewardCookAchievementText;
    private ImageView rewardCookAchievementCoin;

    private TextView tvRecipeAchievementTitle;
    private TextView tvRecipeAchievementDesc;
    private TextView tvRecipeAchievementProgress;
    private LinearLayout rewardRecipeAchievement;
    private TextView rewardRecipeAchievementText;
    private ImageView rewardRecipeAchievementCoin;

    private TextView tvFusionAchievementTitle;
    private TextView tvFusionAchievementDesc;
    private TextView tvFusionAchievementProgress;
    private LinearLayout rewardFusionAchievement;
    private TextView rewardFusionAchievementText;
    private ImageView rewardFusionAchievementCoin;

    private TextView tvHatchAchievementTitle;
    private TextView tvHatchAchievementDesc;
    private TextView tvHatchAchievementProgress;
    private LinearLayout rewardHatchAchievement;
    private TextView rewardHatchAchievementText;
    private ImageView rewardHatchAchievementCoin;

    private TextView tvCollectionAchievementTitle;
    private TextView tvCollectionAchievementDesc;
    private TextView tvCollectionAchievementProgress;
    private LinearLayout rewardCollectionAchievement;
    private TextView rewardCollectionAchievementText;
    private ImageView rewardCollectionAchievementCoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist);

        bindViews();
        bindEvents();
        selectTab(TabType.DAILY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void bindViews() {
        tabDaily = findViewById(R.id.tabDaily);
        tabWeekly = findViewById(R.id.tabWeekly);
        tabAchievement = findViewById(R.id.tabAchievement);

        sectionDaily = findViewById(R.id.sectionDaily);
        sectionWeekly = findViewById(R.id.sectionWeekly);
        sectionAchievement = findViewById(R.id.sectionAchievement);

        rewardFeed = findViewById(R.id.rewardFeed);
        rewardFridgeAdd = findViewById(R.id.rewardFridgeAdd);
        rewardExpiryAdd = findViewById(R.id.rewardExpiryAdd);
        rewardDeleteUsed = findViewById(R.id.rewardDeleteUsed);
        rewardCook = findViewById(R.id.rewardCook);

        rewardFeedText = findViewById(R.id.rewardFeedText);
        rewardFridgeAddText = findViewById(R.id.rewardFridgeAddText);
        rewardExpiryAddText = findViewById(R.id.rewardExpiryAddText);
        rewardDeleteUsedText = findViewById(R.id.rewardDeleteUsedText);
        rewardCookText = findViewById(R.id.rewardCookText);

        rewardFeedCoin = findViewById(R.id.rewardFeedCoin);
        rewardFridgeAddCoin = findViewById(R.id.rewardFridgeAddCoin);
        rewardExpiryAddCoin = findViewById(R.id.rewardExpiryAddCoin);
        rewardDeleteUsedCoin = findViewById(R.id.rewardDeleteUsedCoin);
        rewardCookCoin = findViewById(R.id.rewardCookCoin);

        tvTodayRewardValue = findViewById(R.id.tvTodayRewardValue);
        ivTodayRewardCoin = findViewById(R.id.ivTodayRewardCoin);
        todayRewardWrap = findViewById(R.id.todayRewardWrap);
        tvTodayRewardDesc = findViewById(R.id.tvTodayRewardDesc);

        tvFeedProgress = findViewById(R.id.tvFeedProgress);
        tvFridgeAddProgress = findViewById(R.id.tvFridgeAddProgress);
        tvExpiryAddProgress = findViewById(R.id.tvExpiryAddProgress);
        tvDeleteUsedProgress = findViewById(R.id.tvDeleteUsedProgress);
        tvCookProgress = findViewById(R.id.tvCookProgress);

        rewardWeeklyAttendance = findViewById(R.id.rewardWeeklyAttendance);
        rewardWeeklyCook = findViewById(R.id.rewardWeeklyCook);
        rewardWeeklyShopping = findViewById(R.id.rewardWeeklyShopping);
        rewardWeeklyFeed5 = findViewById(R.id.rewardWeeklyFeed5);
        rewardWeeklyFeed10 = findViewById(R.id.rewardWeeklyFeed10);

        rewardWeeklyAttendanceText = findViewById(R.id.rewardWeeklyAttendanceText);
        rewardWeeklyCookText = findViewById(R.id.rewardWeeklyCookText);
        rewardWeeklyShoppingText = findViewById(R.id.rewardWeeklyShoppingText);
        rewardWeeklyFeed5Text = findViewById(R.id.rewardWeeklyFeed5Text);
        rewardWeeklyFeed10Text = findViewById(R.id.rewardWeeklyFeed10Text);

        rewardWeeklyAttendanceCoin = findViewById(R.id.rewardWeeklyAttendanceCoin);
        rewardWeeklyCookCoin = findViewById(R.id.rewardWeeklyCookCoin);
        rewardWeeklyShoppingCoin = findViewById(R.id.rewardWeeklyShoppingCoin);
        rewardWeeklyFeed5Coin = findViewById(R.id.rewardWeeklyFeed5Coin);
        rewardWeeklyFeed10Coin = findViewById(R.id.rewardWeeklyFeed10Coin);

        tvWeeklyAttendanceProgress = findViewById(R.id.tvWeeklyAttendanceProgress);
        tvWeeklyCookProgress = findViewById(R.id.tvWeeklyCookProgress);
        tvWeeklyShoppingProgress = findViewById(R.id.tvWeeklyShoppingProgress);
        tvWeeklyFeed5Progress = findViewById(R.id.tvWeeklyFeed5Progress);
        tvWeeklyFeed10Progress = findViewById(R.id.tvWeeklyFeed10Progress);

        tvCookAchievementTitle = findViewById(R.id.tvCookAchievementTitle);
        tvCookAchievementDesc = findViewById(R.id.tvCookAchievementDesc);
        tvCookAchievementProgress = findViewById(R.id.tvCookAchievementProgress);
        rewardCookAchievement = findViewById(R.id.rewardCookAchievement);
        rewardCookAchievementText = findViewById(R.id.rewardCookAchievementText);
        rewardCookAchievementCoin = findViewById(R.id.rewardCookAchievementCoin);

        tvRecipeAchievementTitle = findViewById(R.id.tvRecipeAchievementTitle);
        tvRecipeAchievementDesc = findViewById(R.id.tvRecipeAchievementDesc);
        tvRecipeAchievementProgress = findViewById(R.id.tvRecipeAchievementProgress);
        rewardRecipeAchievement = findViewById(R.id.rewardRecipeAchievement);
        rewardRecipeAchievementText = findViewById(R.id.rewardRecipeAchievementText);
        rewardRecipeAchievementCoin = findViewById(R.id.rewardRecipeAchievementCoin);

        tvFusionAchievementTitle = findViewById(R.id.tvFusionAchievementTitle);
        tvFusionAchievementDesc = findViewById(R.id.tvFusionAchievementDesc);
        tvFusionAchievementProgress = findViewById(R.id.tvFusionAchievementProgress);
        rewardFusionAchievement = findViewById(R.id.rewardFusionAchievement);
        rewardFusionAchievementText = findViewById(R.id.rewardFusionAchievementText);
        rewardFusionAchievementCoin = findViewById(R.id.rewardFusionAchievementCoin);

        tvHatchAchievementTitle = findViewById(R.id.tvHatchAchievementTitle);
        tvHatchAchievementDesc = findViewById(R.id.tvHatchAchievementDesc);
        tvHatchAchievementProgress = findViewById(R.id.tvHatchAchievementProgress);
        rewardHatchAchievement = findViewById(R.id.rewardHatchAchievement);
        rewardHatchAchievementText = findViewById(R.id.rewardHatchAchievementText);
        rewardHatchAchievementCoin = findViewById(R.id.rewardHatchAchievementCoin);

        tvCollectionAchievementTitle = findViewById(R.id.tvCollectionAchievementTitle);
        tvCollectionAchievementDesc = findViewById(R.id.tvCollectionAchievementDesc);
        tvCollectionAchievementProgress = findViewById(R.id.tvCollectionAchievementProgress);
        rewardCollectionAchievement = findViewById(R.id.rewardCollectionAchievement);
        rewardCollectionAchievementText = findViewById(R.id.rewardCollectionAchievementText);
        rewardCollectionAchievementCoin = findViewById(R.id.rewardCollectionAchievementCoin);
    }

    private void bindEvents() {
        tabDaily.setOnClickListener(v -> selectTab(TabType.DAILY));
        tabWeekly.setOnClickListener(v -> selectTab(TabType.WEEKLY));
        tabAchievement.setOnClickListener(v -> selectTab(TabType.ACHIEVEMENT));

        rewardFeed.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimFeedReward(this, getCurrentLevelSafe(this))
        ));
        rewardFridgeAdd.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimFridgeAddReward(this, getCurrentLevelSafe(this))
        ));
        rewardExpiryAdd.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimExpiryAddReward(this, getCurrentLevelSafe(this))
        ));
        rewardDeleteUsed.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimDeleteReward(this, getCurrentLevelSafe(this))
        ));
        rewardCook.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimCookReward(this, getCurrentLevelSafe(this))
        ));

        rewardWeeklyAttendance.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimWeeklyAttendanceReward(this)
        ));
        rewardWeeklyCook.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimWeeklyCookReward(this)
        ));
        rewardWeeklyShopping.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimWeeklyShoppingReward(this)
        ));
        rewardWeeklyFeed5.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimWeeklyFeed5Reward(this)
        ));
        rewardWeeklyFeed10.setOnClickListener(v -> claim(() ->
                ChecklistPrefs.claimWeeklyFeed10Reward(this)
        ));

        rewardCookAchievement.setOnClickListener(v -> {
            int target = ChecklistPrefs.getCookAchievementTarget(this);
            int reward = ChecklistPrefs.claimCookAchievementReward(this, target);
            if (reward > 0) {
                addCoinsSafely(reward);
                AppToast.show(this, reward + " 코인을 받았어!");
            }
            refreshUi();
        });

        rewardRecipeAchievement.setOnClickListener(v -> {
            int target = ChecklistPrefs.getRecipeAchievementTarget(this);
            int reward = ChecklistPrefs.claimRecipeAchievementReward(this, target);
            if (reward > 0) {
                addCoinsSafely(reward);
                AppToast.show(this, reward + " 코인을 받았어!");
            }
            refreshUi();
        });

        rewardFusionAchievement.setOnClickListener(v -> {
            int target = ChecklistPrefs.getFusionAchievementTarget(this);
            int reward = ChecklistPrefs.claimFusionAchievementReward(this, target);
            if (reward > 0) {
                addCoinsSafely(reward);
                AppToast.show(this, reward + " 코인을 받았어!");
            }
            refreshUi();
        });

        rewardHatchAchievement.setOnClickListener(v -> {
            int target = ChecklistPrefs.getHatchAchievementTarget(this);
            int reward = ChecklistPrefs.claimHatchAchievementReward(this, target);
            if (reward > 0) {
                addCoinsSafely(reward);
                AppToast.show(this, reward + " 코인을 받았어!");
            }
            refreshUi();
        });

        rewardCollectionAchievement.setOnClickListener(v -> {
            int target = ChecklistPrefs.getCollectionAchievementTarget(this);
            int reward = ChecklistPrefs.claimCollectionAchievementReward(this, target);
            if (reward > 0) {
                addCoinsSafely(reward);
                AppToast.show(this, reward + " 코인을 받았어!");
            }
            refreshUi();
        });
    }

    private void selectTab(TabType tabType) {
        int selectedBg = R.drawable.bg_reward_pill_selected_pastel;

        tabDaily.setBackgroundResource(tabType == TabType.DAILY ? selectedBg : android.R.color.transparent);
        tabWeekly.setBackgroundResource(tabType == TabType.WEEKLY ? selectedBg : android.R.color.transparent);
        tabAchievement.setBackgroundResource(tabType == TabType.ACHIEVEMENT ? selectedBg : android.R.color.transparent);

        tabDaily.setTextColor(tabType == TabType.DAILY ? 0xFF1F1F1F : 0xFF7A8594);
        tabWeekly.setTextColor(tabType == TabType.WEEKLY ? 0xFF1F1F1F : 0xFF7A8594);
        tabAchievement.setTextColor(tabType == TabType.ACHIEVEMENT ? 0xFF1F1F1F : 0xFF7A8594);

        sectionDaily.setVisibility(tabType == TabType.DAILY ? View.VISIBLE : View.GONE);
        sectionWeekly.setVisibility(tabType == TabType.WEEKLY ? View.VISIBLE : View.GONE);
        sectionAchievement.setVisibility(tabType == TabType.ACHIEVEMENT ? View.VISIBLE : View.GONE);
    }

    private void claim(ClaimAction action) {
        int reward = action.run();
        if (reward > 0) {
            addCoinsSafely(reward);
            AppToast.show(this, reward + " 코인을 받았어!");
        }
        refreshUi();
    }

    private void refreshUi() {
        int level = getCurrentLevelSafe(this);
        ChecklistPrefs.ensureToday(this, level);
        ChecklistPrefs.ensureWeek(this);

        boolean feedDone = ChecklistPrefs.isFeedDone(this, level);
        boolean fridgeAddDone = ChecklistPrefs.isFridgeAddDone(this, level);
        boolean expiryAddDone = ChecklistPrefs.isExpiryAddDone(this, level);
        boolean deleteDone = ChecklistPrefs.isDeleteDone(this, level);
        boolean cookDone = ChecklistPrefs.isCookDone(this, level);

        boolean feedClaimed = ChecklistPrefs.isFeedClaimed(this, level);
        boolean fridgeAddClaimed = ChecklistPrefs.isFridgeAddClaimed(this, level);
        boolean expiryAddClaimed = ChecklistPrefs.isExpiryAddClaimed(this, level);
        boolean deleteClaimed = ChecklistPrefs.isDeleteClaimed(this, level);
        boolean cookClaimed = ChecklistPrefs.isCookClaimed(this, level);

        updateMission(rewardFeed, rewardFeedText, rewardFeedCoin, tvFeedProgress,
                feedDone, feedClaimed, ChecklistPrefs.REWARD_FEED);
        updateMission(rewardFridgeAdd, rewardFridgeAddText, rewardFridgeAddCoin, tvFridgeAddProgress,
                fridgeAddDone, fridgeAddClaimed, ChecklistPrefs.REWARD_FRIDGE_ADD);
        updateMission(rewardExpiryAdd, rewardExpiryAddText, rewardExpiryAddCoin, tvExpiryAddProgress,
                expiryAddDone, expiryAddClaimed, ChecklistPrefs.REWARD_EXPIRY_ADD);
        updateMission(rewardDeleteUsed, rewardDeleteUsedText, rewardDeleteUsedCoin, tvDeleteUsedProgress,
                deleteDone, deleteClaimed, ChecklistPrefs.REWARD_DELETE);
        updateMission(rewardCook, rewardCookText, rewardCookCoin, tvCookProgress,
                cookDone, cookClaimed, ChecklistPrefs.REWARD_COOK);

        updateWeeklyMission(
                rewardWeeklyAttendance, rewardWeeklyAttendanceText, rewardWeeklyAttendanceCoin,
                tvWeeklyAttendanceProgress,
                ChecklistPrefs.getWeeklyAttendanceCount(this), 5,
                ChecklistPrefs.isWeeklyAttendanceClaimed(this),
                ChecklistPrefs.REWARD_WEEKLY_ATTENDANCE
        );
        updateWeeklyMission(
                rewardWeeklyCook, rewardWeeklyCookText, rewardWeeklyCookCoin,
                tvWeeklyCookProgress,
                ChecklistPrefs.getWeeklyCookCount(this), 2,
                ChecklistPrefs.isWeeklyCookClaimed(this),
                ChecklistPrefs.REWARD_WEEKLY_COOK
        );
        updateWeeklyMission(
                rewardWeeklyShopping, rewardWeeklyShoppingText, rewardWeeklyShoppingCoin,
                tvWeeklyShoppingProgress,
                ChecklistPrefs.getWeeklyShoppingCount(this), 1,
                ChecklistPrefs.isWeeklyShoppingClaimed(this),
                ChecklistPrefs.REWARD_WEEKLY_SHOPPING
        );
        updateWeeklyMission(
                rewardWeeklyFeed5, rewardWeeklyFeed5Text, rewardWeeklyFeed5Coin,
                tvWeeklyFeed5Progress,
                ChecklistPrefs.getWeeklyFeedCount(this), 5,
                ChecklistPrefs.isWeeklyFeed5Claimed(this),
                ChecklistPrefs.REWARD_WEEKLY_FEED_5
        );
        updateWeeklyMission(
                rewardWeeklyFeed10, rewardWeeklyFeed10Text, rewardWeeklyFeed10Coin,
                tvWeeklyFeed10Progress,
                ChecklistPrefs.getWeeklyFeedCount(this), 10,
                ChecklistPrefs.isWeeklyFeed10Claimed(this),
                ChecklistPrefs.REWARD_WEEKLY_FEED_10
        );

        int total = ChecklistPrefs.getTodayClaimedCoins(this, level);
        if (total > 0) {
            todayRewardWrap.setVisibility(View.VISIBLE);
            tvTodayRewardValue.setText(String.valueOf(total));
            ivTodayRewardCoin.setVisibility(View.VISIBLE);
            tvTodayRewardDesc.setText("오늘 받은 보상이 여기에 누적돼!");
        } else {
            todayRewardWrap.setVisibility(View.GONE);
            tvTodayRewardDesc.setText("아직 받은 보상이 없어.");
        }

        bindAchievementUi();
    }

    private void bindAchievementUi() {
        int cookCount = ChecklistPrefs.getCookCount(this);
        int cookTarget = ChecklistPrefs.getCookAchievementTarget(this);
        int cookReward = ChecklistPrefs.getCookAchievementReward(cookTarget);
        boolean cookClaimed = ChecklistPrefs.isCookAchievementClaimed(this, cookTarget);

        tvCookAchievementTitle.setText(ChecklistPrefs.getCookAchievementTitleByTarget(cookTarget));
        tvCookAchievementDesc.setText(ChecklistPrefs.getCookAchievementDescByTarget(cookTarget));
        tvCookAchievementProgress.setText(cookCount + " / " + cookTarget);
        updateAchievementReward(rewardCookAchievement, rewardCookAchievementText, rewardCookAchievementCoin,
                cookCount >= cookTarget, cookClaimed, cookReward);

        int recipeCount = ChecklistPrefs.getRecipeCreateCount(this);
        int recipeTarget = ChecklistPrefs.getRecipeAchievementTarget(this);
        int recipeReward = ChecklistPrefs.getRecipeAchievementReward(recipeTarget);
        boolean recipeClaimed = ChecklistPrefs.isRecipeAchievementClaimed(this, recipeTarget);

        tvRecipeAchievementTitle.setText(ChecklistPrefs.getRecipeAchievementTitleByTarget(recipeTarget));
        tvRecipeAchievementDesc.setText(ChecklistPrefs.getRecipeAchievementDescByTarget(recipeTarget));
        tvRecipeAchievementProgress.setText(recipeCount + " / " + recipeTarget);
        updateAchievementReward(rewardRecipeAchievement, rewardRecipeAchievementText, rewardRecipeAchievementCoin,
                recipeCount >= recipeTarget, recipeClaimed, recipeReward);

        int fusionCount = ChecklistPrefs.getFusionCount(this);
        int fusionTarget = ChecklistPrefs.getFusionAchievementTarget(this);
        int fusionReward = ChecklistPrefs.getFusionAchievementReward(fusionTarget);
        boolean fusionClaimed = ChecklistPrefs.isFusionAchievementClaimed(this, fusionTarget);

        tvFusionAchievementTitle.setText(ChecklistPrefs.getFusionAchievementTitleByTarget(fusionTarget));
        tvFusionAchievementDesc.setText(ChecklistPrefs.getFusionAchievementDescByTarget(fusionTarget));
        tvFusionAchievementProgress.setText(fusionCount + " / " + fusionTarget);
        updateAchievementReward(rewardFusionAchievement, rewardFusionAchievementText, rewardFusionAchievementCoin,
                fusionCount >= fusionTarget, fusionClaimed, fusionReward);

        int hatchCount = ChecklistPrefs.getHatchCount(this);
        int hatchTarget = ChecklistPrefs.getHatchAchievementTarget(this);
        int hatchReward = ChecklistPrefs.getHatchAchievementReward(hatchTarget);
        boolean hatchClaimed = ChecklistPrefs.isHatchAchievementClaimed(this, hatchTarget);

        tvHatchAchievementTitle.setText(ChecklistPrefs.getHatchAchievementTitleByTarget(hatchTarget));
        tvHatchAchievementDesc.setText(ChecklistPrefs.getHatchAchievementDescByTarget(hatchTarget));
        tvHatchAchievementProgress.setText(hatchCount + " / " + hatchTarget);
        updateAchievementReward(rewardHatchAchievement, rewardHatchAchievementText, rewardHatchAchievementCoin,
                hatchCount >= hatchTarget, hatchClaimed, hatchReward);

        int collectionCount = ChecklistPrefs.getUnlockedCollectionCount(this);
        int collectionTarget = ChecklistPrefs.getCollectionAchievementTarget(this);
        int collectionReward = ChecklistPrefs.getCollectionAchievementReward(collectionTarget);
        boolean collectionClaimed = ChecklistPrefs.isCollectionAchievementClaimed(this, collectionTarget);

        tvCollectionAchievementTitle.setText(ChecklistPrefs.getCollectionAchievementTitleByTarget(collectionTarget));
        tvCollectionAchievementDesc.setText(ChecklistPrefs.getCollectionAchievementDescByTarget(collectionTarget));
        tvCollectionAchievementProgress.setText(collectionCount + " / " + collectionTarget);
        updateAchievementReward(rewardCollectionAchievement, rewardCollectionAchievementText, rewardCollectionAchievementCoin,
                collectionCount >= collectionTarget, collectionClaimed, collectionReward);
    }

    private void updateMission(
            LinearLayout btn,
            TextView text,
            ImageView coin,
            TextView progress,
            boolean done,
            boolean claimed,
            int reward
    ) {
        progress.setText(done ? "1 / 1" : "0 / 1");

        if (claimed) {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_disabled);
            text.setText("완료");
            text.setTextColor(0xFF7A8594);
            coin.setVisibility(View.GONE);
            btn.setEnabled(false);
        } else if (done) {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_enabled);
            text.setText("받기");
            text.setTextColor(0xFF1F1F1F);
            coin.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
        } else {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_disabled);
            text.setText(String.valueOf(reward));
            text.setTextColor(0xFF7A8594);
            coin.setVisibility(View.VISIBLE);
            btn.setEnabled(false);
        }
    }

    private void updateWeeklyMission(
            LinearLayout btn,
            TextView text,
            ImageView coin,
            TextView progress,
            int current,
            int target,
            boolean claimed,
            int reward
    ) {
        progress.setText(current + " / " + target);

        if (claimed) {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_disabled);
            text.setText("완료");
            text.setTextColor(0xFF7A8594);
            coin.setVisibility(View.GONE);
            btn.setEnabled(false);
        } else if (current >= target) {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_enabled);
            text.setText("받기");
            text.setTextColor(0xFF1F1F1F);
            coin.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
        } else {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_disabled);
            text.setText(String.valueOf(reward));
            text.setTextColor(0xFF7A8594);
            coin.setVisibility(View.VISIBLE);
            btn.setEnabled(false);
        }
    }

    private void updateAchievementReward(
            LinearLayout btn,
            TextView text,
            ImageView coin,
            boolean done,
            boolean claimed,
            int reward
    ) {
        if (claimed) {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_disabled);
            text.setText("완료");
            text.setTextColor(0xFF7A8594);
            coin.setVisibility(View.GONE);
            btn.setEnabled(false);
        } else if (done) {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_enabled);
            text.setText("받기");
            text.setTextColor(0xFF1F1F1F);
            coin.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
        } else {
            btn.setBackgroundResource(R.drawable.bg_reward_pill_disabled);
            text.setText(String.valueOf(reward));
            text.setTextColor(0xFF7A8594);
            coin.setVisibility(View.VISIBLE);
            btn.setEnabled(false);
        }
    }

    private int getCurrentLevelSafe(Context context) {
        try {
            Class<?> clazz = Class.forName("com.namgyun.tamakitchen.pet.PetPrefs");
            try {
                Method m = clazz.getMethod("getLevel", Context.class);
                return (int) m.invoke(null, context);
            } catch (Exception ignored) {}
            try {
                Method m = clazz.getMethod("getCurrentLevel", Context.class);
                return (int) m.invoke(null, context);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return 1;
    }

    private void addCoinsSafely(int amount) {
        try {
            Class<?> clazz = Class.forName("com.namgyun.tamakitchen.pet.PetPrefs");
            try {
                Method m = clazz.getMethod("addCoins", Context.class, int.class);
                m.invoke(null, this, amount);
                return;
            } catch (Exception ignored) {}

            try {
                Method getCoins = clazz.getMethod("getCoins", Context.class);
                Method setCoins = clazz.getMethod("setCoins", Context.class, int.class);
                Object currentObj = getCoins.invoke(null, this);
                int current = (currentObj instanceof Integer) ? (Integer) currentObj : 0;
                setCoins.invoke(null, this, current + amount);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private interface ClaimAction {
        int run();
    }

    private enum TabType {
        DAILY, WEEKLY, ACHIEVEMENT
    }
}