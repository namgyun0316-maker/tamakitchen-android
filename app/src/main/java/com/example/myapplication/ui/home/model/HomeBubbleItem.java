package com.namgyun.tamakitchen.ui.home.model;

import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;

public class HomeBubbleItem {

    public enum Type {
        EXPIRY,     // 유통기한 임박
        TODAY_RECIPE // 오늘 추천
    }

    private final Type type;
    private final String title;
    private final String message;
    private final RecipeResponse recipe; // TODAY_RECIPE일 때 사용

    public HomeBubbleItem(Type type, String title, String message, RecipeResponse recipe) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.recipe = recipe;
    }

    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public RecipeResponse getRecipe() { return recipe; }
}
