package com.namgyun.tamakitchen.ui.collection;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.common.ToastUtil;
import com.google.android.material.button.MaterialButton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CollectionBookActivity extends AppCompatActivity
        implements CharacterCollectionAdapter.OnCollectionActionListener {

    private TextView tvCollectionCount;
    private RecyclerView recyclerCollection;

    private MaterialButton btnCategoryAll;
    private MaterialButton btnCategoryIngredient;
    private MaterialButton btnCategoryFood;
    private MaterialButton btnSellMode;

    private CharacterCollectionAdapter adapter;

    private final List<CharacterCollectionItem> allItems = new ArrayList<>();
    private final List<CharacterCollectionItem> filteredItems = new ArrayList<>();

    private CharacterCollectionCategory selectedCategory = null;
    private boolean sellMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_book);

        tvCollectionCount = findViewById(R.id.tvCollectionCount);
        recyclerCollection = findViewById(R.id.recyclerCollection);

        btnCategoryAll = findViewById(R.id.btnCategoryAll);
        btnCategoryIngredient = findViewById(R.id.btnCategoryIngredient);
        btnCategoryFood = findViewById(R.id.btnCategoryFood);
        btnSellMode = findViewById(R.id.btnSellMode);

        recyclerCollection.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new CharacterCollectionAdapter(this, filteredItems, this);
        recyclerCollection.setAdapter(adapter);

        btnCategoryAll.setOnClickListener(v -> {
            selectedCategory = null;
            applyCurrentFilter();
        });

        btnCategoryIngredient.setOnClickListener(v -> {
            selectedCategory = CharacterCollectionCategory.INGREDIENT;
            applyCurrentFilter();
        });

        btnCategoryFood.setOnClickListener(v -> {
            selectedCategory = CharacterCollectionCategory.FOOD;
            applyCurrentFilter();
        });

        btnSellMode.setOnClickListener(v -> toggleSellMode());

        reloadAllItems();
        applyCurrentFilter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadAllItems();
        applyCurrentFilter();
    }

    private void toggleSellMode() {
        sellMode = !sellMode;

        if (sellMode) {
            btnSellMode.setText("판매취소");
            ToastUtil.showIosToast(this, "판매할 캐릭터를 선택해 주세요.");
        } else {
            btnSellMode.setText("판매하기");
            ToastUtil.showIosToast(this, "판매 모드를 종료했어요.");
        }

        if (adapter != null) {
            adapter.setSellMode(sellMode);
        }
    }

    private void reloadAllItems() {
        allItems.clear();
        allItems.addAll(CollectionCatalog.getAllItems(this));
    }

    private void applyCurrentFilter() {
        filteredItems.clear();

        for (CharacterCollectionItem item : allItems) {
            if (selectedCategory == null || item.getCategory() == selectedCategory) {
                filteredItems.add(item);
            }
        }

        adapter.submitList(filteredItems);
        adapter.setSellMode(sellMode);

        updateCountText();
        updateFilterUi();
    }

    private void updateCountText() {
        int unlockedCount = 0;

        for (CharacterCollectionItem item : filteredItems) {
            if (item.isUnlocked()) {
                unlockedCount++;
            }
        }

        if (selectedCategory == null) {
            tvCollectionCount.setText("발견 " + unlockedCount + " / " + filteredItems.size());
        } else {
            tvCollectionCount.setText(
                    selectedCategory.getDisplayName()
                            + " 발견 "
                            + unlockedCount
                            + " / "
                            + filteredItems.size()
            );
        }
    }

    private void updateFilterUi() {
        styleFilterButton(btnCategoryAll, selectedCategory == null);
        styleFilterButton(btnCategoryIngredient, selectedCategory == CharacterCollectionCategory.INGREDIENT);
        styleFilterButton(btnCategoryFood, selectedCategory == CharacterCollectionCategory.FOOD);
    }

    private void styleFilterButton(MaterialButton button, boolean selected) {
        if (button == null) return;

        button.setStrokeWidth(0);
        button.setAlpha(selected ? 1f : 0.72f);
    }

    @Override
    public void onUseClicked(CharacterCollectionItem item) {
        if (sellMode) {
            handleSellClicked(item);
            return;
        }

        if (!item.isUnlocked() || !item.isAvailableToUse()) {
            return;
        }

        List<String> instanceKeys = item.getAvailableInstanceKeys();

        if (instanceKeys.size() <= 1) {
            String targetKey = instanceKeys.isEmpty()
                    ? item.getKey()
                    : instanceKeys.get(0);

            applySelectedItem(targetKey, item.getDisplayName(), null);
            return;
        }

        showInstanceChooser(item, instanceKeys);
    }

    private void handleSellClicked(CharacterCollectionItem item) {
        if (item == null) return;

        if (CollectionCatalog.KEY_EGG_BASIC.equals(item.getKey())) {
            ToastUtil.showIosToast(this, "기본 알은 판매할 수 없어요.");
            return;
        }

        if (!item.isUnlocked() || !item.isAvailableToUse()) {
            ToastUtil.showIosToast(this, "보유 중인 캐릭터만 판매할 수 있어요.");
            return;
        }

        if (item.getCategory() == CharacterCollectionCategory.INGREDIENT) {
            List<String> instanceKeys = item.getAvailableInstanceKeys();

            if (instanceKeys == null || instanceKeys.isEmpty()) {
                ToastUtil.showIosToast(this, "현재 메인 펫은 판매할 수 없어요.");
                return;
            }

            if (instanceKeys.size() == 1) {
                String stateKey = instanceKeys.get(0);
                String label = CollectionCatalog.buildInstanceChoiceLabel(this, item.getDisplayName(), stateKey);
                showSellConfirmDialog(item, stateKey, label);
                return;
            }

            showSellInstanceChooser(item, instanceKeys);
            return;
        }

        showSellConfirmDialog(item, item.getKey(), item.getDisplayName());
    }

    private void showSellInstanceChooser(
            CharacterCollectionItem item,
            List<String> instanceKeys
    ) {
        List<String> labels = new ArrayList<>();

        for (String instanceKey : instanceKeys) {
            String label = CollectionCatalog.buildInstanceChoiceLabel(
                    this,
                    item.getDisplayName(),
                    instanceKey
            );

            int price = calculateSellPrice(item, instanceKey);
            labels.add(label + " - " + price + "코인");
        }

        CharSequence[] arr = labels.toArray(new CharSequence[0]);

        new AlertDialog.Builder(this)
                .setTitle(item.getDisplayName() + " 판매하기")
                .setItems(arr, (dialog, which) -> {
                    if (which < 0 || which >= instanceKeys.size()) return;

                    String instanceKey = instanceKeys.get(which);
                    String label = CollectionCatalog.buildInstanceChoiceLabel(
                            this,
                            item.getDisplayName(),
                            instanceKey
                    );

                    showSellConfirmDialog(item, instanceKey, label);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showSellConfirmDialog(
            CharacterCollectionItem item,
            String stateKey,
            String displayLabel
    ) {
        int price = calculateSellPrice(item, stateKey);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sell_confirm, null);
        dialog.setContentView(view);

        TextView tvSellTitle = view.findViewById(R.id.tvSellTitle);
        TextView tvSellName = view.findViewById(R.id.tvSellName);
        ImageView ivSellCharacter = view.findViewById(R.id.ivSellCharacter);
        TextView tvSellPrice = view.findViewById(R.id.tvSellPrice);
        MaterialButton btnCancel = view.findViewById(R.id.btnSellCancel);
        MaterialButton btnSell = view.findViewById(R.id.btnSellConfirm);

        tvSellTitle.setText("판매");
        tvSellName.setText(displayLabel);

        int imageResId = resolveCollectionDisplayImageResId(item);
        if (imageResId != 0) {
            ivSellCharacter.setImageResource(imageResId);
            ivSellCharacter.setVisibility(View.VISIBLE);
        } else {
            ivSellCharacter.setVisibility(View.GONE);
        }

        tvSellPrice.setText(String.valueOf(price));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSell.setOnClickListener(v -> {
            dialog.dismiss();
            sellItem(item, stateKey, displayLabel, price);
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dpToPx(330);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    private int resolveCollectionDisplayImageResId(@NonNull CharacterCollectionItem item) {
        if (item.getImageResId() == 0) {
            return 0;
        }

        String originalName;
        try {
            originalName = getResources().getResourceEntryName(item.getImageResId());
        } catch (Exception e) {
            return item.getImageResId();
        }

        if (originalName == null || originalName.trim().isEmpty()) {
            return item.getImageResId();
        }

        String packageName = getPackageName();

        String trimmedBodyName = originalName;
        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[]{
                trimmedBodyName + "_catalog",
                originalName + "_catalog",
                originalName
        };

        for (String candidate : candidates) {
            int resId = getResources().getIdentifier(candidate, "drawable", packageName);
            if (resId != 0) {
                return resId;
            }
        }

        return item.getImageResId();
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void sellItem(
            CharacterCollectionItem item,
            String stateKey,
            String displayLabel,
            int price
    ) {
        if (item == null || stateKey == null || stateKey.trim().isEmpty()) return;

        CollectionUnlockPrefs.unlock(this, item.getKey());

        boolean removed;

        if (item.getCategory() == CharacterCollectionCategory.INGREDIENT) {
            removed = CollectionInventoryPrefs.removeIngredientInstance(this, stateKey);
            CollectionPetStatePrefs.clearState(this, stateKey);
        } else {
            removed = CollectionInventoryPrefs.hasItem(this, item.getKey());
            CollectionInventoryPrefs.removeItem(this, item.getKey());

            if (CollectionDisplayPrefs.isSelected(this, item.getKey())) {
                CollectionDisplayPrefs.clearSelectedItem(this);
            }

            CollectionPetStatePrefs.clearState(this, item.getKey());
        }

        if (!removed) {
            ToastUtil.showIosToast(this, "판매할 수 없는 캐릭터예요.");
            return;
        }

        addCoinsSafely(price);

        ToastUtil.showIosToast(
                this,
                displayLabel + getSubjectParticle(displayLabel) + " 판매되었어요. +" + price + "코인"
        );

        sellMode = false;
        btnSellMode.setText("판매하기");

        reloadAllItems();
        applyCurrentFilter();

        setResult(RESULT_OK);
    }

    private void showInstanceChooser(
            CharacterCollectionItem item,
            List<String> instanceKeys
    ) {
        List<String> labels = new ArrayList<>();

        for (String instanceKey : instanceKeys) {
            labels.add(
                    CollectionCatalog.buildInstanceChoiceLabel(
                            this,
                            item.getDisplayName(),
                            instanceKey
                    )
            );
        }

        CharSequence[] arr = labels.toArray(new CharSequence[0]);

        new AlertDialog.Builder(this)
                .setTitle(item.getDisplayName() + " 가져오기")
                .setItems(arr, (dialog, which) -> {
                    if (which < 0 || which >= instanceKeys.size()) {
                        return;
                    }

                    String instanceKey = instanceKeys.get(which);

                    applySelectedItem(
                            instanceKey,
                            item.getDisplayName(),
                            labels.get(which)
                    );
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void applySelectedItem(
            String itemKey,
            String displayName,
            @Nullable String label
    ) {
        CollectionDisplayPrefs.saveSelectedItem(this, itemKey);
        CollectionPetStatePrefs.ensureState(this, itemKey);

        String targetName;

        if (label == null || label.trim().isEmpty()) {
            targetName = displayName;
        } else {
            targetName = label;
        }

        ToastUtil.showIosToast(this, buildUseMessage(targetName));

        setResult(RESULT_OK);
        finish();
    }

    private String buildUseMessage(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "홈에 가져왔어요.";
        }

        String trimmedName = name.trim();
        return trimmedName + getObjectParticle(trimmedName) + " 홈에 가져왔어요.";
    }

    private String getObjectParticle(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "를";
        }

        String trimmedText = text.trim();
        char lastChar = trimmedText.charAt(trimmedText.length() - 1);

        if (lastChar < 0xAC00 || lastChar > 0xD7A3) {
            return "를";
        }

        int jong = (lastChar - 0xAC00) % 28;
        return jong == 0 ? "를" : "을";
    }

    private String getSubjectParticle(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "가";
        }

        String trimmedText = text.trim();
        char lastChar = trimmedText.charAt(trimmedText.length() - 1);

        if (lastChar < 0xAC00 || lastChar > 0xD7A3) {
            return "가";
        }

        int jong = (lastChar - 0xAC00) % 28;
        return jong == 0 ? "가" : "이";
    }

    private int calculateSellPrice(
            CharacterCollectionItem item,
            String stateKey
    ) {
        if (item == null || stateKey == null || stateKey.trim().isEmpty()) {
            return 0;
        }

        int level = CollectionPetStatePrefs.getLevel(this, stateKey);

        int basePrice;
        int levelBonus;
        int maxBonus = 200;

        if (item.getCategory() == CharacterCollectionCategory.FOOD) {
            basePrice = 500;
            levelBonus = 80;
        } else {
            basePrice = 100;
            levelBonus = 30;
        }

        int price = basePrice + ((Math.max(1, level) - 1) * levelBonus);

        int maxLevel = CollectionCatalog.KEY_EGG_BASIC.equals(stateKey)
                ? PetPrefs.HATCH_LEVEL
                : PetPrefs.MAX_LEVEL;

        if (level >= maxLevel) {
            price += maxBonus;
        }

        return price;
    }

    private void addCoinsSafely(int amount) {
        if (amount <= 0) return;

        try {
            Class<?> clazz = Class.forName("com.namgyun.tamakitchen.pet.PetPrefs");

            try {
                Method m = clazz.getMethod("addCoins", Context.class, int.class);
                m.invoke(null, this, amount);
                return;
            } catch (Exception ignored) {
            }

            try {
                Method getCoins = clazz.getMethod("getCoins", Context.class);
                Method setCoins = clazz.getMethod("setCoins", Context.class, int.class);

                Object currentObj = getCoins.invoke(null, this);
                int current = (currentObj instanceof Integer) ? (Integer) currentObj : 0;

                setCoins.invoke(null, this, current + amount);
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }
}