package com.namgyun.tamakitchen.ui.shop;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.PetExpManager;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity implements ShopItemAdapter.Listener {

    private static final int FEED_UNIT_PRICE = 1;
    private static final int EGG_UNIT_PRICE = 300;

    private static final int EXP_POTION_UNIT_PRICE = 100;
    private static final int EXP_POTION_EXP_REWARD = 30;

    private TextView tvCoinsValue;
    private RecyclerView rvShop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        tvCoinsValue = findViewById(R.id.tvCoinsValue);
        rvShop = findViewById(R.id.rvShop);

        rvShop.setLayoutManager(new LinearLayoutManager(this));

        List<ShopItem> items = new ArrayList<>();

        items.add(new ShopItem(
                ShopItem.TYPE_FEED,
                "먹이",
                "1개당 " + FEED_UNIT_PRICE + "코인",
                FEED_UNIT_PRICE,
                1,
                R.drawable.feed_bag
        ));

        items.add(new ShopItem(
                ShopItem.TYPE_EXP_POTION,
                "경험치 포션",
                "1개당 EXP " + EXP_POTION_EXP_REWARD + " 획득",
                EXP_POTION_UNIT_PRICE,
                EXP_POTION_EXP_REWARD,
                R.drawable.exp_potion
        ));

        items.add(new ShopItem(
                ShopItem.TYPE_EGG,
                "알",
                "1개만 보유 가능한 특별한 알",
                EGG_UNIT_PRICE,
                1,
                R.drawable.shop_egg
        ));

        rvShop.setAdapter(new ShopItemAdapter(items, this));

        normalizeEggOwnershipStateIfNeeded();
        refreshCoins();
    }

    @Override
    protected void onResume() {
        super.onResume();
        normalizeEggOwnershipStateIfNeeded();
        refreshCoins();
    }

    private void refreshCoins() {
        int coins = PetPrefs.getCoins(this);
        tvCoinsValue.setText(String.valueOf(coins));
    }

    @Override
    public void onBuyClicked(ShopItem item) {
        if (item.type == ShopItem.TYPE_EGG && hasAnyOwnedEgg()) {
            AppToast.show(this, "알은 1개만 보유할 수 있어요!");
            return;
        }

        showQuantityDialog(item);
    }

    private void showQuantityDialog(ShopItem item) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_buy_quantity, null);

        TextView tvItemName = dialogView.findViewById(R.id.tvItemName);
        TextView tvUnitPrice = dialogView.findViewById(R.id.tvUnitPrice);
        TextView tvQty = dialogView.findViewById(R.id.tvQty);
        TextView tvTotalPrice = dialogView.findViewById(R.id.tvTotalPrice);

        RoundQtyButton btnMinus = dialogView.findViewById(R.id.btnMinus);
        RoundQtyButton btnPlus = dialogView.findViewById(R.id.btnPlus);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnBuy = dialogView.findViewById(R.id.btnBuy);

        final int unitPrice = item.price;
        final boolean isEggItem = item.type == ShopItem.TYPE_EGG;

        tvItemName.setText(item.name);
        tvUnitPrice.setText("단가: " + unitPrice + " 코인");

        btnMinus.setSymbol("−");
        btnPlus.setSymbol("+");

        int[] qty = new int[]{1};
        tvQty.setText(String.valueOf(qty[0]));
        tvTotalPrice.setText(String.valueOf(qty[0] * unitPrice));

        btnMinus.setOnClickListener(v -> {
            if (qty[0] > 1) {
                qty[0]--;
            }
            tvQty.setText(String.valueOf(qty[0]));
            tvTotalPrice.setText(String.valueOf(qty[0] * unitPrice));
        });

        btnPlus.setOnClickListener(v -> {
            if (isEggItem) {
                AppToast.show(this, "알은 한 번에 1개만 구매할 수 있어요!");
                return;
            }

            qty[0]++;
            tvQty.setText(String.valueOf(qty[0]));
            tvTotalPrice.setText(String.valueOf(qty[0] * unitPrice));
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnBuy.setOnClickListener(v -> {
            int count = isEggItem ? 1 : qty[0];
            int totalCost = count * unitPrice;

            normalizeEggOwnershipStateIfNeeded();

            if (isEggItem && hasAnyOwnedEgg()) {
                AppToast.show(this, "이미 알을 보유하고 있어요!");
                return;
            }

            boolean ok = PetPrefs.spendCoins(this, totalCost);
            if (!ok) {
                AppToast.show(this, "코인이 부족해요! (필요: " + totalCost + ")");
                refreshCoins();
                return;
            }

            if (item.type == ShopItem.TYPE_FEED) {
                PetPrefs.addFeed(this, count);
                AppToast.show(this, "먹이 " + count + "개 구매 완료!");

            } else if (item.type == ShopItem.TYPE_EXP_POTION) {
                int totalExp = EXP_POTION_EXP_REWARD * count;
                PetExpManager.giveExp(this, totalExp);
                AppToast.show(this, "경험치 포션 " + count + "개 사용 완료!");

            } else if (item.type == ShopItem.TYPE_EGG) {
                boolean eggAdded = PetPrefs.addEgg(this, 1);

                if (!eggAdded) {
                    PetPrefs.addCoins(this, totalCost);
                    AppToast.show(this, "알은 1개만 보유할 수 있어요!");
                    refreshCoins();
                    return;
                }

                AppToast.show(this, "알 1개 구매 완료!");
            }

            refreshCoins();
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void normalizeEggOwnershipStateIfNeeded() {
        boolean hasShopEggCount = PetPrefs.hasEgg(this);
        boolean hasCollectionEgg = CollectionInventoryPrefs.hasItem(this, CollectionCatalog.KEY_EGG_BASIC);
        boolean isEggStage = PetPrefs.getStage(this) == PetPrefs.STAGE_EGG;

        if (hasShopEggCount && !isEggStage && !hasCollectionEgg) {
            PetPrefs.setEggCount(this, 0);
        }
    }

    private boolean hasAnyOwnedEgg() {
        normalizeEggOwnershipStateIfNeeded();

        boolean hasShopEggCount = PetPrefs.hasEgg(this);
        boolean hasCollectionEgg = CollectionInventoryPrefs.hasItem(this, CollectionCatalog.KEY_EGG_BASIC);
        boolean isEggStage = PetPrefs.getStage(this) == PetPrefs.STAGE_EGG;

        return hasCollectionEgg || (hasShopEggCount && isEggStage);
    }
}