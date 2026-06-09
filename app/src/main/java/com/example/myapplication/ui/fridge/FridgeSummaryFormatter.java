package com.namgyun.tamakitchen.ui.fridge;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.List;

public class FridgeSummaryFormatter {

    public static SpannableString buildSummary(List<FridgeItem> items) {
        int total = (items == null) ? 0 : items.size();
        int nearExpire = 0;
        int expired = 0;

        if (items != null) {
            for (FridgeItem item : items) {
                if (item == null) continue;
                String dDayString = item.getDDayString();
                if ("유통기한 지남".equals(dDayString) || "D-Day".equals(dDayString)) expired++;
                else if (item.isNearExpire()) nearExpire++;
            }
        }

        String summaryText = (expired > 0)
                ? "보관중 " + total + "개 · 임박 " + nearExpire + "개 · 유통기한 지남 " + expired + "개"
                : "보관중 " + total + "개 · 임박 " + nearExpire + "개";

        SpannableString sp = new SpannableString(summaryText);

        int blueColor = Color.parseColor("#2196F3");
        int orangeColor = Color.parseColor("#FF9800");
        int redColor = Color.parseColor("#F44336");

        int storageStart = summaryText.indexOf("보관중");
        int storageEnd = summaryText.indexOf("개", storageStart) + 1;
        if (storageStart >= 0 && storageEnd > storageStart) {
            sp.setSpan(new ForegroundColorSpan(blueColor),
                    storageStart, storageEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int nearStart = summaryText.indexOf("임박");
        if (nearStart >= 0) {
            int nearEnd = summaryText.indexOf("개", nearStart) + 1;
            if (nearEnd > nearStart) {
                sp.setSpan(new ForegroundColorSpan(orangeColor),
                        nearStart, nearEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        int expiredStart = summaryText.indexOf("유통기한 지남");
        if (expiredStart >= 0) {
            int expiredEnd = summaryText.indexOf("개", expiredStart) + 1;
            if (expiredEnd > expiredStart) {
                sp.setSpan(new ForegroundColorSpan(redColor),
                        expiredStart, expiredEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return sp;
    }
}