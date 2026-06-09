package com.namgyun.tamakitchen.ui.recipe;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeResponse implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("summary")
    private String summary;

    // ✅ main image (webp)
    @SerializedName("imageUrl")
    private String imageUrl;

    // ✅ thumb image (webp)
    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    // 구버전 필수재료 CSV
    @SerializedName("ingredients")
    private String ingredients;

    // 서버가 optionalIngredientsText 로 내려줌
    @SerializedName("optionalIngredientsText")
    private String optionalIngredientsText;

    // ✅ 서버가 내려주는 대체재료 JSON 문자열
    @SerializedName("substitutesJson")
    private String substitutesJson;

    @SerializedName("steps")
    private String steps;

    @SerializedName("category")
    private String category;

    @SerializedName("servings")
    private Integer servings;

    @SerializedName("ratingAverage")
    private float ratingAverage;

    @SerializedName("ratingCount")
    private int ratingCount;

    @SerializedName("usedIngredients")
    private ArrayList<String> usedIngredients = new ArrayList<>();

    @SerializedName("missingIngredients")
    private ArrayList<String> missingIngredients = new ArrayList<>();

    // 추천에서 실제 사용된 대체
    @SerializedName("substitutionsUsed")
    private ArrayList<String> substitutionsUsed = new ArrayList<>();

    @SerializedName("matchPercent")
    private int matchPercent;

    // =========================
    // ✅ 작성자 정보
    // =========================
    @SerializedName("authorId")
    private Long authorId;

    @SerializedName("authorNickname")
    private String authorNickname;

    // =========================
    // ✅ 서버 응답 키와 "완전히 동일"하게 맞춤
    // =========================

    // ✅ ALL (REQUIRED/OPTIONAL/SUBSTITUTE 포함) - 서버가 만들어서 내려줌
    @SerializedName("ingredientItems")
    private ArrayList<RecipeIngredientItem> ingredientItems = new ArrayList<>();

    // ✅ 필수/선택 (서버 키: requiredIngredientsItems / optionalIngredientsItems)
    @SerializedName("requiredIngredientsItems")
    private ArrayList<RecipeIngredientItem> requiredIngredientsItems = new ArrayList<>();

    @SerializedName("optionalIngredientsItems")
    private ArrayList<RecipeIngredientItem> optionalIngredientsItems = new ArrayList<>();

    // ✅ 등록된 대체 row (서버 키: substituteIngredientsItems)
    @SerializedName("substituteIngredientsItems")
    private ArrayList<RecipeIngredientItem> substituteIngredientsItems = new ArrayList<>();

    // ✅ 서버가 이미 만들어주는 표시용 라인
    @SerializedName("substituteLines")
    private ArrayList<String> substituteLines = new ArrayList<>();

    // ✅ 상세 재료 표시용 라인 (필수+선택만)
    @SerializedName("detailIngredientLines")
    private ArrayList<String> detailIngredientLines = new ArrayList<>();

    // =========================
    // ✅✅✅ 조리도구 (서버에서 내려옴)
    // =========================
    @SerializedName("cookToolsText")
    private String cookToolsText; // 예: "GAS_RANGE, INDUCTION"

    @SerializedName("cookTools")
    private ArrayList<String> cookTools = new ArrayList<>(); // 예: ["GAS_RANGE","INDUCTION"]

    // -------------------------
    // inner DTO
    // -------------------------
    public static class RecipeIngredientItem implements Serializable {

        @SerializedName("id")
        private Long id;

        @SerializedName("name")
        private String name;

        @SerializedName("type")
        private String type; // REQUIRED / OPTIONAL / SUBSTITUTE

        @SerializedName("amount")
        private Double amount;

        @SerializedName("unit")
        private String unit;

        @SerializedName("substituteFor")
        private String substituteFor;

        @SerializedName("quantityText")
        private String quantityText;

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public Double getAmount() { return amount; }
        public String getUnit() { return unit; }
        public String getSubstituteFor() { return substituteFor; }
        public String getQuantityText() { return quantityText; }

        // 네 코드에서 prettyQuantity를 우선 보려 해서 안전하게
        public String getPrettyQuantity() {
            if (quantityText == null) return null;
            String t = quantityText.trim();
            return t.isEmpty() ? null : t;
        }
    }

    // -------------------------
    // getters / setters
    // -------------------------
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSummary() { return summary; }

    public String getImageUrl() { return imageUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }

    // ✅ 필요하면 쓰라고 setter도 추가(선택)
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getIngredients() { return ingredients; }
    public String getOptionalIngredientsText() { return optionalIngredientsText; }
    public String getSubstitutesJson() { return substitutesJson; }

    public String getSteps() { return steps; }
    public String getCategory() { return category; }
    public Integer getServings() { return servings; }

    public float getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }

    public ArrayList<String> getUsedIngredients() { return usedIngredients; }
    public void setUsedIngredients(ArrayList<String> usedIngredients) {
        this.usedIngredients = usedIngredients == null ? new ArrayList<>() : usedIngredients;
    }

    public ArrayList<String> getMissingIngredients() { return missingIngredients; }
    public void setMissingIngredients(ArrayList<String> missingIngredients) {
        this.missingIngredients = missingIngredients == null ? new ArrayList<>() : missingIngredients;
    }

    public ArrayList<String> getSubstitutionsUsed() { return substitutionsUsed; }

    public int getMatchPercent() { return matchPercent; }
    public void setMatchPercent(int matchPercent) { this.matchPercent = matchPercent; }

    // ✅ 작성자 getter / setter
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }

    // ✅ 서버에서 내려오는 리스트 그대로 반환
    public ArrayList<RecipeIngredientItem> getIngredientItems() {
        return ingredientItems == null ? new ArrayList<>() : ingredientItems;
    }

    public ArrayList<RecipeIngredientItem> getRequiredIngredientsItems() {
        return requiredIngredientsItems == null ? new ArrayList<>() : requiredIngredientsItems;
    }

    public ArrayList<RecipeIngredientItem> getOptionalIngredientsItems() {
        return optionalIngredientsItems == null ? new ArrayList<>() : optionalIngredientsItems;
    }

    public List<RecipeIngredientItem> getSubstituteIngredients() {
        return substituteIngredientsItems == null ? Collections.emptyList() : substituteIngredientsItems;
    }

    public List<String> getDetailIngredientLines() {
        return detailIngredientLines == null ? Collections.emptyList() : detailIngredientLines;
    }

    public List<String> getSubstituteLines() {
        return substituteLines == null ? Collections.emptyList() : substituteLines;
    }

    // =========================
    // ✅✅✅ cook tools getters
    // =========================
    public String getCookToolsText() {
        return cookToolsText;
    }

    public ArrayList<String> getCookTools() {
        return cookTools == null ? new ArrayList<>() : cookTools;
    }

    // 선택재료 pretty (기존 유지)
    public String buildOptionalIngredientsPretty() {
        if (optionalIngredientsText != null && !optionalIngredientsText.trim().isEmpty()) {
            return optionalIngredientsText.trim();
        }

        if (optionalIngredientsItems != null && !optionalIngredientsItems.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (RecipeIngredientItem it : optionalIngredientsItems) {
                if (it == null || it.getName() == null) continue;
                String t = it.getName().trim();
                if (!t.isEmpty()) names.add(t);
            }
            if (!names.isEmpty()) return android.text.TextUtils.join(", ", names);
        }

        return "-";
    }
}