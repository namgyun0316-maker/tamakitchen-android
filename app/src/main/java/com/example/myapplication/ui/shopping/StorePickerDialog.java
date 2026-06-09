package com.namgyun.tamakitchen.ui.shopping;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.network.ShoppingApi;
import com.namgyun.tamakitchen.network.StoreCreateRequest;
import com.namgyun.tamakitchen.network.StoreResponse;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.common.ToastUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StorePickerDialog extends DialogFragment {

    public interface OnStorePickedListener {
        void onPicked(@Nullable Long storeId, @NonNull String storeName);
    }

    private static final String PREF_GUEST_STORE_CACHE = "guest_store_picker_cache";
    private static final String KEY_GUEST_STORE_LINES = "guest_store_lines";

    private final Long userId;
    private final OnStorePickedListener listener;

    private ShoppingApi api;

    private EditText etSearch;
    private RecyclerView rvStores;
    private TextView btnClose;
    private TextView btnAddNew;

    private final List<StoreResponse> serverStores = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    private StoreRvAdapter adapter;

    static class Row {
        @Nullable Long id;
        @NonNull String name;

        Row(@Nullable Long id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }
    }

    public StorePickerDialog(Long userId, OnStorePickedListener listener) {
        this.userId = userId;
        this.listener = listener;
    }

    private boolean isGuestMode() {
        if (getContext() == null) return true;
        return AuthPrefs.isGuest(requireContext()) || userId == null || userId <= 0L;
    }

    private final TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String q = s == null ? "" : s.toString().trim();

            if (isGuestMode()) {
                if (q.isEmpty()) {
                    rebuildRowsForGuest(loadGuestStores());
                } else {
                    searchGuest(q);
                }
            } else {
                if (q.isEmpty()) {
                    loadRecentFromServer();
                } else {
                    searchFromServer(q);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getShoppingApi();

        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_store_picker_pretty, null);

        etSearch = root.findViewById(R.id.etSearch);
        rvStores = root.findViewById(R.id.rvStores);
        btnClose = root.findViewById(R.id.btnClose);
        btnAddNew = root.findViewById(R.id.btnAddNew);

        rvStores.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStores.setHasFixedSize(false);
        rvStores.setNestedScrollingEnabled(true);
        rvStores.setItemAnimator(null);

        adapter = new StoreRvAdapter(rows, new StoreRvAdapter.OnRowClick() {
            @Override
            public void onPick(@Nullable Long storeId, @NonNull String storeName) {
                if (listener != null) listener.onPicked(storeId, storeName);
                if (getContext() != null) {
                    StoreSessionManager.setCurrentStore(getContext(), storeId, storeName);
                }
                dismiss();
            }

            @Override
            public void onDelete(@NonNull Row row) {
                if (row.id == null) return;
                showDeleteConfirmAndDelete(row);
            }
        });
        rvStores.setAdapter(adapter);

        etSearch.addTextChangedListener(searchWatcher);

        btnClose.setOnClickListener(v -> dismiss());

        btnAddNew.setOnClickListener(v -> {
            String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
            if (TextUtils.isEmpty(q)) {
                ToastUtil.showIosToast(requireContext(), "판매점 이름을 입력해 주세요.");
                return;
            }

            if (isGuestMode()) {
                createGuestStore(q);
            } else {
                createStoreOnServer(q);
            }
        });

        if (isGuestMode()) {
            rebuildRowsForGuest(loadGuestStores());
        } else {
            loadRecentFromServer();
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        return dialog;
    }

    // =========================================================
    // 공통 rows 구성
    // =========================================================
    private void rebuildRowsFromMap(@NonNull LinkedHashMap<String, Row> merged) {
        rows.clear();
        rows.add(new Row(null, "미지정"));

        Long currentId = null;
        String currentName = "";
        if (getContext() != null) {
            currentId = StoreSessionManager.getCurrentStoreId(getContext());
            currentName = StoreSessionManager.getCurrentStoreName(getContext());
        }

        if (!TextUtils.isEmpty(currentName)) {
            String currentKey = buildStoreKey(currentId, currentName);
            if (!merged.containsKey(currentKey) && !"미지정".equals(currentName)) {
                merged.put(currentKey, new Row(currentId, safeName(currentName)));
            }
        }

        for (Map.Entry<String, Row> e : merged.entrySet()) {
            Row row = e.getValue();
            if (row == null) continue;
            if ("미지정".equals(safeName(row.name)) && row.id == null) continue;
            rows.add(new Row(row.id, safeName(row.name)));
        }

        if (!TextUtils.isEmpty(currentName) && !"미지정".equals(currentName)) {
            String currentKey = buildStoreKey(currentId, currentName);

            int idx = -1;
            for (int i = 1; i < rows.size(); i++) {
                Row r = rows.get(i);
                String key = buildStoreKey(r.id, r.name);
                if (currentKey.equals(key)) {
                    idx = i;
                    break;
                }
            }

            if (idx > 1) {
                Row picked = rows.remove(idx);
                rows.add(1, picked);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // =========================================================
    // 로그인 사용자: 서버 전용
    // =========================================================
    private void rebuildRowsForServer(@Nullable List<StoreResponse> sourceList) {
        serverStores.clear();
        if (sourceList != null) serverStores.addAll(sourceList);

        LinkedHashMap<String, Row> merged = new LinkedHashMap<>();

        for (StoreResponse s : serverStores) {
            if (s == null) continue;
            Long id = s.getId();
            String name = safeName(s.getName());
            String key = buildStoreKey(id, name);

            if (!merged.containsKey(key)) {
                merged.put(key, new Row(id, name));
            }
        }

        rebuildRowsFromMap(merged);
    }

    private void loadRecentFromServer() {
        if (api == null || userId == null) {
            rebuildRowsForServer(new ArrayList<>());
            return;
        }

        api.getRecentStores(userId).enqueue(new Callback<List<StoreResponse>>() {
            @Override
            public void onResponse(Call<List<StoreResponse>> call, Response<List<StoreResponse>> response) {
                if (!isAdded()) return;
                rebuildRowsForServer(response.isSuccessful() ? response.body() : new ArrayList<>());
            }

            @Override
            public void onFailure(Call<List<StoreResponse>> call, Throwable t) {
                if (!isAdded()) return;
                rebuildRowsForServer(new ArrayList<>());
            }
        });
    }

    private void searchFromServer(@NonNull String query) {
        if (api == null || userId == null) {
            rebuildRowsForServer(new ArrayList<>());
            return;
        }

        api.searchStores(userId, query).enqueue(new Callback<List<StoreResponse>>() {
            @Override
            public void onResponse(Call<List<StoreResponse>> call, Response<List<StoreResponse>> response) {
                if (!isAdded()) return;
                rebuildRowsForServer(response.isSuccessful() ? response.body() : new ArrayList<>());
            }

            @Override
            public void onFailure(Call<List<StoreResponse>> call, Throwable t) {
                if (!isAdded()) return;
                rebuildRowsForServer(new ArrayList<>());
            }
        });
    }

    private void createStoreOnServer(@NonNull String name) {
        if (api == null || userId == null) return;

        final String safe = safeName(name);

        for (StoreResponse s : serverStores) {
            if (s == null) continue;
            if (safe.equalsIgnoreCase(safeName(s.getName()))) {
                Long id = s.getId();
                String n = safeName(s.getName());

                if (listener != null) listener.onPicked(id, n);
                if (getContext() != null) {
                    StoreSessionManager.setCurrentStore(getContext(), id, n);
                }

                clearSearchBox();

                // ✅ 검색창 비운 뒤 전체 목록 즉시 갱신
                loadRecentFromServer();

                ToastUtil.showIosToast(requireContext(), "이미 있는 판매점입니다.");
                return;
            }
        }

        api.createStore(new StoreCreateRequest(userId, safe)).enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    StoreResponse s = response.body();
                    Long id = s.getId();
                    String n = safeName(s.getName());

                    if (listener != null) listener.onPicked(id, n);
                    if (getContext() != null) {
                        StoreSessionManager.setCurrentStore(getContext(), id, n);
                    }

                    clearSearchBox();

                    // ✅ 일단 즉시 현재 화면에 새 판매점 반영
                    upsertServerStore(id, n);
                    rebuildRowsForServer(new ArrayList<>(serverStores));

                    // ✅ 서버에서 기존 판매점 + 새 판매점 다시 받아와 최종 동기화
                    loadRecentFromServer();

                    ToastUtil.showIosToast(requireContext(), "판매점이 추가되었습니다.");
                } else {
                    ToastUtil.showIosToast(requireContext(), "판매점 추가 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                if (!isAdded()) return;
                ToastUtil.showIosToast(requireContext(), "판매점 추가 실패");
            }
        });
    }

    private void upsertServerStore(@Nullable Long id, @NonNull String name) {
        String targetKey = buildStoreKey(id, name);

        for (int i = 0; i < serverStores.size(); i++) {
            StoreResponse old = serverStores.get(i);
            if (old == null) continue;

            String oldKey = buildStoreKey(old.getId(), old.getName());
            if (targetKey.equals(oldKey)) {
                serverStores.remove(i);
                break;
            }
        }

        StoreResponse added = new StoreResponse();
        added.setId(id);
        added.setName(name);
        serverStores.add(0, added);
    }

    private void deleteStoreOnServer(@NonNull Row row) {
        if (api == null || row.id == null || userId == null) return;

        api.deleteStore(row.id, userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    if (getContext() != null) {
                        Long cur = StoreSessionManager.getCurrentStoreId(getContext());
                        if (cur != null && cur.equals(row.id)) {
                            StoreSessionManager.setCurrentStore(getContext(), null, "미지정");
                        }
                    }

                    removeServerStore(row);
                    rebuildRowsForServer(new ArrayList<>(serverStores));
                    ToastUtil.showIosToast(requireContext(), "삭제 완료");
                } else {
                    ToastUtil.showIosToast(requireContext(), "삭제 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;
                ToastUtil.showIosToast(requireContext(), "삭제 실패");
            }
        });
    }

    private void removeServerStore(@NonNull Row row) {
        String deleteKey = buildStoreKey(row.id, row.name);

        for (int i = serverStores.size() - 1; i >= 0; i--) {
            StoreResponse s = serverStores.get(i);
            if (s == null) continue;

            String key = buildStoreKey(s.getId(), s.getName());
            if (deleteKey.equals(key)) {
                serverStores.remove(i);
            }
        }
    }

    // =========================================================
    // 게스트: 로컬 전용
    // =========================================================
    @NonNull
    private List<Row> loadGuestStores() {
        List<Row> out = new ArrayList<>();
        if (getContext() == null) return out;

        SharedPreferences sp = requireContext().getSharedPreferences(PREF_GUEST_STORE_CACHE, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_GUEST_STORE_LINES, "");
        if (TextUtils.isEmpty(raw)) return out;

        String[] lines = raw.split("\n");
        for (String line : lines) {
            if (TextUtils.isEmpty(line)) continue;

            String[] parts = line.split("\t", 2);
            String idText = parts.length > 0 ? parts[0].trim() : "";
            String name = parts.length > 1 ? safeName(parts[1]) : "";

            Long id = null;
            if (!TextUtils.isEmpty(idText)) {
                try {
                    id = Long.parseLong(idText);
                } catch (Exception ignored) {
                }
            }

            if (!TextUtils.isEmpty(name) && !"미지정".equals(name)) {
                out.add(new Row(id, name));
            }
        }
        return out;
    }

    private void persistGuestStores(@NonNull List<Row> list) {
        if (getContext() == null) return;

        LinkedHashMap<String, Row> unique = new LinkedHashMap<>();
        for (Row row : list) {
            if (row == null) continue;
            String name = safeName(row.name);
            if (TextUtils.isEmpty(name) || "미지정".equals(name)) continue;

            String key = buildStoreKey(row.id, name);
            if (!unique.containsKey(key)) {
                unique.put(key, new Row(row.id, name));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Row> e : unique.entrySet()) {
            Row row = e.getValue();
            if (sb.length() > 0) sb.append("\n");

            String idPart = row.id == null ? "" : String.valueOf(row.id);
            sb.append(idPart).append("\t").append(row.name);
        }

        SharedPreferences sp = requireContext().getSharedPreferences(PREF_GUEST_STORE_CACHE, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_GUEST_STORE_LINES, sb.toString()).apply();
    }

    private void rebuildRowsForGuest(@NonNull List<Row> guestList) {
        LinkedHashMap<String, Row> merged = new LinkedHashMap<>();

        for (Row row : guestList) {
            if (row == null) continue;
            String key = buildStoreKey(row.id, row.name);
            if (!merged.containsKey(key)) {
                merged.put(key, new Row(row.id, safeName(row.name)));
            }
        }

        rebuildRowsFromMap(merged);
    }

    private void searchGuest(@NonNull String query) {
        List<Row> all = loadGuestStores();
        List<Row> filtered = new ArrayList<>();

        String q = query.trim();
        for (Row row : all) {
            if (row == null) continue;
            if (safeName(row.name).contains(q)) {
                filtered.add(row);
            }
        }

        rebuildRowsForGuest(filtered);
    }

    private void createGuestStore(@NonNull String name) {
        String safe = safeName(name);

        List<Row> existing = loadGuestStores();
        for (Row row : existing) {
            if (safe.equalsIgnoreCase(safeName(row.name))) {
                if (listener != null) listener.onPicked(row.id, row.name);
                if (getContext() != null) {
                    StoreSessionManager.setCurrentStore(getContext(), row.id, row.name);
                }
                clearSearchBox();
                ToastUtil.showIosToast(requireContext(), "이미 있는 판매점입니다.");
                return;
            }
        }

        long newId = System.currentTimeMillis();
        existing.add(0, new Row(newId, safe));
        persistGuestStores(existing);

        if (listener != null) listener.onPicked(newId, safe);
        if (getContext() != null) {
            StoreSessionManager.setCurrentStore(getContext(), newId, safe);
        }

        clearSearchBox();
        rebuildRowsForGuest(loadGuestStores());
        ToastUtil.showIosToast(requireContext(), "판매점이 추가되었습니다.");
    }

    private void deleteGuestStore(@NonNull Row row) {
        List<Row> existing = loadGuestStores();
        List<Row> filtered = new ArrayList<>();

        String deleteKey = buildStoreKey(row.id, row.name);
        for (Row item : existing) {
            String itemKey = buildStoreKey(item.id, item.name);
            if (deleteKey.equals(itemKey)) continue;
            filtered.add(item);
        }

        persistGuestStores(filtered);

        if (getContext() != null) {
            Long cur = StoreSessionManager.getCurrentStoreId(getContext());
            String curName = StoreSessionManager.getCurrentStoreName(getContext());
            String currentKey = buildStoreKey(cur, curName);

            if (currentKey.equals(deleteKey)) {
                StoreSessionManager.setCurrentStore(getContext(), null, "미지정");
            }
        }

        rebuildRowsForGuest(loadGuestStores());
        ToastUtil.showIosToast(requireContext(), "삭제 완료");
    }

    // =========================================================
    // 삭제 공통
    // =========================================================
    private void showDeleteConfirmAndDelete(@NonNull Row row) {
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_store, null);

        TextView tvItemName = dv.findViewById(R.id.tvItemName);
        View btnCancel = dv.findViewById(R.id.btnCancel);
        View btnDelete = dv.findViewById(R.id.btnDelete);

        if (tvItemName == null || btnCancel == null || btnDelete == null) {
            ToastUtil.showIosToast(requireContext(), "삭제 다이얼로그 레이아웃 id 확인 필요");
            return;
        }

        tvItemName.setText("'" + row.name + "'");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dv)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();

            if (isGuestMode()) {
                deleteGuestStore(row);
            } else {
                deleteStoreOnServer(row);
            }
        });

        dialog.show();
    }

    private void clearSearchBox() {
        if (etSearch == null) return;

        etSearch.removeTextChangedListener(searchWatcher);
        etSearch.setText("");
        etSearch.clearFocus();
        etSearch.setHint("판매점 검색 (없으면 새로 추가)");
        etSearch.addTextChangedListener(searchWatcher);
    }

    @NonNull
    private String buildStoreKey(@Nullable Long id, @Nullable String name) {
        String safeName = safeName(name);
        if (id != null) return "id:" + id;
        return "name:" + safeName.toLowerCase();
    }

    @NonNull
    private String safeName(@Nullable String s) {
        if (s == null) return "미지정";
        String t = s.trim();
        return t.isEmpty() ? "미지정" : t;
    }

    static class StoreRvAdapter extends RecyclerView.Adapter<StoreRvAdapter.VH> {

        interface OnRowClick {
            void onPick(@Nullable Long storeId, @NonNull String storeName);
            void onDelete(@NonNull Row row);
        }

        private final List<Row> rows;
        private final OnRowClick cb;

        StoreRvAdapter(List<Row> rows, OnRowClick cb) {
            this.rows = rows == null ? new ArrayList<>() : rows;
            this.cb = cb;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_store_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Row r = rows.get(position);
            holder.tvName.setText(r.name);

            boolean deletable = r.id != null && !"미지정".equals(r.name);

            if (holder.btnX != null) {
                holder.btnX.setVisibility(deletable ? View.VISIBLE : View.GONE);
                holder.btnX.setOnClickListener(v -> {
                    if (!deletable) return;
                    if (cb != null) cb.onDelete(r);
                });
            }

            holder.itemView.setOnClickListener(v -> {
                if (cb != null) cb.onPick(r.id, r.name);
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            View btnX;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvStoreName);
                btnX = itemView.findViewById(R.id.btnDeleteCircle);
            }
        }
    }
}