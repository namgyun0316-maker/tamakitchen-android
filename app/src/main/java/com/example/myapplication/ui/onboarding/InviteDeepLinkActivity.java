package com.namgyun.tamakitchen.ui.onboarding;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.namgyun.tamakitchen.ui.auth.LoginActivity;
import com.namgyun.tamakitchen.ui.common.AppToast;

import java.util.Locale;

public class InviteDeepLinkActivity extends AppCompatActivity {

    public static final String EXTRA_INVITE_CODE = "extra_invite_code";
    public static final String EXTRA_FRIDGE_NAME = "extra_fridge_name";
    public static final String EXTRA_EXPIRES_AT = "extra_expires_at";

    private static final String KEY_INVITE_CODE = "inviteCode";
    private static final String KEY_FRIDGE_NAME = "fridgeName";
    private static final String KEY_EXPIRES_AT = "expiresAt";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent in = getIntent();
        Uri data = (in != null) ? in.getData() : null;

        String inviteCode = null;
        String fridgeName = null;
        long expiresAt = 0L;

        if (data != null) {
            inviteCode = safeGetQuery(data, KEY_INVITE_CODE);
            fridgeName = safeGetQuery(data, KEY_FRIDGE_NAME);

            String exp = safeGetQuery(data, KEY_EXPIRES_AT);
            if (exp != null) {
                try {
                    expiresAt = Long.parseLong(exp);
                } catch (Exception ignored) {
                }
            }

            if (isEmpty(inviteCode)) {
                inviteCode = safeGetQuery(data, "code");
            }
        }

        if (isEmpty(inviteCode) && in != null) {
            inviteCode = in.getStringExtra(KEY_INVITE_CODE);
            if (inviteCode == null && in.getExtras() != null) {
                Object o = in.getExtras().get(KEY_INVITE_CODE);
                if (o != null) inviteCode = String.valueOf(o);
            }

            fridgeName = in.getStringExtra(KEY_FRIDGE_NAME);
            if (fridgeName == null && in.getExtras() != null) {
                Object o = in.getExtras().get(KEY_FRIDGE_NAME);
                if (o != null) fridgeName = String.valueOf(o);
            }

            String exp = in.getStringExtra(KEY_EXPIRES_AT);
            if (exp == null && in.getExtras() != null) {
                Object o = in.getExtras().get(KEY_EXPIRES_AT);
                if (o != null) exp = String.valueOf(o);
            }

            if (exp != null) {
                try {
                    expiresAt = Long.parseLong(exp);
                } catch (Exception ignored) {
                }
            }
        }

        if (inviteCode == null) inviteCode = "";
        inviteCode = inviteCode.trim().toUpperCase(Locale.ROOT);

        if (inviteCode.isEmpty()) {
            AppToast.show(this, "초대코드를 찾지 못했어. 공유 메시지를 다시 눌러줘!");
            goLogin(null, null, 0L);
            finish();
            return;
        }

        copyToClipboard(inviteCode);

        AppToast.show(
                this,
                "초대코드를 클립보드에 복사했어!\n온보딩에서 '공동 냉장고' → '초대코드로 참여'에서 붙여넣기 해줘."
        );

        goLogin(inviteCode, fridgeName, expiresAt);
        finish();
    }

    private void goLogin(@Nullable String inviteCode, @Nullable String fridgeName, long expiresAt) {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        if (inviteCode != null) {
            i.putExtra(LoginActivity.EXTRA_INVITE_CODE, inviteCode);
        }

        if (fridgeName != null) {
            i.putExtra(EXTRA_FRIDGE_NAME, fridgeName);
        }

        i.putExtra(EXTRA_EXPIRES_AT, expiresAt);

        startActivity(i);
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("invite_code", text));
            }

        } catch (Exception ignored) {
        }
    }

    private boolean isEmpty(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeGetQuery(@Nullable Uri uri, @NonNull String key) {
        try {
            return uri.getQueryParameter(key);
        } catch (Exception e) {
            return null;
        }
    }
}