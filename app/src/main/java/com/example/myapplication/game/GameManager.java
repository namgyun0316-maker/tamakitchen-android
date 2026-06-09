package com.namgyun.tamakitchen.game;

import android.content.Context;
import android.os.SystemClock;

import java.util.HashMap;
import java.util.Map;

public class GameManager {

    // 간단한 쿨타임(남용 방지) - 필요 없으면 제거 가능
    private static final long COOLDOWN_MS = 3_000L;

    private static final Map<GameAction, Long> lastActionTime = new HashMap<>();

    public static class Reward {
        public final int coins;
        public final int exp;
        public final int hungerDelta; // 먹이면 +, 시간이 지나면 -

        public Reward(int coins, int exp, int hungerDelta) {
            this.coins = coins;
            this.exp = exp;
            this.hungerDelta = hungerDelta;
        }
    }

    // 행동별 보상
    public static Reward getReward(GameAction action) {
        switch (action) {
            case ADD_INGREDIENT:
                return new Reward(3, 8, 0);
            case SCAN_RECEIPT:
                return new Reward(8, 15, 0);
            case VIEW_RECIPE_DETAIL:
                return new Reward(1, 2, 0);
            case COOK_COMPLETE:
                return new Reward(15, 30, 0);
            case ADD_SHOPPING_ITEM:
                return new Reward(2, 4, 0);
            default:
                return new Reward(0, 0, 0);
        }
    }

    public static boolean canReward(GameAction action) {
        long now = SystemClock.elapsedRealtime();
        Long last = lastActionTime.get(action);
        if (last == null || now - last >= COOLDOWN_MS) {
            lastActionTime.put(action, now);
            return true;
        }
        return false;
    }

    public static Reward applyAction(Context c, GameAction action) {
        // 쿨타임으로 보상 제한
        if (!canReward(action)) return new Reward(0, 0, 0);

        Reward r = getReward(action);
        if (r.coins != 0) GamePrefs.addCoins(c, r.coins);
        if (r.exp != 0) GamePrefs.addExp(c, r.exp);
        if (r.hungerDelta != 0) GamePrefs.addHunger(c, r.hungerDelta);
        return r;
    }
}