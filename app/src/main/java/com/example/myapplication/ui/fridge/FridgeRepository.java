package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.shopping.IconCatalog;
import com.namgyun.tamakitchen.ui.shopping.IconItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ✅ 데이터 소스 통합: 로컬(게스트), 개인 서버, 공동 서버
 * Fragment는 "repository에 요청"만 하고, 결과를 받아 UI만 업데이트.
 */
public class FridgeRepository {

    private static final String TAG = "FridgeRepository";
    private static final boolean AUTO_FIX_ICONKEY_ON_LOAD = true;

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final FridgeApiService api;

    public FridgeRepository() {
        api = FridgeApi.getClient().create(FridgeApiService.class);
    }

    // =====================
    // 로컬(게스트)
    // =====================
    public void loadLocal(Context ctx, DataCallback<List<FridgeItem>> cb) {
        try {
            List<FridgeItem> local = GuestFridgeStore.load(ctx);
            cb.onSuccess(local == null ? new ArrayList<>() : local);
        } catch (Exception e) {
            Log.e(TAG, "loadLocal failed", e);
            cb.onError("냉장고 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    public void saveLocal(Context ctx, List<FridgeItem> items) {
        GuestFridgeStore.save(ctx, items);
    }

    // =====================
    // 개인 서버
    // =====================
    public void loadPersonal(long userId, DataCallback<List<FridgeItem>> cb) {
        api.getFridgeItems(userId).enqueue(new Callback<List<FridgeItem>>() {
            @Override
            public void onResponse(Call<List<FridgeItem>> call, Response<List<FridgeItem>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    List<FridgeItem> items = res.body();
                    autoFixIconKeysPersonal(items);
                    cb.onSuccess(items);
                } else {
                    Log.e(TAG, "loadPersonal failed. code=" + res.code());
                    cb.onError("냉장고 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<List<FridgeItem>> call, Throwable t) {
                Log.e(TAG, "loadPersonal network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public void addPersonal(long userId, FridgeItem item, DataCallback<Void> cb) {
        api.addFridgeItem(userId, item).enqueue(new Callback<FridgeItem>() {
            @Override
            public void onResponse(Call<FridgeItem> call, Response<FridgeItem> res) {
                if (res.isSuccessful()) {
                    cb.onSuccess(null);
                } else {
                    Log.e(TAG, "addPersonal failed. code=" + res.code());
                    cb.onError("냉장고에 추가하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<FridgeItem> call, Throwable t) {
                Log.e(TAG, "addPersonal network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public void updatePersonal(FridgeItem item, DataCallback<Void> cb) {
        if (item == null || item.getId() == null) {
            cb.onError("수정할 항목을 찾지 못했어요.");
            return;
        }

        fixIconKeyBeforeUpdate(item);

        api.updateFridgeItem(item.getId(), null, item).enqueue(new Callback<FridgeItem>() {
            @Override
            public void onResponse(Call<FridgeItem> call, Response<FridgeItem> res) {
                if (res.isSuccessful()) {
                    cb.onSuccess(null);
                } else {
                    Log.e(TAG, "updatePersonal failed. code=" + res.code());
                    cb.onError("냉장고 항목을 수정하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<FridgeItem> call, Throwable t) {
                Log.e(TAG, "updatePersonal network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public void deletePersonal(FridgeItem item, DataCallback<Void> cb) {
        if (item == null || item.getId() == null) {
            cb.onSuccess(null);
            return;
        }

        api.deleteFridgeItem(item.getId(), null).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> res) {
                if (res.isSuccessful()) {
                    cb.onSuccess(null);
                } else {
                    Log.e(TAG, "deletePersonal failed. code=" + res.code());
                    cb.onError("냉장고 항목을 삭제하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "deletePersonal network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    // =====================
    // 공동 서버
    // =====================
    public void loadShared(long fridgeId, long userId, DataCallback<List<FridgeItem>> cb) {
        api.getSharedFridgeItems(fridgeId, userId).enqueue(new Callback<List<FridgeItem>>() {
            @Override
            public void onResponse(Call<List<FridgeItem>> call, Response<List<FridgeItem>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    List<FridgeItem> items = res.body();
                    autoFixIconKeysShared(items, userId);
                    cb.onSuccess(items);
                } else {
                    Log.e(TAG, "loadShared failed. code=" + res.code());
                    cb.onError("공동 냉장고 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<List<FridgeItem>> call, Throwable t) {
                Log.e(TAG, "loadShared network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public void addShared(long fridgeId, long userId, FridgeItem item, DataCallback<Void> cb) {
        api.addSharedFridgeItem(fridgeId, userId, item).enqueue(new Callback<FridgeItem>() {
            @Override
            public void onResponse(Call<FridgeItem> call, Response<FridgeItem> res) {
                if (res.isSuccessful()) {
                    cb.onSuccess(null);
                } else {
                    Log.e(TAG, "addShared failed. code=" + res.code());
                    cb.onError("공동 냉장고에 추가하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<FridgeItem> call, Throwable t) {
                Log.e(TAG, "addShared network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public void updateShared(long userId, FridgeItem item, DataCallback<Void> cb) {
        if (item == null || item.getId() == null) {
            cb.onError("수정할 항목을 찾지 못했어요.");
            return;
        }

        fixIconKeyBeforeUpdate(item);

        api.updateFridgeItem(item.getId(), userId, item).enqueue(new Callback<FridgeItem>() {
            @Override
            public void onResponse(Call<FridgeItem> call, Response<FridgeItem> res) {
                if (res.isSuccessful()) {
                    cb.onSuccess(null);
                } else {
                    Log.e(TAG, "updateShared failed. code=" + res.code());
                    cb.onError("공동 냉장고 항목을 수정하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<FridgeItem> call, Throwable t) {
                Log.e(TAG, "updateShared network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public void deleteShared(long userId, FridgeItem item, DataCallback<Void> cb) {
        if (item == null || item.getId() == null) {
            cb.onSuccess(null);
            return;
        }

        api.deleteFridgeItem(item.getId(), userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> res) {
                if (res.isSuccessful()) {
                    cb.onSuccess(null);
                } else {
                    Log.e(TAG, "deleteShared failed. code=" + res.code());
                    cb.onError("공동 냉장고 항목을 삭제하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "deleteShared network failed", t);
                cb.onError(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    // =====================
    // IconKey 보정 로직
    // =====================
    private void autoFixIconKeysPersonal(List<FridgeItem> items) {
        if (!AUTO_FIX_ICONKEY_ON_LOAD || items == null) return;

        for (FridgeItem it : items) {
            if (it == null || it.getId() == null) continue;

            String correctKey = findRawKeyByName(it.getName());
            String current = it.getIconName() == null ? "" : it.getIconName().trim();

            if (!TextUtils.isEmpty(correctKey) && !correctKey.equals(current)) {
                it.setIconName(correctKey);

                api.updateFridgeItem(it.getId(), null, it).enqueue(new Callback<FridgeItem>() {
                    @Override
                    public void onResponse(Call<FridgeItem> call, Response<FridgeItem> res) {
                        Log.d(TAG, "iconName auto-fix personal id=" + it.getId()
                                + " [" + current + " -> " + correctKey + "] code=" + res.code());
                    }

                    @Override
                    public void onFailure(Call<FridgeItem> call, Throwable t) {
                        Log.w(TAG, "iconName auto-fix personal failed id=" + it.getId(), t);
                    }
                });
            }
        }
    }

    private void autoFixIconKeysShared(List<FridgeItem> items, long userId) {
        if (!AUTO_FIX_ICONKEY_ON_LOAD || items == null) return;

        for (FridgeItem it : items) {
            if (it == null || it.getId() == null) continue;

            String correctKey = findRawKeyByName(it.getName());
            String current = it.getIconName() == null ? "" : it.getIconName().trim();

            if (!TextUtils.isEmpty(correctKey) && !correctKey.equals(current)) {
                it.setIconName(correctKey);

                api.updateFridgeItem(it.getId(), userId, it).enqueue(new Callback<FridgeItem>() {
                    @Override
                    public void onResponse(Call<FridgeItem> call, Response<FridgeItem> res) {
                        Log.d(TAG, "iconName auto-fix shared id=" + it.getId()
                                + " [" + current + " -> " + correctKey + "] code=" + res.code());
                    }

                    @Override
                    public void onFailure(Call<FridgeItem> call, Throwable t) {
                        Log.w(TAG, "iconName auto-fix shared failed id=" + it.getId(), t);
                    }
                });
            }
        }
    }

    private void fixIconKeyBeforeUpdate(FridgeItem item) {
        if (item == null) return;

        String current = item.getIconName() == null ? "" : item.getIconName().trim();
        String correctKey = findRawKeyByName(item.getName());

        if (!TextUtils.isEmpty(correctKey) && !correctKey.equals(current)) {
            item.setIconName(correctKey);
        }
    }

    private String findRawKeyByName(String name) {
        if (TextUtils.isEmpty(name)) return "";

        List<IconItem> all = IconCatalog.getAllIcons();

        for (IconItem icon : all) {
            if (icon == null) continue;

            if (name.trim().equals(icon.getName())) {
                String key = icon.getRawKey();
                return key == null ? "" : key.trim();
            }
        }

        return "";
    }
}