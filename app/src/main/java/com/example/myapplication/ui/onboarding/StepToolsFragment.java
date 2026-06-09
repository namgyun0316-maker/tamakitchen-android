package com.namgyun.tamakitchen.ui.onboarding;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.R;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StepToolsFragment extends Fragment {

    private final Set<String> selectedTools = new HashSet<>();

    // key -> cardId 매핑
    private static class ToolIds {
        static final String AIR_FRYER = "air_fryer";
        static final String OVEN = "oven";
        static final String MICROWAVE = "microwave";
        static final String BLENDER = "blender";
        static final String RICE_COOKER = "rice_cooker";
            static final String STEAMER = "steamer";
        static final String GAS_RANGE = "gas_range";
        static final String INDUCTION = "induction";
    }

    private final Map<String, Integer> viewIdByKey = new HashMap<String, Integer>() {{
        put(ToolIds.AIR_FRYER, R.id.toolAirFryer);
        put(ToolIds.OVEN, R.id.toolOven);
        put(ToolIds.MICROWAVE, R.id.toolMicrowave);
        put(ToolIds.BLENDER, R.id.toolBlender);
        put(ToolIds.RICE_COOKER, R.id.toolRiceCooker);
        put(ToolIds.STEAMER, R.id.toolSteamer);
        put(ToolIds.GAS_RANGE, R.id.toolGasRange);
        put(ToolIds.INDUCTION, R.id.toolInduction);
    }};

    private final Map<String, MaterialCardView> cardByKey = new HashMap<>();

    private boolean pendingReset = false;

    public StepToolsFragment() {
        super(R.layout.fragment_onboarding_tools);
    }

    public void requestResetSelection() {
        pendingReset = true;
        if (isAdded()) {
            applyPendingResetIfNeeded();
            renderStylesOnly();
            notifyToolsChangedToActivity(); // ✅ 리셋 직후 반영
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardByKey.clear();

        for (Map.Entry<String, Integer> e : viewIdByKey.entrySet()) {
            String key = e.getKey();
            int id = e.getValue();

            View root = view.findViewById(id);
            if (!(root instanceof MaterialCardView)) continue;

            MaterialCardView card = (MaterialCardView) root;
            cardByKey.put(key, card);

            View.OnClickListener toggle = v -> {
                boolean nowSelected = toggleKey(key);
                applyCardStyle(card, nowSelected, true);

                OnboardingPrefs.saveTools(requireContext(), selectedTools);

                // ✅ Activity에 선택 상태 전달 + 버튼 상태 업데이트
                OnboardingActivity a = (OnboardingActivity) requireActivity();
                a.setTools(selectedTools);
                a.onToolsSelectionChanged(selectedTools.size());
            };

            card.setOnClickListener(toggle);
        }

        applyPendingResetIfNeeded();
        renderStylesOnly();
        notifyToolsChangedToActivity(); // ✅ 최초 진입 시도 반영
    }

    @Override
    public void onResume() {
        super.onResume();
        applyPendingResetIfNeeded();
        renderStylesOnly();
        notifyToolsChangedToActivity();
    }

    private void notifyToolsChangedToActivity() {
        if (!isAdded()) return;
        try {
            OnboardingActivity a = (OnboardingActivity) requireActivity();
            a.setTools(selectedTools);
            a.onToolsSelectionChanged(selectedTools.size());
        } catch (Throwable ignored) {}
    }

    private void applyPendingResetIfNeeded() {
        if (!pendingReset) return;
        pendingReset = false;

        selectedTools.clear();

        if (isAdded()) {
            OnboardingPrefs.saveTools(requireContext(), selectedTools);

            OnboardingActivity act = (OnboardingActivity) getActivity();
            if (act != null) act.setTools(selectedTools);
        }

        for (MaterialCardView c : cardByKey.values()) {
            applyCardStyle(c, false, false);
        }
    }

    private void renderStylesOnly() {
        if (!isAdded()) return;
        for (Map.Entry<String, MaterialCardView> e : cardByKey.entrySet()) {
            boolean selected = selectedTools.contains(e.getKey());
            applyCardStyle(e.getValue(), selected, false);
        }
    }

    private boolean toggleKey(String key) {
        if (selectedTools.contains(key)) {
            selectedTools.remove(key);
            return false;
        }
        selectedTools.add(key);
        return true;
    }

    private void applyCardStyle(@NonNull MaterialCardView card, boolean selected, boolean animate) {
        int strokeSelected = ContextCompat.getColor(requireContext(), R.color.pascal_blue_light);
        int strokeNormal = Color.parseColor("#E3EAF2");
        int bg = Color.parseColor("#FFFFFF");

        int targetStrokeW = dp(selected ? 3 : 1);
        int targetStrokeColor = selected ? strokeSelected : strokeNormal;

        float targetScale = selected ? 1.02f : 1.0f;
        float targetElevation = selected ? dpF(4) : 0f;

        card.setCardBackgroundColor(ColorStateList.valueOf(bg));
        card.setStrokeColor(targetStrokeColor);

        if (!animate) {
            card.setStrokeWidth(targetStrokeW);
            card.setScaleX(targetScale);
            card.setScaleY(targetScale);
            card.setCardElevation(targetElevation);
            return;
        }

        int fromStrokeW = card.getStrokeWidth();
        float fromScale = card.getScaleX();
        float fromElevation = card.getCardElevation();

        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(160);
        va.setInterpolator(new AccelerateDecelerateInterpolator());
        va.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            card.setStrokeWidth((int) (fromStrokeW + (targetStrokeW - fromStrokeW) * t));

            float s = fromScale + (targetScale - fromScale) * t;
            card.setScaleX(s);
            card.setScaleY(s);

            card.setCardElevation(fromElevation + (targetElevation - fromElevation) * t);
        });
        va.start();
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }

    private float dpF(int v) {
        float d = getResources().getDisplayMetrics().density;
        return v * d;
    }
}