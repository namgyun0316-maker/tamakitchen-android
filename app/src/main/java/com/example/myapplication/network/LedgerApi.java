package com.namgyun.tamakitchen.network;

import com.namgyun.tamakitchen.ui.menu.LedgerEntryDto;
import com.namgyun.tamakitchen.ui.menu.LedgerFromShoppingRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LedgerApi {

    @GET("ledger/{userId}")
    Call<List<LedgerEntryDto>> getEntries(
            @Path("userId") Long userId,
            @Query("year") int year,
            @Query("month") int month
    );

    @POST("ledger")
    Call<LedgerEntryDto> addEntry(@Body LedgerEntryDto request);

    @PUT("ledger/{entryId}")
    Call<LedgerEntryDto> updateEntry(
            @Path("entryId") Long entryId,
            @Body LedgerEntryDto request
    );

    @DELETE("ledger/{entryId}")
    Call<Void> deleteEntry(@Path("entryId") Long entryId);

    @POST("ledger/from-shopping")
    Call<List<LedgerEntryDto>> addEntriesFromShopping(@Body LedgerFromShoppingRequest request);
}