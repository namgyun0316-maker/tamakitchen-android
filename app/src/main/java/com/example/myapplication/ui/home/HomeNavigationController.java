package com.namgyun.tamakitchen.ui.home;

import android.content.Intent;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.ui.attendance.AttendanceActivity;
import com.namgyun.tamakitchen.ui.collection.CollectionBookActivity;
import com.namgyun.tamakitchen.ui.shop.ShopActivity;
import com.google.android.material.button.MaterialButton;

public class HomeNavigationController {

    private final Fragment fragment;

    public HomeNavigationController(@NonNull Fragment fragment) {
        this.fragment = fragment;
    }

    public void bindShopButton(MaterialButton btnShop) {
        if (btnShop == null) return;
        btnShop.setOnClickListener(v -> {
            if (!fragment.isAdded()) return;
            Intent intent = new Intent(fragment.requireContext(), ShopActivity.class);
            fragment.startActivity(intent);
        });
    }

    public void bindNavigateCard(View target, Class<?> activityClass) {
        if (target == null) return;
        target.setOnClickListener(v -> {
            if (!fragment.isAdded()) return;
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            Intent intent = new Intent(fragment.requireContext(), activityClass);
            fragment.startActivity(intent);
        });
    }

    public void bindDefaultNavigation(View collectionView, View attendanceView) {
        bindNavigateCard(collectionView, CollectionBookActivity.class);
        bindNavigateCard(attendanceView, AttendanceActivity.class);
    }
}
