// File: app/src/main/java/com/example/myapplication/ui/shopping/ShoppingFragment.java
package com.namgyun.tamakitchen.ui.shopping;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.ShoppingItemRequest;
import com.namgyun.tamakitchen.pet.PetExpManager;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingFragment extends Fragment
        implements CalendarDialogFragment.OnDateStorePickedListener,
        CalendarDialogFragment.MonthSpendProvider {

    private static final String TAG = "ShoppingFragment";

    private RecyclerView recyclerView;
    private ShoppingAdapter adapter;
    private final ArrayList<ShoppingItem> items = new ArrayList<>();

    private TextView tvTotalPrice;
    private TextView tvCurrentDate, btnPrevDate, btnNextDate;

    private View dateNav;

    private MaterialButton btnDeleteAll;
    private MaterialButton btnAddItemText;
    private MaterialButton btnShareList;
    private MaterialButton btnFinishShopping;

    private View storeChip;
    private TextView tvStoreNameText;

    private View btnScan;

    private Long currentStoreId = null;
    private String currentStoreName = "미지정";

    private static final String KEY_STORE_ID = "KEY_STORE_ID";
    private static final String KEY_STORE_NAME = "KEY_STORE_NAME";

    private Calendar currentCalendar;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");

    private long currentUserId = 1L;

    private ShoppingRepository repo;
    private ShoppingGrouper grouper;

    private ActivityResultLauncher<Intent> addItemLauncher;
    private ActivityResultLauncher<Intent> receiptScanLauncher;

    private final FridgeModeManager fridgeMode = new FridgeModeManager();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_shopping, container, false);

        repo = new ShoppingRepository();
        grouper = new ShoppingGrouper();

        recyclerView = view.findViewById(R.id.rv_shopping_items);
        tvTotalPrice = view.findViewById(R.id.tv_total_price);

        dateNav = view.findViewById(R.id.date_navigation);

        tvCurrentDate = view.findViewById(R.id.tv_current_date);
        btnPrevDate = view.findViewById(R.id.btn_prev_date);
        btnNextDate = view.findViewById(R.id.btn_next_date);

        btnDeleteAll = view.findViewById(R.id.btn_delete_all);
        btnAddItemText = view.findViewById(R.id.btn_add_item_text);
        btnShareList = view.findViewById(R.id.btn_share_list);
        btnFinishShopping = view.findViewById(R.id.btn_finish_shopping);

        storeChip = view.findViewById(R.id.tv_store_name);
        tvStoreNameText = view.findViewById(R.id.tv_store_name_text);

        btnScan = view.findViewById(R.id.btn_scan);

        updateUserIdSafe();

        restoreStoreState(savedInstanceState);
        normalizeStoreState();
        updateStoreText();

        if (storeChip != null) {
            storeChip.setOnClickListener(v -> openStorePicker());
        }

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> openScanner());
        }

        currentCalendar = Calendar.getInstance();
        updateDateText();

        View.OnClickListener openCalendar = v -> openCalendarDialog();
        if (tvCurrentDate != null) tvCurrentDate.setOnClickListener(openCalendar);
        if (dateNav != null) dateNav.setOnClickListener(openCalendar);

        adapter = new ShoppingAdapter(items);

        adapter.setOnItemClickListener((pos, item) -> {
            if (item == null) return;
            showEditDialog(item);
        });

        adapter.setOnItemLongClickListener((pos, item) -> {
            if (item == null) return;
            showDeleteDialog(item);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        addItemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();

                    Log.d(TAG, "addItemLauncher resultCode=" + resultCode + ", data=" + (data != null));

                    if (resultCode != Activity.RESULT_OK || data == null) return;

                    handleAddItemResult(data);
                }
        );

        receiptScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (!isAdded()) return;

                    int code = result.getResultCode();
                    Intent data = result.getData();

                    Log.d(TAG, "receiptScanLauncher resultCode=" + code + ", data=" + (data != null));

                    if (code != Activity.RESULT_OK) {
                        showToast("스캔이 취소됐어요.");
                        return;
                    }

                    if (data == null) {
                        showToast("스캔 결과를 확인할 수 없어요.");
                        return;
                    }

                    String raw = data.getStringExtra(ReceiptReviewActivity.EXTRA_RAW_TEXT);
                    if (raw == null) raw = data.getStringExtra("EXTRA_RAW_TEXT");

                    if (raw == null) raw = "";
                    raw = raw.trim();

                    Log.d(TAG, "receiptScanLauncher rawLen=" + raw.length());

                    String preview = raw.length() > 80 ? raw.substring(0, 80) + "..." : raw;
                    Log.d(TAG, "receiptScanLauncher rawPreview=" + preview);

                    if (TextUtils.isEmpty(raw)) {
                        showToast("인식된 텍스트가 없어요.");
                        return;
                    }

                    Intent review = new Intent(requireContext(), ReceiptReviewActivity.class);
                    review.putExtra(ReceiptReviewActivity.EXTRA_RAW_TEXT, raw);

                    Log.d(TAG, "start ReceiptReviewActivity with rawLen=" + raw.length());
                    startActivity(review);
                }
        );

        loadItemsFromServer();

        if (btnPrevDate != null) btnPrevDate.setOnClickListener(v -> changeDate(-1));
        if (btnNextDate != null) btnNextDate.setOnClickListener(v -> changeDate(1));

        if (btnDeleteAll != null) {
            btnDeleteAll.setOnClickListener(v -> showDeleteAllDialog());
        }

        if (btnShareList != null) {
            btnShareList.setOnClickListener(v -> shareShoppingListToKakao());
        }

        if (btnAddItemText != null) {
            btnAddItemText.setOnClickListener(v -> {
                normalizeStoreState();

                Intent intent = new Intent(getActivity(), AddItemActivity.class);

                if (currentStoreId != null) {
                    intent.putExtra("storeId", currentStoreId);
                }

                intent.putExtra("storeName", currentStoreName);

                addItemLauncher.launch(intent);
            });
        }

        if (btnFinishShopping != null) {
            btnFinishShopping.setOnClickListener(v -> onClickFinishShopping());
        } else {
            Log.e(TAG, "btn_finish_shopping is NULL. fragment_shopping.xml id 확인 필요");
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserIdSafe();
    }

    private void updateUserIdSafe() {
        if (getContext() == null) return;

        long uid = fridgeMode.getUserIdSafe(getContext());

        if (uid > 0) currentUserId = uid;
        else currentUserId = 1L;
    }

    private void showToast(String message) {
        if (!isAdded()) return;

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;

        AppToast.show(activity, message);
    }

    private void showNetworkToast(Throwable t) {
        showToast(NetworkErrorUtil.getUserMessage(t));
    }

    private void openScanner() {
        if (!isAdded()) return;

        try {
            Intent intent = new Intent(requireContext(), ReceiptScannerActivity.class);
            Log.d(TAG, "openScanner() launch ReceiptScannerActivity");
            receiptScanLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "openScanner error", e);
            showToast("스캐너를 실행하지 못했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private void normalizeStoreState() {
        if (TextUtils.isEmpty(currentStoreName)) currentStoreName = "미지정";

        currentStoreName = currentStoreName.trim();

        if (currentStoreName.isEmpty()) currentStoreName = "미지정";

        if (currentStoreId == null) {
            currentStoreName = "미지정";
        }
    }

    private void showDeleteAllDialog() {
        if (getContext() == null || !isAdded()) return;

        if (items.isEmpty()) {
            showToast("삭제할 항목이 없습니다.");
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_delete_all_shopping, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvStore = dialogView.findViewById(R.id.tvStore);
        TextView tvDesc = dialogView.findViewById(R.id.tvDesc);
        TextView tvCount = dialogView.findViewById(R.id.tvCount);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnDelete = dialogView.findViewById(R.id.btnDelete);

        String dateKey = dateFormat.format(currentCalendar.getTime());
        String storeLabel = (currentStoreName == null || currentStoreName.trim().isEmpty())
                ? "미지정" : currentStoreName.trim();

        if (tvTitle != null) tvTitle.setText("전체삭제");
        if (tvDate != null) tvDate.setText("날짜: " + dateKey);
        if (tvStore != null) tvStore.setText("판매점: " + storeLabel);
        if (tvDesc != null) tvDesc.setText("현재 목록의 항목을 모두 삭제할까요?");
        if (tvCount != null) tvCount.setText("총 " + items.size() + "개 항목");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                deleteAllVisibleItems();
            });
        }

        dialog.show();
    }

    private void deleteAllVisibleItems() {
        if (!isAdded()) return;

        if (items.isEmpty()) {
            showToast("삭제할 항목이 없습니다.");
            return;
        }

        ArrayList<ShoppingItem> targets = new ArrayList<>();

        for (ShoppingItem item : items) {
            if (item != null && item.getId() != null) {
                targets.add(item);
            }
        }

        if (targets.isEmpty()) {
            showToast("삭제할 수 있는 항목이 없습니다.");
            return;
        }

        deleteItemsSequentially(targets, 0, 0);
    }

    private void deleteItemsSequentially(@NonNull List<ShoppingItem> targets, int index, int successCount) {
        if (!isAdded()) return;

        if (index >= targets.size()) {
            showToast(successCount + "개 항목이 삭제되었습니다.");
            loadItemsFromServer();
            return;
        }

        ShoppingItem item = targets.get(index);

        if (item == null || item.getId() == null) {
            deleteItemsSequentially(targets, index + 1, successCount);
            return;
        }

        repo.deleteItem(item.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    deleteItemsSequentially(targets, index + 1, successCount + 1);
                } else {
                    Log.e(TAG, "delete all item failed. code=" + response.code());
                    showToast("일부 항목을 삭제하지 못했어요. 잠시 후 다시 시도해주세요.");
                    loadItemsFromServer();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;

                Log.e(TAG, "delete all item network failure", t);
                showNetworkToast(t);
                loadItemsFromServer();
            }
        });
    }

    private void shareShoppingListToKakao() {
        if (!isAdded()) return;

        if (items.isEmpty()) {
            showToast("공유할 장보기 목록이 없습니다.");
            return;
        }

        String shareText = buildShoppingShareText();

        Intent kakaoIntent = new Intent(Intent.ACTION_SEND);
        kakaoIntent.setType("text/plain");
        kakaoIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        kakaoIntent.setPackage("com.kakao.talk");

        try {
            startActivity(kakaoIntent);
        } catch (Exception e) {
            Intent normalIntent = new Intent(Intent.ACTION_SEND);
            normalIntent.setType("text/plain");
            normalIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            Intent chooser = Intent.createChooser(normalIntent, "장보기 목록 공유");
            startActivity(chooser);
        }
    }

    private String buildShoppingShareText() {
        String dateKey = dateFormat.format(currentCalendar.getTime());

        String storeLabel = currentStoreName == null || currentStoreName.trim().isEmpty()
                ? "미지정"
                : currentStoreName.trim();

        StringBuilder sb = new StringBuilder();

        sb.append("🛒 장보기 목록\n");
        sb.append("날짜: ").append(dateKey).append("\n");
        sb.append("판매점: ").append(storeLabel).append("\n\n");

        long total = 0L;
        int displayIndex = 1;

        for (ShoppingItem item : items) {
            if (item == null) continue;

            String name = item.getName() == null ? "이름없음" : item.getName().trim();
            String unit = ShoppingUtils.safeUnit(item.getUnit());

            double qty = item.getQuantity();
            int price = item.getPrice();

            long lineTotal = Math.round(price * qty);
            total += lineTotal;

            sb.append(displayIndex++)
                    .append(". ")
                    .append(name)
                    .append(" ")
                    .append(formatQty(qty))
                    .append(unit);

            if (price > 0) {
                sb.append(" / ")
                        .append(moneyFormat.format(lineTotal))
                        .append("원");
            }

            sb.append("\n");
        }

        sb.append("\n총 금액: ")
                .append(moneyFormat.format(total))
                .append("원");

        return sb.toString();
    }

    private void onClickFinishShopping() {
        if (!isAdded()) return;

        if (items.isEmpty()) {
            showToast("완료할 항목이 없습니다.");
            return;
        }

        long total = calcCurrentTotal();

        showFinishConfirmDialog(total, this::openShoppingCompleteDialog);
    }

    private long calcCurrentTotal() {
        long total = 0L;

        for (ShoppingItem it : items) {
            if (it == null) continue;
            total += Math.round(it.getPrice() * it.getQuantity());
        }

        return Math.max(0L, total);
    }

    private void showFinishConfirmDialog(long totalSpend, @NonNull Runnable onYes) {
        if (getContext() == null) return;

        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_finish_shopping, null);

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvMessage = v.findViewById(R.id.tvMessage);
        View btnNo = v.findViewById(R.id.btnNo);
        View btnYes = v.findViewById(R.id.btnYes);

        if (tvTitle != null) tvTitle.setText("장보기 완료");

        String msg = moneyFormat.format(totalSpend) + "원 지출하셨습니다.\n냉장고에 재료를 등록하시겠습니까?";

        if (tvMessage != null) tvMessage.setText(msg);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        if (btnNo != null) btnNo.setOnClickListener(view -> dialog.dismiss());

        if (btnYes != null) {
            btnYes.setOnClickListener(view -> {
                dialog.dismiss();
                onYes.run();
            });
        }

        dialog.show();
    }

    private void openShoppingCompleteDialog() {
        if (!isAdded()) return;

        ArrayList<ShoppingItem> copy = new ArrayList<>(items);

        ShoppingCompleteDialog dialog = ShoppingCompleteDialog.newInstance(currentUserId, copy);
        dialog.setOnRegisteredListener(() -> {
            if (!isAdded()) return;

            ChecklistPrefs.markWeeklyShoppingDone(requireContext());

            PetExpManager.giveShoppingCompleteExp(requireActivity());

            loadItemsFromServer();
        });

        dialog.show(getParentFragmentManager(), "ShoppingCompleteDialog");
    }

    private void openCalendarDialog() {
        if (!isAdded()) return;

        CalendarDialogFragment dialog = CalendarDialogFragment.newInstance(
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show(getParentFragmentManager(), "CalendarDialogFragment");
    }

    @Override
    public void onPicked(int year, int month0, int day, @Nullable Long storeId, @NonNull String storeName) {
        currentCalendar.set(Calendar.YEAR, year);
        currentCalendar.set(Calendar.MONTH, month0);
        currentCalendar.set(Calendar.DAY_OF_MONTH, day);

        updateDateText();

        currentStoreId = storeId;
        currentStoreName = (storeName == null || storeName.trim().isEmpty())
                ? "미지정"
                : storeName.trim();

        normalizeStoreState();

        if (getContext() != null) {
            StoreSessionManager.setCurrentStore(getContext(), currentStoreId, currentStoreName);
        }

        updateStoreText();

        loadItemsForCurrentDate();
    }

    @Override
    public Map<Integer, Long> getDayTotalsForMonth(int year, int month0) {
        return grouper.getDayTotalsForMonthAllStores(year, month0);
    }

    @Override
    public long getMonthTotal(int year, int month0) {
        return grouper.getMonthTotalAllStores(year, month0);
    }

    @Override
    public Map<Long, Long> getStoreTotalsForDate(String dateKey) {
        return grouper.getStoreTotalsForDate(dateKey);
    }

    @Override
    public Map<Long, String> getStoreNamesForDate(String dateKey) {
        return grouper.getStoreNamesForDate(dateKey);
    }

    private void showEditDialog(@NonNull ShoppingItem item) {
        if (getContext() == null) return;

        if (item.getId() == null) {
            showToast("서버 id가 없어 수정할 수 없어요.");
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_item, null);

        EditText etName = dialogView.findViewById(R.id.et_edit_name);
        EditText etPrice = dialogView.findViewById(R.id.et_edit_price);
        EditText etQty = dialogView.findViewById(R.id.tv_quantity);

        TextView btnDec = dialogView.findViewById(R.id.btn_decrease);
        TextView btnInc = dialogView.findViewById(R.id.btn_increase);

        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnSave = dialogView.findViewById(R.id.btnSave);

        etName.setText(item.getName() == null ? "" : item.getName());
        etPrice.setText(String.valueOf(item.getPrice()));
        etQty.setText(formatQty(item.getQuantity()));

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        btnDec.setOnClickListener(v -> {
            double q = parseQty(etQty.getText() == null ? "" : etQty.getText().toString());

            q -= 1.0d;

            if (q < 0.1d) q = 0.1d;

            etQty.setText(formatQty(q));
            etQty.setSelection(etQty.getText().length());
        });

        btnInc.setOnClickListener(v -> {
            double q = parseQty(etQty.getText() == null ? "" : etQty.getText().toString());

            q += 1.0d;

            etQty.setText(formatQty(q));
            etQty.setSelection(etQty.getText().length());
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() == null ? "" : etName.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                showToast("상품이름은 비울 수 없어요.");
                return;
            }

            int newPrice = 0;
            String p = etPrice.getText() == null ? "" : etPrice.getText().toString().trim();

            if (!TextUtils.isEmpty(p)) {
                try {
                    newPrice = Integer.parseInt(p);
                } catch (Exception ignored) {
                    newPrice = 0;
                }
            }

            if (newPrice < 0) newPrice = 0;

            double newQty = parseQty(etQty.getText() == null ? "" : etQty.getText().toString());

            if (newQty <= 0d) newQty = 1.0d;

            String unit = ShoppingUtils.safeUnit(item.getUnit());

            String dateKey = item.getShoppingDate();

            if (TextUtils.isEmpty(dateKey)) {
                dateKey = dateFormat.format(currentCalendar.getTime());
            }

            Long storeId = item.getStoreId();

            if (storeId == null) {
                storeId = currentStoreId;
            }

            ShoppingItemRequest req = new ShoppingItemRequest(
                    newName,
                    newQty,
                    item.getIconKey() == null ? "" : item.getIconKey(),
                    currentUserId,
                    newPrice,
                    dateKey,
                    unit,
                    storeId
            );

            repo.updateItem(item.getId(), req, new Callback<ShoppingItem>() {
                @Override
                public void onResponse(Call<ShoppingItem> call, Response<ShoppingItem> response) {
                    if (!isAdded()) return;

                    if (response.isSuccessful()) {
                        showToast("수정 완료!");
                        dialog.dismiss();
                        loadItemsFromServer();
                    } else {
                        Log.e(TAG, "update item failed. code=" + response.code());
                        showToast("수정에 실패했어요. 잠시 후 다시 시도해주세요.");
                    }
                }

                @Override
                public void onFailure(Call<ShoppingItem> call, Throwable t) {
                    if (!isAdded()) return;

                    Log.e(TAG, "update item network failure", t);
                    showNetworkToast(t);
                }
            });
        });

        dialog.show();
    }

    private void showDeleteDialog(@NonNull ShoppingItem item) {
        if (getContext() == null) return;

        if (item.getId() == null) {
            showToast("서버 id가 없어 삭제할 수 없어요.");
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_delete_shopping_item, null);

        ImageView ivIcon = dialogView.findViewById(R.id.ivIcon);
        TextView tvItemName = dialogView.findViewById(R.id.tvItemName);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnDelete = dialogView.findViewById(R.id.btnDelete);

        if (ivIcon == null || tvItemName == null || btnCancel == null || btnDelete == null) {
            showToast("삭제 화면을 불러오지 못했어요.");
            return;
        }

        int resId = item.getIconResId();

        if (resId == 0) {
            resId = IconCatalog.findResIdByRawKey(item.getIconKey());
        }

        if (resId == 0) {
            resId = R.drawable.ic_launcher_foreground;
        }

        ivIcon.setImageResource(resId);

        String name = item.getName() == null ? "" : item.getName().trim();
        tvItemName.setText("'" + name + "'");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> repo.deleteItem(item.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    showToast("삭제 완료!");
                    dialog.dismiss();
                    loadItemsFromServer();
                } else {
                    Log.e(TAG, "delete item failed. code=" + response.code());
                    showToast("삭제에 실패했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;

                Log.e(TAG, "delete item network failure", t);
                showNetworkToast(t);
            }
        }));

        dialog.show();
    }

    private void handleAddItemResult(@NonNull Intent data) {
        String name = data.getStringExtra("name");

        if (TextUtils.isEmpty(name)) {
            showToast("이름이 비어있어 추가할 수 없어요.");
            return;
        }

        name = name.trim();

        int price = data.getIntExtra("price", 0);

        double qty = 1.0d;
        Bundle extras = data.getExtras();

        if (extras != null && extras.containsKey("quantity")) {
            Object qObj = extras.get("quantity");

            if (qObj instanceof Double) qty = (Double) qObj;
            else if (qObj instanceof Float) qty = ((Float) qObj).doubleValue();
            else if (qObj instanceof Integer) qty = ((Integer) qObj).doubleValue();
            else if (qObj instanceof Long) qty = ((Long) qObj).doubleValue();
            else if (qObj != null) {
                try {
                    qty = Double.parseDouble(String.valueOf(qObj));
                } catch (Exception ignored) {
                }
            }
        }

        if (qty <= 0d) qty = 1.0d;

        String iconKey = data.getStringExtra("iconKey");

        if (iconKey == null) iconKey = "";

        iconKey = iconKey.trim();

        String unit = ShoppingUtils.safeUnit(data.getStringExtra("unit"));

        Long storeIdFromResult = null;
        String storeNameFromResult = null;

        Bundle ex2 = data.getExtras();

        if (ex2 != null) {
            Object sid = ex2.get("storeId");

            if (sid instanceof Long) storeIdFromResult = (Long) sid;
            else if (sid instanceof Integer) storeIdFromResult = ((Integer) sid).longValue();
            else if (sid instanceof String) {
                try {
                    storeIdFromResult = Long.parseLong((String) sid);
                } catch (Exception ignored) {
                }
            }

            storeNameFromResult = ex2.getString("storeName", null);
        }

        if (storeIdFromResult == null) storeIdFromResult = currentStoreId;
        if (TextUtils.isEmpty(storeNameFromResult)) storeNameFromResult = currentStoreName;

        final Long finalStoreId = storeIdFromResult;
        final String finalStoreName = (finalStoreId == null) ? "미지정" : storeNameFromResult;

        final String dateKey = dateFormat.format(currentCalendar.getTime());

        ShoppingItemRequest request = new ShoppingItemRequest(
                name,
                qty,
                iconKey,
                currentUserId,
                price,
                dateKey,
                unit,
                finalStoreId
        );

        repo.addItem(request, new Callback<ShoppingItem>() {
            @Override
            public void onResponse(Call<ShoppingItem> call, Response<ShoppingItem> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    currentStoreId = finalStoreId;
                    currentStoreName = finalStoreName;
                    normalizeStoreState();

                    if (getContext() != null) {
                        StoreSessionManager.setCurrentStore(getContext(), currentStoreId, currentStoreName);
                    }

                    updateStoreText();

                    loadItemsFromServer();
                    showToast("추가 완료!");
                } else {
                    Log.e(TAG, "add item failed. code=" + response.code());
                    showToast("추가에 실패했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<ShoppingItem> call, Throwable t) {
                if (!isAdded()) return;

                Log.e(TAG, "add item network failure", t);
                showNetworkToast(t);
            }
        });
    }

    private void loadItemsFromServer() {
        repo.getItems(currentUserId, new Callback<List<ShoppingItem>>() {
            @Override
            public void onResponse(Call<List<ShoppingItem>> call, Response<List<ShoppingItem>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    grouper.clear();

                    for (ShoppingItem item : response.body()) {
                        String dateKey = item.getShoppingDate();

                        if (TextUtils.isEmpty(dateKey)) {
                            dateKey = dateFormat.format(currentCalendar.getTime());
                            item.setShoppingDate(dateKey);
                        }

                        Long storeId = item.getStoreId();

                        ensureIconKeyAndResId(item);

                        grouper.putItem(dateKey, storeId, item);
                    }

                    grouper.recomputeTotals();
                    loadItemsForCurrentDate();
                } else {
                    Log.e(TAG, "load items failed. code=" + response.code());
                    showToast("장보기 목록을 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(Call<List<ShoppingItem>> call, Throwable t) {
                if (!isAdded()) return;

                Log.e(TAG, "load items network failure", t);
                showNetworkToast(t);
            }
        });
    }

    private void loadItemsForCurrentDate() {
        String dateKey = dateFormat.format(currentCalendar.getTime());

        items.clear();
        items.addAll(grouper.getItems(dateKey, currentStoreId));

        adapter.notifyDataSetChanged();

        updateSummary();
    }

    private void updateSummary() {
        long total = 0;

        for (ShoppingItem it : items) {
            if (it == null) continue;
            total += Math.round(it.getPrice() * it.getQuantity());
        }

        if (tvTotalPrice != null) {
            tvTotalPrice.setText("총 금액: " + moneyFormat.format(total) + "원");
        }
    }

    private void changeDate(int offset) {
        currentCalendar.add(Calendar.DAY_OF_MONTH, offset);
        updateDateText();
        loadItemsForCurrentDate();
    }

    private void updateDateText() {
        if (tvCurrentDate != null) {
            tvCurrentDate.setText(dateFormat.format(currentCalendar.getTime()));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_STORE_NAME, currentStoreName == null ? "미지정" : currentStoreName);
        outState.putLong(KEY_STORE_ID, currentStoreId == null ? -1L : currentStoreId);
    }

    private void restoreStoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String n = savedInstanceState.getString(KEY_STORE_NAME, "");
            long id = savedInstanceState.getLong(KEY_STORE_ID, -1L);

            currentStoreId = (id <= 0L) ? null : id;
            currentStoreName = (n == null || n.trim().isEmpty()) ? "미지정" : n.trim();

            return;
        }

        if (getContext() != null) {
            Long sid = StoreSessionManager.getCurrentStoreId(getContext());
            String sname = StoreSessionManager.getCurrentStoreName(getContext());

            currentStoreId = sid;
            currentStoreName = (sname == null || sname.trim().isEmpty()) ? "미지정" : sname.trim();
        }
    }

    private void updateStoreText() {
        normalizeStoreState();

        if (tvStoreNameText != null) {
            tvStoreNameText.setText("판매점: " + currentStoreName);
        }
    }

    private void openStorePicker() {
        StorePickerDialog dialog = new StorePickerDialog(currentUserId, (storeId, storeName) -> {
            currentStoreId = storeId;
            currentStoreName = (storeName == null || storeName.trim().isEmpty()) ? "미지정" : storeName.trim();
            normalizeStoreState();

            if (getContext() != null) {
                StoreSessionManager.setCurrentStore(getContext(), currentStoreId, currentStoreName);
            }

            updateStoreText();
            loadItemsForCurrentDate();
        });

        dialog.show(getParentFragmentManager(), "StorePickerDialog");
    }

    private void ensureIconKeyAndResId(ShoppingItem item) {
        if (item == null) return;

        String iconKey = item.getIconKey();
        int resId = 0;

        if (!TextUtils.isEmpty(iconKey)) {
            resId = IconCatalog.findResIdByRawKey(iconKey);
        }

        if (resId == 0) {
            IconItem icon = findIconByName(item.getName());

            if (icon != null) {
                if (TextUtils.isEmpty(item.getIconKey())) {
                    item.setIconKey(icon.getRawKey());
                }

                resId = icon.getResId();
            }
        }

        item.setIconResId(resId);
    }

    private IconItem findIconByName(String name) {
        if (TextUtils.isEmpty(name)) return null;

        List<IconItem> all = IconCatalog.getAllIcons();

        for (IconItem icon : all) {
            if (icon == null) continue;

            if (name.trim().equals(icon.getName())) {
                return icon;
            }
        }

        return null;
    }

    private double parseQty(String s) {
        if (s == null) return 1.0d;

        String t = s.trim();

        if (t.isEmpty()) return 1.0d;

        t = t.replace(",", ".");

        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return 1.0d;
        }
    }

    private String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) {
            return String.valueOf((long) Math.round(q));
        }

        DecimalFormat df = new DecimalFormat("0.##");

        return df.format(q);
    }
}