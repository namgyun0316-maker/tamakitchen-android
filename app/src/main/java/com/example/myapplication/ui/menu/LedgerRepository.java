package com.namgyun.tamakitchen.ui.menu;

import com.namgyun.tamakitchen.network.LedgerApi;
import com.namgyun.tamakitchen.network.RetrofitClient;

import java.util.List;

import retrofit2.Callback;

public class LedgerRepository {

    private final LedgerApi api;

    public LedgerRepository() {
        this.api = RetrofitClient.getLedgerApi();
    }

    public void getEntries(Long userId, int year, int month, Callback<List<LedgerEntryDto>> callback) {
        api.getEntries(userId, year, month).enqueue(callback);
    }

    public void addEntry(LedgerEntryDto request, Callback<LedgerEntryDto> callback) {
        api.addEntry(request).enqueue(callback);
    }

    public void updateEntry(Long entryId, LedgerEntryDto request, Callback<LedgerEntryDto> callback) {
        api.updateEntry(entryId, request).enqueue(callback);
    }

    public void deleteEntry(Long entryId, Callback<Void> callback) {
        api.deleteEntry(entryId).enqueue(callback);
    }

    public void addEntriesFromShopping(LedgerFromShoppingRequest request, Callback<List<LedgerEntryDto>> callback) {
        api.addEntriesFromShopping(request).enqueue(callback);
    }
}