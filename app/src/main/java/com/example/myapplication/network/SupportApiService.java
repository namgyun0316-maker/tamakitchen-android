package com.namgyun.tamakitchen.network;

import com.namgyun.tamakitchen.ui.menu.InquiryRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SupportApiService {

    // ✅ 문의 전송 (서버가 이메일 발송)
    @POST("api/support/inquiry")
    Call<Void> sendInquiry(@Body InquiryRequest request);
}
