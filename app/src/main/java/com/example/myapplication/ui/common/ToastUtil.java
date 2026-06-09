package com.namgyun.tamakitchen.ui.common;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.namgyun.tamakitchen.R;

public class ToastUtil {

    private ToastUtil() {
    }

    public static void showIosToast(Context context, String message) {
        showIosToast(context, message, Toast.LENGTH_SHORT);
    }

    public static void showIosToast(Context context, String message, int duration) {
        if (context == null) return;

        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.custom_toast, null);

            TextView tv = layout.findViewById(R.id.toast_message);

            if (tv != null) {
                tv.setText(message == null ? "" : message);
            }

            int wSpec = View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
            );

            int hSpec = View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
            );

            layout.measure(wSpec, hSpec);

            layout.layout(
                    0,
                    0,
                    layout.getMeasuredWidth(),
                    layout.getMeasuredHeight()
            );

            Toast toast = new Toast(context.getApplicationContext());
            toast.setView(layout);
            toast.setDuration(duration);

            int yOffset = (int) (
                    110 * context.getResources()
                            .getDisplayMetrics()
                            .density
            );

            toast.setGravity(
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                    0,
                    yOffset
            );

            toast.show();

        } catch (Throwable ignored) {

            try {
                AppToast.show(
                        context instanceof android.app.Activity
                                ? (android.app.Activity) context
                                : null,
                        message == null ? "" : message
                );

            } catch (Throwable ignored2) {
            }
        }
    }

    public static void showCartAddedToast(Context context, int count) {
        if (context == null) return;

        final String message;

        if (count <= 0) {
            message = "선택된 재료가 없어요";
        } else {
            message = "장바구니에 " + count + "개 담았습니다 🛒";
        }

        showIosToast(context, message, Toast.LENGTH_SHORT);
    }
}