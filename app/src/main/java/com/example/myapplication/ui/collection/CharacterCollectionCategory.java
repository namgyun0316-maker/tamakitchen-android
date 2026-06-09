package com.namgyun.tamakitchen.ui.collection;

public enum CharacterCollectionCategory {
    INGREDIENT("재료"),
    FOOD("음식");

    private final String displayName;

    CharacterCollectionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}