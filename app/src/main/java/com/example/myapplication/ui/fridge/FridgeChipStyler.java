package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;

import com.namgyun.tamakitchen.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class FridgeChipStyler {

    private FridgeChipStyler() {}

    public static void apply(Context ctx, ChipGroup chipGroup) {
        if (ctx == null || chipGroup == null) return;

        // ✅ 단일 선택 강제
        chipGroup.setSingleSelection(true);
        chipGroup.setSelectionRequired(true);

        int[] chipIds = new int[]{
                R.id.chip_all,
                R.id.chip_fridge,
                R.id.chip_freezer,
                R.id.chip_room,
                R.id.chip_other
        };

        // 색
        final int colorNormal = Color.parseColor("#87CEEB");
        final int colorChecked = Color.parseColor("#55B6E8");

        final int[][] bgStates = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        final int[] bgColors = new int[]{ colorChecked, colorNormal };
        final ColorStateList bgList = new ColorStateList(bgStates, bgColors);

        Typeface jua = null;
        try { jua = ResourcesCompat.getFont(ctx, R.font.jua_regular); }
        catch (Exception ignored) {}

        // ✅ “납작한” 느낌
        final int chipHeight = dp(ctx, 38);
        final float corner = dp(ctx, 12);

        // ✅ 칩 사이 간격 (너 레이아웃 느낌 유지)
        final int spacing = dp(ctx, 10);

        // 1) 공통 스타일 먼저 적용 (너비는 나중에)
        for (int id : chipIds) {
            View v = chipGroup.findViewById(id);
            if (!(v instanceof Chip)) continue;
            Chip chip = (Chip) v;

            // ✅ 동그랗게 보이게 만드는 최소 터치 타겟 강제 끄기
            chip.setEnsureMinTouchTargetSize(false);

            // 높이
            ViewGroup.LayoutParams lp = chip.getLayoutParams();
            if (lp != null) {
                lp.height = chipHeight;
                chip.setLayoutParams(lp);
            }
            chip.setMinHeight(chipHeight);
            chip.setMinimumHeight(chipHeight);

            // 모양/정렬
            chip.setChipCornerRadius(corner);
            chip.setGravity(Gravity.CENTER);
            chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            // 패딩 최소화
            chip.setChipStartPadding(0f);
            chip.setChipEndPadding(0f);
            chip.setTextStartPadding(0f);
            chip.setTextEndPadding(0f);

            // 색
            chip.setChipBackgroundColor(bgList);
            chip.setTextColor(Color.WHITE);

            // 폰트
            if (jua != null) chip.setTypeface(jua);

            // 체크 아이콘 제거
            chip.setCheckedIconVisible(false);

            // 눌림 애니메이션
            chip.setOnTouchListener((vv, ev) -> {
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    vv.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                    vv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
                return false;
            });
        }

        // 2) ✅ 레이아웃 완료 후 "ChipGroup 실제 너비" 기준으로 균등 분배
        chipGroup.post(() -> {
            int groupW = chipGroup.getWidth();
            if (groupW <= 0) return;

            int available = groupW - chipGroup.getPaddingLeft() - chipGroup.getPaddingRight();
            if (available <= 0) return;

            int count = chipIds.length;
            int totalSpacing = spacing * (count - 1);

            // 너무 빡빡하면 spacing을 줄여서라도 안 잘리게
            int each = (available - totalSpacing) / count;
            if (each < dp(ctx, 54)) {
                int spacing2 = dp(ctx, 6);
                totalSpacing = spacing2 * (count - 1);
                each = (available - totalSpacing) / count;
            }

            // 그래도 최소 보장 (아주 작은 화면 대비)
            each = Math.max(dp(ctx, 48), each);

            for (int id : chipIds) {
                View v = chipGroup.findViewById(id);
                if (!(v instanceof Chip)) continue;
                Chip chip = (Chip) v;

                ViewGroup.LayoutParams lp = chip.getLayoutParams();
                if (lp != null) {
                    lp.width = each;
                    chip.setLayoutParams(lp);
                } else {
                    chip.setMinimumWidth(each);
                }
            }
        });
    }

    private static int dp(Context ctx, int dp) {
        float d = ctx.getResources().getDisplayMetrics().density;
        return (int) (dp * d + 0.5f);
    }
}