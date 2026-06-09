package com.namgyun.tamakitchen.ui.recipe;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RecipeStepResponse implements Serializable {

    @SerializedName(value = "id", alternate = {"stepId"})
    private Long id;

    @SerializedName(value = "recipeId", alternate = {"recipe_id"})
    private Long recipeId;

    @SerializedName(value = "stepNumber", alternate = {"step_number", "stepNo"})
    private Integer stepNumber;

    // description / text / content 모두 대응
    @SerializedName(value = "description", alternate = {"text", "content"})
    private String description;

    // 메인 이미지
    @SerializedName(value = "imageUrl", alternate = {"image_url", "image"})
    private String imageUrl;

    // ✅🔥 추가: 서버 썸네일 URL 직접 받기
    @SerializedName(value = "thumbnailUrl", alternate = {"thumbnail_url", "thumbUrl"})
    private String thumbnailUrl;

    public RecipeStepResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // ✅ 추가 getter/setter
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}