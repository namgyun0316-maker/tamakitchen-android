package com.namgyun.tamakitchen.ui.recipe;

public class RatingRequest {
    private float rating;
    private Long userId;

    public RatingRequest(float rating, Long userId) {
        this.rating = rating;
        this.userId = userId;
    }

    public float getRating() {
        return rating;
    }

    public Long getUserId() {
        return userId;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
