package com.namgyun.tamakitchen.ui.shopping;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.shopping.StoreSpendPickerDialog.StoreSpendRow;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarDialogFragment extends DialogFragment {

    public interface OnDateStorePickedListener {
        void onPicked(int year, int month0, int day, @Nullable Long storeId, @NonNull String storeName);
    }

    public interface MonthSpendProvider {
        @NonNull Map<Integer, Long> getDayTotalsForMonth(int year, int month0);
        long getMonthTotal(int year, int month0);

        @NonNull Map<Long, Long> getStoreTotalsForDate(@NonNull String dateKey);
        @NonNull Map<Long, String> getStoreNamesForDate(@NonNull String dateKey);
    }

    private static final String ARG_YEAR = "ARG_YEAR";
    private static final String ARG_MONTH0 = "ARG_MONTH0";
    private static final String ARG_DAY = "ARG_DAY";

    private int year;
    private int month0;
    private int selectedDay;

    private TextView tvMonthTitle;
    private TextView tvMonthTotal;
    private TextView tvStoreTop3;
    private RecyclerView rvCalendarDays;

    private MonthSpendProvider provider;
    private OnDateStorePickedListener pickedListener;

    private final DecimalFormat money = new DecimalFormat("#,###");
    private CalendarDayInnerAdapter adapter;

    public static CalendarDialogFragment newInstance(int year, int month0, int day) {
        CalendarDialogFragment f = new CalendarDialogFragment();

        Bundle b = new Bundle();
        b.putInt(ARG_YEAR, year);
        b.putInt(ARG_MONTH0, month0);
        b.putInt(ARG_DAY, day);

        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        readArgs();

        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_calendar, null);

        tvMonthTitle = root.findViewById(R.id.tvMonthTitle);
        View btnPrev = root.findViewById(R.id.btnPrevMonth);
        View btnNext = root.findViewById(R.id.btnNextMonth);

        rvCalendarDays = root.findViewById(R.id.rvCalendarDays);
        tvMonthTotal = root.findViewById(R.id.tvMonthTotal);
        tvStoreTop3 = root.findViewById(R.id.tvStoreTop3);

        Host host = findHost();
        provider = host != null ? host.provider : null;
        pickedListener = host != null ? host.listener : null;

        if (rvCalendarDays != null) {
            rvCalendarDays.setLayoutManager(new GridLayoutManager(requireContext(), 7));
            rvCalendarDays.setOverScrollMode(View.OVER_SCROLL_NEVER);
            rvCalendarDays.setHasFixedSize(true);

            adapter = new CalendarDayInnerAdapter(requireContext(), this::onDayClicked);
            rvCalendarDays.setAdapter(adapter);
        }

        if (rvCalendarDays != null) {
            rvCalendarDays.post(this::renderCalendar);
        } else {
            renderCalendar();
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> moveMonth(-1));
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> moveMonth(1));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        return dialog;
    }

    private void moveMonth(int offset) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, offset);

        year = c.get(Calendar.YEAR);
        month0 = c.get(Calendar.MONTH);
        selectedDay = 1;

        renderCalendar();
    }

    private void renderCalendar() {
        if (tvMonthTitle != null) {
            tvMonthTitle.setText(String.format(Locale.KOREA, "%d년 %d월", year, month0 + 1));
        }

        Map<Integer, Long> dayTotals = provider != null
                ? provider.getDayTotalsForMonth(year, month0)
                : Collections.emptyMap();

        long monthTotal = provider != null ? provider.getMonthTotal(year, month0) : 0L;

        if (tvMonthTotal != null) {
            tvMonthTotal.setText("이번 달 총 지출  -" + money.format(Math.max(0, monthTotal)) + "원");
        }

        if (tvStoreTop3 != null) {
            List<StoreSpendRow> top3 = computeMonthTop3(provider);

            if (top3.isEmpty()) {
                tvStoreTop3.setText("");
            } else {
                StringBuilder sb = new StringBuilder();

                int count = Math.min(3, top3.size());
                for (int i = 0; i < count; i++) {
                    StoreSpendRow r = top3.get(i);

                    sb.append(r.storeName)
                            .append("  -")
                            .append(money.format(Math.max(0, r.amount)))
                            .append("원");

                    if (i != count - 1) sb.append("\n");
                }

                tvStoreTop3.setText(sb.toString());
            }
        }

        if (adapter != null) {
            adapter.setMonth(year, month0, selectedDay, dayTotals);
        }

        // ✅ 핵심 수정: RecyclerView 실제 너비를 7등분해서 셀 너비 고정
        if (rvCalendarDays != null && adapter != null) {
            rvCalendarDays.post(() -> {
                int width = rvCalendarDays.getWidth()
                        - rvCalendarDays.getPaddingLeft()
                        - rvCalendarDays.getPaddingRight();

                if (width > 0) {
                    adapter.setCellWidthPx(width / 7);
                }
            });
        }
    }

    private void onDayClicked(int y, int m0, int d) {
        String dateKey = String.format(Locale.KOREA, "%04d-%02d-%02d", y, m0 + 1, d);

        Map<Integer, Long> dayTotals = provider != null
                ? provider.getDayTotalsForMonth(y, m0)
                : Collections.emptyMap();

        long daySpend = dayTotals.containsKey(d) ? Math.max(0, dayTotals.get(d)) : 0L;

        if (daySpend <= 0) {
            if (pickedListener != null) {
                Long sid = StoreSessionManager.getCurrentStoreId(requireContext());
                String sname = StoreSessionManager.getCurrentStoreName(requireContext());

                if (sname == null || sname.trim().isEmpty()) {
                    sname = "미지정";
                }

                pickedListener.onPicked(y, m0, d, sid, sname);
            }

            dismiss();
            return;
        }

        Map<Long, Long> storeTotals = provider != null
                ? provider.getStoreTotalsForDate(dateKey)
                : new HashMap<>();

        Map<Long, String> storeNames = provider != null
                ? provider.getStoreNamesForDate(dateKey)
                : new HashMap<>();

        ArrayList<StoreSpendRow> rows = new ArrayList<>();

        for (Map.Entry<Long, Long> e : storeTotals.entrySet()) {
            Long storeId = e.getKey();
            long amt = Math.max(0, e.getValue() == null ? 0 : e.getValue());

            String name = storeNames.containsKey(storeId) ? storeNames.get(storeId) : "미지정";
            if (name == null || name.trim().isEmpty()) name = "미지정";

            rows.add(new StoreSpendRow(storeId, name, amt));
        }

        rows.sort((a, b) -> Long.compare(b.amount, a.amount));

        if (rows.isEmpty()) {
            if (pickedListener != null) {
                Long sid = StoreSessionManager.getCurrentStoreId(requireContext());
                String sname = StoreSessionManager.getCurrentStoreName(requireContext());

                if (sname == null || sname.trim().isEmpty()) {
                    sname = "미지정";
                }

                pickedListener.onPicked(y, m0, d, sid, sname);
            }

            dismiss();
            return;
        }

        if (rows.size() == 1) {
            StoreSpendRow r = rows.get(0);

            if (pickedListener != null) {
                pickedListener.onPicked(y, m0, d, r.storeId, r.storeName);
            }

            dismiss();
            return;
        }

        StoreSpendPickerDialog picker = StoreSpendPickerDialog.newInstance(
                dateKey,
                rows,
                (pickedDateKey, storeId, storeName) -> {
                    if (pickedListener != null) {
                        pickedListener.onPicked(y, m0, d, storeId, storeName);
                    }

                    dismiss();
                }
        );

        picker.show(getParentFragmentManager(), "StoreSpendPickerDialog");
    }

    private void readArgs() {
        Bundle a = getArguments();
        Calendar now = Calendar.getInstance();

        if (a == null) {
            year = now.get(Calendar.YEAR);
            month0 = now.get(Calendar.MONTH);
            selectedDay = now.get(Calendar.DAY_OF_MONTH);
            return;
        }

        year = a.getInt(ARG_YEAR, now.get(Calendar.YEAR));
        month0 = a.getInt(ARG_MONTH0, now.get(Calendar.MONTH));
        selectedDay = a.getInt(ARG_DAY, now.get(Calendar.DAY_OF_MONTH));
    }

    private static class Host {
        MonthSpendProvider provider;
        OnDateStorePickedListener listener;

        Host(MonthSpendProvider p, OnDateStorePickedListener l) {
            provider = p;
            listener = l;
        }
    }

    @Nullable
    private Host findHost() {
        List<Fragment> fragments = requireActivity().getSupportFragmentManager().getFragments();

        Host h = findHostInList(fragments);
        if (h != null) return h;

        for (Fragment f : fragments) {
            if (f == null) continue;

            Host deep = findHostInList(f.getChildFragmentManager().getFragments());
            if (deep != null) return deep;
        }

        return null;
    }

    @Nullable
    private Host findHostInList(@NonNull List<Fragment> list) {
        for (Fragment f : list) {
            if (f == null) continue;

            if (f instanceof MonthSpendProvider && f instanceof OnDateStorePickedListener) {
                return new Host((MonthSpendProvider) f, (OnDateStorePickedListener) f);
            }
        }

        return null;
    }

    @NonNull
    private List<StoreSpendRow> computeMonthTop3(@Nullable MonthSpendProvider provider) {
        if (provider == null) return Collections.emptyList();

        HashMap<Long, Long> storeSum = new HashMap<>();
        HashMap<Long, String> storeNameAny = new HashMap<>();

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month0);
        c.set(Calendar.DAY_OF_MONTH, 1);

        int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= max; d++) {
            String dateKey = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month0 + 1, d);

            Map<Long, Long> totals = provider.getStoreTotalsForDate(dateKey);
            Map<Long, String> names = provider.getStoreNamesForDate(dateKey);

            for (Map.Entry<Long, Long> e : totals.entrySet()) {
                Long sid = e.getKey();

                long amt = Math.max(0, e.getValue() == null ? 0 : e.getValue());
                long prev = storeSum.containsKey(sid) ? storeSum.get(sid) : 0L;

                storeSum.put(sid, prev + amt);

                if (!storeNameAny.containsKey(sid)) {
                    String nm = names.containsKey(sid) ? names.get(sid) : "미지정";

                    if (nm == null || nm.trim().isEmpty()) {
                        nm = "미지정";
                    }

                    storeNameAny.put(sid, nm);
                }
            }
        }

        ArrayList<StoreSpendRow> rows = new ArrayList<>();

        for (Map.Entry<Long, Long> e : storeSum.entrySet()) {
            Long sid = e.getKey();
            long amt = Math.max(0, e.getValue());

            String nm = storeNameAny.containsKey(sid) ? storeNameAny.get(sid) : "미지정";
            if (nm == null || nm.trim().isEmpty()) nm = "미지정";

            rows.add(new StoreSpendRow(sid, nm, amt));
        }

        rows.sort((a, b) -> Long.compare(b.amount, a.amount));

        if (rows.size() > 3) {
            return rows.subList(0, 3);
        }

        return rows;
    }

    private interface OnCalendarDayClickListener {
        void onClick(int year, int month0, int day);
    }

    private static class CalendarDayCell {
        final int year;
        final int month0;
        final int day;
        final boolean empty;
        final long amount;
        final boolean selected;
        final int dayOfWeek;

        CalendarDayCell(int year,
                        int month0,
                        int day,
                        boolean empty,
                        long amount,
                        boolean selected,
                        int dayOfWeek) {
            this.year = year;
            this.month0 = month0;
            this.day = day;
            this.empty = empty;
            this.amount = amount;
            this.selected = selected;
            this.dayOfWeek = dayOfWeek;
        }
    }

    private static class CalendarDayInnerAdapter extends RecyclerView.Adapter<CalendarDayInnerAdapter.VH> {

        private final Context context;
        private final DecimalFormat money = new DecimalFormat("#,###");
        private final OnCalendarDayClickListener listener;
        private final ArrayList<CalendarDayCell> cells = new ArrayList<>();

        private int cellWidthPx = 0;

        CalendarDayInnerAdapter(@NonNull Context context,
                                @NonNull OnCalendarDayClickListener listener) {
            this.context = context;
            this.listener = listener;
        }

        void setCellWidthPx(int widthPx) {
            if (widthPx <= 0) return;
            cellWidthPx = widthPx;
            notifyDataSetChanged();
        }

        void setMonth(int year,
                      int month0,
                      int selectedDay,
                      @NonNull Map<Integer, Long> dayTotals) {
            cells.clear();

            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month0);
            c.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            int startOffset = firstDayOfWeek - Calendar.SUNDAY;
            int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < startOffset; i++) {
                cells.add(new CalendarDayCell(year, month0, 0, true, 0, false, Calendar.SUNDAY + i));
            }

            for (int day = 1; day <= maxDay; day++) {
                c.set(Calendar.DAY_OF_MONTH, day);

                int dow = c.get(Calendar.DAY_OF_WEEK);

                long amount = dayTotals.containsKey(day)
                        ? Math.max(0, dayTotals.get(day))
                        : 0L;

                cells.add(new CalendarDayCell(
                        year,
                        month0,
                        day,
                        false,
                        amount,
                        day == selectedDay,
                        dow
                ));
            }

            while (cells.size() % 7 != 0) {
                cells.add(new CalendarDayCell(year, month0, 0, true, 0, false, Calendar.SATURDAY));
            }

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            root.setPadding(0, dp(4), 0, dp(2));

            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    cellWidthPx > 0 ? cellWidthPx : ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(58)
            );
            root.setLayoutParams(lp);

            TextView tvDay = new TextView(context);
            tvDay.setGravity(Gravity.CENTER);
            tvDay.setTextSize(16);
            tvDay.setTypeface(Typeface.DEFAULT_BOLD);
            tvDay.setTextColor(Color.parseColor("#2E3A4B"));
            tvDay.setIncludeFontPadding(false);

            LinearLayout.LayoutParams dayLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(24)
            );
            tvDay.setLayoutParams(dayLp);

            TextView tvAmount = new TextView(context);
            tvAmount.setGravity(Gravity.CENTER);
            tvAmount.setTextSize(10);
            tvAmount.setTypeface(Typeface.DEFAULT_BOLD);
            tvAmount.setTextColor(Color.parseColor("#2E74FF"));
            tvAmount.setIncludeFontPadding(false);
            tvAmount.setMaxLines(2);

            LinearLayout.LayoutParams amountLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(28)
            );
            tvAmount.setLayoutParams(amountLp);

            root.addView(tvDay);
            root.addView(tvAmount);

            return new VH(root, tvDay, tvAmount);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CalendarDayCell cell = cells.get(position);

            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            if (cellWidthPx > 0 && lp.width != cellWidthPx) {
                lp.width = cellWidthPx;
                holder.itemView.setLayoutParams(lp);
            }

            if (cell.empty) {
                holder.itemView.setVisibility(View.INVISIBLE);
                holder.tvDay.setText("");
                holder.tvAmount.setText("");
                holder.itemView.setOnClickListener(null);
                return;
            }

            holder.itemView.setVisibility(View.VISIBLE);
            holder.tvDay.setText(String.valueOf(cell.day));

            if (cell.dayOfWeek == Calendar.SUNDAY) {
                holder.tvDay.setTextColor(Color.parseColor("#E64A4A"));
            } else if (cell.dayOfWeek == Calendar.SATURDAY) {
                holder.tvDay.setTextColor(Color.parseColor("#2E74FF"));
            } else {
                holder.tvDay.setTextColor(Color.parseColor("#2E3A4B"));
            }

            if (cell.amount > 0) {
                holder.tvAmount.setText("-" + money.format(cell.amount) + "\n원");
                holder.tvAmount.setVisibility(View.VISIBLE);
            } else {
                holder.tvAmount.setText("");
                holder.tvAmount.setVisibility(View.INVISIBLE);
            }

            holder.itemView.setOnClickListener(v ->
                    listener.onClick(cell.year, cell.month0, cell.day)
            );
        }

        @Override
        public int getItemCount() {
            return cells.size();
        }

        private int dp(int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvDay;
            final TextView tvAmount;

            VH(@NonNull View itemView, @NonNull TextView tvDay, @NonNull TextView tvAmount) {
                super(itemView);
                this.tvDay = tvDay;
                this.tvAmount = tvAmount;
            }
        }
    }
}