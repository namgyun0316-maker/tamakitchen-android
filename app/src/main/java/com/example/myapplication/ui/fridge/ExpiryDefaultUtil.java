package com.namgyun.tamakitchen.ui.fridge;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

public class ExpiryDefaultUtil {

    private ExpiryDefaultUtil() {}

    public enum StorageType { FRIDGE, FREEZER, ROOM, ETC }

    public static StorageType parseStorage(String storageKorean) {
        if (storageKorean == null) return StorageType.ETC;

        String s = storageKorean.trim();

        if (s.contains("냉장")) return StorageType.FRIDGE;
        if (s.contains("냉동")) return StorageType.FREEZER;
        if (s.contains("실온")) return StorageType.ROOM;

        return StorageType.ETC;
    }

    @Nullable
    public static Integer getDefaultDaysOrNull(String itemName, StorageType storageType) {
        return computeDefaultDays(itemName, storageType);
    }

    @Nullable
    public static String computeExpiryDateFromToday(int days) {
        if (days <= 0) return null;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);

        return String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
    }

    @Nullable
    public static String computeDefaultExpiryDateIso(String itemName, String storageKorean) {
        StorageType st = parseStorage(storageKorean);

        Integer addDays = computeDefaultDays(itemName, st);

        if (addDays == null || addDays <= 0) return null;

        return computeExpiryDateFromToday(addDays);
    }

    @Nullable
    private static Integer computeDefaultDays(String itemName, StorageType storageType) {
        if (storageType == null || storageType == StorageType.ETC) return null;

        String name = normalize(itemName);
        if (TextUtils.isEmpty(name)) return null;

        // ===============================
        // ❌ 유통기한 디폴트 제거 대상
        // ===============================
        if (containsAny(name,
                "고추장",
                "굴소스",
                "불닭소스",
                "데리야끼소스", "데리야끼", "테리야끼소스", "테리야끼",
                "레몬즙",
                "맛소금",
                "메로나맛우유", "메로나맛 우유",
                "바베큐소스", "바비큐소스",
                "우유",
                "타르타르소스", "타르타르",
                "초고추장",
                "햄",
                "스팸",
                "리챔",
                "런천", "런천미트",
                "캔햄", "햄캔", "통조림햄",
                "칠리소스",
                "스테이크소스",
                "스리라차", "스리라차소스"
        )) {
            return null;
        }

        // =========================================================
        // 육류
        // =========================================================

        if (containsAny(name, "소막창", "우막창")) {
            if (storageType == StorageType.FREEZER) return 365;
            if (storageType == StorageType.FRIDGE) return 3;
            return null;
        }

        if (containsAny(name, "베이컨", "bacon")) {
            if (storageType == StorageType.FREEZER) return 90;
            if (storageType == StorageType.FRIDGE) return 14;
            return null;
        }

        if (containsAny(name, "토마호크", "tomahawk")) {
            if (storageType == StorageType.FREEZER) return 90;
            if (storageType == StorageType.FRIDGE) return 3;
            return null;
        }

        boolean beefSignal = containsAny(name, "소", "우", "한우", "beef");
        boolean porkSignal = containsAny(name, "돼지", "돈육", "포크", "pork");

        boolean isBeef = containsAny(name,
                "소", "우", "한우", "beef",
                "차돌", "채끝", "등심", "안심", "갈비", "갈비살",
                "부채", "우둔", "홍두깨", "사태", "양지",
                "목심", "앞다리", "앞다리살"
        );

        boolean isChicken = containsAny(name,
                "닭", "치킨", "계육", "chicken",
                "닭가슴", "닭다리", "닭봉", "윙"
        );

        boolean isPork = containsAny(name,
                "돼지", "돈육", "포크", "pork",
                "삼겹", "목심", "앞다리", "뒷다리", "항정", "가브리",
                "등갈비", "갈비",
                "안심", "등심"
        );

        boolean ambiguousCut = containsAny(name, "안심", "등심");

        if (ambiguousCut) {
            if (!beefSignal && !porkSignal) {
                isBeef = false;
                isPork = false;
            } else {
                isBeef = beefSignal;
                isPork = porkSignal;
            }
        }

        if (beefSignal && !porkSignal) {
            isPork = false;
        } else if (porkSignal && !beefSignal) {
            isBeef = false;
        }

        if (isChicken) {
            if (storageType == StorageType.FREEZER) return 180;
            if (storageType == StorageType.FRIDGE) return 2;
            return null;
        }

        if (isBeef && !isPork) {
            if (storageType == StorageType.FREEZER) return 90;
            if (storageType == StorageType.FRIDGE) return 3;
            return null;
        }

        if (isPork && !isBeef) {
            if (storageType == StorageType.FREEZER) return 30;
            if (storageType == StorageType.FRIDGE) return 3;
            return null;
        }

        // =========================================================
        // 채소/버섯/허브
        // =========================================================

        if (containsAny(name, "표고", "표고버섯")) {
            if (storageType == StorageType.FRIDGE) return 7;
            if (storageType == StorageType.FREEZER) return 30;
            return null;
        }

        if (containsAny(name, "버섯", "팽이", "새송이", "느타리")) {
            if (storageType == StorageType.FRIDGE) return 4;
            return null;
        }

        if (containsAny(name, "고추", "청양", "홍고추", "풋고추")) {
            if (storageType == StorageType.FRIDGE) return 7;
            if (storageType == StorageType.FREEZER) return 180;
            return null;
        }

        if (containsAny(name, "무")) {
            if (storageType == StorageType.ROOM) return 5;
            if (storageType == StorageType.FRIDGE) return 10;
            return null;
        }

        if (containsAny(name, "바질", "basil")) {
            if (storageType == StorageType.ROOM) return 5;
            return null;
        }

        if (containsAny(name, "방울토마토", "체리토마토")) {
            if (storageType == StorageType.ROOM) return 4;
            if (storageType == StorageType.FRIDGE) return 7;
            return null;
        }

        if (containsAny(name, "토마토")) {
            if (storageType == StorageType.ROOM) return 3;
            if (storageType == StorageType.FRIDGE) return 7;
            return null;
        }

        if (containsAny(name, "비트")) {
            if (storageType == StorageType.FRIDGE) return 14;
            return null;
        }

        if (containsAny(name, "생강")) {
            if (storageType == StorageType.FRIDGE) return 14;
            if (storageType == StorageType.FREEZER) return 180;
            return null;
        }

        if (containsAny(name, "시금치")) {
            if (storageType == StorageType.FRIDGE) return 4;
            return null;
        }

        if (containsAny(name, "양상추", "로메인")) {
            if (storageType == StorageType.FRIDGE) return 5;
            return null;
        }

        if (containsAny(name, "옥수수", "콘")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 60;
            return null;
        }

        if (containsAny(name, "양파")) {
            if (storageType == StorageType.ROOM) return 21;
            return null;
        }

        if (containsAny(name, "마늘")) {
            if (storageType == StorageType.ROOM) return 21;
            return null;
        }

        if (containsAny(name, "대파", "쪽파", "파")) {
            if (storageType == StorageType.FRIDGE) return 10;
            if (storageType == StorageType.FREEZER) return 30;
            return null;
        }

        if (containsAny(name, "가지")) {
            if (storageType == StorageType.FRIDGE) return 5;
            return null;
        }

        if (containsAny(name, "양배추")) {
            if (storageType == StorageType.FRIDGE) return 14;
            return null;
        }

        if (containsAny(name, "애호박", "호박")) {
            if (storageType == StorageType.FRIDGE) return 6;
            return null;
        }

        if (containsAny(name, "브로콜리")) {
            if (storageType == StorageType.FRIDGE) return 7;
            return null;
        }

        if (containsAny(name, "당근")) {
            if (storageType == StorageType.FRIDGE) return 14;
            return null;
        }

        if (containsAny(name, "오이")) {
            if (storageType == StorageType.FRIDGE) return 7;
            return null;
        }

        if (containsAny(name, "감자")) {
            if (storageType == StorageType.ROOM) return 21;
            return null;
        }

        if (containsAny(name, "고구마")) {
            if (storageType == StorageType.ROOM) return 14;
            return null;
        }

        // =========================================================
        // 과일
        // =========================================================

        if (containsAny(name, "감")) {
            if (storageType == StorageType.ROOM) return 3;
            if (storageType == StorageType.FRIDGE) return 14;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "귤", "밀감")) {
            if (storageType == StorageType.ROOM) return 4;
            if (storageType == StorageType.FRIDGE) return 21;
            if (storageType == StorageType.FREEZER) return 45;
            return null;
        }

        if (containsAny(name, "딸기")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 45;
            return null;
        }

        if (containsAny(name, "라임", "lime")) {
            if (storageType == StorageType.ROOM) return 6;
            if (storageType == StorageType.FRIDGE) return 21;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "레몬", "lemon")) {
            if (storageType == StorageType.ROOM) return 6;
            if (storageType == StorageType.FRIDGE) return 28;
            if (storageType == StorageType.FREEZER) return 105;
            return null;
        }

        if (containsAny(name, "망고", "mango")) {
            if (storageType == StorageType.ROOM) return 3;
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "멜론")) {
            if (storageType == StorageType.ROOM) return 2;
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "배")) {
            if (storageType == StorageType.ROOM) return 3;
            if (storageType == StorageType.FRIDGE) return 28;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "복숭아")) {
            if (storageType == StorageType.ROOM) return 2;
            if (storageType == StorageType.FRIDGE) return 4;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "블루베리")) {
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 180;
            return null;
        }

        if (containsAny(name, "사과")) {
            if (storageType == StorageType.ROOM) return 3;
            if (storageType == StorageType.FRIDGE) return 28;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "샤인머스켓", "샤인")) {
            if (storageType == StorageType.FRIDGE) return 9;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "수박")) {
            if (storageType == StorageType.ROOM) return 2;
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "아보카도", "avocado")) {
            if (storageType == StorageType.ROOM) return 3;
            if (storageType == StorageType.FRIDGE) return 4;
            if (storageType == StorageType.FREEZER) return 105;
            return null;
        }

        if (containsAny(name, "오렌지", "orange")) {
            if (storageType == StorageType.ROOM) return 6;
            if (storageType == StorageType.FRIDGE) return 21;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "올리브", "olive")) {
            if (storageType == StorageType.FRIDGE) return 14;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "자두")) {
            if (storageType == StorageType.ROOM) return 2;
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "체리", "cherry")) {
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 180;
            return null;
        }

        if (containsAny(name, "키위", "kiwi")) {
            if (storageType == StorageType.ROOM) return 4;
            if (storageType == StorageType.FRIDGE) return 21;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "파인애플", "pineapple")) {
            if (storageType == StorageType.ROOM) return 2;
            if (storageType == StorageType.FRIDGE) return 6;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "포도")) {
            if (storageType == StorageType.FRIDGE) return 9;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        // =========================================================
        // 해산물
        // =========================================================

        if (containsAny(name, "갈치")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "굴")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 45;
            return null;
        }

        if (containsAny(name, "고등어")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "광어")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "가리비")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "낙지")) {
            if (storageType == StorageType.FRIDGE) return 1;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "게")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "꽁치")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "대게")) {
            if (storageType == StorageType.FRIDGE) return 1;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "도다리")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "도미")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "랍스터", "로브스터", "lobster")) {
            if (storageType == StorageType.FRIDGE) return 1;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "멍게")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 45;
            return null;
        }

        if (containsAny(name, "문어")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "바지락")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 45;
            return null;
        }

        if (containsAny(name, "새우")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "성게알", "성게")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 30;
            return null;
        }

        if (containsAny(name, "숭어")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "연어")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "오징어")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "장어")) {
            if (storageType == StorageType.FRIDGE) return 1;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "전복")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "청어")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "키조개")) {
            if (storageType == StorageType.FRIDGE) return 2;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "킹크랩")) {
            if (storageType == StorageType.FRIDGE) return 1;
            if (storageType == StorageType.FREEZER) return 75;
            return null;
        }

        if (containsAny(name, "홍합")) {
            if (storageType == StorageType.FRIDGE) return 3;
            if (storageType == StorageType.FREEZER) return 45;
            return null;
        }

        return null;
    }

    private static String normalize(String s) {
        if (s == null) return "";

        String x = s.trim().toLowerCase(Locale.getDefault());
        x = x.replaceAll("\\s+", "");

        return x;
    }

    private static boolean containsAny(String normalizedName, String... keywords) {
        if (TextUtils.isEmpty(normalizedName) || keywords == null) return false;

        for (String k : keywords) {
            if (k == null) continue;

            String kk = k.trim()
                    .toLowerCase(Locale.getDefault())
                    .replaceAll("\\s+", "");

            if (kk.isEmpty()) continue;

            if (normalizedName.contains(kk)) return true;
        }

        return false;
    }
}