package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FridgeDeleteDialogHelper {

    public interface DeleteCallbacks {
        void onDeleteItems(List<FridgeItem> items);
    }

    public static void showDeleteOne(
            Context ctx,
            FridgeItem item,
            DeleteCallbacks cb
    ) {
        if (ctx == null) return;

        TextView tvName;
        ImageView ivIcon;
        Button btnCancel, btnDelete;

        var dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_delete_item, null);
        ivIcon = dialogView.findViewById(R.id.iv_item_icon);
        tvName = dialogView.findViewById(R.id.tv_item_name);
        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnDelete = dialogView.findViewById(R.id.btn_delete);

        // 🔥 여기 완전 교체
        int iconRes = resolveDeleteIcon(item);
        ivIcon.setImageResource(iconRes);

        tvName.setText("'" + (item == null || item.getName() == null ? "" : item.getName()) + "'");

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(dialogView).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            if (cb != null) cb.onDeleteItems(java.util.Collections.singletonList(item));
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.show();
    }

    // 🔥 핵심: 삭제 다이얼로그용 아이콘 로직
    private static int resolveDeleteIcon(FridgeItem item) {
        if (item == null) return R.drawable.ic_custom_item;

        String name = normalize(item.getName());
        String iconName = normalize(item.getIconName());

        // 🔥 강제 기본아이콘 (문제 재료)
        if (name.contains("김치") || name.contains("간장") || name.contains("참치액")) {
            return R.drawable.ic_custom_item;
        }

        // 🔥 직접추가 / 없는 재료
        if ("직접 추가".equals(name) || TextUtils.isEmpty(iconName)) {
            return R.drawable.ic_custom_item;
        }

        // 🔥 기존 로직 (정상 아이콘만)
        int res = FridgeUiUtils.resolveIconResId(item);
        if (res != 0) return res;

        // 🔥 마지막 fallback
        return R.drawable.ic_custom_item;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.getDefault());
    }

    public static void showDeleteExpired(
            Context ctx,
            List<FridgeItem> allItems,
            DeleteCallbacks cb
    ) {
        if (ctx == null) return;

        List<FridgeItem> expired = FridgeFilterSorter.expiredOnly(allItems);
        if (expired.isEmpty()) {
            if (ctx instanceof android.app.Activity) {
                FridgeUiUtils.showCustomToast((android.app.Activity) ctx, "유통기한이 지난 항목이 없습니다");
            }
            return;
        }

        var dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_delete_expired, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        TextView tvHint = dialogView.findViewById(R.id.tvHint);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        int count = expired.size();
        tvTitle.setText("유통기한 지난 항목 삭제");
        tvMessage.setText(count + "개의 항목을 삭제할까요?");
        tvHint.setText("삭제한 재료는 복구할 수 없습니다.");

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(dialogView).create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            if (cb != null) cb.onDeleteItems(expired);
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    public static void showDeleteAll(
            Context ctx,
            List<FridgeItem> allItems,
            DeleteCallbacks cb
    ) {
        if (ctx == null) return;

        if (allItems == null || allItems.isEmpty()) {
            if (ctx instanceof android.app.Activity) {
                FridgeUiUtils.showCustomToast((android.app.Activity) ctx, "삭제할 항목이 없습니다");
            }
            return;
        }

        var dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_delete_all, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        TextView tvHint = dialogView.findViewById(R.id.tvHint);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnDeleteAll = dialogView.findViewById(R.id.btnDeleteAll);

        tvTitle.setText("재료 전체 삭제");
        tvMessage.setText("정말로 모든 재료를 삭제할까요?");
        tvHint.setText("이 작업은 되돌릴 수 없습니다.");

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(dialogView).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDeleteAll.setOnClickListener(v -> {
            if (cb != null) cb.onDeleteItems(new ArrayList<>(allItems));
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    public static void showSelectDeleteConfirm(
            Context ctx,
            int selectedCount,
            List<FridgeItem> selectedItems,
            DeleteCallbacks cb
    ) {
        if (ctx == null) return;

        var dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_select_delete, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        TextView tvHint = dialogView.findViewById(R.id.tvHint);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        tvTitle.setText("재료 선택 삭제");
        tvMessage.setText("선택한 " + selectedCount + "개 항목을 삭제할까요?");
        tvHint.setText("삭제한 재료는 복구할 수 없습니다.");

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(dialogView).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            if (selectedItems == null || selectedItems.isEmpty()) {
                if (ctx instanceof android.app.Activity) {
                    FridgeUiUtils.showCustomToast((android.app.Activity) ctx, "삭제할 항목을 선택해주세요");
                }
                dialog.dismiss();
                return;
            }
            if (cb != null) cb.onDeleteItems(selectedItems);
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}