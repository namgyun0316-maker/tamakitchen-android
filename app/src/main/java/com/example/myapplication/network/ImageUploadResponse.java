package com.namgyun.tamakitchen.network;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ImageUploadResponse implements Serializable {

    // ✅ 서버랑 맞춤 (핵심 수정)
    @SerializedName("imageUrl")
    private String url;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;

    public String getUrl() {
        return url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}