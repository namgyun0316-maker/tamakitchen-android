package com.namgyun.tamakitchen.ui.recipe;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecipeCreateRequest implements Serializable {

    @SerializedName("name")
    private String name;

    @SerializedName("summary")
    private String summary;

    // main image
    @SerializedName("imageUrl")
    private String imageUrl;

    // thumb image
    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    // 구버전 하위호환(콤마 문자열)
    @SerializedName("ingredients")
    private String ingredients;

    @SerializedName("optionalIngredients")
    private String optionalIngredients;

    @SerializedName("steps")
    private String steps;

    @SerializedName("category")
    private String category;

    @SerializedName("substitutesJson")
    private String substitutesJson;

    // 상세 재료 리스트
    @SerializedName("ingredientItems")
    private List<IngredientItemRequest> ingredientItems = new ArrayList<>();

    // 작성자 정보
    @SerializedName("userId")
    private Long userId;

    @SerializedName("userNickname")
    private String userNickname;

    // 사용 안 하면 null
    @SerializedName("calories")
    private Integer calories;

    @SerializedName("externalId")
    private String externalId;
    public RecipeCreateRequest() {}

    public RecipeCreateRequest(String name,
                               String summary,
                               String imageUrl,
                               String thumbnailUrl,
                               String ingredients,
                               String optionalIngredients,
                               String steps,
                               String category,
                               String substitutesJson,
                               List<IngredientItemRequest> ingredientItems,
                               Long userId,
                               String userNickname) {
        this.name = name;
        this.summary = summary;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.ingredients = ingredients;
        this.optionalIngredients = optionalIngredients;
        this.steps = steps;
        this.category = category;
        this.substitutesJson = substitutesJson;
        this.ingredientItems = (ingredientItems == null) ? new ArrayList<>() : ingredientItems;
        this.userId = userId;
        this.userNickname = userNickname;
        this.calories = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getOptionalIngredients() {
        return optionalIngredients;
    }

    public void setOptionalIngredients(String optionalIngredients) {
        this.optionalIngredients = optionalIngredients;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubstitutesJson() {
        return substitutesJson;
    }

    public void setSubstitutesJson(String substitutesJson) {
        this.substitutesJson = substitutesJson;
    }

    public List<IngredientItemRequest> getIngredientItems() {
        return ingredientItems == null ? new ArrayList<>() : ingredientItems;
    }

    public void setIngredientItems(List<IngredientItemRequest> ingredientItems) {
        this.ingredientItems = (ingredientItems == null) ? new ArrayList<>() : ingredientItems;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

}