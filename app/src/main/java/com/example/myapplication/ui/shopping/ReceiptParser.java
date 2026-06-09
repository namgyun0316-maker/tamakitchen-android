package com.namgyun.tamakitchen.ui.shopping;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    private static final Pattern MONEY_TOKEN =
            Pattern.compile("(?<![A-Za-z가-힣])(\\d{1,3}(?:,\\d{3})+|\\d{1,})(?![A-Za-z가-힣번호])");

    private static final Pattern HAS_LETTER  = Pattern.compile("[A-Za-z가-힣]");
    private static final Pattern HAS_HANGUL  = Pattern.compile("[가-힣]");
    private static final Pattern ONLY_PUNC   = Pattern.compile("^[\\p{Punct}\\s]+$");

    private static final String[] STOP_WORDS = {
            "상품명","단가","수량","금액",
            "합계","총계","총액","총합","소계","과세","면세","부가세","vat","세금",
            "면세물품","과세물품",
            "거스름","현금","카드","승인","승인번호","거래","거래번호","전표","영수증",
            "교환","환불","쿠폰","할인","행사","포인트","적립","멤버십","스탬프",
            "단말","단말기","매장","점포","매출","매입","정산",
            "안내","공지","유의","주의","고객","감사","문의",
            "사업자","사업자등록","대표","등록","전화","tel","fax",
            "홈택스","hometax","www","http","go.kr",
            "결제","결제내역","현금영수증","소득공제","카드번호","승인시각",
            "신용카드","카드승인","일시불",
            // ✅ 추가: 편의점/마트 불필요 라인
            "선도민감","일부상품","제외","복지","근로복지","근로","센터",
            "품권","상품권","일상품권","쿠폰권"
    };

    private static final Pattern META_COLON  = Pattern.compile("^.{0,40}:.*$");
    private static final Pattern DATE_LIKE   = Pattern.compile(".*(20\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}|\\d{1,2}:\\d{2}).*");
    private static final Pattern MANY_DIGITS = Pattern.compile(".*\\d{6,}.*");
    private static final Pattern ADDRESS_LIKE = Pattern.compile(
            ".*(?:[가-힣]+(?:시|도)\\s*[가-힣]+(?:구|군)?|[가-힣]+(?:로|길)\\s*\\d+|[가-힣]+동\\s*\\d+(?:-\\d+)?(?:번|호)?|\\d+(?:-\\d+)?번지).*"
    );

    // ✅ 추가: NO1234, NO: 등 영수증 번호/매장코드 패턴
    private static final Pattern RECEIPT_NO = Pattern.compile("(?i).*\\bno\\.?\\s*\\d+.*");

    // ✅ 규격 제거: 이름 끝의 "360ML", "10T", "500G", "1.5L", "2kg" 등
    private static final Pattern SPEC_SUFFIX =
            Pattern.compile("(?i)\\s*\\d+\\.?\\d*\\s*(?:ml|l|kg|g|t|개|ea|pack|봉|팩|입)\\s*$");

    private static final int MIN_PRICE = 100;
    private static final int MAX_PRICE = 300000;

    public static ArrayList<ReceiptLine> parse(String raw) {
        ArrayList<ReceiptLine> out = new ArrayList<>();
        if (raw == null) return out;

        ArrayList<String> linesAll = normalizeLines(raw);
        if (linesAll.isEmpty()) return out;

        ArrayList<String> lines = cropBeforeFooter(linesAll);

        HashSet<String> seen = new HashSet<>();
        long idSeq = 1;

        // Pass 1: 한 줄에 품목명+수량+금액이 모두 있는 경우
        for (String line : lines) {
            if (!looksLikeItemLine(line)) continue;

            ParsedLine p = parseInline(line);
            if (p == null) continue;
            if (!looksLikeRealItemName(p.name)) continue;

            String key = nameKey(p.name);
            if (isSeenDup(key, seen)) continue;
            seen.add(key);

            out.add(new ReceiptLine(
                    idSeq++, p.name,
                    p.qty <= 0 ? 1.0 : p.qty,
                    p.price, true,
                    ReceiptLine.Confidence.HIGH, line
            ));
        }

        // Pass 2: 품목명 줄 + 다음 줄 숫자 조합
        for (int i = 0; i < lines.size() - 1; i++) {
            String nameLine = cleanupName(lines.get(i));
            if (!looksLikeRealItemName(nameLine)) continue;

            String next = lines.get(i + 1);
            QtyPrice qp = parseQtyPriceLine(next);
            if (qp == null) continue;

            String key = nameKey(nameLine);
            if (isSeenDup(key, seen)) continue;
            seen.add(key);

            out.add(new ReceiptLine(
                    idSeq++, nameLine,
                    qp.qty, qp.price, true,
                    ReceiptLine.Confidence.MEDIUM,
                    nameLine + " + " + next
            ));
        }

        return out;
    }

    // ── 내부 구조체 ──────────────────────────────────────────

    private static class ParsedLine { String name; double qty; int price; }
    private static class QtyPrice   { double qty;  int price; }
    private static class Token {
        String text; int start; int end;
        Token(String t, int s, int e) { text=t; start=s; end=e; }
    }

    // ── 핵심 파싱 ────────────────────────────────────────────

    private static ParsedLine parseInline(String line) {
        if (TextUtils.isEmpty(line)) return null;
        String t = line.replace("원"," ").replace("₩"," ");
        t = fixOcrBrokenThousands(t).replaceAll("\\s{2,}"," ").trim();
        if (t.isEmpty()) return null;

        ArrayList<Token> nums = findNumberTokens(t);
        if (nums.isEmpty()) return null;

        // ✅ 개선된 금액 선택:
        // 마트 영수증 = "품목명 단가 수량 금액" → 맨 오른쪽이 금액
        // 반복값이 있으면(단가=금액) 반복값 우선 (기존 로직 유지)
        Token amountTok = selectBestAmountToken(nums);
        if (amountTok == null) return null;

        int amount = toInt(amountTok.text);
        if (amount < MIN_PRICE || amount > MAX_PRICE) return null;

        // ✅ 수량 역탐색: 금액 토큰 왼쪽에서 1~50 정수 탐색
        // "1봉" 같이 한글이 붙은 경우는 수량 1로 처리 (봉=단위이므로)
        double qty = 1.0;
        Token qtyTok = null;
        int amountIndex = nums.indexOf(amountTok);
        for (int i = amountIndex - 1; i >= 0; i--) {
            Token tok = nums.get(i);
            double q = toDouble(tok.text);
            if (q >= 1 && q <= 50) {
                // ✅ 수량 토큰 바로 뒤에 한글 단위(봉,팩,개 등)가 오면 수량=1로 고정
                int afterEnd = tok.end;
                String after = (afterEnd < t.length()) ? t.substring(afterEnd).trim() : "";
                boolean hasUnit = after.matches("^(?:봉|팩|개|묶음|봉지|포|캔|병|컵|잔).*");
                if (hasUnit) {
                    qty = 1.0; qtyTok = tok; break;
                }
                qtyTok = tok; qty = q; break;
            }
        }

        int nameEnd = (qtyTok != null) ? qtyTok.start : amountTok.start;
        String namePart = t.substring(0, Math.max(0, nameEnd)).trim();
        namePart = cleanupName(namePart);
        if (TextUtils.isEmpty(namePart)) return null;

        ParsedLine p = new ParsedLine();
        p.name = namePart; p.qty = qty; p.price = amount;
        return p;
    }

    /**
     * ✅ 금액 토큰 선택:
     * 1) 반복 등장하는 값 중 최대
     * 2) 기본: 맨 오른쪽
     * 3) 맨 오른쪽이 최대값의 60% 미만이면 최대값 선택
     */
    private static Token selectBestAmountToken(ArrayList<Token> nums) {
        if (nums == null || nums.isEmpty()) return null;

        ArrayList<Token>   cands = new ArrayList<>();
        ArrayList<Integer> vals  = new ArrayList<>();
        for (Token tok : nums) {
            int v = toInt(tok.text);
            if (v >= MIN_PRICE && v <= MAX_PRICE) { cands.add(tok); vals.add(v); }
        }
        if (cands.isEmpty()) return null;

        // 반복값
        int bestRepeat = -1;
        for (int i = 0; i < vals.size(); i++) {
            int v = vals.get(i), cnt = 0;
            for (int j = 0; j < vals.size(); j++) if (vals.get(j)==v) cnt++;
            if (cnt >= 2) bestRepeat = Math.max(bestRepeat, v);
        }
        if (bestRepeat >= 0) {
            for (int i = cands.size()-1; i >= 0; i--)
                if (toInt(cands.get(i).text)==bestRepeat) return cands.get(i);
        }

        Token last = cands.get(cands.size()-1);
        int max = vals.get(0);
        for (int v : vals) if (v > max) max = v;

        if (vals.size() >= 3 && toInt(last.text) < (int)(max * 0.6)) {
            for (int i = cands.size()-1; i >= 0; i--)
                if (toInt(cands.get(i).text)==max) return cands.get(i);
        }
        return last;
    }

    private static QtyPrice parseQtyPriceLine(String line) {
        if (TextUtils.isEmpty(line)) return null;
        String t = line.replace("원"," ").replace("₩"," ");
        t = fixOcrBrokenThousands(t).replaceAll("\\s{2,}"," ").trim();
        if (t.isEmpty()) return null;

        if (containsStopWord(t) || META_COLON.matcher(t).matches()
                || DATE_LIKE.matcher(t).matches() || ADDRESS_LIKE.matcher(t).matches()) return null;

        ArrayList<Token> nums = findNumberTokens(t);
        if (nums.size() < 2) return null;

        Token amountTok = selectBestAmountToken(nums);
        if (amountTok == null) return null;
        int amount = toInt(amountTok.text);

        double qty = 0;
        int amountIndex = nums.indexOf(amountTok);
        for (int i = amountIndex-1; i >= 0; i--) {
            double q = toDouble(nums.get(i).text);
            if (q >= 1 && q <= 50) { qty = q; break; }
        }
        if (qty < 1 || qty > 50) return null;

        QtyPrice qp = new QtyPrice(); qp.qty = qty; qp.price = amount;
        return qp;
    }

    // ── 토큰 추출 ────────────────────────────────────────────

    private static ArrayList<Token> findNumberTokens(String line) {
        ArrayList<Token> out = new ArrayList<>();
        Matcher m = MONEY_TOKEN.matcher(line);
        while (m.find()) {
            String g = m.group(1);
            int v = toInt(g);
            if (v > 0) out.add(new Token(g, m.start(1), m.end(1)));
        }
        return out;
    }

    // ── 필터/유틸 ────────────────────────────────────────────

    /**
     * ✅ 헤더 컷: "상품명 단가 수량 금액" 또는 첫 번째 실제 품목 라인이 나오기 전까지의
     * 상호명/주소/대표자/날짜 라인들을 제거
     * 푸터 컷: 합계/결제 라인부터 끝까지 제거
     */
    private static ArrayList<String> cropBeforeFooter(ArrayList<String> linesAll) {
        // ── 헤더 컷 ──────────────────────────────────────────
        int headerEnd = 0;
        for (int i = 0; i < linesAll.size(); i++) {
            String tc = linesAll.get(i).toLowerCase(Locale.ROOT).replace(" ", "");
            // "상품명 단가 수량 금" 행이 나오면 그 다음부터 품목
            if (tc.contains("상품명") && (tc.contains("단가") || tc.contains("수량"))) {
                headerEnd = i + 1;
                break;
            }
            // 실제 품목 패턴: 한글품목명 + 숫자 + 반복숫자(단가=금액)가 있는 줄
            String raw = linesAll.get(i);
            if (HAS_HANGUL.matcher(raw).find()) {
                ParsedLine p = parseInline(raw);
                if (p != null && p.price >= MIN_PRICE && looksLikeRealItemName(p.name)) {
                    headerEnd = i; // 이 줄부터 품목
                    break;
                }
            }
        }

        // ── 푸터 컷 ──────────────────────────────────────────
        int cut = linesAll.size();
        for (int i = headerEnd; i < linesAll.size(); i++) {
            String t  = linesAll.get(i).toLowerCase(Locale.ROOT);
            String tc = t.replace(" ", "");

            int hit = 0;
            if (tc.contains("합계")||tc.contains("총계")||tc.contains("총액")||tc.contains("총합")) hit++;
            if (tc.contains("결제")) hit++;
            if (tc.contains("승인")) hit++;
            if (tc.contains("카드"))  hit++;
            if (tc.contains("현금영수증")) hit++;
            if (tc.startsWith("합계") || tc.startsWith("계:")) hit += 2;
            if (hit >= 2) { cut = i; break; }
        }

        ArrayList<String> out = new ArrayList<>();
        for (int i = headerEnd; i < cut; i++) out.add(linesAll.get(i));
        return out;
    }

    private static boolean looksLikeItemLine(String line) {
        if (TextUtils.isEmpty(line)) return false;
        String t = line.trim();
        if (t.isEmpty() || ONLY_PUNC.matcher(t).matches()) return false;
        if (!HAS_LETTER.matcher(t).find()) return false;
        if (META_COLON.matcher(t).matches()) return false;
        if (DATE_LIKE.matcher(t).matches()) return false;
        if (RECEIPT_NO.matcher(t).matches()) return false;
        if (MANY_DIGITS.matcher(t).matches() && !HAS_HANGUL.matcher(t).find()) return false;
        if (containsStopWord(t)) return false;
        if (ADDRESS_LIKE.matcher(t).matches()) return false;
        ParsedLine p = parseInline(t);
        return p != null && p.price >= MIN_PRICE;
    }

    private static boolean looksLikeRealItemName(String name) {
        if (TextUtils.isEmpty(name)) return false;
        String t = name.trim();
        if (t.length() < 2) return false;
        if (!HAS_LETTER.matcher(t).find()) return false;
        if (containsStopWord(t)) return false;
        if (ADDRESS_LIKE.matcher(t).matches()) return false;
        if (RECEIPT_NO.matcher(t).matches()) return false;
        if (t.contains("*") && t.length() <= 10) return false;

        // ✅ 공백 포함 2글자 이하 한글 제외 ("나 키" 등)
        String noSpace = t.replace(" ","");
        if (noSpace.length() <= 2 && HAS_HANGUL.matcher(t).find()) return false;

        // ✅ 날짜 패턴
        if (t.matches(".*\\d{2}/\\d{2}/\\d{2}.*")) return false;

        // ✅ 영문만 1~2글자 제외
        if (!HAS_HANGUL.matcher(t).find()) {
            String onlyLetters = t.replaceAll("[^A-Za-z]","");
            if (onlyLetters.length() <= 2) return false;
        }
        return true;
    }

    private static boolean containsStopWord(String s) {
        if (TextUtils.isEmpty(s)) return false;
        String t = s.toLowerCase(Locale.ROOT);
        for (String w : STOP_WORDS) if (t.contains(w.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private static ArrayList<String> normalizeLines(String raw) {
        String[] arr = raw.replace("\r\n","\n").replace("\r","\n").split("\n");
        ArrayList<String> out = new ArrayList<>();
        for (String s : arr) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty() || ONLY_PUNC.matcher(t).matches()) continue;
            t = fixOcrBrokenThousands(t).replaceAll("\\s{2,}"," ").trim();
            out.add(t);
        }
        return out;
    }

    private static String fixOcrBrokenThousands(String s) {
        if (s == null) return "";
        String t = s;
        t = t.replaceAll("(\\d)\\s*[\\.,]\\s*(\\d{3})","$1,$2");
        t = t.replaceAll("(\\d),(\\s+)(\\d{3})","$1,$3");
        return t;
    }

    private static String cleanupName(String s) {
        if (s == null) return "";
        String t = s.trim()
                .replace("|"," ").replace("#"," ")
                .replaceAll("\\b단\\.?\\b"," ")
                .replaceAll("[\\p{Punct}]+$","")
                .replaceAll("\\s{2,}"," ").trim();

        // ✅ 끝에 남은 열린 괄호/불완전 조각 제거: "재사용봉투 (" → "재사용봉투"
        t = t.replaceAll("\\s*[\\(\\)]+\\s*$", "").trim();

        // ✅ 브랜드명 접두사 제거: "오뚜기) 옛날자른" → "옛날자른"
        // 패턴: 한글/영문 브랜드 + ) + 공백 으로 시작하면 제거
        t = t.replaceAll("^[가-힣a-zA-Z0-9]+\\)\\s+", "").trim();

        // ✅ 품목명 끝 규격 제거: 360ML, 10T, 500G, 1.5L, 2kg, 3개 등
        t = t.replaceAll("(?i)\\s*\\d+\\.?\\d*\\s*(ml|l|kg|g|t|개|ea|pack|봉|팩|입)\\s*$", "").trim();
        // 숫자+영문 조합 끝 제거 (공백 유무 무관)
        t = t.replaceAll("(?i)\\s*\\d+[a-zA-Z]+$", "").trim();

        return t;
    }

    /** ✅ 포함 관계까지 중복 체크: "아이스컵미디엄" ↔ "아이스컵" → 중복 */
    private static boolean isSeenDup(String key, HashSet<String> seen) {
        if (seen.contains(key)) return true;
        for (String s : seen) {
            if (s.contains(key) || key.contains(s)) return true;
        }
        return false;
    }

    private static int    toInt(String s)    { try { return Integer.parseInt(s.replace(",","").trim()); } catch(Exception e){ return 0; } }
    private static double toDouble(String s) { try { return Double.parseDouble(s.replace(",","").trim()); } catch(Exception e){ return 0; } }
    public  static String nameKey(String name) {
        if (name == null) return "";
        return name.trim().replace(" ","").toLowerCase(Locale.ROOT);
    }
}