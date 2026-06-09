package com.namgyun.tamakitchen.ui.fusion;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionUnlockPrefs;

import java.util.List;

public final class FusionDebugReporter {

    private static final String TAG = "FusionDebug";

    private FusionDebugReporter() {
    }

    public static void print(@NonNull Context context) {
        Log.d(TAG, "==================== FUSION DEBUG START ====================");
        Log.d(TAG, "mainStage=" + PetPrefs.getStage(context)
                + ", mainLevel=" + PetPrefs.getLevel(context)
                + ", mainSelectedIngredient=" + getMainIngredientName(context));

        List<CharacterCollectionItem> catalogItems = CollectionCatalog.getAllItems(context);

        for (IngredientCharacter character : IngredientCharacter.values()) {
            String baseKeyByName = character.name();
            String baseKeyById = character.getId();

            boolean unlockedByPetPrefs = PetPrefs.isCharacterUnlocked(context, character);
            boolean unlockedByCollectionPrefs = CollectionUnlockPrefs.isUnlocked(context, baseKeyByName);

            boolean hasNameState = CollectionPetStatePrefs.hasState(context, baseKeyByName);
            int nameLevel = hasNameState ? CollectionPetStatePrefs.getLevel(context, baseKeyByName) : -1;
            int nameExp = hasNameState ? CollectionPetStatePrefs.getExpPercent(context, baseKeyByName) : -1;
            int nameHunger = hasNameState ? CollectionPetStatePrefs.getHunger(context, baseKeyByName) : -1;

            boolean hasIdState = CollectionPetStatePrefs.hasState(context, baseKeyById);
            int idLevel = hasIdState ? CollectionPetStatePrefs.getLevel(context, baseKeyById) : -1;
            int idExp = hasIdState ? CollectionPetStatePrefs.getExpPercent(context, baseKeyById) : -1;
            int idHunger = hasIdState ? CollectionPetStatePrefs.getHunger(context, baseKeyById) : -1;

            List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(context, baseKeyByName);
            int instanceCount = instanceKeys != null ? instanceKeys.size() : 0;

            StringBuilder instanceInfo = new StringBuilder();
            if (instanceKeys != null) {
                for (String key : instanceKeys) {
                    int lv = CollectionPetStatePrefs.getLevel(context, key);
                    int exp = CollectionPetStatePrefs.getExpPercent(context, key);
                    int hunger = CollectionPetStatePrefs.getHunger(context, key);
                    instanceInfo.append("[")
                            .append(key)
                            .append(" lv=").append(lv)
                            .append(", exp=").append(exp)
                            .append(", hunger=").append(hunger)
                            .append("] ");
                }
            }

            CharacterCollectionItem catalogItem = findCatalogItem(catalogItems, baseKeyByName);
            String catalogLevelText = catalogItem != null ? catalogItem.getLevelText() : "null";
            boolean catalogUnlocked = catalogItem != null && catalogItem.isUnlocked();
            boolean catalogAvailable = catalogItem != null && catalogItem.isAvailableToUse();
            int catalogOwnedCount = catalogItem != null ? catalogItem.getOwnedCount() : -1;

            boolean isMainSelected =
                    PetPrefs.getSelectedIngredient(context) == character
                            && PetPrefs.getStage(context) == PetPrefs.STAGE_INGREDIENT;

            Log.d(TAG,
                    "character=" + character.name()
                            + " (" + character.getDisplayName() + ")"
                            + "\n  catalog: unlocked=" + catalogUnlocked
                            + ", available=" + catalogAvailable
                            + ", ownedCount=" + catalogOwnedCount
                            + ", levelText=" + catalogLevelText
                            + "\n  unlocked: petPrefs=" + unlockedByPetPrefs
                            + ", collectionUnlockPrefs=" + unlockedByCollectionPrefs
                            + "\n  state(name): exists=" + hasNameState
                            + ", level=" + nameLevel
                            + ", exp=" + nameExp
                            + ", hunger=" + nameHunger
                            + "\n  state(id): exists=" + hasIdState
                            + ", level=" + idLevel
                            + ", exp=" + idExp
                            + ", hunger=" + idHunger
                            + "\n  mainSelectedEligible=" + (isMainSelected && PetPrefs.getLevel(context) >= PetPrefs.FUSION_LEVEL)
                            + "\n  instances: count=" + instanceCount
                            + ", data=" + instanceInfo
            );
        }

        Log.d(TAG, "==================== FUSION DEBUG END ======================");
    }

    private static CharacterCollectionItem findCatalogItem(
            List<CharacterCollectionItem> items,
            String key
    ) {
        if (items == null || key == null) return null;

        for (CharacterCollectionItem item : items) {
            if (item == null) continue;
            if (key.equals(item.getKey())) {
                return item;
            }
        }
        return null;
    }

    private static String getMainIngredientName(Context context) {
        IngredientCharacter selected = PetPrefs.getSelectedIngredient(context);
        return selected == null ? "null" : selected.name() + "/" + selected.getId();
    }
}