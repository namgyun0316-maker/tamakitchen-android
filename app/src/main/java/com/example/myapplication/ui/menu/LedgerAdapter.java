package com.namgyun.tamakitchen.ui.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.VH> {

    public interface OnEntryClickListener {
        void onClick(LedgerEntry entry);
    }

    private final List<LedgerEntry> items = new ArrayList<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
    private OnEntryClickListener listener;

    public void setOnEntryClickListener(OnEntryClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<LedgerEntry> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public int findFirstPositionByDate(String date) {
        if (date == null || date.trim().isEmpty()) return -1;
        for (int i = 0; i < items.size(); i++) {
            LedgerEntry item = items.get(i);
            if (item != null && date.equals(item.getDate())) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ledger_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        LedgerEntry item = items.get(position);

        String currentDate = item.getDate();
        String prevDate = position > 0 ? items.get(position - 1).getDate() : "";

        boolean showHeader = position == 0 || !currentDate.equals(prevDate);
        holder.tvLedgerDateHeader.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        holder.tvLedgerDateHeader.setText(formatDateHeader(currentDate));

        holder.tvStore.setText(item.getStoreName());
        holder.tvName.setText(item.getItemName());

        holder.tvQtyPrice.setText(
                formatQty(item.getQuantity()) + item.getUnit()
                        + " · 단가 " + moneyFormat.format(item.getUnitPrice()) + "원"
        );

        String memo = item.getMemo();
        if (memo == null || memo.trim().isEmpty()) {
            holder.tvMemo.setVisibility(View.GONE);
        } else {
            holder.tvMemo.setVisibility(View.VISIBLE);
            holder.tvMemo.setText(memo.trim());
        }

        holder.tvTotal.setText(moneyFormat.format(item.getTotalPrice()) + "원");
        holder.tvSource.setText(sourceLabel(item.getSourceType()));

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String sourceLabel(String source) {
        if (LedgerEntry.SOURCE_SHOPPING_AUTO.equals(source)) return "쇼핑연동";
        if (LedgerEntry.SOURCE_RECEIPT.equals(source)) return "영수증";
        return "직접입력";
    }

    private String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) {
            return String.valueOf((long) Math.round(q));
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        return df.format(q);
    }

    private String formatDateHeader(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String[] parts = raw.split("-");
        if (parts.length != 3) return raw;

        try {
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            return month + "월 " + day + "일";
        } catch (Exception e) {
            return raw;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLedgerDateHeader;
        MaterialCardView cardRoot;
        TextView tvStore;
        TextView tvSource;
        TextView tvName;
        TextView tvQtyPrice;
        TextView tvMemo;
        TextView tvTotal;

        VH(@NonNull View itemView) {
            super(itemView);
            tvLedgerDateHeader = itemView.findViewById(R.id.tvLedgerDateHeader);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            tvStore = itemView.findViewById(R.id.tvLedgerStore);
            tvSource = itemView.findViewById(R.id.tvLedgerSource);
            tvName = itemView.findViewById(R.id.tvLedgerName);
            tvQtyPrice = itemView.findViewById(R.id.tvLedgerQtyPrice);
            tvMemo = itemView.findViewById(R.id.tvLedgerMemo);
            tvTotal = itemView.findViewById(R.id.tvLedgerTotal);
        }
    }
}