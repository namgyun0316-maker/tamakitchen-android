package com.namgyun.tamakitchen.ui.collection;

import android.content.Context;
import android.content.SharedPreferences;

import com.namgyun.tamakitchen.pet.PetPrefs;

public class CollectionPetStatePrefs {

    private static final String PREFS_NAME = "collection_pet_state_prefs";

    private static final int DEFAULT_LEVEL = 1;
    private static final int DEFAULT_HUNGER = 100;
    private static final int DEFAULT_EXP = 0;

    // 12시간 동안 100% -> 0%
    private static final long HUNGER_FULL_DECAY_MS =
            5L * 60L * 60L * 1000L;

    private CollectionPetStatePrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String levelKey(String itemKey) {
        return "level_" + itemKey;
    }

    private static String hungerKey(String itemKey) {
        return "hunger_" + itemKey;
    }

    private static String expKey(String itemKey) {
        return "exp_" + itemKey;
    }

    private static String lastHungerUpdateKey(String itemKey) {
        return "last_hunger_update_" + itemKey;
    }

    private static boolean isBasicEgg(String itemKey) {
        return CollectionCatalog.KEY_EGG_BASIC.equals(itemKey);
    }

    private static int getItemMaxLevel(String itemKey) {
        return isBasicEgg(itemKey) ? PetPrefs.HATCH_LEVEL : PetPrefs.MAX_LEVEL;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int getRawExp(Context context, String itemKey) {
        ensureState(context, itemKey);

        int level = getLevel(context, itemKey);
        int maxLevel = getItemMaxLevel(itemKey);

        if (level >= maxLevel) return 0;

        return Math.max(0, prefs(context).getInt(expKey(itemKey), DEFAULT_EXP));
    }

    private static void setRawExp(Context context, String itemKey, int exp) {
        ensureState(context, itemKey);

        int level = getLevel(context, itemKey);
        int maxLevel = getItemMaxLevel(itemKey);

        if (level >= maxLevel) {
            prefs(context).edit().putInt(expKey(itemKey), 0).apply();
            return;
        }

        prefs(context).edit().putInt(expKey(itemKey), Math.max(0, exp)).apply();
    }

    public static boolean hasState(Context context, String itemKey) {
        if (context == null || itemKey == null || itemKey.trim().isEmpty()) return false;
        return prefs(context).contains(levelKey(itemKey));
    }

    public static void copyState(Context context, String fromKey, String toKey) {
        if (context == null || fromKey == null || toKey == null) return;
        if (fromKey.trim().isEmpty() || toKey.trim().isEmpty()) return;

        ensureState(context, fromKey);
        ensureState(context, toKey);

        int fromLevel = getLevel(context, fromKey);
        int fromHunger = getHunger(context, fromKey);
        int fromExpPercent = getExpPercent(context, fromKey);

        setLevel(context, toKey, fromLevel);
        setHunger(context, toKey, fromHunger);
        setExpPercent(context, toKey, fromExpPercent);
    }

    public static void ensureState(Context context, String itemKey) {
        if (context == null || itemKey == null || itemKey.trim().isEmpty()) return;

        SharedPreferences sp = prefs(context);
        SharedPreferences.Editor editor = sp.edit();
        boolean changed = false;

        if (!sp.contains(levelKey(itemKey))) {
            editor.putInt(levelKey(itemKey), DEFAULT_LEVEL);
            changed = true;
        }

        if (!sp.contains(hungerKey(itemKey))) {
            editor.putInt(hungerKey(itemKey), DEFAULT_HUNGER);
            changed = true;
        }

        if (!sp.contains(expKey(itemKey))) {
            editor.putInt(expKey(itemKey), DEFAULT_EXP);
            changed = true;
        }

        if (!sp.contains(lastHungerUpdateKey(itemKey))) {
            editor.putLong(lastHungerUpdateKey(itemKey), System.currentTimeMillis());
            changed = true;
        }

        if (changed) {
            editor.apply();
        }
    }

    public static void clearState(Context context, String itemKey) {
        if (context == null || itemKey == null || itemKey.trim().isEmpty()) return;

        prefs(context).edit()
                .remove(levelKey(itemKey))
                .remove(hungerKey(itemKey))
                .remove(expKey(itemKey))
                .remove(lastHungerUpdateKey(itemKey))
                .apply();
    }

    public static int getLevel(Context context, String itemKey) {
        ensureState(context, itemKey);

        int maxLevel = getItemMaxLevel(itemKey);
        int saved = prefs(context).getInt(levelKey(itemKey), DEFAULT_LEVEL);
        int safe = clamp(saved, 1, maxLevel);

        if (safe != saved) {
            prefs(context).edit().putInt(levelKey(itemKey), safe).apply();
        }

        return safe;
    }

    public static void applyScheduledHunger(Context context, String itemKey) {
        if (context == null || itemKey == null || itemKey.trim().isEmpty()) return;

        ensureState(context, itemKey);

        SharedPreferences sp = prefs(context);

        long nowMs = System.currentTimeMillis();
        long lastUpdateMs = sp.getLong(lastHungerUpdateKey(itemKey), nowMs);

        if (lastUpdateMs <= 0L || lastUpdateMs > nowMs) {
            sp.edit()
                    .putLong(lastHungerUpdateKey(itemKey), nowMs)
                    .putInt(hungerKey(itemKey), clamp(sp.getInt(hungerKey(itemKey), DEFAULT_HUNGER), 0, 100))
                    .apply();
            return;
        }

        int currentHunger = clamp(sp.getInt(hungerKey(itemKey), DEFAULT_HUNGER), 0, 100);

        if (currentHunger <= 0) {
            sp.edit()
                    .putInt(hungerKey(itemKey), 0)
                    .putLong(lastHungerUpdateKey(itemKey), nowMs)
                    .apply();
            return;
        }

        long elapsedMs = nowMs - lastUpdateMs;
        if (elapsedMs <= 0L) return;

        long onePercentMs = HUNGER_FULL_DECAY_MS / 100L;
        if (onePercentMs <= 0L) onePercentMs = 1L;

        int decrease = (int) (elapsedMs / onePercentMs);
        if (decrease <= 0) return;

        int nextHunger = clamp(currentHunger - decrease, 0, 100);

        long consumedMs = decrease * onePercentMs;
        long nextLastUpdateMs = lastUpdateMs + consumedMs;

        if (nextHunger <= 0) {
            nextLastUpdateMs = nowMs;
        }

        sp.edit()
                .putInt(hungerKey(itemKey), nextHunger)
                .putLong(lastHungerUpdateKey(itemKey), nextLastUpdateMs)
                .apply();
    }

    public static int getHunger(Context context, String itemKey) {
        applyScheduledHunger(context, itemKey);
        return clamp(prefs(context).getInt(hungerKey(itemKey), DEFAULT_HUNGER), 0, 100);
    }

    public static int getExpPercent(Context context, String itemKey) {
        ensureState(context, itemKey);

        int level = getLevel(context, itemKey);
        int maxLevel = getItemMaxLevel(itemKey);

        if (level >= maxLevel) {
            return 100;
        }

        int exp = getRawExp(context, itemKey);
        int need = PetPrefs.getNeedExpForLevel(level);

        if (need <= 0) return 0;

        return clamp((int) Math.round((exp * 100.0) / need), 0, 100);
    }

    public static void setLevel(Context context, String itemKey, int level) {
        ensureState(context, itemKey);

        int maxLevel = getItemMaxLevel(itemKey);
        int safeLevel = clamp(level, 1, maxLevel);

        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putInt(levelKey(itemKey), safeLevel);

        if (safeLevel >= maxLevel) {
            editor.putInt(expKey(itemKey), 0);
        }

        editor.apply();
    }

    public static void setHunger(Context context, String itemKey, int hunger) {
        ensureState(context, itemKey);

        int safe = clamp(hunger, 0, 100);

        prefs(context).edit()
                .putInt(hungerKey(itemKey), safe)
                .putLong(lastHungerUpdateKey(itemKey), System.currentTimeMillis())
                .apply();
    }

    public static void setExpPercent(Context context, String itemKey, int expPercent) {
        ensureState(context, itemKey);

        int level = getLevel(context, itemKey);
        int maxLevel = getItemMaxLevel(itemKey);

        if (level >= maxLevel) {
            prefs(context).edit().putInt(expKey(itemKey), 0).apply();
            return;
        }

        int safePercent = clamp(expPercent, 0, 100);
        int need = PetPrefs.getNeedExpForLevel(level);
        int rawExp = (int) Math.round((need * safePercent) / 100.0);

        setRawExp(context, itemKey, rawExp);
    }

    public static boolean feedPlus20(Context context, String itemKey) {
        applyScheduledHunger(context, itemKey);

        int current = getHunger(context, itemKey);
        if (current >= 100) return false;

        int next = Math.min(100, current + 20);

        prefs(context).edit()
                .putInt(hungerKey(itemKey), next)
                .putLong(lastHungerUpdateKey(itemKey), System.currentTimeMillis())
                .apply();

        return true;
    }

    public static boolean addExpAndMaybeLevelUp(Context context, String itemKey, int expReward) {
        ensureState(context, itemKey);

        int level = getLevel(context, itemKey);
        int exp = getRawExp(context, itemKey);
        int maxLevel = getItemMaxLevel(itemKey);

        if (level >= maxLevel) {
            setLevel(context, itemKey, maxLevel);
            setRawExp(context, itemKey, 0);
            return false;
        }

        exp += Math.max(0, expReward);

        boolean leveledUp = false;

        while (level < maxLevel) {
            int need = PetPrefs.getNeedExpForLevel(level);
            if (need <= 0 || exp < need) break;

            exp -= need;
            level++;
            leveledUp = true;

            if (level >= maxLevel) {
                level = maxLevel;
                exp = 0;
                break;
            }
        }

        setLevel(context, itemKey, level);
        setRawExp(context, itemKey, exp);

        return leveledUp;
    }

    public static int debugHungerDown(Context context, String itemKey, int amount) {
        ensureState(context, itemKey);

        int current = getHunger(context, itemKey);
        int next = Math.max(0, current - Math.max(0, amount));

        setHunger(context, itemKey, next);
        return next;
    }

    public static int debugLevelUp(Context context, String itemKey) {
        ensureState(context, itemKey);

        int maxLevel = getItemMaxLevel(itemKey);
        int current = getLevel(context, itemKey);
        int next = Math.min(maxLevel, current + 1);

        setLevel(context, itemKey, next);
        setRawExp(context, itemKey, 0);

        return next;
    }

    public static int debugLevelDown(Context context, String itemKey) {
        ensureState(context, itemKey);

        int next = Math.max(1, getLevel(context, itemKey) - 1);

        setLevel(context, itemKey, next);
        setRawExp(context, itemKey, 0);

        return next;
    }
}