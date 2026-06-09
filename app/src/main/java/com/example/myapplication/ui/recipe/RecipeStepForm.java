package com.namgyun.tamakitchen.ui.recipe;

import android.content.Context;
import android.text.TextUtils;

public class RecipeStepForm {

    private String text;
    private String imageUrl;
    private String thumbnailUrl;

    public RecipeStepForm() {
        this("", "", "");
    }

    public RecipeStepForm(String text, String imageUrl) {
        this(text, imageUrl, "");
    }

    public RecipeStepForm(String text, String imageUrl, String thumbnailUrl) {
        this.text = (text == null) ? "" : text;
        this.imageUrl = (imageUrl == null) ? "" : imageUrl;
        this.thumbnailUrl = (thumbnailUrl == null) ? "" : thumbnailUrl;
    }

    public RecipeStepForm(Context context, String text, int imageResId) {
        this.text = (text == null) ? "" : text;
        this.imageUrl = buildAndroidResourceUri(context, imageResId);
        this.thumbnailUrl = "";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = (text == null) ? "" : text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = (imageUrl == null) ? "" : imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = (thumbnailUrl == null) ? "" : thumbnailUrl;
    }

    public void setImageResId(Context context, int imageResId) {
        this.imageUrl = buildAndroidResourceUri(context, imageResId);
        this.thumbnailUrl = "";
    }

    public boolean hasImage() {
        return !TextUtils.isEmpty(imageUrl);
    }

    public boolean hasThumbnail() {
        return !TextUtils.isEmpty(thumbnailUrl);
    }

    public boolean isRemoteImage() {
        return startsWithHttp(imageUrl);
    }

    public boolean isLocalImage() {
        if (TextUtils.isEmpty(imageUrl)) return false;

        return imageUrl.startsWith("content://")
                || imageUrl.startsWith("file://")
                || imageUrl.startsWith("android.resource://");
    }

    public boolean needsUpload() {
        return hasImage() && !isRemoteImage();
    }

    public void applyUploadedImage(String uploadedImageUrl, String uploadedThumbnailUrl) {
        this.imageUrl = (uploadedImageUrl == null) ? "" : uploadedImageUrl;
        this.thumbnailUrl = (uploadedThumbnailUrl == null) ? "" : uploadedThumbnailUrl;
    }

    private boolean startsWithHttp(String value) {
        if (value == null) return false;
        String s = value.trim();
        return s.startsWith("http://") || s.startsWith("https://");
    }

    private String buildAndroidResourceUri(Context context, int resId) {
        if (context == null || resId == 0) return "";
        return "android.resource://" + context.getPackageName() + "/" + resId;
    }
}