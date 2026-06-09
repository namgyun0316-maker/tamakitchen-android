package com.namgyun.tamakitchen.ui.menu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.namgyun.tamakitchen.ui.shopping.ReceiptReviewActivity;
import com.namgyun.tamakitchen.ui.shopping.ReceiptScannerActivity;
import com.namgyun.tamakitchen.ui.shopping.StorePickerDialog;
import com.namgyun.tamakitchen.ui.shopping.StoreSessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LedgerActivity extends AppCompatActivity {

    private TextView tvMonthTitle;
    private TextView btnPrevMonth;
    private TextView btnNextMonth;

    private TextView tvMonthTotal;
    private TextView tvMonthCount;
    private TextView tvStoreSummary;
    private TextView tvEmpty;
    private TextView tvChartSub;

    private TextView btnPrevDay;
    private TextView btnNextDay;
    private TextView tvSelectedDate;

    private MaterialButton btnAddManual;
    private MaterialButton btnLedgerImportReceipt;
    private RecyclerView rvLedger;

    private HorizontalScrollView hsvStoreFilter;
    private ChipGroup chipGroupStores;
    private LedgerMonthlyChartView viewLedgerChart;
    private NestedScrollView nsvLedgerRoot;
    private LinearLayout layoutLedgerContent;

    private LedgerAdapter adapter;
    private Calendar displayMonth;
    private Calendar selectedDate;
    private long currentUserId = -1L;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
    private final FridgeModeManager fridgeModeManager = new FridgeModeManager();

    private final List<LedgerEntry> monthEntries = new ArrayList<>();
    private String selectedStoreFilter = "전체";

    private ActivityResultLauncher<Intent> receiptScanLauncher;
    private LedgerRepository ledgerRepository;

    private boolean firstRenderDone = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ledger);

        tvMonthTitle = findViewById(R.id.tvLedgerMonthTitle);
        btnPrevMonth = findViewById(R.id.btnLedgerPrevMonth);
        btnNextMonth = findViewById(R.id.btnLedgerNextMonth);

        tvMonthTotal = findViewById(R.id.tvLedgerMonthTotal);
        tvMonthCount = findViewById(R.id.tvLedgerMonthCount);
        tvStoreSummary = findViewById(R.id.tvLedgerStoreSummary);
        tvEmpty = findViewById(R.id.tvLedgerEmpty);
        tvChartSub = findViewById(R.id.tvLedgerChartSub);

        btnPrevDay = findViewById(R.id.btnLedgerPrevDay);
        btnNextDay = findViewById(R.id.btnLedgerNextDay);
        tvSelectedDate = findViewById(R.id.tvLedgerSelectedDate);

        btnAddManual = findViewById(R.id.btnLedgerAddManual);
        btnLedgerImportReceipt = findViewById(R.id.btnLedgerImportReceipt);
        rvLedger = findViewById(R.id.rvLedger);

        hsvStoreFilter = findViewById(R.id.hsvLedgerStoreFilter);
        chipGroupStores = findViewById(R.id.chipGroupLedgerStores);
        viewLedgerChart = findViewById(R.id.viewLedgerChart);
        nsvLedgerRoot = findViewById(R.id.nsvLedgerRoot);
        layoutLedgerContent = findViewById(R.id.layoutLedgerContent);

        adapter = new LedgerAdapter();
        adapter.setOnEntryClickListener(this::showEntryMenuDialog);

        rvLedger.setLayoutManager(new LinearLayoutManager(this));
        rvLedger.setNestedScrollingEnabled(false);
        rvLedger.setItemAnimator(null);
        rvLedger.setAdapter(adapter);

        displayMonth = Calendar.getInstance(Locale.KOREA);
        displayMonth.set(Calendar.DAY_OF_MONTH, 1);

        selectedDate = Calendar.getInstance(Locale.KOREA);

        currentUserId = fridgeModeManager.getUserIdSafe(this);
        ledgerRepository = new LedgerRepository();

        layoutLedgerContent.setAlpha(0f);

        nsvLedgerRoot.setSaveEnabled(false);
        rvLedger.setSaveEnabled(false);

        receiptScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        AppToast.show(this, "스캔이 취소됐어.");
                        return;
                    }

                    String raw = result.getData().getStringExtra(ReceiptReviewActivity.EXTRA_RAW_TEXT);
                    if (raw == null) raw = result.getData().getStringExtra("EXTRA_RAW_TEXT");
                    if (raw == null) raw = "";
                    raw = raw.trim();

                    if (raw.isEmpty()) {
                        AppToast.show(this, "인식된 영수증 텍스트가 없어.");
                        return;
                    }

                    Intent intent = new Intent(this, LedgerReceiptImportActivity.class);
                    intent.putExtra(LedgerReceiptImportActivity.EXTRA_RAW_TEXT, raw);
                    startActivity(intent);
                }
        );

        btnPrevMonth.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, -1);
            clampSelectedDateToMonth();
            selectedStoreFilter = "전체";
            refreshUi();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, 1);
            clampSelectedDateToMonth();
            selectedStoreFilter = "전체";
            refreshUi();
        });

        btnPrevDay.setOnClickListener(v -> moveSelectedDay(-1));
        btnNextDay.setOnClickListener(v -> moveSelectedDay(1));
        tvSelectedDate.setOnClickListener(v -> showCalendarDialog());
        tvMonthTitle.setOnClickListener(v -> showMonthPickerDialog());

        btnAddManual.setOnClickListener(v -> showEntryEditDialog(null));

        if (btnLedgerImportReceipt != null) {
            btnLedgerImportReceipt.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReceiptScannerActivity.class);
                receiptScanLauncher.launch(intent);
            });
        }

        forceScrollToTopImmediate();

        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        forceScrollToTopImmediate();
        refreshUi();
    }

    private boolean isGuestMode() {
        return currentUserId <= 0L;
    }

    private void refreshUi() {
        int year = displayMonth.get(Calendar.YEAR);
        int month = displayMonth.get(Calendar.MONTH) + 1;

        tvMonthTitle.setText(String.format(Locale.KOREA, "%d년 %d월", year, month));
        updateSelectedDateText();

        if (isGuestMode()) {
            loadFromLocalFallback(year, month);
            return;
        }

        ledgerRepository.getEntries(currentUserId, year, month, new Callback<List<LedgerEntryDto>>() {
            @Override
            public void onResponse(Call<List<LedgerEntryDto>> call, Response<List<LedgerEntryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    monthEntries.clear();

                    List<LedgerEntry> localList = new ArrayList<>();
                    for (LedgerEntryDto dto : response.body()) {
                        if (dto == null) continue;
                        LedgerEntry entry = dto.toLocal();
                        monthEntries.add(entry);
                        localList.add(entry);
                    }

                    LedgerLocalStore.saveAll(LedgerActivity.this, localList);
                    renderLedgerScreen();
                } else {
                    loadFromLocalFallback(year, month);
                }
            }

            @Override
            public void onFailure(Call<List<LedgerEntryDto>> call, Throwable t) {
                loadFromLocalFallback(year, month);
            }
        });
    }

    private void loadFromLocalFallback(int year, int month) {
        monthEntries.clear();
        monthEntries.addAll(LedgerLocalStore.getEntriesForMonth(this, year, month));
        renderLedgerScreen();
    }

    private void renderLedgerScreen() {
        bindStoreFilterChips(monthEntries);
        bindSummary(monthEntries);
        applyDateAndStoreFilterAndRender();
        bindChart();

        if (!firstRenderDone) {
            layoutLedgerContent.post(() -> {
                layoutLedgerContent.animate()
                        .alpha(1f)
                        .setDuration(120)
                        .start();
                firstRenderDone = true;
                forceScrollToTopImmediate();
            });
        } else {
            layoutLedgerContent.setAlpha(1f);
            forceScrollToTopImmediate();
        }
    }

    private void bindSummary(List<LedgerEntry> entries) {
        long total = 0L;
        Map<String, Long> storeTotals = new LinkedHashMap<>();

        for (LedgerEntry item : entries) {
            if (item == null) continue;

            total += item.getTotalPrice();

            String store = safeStoreName(item.getStoreName());
            Long prev = storeTotals.get(store);
            storeTotals.put(store, (prev == null ? 0L : prev) + item.getTotalPrice());
        }

        tvMonthTotal.setText(moneyFormat.format(total) + "원");
        tvMonthCount.setText("총 " + entries.size() + "건");

        if (storeTotals.isEmpty()) {
            tvStoreSummary.setText("판매점 내역이 없어.");
        } else {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Map.Entry<String, Long> e : storeTotals.entrySet()) {
                if (i > 0) sb.append(" · ");
                sb.append(e.getKey()).append(" ").append(moneyFormat.format(e.getValue())).append("원");
                i++;
                if (i >= 3 && storeTotals.size() > 3) {
                    sb.append(" 외 ").append(storeTotals.size() - 3).append("곳");
                    break;
                }
            }
            tvStoreSummary.setText(sb.toString());
        }
    }

    private void bindChart() {
        List<LedgerMonthlyChartView.ChartItem> chartItems = new ArrayList<>();
        Calendar cal = (Calendar) displayMonth.clone();
        cal.add(Calendar.MONTH, -5);

        for (int i = 0; i < 6; i++) {
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;

            List<LedgerEntry> entries;
            if (year == displayMonth.get(Calendar.YEAR)
                    && month == displayMonth.get(Calendar.MONTH) + 1) {
                entries = new ArrayList<>(monthEntries);
            } else {
                entries = LedgerLocalStore.getEntriesForMonth(this, year, month);
            }

            long total = 0L;
            for (LedgerEntry entry : entries) {
                if (entry == null) continue;
                total += entry.getTotalPrice();
            }

            chartItems.add(new LedgerMonthlyChartView.ChartItem(month + "월", total));
            cal.add(Calendar.MONTH, 1);
        }

        viewLedgerChart.setItems(chartItems);

        long current = 0L;
        long previous = 0L;
        if (chartItems.size() >= 1) current = chartItems.get(chartItems.size() - 1).value;
        if (chartItems.size() >= 2) previous = chartItems.get(chartItems.size() - 2).value;

        String chartText;
        if (current > previous) {
            chartText = "지난달보다 " + moneyFormat.format(current - previous) + "원 더 썼어.";
        } else if (current < previous) {
            chartText = "지난달보다 " + moneyFormat.format(previous - current) + "원 덜 썼어.";
        } else {
            chartText = "지난달과 지출이 같아.";
        }
        tvChartSub.setText(chartText);
    }

    private void bindStoreFilterChips(List<LedgerEntry> entries) {
        chipGroupStores.removeAllViews();

        List<String> stores = new ArrayList<>();
        stores.add("전체");

        for (LedgerEntry item : entries) {
            if (item == null) continue;
            String store = safeStoreName(item.getStoreName());
            if (!stores.contains(store)) {
                stores.add(store);
            }
        }

        for (String store : stores) {
            Chip chip = new Chip(this);
            chip.setText(store);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setChipCornerRadius(dp(16));
            chip.setChipBackgroundColorResource(R.color.selector_chip_bg_fallback);
            chip.setTextColor(getResources().getColorStateList(R.color.selector_chip_text_fallback, getTheme()));
            chip.setChecked(store.equals(selectedStoreFilter));

            chip.setOnClickListener(v -> {
                selectedStoreFilter = store;
                updateChipCheckedState();
                applyDateAndStoreFilterAndRender();
            });

            chipGroupStores.addView(chip);
        }

        hsvStoreFilter.setVisibility(stores.size() > 1 ? View.VISIBLE : View.GONE);
        updateChipCheckedState();
    }

    private void updateChipCheckedState() {
        for (int i = 0; i < chipGroupStores.getChildCount(); i++) {
            View child = chipGroupStores.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(chip.getText() != null
                        && chip.getText().toString().equals(selectedStoreFilter));
            }
        }
    }

    private void applyDateAndStoreFilterAndRender() {
        List<LedgerEntry> filtered = new ArrayList<>();
        String selectedDateKey = getSelectedDateKey();

        for (LedgerEntry item : monthEntries) {
            if (item == null) continue;

            boolean storeMatch = "전체".equals(selectedStoreFilter)
                    || safeStoreName(item.getStoreName()).equals(selectedStoreFilter);

            boolean dateMatch = selectedDateKey.equals(item.getDate());

            if (storeMatch && dateMatch) {
                filtered.add(item);
            }
        }

        adapter.submit(filtered);

        boolean empty = filtered.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvLedger.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            tvEmpty.setText(formatSelectedDateText() + " 내역이 아직 없어.");
        }

        forceScrollToTopImmediate();
    }

    private void moveSelectedDay(int diff) {
        selectedDate.add(Calendar.DAY_OF_MONTH, diff);

        displayMonth.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
        displayMonth.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        displayMonth.set(Calendar.DAY_OF_MONTH, 1);

        refreshUi();
    }

    private void clampSelectedDateToMonth() {
        int maxDay = displayMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDay = selectedDate.get(Calendar.DAY_OF_MONTH);

        selectedDate.set(Calendar.YEAR, displayMonth.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, displayMonth.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH, Math.min(currentDay, maxDay));
    }

    private void updateSelectedDateText() {
        tvSelectedDate.setText(formatSelectedDateText());
    }

    private String formatSelectedDateText() {
        return String.format(
                Locale.KOREA,
                "%02d.%02d",
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
    }

    private String getSelectedDateKey() {
        return String.format(
                Locale.KOREA,
                "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
    }

    private void showCalendarDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_ledger_calendar, null);

        TextView tvTitle = view.findViewById(R.id.tvCalendarDialogTitle);
        TextView btnPrev = view.findViewById(R.id.btnCalendarPrev);
        TextView btnNext = view.findViewById(R.id.btnCalendarNext);
        RecyclerView rvCalendar = view.findViewById(R.id.rvLedgerCalendarDialog);

        Calendar tempMonth = (Calendar) displayMonth.clone();

        LedgerCalendarAdapter dialogAdapter = new LedgerCalendarAdapter();
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        rvCalendar.setNestedScrollingEnabled(false);
        rvCalendar.setItemAnimator(null);
        rvCalendar.setAdapter(dialogAdapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        Runnable updateCalendar = () -> {
            tvTitle.setText(String.format(
                    Locale.KOREA,
                    "%d년 %d월",
                    tempMonth.get(Calendar.YEAR),
                    tempMonth.get(Calendar.MONTH) + 1
            ));
            dialogAdapter.submit(buildCalendarCells(tempMonth));
        };

        btnPrev.setOnClickListener(v -> {
            tempMonth.add(Calendar.MONTH, -1);
            updateCalendar.run();
        });

        btnNext.setOnClickListener(v -> {
            tempMonth.add(Calendar.MONTH, 1);
            updateCalendar.run();
        });

        tvTitle.setOnClickListener(v -> showMonthPickerDialogForCalendar(tempMonth, dialogAdapter, tvTitle));

        dialogAdapter.setOnDayClickListener(item -> {
            if (item == null) return;

            selectedDate.set(Calendar.YEAR, Integer.parseInt(item.dateKey.substring(0, 4)));
            selectedDate.set(Calendar.MONTH, Integer.parseInt(item.dateKey.substring(5, 7)) - 1);
            selectedDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(item.dateKey.substring(8, 10)));

            displayMonth.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
            displayMonth.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
            displayMonth.set(Calendar.DAY_OF_MONTH, 1);

            updateSelectedDateText();
            refreshUi();
            dialog.dismiss();
        });

        dialog.show();
        rvCalendar.post(updateCalendar);
    }

    private void showMonthPickerDialog() {
        Calendar temp = (Calendar) displayMonth.clone();

        View view = getLayoutInflater().inflate(R.layout.dialog_ledger_month_picker, null);
        TextView tvYear = view.findViewById(R.id.tvMonthPickerYear);
        TextView btnPrevYear = view.findViewById(R.id.btnMonthPickerPrevYear);
        TextView btnNextYear = view.findViewById(R.id.btnMonthPickerNextYear);
        GridLayout gridMonth = view.findViewById(R.id.gridMonthPicker);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        Runnable renderMonths = () -> {
            tvYear.setText(String.format(Locale.KOREA, "%d년", temp.get(Calendar.YEAR)));
            buildMonthGrid(gridMonth, month -> {
                displayMonth.set(Calendar.YEAR, temp.get(Calendar.YEAR));
                displayMonth.set(Calendar.MONTH, month);
                displayMonth.set(Calendar.DAY_OF_MONTH, 1);

                selectedDate.set(Calendar.YEAR, temp.get(Calendar.YEAR));
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, 1);

                refreshUi();
                dialog.dismiss();
            });
        };

        btnPrevYear.setOnClickListener(v -> {
            temp.add(Calendar.YEAR, -1);
            renderMonths.run();
        });

        btnNextYear.setOnClickListener(v -> {
            temp.add(Calendar.YEAR, 1);
            renderMonths.run();
        });

        renderMonths.run();
        dialog.show();
    }

    private void showMonthPickerDialogForCalendar(
            Calendar tempMonth,
            LedgerCalendarAdapter dialogAdapter,
            TextView tvTitle
    ) {
        View view = getLayoutInflater().inflate(R.layout.dialog_ledger_month_picker, null);
        TextView tvYear = view.findViewById(R.id.tvMonthPickerYear);
        TextView btnPrevYear = view.findViewById(R.id.btnMonthPickerPrevYear);
        TextView btnNextYear = view.findViewById(R.id.btnMonthPickerNextYear);
        GridLayout gridMonth = view.findViewById(R.id.gridMonthPicker);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        Runnable renderMonths = () -> {
            tvYear.setText(String.format(Locale.KOREA, "%d년", tempMonth.get(Calendar.YEAR)));
            buildMonthGrid(gridMonth, month -> {
                tempMonth.set(Calendar.MONTH, month);
                tvTitle.setText(String.format(
                        Locale.KOREA,
                        "%d년 %d월",
                        tempMonth.get(Calendar.YEAR),
                        tempMonth.get(Calendar.MONTH) + 1
                ));
                dialogAdapter.submit(buildCalendarCells(tempMonth));
                dialog.dismiss();
            });
        };

        btnPrevYear.setOnClickListener(v -> {
            tempMonth.add(Calendar.YEAR, -1);
            renderMonths.run();
        });

        btnNextYear.setOnClickListener(v -> {
            tempMonth.add(Calendar.YEAR, 1);
            renderMonths.run();
        });

        renderMonths.run();
        dialog.show();
    }

    private interface OnMonthClickListener {
        void onClick(int monthZeroBased);
    }

    private void buildMonthGrid(GridLayout gridMonth, OnMonthClickListener listener) {
        gridMonth.removeAllViews();

        for (int i = 0; i < 12; i++) {
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(16));
            card.setCardElevation(0f);
            card.setStrokeColor(0xFFE7EDF5);
            card.setStrokeWidth(dp(1));
            card.setCardBackgroundColor(0xFFF8FBFF);
            card.setClickable(true);
            card.setFocusable(true);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(64);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            card.setLayoutParams(lp);

            TextView tv = new TextView(this);
            tv.setText(String.format(Locale.KOREA, "%d월", i + 1));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tv.setTextColor(0xFF2E3A4B);
            tv.setTypeface(null, Typeface.BOLD);

            card.addView(tv, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            final int month = i;
            card.setOnClickListener(v -> {
                if (listener != null) listener.onClick(month);
            });

            gridMonth.addView(card);
        }
    }

    private List<LedgerCalendarAdapter.DayCell> buildCalendarCells(Calendar targetMonth) {
        List<LedgerEntry> targetEntries = new ArrayList<>();

        for (LedgerEntry item : monthEntries) {
            if (item == null) continue;

            boolean storeMatch = "전체".equals(selectedStoreFilter)
                    || safeStoreName(item.getStoreName()).equals(selectedStoreFilter);

            if (storeMatch) targetEntries.add(item);
        }

        Map<String, Long> dateExpenseMap = new LinkedHashMap<>();
        for (LedgerEntry item : targetEntries) {
            String date = item.getDate();
            if (date == null || date.trim().isEmpty()) continue;

            Long prev = dateExpenseMap.get(date);
            dateExpenseMap.put(date, (prev == null ? 0L : prev) + item.getTotalPrice());
        }

        List<LedgerCalendarAdapter.DayCell> result = new ArrayList<>();

        Calendar firstDay = (Calendar) targetMonth.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);

        int startOffset = firstDay.get(Calendar.DAY_OF_WEEK) - 1;

        Calendar prevMonth = (Calendar) targetMonth.clone();
        prevMonth.add(Calendar.MONTH, -1);
        int prevMonthMaxDay = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = startOffset - 1; i >= 0; i--) {
            int day = prevMonthMaxDay - i;
            String dateKey = String.format(
                    Locale.KOREA,
                    "%04d-%02d-%02d",
                    prevMonth.get(Calendar.YEAR),
                    prevMonth.get(Calendar.MONTH) + 1,
                    day
            );
            long total = dateExpenseMap.containsKey(dateKey) ? dateExpenseMap.get(dateKey) : 0L;
            boolean selected = dateKey.equals(getSelectedDateKey());

            result.add(new LedgerCalendarAdapter.DayCell(dateKey, day, false, total, selected));
        }

        int maxDay = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDay; day++) {
            String dateKey = String.format(
                    Locale.KOREA,
                    "%04d-%02d-%02d",
                    targetMonth.get(Calendar.YEAR),
                    targetMonth.get(Calendar.MONTH) + 1,
                    day
            );
            long total = dateExpenseMap.containsKey(dateKey) ? dateExpenseMap.get(dateKey) : 0L;
            boolean selected = dateKey.equals(getSelectedDateKey());

            result.add(new LedgerCalendarAdapter.DayCell(dateKey, day, true, total, selected));
        }

        Calendar nextMonth = (Calendar) targetMonth.clone();
        nextMonth.add(Calendar.MONTH, 1);

        int nextDay = 1;
        while (result.size() < 35) {
            String dateKey = String.format(
                    Locale.KOREA,
                    "%04d-%02d-%02d",
                    nextMonth.get(Calendar.YEAR),
                    nextMonth.get(Calendar.MONTH) + 1,
                    nextDay
            );
            long total = dateExpenseMap.containsKey(dateKey) ? dateExpenseMap.get(dateKey) : 0L;
            boolean selected = dateKey.equals(getSelectedDateKey());

            result.add(new LedgerCalendarAdapter.DayCell(dateKey, nextDay, false, total, selected));
            nextDay++;
        }

        return result;
    }

    private void showEntryMenuDialog(LedgerEntry entry) {
        if (entry == null) return;

        String[] items = new String[]{"수정", "삭제", "취소"};

        new AlertDialog.Builder(this)
                .setTitle(entry.getItemName())
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showEntryEditDialog(entry);
                    } else if (which == 1) {
                        showDeleteDialog(entry);
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showDeleteDialog(LedgerEntry entry) {
        if (entry == null) return;

        new AlertDialog.Builder(this)
                .setTitle("지출 삭제")
                .setMessage("'" + entry.getItemName() + "' 내역을 삭제할까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (!isGuestMode() && entry.getServerId() != null) {
                        ledgerRepository.deleteEntry(entry.getServerId(), new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                LedgerLocalStore.deleteEntry(LedgerActivity.this, entry.getId());
                                refreshUi();
                                AppToast.show(LedgerActivity.this, "삭제했어.");
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                AppToast.show(LedgerActivity.this, "삭제 실패: " + t.getMessage());
                            }
                        });
                    } else {
                        LedgerLocalStore.deleteEntry(LedgerActivity.this, entry.getId());
                        refreshUi();
                        AppToast.show(LedgerActivity.this, "삭제했어.");
                    }
                })
                .show();
    }

    private void showEntryEditDialog(@Nullable LedgerEntry target) {
        final boolean isEdit = target != null;

        final Long[] selectedStoreId = {isEdit ? target.getStoreId() : StoreSessionManager.getCurrentStoreId(this)};
        final String[] selectedStoreName = {
                isEdit ? safeStoreName(target.getStoreName()) : safeStoreName(StoreSessionManager.getCurrentStoreName(this))
        };

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);

        TextView labelDate = makeLabel("날짜 (yyyy-MM-dd)");
        EditText etDate = makeEditText("예: 2026-04-04");
        etDate.setText(isEdit ? target.getDate() : todayDate());

        TextView labelName = makeLabel("품목명");
        EditText etName = makeEditText("예: 우유");
        etName.setText(isEdit ? target.getItemName() : "");

        TextView labelQty = makeLabel("수량");
        EditText etQty = makeEditText("예: 1");
        etQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etQty.setText(isEdit ? formatQty(target.getQuantity()) : "1");

        TextView labelUnit = makeLabel("단위");
        EditText etUnit = makeEditText("예: 개 / 팩 / kg");
        etUnit.setText(isEdit ? target.getUnit() : "개");

        TextView labelPrice = makeLabel("단가");
        EditText etUnitPrice = makeEditText("예: 3000");
        etUnitPrice.setInputType(InputType.TYPE_CLASS_NUMBER);
        etUnitPrice.setText(isEdit ? String.valueOf(target.getUnitPrice()) : "");

        TextView labelStore = makeLabel("판매점");
        TextView tvStorePicker = new TextView(this);
        tvStorePicker.setText("판매점: " + selectedStoreName[0]);
        tvStorePicker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvStorePicker.setTextColor(0xFF2E3A4B);
        tvStorePicker.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        tvStorePicker.setPadding(dp(14), dp(12), dp(14), dp(12));
        tvStorePicker.setOnClickListener(v -> {
            if (isGuestMode()) {
                showGuestStoreInputDialog(selectedStoreName[0], storeName -> {
                    selectedStoreId[0] = null;
                    selectedStoreName[0] = safeStoreName(storeName);
                    tvStorePicker.setText("판매점: " + selectedStoreName[0]);
                });
            } else {
                StorePickerDialog dialog = new StorePickerDialog(currentUserId, (storeId, storeName) -> {
                    selectedStoreId[0] = storeId;
                    selectedStoreName[0] = safeStoreName(storeName);
                    tvStorePicker.setText("판매점: " + selectedStoreName[0]);
                });
                dialog.show(getSupportFragmentManager(), "LedgerStorePickerDialog");
            }
        });

        TextView labelMemo = makeLabel("메모");
        EditText etMemo = makeEditText("선택 입력");
        etMemo.setMinLines(2);
        etMemo.setText(isEdit ? target.getMemo() : "");

        root.addView(labelDate);
        root.addView(etDate);
        root.addView(labelName);
        root.addView(etName);
        root.addView(labelQty);
        root.addView(etQty);
        root.addView(labelUnit);
        root.addView(etUnit);
        root.addView(labelPrice);
        root.addView(etUnitPrice);
        root.addView(labelStore);
        root.addView(tvStorePicker);
        root.addView(labelMemo);
        root.addView(etMemo);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "지출 수정" : "지출 직접 입력")
                .setView(scrollView)
                .setNegativeButton("취소", null)
                .setPositiveButton(isEdit ? "저장" : "추가", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String date = safe(etDate.getText());
            String name = safe(etName.getText());
            String qtyText = safe(etQty.getText());
            String unit = safe(etUnit.getText());
            String unitPriceText = safe(etUnitPrice.getText());
            String memo = safe(etMemo.getText());

            if (date.isEmpty() || !date.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                AppToast.show(this, "날짜 형식을 확인해줘. 예: 2026-04-04");
                return;
            }

            if (name.isEmpty()) {
                AppToast.show(this, "품목명을 입력해줘.");
                return;
            }

            double qty;
            try {
                qty = Double.parseDouble(qtyText.replace(",", "."));
            } catch (Exception e) {
                AppToast.show(this, "수량 형식이 올바르지 않아.");
                return;
            }
            if (qty <= 0d) qty = 1.0d;

            int unitPrice;
            try {
                unitPrice = Integer.parseInt(unitPriceText.replace(",", ""));
            } catch (Exception e) {
                AppToast.show(this, "단가 형식이 올바르지 않아.");
                return;
            }
            if (unitPrice < 0) unitPrice = 0;

            if (unit.isEmpty()) unit = "개";

            LedgerEntry entry;
            if (isEdit) {
                entry = target;
                entry.setDate(date);
                entry.setItemName(name);
                entry.setQuantity(qty);
                entry.setUnit(unit);
                entry.setUnitPrice(unitPrice);
                entry.setStoreId(selectedStoreId[0]);
                entry.setStoreName(selectedStoreName[0]);
                entry.setMemo(memo);
                if (TextUtils.isEmpty(entry.getSourceType())) {
                    entry.setSourceType(LedgerEntry.SOURCE_MANUAL);
                }
            } else {
                entry = LedgerEntry.createManual(
                        date,
                        selectedStoreId[0],
                        selectedStoreName[0],
                        name,
                        qty,
                        unit,
                        unitPrice,
                        memo
                );
            }

            if (isGuestMode()) {
                if (isEdit) {
                    LedgerLocalStore.updateEntry(LedgerActivity.this, entry);
                    AppToast.show(LedgerActivity.this, "로컬에 수정했어.");
                } else {
                    LedgerLocalStore.addEntry(LedgerActivity.this, entry);
                    AppToast.show(LedgerActivity.this, "로컬에 추가했어.");
                }
                refreshUi();
                dialog.dismiss();
                return;
            }

            LedgerEntryDto dto = LedgerEntryDto.fromLocal(currentUserId, entry);

            if (isEdit && entry.getServerId() != null) {
                ledgerRepository.updateEntry(entry.getServerId(), dto, new Callback<LedgerEntryDto>() {
                    @Override
                    public void onResponse(Call<LedgerEntryDto> call, Response<LedgerEntryDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LedgerEntry saved = response.body().toLocal();
                            saved.setId(entry.getId());
                            LedgerLocalStore.updateEntry(LedgerActivity.this, saved);
                            refreshUi();
                            dialog.dismiss();
                            AppToast.show(LedgerActivity.this, "수정했어.");
                        } else {
                            AppToast.show(LedgerActivity.this, "수정 실패");
                        }
                    }

                    @Override
                    public void onFailure(Call<LedgerEntryDto> call, Throwable t) {
                        AppToast.show(LedgerActivity.this, "수정 실패: " + t.getMessage());
                    }
                });
            } else {
                ledgerRepository.addEntry(dto, new Callback<LedgerEntryDto>() {
                    @Override
                    public void onResponse(Call<LedgerEntryDto> call, Response<LedgerEntryDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LedgerEntry saved = response.body().toLocal();
                            if (isEdit) saved.setId(entry.getId());
                            LedgerLocalStore.updateEntry(LedgerActivity.this, saved);
                            refreshUi();
                            dialog.dismiss();
                            AppToast.show(LedgerActivity.this, isEdit ? "수정했어." : "추가했어.");
                        } else {
                            AppToast.show(LedgerActivity.this, isEdit ? "수정 실패" : "추가 실패");
                        }
                    }

                    @Override
                    public void onFailure(Call<LedgerEntryDto> call, Throwable t) {
                        AppToast.show(LedgerActivity.this, (isEdit ? "수정 실패: " : "추가 실패: ") + t.getMessage());
                    }
                });
            }
        }));

        dialog.show();
    }

    private void showGuestStoreInputDialog(String currentValue, OnGuestStorePickedListener listener) {
        final EditText et = new EditText(this);
        et.setHint("판매점 이름 입력");
        et.setText("미지정".equals(currentValue) ? "" : currentValue);
        et.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("판매점 입력")
                .setView(et)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String value = safe(et.getText());
                    listener.onPicked(value.isEmpty() ? "미지정" : value);
                })
                .show();
    }

    private interface OnGuestStorePickedListener {
        void onPicked(String storeName);
    }

    private void forceScrollToTopImmediate() {
        nsvLedgerRoot.post(() -> {
            nsvLedgerRoot.stopNestedScroll();
            nsvLedgerRoot.scrollTo(0, 0);
            nsvLedgerRoot.fullScroll(View.FOCUS_UP);
        });
    }

    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(0xFF555555);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(10);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText makeEditText(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setBackgroundResource(android.R.drawable.edit_text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(6);
        et.setLayoutParams(lp);
        return et;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private String todayDate() {
        Calendar cal = Calendar.getInstance(Locale.KOREA);
        return String.format(
                Locale.KOREA,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
    }

    private String safe(CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }

    private String safeStoreName(String s) {
        return (s == null || s.trim().isEmpty()) ? "미지정" : s.trim();
    }

    private String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) {
            return String.valueOf((long) Math.round(q));
        }
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(q);
    }
}