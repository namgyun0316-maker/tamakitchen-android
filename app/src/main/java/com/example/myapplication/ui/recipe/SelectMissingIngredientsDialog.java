package com.namgyun.tamakitchen.ui.recipe;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.List;

public class SelectMissingIngredientsDialog extends Dialog {

    public interface OnConfirm {
        void onConfirm(List<String> selected);
    }

    private final List<String> names;
    private final OnConfirm onConfirm;
    private boolean allSelected = false;

    public SelectMissingIngredientsDialog(@NonNull Context context,
                                          @NonNull List<String> names,
                                          @NonNull OnConfirm onConfirm) {
        super(context);
        this.names = names;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_select_missing_ingredients);

        RecyclerView rv = findViewById(R.id.rvIngredients);
        Button btnSelectAll = findViewById(R.id.btnSelectAll);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnConfirm = findViewById(R.id.btnConfirm);

        List<MissingIngredientItem> items = new ArrayList<>();
        for (String n : names) {
            if (n == null) continue;
            String t = n.trim();
            if (!t.isEmpty()) items.add(new MissingIngredientItem(t, false));
        }

        MissingIngredientAdapter adapter = new MissingIngredientAdapter(items);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        btnSelectAll.setOnClickListener(v -> {
            allSelected = !allSelected;
            adapter.setAllChecked(allSelected);
            btnSelectAll.setText(allSelected ? "전체해제" : "전체선택");
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnConfirm.setOnClickListener(v -> {
            List<String> selected = adapter.getSelectedNames();
            onConfirm.onConfirm(selected);
            dismiss();
        });
    }

    /**
     * ✅ 다이얼로그 크기: 빨간 박스 수준으로 줄이기
     * - width: 화면의 0.78배
     * - height: 화면의 0.55배 (리스트가 길면 내부 RecyclerView가 스크롤됨)
     */
    @Override
    protected void onStart() {
        super.onStart();

        Window w = getWindow();
        if (w == null) return;

        // 검은 테두리 같은 거 방지 + 모서리 둥근 배경 유지
        w.setBackgroundDrawableResource(android.R.color.transparent);

        int screenW = getContext().getResources().getDisplayMetrics().widthPixels;
        int screenH = getContext().getResources().getDisplayMetrics().heightPixels;

        int targetW = (int) (screenW * 0.78f);  // ✅ 폭: 빨간선 느낌
        int targetH = (int) (screenH * 0.55f);  // ✅ 높이: 빨간선 느낌

        w.setLayout(targetW, targetH);

        // 가운데 고정
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.dimAmount = 0.4f; // 배경 어둡게(원하면 조절)
        w.setAttributes(lp);
    }
}
