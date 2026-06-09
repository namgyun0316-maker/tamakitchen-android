package com.namgyun.tamakitchen.ui.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class CalculatorActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;

    private final StringBuilder input = new StringBuilder();

    private static final String PREF_NAME = "calc_pref";
    private static final String KEY_HISTORY = "calc_history_json";
    private static final int HISTORY_MAX = 50;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        int[] ids = new int[]{
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot,
                R.id.btnPlus, R.id.btnMinus, R.id.btnMul, R.id.btnDiv,
                R.id.btnClear, R.id.btnEqual,
                R.id.btnBackTop,
                R.id.btnHistory,
                R.id.btnParen,
                R.id.btnPercent,
                R.id.btnPlusMinus
        };

        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) v.setOnClickListener(this);
        }

        render();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnHistory) {
            openHistorySheet();
            return;
        }

        if (id == R.id.btnBackTop) {
            if (input.length() > 0) input.deleteCharAt(input.length() - 1);
            render();
            return;
        }

        if (id == R.id.btnClear) {
            input.setLength(0);
            tvResult.setText("");
            render();
            return;
        }

        if (id == R.id.btnEqual) {
            if (TextUtils.isEmpty(input.toString())) return;

            String exprForSave = trimTrailingOps(input.toString());

            try {
                BigDecimal res = evalSimple(exprForSave);
                String out = formatBigDecimal(res);
                tvResult.setText(out);

                saveHistory(exprForSave, out);

                input.setLength(0);
                input.append(out);
                render();
            } catch (Exception e) {
                AppToast.show(this, "계산할 수 없어요.");
            }
            return;
        }

        if (id == R.id.btn0) { append("0"); render(); return; }
        if (id == R.id.btn1) { append("1"); render(); return; }
        if (id == R.id.btn2) { append("2"); render(); return; }
        if (id == R.id.btn3) { append("3"); render(); return; }
        if (id == R.id.btn4) { append("4"); render(); return; }
        if (id == R.id.btn5) { append("5"); render(); return; }
        if (id == R.id.btn6) { append("6"); render(); return; }
        if (id == R.id.btn7) { append("7"); render(); return; }
        if (id == R.id.btn8) { append("8"); render(); return; }
        if (id == R.id.btn9) { append("9"); render(); return; }

        if (id == R.id.btnDot) { onDot(); render(); return; }

        if (id == R.id.btnPlus) { onOp('+'); render(); return; }
        if (id == R.id.btnMinus) { onOp('-'); render(); return; }
        if (id == R.id.btnMul) { onOp('*'); render(); return; }
        if (id == R.id.btnDiv) { onOp('/'); render(); return; }

        if (id == R.id.btnPercent) {
            onPercent();
            render();
            return;
        }

        if (id == R.id.btnPlusMinus) {
            toggleSign();
            render();
            return;
        }

        if (id == R.id.btnParen) {
            AppToast.show(this, "괄호는 현재 버전에서 지원하지 않아요.");
        }
    }

    private void append(String s) {
        if (isLastTokenNumber()) {
            String token = getCurrentNumberToken();
            if ("0".equals(token) && !token.contains(".") && !".".equals(s)) {
                if (input.length() == 1 || isOp(input.charAt(input.length() - 2))) {
                    input.deleteCharAt(input.length() - 1);
                }
            }
        }
        input.append(s);
    }

    private void onDot() {
        if (input.length() == 0 || isOp(lastChar())) {
            input.append("0.");
            return;
        }

        String token = getCurrentNumberToken();
        if (token.contains(".")) return;

        input.append(".");
    }

    private void onOp(char op) {
        if (input.length() == 0) {
            if (op == '-') input.append("-");
            return;
        }

        char last = lastChar();
        if (isOp(last)) {
            input.setCharAt(input.length() - 1, op);
            return;
        }

        input.append(op);
    }

    private void onPercent() {
        if (input.length() == 0) return;
        if (isOp(lastChar())) return;

        String token = getCurrentNumberToken();
        if (TextUtils.isEmpty(token) || "-".equals(token)) return;

        try {
            BigDecimal v = new BigDecimal(token);
            BigDecimal out = v.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
            replaceCurrentNumberToken(formatBigDecimal(out));
        } catch (Exception ignored) {
        }
    }

    private void toggleSign() {
        if (input.length() == 0) {
            input.append("-");
            return;
        }

        if (isOp(lastChar())) {
            input.append("-");
            return;
        }

        String token = getCurrentNumberToken();
        if (TextUtils.isEmpty(token)) return;

        if (token.startsWith("-")) {
            replaceCurrentNumberToken(token.substring(1));
        } else {
            replaceCurrentNumberToken("-" + token);
        }
    }

    private void replaceCurrentNumberToken(String newToken) {
        int end = input.length();
        int i = end - 1;

        while (i >= 0) {
            char c = input.charAt(i);
            if (isOp(c)) break;
            i--;
        }

        input.replace(i + 1, end, newToken);
    }

    private void render() {
        tvExpression.setText(input.length() == 0 ? "0" : input.toString());
    }

    private char lastChar() {
        return input.charAt(input.length() - 1);
    }

    private boolean isOp(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private boolean isLastTokenNumber() {
        if (input.length() == 0) return false;
        char last = lastChar();
        return Character.isDigit(last) || last == '.';
    }

    private String getCurrentNumberToken() {
        if (input.length() == 0) return "";

        int i = input.length() - 1;

        while (i >= 0) {
            char c = input.charAt(i);
            if (isOp(c)) break;
            i--;
        }

        return input.substring(i + 1);
    }

    private BigDecimal evalSimple(String expr) {
        expr = trimTrailingOps(expr);

        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (isOp(c)) {
                if (c == '-' && (i == 0 || isOp(expr.charAt(i - 1)))) {
                    num.append('-');
                    continue;
                }

                if (num.length() == 0) continue;

                tokens.add(num.toString());
                tokens.add(String.valueOf(c));
                num.setLength(0);
            } else {
                num.append(c);
            }
        }

        if (num.length() > 0) tokens.add(num.toString());
        if (tokens.isEmpty()) throw new IllegalArgumentException("empty");

        ArrayList<String> pass = new ArrayList<>();
        pass.add(tokens.get(0));

        for (int i = 1; i < tokens.size(); i += 2) {
            String op = tokens.get(i);
            String right = tokens.get(i + 1);

            if ("*".equals(op) || "/".equals(op)) {
                BigDecimal leftVal = new BigDecimal(pass.remove(pass.size() - 1));
                BigDecimal rightVal = new BigDecimal(right);

                BigDecimal out;
                if ("*".equals(op)) {
                    out = leftVal.multiply(rightVal);
                } else {
                    if (rightVal.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("divide by zero");
                    }
                    out = leftVal.divide(rightVal, 10, RoundingMode.HALF_UP);
                }

                pass.add(out.toPlainString());
            } else {
                pass.add(op);
                pass.add(right);
            }
        }

        BigDecimal result = new BigDecimal(pass.get(0));

        for (int i = 1; i < pass.size(); i += 2) {
            String op = pass.get(i);
            BigDecimal rightVal = new BigDecimal(pass.get(i + 1));

            if ("+".equals(op)) result = result.add(rightVal);
            else result = result.subtract(rightVal);
        }

        return result;
    }

    private String trimTrailingOps(String s) {
        if (TextUtils.isEmpty(s)) return "";

        int end = s.length();
        while (end > 0 && isOp(s.charAt(end - 1))) end--;

        return s.substring(0, end);
    }

    private String formatBigDecimal(BigDecimal v) {
        String s = v.stripTrailingZeros().toPlainString();

        if ("-0".equals(s) || "-0.0".equals(s)) return "0";

        return s;
    }

    private void openHistorySheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_calc_history, null);
        dialog.setContentView(sheet);

        RecyclerView rv = sheet.findViewById(R.id.rvHistory);
        View btnClear = sheet.findViewById(R.id.btnClearHistory);
        View btnClose = sheet.findViewById(R.id.btnCloseHistory);

        ArrayList<HistoryItem> items = loadHistory();

        rv.setLayoutManager(new LinearLayoutManager(this));
        HistoryAdapter adapter = new HistoryAdapter(items, dialog);
        rv.setAdapter(adapter);

        btnClear.setOnClickListener(v -> {
            clearHistory();
            items.clear();
            adapter.notifyDataSetChanged();
            AppToast.show(this, "계산기록을 삭제했어요.");
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveHistory(String expr, String result) {
        try {
            ArrayList<HistoryItem> list = loadHistory();

            HistoryItem item = new HistoryItem(expr, result);
            list.add(0, item);

            if (list.size() > HISTORY_MAX) {
                list = new ArrayList<>(list.subList(0, HISTORY_MAX));
            }

            JSONArray arr = new JSONArray();
            for (HistoryItem hi : list) {
                JSONObject o = new JSONObject();
                o.put("expr", hi.expr);
                o.put("res", hi.res);
                arr.put(o);
            }

            SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private ArrayList<HistoryItem> loadHistory() {
        ArrayList<HistoryItem> list = new ArrayList<>();

        try {
            SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_HISTORY, "[]");

            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new HistoryItem(
                        o.optString("expr", ""),
                        o.optString("res", "")
                ));
            }
        } catch (Exception ignored) {
        }

        return list;
    }

    private void clearHistory() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_HISTORY).apply();
    }

    static class HistoryItem {
        final String expr;
        final String res;

        HistoryItem(String expr, String res) {
            this.expr = expr;
            this.res = res;
        }
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final ArrayList<HistoryItem> items;
        private final BottomSheetDialog dialog;

        HistoryAdapter(ArrayList<HistoryItem> items, BottomSheetDialog dialog) {
            this.items = items;
            this.dialog = dialog;
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvExpr, tvRes;

            VH(View itemView) {
                super(itemView);

                tvExpr = itemView.findViewById(R.id.tvHistExpr);
                tvRes = itemView.findViewById(R.id.tvHistRes);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    HistoryItem it = items.get(pos);

                    input.setLength(0);
                    input.append(it.res);
                    tvResult.setText(it.res);
                    render();

                    AppToast.show(CalculatorActivity.this, "결과를 불러왔어요.");

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                });
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calc_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            HistoryItem it = items.get(position);
            holder.tvExpr.setText(it.expr);
            holder.tvRes.setText("=" + it.res);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}