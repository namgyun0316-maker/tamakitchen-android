package com.namgyun.tamakitchen.ui.shopping;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoreSpendPickerDialog extends DialogFragment {

    public interface OnStorePicked {
        void onPicked(@NonNull String dateKey, @Nullable Long storeId, @NonNull String storeName);
    }

    public static class StoreSpendRow implements Serializable {
        @Nullable public Long storeId;
        @NonNull public String storeName;
        public long amount;

        public StoreSpendRow(@Nullable Long storeId, @NonNull String storeName, long amount) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.amount = amount;
        }
    }

    private static final String ARG_DATE_KEY = "ARG_DATE_KEY";
    private static final String ARG_ROWS = "ARG_ROWS";

    private OnStorePicked listener;

    public static StoreSpendPickerDialog newInstance(
            @NonNull String dateKey,
            @NonNull List<StoreSpendRow> rows,
            @NonNull OnStorePicked listener
    ) {
        StoreSpendPickerDialog f = new StoreSpendPickerDialog();

        Bundle b = new Bundle();
        b.putString(ARG_DATE_KEY, dateKey);
        b.putSerializable(ARG_ROWS, new ArrayList<>(rows));

        f.setArguments(b);
        f.listener = listener;

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_store_spend_picker, null);

        TextView tvTitle = root.findViewById(R.id.tvTitle);
        TextView tvSub = root.findViewById(R.id.tvSub);
        RecyclerView rv = root.findViewById(R.id.rvStores);
        View btnClose = root.findViewById(R.id.btnClose);

        String dateKey = getArguments() != null
                ? getArguments().getString(ARG_DATE_KEY, "")
                : "";

        List<StoreSpendRow> rows = extractRows();

        if (tvTitle != null) {
            tvTitle.setText(dateKey + " 판매점 선택");
        }

        // ✅ "2026-01-27 · 총 0원" 같은 보조 문구 제거
        if (tvSub != null) {
            tvSub.setVisibility(View.GONE);
            tvSub.setText("");
        }

        if (rv != null) {
            ViewGroup.LayoutParams rvLp = rv.getLayoutParams();
            if (rvLp != null) {
                rvLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                rv.setLayoutParams(rvLp);
            }

            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setAdapter(new StoreSpendAdapter(rows, (storeId, storeName) -> {
                if (listener != null) {
                    listener.onPicked(dateKey, storeId, storeName);
                }
                dismiss();
            }));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();

            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(window.getAttributes());
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.86f);
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(lp);
            }
        });

        return dialog;
    }

    @SuppressWarnings("unchecked")
    private List<StoreSpendRow> extractRows() {
        if (getArguments() == null) return Collections.emptyList();

        Serializable s = getArguments().getSerializable(ARG_ROWS);

        if (s instanceof ArrayList) {
            return (ArrayList<StoreSpendRow>) s;
        }

        return Collections.emptyList();
    }

    private static class StoreSpendAdapter extends RecyclerView.Adapter<StoreSpendAdapter.VH> {

        interface OnClick {
            void onClick(@Nullable Long storeId, @NonNull String storeName);
        }

        private final List<StoreSpendRow> rows;
        private final OnClick onClick;
        private final DecimalFormat df = new DecimalFormat("#,###");

        StoreSpendAdapter(List<StoreSpendRow> rows, OnClick onClick) {
            this.rows = rows == null ? new ArrayList<>() : rows;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_store_spend, parent, false);

            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 8);
            v.setLayoutParams(lp);

            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            StoreSpendRow r = rows.get(position);

            holder.tvStore.setText(r.storeName);

            if (r.amount > 0) {
                holder.tvAmount.setText("-" + df.format(r.amount) + "원");
            } else {
                holder.tvAmount.setText("0원");
            }

            holder.itemView.setOnClickListener(v -> {
                if (onClick != null) {
                    onClick.onClick(r.storeId, r.storeName);
                }
            });

            if (holder.btnMove != null) {
                holder.btnMove.setOnClickListener(v -> {
                    if (onClick != null) {
                        onClick.onClick(r.storeId, r.storeName);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvStore;
            TextView tvAmount;
            TextView btnMove;

            VH(@NonNull View itemView) {
                super(itemView);
                tvStore = itemView.findViewById(R.id.tvStoreName);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                btnMove = itemView.findViewById(R.id.btnMove);
            }
        }
    }
}