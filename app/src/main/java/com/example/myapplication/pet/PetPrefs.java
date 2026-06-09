package com.namgyun.tamakitchen.pet;

import android.content.Context;
import android.content.SharedPreferences;

import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionUnlockPrefs;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PetPrefs {

    private static final String PREF = "pet_prefs";

    private static final String KEY_EGG_GRANTED = "egg_granted";
    private static final String KEY_STAGE = "pet_stage";
    private static final String KEY_LEVEL = "pet_level";
    private static final String KEY_EXP = "pet_exp";
    private static final String KEY_COINS = "pet_coins";

    private static final String KEY_HUNGER = "pet_hunger";
    private static final String KEY_LAST_HUNGER_UPDATE_MS = "last_hunger_update_ms";

    private static final String KEY_FEED_COUNT = "feed_count";
    private static final String KEY_EGG_COUNT = "egg_count";

    private static final String KEY_HUNGER_OFFSET_MS = "hunger_offset_ms";
    private static final String KEY_MEAL_ANCHOR_MS = "meal_anchor_ms";

    private static final String KEY_HATCH_PLAYED = "hatch_played";
    private static final String KEY_SELECTED_INGREDIENT_ID = "selected_ingredient_id";
    private static final String KEY_UNLOCKED_CHARACTERS = "unlocked_characters";
    private static final String KEY_CHARACTER_COUNTS = "character_counts";
    private static final String KEY_FUSION_GUIDE_SHOWN = "fusion_guide_shown";

    public static final int STAGE_EGG = 0;
    public static final int STAGE_INGREDIENT = 1;
    public static final int STAGE_FOOD = 2;

    public static final int HATCH_LEVEL = 11;
    public static final int FUSION_LEVEL = 20;
    public static final int MAX_LEVEL = 21;

    private static final long HUNGER_FULL_DECAY_MS =
            5L * 60L * 60L * 1000L;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void ensureEggGranted(Context c) {
        SharedPreferences s = sp(c);
        if (s.getBoolean(KEY_EGG_GRANTED, false)) return;

        long now = System.currentTimeMillis();

        s.edit()
                .putBoolean(KEY_EGG_GRANTED, true)
                .putInt(KEY_STAGE, STAGE_EGG)
                .putInt(KEY_LEVEL, 1)
                .putInt(KEY_EXP, 0)
                .putInt(KEY_COINS, 0)
                .putInt(KEY_HUNGER, 100)
                .putLong(KEY_LAST_HUNGER_UPDATE_MS, now)
                .putInt(KEY_FEED_COUNT, 10)
                .putInt(KEY_EGG_COUNT, 0)
                .putLong(KEY_HUNGER_OFFSET_MS, 0L)
                .putLong(KEY_MEAL_ANCHOR_MS, 0L)
                .putBoolean(KEY_HATCH_PLAYED, false)
                .remove(KEY_SELECTED_INGREDIENT_ID)
                .putStringSet(KEY_UNLOCKED_CHARACTERS, new HashSet<>())
                .putStringSet(KEY_CHARACTER_COUNTS, new HashSet<>())
                .putStringSet(KEY_FUSION_GUIDE_SHOWN, new HashSet<>())
                .apply();
    }

    public static int getCoins(Context c) {
        return sp(c).getInt(KEY_COINS, 0);
    }

    public static void setCoins(Context c, int value) {
        sp(c).edit().putInt(KEY_COINS, Math.max(0, value)).apply();
    }

    public static void addCoins(Context c, int delta) {
        int v = Math.max(0, getCoins(c) + delta);
        sp(c).edit().putInt(KEY_COINS, v).apply();
    }

    public static boolean spendCoins(Context c, int cost) {
        int coins = getCoins(c);
        if (coins < cost) return false;
        setCoins(c, coins - cost);
        return true;
    }

    public static boolean hasShownFusionGuide(Context c, String key) {
        if (c == null || key == null || key.trim().isEmpty()) return false;
        Set<String> saved = sp(c).getStringSet(KEY_FUSION_GUIDE_SHOWN, new HashSet<>());
        return saved != null && saved.contains(key);
    }

    public static void markFusionGuideShown(Context c, String key) {
        if (c == null || key == null || key.trim().isEmpty()) return;
        SharedPreferences s = sp(c);
        Set<String> saved = new HashSet<>(s.getStringSet(KEY_FUSION_GUIDE_SHOWN, new HashSet<>()));
        saved.add(key);
        s.edit().putStringSet(KEY_FUSION_GUIDE_SHOWN, saved).apply();
    }

    public static boolean isMainEggHatchPending(Context c) {
        if (c == null) return false;

        SharedPreferences s = sp(c);
        boolean hatchPlayed = s.getBoolean(KEY_HATCH_PLAYED, false);
        int rawLevel = clamp(s.getInt(KEY_LEVEL, 1), 1, MAX_LEVEL);

        return !hatchPlayed && rawLevel >= HATCH_LEVEL;
    }

    public static int getLevel(Context c) {
        SharedPreferences s = sp(c);
        int rawLevel = clamp(s.getInt(KEY_LEVEL, 1), 1, MAX_LEVEL);
        boolean hatchPlayed = s.getBoolean(KEY_HATCH_PLAYED, false);

        if (!hatchPlayed && rawLevel > HATCH_LEVEL) {
            s.edit()
                    .putInt(KEY_LEVEL, HATCH_LEVEL)
                    .putInt(KEY_EXP, getNeedExpForLevel(HATCH_LEVEL))
                    .apply();
            return HATCH_LEVEL;
        }

        if (isMainEggHatchPending(c)) {
            return HATCH_LEVEL;
        }

        return rawLevel;
    }

    public static int getExp(Context c) {
        if (isMainEggHatchPending(c)) {
            return getNeedExpForLevel(HATCH_LEVEL);
        }

        int level = getLevel(c);
        if (level >= MAX_LEVEL) return 0;

        return Math.max(0, sp(c).getInt(KEY_EXP, 0));
    }

    public static int getNeedExpForLevel(int level) {
        if (level >= MAX_LEVEL) return 0;
        if (level <= 1) return 100;
        return 100 + ((level - 1) * 10);
    }

    public static int getCurrentLevelNeedExp(Context c) {
        if (isMainEggHatchPending(c)) {
            return getNeedExpForLevel(HATCH_LEVEL);
        }
        return getNeedExpForLevel(getLevel(c));
    }

    public static int getCurrentLevelExpPercent(Context c) {
        if (isMainEggHatchPending(c)) {
            return 100;
        }

        int level = getLevel(c);
        if (level >= MAX_LEVEL) return 100;

        int exp = getExp(c);
        int need = getCurrentLevelNeedExp(c);
        if (need <= 0) return 0;

        int percent = (int) Math.round((exp * 100.0) / need);
        return clamp(percent, 0, 100);
    }

    public static boolean addExpAndMaybeLevelUp(Context c, int delta) {
        SharedPreferences s = sp(c);

        boolean hatchPlayed = s.getBoolean(KEY_HATCH_PLAYED, false);
        int level = getLevel(c);
        int exp = Math.max(0, s.getInt(KEY_EXP, 0));

        if (!hatchPlayed) {
            exp += Math.max(0, delta);
            boolean leveledUp = false;

            while (level < HATCH_LEVEL) {
                int need = getNeedExpForLevel(level);
                if (need <= 0 || exp < need) break;

                exp -= need;
                level++;
                leveledUp = true;
            }

            if (level >= HATCH_LEVEL) {
                level = HATCH_LEVEL;
                exp = getNeedExpForLevel(HATCH_LEVEL);
                leveledUp = true;
            }

            s.edit()
                    .putInt(KEY_LEVEL, level)
                    .putInt(KEY_EXP, exp)
                    .putInt(KEY_STAGE, STAGE_EGG)
                    .apply();

            return leveledUp;
        }

        if (level >= MAX_LEVEL) {
            s.edit()
                    .putInt(KEY_LEVEL, MAX_LEVEL)
                    .putInt(KEY_EXP, 0)
                    .apply();
            syncStageWithLevel(c);
            ensureIngredientSelectedIfNeeded(c);
            unlockSelectedCharacterIfNeeded(c);
            return false;
        }

        exp += Math.max(0, delta);
        boolean leveledUp = false;

        while (level < MAX_LEVEL) {
            int need = getNeedExpForLevel(level);
            if (need <= 0 || exp < need) break;

            exp -= need;
            level++;
            leveledUp = true;

            if (level >= MAX_LEVEL) {
                level = MAX_LEVEL;
                exp = 0;
                break;
            }
        }

        s.edit()
                .putInt(KEY_LEVEL, level)
                .putInt(KEY_EXP, level >= MAX_LEVEL ? 0 : exp)
                .apply();

        syncStageWithLevel(c);
        ensureIngredientSelectedIfNeeded(c);
        unlockSelectedCharacterIfNeeded(c);
        return leveledUp;
    }

    public static int debugLevelUp(Context c) {
        SharedPreferences s = sp(c);
        boolean hatchPlayed = s.getBoolean(KEY_HATCH_PLAYED, false);
        int currentLevel = getLevel(c);

        if (!hatchPlayed) {
            int nextLevel = Math.min(HATCH_LEVEL, currentLevel + 1);
            int nextExp = 0;

            if (nextLevel >= HATCH_LEVEL) {
                nextLevel = HATCH_LEVEL;
                nextExp = getNeedExpForLevel(HATCH_LEVEL);
            }

            s.edit()
                    .putInt(KEY_LEVEL, nextLevel)
                    .putInt(KEY_EXP, nextExp)
                    .putInt(KEY_STAGE, STAGE_EGG)
                    .apply();

            return nextLevel;
        }

        int nextLevel = Math.min(MAX_LEVEL, currentLevel + 1);

        s.edit()
                .putInt(KEY_LEVEL, nextLevel)
                .putInt(KEY_EXP, 0)
                .apply();

        syncStageWithLevel(c);
        ensureIngredientSelectedIfNeeded(c);
        unlockSelectedCharacterIfNeeded(c);

        return nextLevel;
    }

    public static int debugLevelDown(Context c) {
        SharedPreferences s = sp(c);

        int currentLevel = getLevel(c);
        int nextLevel = Math.max(1, currentLevel - 1);

        s.edit()
                .putInt(KEY_LEVEL, nextLevel)
                .putInt(KEY_EXP, 0)
                .putBoolean(KEY_HATCH_PLAYED, false)
                .putInt(KEY_STAGE, STAGE_EGG)
                .remove(KEY_SELECTED_INGREDIENT_ID)
                .apply();

        return nextLevel;
    }

    public static int getStage(Context c) {
        if (isMainEggHatchPending(c)) {
            return STAGE_EGG;
        }

        SharedPreferences s = sp(c);
        boolean hatchPlayed = s.getBoolean(KEY_HATCH_PLAYED, false);

        if (!hatchPlayed) {
            return STAGE_EGG;
        }

        return s.getInt(KEY_STAGE, STAGE_EGG);
    }

    public static void setStage(Context c, int stage) {
        int safe = clamp(stage, STAGE_EGG, STAGE_FOOD);
        sp(c).edit().putInt(KEY_STAGE, safe).apply();
    }

    public static boolean syncStageWithLevel(Context c) {
        SharedPreferences s = sp(c);
        int before = s.getInt(KEY_STAGE, STAGE_EGG);
        boolean hatchPlayed = s.getBoolean(KEY_HATCH_PLAYED, false);

        int after = (!hatchPlayed) ? STAGE_EGG : STAGE_INGREDIENT;

        boolean changed = before != after;
        if (changed) {
            s.edit().putInt(KEY_STAGE, after).apply();
        }
        return changed;
    }

    public static void applyPostHatchInitialState(Context c) {
        if (c == null) return;

        SharedPreferences s = sp(c);
        long nowMs = System.currentTimeMillis();

        s.edit()
                .putInt(KEY_STAGE, STAGE_INGREDIENT)
                .putInt(KEY_LEVEL, HATCH_LEVEL)
                .putInt(KEY_EXP, 0)
                .putInt(KEY_HUNGER, 100)
                .putLong(KEY_HUNGER_OFFSET_MS, 0L)
                .putLong(KEY_MEAL_ANCHOR_MS, 0L)
                .putLong(KEY_LAST_HUNGER_UPDATE_MS, nowMs)
                .putBoolean(KEY_HATCH_PLAYED, true)
                .apply();

        ChecklistPrefs.addHatchCount(c);

        ensureIngredientSelectedIfNeeded(c);
        unlockSelectedCharacterIfNeeded(c);
    }

    public static boolean shouldPlayHatchAnimation(Context c) {
        return isMainEggHatchPending(c);
    }

    public static void markHatchAnimationPlayed(Context c) {
        sp(c).edit().putBoolean(KEY_HATCH_PLAYED, true).apply();
    }

    public static IngredientCharacter getRandomAvailableIngredientForHatch(Context c) {
        IngredientCharacter[] values = IngredientCharacter.values();
        if (values.length == 0) return null;

        List<IngredientCharacter> neverOwned = new ArrayList<>();
        for (IngredientCharacter character : IngredientCharacter.values()) {
            boolean hasAny = CollectionInventoryPrefs.hasIngredientInstance(c, character.name());
            boolean unlocked = isCharacterUnlocked(c, character);

            if (!hasAny && !unlocked) {
                neverOwned.add(character);
            }
        }

        Random random = new Random();

        if (!neverOwned.isEmpty()) {
            return neverOwned.get(random.nextInt(neverOwned.size()));
        }

        List<IngredientCharacter> notCurrentlyOwned = new ArrayList<>();
        for (IngredientCharacter character : IngredientCharacter.values()) {
            if (!CollectionInventoryPrefs.hasIngredientInstance(c, character.name())) {
                notCurrentlyOwned.add(character);
            }
        }

        if (!notCurrentlyOwned.isEmpty()) {
            return notCurrentlyOwned.get(random.nextInt(notCurrentlyOwned.size()));
        }

        return values[random.nextInt(values.length)];
    }

    public static void ensureIngredientSelectedIfNeeded(Context c) {
        if (getLevel(c) < HATCH_LEVEL) return;

        SharedPreferences s = sp(c);
        String current = s.getString(KEY_SELECTED_INGREDIENT_ID, null);
        if (current != null && !current.isEmpty()) return;

        IngredientCharacter picked = getRandomAvailableIngredientForHatch(c);
        if (picked == null) return;

        s.edit().putString(KEY_SELECTED_INGREDIENT_ID, picked.getId()).apply();
    }

    public static IngredientCharacter getSelectedIngredient(Context c) {
        String id = sp(c).getString(KEY_SELECTED_INGREDIENT_ID, null);
        return IngredientCharacter.fromId(id);
    }

    public static void forceSelectIngredient(Context c, IngredientCharacter ingredient) {
        if (ingredient == null) return;
        sp(c).edit().putString(KEY_SELECTED_INGREDIENT_ID, ingredient.getId()).apply();
    }

    public static void unlockCharacter(Context c, IngredientCharacter character) {
        if (c == null || character == null) return;

        SharedPreferences s = sp(c);
        Set<String> saved = new HashSet<>(s.getStringSet(KEY_UNLOCKED_CHARACTERS, new HashSet<>()));
        saved.add(character.name());

        s.edit().putStringSet(KEY_UNLOCKED_CHARACTERS, saved).apply();
        CollectionUnlockPrefs.unlock(c, character.name());
    }

    public static void unlockSelectedCharacterIfNeeded(Context c) {
        if (c == null) return;

        if (getStage(c) == STAGE_EGG) return;

        IngredientCharacter selected = getSelectedIngredient(c);
        if (selected == null) return;

        unlockCharacter(c, selected);
    }

    public static boolean isCharacterUnlocked(Context c, IngredientCharacter character) {
        if (c == null || character == null) return false;

        Set<String> saved = sp(c).getStringSet(KEY_UNLOCKED_CHARACTERS, new HashSet<>());
        if (saved != null && saved.contains(character.name())) {
            return true;
        }

        return CollectionUnlockPrefs.isUnlocked(c, character.name());
    }

    public static Set<String> getUnlockedCharacterNames(Context c) {
        if (c == null) return new HashSet<>();
        return new HashSet<>(sp(c).getStringSet(KEY_UNLOCKED_CHARACTERS, new HashSet<>()));
    }

    public static boolean hasCharacterCountEntry(Context c, IngredientCharacter character) {
        if (c == null || character == null) return false;
        return CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name()) > 0;
    }

    public static int getCharacterCount(Context c, IngredientCharacter character) {
        if (c == null || character == null) return 0;

        int instanceCount = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());
        syncLegacyCharacterCount(c, character, instanceCount);
        return instanceCount;
    }

    public static void setCharacterCount(Context c, IngredientCharacter character, int count) {
        if (c == null || character == null) return;

        int safeCount = Math.max(0, count);
        int currentCount = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());

        if (safeCount > currentCount) {
            int addNeed = safeCount - currentCount;
            for (int i = 0; i < addNeed; i++) {
                CollectionInventoryPrefs.addIngredientInstance(c, character.name());
            }
        } else if (safeCount < currentCount) {
            int removeNeed = currentCount - safeCount;
            removeLowestPriorityInstances(c, character, removeNeed);
        }

        SharedPreferences s = sp(c);
        Set<String> raw = new HashSet<>(s.getStringSet(KEY_CHARACTER_COUNTS, new HashSet<>()));
        String prefix = character.name() + ":";

        Set<String> next = new HashSet<>();
        for (String entry : raw) {
            if (entry == null) continue;
            if (!entry.startsWith(prefix)) {
                next.add(entry);
            }
        }

        next.add(prefix + safeCount);

        s.edit().putStringSet(KEY_CHARACTER_COUNTS, next).apply();
    }

    public static int addCharacterCount(Context c, IngredientCharacter character, int delta) {
        if (c == null || character == null) return 0;
        if (delta == 0) return getCharacterCount(c, character);

        if (delta > 0) {
            for (int i = 0; i < delta; i++) {
                CollectionInventoryPrefs.addIngredientInstance(c, character.name());
            }
        } else {
            removeLowestPriorityInstances(c, character, Math.abs(delta));
        }

        int next = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());
        syncLegacyCharacterCount(c, character, next);
        return next;
    }

    public static int obtainCharacter(Context c, IngredientCharacter character) {
        if (c == null || character == null) return 0;

        unlockCharacter(c, character);
        CollectionInventoryPrefs.addIngredientInstance(c, character.name());

        int next = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());
        syncLegacyCharacterCount(c, character, next);
        return next;
    }

    public static boolean consumeCharacter(Context c, IngredientCharacter character, int amount) {
        if (c == null || character == null) return false;
        if (amount <= 0) return false;

        int current = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());
        if (current < amount) return false;

        removeLowestPriorityInstances(c, character, amount);

        int next = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());
        syncLegacyCharacterCount(c, character, next);
        return true;
    }

    public static boolean hasEnoughCharacter(Context c, IngredientCharacter character, int needCount) {
        if (c == null || character == null) return false;
        if (needCount <= 0) return true;

        return CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name()) >= needCount;
    }

    public static boolean canFuseCharacter(Context c, IngredientCharacter character) {
        if (c == null || character == null) return false;
        return CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name()) >= 2;
    }

    public static void migrateLegacyUnlockedCharacterCountsIfNeeded(Context c) {
        if (c == null) return;

        for (IngredientCharacter character : IngredientCharacter.values()) {
            int currentInstanceCount = CollectionInventoryPrefs.getIngredientInstanceCount(c, character.name());
            if (currentInstanceCount > 0) {
                syncLegacyCharacterCount(c, character, currentInstanceCount);
                continue;
            }

            int legacyCount = readLegacyCharacterCount(c, character);
            if (legacyCount > 0) {
                for (int i = 0; i < legacyCount; i++) {
                    String newInstanceKey = CollectionInventoryPrefs.addIngredientInstance(c, character.name());
                    if (newInstanceKey != null && CollectionPetStatePrefs.hasState(c, character.name())) {
                        CollectionPetStatePrefs.copyState(c, character.name(), newInstanceKey);
                    }
                }

                unlockCharacter(c, character);
                syncLegacyCharacterCount(c, character, legacyCount);
            }
        }
    }

    private static void removeLowestPriorityInstances(Context c, IngredientCharacter character, int amount) {
        if (amount <= 0) return;

        List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(c, character.name());
        if (instanceKeys.isEmpty()) return;

        final String selectedKey = CollectionDisplayPrefs.getSelectedItemKey(c);

        Collections.sort(instanceKeys, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                boolean aSelected = a.equals(selectedKey);
                boolean bSelected = b.equals(selectedKey);

                if (aSelected != bSelected) {
                    return aSelected ? 1 : -1;
                }

                int lvA = CollectionPetStatePrefs.getLevel(c, a);
                int lvB = CollectionPetStatePrefs.getLevel(c, b);
                if (lvA != lvB) {
                    return Integer.compare(lvA, lvB);
                }

                return a.compareTo(b);
            }
        });

        int removed = 0;
        for (String instanceKey : instanceKeys) {
            if (removed >= amount) break;
            if (CollectionInventoryPrefs.removeIngredientInstance(c, instanceKey)) {
                removed++;
            }
        }
    }

    private static void syncLegacyCharacterCount(Context c, IngredientCharacter character, int count) {
        SharedPreferences s = sp(c);
        Set<String> raw = new HashSet<>(s.getStringSet(KEY_CHARACTER_COUNTS, new HashSet<>()));
        String prefix = character.name() + ":";

        Set<String> next = new HashSet<>();
        for (String entry : raw) {
            if (entry == null) continue;
            if (!entry.startsWith(prefix)) {
                next.add(entry);
            }
        }
        next.add(prefix + Math.max(0, count));

        s.edit().putStringSet(KEY_CHARACTER_COUNTS, next).apply();
    }

    private static int readLegacyCharacterCount(Context c, IngredientCharacter character) {
        Set<String> raw = sp(c).getStringSet(KEY_CHARACTER_COUNTS, new HashSet<>());
        if (raw == null || raw.isEmpty()) return 0;

        String prefix = character.name() + ":";

        for (String entry : raw) {
            if (entry == null) continue;
            if (!entry.startsWith(prefix)) continue;

            try {
                return Math.max(0, Integer.parseInt(entry.substring(prefix.length())));
            } catch (Exception ignored) {
                return 0;
            }
        }

        return 0;
    }

    public static int getFeedCount(Context c) {
        return sp(c).getInt(KEY_FEED_COUNT, 0);
    }

    public static void addFeed(Context c, int delta) {
        int v = Math.max(0, getFeedCount(c) + delta);
        sp(c).edit().putInt(KEY_FEED_COUNT, v).apply();
    }

    public static boolean consumeFeed(Context c, int amount) {
        int cur = getFeedCount(c);
        if (cur < amount) return false;
        sp(c).edit().putInt(KEY_FEED_COUNT, cur - amount).apply();
        return true;
    }

    public static int getEggCount(Context c) {
        return clamp(sp(c).getInt(KEY_EGG_COUNT, 0), 0, 1);
    }

    public static boolean hasEgg(Context c) {
        return getEggCount(c) >= 1;
    }

    public static boolean canAddEgg(Context c) {
        return getEggCount(c) < 1;
    }

    public static boolean addEgg(Context c, int delta) {
        if (delta <= 0) return false;

        int current = getEggCount(c);
        if (current >= 1) return false;

        int next = current + delta;
        next = clamp(next, 0, 1);

        boolean added = next > current;

        SharedPreferences.Editor editor = sp(c).edit();
        editor.putInt(KEY_EGG_COUNT, next);

        if (added) {
            editor.putBoolean(KEY_HATCH_PLAYED, false)
                    .putInt(KEY_STAGE, STAGE_EGG)
                    .putInt(KEY_LEVEL, 1)
                    .putInt(KEY_EXP, 0)
                    .remove(KEY_SELECTED_INGREDIENT_ID);
        }

        editor.apply();
        return added;
    }

    public static boolean consumeEgg(Context c, int amount) {
        if (amount <= 0) return false;

        int current = getEggCount(c);
        if (current < amount) return false;

        int next = clamp(current - amount, 0, 1);
        sp(c).edit().putInt(KEY_EGG_COUNT, next).apply();
        return true;
    }

    public static void setEggCount(Context c, int value) {
        int safe = clamp(value, 0, 1);

        SharedPreferences.Editor editor = sp(c).edit();
        editor.putInt(KEY_EGG_COUNT, safe);

        if (safe > 0) {
            editor.putBoolean(KEY_HATCH_PLAYED, false)
                    .putInt(KEY_STAGE, STAGE_EGG)
                    .putInt(KEY_LEVEL, 1)
                    .putInt(KEY_EXP, 0)
                    .remove(KEY_SELECTED_INGREDIENT_ID);
        }

        editor.apply();
    }

    public static int getHunger(Context c) {
        applyScheduledHunger(c);
        return clamp(sp(c).getInt(KEY_HUNGER, 100), 0, 100);
    }

    public static void applyScheduledHunger(Context c) {
        if (c == null) return;

        SharedPreferences s = sp(c);

        long nowMs = System.currentTimeMillis();
        long lastUpdate = s.getLong(KEY_LAST_HUNGER_UPDATE_MS, 0L);

        if (lastUpdate <= 0L || lastUpdate > nowMs) {
            s.edit()
                    .putLong(KEY_LAST_HUNGER_UPDATE_MS, nowMs)
                    .putInt(KEY_HUNGER, clamp(s.getInt(KEY_HUNGER, 100), 0, 100))
                    .apply();
            return;
        }

        int currentHunger = clamp(s.getInt(KEY_HUNGER, 100), 0, 100);
        if (currentHunger <= 0) {
            s.edit()
                    .putInt(KEY_HUNGER, 0)
                    .putLong(KEY_LAST_HUNGER_UPDATE_MS, nowMs)
                    .apply();
            return;
        }

        long elapsedMs = nowMs - lastUpdate;
        if (elapsedMs <= 0L) return;

        long onePercentMs = HUNGER_FULL_DECAY_MS / 100L;
        if (onePercentMs <= 0L) onePercentMs = 1L;

        int decrease = (int) (elapsedMs / onePercentMs);
        if (decrease <= 0) return;

        int nextHunger = clamp(currentHunger - decrease, 0, 100);

        long consumedMs = decrease * onePercentMs;
        long nextLastUpdate = lastUpdate + consumedMs;

        if (nextHunger <= 0) {
            nextLastUpdate = nowMs;
        }

        s.edit()
                .putInt(KEY_HUNGER, nextHunger)
                .putLong(KEY_LAST_HUNGER_UPDATE_MS, nextLastUpdate)
                .apply();
    }

    public static boolean feedPlus20(Context c) {
        if (c == null) return false;

        SharedPreferences s = sp(c);

        applyScheduledHunger(c);

        int currentHunger = clamp(s.getInt(KEY_HUNGER, 100), 0, 100);
        if (currentHunger >= 100) {
            return false;
        }

        int nextHunger = clamp(currentHunger + 20, 0, 100);
        long nowMs = System.currentTimeMillis();

        s.edit()
                .putInt(KEY_HUNGER, nextHunger)
                .putLong(KEY_LAST_HUNGER_UPDATE_MS, nowMs)
                .putLong(KEY_HUNGER_OFFSET_MS, 0L)
                .putLong(KEY_MEAL_ANCHOR_MS, 0L)
                .apply();

        return true;
    }

    public static int debugHungerDown(Context c, int percentDown) {
        if (c == null) return 0;

        SharedPreferences s = sp(c);

        applyScheduledHunger(c);

        percentDown = clamp(percentDown, 0, 100);
        if (percentDown == 0) return getHunger(c);

        int currentHunger = clamp(s.getInt(KEY_HUNGER, 100), 0, 100);
        int nextHunger = clamp(currentHunger - percentDown, 0, 100);

        s.edit()
                .putInt(KEY_HUNGER, nextHunger)
                .putLong(KEY_LAST_HUNGER_UPDATE_MS, System.currentTimeMillis())
                .putLong(KEY_HUNGER_OFFSET_MS, 0L)
                .putLong(KEY_MEAL_ANCHOR_MS, 0L)
                .apply();

        return nextHunger;
    }

    public static int debugFeedPlus(Context c, int amount) {
        addFeed(c, Math.max(0, amount));
        return getFeedCount(c);
    }

    public static int getLevelUpCoinReward(int newLevel) {
        if (newLevel <= 1) return 0;
        return newLevel * 10;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}