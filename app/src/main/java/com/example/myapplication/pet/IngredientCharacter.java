package com.namgyun.tamakitchen.pet;

public enum IngredientCharacter {
    GREEN_GRAPE("green_grape", "청포도", "green_grape_body"),
    SWEET_POTATO("sweet_potato", "고구마", "sweet_potato_body"),
    STRAWBERRY("strawberry", "딸기", "strawberry_body"),
    FLOUR("flour", "밀가루", "flour_body"),
    SUGAR("sugar", "설탕", "sugar_cube_body"),
    BANANA("banana", "바나나", "banana_body"),
    ONION("onion", "양파", "onion_body"),
    GREEN_ONION("green_onion", "대파", "green_onion_body"),
    WALNUT("walnut", "호두", "walnut_body"),
    PISTACHIO("pistachio", "피스타치오", "pistachio_body"),
    TOMATO("tomato", "토마토", "tomato_body"),
    BLUEBERRY("blueberry", "블루베리", "blueberry_body"),

    CHOCOLATE("chocolate", "초콜릿", "chocolate_body"),
    CHESTNUT("chestnut", "밤", "chestnut_body"),
    COFFEE_BEAN("coffee_bean", "커피원두", "coffee_bean_body"),
    MILK("milk", "우유", "milk_body"),
    SPARKLING_WATER("sparkling_water", "탄산수", "sparkling_water_body"),
    // EGG("egg", "계란", "egg_body"),
    BUTTER("butter", "버터", "butter_body"),
    POTATO("potato", "감자", "potato_body"),
    APPLE("apple", "사과", "apple_body"),
    LEMON("lemon", "레몬", "lemon_body"),
    GREEN_TEA("green_tea", "녹차", "green_tea_body"),
    PASTA_NOODLE("pasta_noodle", "파스타면", "pasta_noodle_body"),

    CHOCOLATE_MILK("chocolate_milk", "초코우유", "chocolate_milk_body");

    private final String id;
    private final String displayName;
    private final String drawableName;

    IngredientCharacter(String id, String displayName, String drawableName) {
        this.id = id;
        this.displayName = displayName;
        this.drawableName = drawableName;
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

    public static IngredientCharacter fromId(String id) {
        if (id == null) return null;

        for (IngredientCharacter value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return null;
    }
}