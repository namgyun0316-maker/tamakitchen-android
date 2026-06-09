package com.namgyun.tamakitchen.ui.shopping;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ✅ 사전(등록된 재료/상품명) 기반 파서 (DB 연동용)
 * - coreNorm(괄호/행사/단위/숫자/불용어/공백/특수문자 제거) 기반으로 안정 매칭
 * - OCR이 띄어쓰기/기호를 끼워넣는 경우를 위한 Loose Regex 매칭 추가
 * - Fuzzy(오인식 보정)는 "짧은 키"에는 적용 금지 + core 기준으로만 수행 (오탐 감소)
 */
public class ReceiptDictionaryParser {

    private static final String TAG = "ReceiptDictionaryParser";

    private static final Pattern NUM_TOKEN =
            Pattern.compile("(\\d{1,3}(?:,\\d{3})+|\\d{1,})");

    private static final int MIN_PRICE = 100;
    private static final int MAX_PRICE = 300000;
    private static final int WINDOW = 4;

    private static final double FUZZY_THRESHOLD = 0.72; // 유사도 임계값

    // ✅ fuzzy를 허용할 최소 글자수(정규화 기준). 너무 짧으면 오탐 폭발
    private static final int MIN_CORE_LEN_FOR_FUZZY = 3;

    // ✅ 너무 짧은 키는 substring 오탐이 생김 → 최소 길이 올려서 안정화
    private static final int MIN_CORE_LEN_FOR_MATCH = 2; // 필요하면 3으로 올려도 됨

    // ✅ 불용어(필요하면 계속 추가 가능)
    private static final String[] STOP_WORDS = new String[] {
            // 원산지
            "국산", "수입", "미국산", "호주산", "중국산", "칠레산", "캐나다산",

            // 행사/프로모션
            "행사", "특가", "세일", "증정", "할인", "기획", "사은품", "덤",
            "행사상품", "기획상품", "한정", "추천", "인기", "베스트",

            // 상태/카테고리
            "냉동", "냉장", "상온", "신선", "프리미엄",
            "대용량", "소용량", "대", "중", "소",

            // 포장/수량 관련(재료명 매칭에는 방해)
            "봉", "팩", "팩트", "박스", "상자", "입", "개", "ea", "pack",

            // 영수증 잡단어
            "상품", "상품명", "코드", "바코드", "과세", "면세",

            // 유통/설명
            "유통기한", "제조", "제조일", "소비기한",

            // (옵션) 흔한 유통사/브랜드 접두 (필요시 유지/삭제)
            "이마트", "홈플러스", "롯데", "농협", "gs", "cu"
    };

    public static ArrayList<ReceiptLine> parseWithDictionary(String raw, ArrayList<String> knownNames) {
        ArrayList<ReceiptLine> out = new ArrayList<>();
        if (raw == null || knownNames == null || knownNames.isEmpty()) return out;

        ArrayList<String> lines = splitLines(raw);
        if (lines.isEmpty()) return out;

        String joined = joinLines(lines);

        // joinedNorm/joinedCore는 "전체 텍스트" 포함 여부를 빠르게 판단하는데 사용
        String joinedNorm = norm(joined);
        String joinedCore = coreNorm(joined);

        HashSet<String> seen = new HashSet<>();
        long idSeq = 1;

        // 긴 이름 먼저 매칭 (부분 중복 방지)
        ArrayList<String> dict = new ArrayList<>(knownNames);
        dict.sort((a, b) -> Integer.compare(
                b == null ? 0 : b.length(),
                a == null ? 0 : a.length()
        ));

        for (String dictName : dict) {
            if (TextUtils.isEmpty(dictName)) continue;

            String keyNorm = norm(dictName);
            String keyCore = coreNorm(dictName);

            if (keyCore.length() < MIN_CORE_LEN_FOR_MATCH) continue;
            if (seen.contains(keyCore)) continue;

            // ✅ Loose regex (띄어쓰기/특수문자 끼워넣기 대응) - core 기반
            Pattern loose = buildLooseCorePattern(keyCore);

            // 1) 정확/포함 매칭 (norm/core + loose regex)
            boolean exactMatch =
                    containsLoosely(joinedNorm, joinedCore, keyNorm, keyCore) ||
                            (loose != null && loose.matcher(joined).find());

            // 2) fuzzy 매칭 (짧은 키는 금지, core 기반)
            String fuzzyLine = null;
            if (!exactMatch) {
                if (keyCore.length() >= MIN_CORE_LEN_FOR_FUZZY) {
                    fuzzyLine = findFuzzyMatchLine(lines, keyCore, FUZZY_THRESHOLD);
                }
                if (fuzzyLine == null) continue;
            }

            Hit hit;
            if (exactMatch) {
                hit = findBestHit(lines, keyNorm, keyCore, loose);
            } else {
                hit = findHitByLine(lines, fuzzyLine);
            }

            if (hit == null || hit.score <= 0) continue;

            // ✅ 표시 이름은 DB(사전) 이름 그대로
            String displayName = dictName;

            QtyPrice qp = extractBestQtyPrice(lines, hit.lineIndex);
            if (qp == null) continue;

            seen.add(keyCore);

            ReceiptLine line = new ReceiptLine(
                    idSeq++,
                    displayName,
                    qp.qty <= 0 ? 1.0 : qp.qty,
                    qp.price,
                    true,
                    ReceiptLine.Confidence.HIGH,
                    buildSource(lines, hit.lineIndex)
            );
            out.add(line);

            Log.d(TAG, "HIT: " + displayName
                    + " qty=" + line.getQty()
                    + " price=" + line.getPrice()
                    + " exact=" + exactMatch
                    + " src=" + lines.get(hit.lineIndex));
        }

        Log.d(TAG, "total=" + out.size());
        return out;
    }

    // ─────────────────────────────────────────────────────────
    // ✅ 정확 매칭
    // ─────────────────────────────────────────────────────────

    private static class Hit { int lineIndex; int score; }

    private static boolean containsLoosely(String rawNorm, String rawCore,
                                           String keyNorm, String keyCore) {
        return (rawNorm != null && rawNorm.contains(keyNorm))
                || (rawCore != null && rawCore.contains(keyCore));
    }

    private static Hit findBestHit(ArrayList<String> lines,
                                   String keyNorm,
                                   String keyCore,
                                   Pattern looseCorePattern) {
        Hit best = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            String lnNorm = norm(line);
            String lnCore = coreNorm(line);

            int score = 0;

            // 1) 가장 강한 케이스: norm 포함
            if (lnNorm.contains(keyNorm)) score += 110;

                // 2) core 포함 (단위/숫자/행사표기 제거되어 재료명에 강함)
            else if (lnCore.contains(keyCore)) score += 90;

                // 3) loose regex (띄어쓰기/기호 끼움 대응)
            else if (looseCorePattern != null && looseCorePattern.matcher(line).find()) score += 85;

            else continue;

            // 가산/감산
            if (hasHangul(line)) score += 5;
            if (countNumberTokens(line) >= 5) score -= 5;

            if (best == null || score > best.score) {
                best = new Hit();
                best.lineIndex = i;
                best.score = score;
            }
        }
        return best;
    }

    private static Hit findHitByLine(ArrayList<String> lines, String targetLine) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals(targetLine)) {
                Hit h = new Hit();
                h.lineIndex = i;
                h.score = 70;
                return h;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────
    // ✅ Loose Regex: keyCore 글자 사이에 공백/기호가 껴도 매칭
    // 예) keyCore="청양고추"
    //     line="청 양-고.추" 도 매칭
    // ─────────────────────────────────────────────────────────

    private static Pattern buildLooseCorePattern(String keyCore) {
        if (TextUtils.isEmpty(keyCore) || keyCore.length() < MIN_CORE_LEN_FOR_MATCH) return null;

        StringBuilder sb = new StringBuilder();
        String gap = "[\\s\\p{Punct}·ㆍ•]*";

        for (int i = 0; i < keyCore.length(); i++) {
            String ch = String.valueOf(keyCore.charAt(i));
            sb.append(Pattern.quote(ch));
            if (i != keyCore.length() - 1) sb.append(gap);
        }

        try {
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // ✅ 유사도 매칭 (Jaro-Winkler)
    // - "coreNorm(line)" vs "keyCore"로만 비교해서 오탐 줄임
    // ─────────────────────────────────────────────────────────

    private static String findFuzzyMatchLine(ArrayList<String> lines,
                                             String keyCore, double threshold) {
        String bestLine = null;
        double bestScore = 0;

        for (String line : lines) {
            String lnCore = coreNorm(line);

            if (TextUtils.isEmpty(lnCore)) continue;
            if (lnCore.length() < keyCore.length() - 3) continue;

            double score = slidingWindowSimilarity(lnCore, keyCore);
            if (score >= threshold && score > bestScore) {
                bestScore = score;
                bestLine = line;
            }
        }

        if (bestLine != null) {
            Log.d(TAG, "FUZZY: key=" + keyCore
                    + " score=" + String.format(Locale.getDefault(), "%.2f", bestScore)
                    + " line=" + bestLine);
        }
        return bestLine;
    }

    private static double slidingWindowSimilarity(String text, String key) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(key)) return 0;
        int kLen = key.length();
        int winMin = Math.max(2, kLen - 2);
        int winMax = Math.min(text.length(), kLen + 3);
        double best = 0;

        for (int size = winMin; size <= winMax; size++) {
            for (int start = 0; start + size <= text.length(); start++) {
                String sub = text.substring(start, start + size);
                double sim = jaroWinkler(sub, key);
                if (sim > best) best = sim;
            }
        }
        return best;
    }

    private static double jaroWinkler(String s1, String s2) {
        double jaro = jaro(s1, s2);
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
        int prefix = 0;
        for (int i = 0; i < maxPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + prefix * 0.1 * (1 - jaro);
    }

    private static double jaro(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length(), len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int matchDist = Math.max(len1, len2) / 2 - 1;
        if (matchDist < 0) matchDist = 0;

        boolean[] m1 = new boolean[len1];
        boolean[] m2 = new boolean[len2];
        int matches = 0;

        for (int i = 0; i < len1; i++) {
            int lo = Math.max(0, i - matchDist);
            int hi = Math.min(i + matchDist + 1, len2);
            for (int j = lo; j < hi; j++) {
                if (!m2[j] && s1.charAt(i) == s2.charAt(j)) {
                    m1[i] = m2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0.0;

        int trans = 0, k = 0;
        for (int i = 0; i < len1; i++) {
            if (!m1[i]) continue;
            while (!m2[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) trans++;
            k++;
        }
        double m = matches;
        return (m / len1 + m / len2 + (m - trans / 2.0) / m) / 3.0;
    }

    // ─────────────────────────────────────────────────────────
    // ✅ 수량/가격 추출
    // ─────────────────────────────────────────────────────────

    private static class QtyPrice { double qty; int price; }

    private static QtyPrice extractBestQtyPrice(ArrayList<String> lines, int hitLine) {
        if (lines == null || lines.isEmpty()) return null;

        QtyPrice direct = extractQtyPriceFromLine(lines.get(hitLine));
        if (direct != null) return direct;

        for (int dist = 1; dist <= WINDOW; dist++) {
            int up = hitLine - dist;
            int down = hitLine + dist;
            if (up >= 0) {
                QtyPrice qp = extractQtyPriceFromLine(lines.get(up));
                if (qp != null) return qp;
            }
            if (down < lines.size()) {
                QtyPrice qp = extractQtyPriceFromLine(lines.get(down));
                if (qp != null) return qp;
            }
        }
        return null;
    }

    private static QtyPrice extractQtyPriceFromLine(String line) {
        if (TextUtils.isEmpty(line)) return null;
        String t = line.replace("원", " ").replace("₩", " ");
        t = fixOcrBrokenThousands(t).trim();

        ArrayList<Token> nums = findNumberTokensWithPos(t);
        if (nums.isEmpty()) return null;

        Token amountTok = selectBestAmountToken(nums);
        if (amountTok == null) return null;

        int amount = toInt(amountTok.text);
        if (amount < MIN_PRICE || amount > MAX_PRICE) return null;

        double qty = 1.0;
        int amountIndex = nums.indexOf(amountTok);
        for (int i = amountIndex - 1; i >= 0; i--) {
            double q = toDouble(nums.get(i).text);
            if (q >= 1 && q <= 50) { qty = q; break; }
        }

        QtyPrice qp = new QtyPrice();
        qp.qty = qty;
        qp.price = amount;
        return qp;
    }

    private static Token selectBestAmountToken(ArrayList<Token> nums) {
        ArrayList<Token> cands = new ArrayList<>();
        ArrayList<Integer> vals = new ArrayList<>();
        for (Token tok : nums) {
            int v = toInt(tok.text);
            if (v >= MIN_PRICE && v <= MAX_PRICE) {
                cands.add(tok);
                vals.add(v);
            }
        }
        if (cands.isEmpty()) return null;

        int bestRepeat = -1;
        for (int i = 0; i < vals.size(); i++) {
            int v = vals.get(i), cnt = 0;
            for (int j = 0; j < vals.size(); j++) if (vals.get(j) == v) cnt++;
            if (cnt >= 2) bestRepeat = Math.max(bestRepeat, v);
        }
        if (bestRepeat >= 0) {
            for (int i = cands.size() - 1; i >= 0; i--) {
                if (toInt(cands.get(i).text) == bestRepeat) return cands.get(i);
            }
        }

        Token last = cands.get(cands.size() - 1);
        int max = vals.get(0);
        for (int v : vals) if (v > max) max = v;

        if (vals.size() >= 3 && toInt(last.text) < (int) (max * 0.6)) {
            for (int i = cands.size() - 1; i >= 0; i--) {
                if (toInt(cands.get(i).text) == max) return cands.get(i);
            }
        }
        return last;
    }

    // ─────────────────────────────────────────────────────────
    // ✅ 숫자 토큰
    // ─────────────────────────────────────────────────────────

    private static class Token {
        String text; int start; int end;
        Token(String t, int s, int e) { text = t; start = s; end = e; }
    }

    private static ArrayList<Token> findNumberTokensWithPos(String line) {
        ArrayList<Token> out = new ArrayList<>();
        if (line == null) return out;
        Matcher m = NUM_TOKEN.matcher(fixOcrBrokenThousands(line));
        while (m.find()) {
            String g = m.group(1);
            if (!TextUtils.isEmpty(g)) out.add(new Token(g, m.start(1), m.end(1)));
        }
        return out;
    }

    private static int countNumberTokens(String line) {
        if (line == null) return 0;
        Matcher m = NUM_TOKEN.matcher(fixOcrBrokenThousands(line));
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    private static boolean hasHangul(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) if (c >= '가' && c <= '힣') return true;
        return false;
    }

    // ─────────────────────────────────────────────────────────
    // ✅ 유틸
    // ─────────────────────────────────────────────────────────

    private static ArrayList<String> splitLines(String raw) {
        String[] arr = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        ArrayList<String> out = new ArrayList<>();
        for (String s : arr) {
            if (s != null && !s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }

    private static String joinLines(ArrayList<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) if (s != null) sb.append(s).append('\n');
        return sb.toString();
    }

    private static String buildSource(ArrayList<String> lines, int hitLine) {
        StringBuilder sb = new StringBuilder();
        for (int d = -1; d <= 1; d++) {
            int idx = hitLine + d;
            if (idx < 0 || idx >= lines.size()) continue;
            sb.append(lines.get(idx));
            if (d != 1) sb.append(" | ");
        }
        return sb.toString();
    }

    private static String fixOcrBrokenThousands(String s) {
        if (s == null) return "";
        String t = s;
        t = t.replaceAll("(\\d)\\s*[\\.,]\\s*(\\d{3})", "$1,$2");
        t = t.replaceAll("(\\d),(\\s+)(\\d{3})", "$1,$3");
        return t;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}]+", "");
    }

    /**
     * ✅ coreNorm: 재료명 매칭용 강력 정규화
     * - 괄호 내용 제거: 대파(국산) -> 대파
     * - 행사 표기 제거: 1+1, 2+1
     * - 숫자 제거
     * - 단위 제거
     * - 불용어 제거
     * - 공백/특수문자 제거
     */
    private static String coreNorm(String s) {
        if (s == null) return "";

        String t = s.toLowerCase(Locale.ROOT);

        // 1) 괄호 안 내용 제거
        t = t.replaceAll("\\(.*?\\)", " ");

        // 2) 1+1, 2+1 같은 행사표기 제거
        t = t.replaceAll("\\d+\\+\\d+", " ");

        // 3) 숫자 제거
        t = t.replaceAll("\\d+", " ");

        // 4) 단위 제거 (영수증에서 흔한 표기들)
        t = t.replaceAll("\\b(ml|kg|g|l|t|ea|pack|봉|개|입)\\b", " ");

        // 5) 불용어 제거
        t = removeStopWords(t);

        // 6) 공백 + 특수문자 제거
        t = t.replaceAll("[\\s\\p{Punct}·ㆍ•]+", "");

        return t.trim();
    }

    private static String removeStopWords(String input) {
        String t = input == null ? "" : input;
        for (String w : STOP_WORDS) {
            if (TextUtils.isEmpty(w)) continue;
            t = t.replace(w, " ");
        }
        return t;
    }

    private static int toInt(String s) {
        try { return Integer.parseInt(s.replace(",", "").trim()); }
        catch (Exception e) { return 0; }
    }

    private static double toDouble(String s) {
        try { return Double.parseDouble(s.replace(",", "").trim()); }
        catch (Exception e) { return 0; }
    }
}