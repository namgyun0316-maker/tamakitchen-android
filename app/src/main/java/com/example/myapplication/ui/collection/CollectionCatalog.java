package com.namgyun.tamakitchen.ui.collection;

import android.content.Context;

import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CollectionCatalog {

    public static final String KEY_EGG_BASIC = "egg_basic";

    public static final String KEY_FOOD_STRAWBERRY_CAKE = "food_strawberry_cake";
    public static final String KEY_FOOD_DOOJJON_COOKIE = "food_doojjon_cookie";
    public static final String KEY_FOOD_CHESTNUT_TIRAMISU = "food_chestnut_tiramisu";
    public static final String KEY_FOOD_STRAWBERRY_SHAKE = "food_strawberry_shake";
    public static final String KEY_FOOD_GREEN_GRAPE_ADE = "food_green_grape_ade";
    public static final String KEY_FOOD_STRAWBERRY_ADE = "food_strawberry_ade";
    public static final String KEY_FOOD_COOKIE = "food_cookie";
    public static final String KEY_FOOD_GREEN_ONION_PANCAKE = "food_green_onion_pancake";
    public static final String KEY_FOOD_SWEET_POTATO_MATTANG = "food_sweet_potato_mattang";
    public static final String KEY_FOOD_BANANA_SHAKE = "food_banana_shake";
    public static final String KEY_FOOD_STRAWBERRY_JAM = "food_strawberry_jam";
    public static final String KEY_FOOD_WALNUT_GANGJEONG = "food_walnut_gangjeong";
    public static final String KEY_FOOD_GRAPE_JAM = "food_grape_jam";
    public static final String KEY_FOOD_ROASTED_CHESTNUT = "food_roasted_chestnut";
    public static final String KEY_FOOD_CHOCO_BANANA = "food_choco_banana";
    public static final String KEY_FOOD_CHOCO_MILK = "food_choco_milk";
    public static final String KEY_FOOD_STRAWBERRY_MILK = "food_strawberry_milk";
    public static final String KEY_FOOD_BANANA_MILK = "food_banana_milk";
    public static final String KEY_FOOD_CAFE_LATTE = "food_cafe_latte";
    public static final String KEY_FOOD_BAGUETTE = "food_baguette";
    public static final String KEY_FOOD_ONION_CREAM_SOUP = "food_onion_cream_soup";
    public static final String KEY_FOOD_TOMATO_SPAGHETTI = "food_tomato_spaghetti";
    public static final String KEY_FOOD_BROWNIE = "food_brownie";
    public static final String KEY_FOOD_PANCAKE = "food_pancake";
    public static final String KEY_FOOD_HASH_BROWN = "food_hash_brown";
    public static final String KEY_FOOD_APPLE_JAM = "food_apple_jam";
    public static final String KEY_FOOD_BLUEBERRY_MUFFIN = "food_blueberry_muffin";
    public static final String KEY_FOOD_LEMON_ADE = "food_lemon_ade";
    public static final String KEY_FOOD_MATCHA_LATTE = "food_matcha_latte";

    private CollectionCatalog() {}

    public static List<CharacterCollectionItem> getAllItems(Context context) {
        List<CharacterCollectionItem> list = new ArrayList<>();

        PetPrefs.ensureEggGranted(context);
        PetPrefs.migrateLegacyUnlockedCharacterCountsIfNeeded(context);

        String selectedKey = CollectionDisplayPrefs.getSelectedItemKey(context);

        addEggItem(context, list, selectedKey);
        addIngredientItems(context, list, selectedKey);
        addFoodItems(context, list, selectedKey);

        return list;
    }

    public static CharacterCollectionItem findByKey(Context context, String key) {
        if (key == null || key.trim().isEmpty()) return null;

        String normalizedKey = key;
        if (CollectionInventoryPrefs.isIngredientInstanceKey(key)) {
            String base = CollectionInventoryPrefs.getBaseKeyFromInstanceKey(key);
            if (base != null) {
                normalizedKey = base;
            }
        }

        List<CharacterCollectionItem> allItems = getAllItems(context);
        for (CharacterCollectionItem item : allItems) {
            if (normalizedKey.equals(item.getKey())) {
                return item;
            }
        }
        return null;
    }

    public static String buildInstanceChoiceLabel(Context context, String displayName, String instanceKey) {
        String levelText = formatLevelText(instanceKey, CollectionPetStatePrefs.getLevel(context, instanceKey));
        return displayName + " " + levelText;
    }

    private static void addEggItem(Context context, List<CharacterCollectionItem> list, String selectedKey) {
        int eggResId = resolveDrawableRes(context, "pet_hatch_0000");
        if (eggResId == 0) return;

        boolean isEggStage = PetPrefs.getStage(context) == PetPrefs.STAGE_EGG;
        boolean hasEggCount = PetPrefs.hasEgg(context);

        if (hasEggCount && !isEggStage) {
            PetPrefs.setEggCount(context, 0);
            hasEggCount = false;
        }

        boolean unlocked = true;
        boolean availableToUse = isEggStage && hasEggCount;
        boolean selected = availableToUse && KEY_EGG_BASIC.equals(selectedKey);

        CollectionPetStatePrefs.ensureState(context, KEY_EGG_BASIC);

        list.add(new CharacterCollectionItem(
                KEY_EGG_BASIC,
                "알",
                eggResId,
                unlocked,
                availableToUse,
                CharacterCollectionCategory.INGREDIENT,
                availableToUse
                        ? formatLevelText(KEY_EGG_BASIC, CollectionPetStatePrefs.getLevel(context, KEY_EGG_BASIC))
                        : "",
                selected,
                availableToUse ? 1 : 0,
                new ArrayList<>()
        ));
    }

    private static void addIngredientItems(Context context, List<CharacterCollectionItem> list, String selectedKey) {
        for (IngredientCharacter character : IngredientCharacter.values()) {
            int imageResId = resolveDrawableRes(context, character.getDrawableName());
            if (imageResId == 0) {
                continue;
            }

            List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(context, character.name());
            for (String instanceKey : instanceKeys) {
                CollectionPetStatePrefs.ensureState(context, instanceKey);
            }

            boolean isCurrentMainIngredient =
                    PetPrefs.getStage(context) == PetPrefs.STAGE_INGREDIENT
                            && PetPrefs.getSelectedIngredient(context) == character;

            boolean unlocked =
                    PetPrefs.isCharacterUnlocked(context, character)
                            || !instanceKeys.isEmpty()
                            || isCurrentMainIngredient;

            boolean availableToUse =
                    !instanceKeys.isEmpty()
                            || isCurrentMainIngredient;

            String representativeStateKey =
                    resolveRepresentativeStateKey(context, character.name(), selectedKey, instanceKeys, isCurrentMainIngredient);

            String levelText = availableToUse
                    ? buildIngredientLevelSummary(
                    context,
                    character.name(),
                    representativeStateKey,
                    instanceKeys,
                    isCurrentMainIngredient
            )
                    : "";

            boolean selected = false;
            if (selectedKey != null) {
                if (selectedKey.equals(character.name())) {
                    selected = true;
                } else if (CollectionInventoryPrefs.isIngredientInstanceKey(selectedKey)) {
                    String base = CollectionInventoryPrefs.getBaseKeyFromInstanceKey(selectedKey);
                    selected = character.name().equals(base);
                }
            }

            sortInstanceKeysForChooser(context, instanceKeys);

            int ownedCount = instanceKeys.size();
            if (ownedCount == 0 && isCurrentMainIngredient) {
                ownedCount = 1;
            }

            list.add(new CharacterCollectionItem(
                    character.name(),
                    character.getDisplayName(),
                    imageResId,
                    unlocked,
                    availableToUse,
                    CharacterCollectionCategory.INGREDIENT,
                    levelText,
                    selected,
                    ownedCount,
                    instanceKeys
            ));
        }
    }

    private static void addFoodItems(Context context, List<CharacterCollectionItem> list, String selectedKey) {
        addFoodItem(context, list, KEY_FOOD_STRAWBERRY_CAKE, "딸기케이크", "strawberry_cake_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_DOOJJON_COOKIE, "두쫀쿠", "doojjon_cookie_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_CHESTNUT_TIRAMISU, "밤티라미수", "chestnut_tiramisu_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_STRAWBERRY_SHAKE, "딸기쉐이크", "strawberry_shake_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_GREEN_GRAPE_ADE, "청포도에이드", "green_grape_ade_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_STRAWBERRY_ADE, "딸기에이드", "strawberry_ade_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_COOKIE, "쿠키", "cookie_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_GREEN_ONION_PANCAKE, "파전", "green_onion_pancake_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_SWEET_POTATO_MATTANG, "고구마맛탕", "sweet_potato_mattang_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_BANANA_SHAKE, "바나나쉐이크", "banana_shake_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_STRAWBERRY_JAM, "딸기잼", "strawberry_jam_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_WALNUT_GANGJEONG, "호두강정", "walnut_gangjeong_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_GRAPE_JAM, "포도잼", "grape_jam_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_ROASTED_CHESTNUT, "맛밤", "roasted_chestnut_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_CHOCO_BANANA, "초코바나나", "choco_banana_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_CHOCO_MILK, "초코우유", "choco_milk_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_STRAWBERRY_MILK, "딸기우유", "strawberry_milk_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_BANANA_MILK, "바나나우유", "banana_milk_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_CAFE_LATTE, "카페라떼", "cafe_latte_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_BAGUETTE, "바게트", "baguette_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_ONION_CREAM_SOUP, "어니언크림스프", "onion_cream_soup_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_TOMATO_SPAGHETTI, "토마토스파게티", "tomato_spaghetti_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_BROWNIE, "브라우니", "brownie_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_PANCAKE, "팬케이크", "pancake_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_HASH_BROWN, "해쉬브라운", "hash_brown_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_APPLE_JAM, "사과잼", "apple_jam_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_BLUEBERRY_MUFFIN, "블루베리머핀", "blueberry_muffin_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_LEMON_ADE, "레몬에이드", "lemon_ade_body", selectedKey);
        addFoodItem(context, list, KEY_FOOD_MATCHA_LATTE, "말차라떼", "matcha_latte_body", selectedKey);
    }

    private static void addFoodItem(
            Context context,
            List<CharacterCollectionItem> list,
            String key,
            String displayName,
            String drawableName,
            String selectedKey
    ) {
        int resId = resolveDrawableRes(context, drawableName);
        if (resId == 0) return;

        CollectionPetStatePrefs.ensureState(context, key);

        boolean selected = key.equals(selectedKey);
        boolean discovered = CollectionUnlockPrefs.isUnlocked(context, key);
        boolean owned = CollectionInventoryPrefs.hasItem(context, key);

        boolean unlocked = discovered || owned || selected;
        boolean availableToUse = owned || selected;

        list.add(new CharacterCollectionItem(
                key,
                displayName,
                resId,
                unlocked,
                availableToUse,
                CharacterCollectionCategory.FOOD,
                availableToUse
                        ? formatLevelText(key, CollectionPetStatePrefs.getLevel(context, key))
                        : "",
                selected,
                availableToUse ? 1 : 0,
                new ArrayList<>()
        ));
    }

    private static String resolveRepresentativeStateKey(
            Context context,
            String baseKey,
            String selectedKey,
            List<String> instanceKeys,
            boolean isCurrentMainIngredient
    ) {
        if (selectedKey != null && CollectionInventoryPrefs.isIngredientInstanceKey(selectedKey)) {
            String selectedBase = CollectionInventoryPrefs.getBaseKeyFromInstanceKey(selectedKey);
            if (baseKey.equals(selectedBase) && instanceKeys.contains(selectedKey)) {
                return selectedKey;
            }
        }

        if (instanceKeys != null && !instanceKeys.isEmpty()) {
            String best = instanceKeys.get(0);
            int bestLevel = CollectionPetStatePrefs.getLevel(context, best);

            for (String key : instanceKeys) {
                int lv = CollectionPetStatePrefs.getLevel(context, key);
                if (lv > bestLevel) {
                    bestLevel = lv;
                    best = key;
                }
            }
            return best;
        }

        if (isCurrentMainIngredient) {
            return baseKey;
        }

        return null;
    }

    private static String buildIngredientLevelSummary(
            Context context,
            String baseKey,
            String representativeStateKey,
            List<String> instanceKeys,
            boolean isCurrentMainIngredient
    ) {
        int maxLevel = KEY_EGG_BASIC.equals(baseKey) ? PetPrefs.HATCH_LEVEL : PetPrefs.MAX_LEVEL;

        if (instanceKeys == null || instanceKeys.isEmpty()) {
            if (isCurrentMainIngredient) {
                int mainLevel = PetPrefs.getLevel(context);
                return mainLevel >= maxLevel ? "MAX" : "Lv" + mainLevel;
            }

            if (representativeStateKey == null) return "";
            return formatLevelText(
                    representativeStateKey,
                    CollectionPetStatePrefs.getLevel(context, representativeStateKey)
            );
        }

        int highest = Integer.MIN_VALUE;
        int lowest = Integer.MAX_VALUE;

        for (String key : instanceKeys) {
            int level = CollectionPetStatePrefs.getLevel(context, key);
            highest = Math.max(highest, level);
            lowest = Math.min(lowest, level);
        }

        if (isCurrentMainIngredient) {
            int mainLevel = PetPrefs.getLevel(context);
            highest = Math.max(highest, mainLevel);
            lowest = Math.min(lowest, mainLevel);
        }

        if (highest == Integer.MIN_VALUE) {
            return "";
        }

        if (highest == lowest) {
            return formatLevelText(baseKey, highest);
        }

        if (highest >= maxLevel) {
            return "최고 MAX";
        }

        return "최고 Lv" + highest;
    }

    private static void sortInstanceKeysForChooser(Context context, List<String> instanceKeys) {
        if (instanceKeys == null) return;

        Collections.sort(instanceKeys, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int lv1 = CollectionPetStatePrefs.getLevel(context, o1);
                int lv2 = CollectionPetStatePrefs.getLevel(context, o2);

                if (lv1 != lv2) {
                    return Integer.compare(lv2, lv1);
                }
                return o1.compareTo(o2);
            }
        });
    }

    private static String formatLevelText(String itemKey, int level) {
        int maxLevel = KEY_EGG_BASIC.equals(itemKey) ? PetPrefs.HATCH_LEVEL : PetPrefs.MAX_LEVEL;
        return level >= maxLevel ? "MAX" : "Lv" + level;
    }

    private static int resolveDrawableRes(Context context, String drawableName) {
        return context.getResources().getIdentifier(
                drawableName,
                "drawable",
                context.getPackageName()
        );
    }
}