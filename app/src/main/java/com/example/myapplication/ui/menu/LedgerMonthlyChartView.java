package com.namgyun.tamakitchen.ui.menu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LedgerMonthlyChartView extends View {

    public static class ChartItem {
        public final String label;
        public final long value;

        public ChartItem(String label, long value) {
            this.label = label;
            this.value = value;
        }
    }

    private final List<ChartItem> items = new ArrayList<>();

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rect = new RectF();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");

    private final float topPadding;
    private final float bottomPadding;
    private final float sidePadding;
    private final float labelGap;
    private final float valueGap;
    private final float barCorner;

    public LedgerMonthlyChartView(Context context) {
        this(context, null);
    }

    public LedgerMonthlyChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LedgerMonthlyChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        topPadding = dp(24);
        bottomPadding = dp(36);
        sidePadding = dp(12);
        labelGap = dp(8);
        valueGap = dp(6);
        barCorner = dp(10);

        axisPaint.setColor(0xFFD9E3EE);
        axisPaint.setStrokeWidth(dp(1));

        gridPaint.setColor(0xFFEEF2F6);
        gridPaint.setStrokeWidth(dp(1));

        barPaint.setColor(0xFF7FC8F8);

        labelPaint.setColor(0xFF5E748A);
        labelPaint.setTextSize(sp(12));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(0xFF2E3A4B);
        valuePaint.setTextSize(sp(11));
        valuePaint.setTextAlign(Paint.Align.CENTER);

        emptyPaint.setColor(0xFF8A94A6);
        emptyPaint.setTextSize(sp(14));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setItems(List<ChartItem> chartItems) {
        items.clear();
        if (chartItems != null) items.addAll(chartItems);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        if (w <= 0 || h <= 0) return;

        if (items.isEmpty()) {
            canvas.drawText("차트 데이터가 없어.", w / 2f, h / 2f, emptyPaint);
            return;
        }

        long max = 0;
        for (ChartItem item : items) {
            if (item != null && item.value > max) max = item.value;
        }
        if (max <= 0) {
            max = 1;
        }

        float chartLeft = sidePadding;
        float chartRight = w - sidePadding;
        float chartTop = topPadding;
        float chartBottom = h - bottomPadding;

        // 기준선
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint);

        // 그리드 3줄
        for (int i = 1; i <= 3; i++) {
            float y = chartTop + ((chartBottom - chartTop) / 4f) * i;
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
        }

        int count = items.size();
        float slotWidth = (chartRight - chartLeft) / count;
        float barWidth = Math.min(dp(34), slotWidth * 0.55f);

        for (int i = 0; i < count; i++) {
            ChartItem item = items.get(i);
            if (item == null) continue;

            float centerX = chartLeft + slotWidth * i + slotWidth / 2f;

            float ratio = Math.max(0f, Math.min(1f, item.value / (float) max));
            float barHeight = (chartBottom - chartTop) * ratio;

            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;
            float top = chartBottom - barHeight;

            rect.set(left, top, right, chartBottom);
            canvas.drawRoundRect(rect, barCorner, barCorner, barPaint);

            String label = item.label == null ? "" : item.label;
            canvas.drawText(label, centerX, h - dp(10), labelPaint);

            String valueText = formatCompact(item.value);
            canvas.drawText(valueText, centerX, top - valueGap, valuePaint);
        }
    }

    private String formatCompact(long value) {
        if (value >= 100000000) {
            return String.format(Locale.KOREA, "%.1f억", value / 100000000f);
        }
        if (value >= 10000) {
            return String.format(Locale.KOREA, "%.1f만", value / 10000f);
        }
        return moneyFormat.format(value);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(float v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                v,
                getResources().getDisplayMetrics()
        );
    }
}