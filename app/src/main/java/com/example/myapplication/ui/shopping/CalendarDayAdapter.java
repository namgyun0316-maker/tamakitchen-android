// File: app/src/main/java/com/example/myapplication/ui/shopping/CalendarDayAdapter.java
package com.namgyun.tamakitchen.ui.shopping;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.VH> {

    public interface OnDayClickListener {
        void onDayClick(int year, int month0, int day);
    }

    private final Context context;
    private final OnDayClickListener listener;

    // 7x6 = 42칸
    private final List<DayCell> cells = new ArrayList<>();

    private int year;
    private int month0;

    // day(1~31) -> amount(양수)
    private final Map<Integer, Long> dayAmountMap = new HashMap<>();

    private final DecimalFormat df = new DecimalFormat("#,###");

    public CalendarDayAdapter(Context context, OnDayClickListener listener) {
        this.context = context;
        this.listener = listener;

        Calendar c = Calendar.getInstance();
        setMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH), null);
    }

    public void setMonth(int year, int month0, Map<Integer, Long> dayTotals) {
        this.year = year;
        this.month0 = month0;

        dayAmountMap.clear();
        if (dayTotals != null) dayAmountMap.putAll(dayTotals);

        cells.clear();

        Calendar first = Calendar.getInstance();
        first.set(Calendar.YEAR, year);
        first.set(Calendar.MONTH, month0);
        first.set(Calendar.DAY_OF_MONTH, 1);

        int firstDow = first.get(Calendar.DAY_OF_WEEK); // 1=일,2=월...
        int startOffset = firstDow - Calendar.SUNDAY;   // 0이면 일요일 시작
        if (startOffset < 0) startOffset += 7;

        int maxDay = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 앞 빈칸
        for (int i = 0; i < startOffset; i++) {
            cells.add(DayCell.empty());
        }

        // 날짜
        for (int d = 1; d <= maxDay; d++) {
            cells.add(DayCell.day(d));
        }

        // 뒤 빈칸(42 맞추기)
        while (cells.size() < 42) cells.add(DayCell.empty());

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DayCell cell = cells.get(position);

        if (!cell.isDay) {
            holder.tvDay.setText("");
            holder.tvAmount.setText("");
            holder.itemView.setOnClickListener(null);
            holder.itemView.setEnabled(false);
            holder.itemView.setAlpha(0.12f);
            return;
        }

        holder.itemView.setEnabled(true);
        holder.itemView.setAlpha(1f);

        holder.tvDay.setText(String.valueOf(cell.day));

        long amount = dayAmountMap.get(cell.day) == null ? 0L : dayAmountMap.get(cell.day);

        // ✅ 지출 표기 "-9,000원" / 큰 금액은 2줄로 줄바꿈해서 ... 방지
        if (amount > 0) {
            holder.tvAmount.setText(formatAmountForCell(amount));
            holder.tvAmount.setVisibility(View.VISIBLE);
        } else {
            holder.tvAmount.setText("");
            holder.tvAmount.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(year, month0, cell.day);
        });
    }

    private String formatAmountForCell(long amount) {
        // 기본: -5,000원
        String s = "-" + df.format(amount) + "원";

        // ✅ 100,000원 이상이면 콤마 뒤에 줄바꿈 넣어서 폭 부족 해결
        // 예: -110,666원 -> -110,\n666원
        if (amount >= 100_000) {
            int idx = s.lastIndexOf(',');
            if (idx > 0) {
                s = s.substring(0, idx + 1) + "\n" + s.substring(idx + 1);
            }
        }
        return s;
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDay, tvAmount;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }

    static class DayCell {
        boolean isDay;
        int day;

        static DayCell empty() {
            DayCell c = new DayCell();
            c.isDay = false;
            c.day = 0;
            return c;
        }

        static DayCell day(int d) {
            DayCell c = new DayCell();
            c.isDay = true;
            c.day = d;
            return c;
        }
    }
}
