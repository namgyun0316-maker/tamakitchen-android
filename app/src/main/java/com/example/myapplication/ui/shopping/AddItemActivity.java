package com.namgyun.tamakitchen.ui.shopping;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.PriceSuggestResponse;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.network.ShoppingApi;
import com.namgyun.tamakitchen.ui.fridge.FridgeModeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddItemActivity extends AppCompatActivity {

    private static final String TAG = "AddItemActivity";

    private EditText etSearch;
    private RecyclerView rvIcons;
    private IconAdapter iconAdapter;

    private ImageView ivSelectedProduct;
    private EditText etSelectedProduct;

    private MaterialButton btnDecrease, btnIncrease;
    private TextView tvQuantity;
    private View qtyClickArea;

    private double quantity = 1.0d;

    private MaterialButton btnAdd, btnCancel;
    private TabLayout tabCategories;

    private MaterialCardView layoutSelectedItem;
    private IconItem selectedIcon;

    private EditText etPrice;

    private TextView tvUnit;
    private String selectedUnit = "개";

    private List<IconItem> allIconsList;
    private List<IconItem> filteredIconsList;

    private final List<String> unitList = Arrays.asList("개", "g", "kg", "ml", "L", "봉", "팩");

    // 최근 구매가 영역
    private View layoutRecentPrice;
    private TextView tvRecentPriceMain;
    private TextView tvRecentPriceSub;
    private MaterialButton btnApplyRecentPrice;

    // 판매점 정보
    private Long storeId;
    private String storeName;

    private ShoppingApi api;

    // ✅ 실제 로그인 사용자 id 사용
    private long currentUserId = 1L;
    private final FridgeModeManager fridgeModeManager = new FridgeModeManager();

    private String lastRequestedName = "";
    private String lastRequestedUnit = "";
    private Long lastRequestedStoreId = null;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable suggestRunnable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        api = RetrofitClient.getShoppingApi();
        updateUserIdSafe();

        Intent it = getIntent();
        if (it != null) {
            Object sid = it.getExtras() != null ? it.getExtras().get("storeId") : null;
            if (sid instanceof Long) storeId = (Long) sid;
            else if (sid instanceof Integer) storeId = ((Integer) sid).longValue();
            else if (sid instanceof String) {
                try {
                    storeId = Long.parseLong((String) sid);
                } catch (Exception e) {
                    storeId = null;
                }
            } else {
                storeId = null;
            }

            storeName = it.getStringExtra("storeName");
            if (storeName == null) storeName = "";
        }

        Log.d(TAG, "onCreate() userId=" + currentUserId + ", storeId=" + storeId + ", storeName=" + storeName);

        initViews();
        setupIconRecyclerView();
        setupTabCategories();
        setupEventListeners();
        updateAddButtonEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserIdSafe();
    }

    private void updateUserIdSafe() {
        long uid = fridgeModeManager.getUserIdSafe(this);
        if (uid > 0) currentUserId = uid;
        else currentUserId = 1L;
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        rvIcons = findViewById(R.id.rv_icons);
        ivSelectedProduct = findViewById(R.id.iv_selected_product);
        etSelectedProduct = findViewById(R.id.et_selected_product);

        btnDecrease = findViewById(R.id.btn_decrease);
        btnIncrease = findViewById(R.id.btn_increase);
        tvQuantity = findViewById(R.id.tv_quantity);
        qtyClickArea = findViewById(R.id.qty_click_area);

        btnAdd = findViewById(R.id.btn_add);
        btnCancel = findViewById(R.id.btn_cancel);

        tabCategories = findViewById(R.id.tab_categories);
        layoutSelectedItem = findViewById(R.id.layout_selected_item);

        etPrice = findViewById(R.id.et_price);

        tvUnit = findViewById(R.id.tv_unit);
        tvUnit.setText(selectedUnit);

        layoutRecentPrice = findViewById(R.id.layout_recent_price);
        tvRecentPriceMain = findViewById(R.id.tv_recent_price_main);
        tvRecentPriceSub = findViewById(R.id.tv_recent_price_sub);
        btnApplyRecentPrice = findViewById(R.id.btn_apply_recent_price);

        if (layoutRecentPrice != null) layoutRecentPrice.setVisibility(View.GONE);

        layoutSelectedItem.setVisibility(View.GONE);
        btnAdd.setEnabled(false);
        btnAdd.setAlpha(0.55f);

        tvQuantity.setText(formatQty(quantity));
    }

    private void setupEventListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterIcons(s == null ? "" : s.toString(), getCurrentCategory());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSelectedProduct.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAddButtonEnabled();
                if (selectedIcon == null) return;

                if (suggestRunnable != null) handler.removeCallbacks(suggestRunnable);
                suggestRunnable = () -> requestPriceSuggestion();
                handler.postDelayed(suggestRunnable, 350);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDecrease.setOnClickListener(v -> {
            quantity -= 1.0d;
            if (quantity < 0.1d) quantity = 0.1d;
            tvQuantity.setText(formatQty(quantity));
        });

        btnIncrease.setOnClickListener(v -> {
            quantity += 1.0d;
            tvQuantity.setText(formatQty(quantity));
        });

        if (qtyClickArea != null) {
            qtyClickArea.setClickable(true);
            qtyClickArea.setOnClickListener(v -> showQuantityInputDialogPretty());
        }

        tvQuantity.setClickable(true);
        tvQuantity.setOnClickListener(v -> showQuantityInputDialogPretty());

        tvUnit.setOnClickListener(v -> showUnitPickerDialogPretty());

        btnAdd.setOnClickListener(v -> {
            if (selectedIcon == null) return;

            String name = safe(etSelectedProduct.getText() == null ? "" : etSelectedProduct.getText().toString());
            if (name.isEmpty()) return;

            String priceText = (etPrice.getText() != null) ? safe(etPrice.getText().toString()) : "";
            int price = 0;
            if (!priceText.isEmpty()) {
                try {
                    price = (int) Math.round(Double.parseDouble(priceText));
                } catch (NumberFormatException ignored) {
                    price = 0;
                }
            }

            String iconKey = selectedIcon.getNormalizedKey();
            if (iconKey == null || iconKey.trim().isEmpty()) iconKey = selectedIcon.getSearchKey();
            if (iconKey == null) iconKey = "";
            iconKey = iconKey.trim();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("name", name);
            resultIntent.putExtra("quantity", quantity);
            resultIntent.putExtra("price", price);
            resultIntent.putExtra("iconKey", iconKey);
            resultIntent.putExtra("unit", selectedUnit);

            if (storeId != null) resultIntent.putExtra("storeId", storeId);
            if (storeName != null) resultIntent.putExtra("storeName", storeName);

            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnCancel.setOnClickListener(v -> hideSelectedPanel(true));
    }

    private void setupIconRecyclerView() {
        allIconsList = IconCatalog.getAllIcons();
        filteredIconsList = new ArrayList<>(allIconsList);

        rvIcons.setLayoutManager(new GridLayoutManager(this, 4));

        iconAdapter = new IconAdapter(filteredIconsList, icon -> {
            if (layoutSelectedItem.getVisibility() == View.VISIBLE && selectedIcon == icon) {
                hideSelectedPanel(true);
                return;
            }

            selectedIcon = icon;

            ivSelectedProduct.setImageResource(icon.getResId());
            etSelectedProduct.setText(icon.getName());

            quantity = 1.0d;
            tvQuantity.setText(formatQty(quantity));

            selectedUnit = "개";
            tvUnit.setText(selectedUnit);

            if (etPrice != null) etPrice.setText("");

            showSelectedPanel(true);
            updateAddButtonEnabled();

            requestPriceSuggestion();
        });

        rvIcons.setAdapter(iconAdapter);
    }

    private void setupTabCategories() {
        String[] categories = {"전체", "채소", "과일", "육류", "해산물", "유제품", "냉동식품", "기타"};

        tabCategories.removeAllTabs();
        for (String category : categories) {
            tabCategories.addTab(tabCategories.newTab().setText(category));
        }

        tabCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                filterIcons(etSearch.getText() == null ? "" : etSearch.getText().toString(), String.valueOf(tab.getText()));
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private String getCurrentCategory() {
        TabLayout.Tab selectedTab = tabCategories.getTabAt(tabCategories.getSelectedTabPosition());
        return selectedTab != null ? String.valueOf(selectedTab.getText()) : "전체";
    }

    private void filterIcons(String searchText, String category) {
        String q = (searchText == null) ? "" : searchText.trim().toLowerCase(Locale.getDefault());
        filteredIconsList.clear();

        for (IconItem icon : allIconsList) {
            String name = icon.getName() != null ? icon.getName() : "";
            String cat = icon.getCategory() != null ? icon.getCategory() : "";
            String searchKey = icon.getSearchKey() != null ? icon.getSearchKey() : "";

            boolean matchesSearch =
                    q.isEmpty()
                            || name.toLowerCase(Locale.getDefault()).contains(q)
                            || searchKey.toLowerCase(Locale.getDefault()).contains(q);

            boolean matchesCategory = category.equals("전체") || cat.equals(category);

            if (matchesSearch && matchesCategory) filteredIconsList.add(icon);
        }

        iconAdapter.notifyDataSetChanged();
    }

    private void updateAddButtonEnabled() {
        boolean hasIcon = (selectedIcon != null);
        boolean hasName = etSelectedProduct != null
                && etSelectedProduct.getText() != null
                && !safe(etSelectedProduct.getText().toString()).isEmpty();

        btnAdd.setEnabled(hasIcon && hasName);
        btnAdd.setAlpha(btnAdd.isEnabled() ? 1f : 0.55f);
    }

    private void showSelectedPanel(boolean animate) {
        if (layoutSelectedItem.getVisibility() == View.VISIBLE) return;

        layoutSelectedItem.setVisibility(View.VISIBLE);

        if (animate) {
            layoutSelectedItem.setTranslationY(layoutSelectedItem.getHeight() == 0 ? 400f : layoutSelectedItem.getHeight());
            layoutSelectedItem.setAlpha(0f);
            layoutSelectedItem.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            layoutSelectedItem.setTranslationY(0f);
            layoutSelectedItem.setAlpha(1f);
        }
    }

    private void hideSelectedPanel(boolean animate) {
        if (layoutSelectedItem.getVisibility() != View.VISIBLE) return;

        if (animate) {
            float down = layoutSelectedItem.getHeight() == 0 ? 400f : layoutSelectedItem.getHeight();
            layoutSelectedItem.animate()
                    .translationY(down)
                    .alpha(0f)
                    .setDuration(180)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        layoutSelectedItem.setVisibility(View.GONE);
                        layoutSelectedItem.setTranslationY(0f);
                        layoutSelectedItem.setAlpha(1f);

                        selectedIcon = null;
                        btnAdd.setEnabled(false);
                        btnAdd.setAlpha(0.55f);

                        hideRecentPrice();
                    })
                    .start();
        } else {
            layoutSelectedItem.setVisibility(View.GONE);
            selectedIcon = null;
            btnAdd.setEnabled(false);
            btnAdd.setAlpha(0.55f);
            hideRecentPrice();
        }
    }

    private void requestPriceSuggestion() {
        if (api == null) return;
        if (selectedIcon == null) return;

        if (storeId == null) {
            hideRecentPrice();
            return;
        }

        String name = safe(etSelectedProduct.getText() == null ? "" : etSelectedProduct.getText().toString());
        if (name.isEmpty()) {
            hideRecentPrice();
            return;
        }

        String unit = safe(selectedUnit);

        lastRequestedName = name;
        lastRequestedUnit = unit;
        lastRequestedStoreId = storeId;

        Log.d(TAG, "suggestPrice() userId=" + currentUserId + ", name=" + name + ", unit=" + unit + ", storeId=" + storeId);

        api.suggestPrice(currentUserId, name, unit, storeId).enqueue(new Callback<PriceSuggestResponse>() {
            @Override
            public void onResponse(Call<PriceSuggestResponse> call, Response<PriceSuggestResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    hideRecentPrice();
                    return;
                }

                String nowName = safe(etSelectedProduct.getText() == null ? "" : etSelectedProduct.getText().toString());
                String nowUnit = safe(selectedUnit);
                Long nowStoreId = storeId;

                if (!safeEq(lastRequestedName, nowName)
                        || !safeEq(lastRequestedUnit, nowUnit)
                        || !safeEqLong(lastRequestedStoreId, nowStoreId)) {
                    return;
                }

                PriceSuggestResponse body = response.body();

                boolean hasCurrent =
                        body.currentStoreRecent != null
                                && body.currentStoreRecent.price != null
                                && body.currentStoreRecent.price > 0;

                boolean hasCheaper =
                        body.cheaperElsewhere != null
                                && body.cheaperElsewhere.price != null
                                && body.cheaperElsewhere.price > 0;

                if (!hasCurrent && !hasCheaper) {
                    hideRecentPrice();
                    return;
                }

                bindRecentPriceUI(body);
            }

            @Override
            public void onFailure(Call<PriceSuggestResponse> call, Throwable t) {
                hideRecentPrice();
            }
        });
    }

    private void hideRecentPrice() {
        if (layoutRecentPrice != null) layoutRecentPrice.setVisibility(View.GONE);
    }

    private void bindRecentPriceUI(PriceSuggestResponse res) {
        if (layoutRecentPrice == null || tvRecentPriceMain == null || tvRecentPriceSub == null || btnApplyRecentPrice == null) {
            return;
        }

        Integer applyPriceObj = null;

        boolean hasCurrent =
                res.currentStoreRecent != null
                        && res.currentStoreRecent.price != null
                        && res.currentStoreRecent.price > 0
                        && res.currentStoreRecent.storeName != null
                        && res.currentStoreRecent.daysAgo != null;

        boolean hasCheaper =
                res.cheaperElsewhere != null
                        && res.cheaperElsewhere.price != null
                        && res.cheaperElsewhere.price > 0
                        && res.cheaperElsewhere.storeName != null
                        && res.cheaperElsewhere.daysAgo != null;

        String mainText = "";
        String subText = "";

        if (hasCurrent) {
            applyPriceObj = res.currentStoreRecent.price;
            mainText = "현재 판매점 최근가: " + formatMoney(applyPriceObj) + "원";
            subText = "(" + res.currentStoreRecent.daysAgo + "일 전 · " + res.currentStoreRecent.storeName + ")";
        } else if (hasCheaper) {
            applyPriceObj = res.cheaperElsewhere.price;
            mainText = "다른 판매점 최근가: " + formatMoney(applyPriceObj) + "원";

            String diffText = "";
            if (res.cheaperElsewhere.diff != null && res.cheaperElsewhere.diff > 0) {
                diffText = " · " + res.cheaperElsewhere.diff + "원 더 저렴";
            }
            subText = "(" + res.cheaperElsewhere.daysAgo + "일 전 · " + res.cheaperElsewhere.storeName + ")" + diffText;
        }

        if (applyPriceObj == null || applyPriceObj <= 0) {
            hideRecentPrice();
            return;
        }

        final int finalApplyPrice = applyPriceObj;

        layoutRecentPrice.setVisibility(View.VISIBLE);
        tvRecentPriceMain.setText(mainText);
        tvRecentPriceSub.setText(subText);

        btnApplyRecentPrice.setOnClickListener(v -> {
            if (etPrice != null) etPrice.setText(String.valueOf(finalApplyPrice));
        });
    }

    private String formatMoney(int value) {
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(value);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean safeEq(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }

    private boolean safeEqLong(Long a, Long b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private void showQuantityInputDialogPretty() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_quantity_pretty, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        EditText et = dialogView.findViewById(R.id.etQuantity);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnOk = dialogView.findViewById(R.id.btnOk);

        tvTitle.setText("수량 입력");

        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setSingleLine(true);

        String cur = formatQty(quantity);
        et.setText(cur);
        et.setSelection(et.getText().length());

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                        .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
            et.requestFocus();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String t = et.getText() == null ? "" : et.getText().toString().trim();
            if (t.isEmpty() || t.equals(".") || t.equals(",")) return;

            t = t.replace(",", ".");
            try {
                double q = Double.parseDouble(t);
                if (q <= 0d) return;

                quantity = q;
                tvQuantity.setText(formatQty(quantity));
                dialog.dismiss();
            } catch (Exception ignored) {}
        });

        dialog.show();
    }

    private String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) return String.valueOf((long) Math.round(q));
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(q);
    }

    private void showUnitPickerDialogPretty() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unit_picker_pretty, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        GridLayout grid = dialogView.findViewById(R.id.grid_units);
        MaterialButton btnClose = dialogView.findViewById(R.id.btn_close);

        grid.removeAllViews();

        for (String unit : unitList) {
            TextView chip = new TextView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(8, 8, 8, 8);
            chip.setLayoutParams(lp);

            chip.setText(unit);
            chip.setTextSize(16f);
            chip.setTextColor(0xFF2E3A4B);
            chip.setPadding(16, 18, 16, 18);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setBackgroundResource(R.drawable.bg_unit_item);

            chip.setOnClickListener(v -> {
                selectedUnit = unit;
                tvUnit.setText(selectedUnit);
                dialog.dismiss();

                if (etPrice != null) etPrice.setText("");
                requestPriceSuggestion();
            });

            grid.addView(chip);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (suggestRunnable != null) handler.removeCallbacks(suggestRunnable);
    }
}