package com.namgyun.tamakitchen.ui.fridge;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.namgyun.tamakitchen.R;
import com.google.android.material.chip.ChipGroup;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;

public class FridgeEditDialogHelper {

    public interface Callbacks {
        void onSaved(FridgeItem updatedItem);
    }

    private static final DecimalFormat QTY_FMT = new DecimalFormat("0.########");

    private static String formatQty(double v) { return QTY_FMT.format(v); }

    public static void showEditDialog(
            Context ctx,
            FridgeItem item,
            Callbacks cb
    ) {
        if (ctx == null || item == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        var dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_fridge_item, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        android.widget.EditText etEditName = dialogView.findViewById(R.id.et_edit_name);
        TextView tvQuantityEdit = dialogView.findViewById(R.id.tv_quantity_edit1);
        TextView btnDecreaseEdit = dialogView.findViewById(R.id.btn_decrease_edit1);
        TextView btnIncreaseEdit = dialogView.findViewById(R.id.btn_increase_edit1);
        ChipGroup chipGroupStorageEdit = dialogView.findViewById(R.id.chip_group_storage_edit1);
        Button btnCancelEdit = dialogView.findViewById(R.id.btn_cancel_edit1);
        Button btnSaveEdit = dialogView.findViewById(R.id.btn_save_edit1);
        TextView tvExpiryDate = dialogView.findViewById(R.id.tv_expiry_date_edit1);
        Button btnPickDate = dialogView.findViewById(R.id.btn_pick_date_edit1);

        etEditName.setText(item.getName());
        etEditName.setSelection(etEditName.getText().length());

        final double[] currentQuantity = { item.getQuantity() };
        tvQuantityEdit.setText(formatQty(currentQuantity[0]));

        tvQuantityEdit.setOnClickListener(v ->
                showQuantityInputDialog(ctx, currentQuantity, tvQuantityEdit)
        );

        selectStorageChip(chipGroupStorageEdit, item.getStorage());

        if (!TextUtils.isEmpty(item.getExpiryDate())) tvExpiryDate.setText(item.getExpiryDate());
        else tvExpiryDate.setText("유통기한 선택 (선택사항)");

        btnDecreaseEdit.setOnClickListener(v -> {
            double next = currentQuantity[0] - 1.0;
            if (next < 0.1) next = 0.1;
            currentQuantity[0] = next;
            tvQuantityEdit.setText(formatQty(currentQuantity[0]));
        });

        btnIncreaseEdit.setOnClickListener(v -> {
            double next = currentQuantity[0] + 1.0;
            if (next > 9999) next = 9999;
            currentQuantity[0] = next;
            tvQuantityEdit.setText(formatQty(currentQuantity[0]));
        });

        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(ctx,
                    (view, y, m, d) -> {
                        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, (m + 1), d);
                        tvExpiryDate.setText(dateStr);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnCancelEdit.setOnClickListener(v -> dialog.dismiss());

        btnSaveEdit.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            if (newName.isEmpty()) return; // Fragment에서 토스트 처리하는 구조로 하면 더 깔끔

            String newStorage = getSelectedStorage(chipGroupStorageEdit);

            String newExpiryDate = tvExpiryDate.getText().toString();
            if (newExpiryDate.contains("선택사항")) newExpiryDate = "";
            if (newExpiryDate == null) newExpiryDate = "";
            newExpiryDate = newExpiryDate.trim();

            // ✅ 기본 유통기한 자동 세팅(비어 있을 때)
            if (newExpiryDate.isEmpty()) {
                ExpiryDefaultUtil.StorageType st = mapStorageToType(newStorage);
                if (st != null) {
                    Integer days = ExpiryDefaultUtil.getDefaultDaysOrNull(newName, st);
                    if (days != null) {
                        String auto = ExpiryDefaultUtil.computeExpiryDateFromToday(days);
                        if (!TextUtils.isEmpty(auto)) {
                            newExpiryDate = auto;
                            tvExpiryDate.setText(newExpiryDate);
                        }
                    }
                }
            }

            item.setName(newName);
            item.setQuantity(currentQuantity[0]);
            item.setStorage(newStorage);
            item.setExpiryDate(newExpiryDate);

            if (cb != null) cb.onSaved(item);
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private static void showQuantityInputDialog(Context ctx, double[] currentQuantity, TextView tvQuantityEdit) {
        var dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_quantity_input, null);

        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnOk = dialogView.findViewById(R.id.btn_ok);
        android.widget.EditText etQty = dialogView.findViewById(R.id.et_quantity);

        etQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etQty.setText(formatQty(currentQuantity[0]));
        etQty.setSelection(etQty.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            String raw = etQty.getText().toString().trim();
            if (!raw.isEmpty()) {
                try {
                    double val = Double.parseDouble(raw);
                    if (val < 0.1) val = 0.1;
                    if (val > 9999) val = 9999;
                    currentQuantity[0] = val;
                    tvQuantityEdit.setText(formatQty(currentQuantity[0]));
                } catch (NumberFormatException ignored) {}
            }
            dialog.dismiss();
        });

        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        etQty.requestFocus();
    }

    private static void selectStorageChip(ChipGroup group, String storage) {
        if (group == null) return;
        group.clearCheck();
        if ("냉장".equals(storage)) group.check(R.id.chip_storage_fridge_edit1);
        else if ("냉동".equals(storage)) group.check(R.id.chip_storage_freezer_edit1);
        else if ("실온".equals(storage)) group.check(R.id.chip_storage_room_edit1);
        else group.check(R.id.chip_storage_etc_edit1);
    }

    private static String getSelectedStorage(ChipGroup group) {
        if (group == null) return "기타";
        int checkedId = group.getCheckedChipId();
        if (checkedId == R.id.chip_storage_fridge_edit1) return "냉장";
        if (checkedId == R.id.chip_storage_freezer_edit1) return "냉동";
        if (checkedId == R.id.chip_storage_room_edit1) return "실온";
        return "기타";
    }

    private static ExpiryDefaultUtil.StorageType mapStorageToType(String storage) {
        if (storage == null) return null;
        String s = storage.trim();
        if ("냉장".equals(s)) return ExpiryDefaultUtil.StorageType.FRIDGE;
        if ("냉동".equals(s)) return ExpiryDefaultUtil.StorageType.FREEZER;
        if ("실온".equals(s)) return ExpiryDefaultUtil.StorageType.ROOM;
        return null;
    }
}