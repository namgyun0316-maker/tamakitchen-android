package com.namgyun.tamakitchen.network;

import com.namgyun.tamakitchen.ui.recipe.RatingRequest;
import com.namgyun.tamakitchen.ui.recipe.RecipeCreateRequest;
import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;
import com.namgyun.tamakitchen.ui.recipe.RecipeStepResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RecipeApiService {

    @GET("api/recipes/recommend")
    Call<List<RecipeResponse>> getRecommendedRecipes(
            @Query("userId") Long userId,
            @Query("fridgeId") Long fridgeId
    );

    @GET("api/recipes")
    Call<List<RecipeResponse>> getAllRecipes();

    @GET("api/recipes/light")
    Call<List<RecipeResponse>> getAllRecipesLight();

    @GET("api/recipes/my")
    Call<List<RecipeResponse>> getMyRecipes(
            @Query("userId") Long userId
    );

    @GET("api/recipes/{id}")
    Call<RecipeResponse> getRecipeById(@Path("id") Long recipeId);

    @POST("api/recipes")
    Call<Void> createRecipe(@Body RecipeCreateRequest request);

    @PUT("api/recipes/{id}")
    Call<Void> updateRecipe(
            @Path("id") Long recipeId,
            @Body RecipeCreateRequest request
    );

    @DELETE("api/recipes/{id}")
    Call<Void> deleteRecipe(
            @Path("id") Long recipeId,
            @Query("userId") Long userId
    );

    @POST("api/recipes/{id}/rating")
    Call<RecipeResponse> rateRecipe(
            @Path("id") Long recipeId,
            @Body RatingRequest request
    );

    @GET("api/recipes/{id}/steps")
    Call<List<RecipeStepResponse>> getRecipeSteps(@Path("id") Long recipeId);

    @POST("api/recipes/{id}/view")
    Call<Void> increaseViewCount(@Path("id") Long recipeId);
}