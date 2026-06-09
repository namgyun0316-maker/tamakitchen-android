package com.namgyun.tamakitchen.ui.shopping;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.namgyun.tamakitchen.R;

import java.text.DecimalFormat;

public class FinishShoppingConfirmDialog extends DialogFragment {

    public interface Callback {
        void onYes();
        void onNo();
    }

    private static final String ARG_TOTAL = "arg_total";

    private long totalSpend = 0L;
    private Callback callback;

    public static FinishShoppingConfirmDialog newInstance(long totalSpend) {
        FinishShoppingConfirmDialog f = new FinishShoppingConfirmDialog();
        Bundle b = new Bundle();
        b.putLong(ARG_TOTAL, totalSpend);
        f.setArguments(b);
        return f;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        if (getArguments() != null) {
            totalSpend = getArguments().getLong(ARG_TOTAL, 0L);
        }

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_finish_shopping, null);

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvMessage = v.findViewById(R.id.tvMessage);
        Button btnNo = v.findViewById(R.id.btnNo);
        Button btnYes = v.findViewById(R.id.btnYes);

        if (tvTitle != null && TextUtils.isEmpty(tvTitle.getText())) {
            tvTitle.setText("장보기 완료");
        }

        DecimalFormat money = new DecimalFormat("#,###");
        String msg = money.format(Math.max(0, totalSpend)) + "원 지출하셨습니다.\n냉장고에 재료를 등록하시겠습니까?";
        if (tvMessage != null) tvMessage.setText(msg);

        if (btnNo != null) {
            btnNo.setOnClickListener(view -> {
                if (callback != null) callback.onNo();
                dismiss();
            });
        }

        if (btnYes != null) {
            btnYes.setOnClickListener(view -> {
                if (callback != null) callback.onYes();
                dismiss();
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        return dialog;
    }
}
