package com.namgyun.tamakitchen.ui.recipe.adapter;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetailIngredientLineAdapter extends RecyclerView.Adapter<DetailIngredientLineAdapter.VH> {

    private final List<String> items = new ArrayList<>();

    public DetailIngredientLineAdapter() {}

    public void submit(List<String> lines) {
        items.clear();
        if (lines != null) items.addAll(lines);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detail_ingredient_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String line = items.get(position);

        if (line == null || line.trim().isEmpty()) {
            holder.tvLine.setText("-");
            return;
        }

        String converted = MeasurementConverter.convert(line);

        if (TextUtils.isEmpty(converted)) {
            holder.tvLine.setText(line);
            return;
        }

        String fullText = line + "\n💡 " + converted;
        SpannableString span = new SpannableString(fullText);

        int start = line.length() + 1;
        int end = fullText.length();

        span.setSpan(
                new ForegroundColorSpan(Color.parseColor("#777777")),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        span.setSpan(
                new RelativeSizeSpan(0.9f),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        holder.tvLine.setText(span);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLine;

        VH(@NonNull View itemView) {
            super(itemView);
            tvLine = itemView.findViewById(R.id.tvIngredientLine);
        }
    }

    private static class MeasurementConverter {

        private static final Pattern AMOUNT_PATTERN = Pattern.compile(
                "(.+?)\\s*(\\d+(?:\\.\\d+)?)\\s*(g|gram|그램|ml|mL|ML|l|L|리터|스푼|큰술|작은술|티스푼|컵)",
                Pattern.CASE_INSENSITIVE
        );

        private static final Pattern FRACTION_AMOUNT_PATTERN = Pattern.compile(
                ".*\\d+\\s*/\\s*\\d+.*"
        );

        private static final double PAPER_CUP_ML = 180.0;
        private static final double TABLE_SPOON_ML = 15.0;
        private static final double TEA_SPOON_ML = 5.0;

        private static final Map<String, IngredientRule> RULES = new HashMap<>();
        private static final Map<String, PieceRule> PIECE_RULES = new HashMap<>();

        static {
            addRule("물", 15.0, 180.0);
            addRule("육수", 15.0, 180.0);
            addRule("우유", 15.0, 185.0);
            addRule("간장", 15.0, 200.0);
            addRule("진간장", 15.0, 200.0);
            addRule("국간장", 15.0, 200.0);
            addRule("식초", 15.0, 180.0);
            addRule("맛술", 15.0, 180.0);
            addRule("참치액", 15.0, 200.0);
            addRule("액젓", 15.0, 200.0);
            addRule("굴소스", 18.0, 215.0);

            addRule("고추장", 20.0, 240.0);
            addRule("된장", 20.0, 230.0);
            addRule("쌈장", 20.0, 230.0);
            addRule("설탕", 12.0, 160.0);
            addRule("소금", 5.0, 220.0);
            addRule("고춧가루", 7.0, 100.0);
            addRule("다진마늘", 15.0, 180.0);
            addRule("밀가루", 8.0, 100.0);
            addRule("부침가루", 8.0, 100.0);
            addRule("전분", 8.0, 100.0);

            addPieceRule("양배추", "통", 800.0);
            addPieceRule("무", "개", 1000.0);
            addPieceRule("양파", "개", 200.0);
            addPieceRule("대파", "대", 100.0);
            addPieceRule("두부", "모", 300.0);
            addPieceRule("당근", "개", 150.0);
            addPieceRule("감자", "개", 150.0);
            addPieceRule("고구마", "개", 200.0);
        }

        private static void addRule(String name, double gPerTbsp, double gPerCup) {
            RULES.put(name, new IngredientRule(gPerTbsp, gPerCup));
        }

        private static void addPieceRule(String name, String unitName, double gPerPiece) {
            PIECE_RULES.put(name, new PieceRule(unitName, gPerPiece));
        }

        static String convert(String rawLine) {
            if (rawLine == null) return null;

            String line = rawLine.trim();
            if (line.isEmpty() || "-".equals(line)) return null;

            // ✅ 1/2컵, 1/4개, 1/2대처럼 이미 분수로 적힌 경우는 변환하지 않음
            if (FRACTION_AMOUNT_PATTERN.matcher(line).matches()) {
                return null;
            }

            Matcher matcher = AMOUNT_PATTERN.matcher(line);
            if (!matcher.find()) return null;

            String ingredientPart = matcher.group(1) == null ? "" : matcher.group(1).trim();
            String amountText = matcher.group(2);
            String unit = matcher.group(3) == null ? "" : matcher.group(3).trim();

            double amount;
            try {
                amount = Double.parseDouble(amountText);
            } catch (Exception e) {
                return null;
            }

            if (amount <= 0) return null;

            String ingredientName = normalizeIngredientName(ingredientPart);
            String unitLower = unit.toLowerCase(Locale.ROOT);

            if (unitLower.equals("ml")) {
                return convertMl(amount);
            }

            if (unitLower.equals("l") || unit.equals("리터")) {
                return convertMl(amount * 1000.0);
            }

            if (unit.equals("스푼") || unit.equals("큰술")) {
                return convertMl(amount * TABLE_SPOON_ML);
            }

            if (unit.equals("작은술") || unit.equals("티스푼")) {
                return convertMl(amount * TEA_SPOON_ML);
            }

            if (unit.equals("컵")) {
                return convertMl(amount * PAPER_CUP_ML);
            }

            if (unitLower.equals("g") || unit.equals("그램") || unitLower.equals("gram")) {
                PieceRule pieceRule = findPieceRule(ingredientName);
                if (pieceRule != null) {
                    return convertPiece(ingredientName, amount, pieceRule);
                }

                IngredientRule rule = findRule(ingredientName);
                if (rule == null) {
                    return null;
                }

                return convertGram(amount, rule);
            }

            return null;
        }

        private static String convertMl(double ml) {
            double paperCup = ml / PAPER_CUP_ML;
            double tableSpoon = ml / TABLE_SPOON_ML;

            if (ml >= 100) {
                return "종이컵 약 " + formatCup(paperCup) + "컵";
            }

            return "밥숟가락 약 " + formatNumber(tableSpoon) + "스푼";
        }

        private static String convertGram(double gram, IngredientRule rule) {
            double cup = gram / rule.gPerCup;
            double spoon = gram / rule.gPerTbsp;

            if (cup >= 0.5) {
                return "종이컵 약 " + formatCup(cup) + "컵 / 밥숟가락 약 " + formatNumber(spoon) + "스푼";
            }

            return "밥숟가락 약 " + formatNumber(spoon) + "스푼";
        }

        private static String convertPiece(String ingredientName, double gram, PieceRule rule) {
            double ratio = gram / rule.gPerPiece;
            String fraction = formatPieceFraction(ratio);

            if (TextUtils.isEmpty(fraction)) {
                return null;
            }

            return ingredientName + " 약 " + fraction + rule.unitName;
        }

        private static String normalizeIngredientName(String raw) {
            if (raw == null) return "";

            String s = raw.trim();

            s = s.replace("필수", "")
                    .replace("선택", "")
                    .replace("재료", "")
                    .replace(":", "")
                    .replace("-", "")
                    .replace("·", "")
                    .replace("(선택)", "")
                    .trim();

            String[] parts = s.split("\\s+");
            if (parts.length > 0) {
                return parts[parts.length - 1].trim();
            }

            return s;
        }

        private static IngredientRule findRule(String ingredientName) {
            if (ingredientName == null) return null;

            String target = ingredientName.trim();
            if (target.isEmpty()) return null;

            if (RULES.containsKey(target)) {
                return RULES.get(target);
            }

            for (String key : RULES.keySet()) {
                if (target.contains(key) || key.contains(target)) {
                    return RULES.get(key);
                }
            }

            return null;
        }

        private static PieceRule findPieceRule(String ingredientName) {
            if (ingredientName == null) return null;

            String target = ingredientName.trim();
            if (target.isEmpty()) return null;

            if (PIECE_RULES.containsKey(target)) {
                return PIECE_RULES.get(target);
            }

            for (String key : PIECE_RULES.keySet()) {
                if (target.contains(key) || key.contains(target)) {
                    return PIECE_RULES.get(key);
                }
            }

            return null;
        }

        private static String formatCup(double value) {
            if (value >= 0.20 && value < 0.35) return "1/4";
            if (value >= 0.35 && value < 0.65) return "1/2";
            if (value >= 0.65 && value < 0.85) return "3/4";

            return formatNumber(value);
        }

        private static String formatPieceFraction(double ratio) {
            if (ratio >= 0.08 && ratio < 0.17) return "1/8";
            if (ratio >= 0.17 && ratio < 0.23) return "1/5";
            if (ratio >= 0.23 && ratio < 0.30) return "1/4";
            if (ratio >= 0.30 && ratio < 0.42) return "1/3";
            if (ratio >= 0.42 && ratio < 0.60) return "1/2";
            if (ratio >= 0.60 && ratio < 0.80) return "2/3";
            if (ratio >= 0.80 && ratio < 1.20) return "1";

            if (ratio > 1.20 && ratio < 3.0) {
                return formatNumber(ratio);
            }

            return null;
        }

        private static String formatNumber(double value) {
            if (Math.abs(value - Math.round(value)) < 0.05) {
                return String.valueOf((int) Math.round(value));
            }

            return String.format(Locale.KOREA, "%.1f", value);
        }

        private static class IngredientRule {
            final double gPerTbsp;
            final double gPerCup;

            IngredientRule(double gPerTbsp, double gPerCup) {
                this.gPerTbsp = gPerTbsp;
                this.gPerCup = gPerCup;
            }
        }

        private static class PieceRule {
            final String unitName;
            final double gPerPiece;

            PieceRule(String unitName, double gPerPiece) {
                this.unitName = unitName;
                this.gPerPiece = gPerPiece;
            }
        }
    }
}