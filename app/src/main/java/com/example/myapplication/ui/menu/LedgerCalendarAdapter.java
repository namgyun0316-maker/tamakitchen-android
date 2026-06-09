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
import java.util.Locale;

public class LedgerCalendarAdapter extends RecyclerView.Adapter<LedgerCalendarAdapter.VH> {

    public interface OnDayClickListener {
        void onDayClick(DayCell item);
    }

    public static class DayCell {
        public String dateKey;       // yyyy-MM-dd
        public int day;
        public boolean inCurrentMonth;
        public long totalExpense;
        public boolean selected;

        public DayCell(String dateKey, int day, boolean inCurrentMonth, long totalExpense, boolean selected) {
            this.dateKey = dateKey;
            this.day = day;
            this.inCurrentMonth = inCurrentMonth;
            this.totalExpense = totalExpense;
            this.selected = selected;
        }
    }

    private final List<DayCell> items = new ArrayList<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
    private OnDayClickListener listener;

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<DayCell> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ledger_calendar_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DayCell item = items.get(position);

        holder.tvDay.setText(String.valueOf(item.day));
        holder.tvExpense.setText(formatExpense(item.totalExpense));

        if (item.selected) {
            holder.cardRoot.setCardBackgroundColor(0xFFEAF5FF);
            holder.cardRoot.setStrokeColor(0xFF7FC8F8);
            holder.cardRoot.setStrokeWidth(dp(holder.itemView, 1));
        } else {
            holder.cardRoot.setCardBackgroundColor(0xFFFFFFFF);
            holder.cardRoot.setStrokeColor(0xFFE7EDF5);
            holder.cardRoot.setStrokeWidth(dp(holder.itemView, 1));
        }

        if (item.inCurrentMonth) {
            holder.tvDay.setTextColor(0xFF2E3A4B);
            holder.tvExpense.setTextColor(0xFF5E748A);
        } else {
            holder.tvDay.setTextColor(0xFFB9C1CC);
            holder.tvExpense.setTextColor(0xFFCBD2DB);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatExpense(long totalExpense) {
        if (totalExpense <= 0) return "";

        if (totalExpense < 10000) {
            return "-" + moneyFormat.format(totalExpense);
        }

        if (totalExpense < 100000) {
            return "-" + moneyFormat.format(totalExpense);
        }

        if (totalExpense < 1000000) {
            long man = Math.round(totalExpense / 10000.0);
            return "-" + man + "만";
        }

        double man = totalExpense / 10000.0;
        return String.format(Locale.KOREA, "-%.1f만", man);
    }

    private int dp(View view, int value) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                value,
                view.getResources().getDisplayMetrics()
        );
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvDay;
        TextView tvExpense;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardCalendarDay);
            tvDay = itemView.findViewById(R.id.tvCalendarDay);
            tvExpense = itemView.findViewById(R.id.tvCalendarExpense);
        }
    }
}