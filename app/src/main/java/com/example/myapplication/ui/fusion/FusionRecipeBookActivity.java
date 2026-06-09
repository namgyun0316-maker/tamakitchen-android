package com.namgyun.tamakitchen.ui.fusion;

import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.IngredientCharacter;

import java.util.ArrayList;
import java.util.List;

public class FusionRecipeBookActivity extends AppCompatActivity {

    private RecyclerView recyclerFusionRecipes;

    private FusionRecipeBookAdapter adapter;
    private final List<FusionRecipeBookItem> recipeItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fusion_recipe_book);

        bindViews();
        setupRecyclerView();
        loadRecipes();
    }

    private void bindViews() {
        recyclerFusionRecipes = findViewById(R.id.recyclerFusionRecipes);
    }

    private void setupRecyclerView() {
        recyclerFusionRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new com.namgyun.tamakitchen.ui.fusion.FusionRecipeBookAdapter(this, recipeItems);
        recyclerFusionRecipes.setAdapter(adapter);
    }

    private void loadRecipes() {
        recipeItems.clear();

        addRecipe(IngredientCharacter.STRAWBERRY, IngredientCharacter.FLOUR, FusionFood.STRAWBERRY_CAKE);
        addRecipe(IngredientCharacter.PISTACHIO, IngredientCharacter.CHOCOLATE, FusionFood.DDU_JJON_KKU);
        addRecipe(IngredientCharacter.CHESTNUT, IngredientCharacter.COFFEE_BEAN, FusionFood.CHESTNUT_TIRAMISU);

        addRecipe(IngredientCharacter.STRAWBERRY, IngredientCharacter.MILK, FusionFood.STRAWBERRY_SHAKE);
        addRecipe(IngredientCharacter.STRAWBERRY, IngredientCharacter.SPARKLING_WATER, FusionFood.STRAWBERRY_ADE);
        addRecipe(IngredientCharacter.SPARKLING_WATER, IngredientCharacter.GREEN_GRAPE, FusionFood.GREEN_GRAPE_ADE);

        addRecipe(IngredientCharacter.FLOUR, IngredientCharacter.CHOCOLATE, FusionFood.COOKIE);
        addRecipe(IngredientCharacter.FLOUR, IngredientCharacter.GREEN_ONION, FusionFood.PAJEON);
        addRecipe(IngredientCharacter.SWEET_POTATO, IngredientCharacter.SUGAR, FusionFood.SWEET_POTATO_MATANG);
        addRecipe(IngredientCharacter.BANANA, IngredientCharacter.MILK, FusionFood.BANANA_SHAKE);
        addRecipe(IngredientCharacter.STRAWBERRY, IngredientCharacter.SUGAR, FusionFood.STRAWBERRY_JAM);
        addRecipe(IngredientCharacter.WALNUT, IngredientCharacter.SUGAR, FusionFood.WALNUT_GANGJEONG);
        addRecipe(IngredientCharacter.GREEN_GRAPE, IngredientCharacter.SUGAR, FusionFood.GRAPE_JAM);
        addRecipe(IngredientCharacter.CHESTNUT, IngredientCharacter.SUGAR, FusionFood.SWEET_CHESTNUT);
        addRecipe(IngredientCharacter.CHOCOLATE, IngredientCharacter.BANANA, FusionFood.CHOCO_BANANA);
        addRecipe(IngredientCharacter.CHOCOLATE, IngredientCharacter.MILK, FusionFood.CHOCO_MILK);
        addRecipe(IngredientCharacter.MILK, IngredientCharacter.COFFEE_BEAN, FusionFood.CAFE_LATTE);
        addRecipe(IngredientCharacter.FLOUR, IngredientCharacter.MILK, FusionFood.BAGUETTE);
        addRecipe(IngredientCharacter.MILK, IngredientCharacter.ONION, FusionFood.ONION_CREAM_SOUP);
        addRecipe(IngredientCharacter.TOMATO, IngredientCharacter.PASTA_NOODLE, FusionFood.TOMATO_SPAGHETTI);
        addRecipe(IngredientCharacter.FLOUR, IngredientCharacter.BUTTER, FusionFood.PANCAKE);
        addRecipe(IngredientCharacter.POTATO, IngredientCharacter.ONION, FusionFood.HASH_BROWN);
        addRecipe(IngredientCharacter.APPLE, IngredientCharacter.SUGAR, FusionFood.APPLE_JAM);
        addRecipe(IngredientCharacter.BLUEBERRY, IngredientCharacter.FLOUR, FusionFood.BLUEBERRY_MUFFIN);
        addRecipe(IngredientCharacter.LEMON, IngredientCharacter.SPARKLING_WATER, FusionFood.LEMON_ADE);
        addRecipe(IngredientCharacter.GREEN_TEA, IngredientCharacter.MILK, FusionFood.MATCHA_LATTE);
        addRecipe(IngredientCharacter.CHOCOLATE, IngredientCharacter.BUTTER, FusionFood.BROWNIE);

        adapter.notifyDataSetChanged();
    }

    private void addRecipe(
            IngredientCharacter firstIngredient,
            IngredientCharacter secondIngredient,
            FusionFood resultFood
    ) {
        recipeItems.add(new FusionRecipeBookItem(
                firstIngredient,
                secondIngredient,
                resultFood
        ));
    }

    @DrawableRes
    public int resolveIngredientImageResId(@Nullable IngredientCharacter ingredient) {
        if (ingredient == null) return 0;

        String originalName = ingredient.getDrawableName();
        if (originalName == null || originalName.trim().isEmpty()) return 0;

        String trimmedBodyName = originalName;
        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[]{
                trimmedBodyName + "_catalog",
                originalName + "_catalog",
                originalName
        };

        for (String candidate : candidates) {
            int resId = getResources().getIdentifier(candidate, "drawable", getPackageName());
            if (resId != 0) {
                return resId;
            }
        }

        return 0;
    }

    @DrawableRes
    public int resolveFoodImageResId(@Nullable FusionFood food) {
        if (food == null) return 0;

        String originalName = food.getDrawableName();
        if (originalName == null || originalName.trim().isEmpty()) return 0;

        String trimmedBodyName = originalName;
        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[]{
                trimmedBodyName + "_catalog",
                originalName + "_catalog",
                originalName
        };

        for (String candidate : candidates) {
            int resId = getResources().getIdentifier(candidate, "drawable", getPackageName());
            if (resId != 0) {
                return resId;
            }
        }

        return 0;
    }
}