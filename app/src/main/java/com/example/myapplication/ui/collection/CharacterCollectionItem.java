package com.namgyun.tamakitchen.ui.collection;

import java.util.ArrayList;
import java.util.List;

public class    CharacterCollectionItem {

    private final String key;
    private final String displayName;
    private final int imageResId;
    private final boolean unlocked;
    private final boolean availableToUse;
    private final CharacterCollectionCategory category;
    private final String levelText;
    private boolean selected;

    private final int ownedCount;
    private final List<String> availableInstanceKeys;

    public CharacterCollectionItem(
            String key,
            String displayName,
            int imageResId,
            boolean unlocked,
            boolean availableToUse,
            CharacterCollectionCategory category,
            String levelText,
            boolean selected,
            int ownedCount,
            List<String> availableInstanceKeys
    ) {
        this.key = key;
        this.displayName = displayName;
        this.imageResId = imageResId;
        this.unlocked = unlocked;
        this.availableToUse = availableToUse;
        this.category = category;
        this.levelText = levelText;
        this.selected = selected;
        this.ownedCount = Math.max(0, ownedCount);
        this.availableInstanceKeys = availableInstanceKeys == null
                ? new ArrayList<>()
                : new ArrayList<>(availableInstanceKeys);
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getImageResId() {
        return imageResId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isAvailableToUse() {
        return availableToUse;
    }

    public CharacterCollectionCategory getCategory() {
        return category;
    }

    public String getLevelText() {
        return levelText;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getOwnedCount() {
        return ownedCount;
    }

    public List<String> getAvailableInstanceKeys() {
        return new ArrayList<>(availableInstanceKeys);
    }

    public boolean hasMultipleInstances() {
        return ownedCount > 1 && !availableInstanceKeys.isEmpty();
    }

    public String getDisplayNameWithCount() {
        if (ownedCount > 1) {
            return displayName + " x" + ownedCount;
        }
        return displayName;
    }
}