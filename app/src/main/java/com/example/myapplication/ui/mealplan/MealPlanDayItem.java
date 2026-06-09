package com.namgyun.tamakitchen.ui.mealplan;

public class MealPlanDayItem {

    private final String dayKey;
    private final String dayLabel;
    private final int dayOfMonth;
    private final Long recipeId;
    private final String recipeName;
    private final String recipeSummary;
    private final String imageUrl;
    private final String thumbnailUrl;
    private boolean favorite;

    public MealPlanDayItem(String dayKey,
                           String dayLabel,
                           int dayOfMonth,
                           Long recipeId,
                           String recipeName,
                           String recipeSummary,
                           String imageUrl,
                           String thumbnailUrl) {
        this.dayKey = dayKey;
        this.dayLabel = dayLabel;
        this.dayOfMonth = dayOfMonth;
        this.recipeId = recipeId;
        this.recipeName = recipeName == null ? "" : recipeName;
        this.recipeSummary = recipeSummary == null ? "" : recipeSummary;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.thumbnailUrl = thumbnailUrl == null ? "" : thumbnailUrl;
    }

    public String getDayKey() {
        return dayKey;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String getRecipeSummary() {
        return recipeSummary;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean hasRecipe() {
        return recipeId != null && recipeId > 0;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}