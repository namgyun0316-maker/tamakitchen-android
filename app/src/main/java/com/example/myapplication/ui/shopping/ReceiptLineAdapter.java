package com.namgyun.tamakitchen.ui.shopping;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptLineAdapter extends ListAdapter<ReceiptLine, ReceiptLineAdapter.VH> {

    public interface OnChangedListener { void onAnyChanged(); }
    public interface OnDeleteListener  { void onDelete(ReceiptLine item); }

    private final OnChangedListener changedListener;
    private final OnDeleteListener  deleteListener;

    // ── 개발자 모드: true 이면 원문 힌트 표시 ──
    private boolean devMode = false;

    public ReceiptLineAdapter(OnChangedListener changedListener,
                              OnDeleteListener deleteListener) {
        super(DIFF);
        setHasStableIds(true);
        this.changedListener = changedListener;
        this.deleteListener  = deleteListener;
    }

    public void setDevMode(boolean enabled) {
        devMode = enabled;
        notifyDataSetChanged();
    }

    @Override public long getItemId(int pos) {
        ReceiptLine it = getItem(pos);
        return it == null ? RecyclerView.NO_ID : it.getId();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receipt_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ReceiptLine item = getItem(pos);
        if (item == null) return;

        h.detachWatchers();

        h.cb.setChecked(item.isChecked());
        h.etName.setText(item.getName() == null ? "" : item.getName());
        h.etQty.setText(fmtQty(item.getQty()));
        h.etPrice.setText(item.getPrice() <= 0 ? "" : String.valueOf(item.getPrice()));
        h.tvConfidence.setText(confText(item.getConfidence()));

        // 원문: 개발자 모드일 때만 표시
        h.tvRawHint.setVisibility(devMode ? View.VISIBLE : View.GONE);
        h.tvRawHint.setText("원문: " + (item.getSourceLine() == null ? "-" : item.getSourceLine()));

        // ── 이벤트 ──
        h.cb.setOnCheckedChangeListener((btn, checked) -> {
            item.setChecked(checked);
            notifyChanged();
        });

        h.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(item);
        });

        h.wName = new SW(s -> { item.setName(s); notifyChanged(); });
        h.etName.addTextChangedListener(h.wName);

        h.wQty = new SW(s -> {
            double q = safeD(s); item.setQty(q <= 0 ? 1.0 : q); notifyChanged();
        });
        h.etQty.addTextChangedListener(h.wQty);

        h.wPrice = new SW(s -> {
            int p = safeI(s); item.setPrice(Math.max(0, p)); notifyChanged();
        });
        h.etPrice.addTextChangedListener(h.wPrice);
    }

    private void notifyChanged() { if (changedListener != null) changedListener.onAnyChanged(); }

    public List<ReceiptLine> getCurrentItems() {
        List<ReceiptLine> cur = getCurrentList();
        return cur == null ? new ArrayList<>() : new ArrayList<>(cur);
    }

    public List<ReceiptLine> getSelectedItems() {
        List<ReceiptLine> out = new ArrayList<>();
        for (ReceiptLine it : getCurrentItems()) if (it != null && it.isChecked()) out.add(it);
        return out;
    }

    public void setAllChecked(boolean checked) {
        List<ReceiptLine> cur = getCurrentItems();
        for (ReceiptLine it : cur) if (it != null) it.setChecked(checked);
        submitList(cur);
        notifyChanged();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────

    private String confText(ReceiptLine.Confidence c) {
        if (c == null) return "";
        switch (c) {
            case HIGH:   return "신뢰도 HIGH";
            case MEDIUM: return "신뢰도 MEDIUM";
            case LOW:    return "신뢰도 LOW";
        }
        return "";
    }

    private String fmtQty(double q) {
        return q == (int) q ? String.valueOf((int) q)
                : String.format(Locale.getDefault(), "%.2f", q);
    }

    private int safeI(String s) {
        try { return Integer.parseInt(s == null ? "" : s.trim().replace(",","")); }
        catch (Exception e) { return 0; }
    }

    private double safeD(String s) {
        try { return Double.parseDouble(s == null ? "" : s.trim().replace(",","")); }
        catch (Exception e) { return 0; }
    }

    // ── ViewHolder ─────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        MaterialCheckBox      cb;
        TextView              tvConfidence;
        TextInputEditText     etName, etQty, etPrice;
        TextView              tvRawHint;
        ImageButton           btnDelete;
        TextWatcher           wName, wQty, wPrice;

        VH(@NonNull View v) {
            super(v);
            cb           = v.findViewById(R.id.cb);
            tvConfidence = v.findViewById(R.id.tvConfidence);
            etName       = v.findViewById(R.id.etName);
            etQty        = v.findViewById(R.id.etQty);
            etPrice      = v.findViewById(R.id.etPrice);
            tvRawHint    = v.findViewById(R.id.tvRawHint);
            btnDelete    = v.findViewById(R.id.btnDelete);
        }

        void detachWatchers() {
            if (wName  != null) etName.removeTextChangedListener(wName);
            if (wQty   != null) etQty.removeTextChangedListener(wQty);
            if (wPrice != null) etPrice.removeTextChangedListener(wPrice);
            wName = wQty = wPrice = null;
        }
    }

    // ── Simple TextWatcher ─────────────────────────────────

    static class SW implements TextWatcher {
        interface OnText { void on(String s); }
        private final OnText cb;
        SW(OnText cb) { this.cb = cb; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
            if (cb != null) cb.on(s == null ? "" : s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    }

    // ── DiffUtil ───────────────────────────────────────────

    private static final DiffUtil.ItemCallback<ReceiptLine> DIFF =
            new DiffUtil.ItemCallback<ReceiptLine>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReceiptLine a, @NonNull ReceiptLine b) {
                    return a.getId() == b.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull ReceiptLine a, @NonNull ReceiptLine b) {
                    return a.isChecked() == b.isChecked()
                            && eq(a.getName(), b.getName())
                            && Double.compare(a.getQty(), b.getQty()) == 0
                            && a.getPrice() == b.getPrice()
                            && a.getConfidence() == b.getConfidence();
                }
                private boolean eq(String a, String b) {
                    return (a == null ? "" : a).equals(b == null ? "" : b);
                }
            };
}