// File: app/src/main/java/com/example/myapplication/ui/shopping/ShoppingRepository.java
package com.namgyun.tamakitchen.ui.shopping;

import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.network.ShoppingApi;
import com.namgyun.tamakitchen.network.ShoppingItemRequest;

import java.util.List;

import retrofit2.Callback;

public class ShoppingRepository {

    private final ShoppingApi api;

    public ShoppingRepository() {
        api = RetrofitClient.getShoppingApi();
    }

    public void getItems(Long userId, Callback<List<ShoppingItem>> cb) {
        api.getItems(userId).enqueue(cb);
    }

    public void addItem(ShoppingItemRequest req, Callback<ShoppingItem> cb) {
        api.addItem(req).enqueue(cb);
    }

    public void updateItem(Long id, ShoppingItemRequest req, Callback<ShoppingItem> cb) {
        api.updateItem(id, req).enqueue(cb);
    }

    public void deleteItem(Long id, Callback<Void> cb) {
        api.deleteItem(id).enqueue(cb);
    }
}
