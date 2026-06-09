package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingActivity;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgeManageActivity;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgePrefs;

import java.util.Locale;

public class FridgeModeManager {

    private static final String TAG = "FridgeModeManager";

    public enum Mode {
        GUEST,
        PERSONAL,
        SHARED
    }

    public Mode getMode(Context ctx) {
        if (ctx == null) return Mode.PERSONAL;
        if (isGuestMode(ctx)) return Mode.GUEST;
        return isSharedFridgeMode(ctx) ? Mode.SHARED : Mode.PERSONAL;
    }

    public boolean isGuestMode(Context ctx) {
        return ctx == null || AuthPrefs.isGuest(ctx);
    }

    public boolean isSharedFridgeMode(Context ctx) {
        if (ctx == null) return false;

        String type = OnboardingPrefs.getFridgeType(ctx);
        if (type == null) type = "";
        type = type.trim().toUpperCase(Locale.ROOT);

        return type.contains("SHARED") || type.contains("COMMON") || type.contains("PUBLIC");
    }

    /**
     * ✅ 공동 냉장고 id는 SharedFridgePrefs(shared_fridge_id)가 “정답”
     * OnboardingPrefs.fridgeId는 fallback만.
     */
    public long getSharedFridgeIdSafe(Context ctx) {
        if (ctx == null) return -1L;

        long sharedId = SharedFridgePrefs.getFridgeId(ctx);
        if (sharedId > 0) return sharedId;

        long fallback = OnboardingPrefs.getFridgeId(ctx);
        if (fallback >= 1_000_000_000_000L) return -1L; // timestamp 형태면 무시
        return fallback;
    }

    public long getUserIdSafe(Context ctx) {
        if (ctx == null) return -1L;
        return AuthPrefs.getUserId(ctx);
    }

    /**
     * ✅ 공동 모드인데 fridgeId가 없을 때 UX 처리
     */
    public void handleMissingSharedFridgeId(Context ctx, Runnable onSwitchToPersonal) {
        if (ctx == null) return;

        SharedFridgePrefs.PendingInvite pending = SharedFridgePrefs.getPendingInvite(ctx);
        if (pending != null && !TextUtils.isEmpty(pending.inviteCode)) {
            try {
                Intent i = new Intent(ctx, SharedFridgeManageActivity.class);
                i.putExtra("pending_invite_code", pending.inviteCode);
                i.putExtra("pending_fridge_name", pending.fridgeName);
                i.putExtra("pending_expires_at", pending.expiresAt);
                ctx.startActivity(i);
            } catch (Exception e) {
                Log.w(TAG, "Cannot open SharedFridgeManageActivity", e);
                Intent i = new Intent(ctx, OnboardingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ctx.startActivity(i);
            }
            return;
        }

        new AlertDialog.Builder(ctx)
                .setTitle("공동 냉장고 정보 없음")
                .setMessage("공동 냉장고 ID가 없어 데이터를 불러올 수 없습니다.\n개인 냉장고로 전환할까요?")
                .setPositiveButton("개인 냉장고로 전환", (d, w) -> {
                    OnboardingPrefs.saveFridgeType(ctx, "PERSONAL");
                    if (onSwitchToPersonal != null) onSwitchToPersonal.run();
                })
                .setNegativeButton("온보딩으로 이동", (d, w) -> {
                    Intent i = new Intent(ctx, OnboardingActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ctx.startActivity(i);
                })
                .show();
    }
}