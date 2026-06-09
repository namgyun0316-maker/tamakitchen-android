package com.namgyun.tamakitchen.ui.fridge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class KoreanJosaUtil {

    private KoreanJosaUtil() {}

    /**
     * 마지막 글자 받침 유무로 '이/가' 결정
     * - 받침 있음 → 이
     * - 받침 없음 → 가
     * - 한글 아니면 기본 가
     *
     * ✅ 개선
     * - 뒤에 붙는 문장부호/괄호/따옴표/이모지 등을 뒤에서부터 건너뛰고 "마지막 유효 글자"를 찾음
     * - 예) "오이 ", "오이(유기농)", "오이!" -> 모두 "가"
     * - 예) "당근", "김" -> "이"
     */
    @NonNull
    public static String josaYiGa(@Nullable String word) {
        if (word == null) return "가";

        String w = word.trim();
        if (w.isEmpty()) return "가";

        int i = w.length() - 1;
        while (i >= 0) {
            char ch = w.charAt(i);

            // 공백은 건너뜀
            if (Character.isWhitespace(ch)) {
                i--;
                continue;
            }

            // ✅ 문장부호/괄호/따옴표 등은 건너뜀
            if (isIgnorableTailChar(ch)) {
                i--;
                continue;
            }

            // ✅ 한글 완성형이면 받침 판별
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int code = ch - 0xAC00;
                int jong = code % 28;
                return (jong == 0) ? "가" : "이";
            }

            // ✅ 한글이 아니면 기본 "가"
            break;
        }

        return "가";
    }

    /**
     * 👉 "오이가 추가되었습니다" 형태로 생성
     */
    @NonNull
    public static String buildAddedMessage(@Nullable String itemName) {
        String name = (itemName == null) ? "" : itemName.trim();
        if (name.isEmpty()) return "아이템이 추가되었습니다";
        return name + josaYiGa(name) + " 추가되었습니다";
    }

    // ---------------------------------------------------------
    // 내부 유틸
    // ---------------------------------------------------------

    /**
     * 단어 맨 끝에 붙는 경우가 많은 "무시 가능한 문자"들
     * - 괄호/따옴표/마침표/느낌표/물음표/쉼표 등
     * - 기호류로 끝나는 아이템명을 넣어도 조사 판별이 자연스럽게 되도록 함
     */
    private static boolean isIgnorableTailChar(char ch) {
        switch (ch) {
            case ')': case ']': case '}':
            case '(': case '[': case '{':
            case '"': case '\'': case '”': case '“': case '’': case '‘':
            case '.': case ',': case '!': case '?':
            case ':': case ';':
            case '~': case '…':
            case '·': case '•':
            case '/': case '\\':
            case '|':
            case '+': case '-': case '_':
            case '=':
            case '#': case '@':
            case '*':
                return true;
            default:
                // 일반적인 구두점/기호도 함께 처리
                return Character.getType(ch) == Character.OTHER_PUNCTUATION
                        || Character.getType(ch) == Character.DASH_PUNCTUATION
                        || Character.getType(ch) == Character.START_PUNCTUATION
                        || Character.getType(ch) == Character.END_PUNCTUATION
                        || Character.getType(ch) == Character.CONNECTOR_PUNCTUATION
                        || Character.getType(ch) == Character.MATH_SYMBOL
                        || Character.getType(ch) == Character.CURRENCY_SYMBOL
                        || Character.getType(ch) == Character.MODIFIER_SYMBOL
                        || Character.getType(ch) == Character.OTHER_SYMBOL;
        }
    }
}
