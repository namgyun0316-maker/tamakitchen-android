package com.namgyun.tamakitchen.ui.recipe;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class IngredientItemRequest implements Serializable {

    // ---------- type 상수 ----------
    public static final String TYPE_REQUIRED = "REQUIRED";
    public static final String TYPE_OPTIONAL = "OPTIONAL";
    public static final String TYPE_SUBSTITUTE = "SUBSTITUTE";

    @SerializedName("name")
    private String name; // 재료명

    @SerializedName("type")
    private String type; // REQUIRED / OPTIONAL / SUBSTITUTE

    // (확장용) 정량 데이터 필요해지면 사용
    @SerializedName("amount")
    private Double amount;

    @SerializedName("unit")
    private String unit;

    // type=SUBSTITUTE일 때 어떤 재료의 대체인지
    @SerializedName("substituteFor")
    private String substituteFor;

    // 자유 수량 표현(예: 300g, 1/2개, 한꼬집, 2스푼)
    @SerializedName("quantityText")
    private String quantityText;

    public IngredientItemRequest() {}

    /** 가장 자주 쓰는 형태: name + type + quantityText */
    public IngredientItemRequest(String name, String type, String quantityText) {
        this.name = safeTrim(name);
        this.type = safeTrim(type);
        this.quantityText = safeTrim(quantityText);
    }

    /** 전체 필드 생성자(확장용) */
    public IngredientItemRequest(String name,
                                 String type,
                                 Double amount,
                                 String unit,
                                 String substituteFor,
                                 String quantityText) {
        this.name = safeTrim(name);
        this.type = safeTrim(type);
        this.amount = amount;
        this.unit = safeTrim(unit);
        this.substituteFor = safeTrim(substituteFor);
        this.quantityText = safeTrim(quantityText);
    }

    // ---------- getters/setters ----------
    public String getName() { return name; }
    public void setName(String name) { this.name = safeTrim(name); }

    public String getType() { return type; }
    public void setType(String type) { this.type = safeTrim(type); }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = safeTrim(unit); }

    public String getSubstituteFor() { return substituteFor; }
    public void setSubstituteFor(String substituteFor) { this.substituteFor = safeTrim(substituteFor); }

    public String getQuantityText() { return quantityText; }
    public void setQuantityText(String quantityText) { this.quantityText = safeTrim(quantityText); }

    // ---------- 편의 메서드 ----------
    public boolean isRequired() {
        return TYPE_REQUIRED.equalsIgnoreCase(type);
    }

    public boolean isOptional() {
        return TYPE_OPTIONAL.equalsIgnoreCase(type);
    }

    public boolean isSubstitute() {
        return TYPE_SUBSTITUTE.equalsIgnoreCase(type);
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
