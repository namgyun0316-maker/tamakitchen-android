// File: app/src/main/java/com/example/myapplication/ui/shopping/ReceiptKnownItemsRepository.java
package com.namgyun.tamakitchen.ui.shopping;

import androidx.annotation.NonNull;

import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.ui.fridge.FridgeItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReceiptKnownItemsRepository {

    public interface CallbackResult {
        void onSuccess(@NonNull ArrayList<String> names);
        void onFail(@NonNull Throwable t);
    }

    private final FridgeApiService api;

    public ReceiptKnownItemsRepository() {
        // ✅ RetrofitClient에는 getClient()가 없고, getFridgeApi()가 있음
        api = RetrofitClient.getFridgeApi();
    }

    public void fetchKnownNamesFromServer(long userId, @NonNull CallbackResult cb) {
        api.getFridgeItems(userId).enqueue(new Callback<List<FridgeItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<FridgeItem>> call,
                                   @NonNull Response<List<FridgeItem>> response) {
                try {
                    ArrayList<String> out = new ArrayList<>();

                    if (response.isSuccessful() && response.body() != null) {
                        Set<String> set = new LinkedHashSet<>();
                        for (FridgeItem it : response.body()) {
                            if (it == null) continue;
                            String name = it.getName(); // ✅ FridgeItem.name
                            if (name == null) continue;
                            name = name.trim();
                            if (!name.isEmpty()) set.add(name);
                        }
                        out.addAll(set);
                    }

                    cb.onSuccess(out);
                } catch (Exception e) {
                    cb.onFail(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FridgeItem>> call, @NonNull Throwable t) {
                cb.onFail(t);
            }
        });
    }
}