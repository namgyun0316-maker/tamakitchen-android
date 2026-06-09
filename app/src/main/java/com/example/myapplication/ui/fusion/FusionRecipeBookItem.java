package com.namgyun.tamakitchen.ui.fusion;

import androidx.annotation.NonNull;

import com.namgyun.tamakitchen.pet.IngredientCharacter;

public class FusionRecipeBookItem {

    private final IngredientCharacter firstIngredient;
    private final IngredientCharacter secondIngredient;
    private final FusionFood resultFood;

    public FusionRecipeBookItem(
            @NonNull IngredientCharacter firstIngredient,
            @NonNull IngredientCharacter secondIngredient,
            @NonNull FusionFood resultFood
    ) {
        this.firstIngredient = firstIngredient;
        this.secondIngredient = secondIngredient;
        this.resultFood = resultFood;
    }

    public IngredientCharacter getFirstIngredient() {
        return firstIngredient;
    }

    public IngredientCharacter getSecondIngredient() {
        return secondIngredient;
    }

    public FusionFood getResultFood() {
        return resultFood;
    }
}