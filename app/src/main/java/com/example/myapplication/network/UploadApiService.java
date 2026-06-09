package com.namgyun.tamakitchen.network;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadApiService {

    @Multipart
    @POST("/api/upload/image")
    Call<ImageUploadResponse> uploadImage(
            @Part MultipartBody.Part file
    );
}
