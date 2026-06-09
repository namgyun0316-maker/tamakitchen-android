package com.namgyun.tamakitchen.ui.onboarding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SharedTime {
    private SharedTime() {}

    public static String formatExpireText(long expiresAt) {
        if (expiresAt <= 0) return "만료 정보 없음";
        SimpleDateFormat f = new SimpleDateFormat("만료: yyyy-MM-dd HH:mm", Locale.getDefault());
        return f.format(new Date(expiresAt));
    }
}
