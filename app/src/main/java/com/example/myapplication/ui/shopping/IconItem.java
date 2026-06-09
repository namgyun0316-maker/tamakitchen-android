package com.namgyun.tamakitchen.ui.shopping;

import java.util.Locale;

public class IconItem {

    private int resId;
    private String name;
    private String category;

    // 파일명 기반 키 (없을 수도 있음)
    private String rawKey;

    // 항상 통일된 형태로 제공
    private String normalizedKey;

    public IconItem(int resId, String name, String category) {
        this.resId = resId;
        this.name = name;
        this.category = category;
        this.rawKey = null;
        this.normalizedKey = "";
    }

    public IconItem(int resId, String name, String category, String rawKey) {
        this.resId = resId;
        this.name = name;
        this.category = category;
        setRawKey(rawKey);
    }

    public int getResId() { return resId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getRawKey() { return rawKey; }

    public String getNormalizedKey() {
        return normalizedKey != null ? normalizedKey : "";
    }

    public String getSearchKey() {
        String nk = getNormalizedKey();
        if (nk != null && !nk.trim().isEmpty()) return nk;
        return name != null ? name.trim() : "";
    }

    public void setResId(int resId) { this.resId = resId; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }

    public void setRawKey(String rawKey) {
        this.rawKey = rawKey;
        this.normalizedKey = normalizeKey(rawKey);
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.trim()
                .toLowerCase(Locale.getDefault())
                .replaceAll("\\s+", "_");
    }
}
