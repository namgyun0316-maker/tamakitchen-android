package com.namgyun.tamakitchen.ui.home.factory;

import android.content.Context;

import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
import com.namgyun.tamakitchen.ui.fusion.FusionFood;

import java.util.Random;

public class HomeBubbleTextFactory {

    private static final Random random = new Random();

    private HomeBubbleTextFactory() {
    }

    public static String buildStatusMessage(Context context, int stage, int hunger, int localTapCount) {
        switch (stage) {
            case PetPrefs.STAGE_EGG:
                return buildEggMessage(hunger, localTapCount);
            case PetPrefs.STAGE_INGREDIENT:
                return buildIngredientMessage(context, hunger);
            case PetPrefs.STAGE_FOOD:
                return buildFoodMessage(context, hunger);
            default:
                return buildEggMessage(hunger, localTapCount);
        }
    }

    public static String buildHatchingMessage() {
        return pickRandom(
                "톡톡... 나 곧 나갈게!",
                "껍질 밖은 어떤 곳일까?",
                "조금만 기다려줘!",
                "열심히 나오고 있어!",
                "곧 만나자!"
        );
    }

    public static String buildHatchedMessage(String name) {
        if (name == null || name.trim().isEmpty()) {
            return pickRandom(
                    "짜잔! 나 왔어!",
                    "드디어 만났네!",
                    "앞으로 잘 부탁해!"
            );
        }

        return pickRandom(
                "안녕! 나는 " + name + "야!",
                name + " 등장!",
                "드디어 태어났어!",
                "앞으로 잘 부탁해!"
        );
    }

    private static String buildEggMessage(int hunger, int localTapCount) {
        if (localTapCount % 7 == 0 && localTapCount != 0 && hunger > 15) {
            return pickRandom(
                    "톡톡... 안에서 움직였어!",
                    "곧 만날 수 있을 것 같아.",
                    "계속 말 걸어줘!"
            );
        }

        if (hunger <= 15) {
            return pickRandom(
                    "조금 힘이 없어...",
                    "배가 고파...",
                    "먹이를 조금만 줄래?"
            );
        } else if (hunger <= 35) {
            return pickRandom(
                    "조금 출출해졌어.",
                    "배가 살짝 꼬르륵해.",
                    "먹이 생각이 나."
            );
        } else if (hunger <= 70) {
            return pickRandom(
                    "안쪽은 꽤 아늑해.",
                    "천천히 자라고 있어.",
                    "오늘도 잘 크는 중!"
            );
        } else {
            return pickRandom(
                    "지금 컨디션 최고야!",
                    "엄청 잘 크고 있어!",
                    "곧 멋지게 나갈게!"
            );
        }
    }

    private static String buildIngredientMessage(Context context, int hunger) {
        IngredientCharacter ingredient = PetPrefs.getSelectedIngredient(context);
        String id = ingredient != null ? ingredient.getId() : "";

        if (hunger <= 15) {
            return ingredientHungry(id);
        } else if (hunger <= 35) {
            return ingredientLow(id);
        } else if (hunger <= 70) {
            return ingredientNormal(id);
        } else {
            return ingredientHappy(id);
        }
    }

    private static String ingredientHungry(String id) {
        switch (id) {
            case "green_grape":
                return pickRandom("포도알이 축 처졌어...", "상큼함이 부족해...", "먹이 하나만!");
            case "sweet_potato":
                return pickRandom("속까지 허전해...", "따끈한 기운이 필요해.", "먹고 싶어...");
            case "strawberry":
                return pickRandom("딸기향이 약해졌어...", "상큼함이 떨어졌어.", "조금만 챙겨줘.");
            case "banana":
                return pickRandom("축 처졌어...", "달콤한 에너지가 필요해.", "먹이 줘!");
            case "onion":
                return pickRandom("배고파서 눈물 날 것 같아...", "속이 텅 빈 양파야.", "먹이 하나만.");
            case "green_onion":
                return pickRandom("파릇함이 시들시들해...", "힘이 쪽 빠졌어.", "다시 쑥쑥 자라고 싶어.");
            default:
                return pickRandom("배고파...", "먹이 하나만 줄래?", "지금 꼬르륵 중이야...");
        }
    }

    private static String ingredientLow(String id) {
        switch (id) {
            case "green_grape":
                return pickRandom("조금 출출해.", "상큼함은 아직 남아있어!", "톡 터질 힘을 모으는 중이야.");
            case "sweet_potato":
                return pickRandom("조금 출출해.", "아직 포근해.", "조금만 더 먹고 싶어.");
            default:
                return pickRandom("조금 출출해.", "먹이 생각 중이야.", "살짝 배고파.");
        }
    }

    private static String ingredientNormal(String id) {
        switch (id) {
            case "green_onion":
                return pickRandom("파릇파릇 준비 완료!", "오늘도 쑥쑥 자라는 느낌이야.", "싱싱함 MAX 직전이야.");
            case "strawberry":
                return pickRandom("오늘 딸기향이 좋아!", "상큼하게 기다리고 있었어.", "한 번 톡 건드려봐!");
            case "banana":
                return pickRandom("오늘 기분이 노랗게 밝아!", "흔들흔들 놀자!", "달콤한 하루야.");
            default:
                return pickRandom("여기서 기다리고 있었어.", "나랑 조금 놀래?", "오늘 컨디션 괜찮아.");
        }
    }

    private static String ingredientHappy(String id) {
        switch (id) {
            case "green_onion":
                return pickRandom("완전 파릇파릇해!", "기운이 쭉 올라왔어!", "오늘도 싱싱해!");
            case "strawberry":
                return pickRandom("상큼함 폭발!", "딸기향 충전 완료!", "기분 최고야!");
            default:
                return pickRandom("지금 기분 최고야!", "배부르고 행복해.", "오늘 정말 좋아!");
        }
    }

    private static String buildFoodMessage(Context context, int hunger) {
        FusionFood food = resolveSelectedFood(context);
        String id = food != null ? food.getId() : "";

        if (hunger <= 35) {
            return foodHungry(id);
        } else if (hunger <= 70) {
            return foodNormal(id);
        } else {
            return foodHappy(id);
        }
    }

    private static FusionFood resolveSelectedFood(Context context) {
        try {
            String selectedKey = CollectionDisplayPrefs.getSelectedItemKey(context);

            if (selectedKey == null || selectedKey.trim().isEmpty()) {
                return null;
            }

            for (FusionFood food : FusionFood.values()) {
                if (selectedKey.equals(food.getCollectionKey())
                        || selectedKey.equals(food.getId())) {
                    return food;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String foodHungry(String id) {
        switch (id) {
            case "strawberry_cake":
                return pickRandom("크림이 축 처졌어...", "딸기 장식도 힘이 없어.", "달콤한 힘이 필요해.");
            case "green_grape_ade":
                return pickRandom("탄산이 빠졌어...", "톡톡하게 충전해줘.", "청포도알이 조용해졌어.");
            default:
                return pickRandom("조금 힘이 빠졌어...", "먹이 하나만 줄래?", "맛있는 기운이 부족해.");
        }
    }

    private static String foodNormal(String id) {
        switch (id) {
            case "strawberry_cake":
                return pickRandom("크림도 딸기도 안정적이야.", "오늘 꽤 달콤해.", "나랑 티타임 할래?");
            case "green_grape_ade":
                return pickRandom("톡톡 상큼하게 대기 중!", "기포가 기분 좋게 올라와.", "청포도 향이 좋아.");
            default:
                return pickRandom("여기서 기다리고 있었어.", "나랑 조금 놀래?", "오늘 기분이 좋아.");
        }
    }

    private static String foodHappy(String id) {
        switch (id) {
            case "strawberry_cake":
                return pickRandom("달콤함 MAX!", "오늘은 축하파티야.", "크림도 딸기도 완벽해!");
            case "green_grape_ade":
                return pickRandom("톡톡 상큼 MAX!", "기포가 춤추고 있어.", "청포도에이드 완전 신났어!");
            default:
                return pickRandom("지금 기분 최고야!", "배부르고 행복해.", "오늘 완전 신났어!");
        }
    }

    public static String buildFeedActionMessage(int stage) {
        switch (stage) {
            case PetPrefs.STAGE_EGG:
                return pickRandom("냠... 고마워!", "힘이 조금 났어!", "조금 더 자랄 수 있을 것 같아.");
            case PetPrefs.STAGE_INGREDIENT:
                return pickRandom("냠냠! 맛있다!", "다시 힘났어!", "고마워!");
            case PetPrefs.STAGE_FOOD:
                return pickRandom("맛있게 먹었어!", "기분 좋아졌어!", "고마워!");
            default:
                return "고마워!";
        }
    }

    public static String buildLevelUpFeedMessage(int stage) {
        switch (stage) {
            case PetPrefs.STAGE_EGG:
                return pickRandom("조금 더 자란 것 같아!", "껍질이 좁아진 것 같아!", "곧 만날 수 있을지도!");
            case PetPrefs.STAGE_INGREDIENT:
                return pickRandom("레벨업했어!", "더 강해졌어!", "조금 더 성장했어!");
            case PetPrefs.STAGE_FOOD:
                return pickRandom("한 단계 더 성장했어!", "레벨업 성공!", "오늘 정말 뿌듯해!");
            default:
                return "나 조금 더 자랐어!";
        }
    }

    public static String buildLikedFeedMessage(Context context, int stage) {
        return pickRandom(
                "이거 내가 정말 좋아하는 맛이야!",
                "취향 저격이야!",
                "완전 맛있어!",
                "너무 좋아!"
        );
    }

    public static String buildLikedFeedLevelUpMessage(Context context, int stage) {
        return pickRandom(
                "좋아하는 걸 먹고 레벨업했어!",
                "완전 힘이 났어!",
                "덕분에 성장했어!",
                "너무 행복해!"
        );
    }

    public static String buildTapReactionMessage(int stage, int hunger) {
        if (stage == PetPrefs.STAGE_EGG) {
            return pickRandom("톡톡!", "안에서 들었어!", "나 여기 있어!");
        }

        if (hunger <= 35) {
            return pickRandom("간지럽지만 조금 배고파...", "톡 건드린 거 느꼈어.", "먹이도 있으면 좋겠다.");
        }

        return pickRandom("헤헤, 간지러워!", "또 놀아줘!", "나랑 더 놀자!");
    }

    public static String fullMessage() {
        return pickRandom("나 배불러!", "지금은 충분히 먹었어.", "조금 쉬고 싶어.");
    }

    public static String noFeedMessage() {
        return pickRandom("먹이가 없어...", "상점에서 사다 줄래?", "먹이 주머니가 비었어.");
    }

    private static String pickRandom(String... messages) {
        if (messages == null || messages.length == 0) {
            return "";
        }
        return messages[random.nextInt(messages.length)];
    }
}