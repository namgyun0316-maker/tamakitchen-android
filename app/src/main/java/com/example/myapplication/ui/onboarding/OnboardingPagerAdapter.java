package com.namgyun.tamakitchen.ui.onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    private final StepNicknameFragment stepNicknameFragment = new StepNicknameFragment();
    private final StepFridgeTypeFragment stepFridgeTypeFragment = new StepFridgeTypeFragment();
    private final StepToolsFragment stepToolsFragment = new StepToolsFragment();

    public OnboardingPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return stepNicknameFragment;
        if (position == 1) return stepFridgeTypeFragment;
        return stepToolsFragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public StepToolsFragment getStepToolsFragment() {
        return stepToolsFragment;
    }
}