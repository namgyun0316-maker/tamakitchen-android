package com.namgyun.tamakitchen.ui.fusion;

import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;

public enum FusionFood {

    STRAWBERRY_CAKE(
            "strawberry_cake",
            "딸기케이크",
            "strawberry_cake_body",
            CollectionCatalog.KEY_FOOD_STRAWBERRY_CAKE
    ),

    DDU_JJON_KKU(
            "doojjon_cookie",
            "두쫀쿠",
            "doojjon_cookie_body",
            CollectionCatalog.KEY_FOOD_DOOJJON_COOKIE
    ),

    CHESTNUT_TIRAMISU(
            "chestnut_tiramisu",
            "밤티라미수",
            "chestnut_tiramisu_body",
            CollectionCatalog.KEY_FOOD_CHESTNUT_TIRAMISU
    ),

    STRAWBERRY_SHAKE(
            "strawberry_shake",
            "딸기쉐이크",
            "strawberry_shake_body",
            CollectionCatalog.KEY_FOOD_STRAWBERRY_SHAKE
    ),

    GREEN_GRAPE_ADE(
            "green_grape_ade",
            "청포도에이드",
            "green_grape_ade_body",
            CollectionCatalog.KEY_FOOD_GREEN_GRAPE_ADE
    ),

    STRAWBERRY_ADE(
            "strawberry_ade",
            "딸기에이드",
            "strawberry_ade_body",
            CollectionCatalog.KEY_FOOD_STRAWBERRY_ADE
    ),

    COOKIE(
            "cookie",
            "쿠키",
            "cookie_body",
            CollectionCatalog.KEY_FOOD_COOKIE
    ),

    PAJEON(
            "green_onion_pancake",
            "파전",
            "green_onion_pancake_body",
            CollectionCatalog.KEY_FOOD_GREEN_ONION_PANCAKE
    ),

    SWEET_POTATO_MATANG(
            "sweet_potato_mattang",
            "고구마맛탕",
            "sweet_potato_mattang_body",
            CollectionCatalog.KEY_FOOD_SWEET_POTATO_MATTANG
    ),

    BANANA_SHAKE(
            "banana_shake",
            "바나나쉐이크",
            "banana_shake_body",
            CollectionCatalog.KEY_FOOD_BANANA_SHAKE
    ),

    STRAWBERRY_JAM(
            "strawberry_jam",
            "딸기잼",
            "strawberry_jam_body",
            CollectionCatalog.KEY_FOOD_STRAWBERRY_JAM
    ),

    WALNUT_GANGJEONG(
            "walnut_gangjeong",
            "호두강정",
            "walnut_gangjeong_body",
            CollectionCatalog.KEY_FOOD_WALNUT_GANGJEONG
    ),

    GRAPE_JAM(
            "grape_jam",
            "포도잼",
            "grape_jam_body",
            CollectionCatalog.KEY_FOOD_GRAPE_JAM
    ),

    SWEET_CHESTNUT(
            "roasted_chestnut",
            "맛밤",
            "roasted_chestnut_body",
            CollectionCatalog.KEY_FOOD_ROASTED_CHESTNUT
    ),

    CHOCO_BANANA(
            "choco_banana",
            "초코바나나",
            "choco_banana_body",
            CollectionCatalog.KEY_FOOD_CHOCO_BANANA
    ),

    CHOCO_MILK(
            "chocolate_milk",
            "초코우유",
            "chocolate_milk_body",
            CollectionCatalog.KEY_FOOD_CHOCO_MILK
    ),

    STRAWBERRY_MILK(
            "strawberry_milk",
            "딸기우유",
            "strawberry_milk_body",
            CollectionCatalog.KEY_FOOD_STRAWBERRY_MILK
    ),

    BANANA_MILK(
            "banana_milk",
            "바나나우유",
            "banana_milk_body",
            CollectionCatalog.KEY_FOOD_BANANA_MILK
    ),

    CAFE_LATTE(
            "cafe_latte",
            "카페라떼",
            "cafe_latte_body",
            CollectionCatalog.KEY_FOOD_CAFE_LATTE
    ),

    BAGUETTE(
            "baguette",
            "바게트",
            "baguette_body",
            CollectionCatalog.KEY_FOOD_BAGUETTE
    ),

    ONION_CREAM_SOUP(
            "onion_cream_soup",
            "어니언크림스프",
            "onion_cream_soup_body",
            CollectionCatalog.KEY_FOOD_ONION_CREAM_SOUP
    ),

    TOMATO_SPAGHETTI(
            "tomato_spaghetti",
            "토마토스파게티",
            "tomato_spaghetti_body",
            CollectionCatalog.KEY_FOOD_TOMATO_SPAGHETTI
    ),

    BROWNIE(
            "brownie",
            "브라우니",
            "brownie_body",
            CollectionCatalog.KEY_FOOD_BROWNIE
    ),

    PANCAKE(
            "pancake",
            "팬케이크",
            "pancake_body",
            CollectionCatalog.KEY_FOOD_PANCAKE
    ),

    HASH_BROWN(
            "hash_brown",
            "해쉬브라운",
            "hash_brown_body",
            CollectionCatalog.KEY_FOOD_HASH_BROWN
    ),

    APPLE_JAM(
            "apple_jam",
            "사과잼",
            "apple_jam_body",
            CollectionCatalog.KEY_FOOD_APPLE_JAM
    ),

    BLUEBERRY_MUFFIN(
            "blueberry_muffin",
            "블루베리머핀",
            "blueberry_muffin_body",
            CollectionCatalog.KEY_FOOD_BLUEBERRY_MUFFIN
    ),

    LEMON_ADE(
            "lemon_ade",
            "레몬에이드",
            "lemon_ade_body",
            CollectionCatalog.KEY_FOOD_LEMON_ADE
    ),

    MATCHA_LATTE(
            "matcha_latte",
            "말차라떼",
            "matcha_latte_body",
            CollectionCatalog.KEY_FOOD_MATCHA_LATTE
    );

    private final String id;
    private final String displayName;
    private final String drawableName;
    private final String collectionKey;

    FusionFood(String id, String displayName, String drawableName, String collectionKey) {
        this.id = id;
        this.displayName = displayName;
        this.drawableName = drawableName;
        this.collectionKey = collectionKey;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDrawableName() {
        return drawableName;
    }

    public String getCollectionKey() {
        return collectionKey;
    }

    public static FusionFood fromId(String id) {
        if (id == null) return null;

        for (FusionFood value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return null;
    }
}