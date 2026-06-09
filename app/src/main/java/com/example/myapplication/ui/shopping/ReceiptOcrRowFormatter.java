    package com.namgyun.tamakitchen.ui.shopping;

    import android.graphics.Rect;
    import android.text.TextUtils;

    import com.google.mlkit.vision.text.Text;

    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.Comparator;
    import java.util.Locale;

    /**
     * ✅ MLKit OCR 결과를 "행(Row)" 단위로 재구성
     * - result.getText()는 레이아웃을 망가뜨려서 (품목/수량/금액) 줄이 깨짐
     * - TextBlock/Line/Element boundingBox로 y기준 정렬 후, 비슷한 y를 같은 행으로 묶음
     * - 같은 행에서는 x 기준 정렬 후, 토큰을 합쳐 "품목 수량 금액"처럼 반환
     */
    public class ReceiptOcrRowFormatter {

        private static class Word {
            final String text;
            final Rect box;

            Word(String text, Rect box) {
                this.text = text;
                this.box = box;
            }

            int centerY() { return box.top + (box.height() / 2); }
            int centerX() { return box.left + (box.width() / 2); }
        }

        private static class Row {
            final ArrayList<Word> words = new ArrayList<>();
            int centerY;

            void add(Word w) {
                words.add(w);
                recomputeCenterY();
            }

            void recomputeCenterY() {
                if (words.isEmpty()) { centerY = 0; return; }
                int sum = 0;
                for (Word w : words) sum += w.centerY();
                centerY = sum / words.size();
            }
        }

        /**
         * @return 행 단위 텍스트 (각 행은 \n으로 구분)
         */
        public static String formatToRows(Text result) {
            if (result == null) return "";

            ArrayList<Word> all = new ArrayList<>();

            for (Text.TextBlock block : result.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    // line.getText()만 쓰면 내부 요소 순서가 깨질 때가 있어서 element 기준으로 수집
                    for (Text.Element el : line.getElements()) {
                        String t = safe(el.getText());
                        Rect r = el.getBoundingBox();
                        if (TextUtils.isEmpty(t) || r == null) continue;
                        all.add(new Word(t, r));
                    }
                }
            }

            if (all.isEmpty()) {
                // fallback
                return safe(result.getText());
            }

            // 1) y 기준 정렬
            Collections.sort(all, Comparator.comparingInt(Word::centerY));

            // 2) y가 비슷한 것들을 같은 row로 묶기 (tolerance는 글자 높이 기반)
            ArrayList<Row> rows = new ArrayList<>();

            int medianH = estimateMedianHeight(all);
            int tol = Math.max(10, (int) (medianH * 0.6)); // ✅ 영수증에서 보통 이 정도면 행 묶임

            for (Word w : all) {
                Row best = null;
                int bestDiff = Integer.MAX_VALUE;

                for (Row row : rows) {
                    int diff = Math.abs(row.centerY - w.centerY());
                    if (diff <= tol && diff < bestDiff) {
                        best = row;
                        bestDiff = diff;
                    }
                }

                if (best == null) {
                    Row r = new Row();
                    r.add(w);
                    rows.add(r);
                } else {
                    best.add(w);
                }
            }

            // 3) 각 row 내부는 x 기준 정렬 후 한 줄 문자열로
            //    숫자 "9, 400" / "5.500" 같은 깨짐도 일부 보정
            Collections.sort(rows, Comparator.comparingInt(r -> r.centerY));

            StringBuilder out = new StringBuilder();
            for (Row row : rows) {
                if (row.words.isEmpty()) continue;

                Collections.sort(row.words, Comparator.comparingInt(Word::centerX));

                String line = joinRow(row.words);
                line = normalizeMoneyLike(line);

                line = line.trim();
                if (line.isEmpty()) continue;

                out.append(line).append('\n');
            }

            return out.toString().trim();
        }

        private static int estimateMedianHeight(ArrayList<Word> all) {
            ArrayList<Integer> hs = new ArrayList<>();
            for (Word w : all) hs.add(Math.max(1, w.box.height()));
            Collections.sort(hs);
            return hs.get(hs.size() / 2);
        }

        private static String joinRow(ArrayList<Word> words) {
            // 단순 공백 결합 (품목명은 붙어서 나올 수 있으니, 너무 과한 스페이싱은 안함)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.size(); i++) {
                String t = safe(words.get(i).text);

                // 토큰 사이 공백: 기본 1칸
                if (sb.length() > 0) sb.append(' ');
                sb.append(t);
            }
            return sb.toString();
        }

        private static String normalizeMoneyLike(String s) {
            if (s == null) return "";

            String t = s;

            // "9, 400" -> "9,400"
            t = t.replaceAll("(\\d)\\s*,\\s*(\\d)", "$1,$2");

            // "9 ,400" -> "9,400"
            t = t.replaceAll("(\\d)\\s*,\\s*(\\d)", "$1,$2");

            // "5.500" 같이 점으로 천단위가 찍히는 경우 -> "5,500"
            // (단, 1.5 같은 소수는 영수증 가격에서 거의 없으니 천단위로 간주)
            t = t.replaceAll("(\\d)\\.(\\d{3})\\b", "$1,$2");

            // "9,400원" 같은 것 대비
            t = t.replace("원", " 원");

            // 공백 정리
            t = t.replaceAll("\\s{2,}", " ").trim();

            return t;
        }

        private static String safe(String s) {
            return s == null ? "" : s.trim();
        }
    }