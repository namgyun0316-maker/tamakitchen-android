package com.namgyun.tamakitchen.ui.fridge;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.shopping.IconCatalog;
import com.namgyun.tamakitchen.ui.shopping.IconItem;

import java.lang.reflect.Method;
import java.util.List;

public class FridgeUiUtils {

    private FridgeUiUtils() {}

    public static int dp(Context ctx, int dp) {
        float d = ctx.getResources().getDisplayMetrics().density;
        return (int) (dp * d + 0.5f);
    }

    public static void showCustomToast(Activity activity, String message) {
        if (activity == null) return;
        View layout = LayoutInflater.from(activity).inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(activity.getApplicationContext());
        toast.setView(layout);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
        toast.show();
    }

    public static void setAddedByNicknameSafe(FridgeItem item, String nickname) {
        if (item == null) return;
        try {
            Method m = FridgeItem.class.getMethod("setAddedByNickname", String.class);
            m.invoke(item, nickname);
        } catch (Exception ignored) {}
    }

    public static int resolveIconResId(FridgeItem item) {
        if (item == null) return 0;
        String iconName = item.getIconName();
        if (iconName != null) iconName = iconName.trim();
        if (TextUtils.isEmpty(iconName)) return 0;

        int res = IconCatalog.findResIdByRawKey(iconName);
        if (res != 0) return res;

        List<IconItem> all = IconCatalog.getAllIcons();
        for (IconItem icon : all) {
            if (icon == null) continue;
            if (iconName.equals(icon.getName())) return icon.getResId();
        }
        return 0;
    }
}