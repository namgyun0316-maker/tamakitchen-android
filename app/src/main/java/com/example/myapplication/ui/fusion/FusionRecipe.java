package com.namgyun.tamakitchen.ui.fusion;

import androidx.annotation.Nullable;

import com.namgyun.tamakitchen.pet.IngredientCharacter;

public final class FusionRecipe {

    private FusionRecipe() {
    }

    @Nullable
    public static FusionFood getResult(
            IngredientCharacter first,
            IngredientCharacter second
    ) {
        if (first == null || second == null) return null;

        if (isPair(first, second, IngredientCharacter.STRAWBERRY, IngredientCharacter.FLOUR)) {
            return FusionFood.STRAWBERRY_CAKE;
        }

        if (isPair(first, second, IngredientCharacter.PISTACHIO, IngredientCharacter.CHOCOLATE)) {
            return FusionFood.DDU_JJON_KKU;
        }

        if (isPair(first, second, IngredientCharacter.CHESTNUT, IngredientCharacter.COFFEE_BEAN)) {
            return FusionFood.CHESTNUT_TIRAMISU;
        }

        if (isPair(first, second, IngredientCharacter.STRAWBERRY, IngredientCharacter.MILK)) {
            return FusionFood.STRAWBERRY_SHAKE;
        }

        if (isPair(first, second, IngredientCharacter.STRAWBERRY, IngredientCharacter.SPARKLING_WATER)) {
            return FusionFood.STRAWBERRY_ADE;
        }

        if (isPair(first, second, IngredientCharacter.SPARKLING_WATER, IngredientCharacter.GREEN_GRAPE)) {
            return FusionFood.GREEN_GRAPE_ADE;
        }


        if (isPair(first, second, IngredientCharacter.FLOUR, IngredientCharacter.CHOCOLATE)) {
            return FusionFood.COOKIE;
        }

        if (isPair(first, second, IngredientCharacter.FLOUR, IngredientCharacter.GREEN_ONION)) {
            return FusionFood.PAJEON;
        }

        if (isPair(first, second, IngredientCharacter.SWEET_POTATO, IngredientCharacter.SUGAR)) {
            return FusionFood.SWEET_POTATO_MATANG;
        }
        if (isPair(first, second, IngredientCharacter.CHOCOLATE, IngredientCharacter.BUTTER)) {
            return FusionFood.BROWNIE;
        }
        if (isPair(first, second, IngredientCharacter.BANANA, IngredientCharacter.MILK)) {
            return FusionFood.BANANA_SHAKE;
        }

        if (isPair(first, second, IngredientCharacter.STRAWBERRY, IngredientCharacter.SUGAR)) {
            return FusionFood.STRAWBERRY_JAM;
        }

        if (isPair(first, second, IngredientCharacter.WALNUT, IngredientCharacter.SUGAR)) {
            return FusionFood.WALNUT_GANGJEONG;
        }

        if (isPair(first, second, IngredientCharacter.GREEN_GRAPE, IngredientCharacter.SUGAR)) {
            return FusionFood.GRAPE_JAM;
        }

        if (isPair(first, second, IngredientCharacter.CHESTNUT, IngredientCharacter.SUGAR)) {
            return FusionFood.SWEET_CHESTNUT;
        }

        if (isPair(first, second, IngredientCharacter.CHOCOLATE, IngredientCharacter.BANANA)) {
            return FusionFood.CHOCO_BANANA;
        }

        if (isPair(first, second, IngredientCharacter.CHOCOLATE, IngredientCharacter.MILK)) {
            return FusionFood.CHOCO_MILK;
        }

        if (isPair(first, second, IngredientCharacter.MILK, IngredientCharacter.COFFEE_BEAN)) {
            return FusionFood.CAFE_LATTE;
        }

        if (isPair(first, second, IngredientCharacter.FLOUR, IngredientCharacter.MILK)) {
            return FusionFood.BAGUETTE;
        }

        if (isPair(first, second, IngredientCharacter.MILK, IngredientCharacter.ONION)) {
            return FusionFood.ONION_CREAM_SOUP;
        }

        if (isPair(first, second, IngredientCharacter.TOMATO, IngredientCharacter.PASTA_NOODLE)) {
            return FusionFood.TOMATO_SPAGHETTI;
        }

        if (isPair(first, second, IngredientCharacter.FLOUR, IngredientCharacter.BUTTER)) {
            return FusionFood.PANCAKE;
        }

        if (isPair(first, second, IngredientCharacter.POTATO, IngredientCharacter.ONION)) {
            return FusionFood.HASH_BROWN;
        }

        if (isPair(first, second, IngredientCharacter.APPLE, IngredientCharacter.SUGAR)) {
            return FusionFood.APPLE_JAM;
        }

        if (isPair(first, second, IngredientCharacter.APPLE, IngredientCharacter.FLOUR)) {
            return FusionFood.APPLE_JAM;
        }

        if (isPair(first, second, IngredientCharacter.BLUEBERRY, IngredientCharacter.FLOUR)) {
            return FusionFood.BLUEBERRY_MUFFIN;
        }

        if (isPair(first, second, IngredientCharacter.LEMON, IngredientCharacter.SPARKLING_WATER)) {
            return FusionFood.LEMON_ADE;
        }

        if (isPair(first, second, IngredientCharacter.GREEN_TEA, IngredientCharacter.MILK)) {
            return FusionFood.MATCHA_LATTE;
        }

        return null;
    }

    public static boolean canFuse(
            IngredientCharacter first,
            IngredientCharacter second
    ) {
        return getResult(first, second) != null;
    }

    private static boolean isPair(
            IngredientCharacter first,
            IngredientCharacter second,
            IngredientCharacter a,
            IngredientCharacter b
    ) {
        return (first == a && second == b) || (first == b && second == a);
    }
}