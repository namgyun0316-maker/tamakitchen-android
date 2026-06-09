// File: app/src/main/java/com/example/myapplication/ui/shopping/ReceiptReviewActivity.java
package com.namgyun.tamakitchen.ui.shopping;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptReviewActivity extends AppCompatActivity {

    public static final String EXTRA_RAW_TEXT = "EXTRA_RAW_TEXT";

    private int devTapCount = 0;
    private long devTapLastMs = 0;

    private MaterialToolbar toolbar;
    private TextView tvSub;
    private RecyclerView rv;
    private MaterialButton btnSelectAll, btnAddManual;
    private TextView tvBottomHint;
    private MaterialButton btnRegister;
    private ReceiptLineAdapter adapter;

    private String raw = "";
    private boolean allSelected = true;
    private long idSeq = 9000;

    private ArrayList<String> knownNames = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_review);

        toolbar = findViewById(R.id.toolbar);
        tvSub = findViewById(R.id.tvSub);
        rv = findViewById(R.id.rv);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnAddManual = findViewById(R.id.btnAddManual);
        tvBottomHint = findViewById(R.id.tvBottomHint);
        btnRegister = findViewById(R.id.btnRegister);

        toolbar.setNavigationIcon(
                com.google.android.material.R.drawable.material_ic_keyboard_arrow_left_black_24dp
        );
        toolbar.setNavigationOnClickListener(v -> finish());

        toolbar.setOnClickListener(v -> {
            long now = System.currentTimeMillis();

            if (now - devTapLastMs > 2000) {
                devTapCount = 0;
            }

            devTapLastMs = now;

            if (++devTapCount >= 5) {
                devTapCount = 0;
                showRawDialog(raw);

                if (adapter != null) {
                    adapter.setDevMode(true);
                }
            }
        });

        Intent intent = getIntent();
        raw = intent != null ? intent.getStringExtra(EXTRA_RAW_TEXT) : "";

        if (raw == null) {
            raw = "";
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptLineAdapter(this::updateBottomUi, this::onDeleteItem);
        rv.setAdapter(adapter);

        tvSub.setText("내 냉장고 재료를 불러오는 중...");
        loadKnownNamesThenParse();

        btnSelectAll.setOnClickListener(v -> {
            allSelected = !allSelected;
            adapter.setAllChecked(allSelected);
            btnSelectAll.setText(allSelected ? "전체 해제" : "전체 선택");
        });

        btnAddManual.setOnClickListener(v -> showAddManualDialog());

        btnRegister.setOnClickListener(v -> {
            List<ReceiptLine> selected = adapter.getSelectedItems();

            if (selected.isEmpty()) {
                AppToast.show(this, "선택된 항목이 없습니다.");
                return;
            }

            AppToast.show(this, selected.size() + "개 냉장고에 등록됐어요!");
        });
    }

    private void loadKnownNamesThenParse() {
        ArrayList<String> sample = getKnownNamesSample();

        long userId = AuthPrefs.getUserId(this);

        if (userId <= 0) {
            knownNames = sample;
            reparseAndShow();
            return;
        }

        ReceiptKnownItemsRepository repo = new ReceiptKnownItemsRepository();
        repo.fetchKnownNamesFromServer(userId, new ReceiptKnownItemsRepository.CallbackResult() {
            @Override
            public void onSuccess(ArrayList<String> names) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    if (names == null || names.isEmpty()) {
                        knownNames = sample;
                        tvSub.setText("냉장고 재료가 비어있어 샘플로 인식했어요.");
                    } else {
                        knownNames = names;
                    }

                    reparseAndShow();
                });
            }

            @Override
            public void onFail(Throwable t) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    knownNames = sample;
                    tvSub.setText("네트워크 오류로 샘플로 인식했어요.");
                    reparseAndShow();
                });
            }
        });
    }

    private void reparseAndShow() {
        String cleanedRaw = preprocessRaw(raw);

        ArrayList<String> known = (knownNames == null || knownNames.isEmpty())
                ? getKnownNamesSample()
                : knownNames;

        ArrayList<ReceiptLine> result =
                ReceiptDictionaryParser.parseWithDictionary(cleanedRaw, known);

        if (result.isEmpty()) {
            tvSub.setText("등록된 품목을 찾지 못했어요. 직접 추가해 주세요.");
        } else {
            tvSub.setText("체크/수정 후 냉장고에 등록하세요 (" + result.size() + "개)");
        }

        for (ReceiptLine it : result) {
            if (it != null) {
                it.setChecked(true);
            }
        }

        allSelected = true;
        btnSelectAll.setText("전체 해제");
        adapter.submitList(result);
        updateBottomUi();
    }

    private void showAddManualDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_manual, null);

        com.google.android.material.textfield.TextInputEditText etName = view.findViewById(R.id.etName);
        com.google.android.material.textfield.TextInputEditText etQty = view.findViewById(R.id.etQty);
        com.google.android.material.textfield.TextInputEditText etPrice = view.findViewById(R.id.etPrice);

        com.google.android.material.textfield.TextInputLayout tilName = view.findViewById(R.id.tilName);
        com.google.android.material.textfield.TextInputLayout tilQty = view.findViewById(R.id.tilQty);
        com.google.android.material.textfield.TextInputLayout tilPrice = view.findViewById(R.id.tilPrice);

        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnAdd = view.findViewById(R.id.btnAdd);

        if (etQty != null) {
            etQty.setText("1");
        }

        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setView(view)
                        .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String name = etName == null ? "" : etName.getText().toString().trim();
            String qtyS = etQty == null ? "" : etQty.getText().toString().trim();
            String prcS = etPrice == null ? "" : etPrice.getText().toString().trim();

            tilName.setError(null);
            tilQty.setError(null);
            tilPrice.setError(null);

            boolean ok = true;

            if (name.isEmpty()) {
                tilName.setError("품목명을 입력하세요");
                ok = false;
            }

            double qty = 1.0;

            if (!qtyS.isEmpty()) {
                try {
                    qty = Double.parseDouble(qtyS.replace(",", ""));
                } catch (Exception e) {
                    tilQty.setError("수량 형식이 올바르지 않아요");
                    ok = false;
                }
            }

            if (qty <= 0) {
                tilQty.setError("수량은 1 이상이어야 해요");
                ok = false;
            }

            int price = 0;

            if (!prcS.isEmpty()) {
                try {
                    price = Integer.parseInt(prcS.replace(",", ""));
                } catch (Exception e) {
                    tilPrice.setError("가격 형식이 올바르지 않아요");
                    ok = false;
                }
            }

            if (price < 0) {
                tilPrice.setError("가격은 0 이상이어야 해요");
                ok = false;
            }

            if (!ok) return;

            ReceiptLine line = new ReceiptLine(
                    idSeq++,
                    name,
                    qty,
                    price,
                    true,
                    ReceiptLine.Confidence.HIGH,
                    "직접 추가"
            );

            List<ReceiptLine> cur = new ArrayList<>(adapter.getCurrentItems());
            cur.add(line);

            tvSub.setText("체크/수정 후 냉장고에 등록하세요 (" + cur.size() + "개)");
            adapter.submitList(cur);
            updateBottomUi();

            dialog.dismiss();
        });

        dialog.show();
    }

    private void onDeleteItem(ReceiptLine item) {
        List<ReceiptLine> cur = new ArrayList<>(adapter.getCurrentItems());
        cur.remove(item);

        int cnt = cur.size();

        tvSub.setText(
                cnt == 0
                        ? "품목이 없어요. 직접 추가해 주세요."
                        : "체크/수정 후 냉장고에 등록하세요 (" + cnt + "개)"
        );

        adapter.submitList(cur);
        updateBottomUi();
    }

    private void updateBottomUi() {
        List<ReceiptLine> selected = adapter.getSelectedItems();

        int cnt = selected.size();
        long sum = 0;

        for (ReceiptLine it : selected) {
            if (it != null) {
                sum += Math.max(0, it.getPrice());
            }
        }

        tvBottomHint.setText(
                "선택 합계: "
                        + NumberFormat.getInstance(Locale.getDefault()).format(sum)
                        + "원"
        );

        btnRegister.setText("선택 " + cnt + "개 냉장고 등록");
    }

    private void showRawDialog(String rawText) {
        String show = TextUtils.isEmpty(rawText) ? "(RAW 텍스트 없음)" : rawText.trim();

        new AlertDialog.Builder(this)
                .setTitle("[개발자] RAW 텍스트")
                .setMessage(show)
                .setPositiveButton("닫기", null)
                .show();
    }

    private ArrayList<String> getKnownNamesSample() {
        ArrayList<String> list = new ArrayList<>();

        list.add("진로병");
        list.add("아이스컵미디엄");
        list.add("다크로스트");
        list.add("대파");
        list.add("청양고추");
        list.add("양념돈고추장불고기");
        list.add("돼지왕양념구이");
        list.add("깻잎");

        return list;
    }

    private String preprocessRaw(String input) {
        if (input == null) return "";

        String s = input.replace("#", "#\n").replace("|", " ");
        String[] lines = s.split("\\r?\\n");
        StringBuilder out = new StringBuilder();

        boolean foundItemSection = false;

        for (String line : lines) {
            if (line == null) continue;

            String lc = line.replace(" ", "").toLowerCase(Locale.ROOT);

            if (lc.contains("상품명") && (lc.contains("단가") || lc.contains("수량"))) {
                foundItemSection = true;
                break;
            }
        }

        boolean inItemSection = !foundItemSection;
        boolean reachedFooter = false;

        for (String line : lines) {
            if (line == null) continue;

            String t = line.trim();

            if (t.isEmpty()) continue;

            String lower = t.replace(" ", "").toLowerCase(Locale.ROOT);

            if (!inItemSection) {
                if (lower.contains("상품명") && (lower.contains("단가") || lower.contains("수량"))) {
                    inItemSection = true;
                }
                continue;
            }

            if (!reachedFooter) {
                int fh = 0;

                if (lower.contains("합계") || lower.contains("총계")) fh++;
                if (lower.contains("카드") && lower.contains("승인")) fh++;
                if (lower.startsWith("합계")) fh += 2;

                if (fh >= 2) {
                    reachedFooter = true;
                    continue;
                }
            }

            if (reachedFooter) continue;

            if (lower.contains("상품명단가수량금")) continue;
            if (lower.contains("과세물품")) continue;
            if (lower.contains("면세물품")) continue;
            if (lower.contains("부가세")) continue;
            if (lower.contains("vat")) continue;
            if (lower.contains("합계수량")) continue;
            if (lower.contains("합계금액")) continue;
            if (lower.startsWith("합계")) continue;
            if (lower.matches("합?계:?[\\d,\\s].*")) continue;
            if (lower.startsWith("계") && lower.length() < 15) continue;
            if (lower.contains("카드승인")) continue;
            if (lower.contains("신용카드")) continue;
            if (lower.contains("승인번호")) continue;
            if (lower.contains("사업자")) continue;
            if (lower.contains("전화")) continue;
            if (lower.contains("주소")) continue;
            if (lower.contains("현금영수증")) continue;
            if (lower.contains("소득공제")) continue;
            if (lower.contains("연금영수증")) continue;
            if (lower.contains("거래자번")) continue;
            if (lower.contains("거래번호")) continue;
            if (lower.contains("상품권")) continue;
            if (lower.contains("결제내역")) continue;
            if (lower.contains("품권명")) continue;
            if (lower.contains("코액액")) continue;
            if (lower.contains("결자")) continue;
            if (lower.matches(".*no\\.?\\s*\\d{4,}.*")) continue;
            if (lower.contains("선도민감")) continue;
            if (lower.contains("일부상품")) continue;
            if (lower.contains("근로복지")) continue;
            if (lower.contains("복지")) continue;
            if (t.matches("^[\\d,\\.\\s]+$")) continue;
            if (t.contains("*")) continue;
            if (lower.matches("그?병\\s*\\d+")) continue;
            if (t.startsWith("[")) continue;
            if (lower.contains("카드결세")) continue;
            if (lower.contains("시요카드")) continue;
            if (lower.contains("카드지불")) continue;
            if (lower.contains("일시불")) continue;
            if (lower.contains("정부방침")) continue;
            if (lower.contains("반드시영수증")) continue;
            if (lower.contains("냉동식품")) continue;
            if (lower.contains("공산품")) continue;
            if (lower.contains("정과정육")) continue;
            if (lower.contains("수산식품")) continue;
            if (lower.matches("^[-=*]+.*[-=*]+$")) continue;
            if (t.length() <= 1) continue;

            out.append(t).append("\n");
        }

        return out.toString().trim();
    }
}