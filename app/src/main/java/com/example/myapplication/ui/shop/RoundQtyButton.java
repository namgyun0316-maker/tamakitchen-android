package com.namgyun.tamakitchen.ui.shop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.namgyun.tamakitchen.R;

public class RoundQtyButton extends View {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private String symbol;

    public RoundQtyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RoundQtyButton(Context context) {
        super(context);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFFFFFFFF);
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFFDDDDDD);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        Typeface jua = ResourcesCompat.getFont(getContext(), R.font.jua_regular);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF1F1F1F);
        textPaint.setTextSize(60f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(jua);
    }

    public void setSymbol(String s) {
        this.symbol = s;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float r = Math.min(w, h) * 0.30f;
        RectF rect = new RectF(2f, 2f, w - 2f, h - 2f);

        canvas.drawRoundRect(rect, r, r, bgPaint);
        canvas.drawRoundRect(rect, r, r, borderPaint);

        if (symbol != null) {
            android.graphics.Rect bounds = new android.graphics.Rect();
            textPaint.getTextBounds(symbol, 0, symbol.length(), bounds);
            float textY = h / 2f + bounds.height() / 2f - bounds.bottom;
            canvas.drawText(symbol, w / 2f, textY, textPaint);
        }
    }
}