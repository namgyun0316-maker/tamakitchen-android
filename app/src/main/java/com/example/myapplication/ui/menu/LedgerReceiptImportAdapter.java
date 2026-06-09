package com.namgyun.tamakitchen.ui.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.shopping.ReceiptLine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class LedgerReceiptImportAdapter extends RecyclerView.Adapter<LedgerReceiptImportAdapter.VH> {

    private final List<ReceiptLine> items = new ArrayList<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");

    public void submit(List<ReceiptLine> list) {
        items.clear();
        if (list != null) {
            for (ReceiptLine item : list) {
                if (item != null) item.setChecked(true);
                items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public List<ReceiptLine> getSelectedItems() {
        List<ReceiptLine> out = new ArrayList<>();
        for (ReceiptLine item : items) {
            if (item != null && item.isChecked()) out.add(item);
        }
        return out;
    }

    public boolean isAllSelected() {
        if (items.isEmpty()) return false;
        for (ReceiptLine item : items) {
            if (item == null || !item.isChecked()) return false;
        }
        return true;
    }

    public void toggleSelectAll() {
        boolean next = !isAllSelected();
        for (ReceiptLine item : items) {
            if (item != null) item.setChecked(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ledger_receipt_line, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReceiptLine item = items.get(position);

        holder.cbLedgerReceipt.setChecked(item != null && item.isChecked());
        holder.tvLedgerReceiptName.setText(item != null ? item.getName() : "");
        holder.tvLedgerReceiptQty.setText(formatQty(resolveReceiptLineQuantity(item)) + "개");
        holder.tvLedgerReceiptPrice.setText(moneyFormat.format(item != null ? item.getPrice() : 0) + "원");

        holder.itemView.setOnClickListener(v -> {
            if (item == null) return;
            item.setChecked(!item.isChecked());
            notifyItemChanged(position);
        });

        holder.cbLedgerReceipt.setOnClickListener(v -> {
            if (item == null) return;
            item.setChecked(holder.cbLedgerReceipt.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private double resolveReceiptLineQuantity(ReceiptLine line) {
        if (line == null) return 1.0d;

        try {
            Method m = line.getClass().getMethod("getQuantity");
            Object value = m.invoke(line);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception ignored) {
        }

        try {
            Method m = line.getClass().getMethod("getQty");
            Object value = m.invoke(line);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception ignored) {
        }

        try {
            Field f = line.getClass().getDeclaredField("quantity");
            f.setAccessible(true);
            Object value = f.get(line);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception ignored) {
        }

        try {
            Field f = line.getClass().getDeclaredField("qty");
            f.setAccessible(true);
            Object value = f.get(line);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception ignored) {
        }

        return 1.0d;
    }

    private String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) {
            return String.valueOf((long) Math.round(q));
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        return df.format(q);
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbLedgerReceipt;
        TextView tvLedgerReceiptName;
        TextView tvLedgerReceiptQty;
        TextView tvLedgerReceiptPrice;

        VH(@NonNull View itemView) {
            super(itemView);
            cbLedgerReceipt = itemView.findViewById(R.id.cbLedgerReceipt);
            tvLedgerReceiptName = itemView.findViewById(R.id.tvLedgerReceiptName);
            tvLedgerReceiptQty = itemView.findViewById(R.id.tvLedgerReceiptQty);
            tvLedgerReceiptPrice = itemView.findViewById(R.id.tvLedgerReceiptPrice);
        }
    }
}