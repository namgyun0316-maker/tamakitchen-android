package com.namgyun.tamakitchen.ui.shopping;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.network.FridgeItemCreateRequest;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.fridge.FridgeItem;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.namgyun.tamakitchen.ui.fridge.GuestFridgeStore;
import com.namgyun.tamakitchen.ui.menu.LedgerEntry;
import com.namgyun.tamakitchen.ui.menu.LedgerEntryDto;
import com.namgyun.tamakitchen.ui.menu.LedgerFromShoppingRequest;
import com.namgyun.tamakitchen.ui.menu.LedgerLocalStore;
import com.namgyun.tamakitchen.ui.menu.LedgerRepository;
import com.namgyun.tamakitchen.ui.menu.LedgerShoppingItemDto;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingCompleteDialog extends DialogFragment {

    private static final String TAG = "ShoppingCompleteDialog";

    public interface OnRegisteredListener {
        void onRegistered();
    }

    private static final String ARG_USER_ID = "arg_user_id";

    private ArrayList<ShoppingItem> items = new ArrayList<>();
    private long userId = 1L;

    private OnRegisteredListener listener;

    private RecyclerView rv;
    private ShoppingCompleteAdapter adapter;

    private View btnCancel;
    private View btnRegister;
    private View btnSelectAll;

    private FridgeApiService fridgeApi;
    private LedgerRepository ledgerRepository;

    private static final float WIDTH_RATIO = 0.95f;
    private static final float HEIGHT_RATIO = 0.78f;

    private final FridgeModeManager mode = new FridgeModeManager();

    public static ShoppingCompleteDialog newInstance(@NonNull ArrayList<ShoppingItem> items) {
        ShoppingCompleteDialog dialog = new ShoppingCompleteDialog();
        dialog.items = (items != null) ? items : new ArrayList<>();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, 1L);
        dialog.setArguments(b);
        return dialog;
    }

    public static ShoppingCompleteDialog newInstance(long userId, @NonNull ArrayList<ShoppingItem> items) {
        ShoppingCompleteDialog dialog = new ShoppingCompleteDialog();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        dialog.setArguments(b);
        dialog.items = (items != null) ? items : new ArrayList<>();
        return dialog;
    }

    public void setOnRegisteredListener(OnRegisteredListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        userId = (getArguments() != null) ? getArguments().getLong(ARG_USER_ID, 1L) : 1L;
        fridgeApi = RetrofitClient.getFridgeApi();
        ledgerRepository = new LedgerRepository();

        try {
            long uid = mode.getUserIdSafe(requireContext());
            if (uid > 0) userId = uid;
        } catch (Exception ignored) {
        }

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_shopping_complete, null);

        rv = v.findViewById(R.id.rv_complete_items);
        btnCancel = v.findViewById(R.id.btn_cancel);
        btnRegister = v.findViewById(R.id.btn_register_fridge);
        btnSelectAll = v.findViewById(R.id.btnSelectAll);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ShoppingCompleteAdapter(items);
        rv.setAdapter(adapter);

        if (adapter != null) {
            adapter.setOnSelectionChangedListener((selectedCount, totalCount, allSelected) -> {
                setSelectAllText(allSelected);
            });
        }

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(view -> {
                if (adapter == null) return;
                adapter.toggleSelectAll();
                setSelectAllText(adapter.isAllSelected());
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(view -> dismiss());
        }

        if (btnRegister != null) {
            btnRegister.setOnClickListener(view -> registerToFridgeByMode());
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(v);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) applyFixedSize(dialog);
    }

    private void setSelectAllText(boolean allSelected) {
        if (btnSelectAll == null) return;
        try {
            ((com.google.android.material.button.MaterialButton) btnSelectAll)
                    .setText(allSelected ? "전체해제" : "전체선택");
        } catch (Exception ignored) {
        }
    }

    private void applyFixedSize(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;

        DisplayMetrics dm = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = (int) (dm.widthPixels * WIDTH_RATIO);
        int height = (int) (dm.heightPixels * HEIGHT_RATIO);

        window.setLayout(width, height);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = 0.45f;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private void showToast(String message) {
        if (!isAdded()) return;
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;
        AppToast.show(activity, message);
    }

    private void registerToFridgeByMode() {
        if (adapter == null) {
            showToast("목록을 불러오는 중입니다.");
            return;
        }

        List<ShoppingItem> selected = adapter.getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showToast("냉장고에 넣을 재료를 체크해 주세요.");
            return;
        }

        if (mode.isGuestMode(requireContext())) {
            registerGuestLocal(selected);
            return;
        }

        if (userId <= 0) {
            showToast("로그인이 필요합니다.");
            return;
        }

        if (fridgeApi == null) {
            showToast("냉장고 등록을 준비하지 못했어요. 잠시 후 다시 시도해주세요.");
            return;
        }

        if (mode.isSharedFridgeMode(requireContext())) {
            long fridgeId = mode.getSharedFridgeIdSafe(requireContext());
            if (fridgeId <= 0) {
                mode.handleMissingSharedFridgeId(requireContext(), () -> {
                    showToast("개인 냉장고로 전환했습니다. 다시 시도해주세요.");
                    dismiss();
                });
                return;
            }
            registerServerShared(fridgeId, userId, selected);
        } else {
            registerServerPersonal(userId, selected);
        }
    }

    private void registerGuestLocal(@NonNull List<ShoppingItem> selected) {
        try {
            List<FridgeItem> local = GuestFridgeStore.load(requireContext());
            if (local == null) local = new ArrayList<>();

            int ok = 0;
            for (ShoppingItem it : selected) {
                if (it == null) continue;

                String name = safe(it.getName());
                if (TextUtils.isEmpty(name)) continue;

                double qty = it.getQuantity();
                if (qty <= 0d) qty = 1.0d;

                String unit = safeUnit(it.getUnit());
                String storage = "냉장";

                String iconKey = it.getIconKey();
                if (iconKey == null) iconKey = "";
                iconKey = iconKey.trim();

                int resId = 0;
                if (!TextUtils.isEmpty(iconKey)) {
                    resId = IconCatalog.findResIdByRawKey(iconKey);
                }
                if (resId == 0) {
                    IconItem icon = findIconByName(name);
                    if (icon != null) {
                        iconKey = (icon.getRawKey() == null) ? "" : icon.getRawKey().trim();
                        resId = icon.getResId();
                    }
                }

                FridgeItem fi = new FridgeItem();
                fi.setName(name);
                fi.setStorage(storage);
                fi.setQuantity(qty);
                fi.setUnit(unit);
                fi.setIconName(iconKey);
                fi.setIconResId(resId);

                String nick = OnboardingPrefs.getNickname(requireContext());
                if (TextUtils.isEmpty(nick)) nick = "게스트";
                fi.setAddedByNickname(nick);

                fi.setExpiryDate("");

                local.add(fi);
                ok++;
            }

            GuestFridgeStore.save(requireContext(), local);

            int ledgerAdded = LedgerLocalStore.addShoppingEntries(requireContext(), selected);

            showToast("게스트 냉장고 저장 " + ok + "개 · 가계부 등록 " + ledgerAdded + "건");

            if (listener != null) listener.onRegistered();
            dismiss();

        } catch (Exception e) {
            Log.e(TAG, "registerGuestLocal error", e);
            showToast("저장에 실패했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private void registerServerPersonal(long userId, @NonNull List<ShoppingItem> selected) {
        if (btnRegister != null) btnRegister.setEnabled(false);
        registerPersonalSequential(selected, 0, 0, 0, userId);
    }

    private void registerPersonalSequential(@NonNull List<ShoppingItem> selected,
                                            int index,
                                            int ok,
                                            int fail,
                                            long userId) {

        if (index >= selected.size()) {
            if (btnRegister != null) btnRegister.setEnabled(true);
            saveLedgerFromShoppingToServer(ok, fail, selected);
            return;
        }

        ShoppingItem it = selected.get(index);
        if (it == null || TextUtils.isEmpty(safe(it.getName()))) {
            registerPersonalSequential(selected, index + 1, ok, fail + 1, userId);
            return;
        }

        FridgeItemCreateRequest req = buildCreateRequestFromShoppingItem(it);

        fridgeApi.addPersonalItemReq(userId, req).enqueue(new Callback<FridgeItem>() {
            @Override
            public void onResponse(Call<FridgeItem> call, Response<FridgeItem> response) {
                if (response.isSuccessful()) {
                    registerPersonalSequential(selected, index + 1, ok + 1, fail, userId);
                } else {
                    Log.e(TAG, "personal fridge register failed. code=" + response.code());
                    registerPersonalSequential(selected, index + 1, ok, fail + 1, userId);
                }
            }

            @Override
            public void onFailure(Call<FridgeItem> call, Throwable t) {
                Log.e(TAG, "personal fridge register network failure", t);
                registerPersonalSequential(selected, index + 1, ok, fail + 1, userId);
            }
        });
    }

    private void registerServerShared(long fridgeId, long userId, @NonNull List<ShoppingItem> selected) {
        if (btnRegister != null) btnRegister.setEnabled(false);
        registerSharedSequential(selected, 0, 0, 0, fridgeId, userId);
    }

    private void registerSharedSequential(@NonNull List<ShoppingItem> selected,
                                          int index,
                                          int ok,
                                          int fail,
                                          long fridgeId,
                                          long userId) {

        if (index >= selected.size()) {
            if (btnRegister != null) btnRegister.setEnabled(true);
            saveLedgerFromShoppingToServer(ok, fail, selected);
            return;
        }

        ShoppingItem it = selected.get(index);
        if (it == null || TextUtils.isEmpty(safe(it.getName()))) {
            registerSharedSequential(selected, index + 1, ok, fail + 1, fridgeId, userId);
            return;
        }

        FridgeItemCreateRequest req = buildCreateRequestFromShoppingItem(it);

        fridgeApi.addSharedItemReq(fridgeId, userId, req).enqueue(new Callback<FridgeItem>() {
            @Override
            public void onResponse(Call<FridgeItem> call, Response<FridgeItem> response) {
                if (response.isSuccessful()) {
                    registerSharedSequential(selected, index + 1, ok + 1, fail, fridgeId, userId);
                } else {
                    Log.e(TAG, "shared fridge register failed. code=" + response.code());
                    registerSharedSequential(selected, index + 1, ok, fail + 1, fridgeId, userId);
                }
            }

            @Override
            public void onFailure(Call<FridgeItem> call, Throwable t) {
                Log.e(TAG, "shared fridge register network failure", t);
                registerSharedSequential(selected, index + 1, ok, fail + 1, fridgeId, userId);
            }
        });
    }

    private void saveLedgerFromShoppingToServer(int fridgeSuccessCount,
                                                int fridgeFailCount,
                                                @NonNull List<ShoppingItem> selected) {
        List<LedgerShoppingItemDto> dtoItems = new ArrayList<>();
        for (ShoppingItem item : selected) {
            if (item == null) continue;
            dtoItems.add(LedgerShoppingItemDto.fromShoppingItem(item));
        }

        LedgerFromShoppingRequest request = new LedgerFromShoppingRequest(userId, dtoItems);

        ledgerRepository.addEntriesFromShopping(request, new Callback<List<LedgerEntryDto>>() {
            @Override
            public void onResponse(Call<List<LedgerEntryDto>> call, Response<List<LedgerEntryDto>> response) {
                int ledgerAdded = 0;

                if (response.isSuccessful() && response.body() != null) {
                    for (LedgerEntryDto dto : response.body()) {
                        if (dto == null) continue;
                        LedgerEntry entry = dto.toLocal();
                        LedgerLocalStore.updateEntry(requireContext(), entry);
                        ledgerAdded++;
                    }
                } else {
                    Log.e(TAG, "ledger register from shopping failed. code=" + response.code());
                    ledgerAdded = LedgerLocalStore.addShoppingEntries(requireContext(), selected);
                }

                finishRegisterMessage(fridgeSuccessCount, fridgeFailCount, ledgerAdded);
            }

            @Override
            public void onFailure(Call<List<LedgerEntryDto>> call, Throwable t) {
                Log.e(TAG, "ledger register from shopping network failure", t);
                int ledgerAdded = LedgerLocalStore.addShoppingEntries(requireContext(), selected);
                finishRegisterMessage(fridgeSuccessCount, fridgeFailCount, ledgerAdded);
            }
        });
    }

    private void finishRegisterMessage(int fridgeSuccessCount,
                                       int fridgeFailCount,
                                       int ledgerAdded) {
        int total = fridgeSuccessCount + fridgeFailCount;

        String message;
        if (fridgeFailCount == 0) {
            message = "냉장고 등록 " + fridgeSuccessCount + "/" + total
                    + " · 가계부 등록 " + ledgerAdded + "건";
        } else if (fridgeSuccessCount > 0) {
            message = "냉장고 등록 " + fridgeSuccessCount + "/" + total
                    + " · 일부 재료는 다시 시도해주세요."
                    + " · 가계부 등록 " + ledgerAdded + "건";
        } else {
            message = NetworkErrorUtil.getDefaultMessage()
                    + " · 가계부 등록 " + ledgerAdded + "건";
        }

        showToast(message);

        if (listener != null) listener.onRegistered();
        dismiss();
    }

    private FridgeItemCreateRequest buildCreateRequestFromShoppingItem(@NonNull ShoppingItem it) {
        String name = safe(it.getName());

        double qty = it.getQuantity();
        if (qty <= 0d) qty = 1.0d;

        String storage = "냉장";
        String unit = safeUnit(it.getUnit());

        String iconKey = it.getIconKey();
        if (iconKey == null) iconKey = "";
        iconKey = iconKey.trim();

        int resId = 0;
        if (!TextUtils.isEmpty(iconKey)) {
            resId = IconCatalog.findResIdByRawKey(iconKey);
        }

        if (resId == 0) {
            IconItem icon = findIconByName(name);
            if (icon != null) {
                iconKey = (icon.getRawKey() == null) ? "" : icon.getRawKey().trim();
                resId = icon.getResId();
            }
        }

        String nick = OnboardingPrefs.getNickname(requireContext());
        if (TextUtils.isEmpty(nick)) nick = "사용자";

        FridgeItemCreateRequest req = new FridgeItemCreateRequest();
        req.setName(name);
        req.setStorage(storage);
        req.setQuantity(qty);
        req.setExpiryDate("");
        req.setIconResId(resId);
        req.setUnit(unit);
        req.setIconName(iconKey);
        req.setAddedByNickname(nick);

        return req;
    }

    @Nullable
    private IconItem findIconByName(String name) {
        if (TextUtils.isEmpty(name)) return null;

        List<IconItem> all = IconCatalog.getAllIcons();

        for (IconItem icon : all) {
            if (icon == null) continue;
            if (name.trim().equals(icon.getName())) return icon;
        }

        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String safeUnit(String s) {
        if (s == null || s.trim().isEmpty()) return "개";
        return s.trim();
    }
}