package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.namgyun.tamakitchen.R;

public class FridgeOptionsPopupHelper {

    public interface Callbacks {
        void onSortExpiry();
        void onSortName();
        void onToggleSelectDelete();
        void onDeleteExpired();
        void onDeleteAll();
    }

    private PopupWindow popup;

    public void dismiss() {
        if (popup != null) {
            try { popup.dismiss(); } catch (Exception ignored) {}
            popup = null;
        }
    }

    public void toggle(
            Context ctx,
            View anchor,
            boolean isSelectDeleteMode,
            FridgeFilterSorter.SortMode sortMode,
            Callbacks cb
    ) {
        if (ctx == null || anchor == null) return;

        if (popup != null && popup.isShowing()) {
            dismiss();
            return;
        }

        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_fridge_options, null);

        View itemSortExpiry = v.findViewById(R.id.item_sort_expiry);
        View itemSortName = v.findViewById(R.id.item_sort_name);
        View itemSelectDelete = v.findViewById(R.id.item_select_delete);
        View itemDeleteExpired = v.findViewById(R.id.item_delete_expired);
        View itemDeleteAll = v.findViewById(R.id.item_delete_all);

        if (itemSelectDelete instanceof TextView) {
            ((TextView) itemSelectDelete).setText(isSelectDeleteMode ? "재료 선택 삭제 종료" : "재료 선택 삭제");
        }

        TextView tvSortExpiry = (itemSortExpiry instanceof TextView) ? (TextView) itemSortExpiry : null;
        TextView tvSortName = (itemSortName instanceof TextView) ? (TextView) itemSortName : null;

        if (tvSortExpiry != null) tvSortExpiry.setTypeface(tvSortExpiry.getTypeface(), Typeface.NORMAL);
        if (tvSortName != null) tvSortName.setTypeface(tvSortName.getTypeface(), Typeface.NORMAL);

        if (sortMode == FridgeFilterSorter.SortMode.EXPIRY) {
            if (tvSortExpiry != null) tvSortExpiry.setTypeface(tvSortExpiry.getTypeface(), Typeface.BOLD);
        } else {
            if (tvSortName != null) tvSortName.setTypeface(tvSortName.getTypeface(), Typeface.BOLD);
        }

        popup = new PopupWindow(v, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(FridgeUiUtils.dp(ctx, 10));

        itemSortExpiry.setOnClickListener(x -> { dismiss(); if (cb != null) cb.onSortExpiry(); });
        itemSortName.setOnClickListener(x -> { dismiss(); if (cb != null) cb.onSortName(); });
        itemSelectDelete.setOnClickListener(x -> { dismiss(); if (cb != null) cb.onToggleSelectDelete(); });
        itemDeleteExpired.setOnClickListener(x -> { dismiss(); if (cb != null) cb.onDeleteExpired(); });
        itemDeleteAll.setOnClickListener(x -> { dismiss(); if (cb != null) cb.onDeleteAll(); });

        playDropDownAnim(ctx, v);

        v.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int contentW = v.getMeasuredWidth();
        int anchorW = anchor.getWidth();
        int xOff = anchorW - contentW;
        int yOff = FridgeUiUtils.dp(ctx, 6);

        popup.showAsDropDown(anchor, xOff, yOff, Gravity.END);
    }

    private void playDropDownAnim(Context ctx, View v) {
        AnimationSet set = new AnimationSet(true);
        TranslateAnimation t = new TranslateAnimation(0, 0, -FridgeUiUtils.dp(ctx, 10), 0);
        t.setDuration(160);
        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(160);
        set.addAnimation(t);
        set.addAnimation(a);
        v.startAnimation(set);
    }
}