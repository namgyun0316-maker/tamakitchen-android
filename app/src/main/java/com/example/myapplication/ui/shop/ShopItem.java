package com.namgyun.tamakitchen.ui.shop;

public class ShopItem {

    public static final int TYPE_FEED = 1;
    public static final int TYPE_EGG = 2;
    public static final int TYPE_EXP_POTION = 3;

    public final int type;
    public final String name;
    public final String desc;
    public final int price;
    public final int amount;
    public final int iconRes;

    public ShopItem(int type, String name, String desc, int price, int amount, int iconRes) {
        this.type = type;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.amount = amount;
        this.iconRes = iconRes;
    }
}