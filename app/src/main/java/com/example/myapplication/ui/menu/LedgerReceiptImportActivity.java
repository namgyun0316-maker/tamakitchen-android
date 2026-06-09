package com.namgyun.tamakitchen.ui.menu;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.namgyun.tamakitchen.ui.shopping.ReceiptLine;
import com.namgyun.tamakitchen.ui.shopping.ReceiptParser;
import com.namgyun.tamakitchen.ui.shopping.StorePickerDialog;
import com.namgyun.tamakitchen.ui.shopping.StoreSessionManager;
import com.google.android.material.button.MaterialButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LedgerReceiptImportActivity extends AppCompatActivity {

    public static final String EXTRA_RAW_TEXT = "EXTRA_LEDGER_RAW_TEXT";

    private static final String TAG = "LedgerReceiptImport";

    private TextView tvLedgerReceiptSub;
    private TextView tvLedgerReceiptDate;
    private TextView tvLedgerReceiptStore;
    private RecyclerView rvLedgerReceipt;
    private MaterialButton btnLedgerReceiptSelectAll;
    private MaterialButton btnLedgerReceiptSave;

    private LedgerReceiptImportAdapter adapter;

    private String selectedDate;
    private Long selectedStoreId;
    private String selectedStoreName;

    private long currentUserId = -1L;
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
    private LedgerRepository ledgerRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ledger_receipt_import);

        tvLedgerReceiptSub = findViewById(R.id.tvLedgerReceiptSub);
        tvLedgerReceiptDate = findViewById(R.id.tvLedgerReceiptDate);
        tvLedgerReceiptStore = findViewById(R.id.tvLedgerReceiptStore);
        rvLedgerReceipt = findViewById(R.id.rvLedgerReceipt);
        btnLedgerReceiptSelectAll = findViewById(R.id.btnLedgerReceiptSelectAll);
        btnLedgerReceiptSave = findViewById(R.id.btnLedgerReceiptSave);

        currentUserId = new FridgeModeManager().getUserIdSafe(this);
        ledgerRepository = new LedgerRepository();

        selectedDate = todayDate();
        selectedStoreId = StoreSessionManager.getCurrentStoreId(this);
        selectedStoreName = safeStoreName(StoreSessionManager.getCurrentStoreName(this));

        tvLedgerReceiptDate.setText("날짜: " + selectedDate);
        tvLedgerReceiptStore.setText("판매점: " + selectedStoreName);

        tvLedgerReceiptDate.setOnClickListener(v -> showDateInputDialog());

        tvLedgerReceiptStore.setOnClickListener(v -> {
            if (isGuestMode()) {
                showGuestStoreInputDialog(selectedStoreName, storeName -> {
                    selectedStoreId = null;
                    selectedStoreName = safeStoreName(storeName);
                    tvLedgerReceiptStore.setText("판매점: " + selectedStoreName);
                });
            } else {
                StorePickerDialog dialog = new StorePickerDialog(currentUserId, (storeId, storeName) -> {
                    selectedStoreId = storeId;
                    selectedStoreName = safeStoreName(storeName);
                    tvLedgerReceiptStore.setText("판매점: " + selectedStoreName);
                });

                dialog.show(getSupportFragmentManager(), "LedgerReceiptStorePicker");
            }
        });

        String raw = getIntent() != null ? getIntent().getStringExtra(EXTRA_RAW_TEXT) : "";
        if (raw == null) raw = "";

        List<ReceiptLine> parsed = ReceiptParser.parse(raw);

        adapter = new LedgerReceiptImportAdapter();
        adapter.submit(parsed);

        rvLedgerReceipt.setLayoutManager(new LinearLayoutManager(this));
        rvLedgerReceipt.setAdapter(adapter);

        updateSummary();

        btnLedgerReceiptSelectAll.setOnClickListener(v -> {
            adapter.toggleSelectAll();
            updateSummary();
        });

        btnLedgerReceiptSave.setOnClickListener(v -> saveSelectedToLedger());
    }

    private boolean isGuestMode() {
        return currentUserId <= 0L;
    }

    private void saveSelectedToLedger() {
        List<ReceiptLine> selected = adapter.getSelectedItems();

        if (selected.isEmpty()) {
            AppToast.show(this, "선택된 항목이 없어요.");
            return;
        }

        List<LedgerEntry> localEntries = new ArrayList<>();

        for (ReceiptLine line : selected) {
            if (line == null) continue;
            if (TextUtils.isEmpty(line.getName())) continue;

            double qty = resolveReceiptLineQuantity(line);
            if (qty <= 0d) qty = 1.0d;

            int totalPrice = Math.max(0, line.getPrice());
            int unitPrice = qty > 0 ? (int) Math.round((double) totalPrice / qty) : totalPrice;

            LedgerEntry entry = new LedgerEntry();
            entry.setDate(selectedDate);
            entry.setStoreId(selectedStoreId);
            entry.setStoreName(selectedStoreName);
            entry.setItemName(line.getName().trim());
            entry.setQuantity(qty);
            entry.setUnit("개");
            entry.setUnitPrice(unitPrice);
            entry.setTotalPrice(totalPrice);
            entry.setMemo("영수증 등록");
            entry.setSourceType(LedgerEntry.SOURCE_RECEIPT);
            entry.setReceiptAttached(true);

            localEntries.add(entry);
        }

        if (localEntries.isEmpty()) {
            AppToast.show(this, "저장할 항목이 없어요.");
            return;
        }

        if (isGuestMode()) {
            for (LedgerEntry entry : localEntries) {
                LedgerLocalStore.addEntry(this, entry);
            }

            AppToast.show(this, "게스트 모드라 로컬에만 " + localEntries.size() + "건 저장했어요.");
            finish();
            return;
        }

        btnLedgerReceiptSave.setEnabled(false);
        btnLedgerReceiptSave.setAlpha(0.6f);

        saveReceiptEntriesToServer(localEntries);
    }

    private void saveReceiptEntriesToServer(List<LedgerEntry> localEntries) {
        saveReceiptEntrySequentially(localEntries, 0, 0);
    }

    private void saveReceiptEntrySequentially(List<LedgerEntry> localEntries, int index, int successCount) {
        if (index >= localEntries.size()) {
            restoreSaveButton();
            AppToast.show(this, "가계부에 " + successCount + "건 등록했어요.");
            finish();
            return;
        }

        LedgerEntry localEntry = localEntries.get(index);
        LedgerEntryDto dto = LedgerEntryDto.fromLocal(currentUserId, localEntry);

        ledgerRepository.addEntry(dto, new Callback<LedgerEntryDto>() {
            @Override
            public void onResponse(Call<LedgerEntryDto> call, Response<LedgerEntryDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LedgerEntry saved = response.body().toLocal();
                    saved.setId(localEntry.getId());
                    LedgerLocalStore.addEntry(LedgerReceiptImportActivity.this, saved);
                    saveReceiptEntrySequentially(localEntries, index + 1, successCount + 1);
                } else {
                    restoreSaveButton();
                    Log.e(TAG, "server save failed. code=" + response.code());
                    AppToast.show(
                            LedgerReceiptImportActivity.this,
                            "가계부 저장에 실패했어요. 잠시 후 다시 시도해주세요."
                    );
                }
            }

            @Override
            public void onFailure(Call<LedgerEntryDto> call, Throwable t) {
                restoreSaveButton();
                Log.e(TAG, "server save failure", t);
                AppToast.show(
                        LedgerReceiptImportActivity.this,
                        NetworkErrorUtil.getUserMessage(t)
                );
            }
        });
    }

    private void restoreSaveButton() {
        if (btnLedgerReceiptSave == null) return;

        btnLedgerReceiptSave.setEnabled(true);
        btnLedgerReceiptSave.setAlpha(1f);
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

    private void showDateInputDialog() {
        final EditText et = new EditText(this);
        et.setText(selectedDate);
        et.setHint("예: 2026-03-31");
        et.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("날짜 입력")
                .setView(et)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String value = et.getText() == null ? "" : et.getText().toString().trim();

                    if (!value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                        AppToast.show(this, "날짜 형식을 확인해주세요. 예: 2026-03-31");
                        return;
                    }

                    selectedDate = value;
                    tvLedgerReceiptDate.setText("날짜: " + selectedDate);
                })
                .show();
    }

    private void showGuestStoreInputDialog(String currentValue, OnGuestStorePickedListener listener) {
        final EditText et = new EditText(this);
        et.setHint("판매점 이름 입력");
        et.setText("미지정".equals(currentValue) ? "" : currentValue);
        et.setPadding(dp(16), dp(16), dp(16), dp(16));
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        new AlertDialog.Builder(this)
                .setTitle("판매점 입력")
                .setView(et)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String value = et.getText() == null ? "" : et.getText().toString().trim();
                    listener.onPicked(value.isEmpty() ? "미지정" : value);
                })
                .show();
    }

    private interface OnGuestStorePickedListener {
        void onPicked(String storeName);
    }

    private void updateSummary() {
        List<ReceiptLine> selected = adapter.getSelectedItems();

        long sum = 0L;

        for (ReceiptLine item : selected) {
            if (item == null) continue;
            sum += Math.max(0, item.getPrice());
        }

        tvLedgerReceiptSub.setText(
                "선택 " + selected.size() + "건 · 합계 " + moneyFormat.format(sum) + "원"
        );

        btnLedgerReceiptSelectAll.setText(adapter.isAllSelected() ? "전체해제" : "전체선택");
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

    private String safeStoreName(String s) {
        if (s == null || s.trim().isEmpty()) return "미지정";
        return s.trim();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}