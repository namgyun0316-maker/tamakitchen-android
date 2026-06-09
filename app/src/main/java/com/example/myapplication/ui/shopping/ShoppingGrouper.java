// File: app/src/main/java/com/example/myapplication/ui/shopping/ShoppingGrouper.java
package com.namgyun.tamakitchen.ui.shopping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingGrouper {

    // key: "yyyy-MM-dd|storeId(or ALL)"
    private final Map<String, List<ShoppingItem>> grouped = new HashMap<>();

    // ✅ 합계는 long 권장(안전)
    private final Map<String, Long> totalMap = new HashMap<>();

    private String makeKey(String dateKey, Long storeId) {
        String d = (dateKey == null) ? "" : dateKey.trim();
        String sid = (storeId == null) ? "ALL" : String.valueOf(storeId);
        return d + "|" + sid;
    }

    public void clear() {
        grouped.clear();
        totalMap.clear();
    }

    public void putItem(String dateKey, Long storeId, ShoppingItem item) {
        String key = makeKey(dateKey, storeId);
        List<ShoppingItem> list = grouped.get(key);
        if (list == null) list = new ArrayList<>();
        list.add(item);
        grouped.put(key, list);
    }

    public List<ShoppingItem> getItems(String dateKey, Long storeId) {
        String key = makeKey(dateKey, storeId);
        List<ShoppingItem> list = grouped.get(key);
        return (list == null) ? new ArrayList<>() : new ArrayList<>(list);
    }

    public void recomputeTotals() {
        totalMap.clear();
        for (Map.Entry<String, List<ShoppingItem>> e : grouped.entrySet()) {
            long sum = 0L;
            List<ShoppingItem> list = e.getValue();
            if (list != null) {
                for (ShoppingItem it : list) {
                    if (it == null) continue;
                    // price(int) * quantity(double)
                    sum += Math.round((double) it.getPrice() * it.getQuantity());
                }
            }
            totalMap.put(e.getKey(), sum);
        }
    }

    public long getTotal(String dateKey, Long storeId) {
        String key = makeKey(dateKey, storeId);
        Long v = totalMap.get(key);
        return v == null ? 0L : v;
    }

    public Map<String, Long> getTotalMap() {
        return totalMap;
    }

    // =========================================================
    // ✅ 추가 기능: 날짜/월 기준 합계 & 판매점별 합계
    // =========================================================

    /** dateKey(yyyy-MM-dd) 기준: 모든 판매점 합계 */
    public long getDayTotalAllStores(String dateKey) {
        if (dateKey == null) return 0L;
        String prefix = dateKey.trim() + "|";
        long sum = 0L;
        for (Map.Entry<String, Long> e : totalMap.entrySet()) {
            String k = e.getKey();
            if (k != null && k.startsWith(prefix)) {
                Long v = e.getValue();
                if (v != null) sum += v;
            }
        }
        return sum;
    }

    /**
     * dateKey 기준: 판매점별 합계 (storeId -> amount)
     * - storeId가 null(미지정)이면 "ALL" 키가 아니라 "dateKey|ALL"로 들어가므로 storeId=null로 매핑한다.
     */
    public Map<Long, Long> getStoreTotalsForDate(String dateKey) {
        if (dateKey == null) return Collections.emptyMap();
        String prefix = dateKey.trim() + "|";
        Map<Long, Long> out = new HashMap<>();

        for (Map.Entry<String, Long> e : totalMap.entrySet()) {
            String k = e.getKey();
            if (k == null || !k.startsWith(prefix)) continue;

            String sidStr = k.substring(prefix.length()); // storeId or "ALL"
            Long storeId = null;
            if (!"ALL".equals(sidStr)) {
                try {
                    storeId = Long.parseLong(sidStr);
                } catch (Exception ignore) {
                    storeId = null;
                }
            }

            long v = (e.getValue() == null) ? 0L : e.getValue();
            Long prev = out.get(storeId);
            out.put(storeId, (prev == null ? 0L : prev) + v);
        }
        return out;
    }

    /**
     * dateKey 기준: 판매점 이름 (storeId -> storeName)
     * - grouped에서 해당 판매점 그룹 첫 아이템의 storeName을 사용
     * - 미지정(null)인 경우 "미지정"
     */
    public Map<Long, String> getStoreNamesForDate(String dateKey) {
        if (dateKey == null) return Collections.emptyMap();
        String prefix = dateKey.trim() + "|";

        Map<Long, String> out = new HashMap<>();
        for (Map.Entry<String, List<ShoppingItem>> e : grouped.entrySet()) {
            String k = e.getKey();
            if (k == null || !k.startsWith(prefix)) continue;

            String sidStr = k.substring(prefix.length());
            Long storeId = null;
            if (!"ALL".equals(sidStr)) {
                try {
                    storeId = Long.parseLong(sidStr);
                } catch (Exception ignore) {
                    storeId = null;
                }
            }

            String storeName = null;
            List<ShoppingItem> list = e.getValue();
            if (list != null) {
                for (ShoppingItem it : list) {
                    if (it == null) continue;
                    storeName = it.getStoreName();
                    if (storeName != null && !storeName.trim().isEmpty()) break;
                }
            }
            if (storeName == null || storeName.trim().isEmpty()) storeName = "미지정";
            out.put(storeId, storeName);
        }

        // 혹시 grouped에 없는데 totals만 있는 경우 대비
        if (!out.containsKey(null)) out.put(null, "미지정");
        return out;
    }

    /** 월 기준(년, month0) : 일별 합계 (day 1~31 -> amount) */
    public Map<Integer, Long> getDayTotalsForMonthAllStores(int year, int month0) {
        Map<Integer, Long> out = new HashMap<>();

        for (String key : totalMap.keySet()) {
            if (key == null) continue;

            // key = "yyyy-MM-dd|storeId"
            int bar = key.indexOf('|');
            if (bar <= 0) continue;

            String dateKey = key.substring(0, bar);
            int[] ymd = parseYmd(dateKey);
            if (ymd == null) continue;

            int y = ymd[0];
            int m0 = ymd[1];
            int d = ymd[2];

            if (y == year && m0 == month0) {
                long v = totalMap.get(key) == null ? 0L : totalMap.get(key);
                Long prev = out.get(d);
                out.put(d, (prev == null ? 0L : prev) + v);
            }
        }

        return out;
    }

    /** 월 기준(년, month0) : 전체 합계 */
    public long getMonthTotalAllStores(int year, int month0) {
        Map<Integer, Long> dayMap = getDayTotalsForMonthAllStores(year, month0);
        long sum = 0L;
        for (Long v : dayMap.values()) {
            if (v != null) sum += v;
        }
        return sum;
    }

    // yyyy-MM-dd -> [year, month0, day]
    private int[] parseYmd(String dateKey) {
        try {
            String[] p = dateKey.split("-");
            if (p.length < 3) return null;
            int y = Integer.parseInt(p[0]);
            int m1 = Integer.parseInt(p[1]); // 1~12
            int d = Integer.parseInt(p[2]);
            return new int[]{y, m1 - 1, d};
        } catch (Exception e) {
            return null;
        }
    }
}
