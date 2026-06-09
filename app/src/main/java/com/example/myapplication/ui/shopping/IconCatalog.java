package com.namgyun.tamakitchen.ui.shopping;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.fridge.ResourceNameHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IconCatalog {

    private IconCatalog() {}

    private static List<IconItem> CACHED_ICONS;
    private static Map<String, Integer> RAWKEY_TO_RESID;

    private static void add(List<IconItem> icons, int resId, String name, String category, String rawKey) {
        icons.add(new IconItem(resId, name, category, rawKey));
    }

    public static List<IconItem> getAllIcons() {
        if (CACHED_ICONS != null) return CACHED_ICONS;

        List<IconItem> icons = new ArrayList<>();

        // =========================
        // 채소
        // =========================
        add(icons, R.drawable.itme_eggplant, "가지", "채소", "eggplant");
        add(icons, R.drawable.item_cherrytomatoes, "방울토마토", "채소", "cherrytomatoes");
        add(icons, R.drawable.item_greenonion, "대파", "채소", "greenonion");
        add(icons, R.drawable.item_asparagus, "아스파라거스", "채소", "asparagus");
        add(icons, R.drawable.item_basil, "바질", "채소", "basil");
        add(icons, R.drawable.item_squash, "애호박", "채소", "squash");
        add(icons, R.drawable.item_radish, "무", "채소", "radish");
        add(icons, R.drawable.item_sweetpotato, "고구마", "채소", "sweetpotato");
        add(icons, R.drawable.item_tomato, "토마토", "채소", "tomato");
        add(icons, R.drawable.item_corner, "옥수수", "채소", "corn");
        add(icons, R.drawable.item_scallion, "쪽파", "채소", "scallion");
        add(icons, R.drawable.item_chillipepper, "고추", "채소", "chillipepper");
        add(icons, R.drawable.item_beetroot, "비트", "채소", "beetroot");
        add(icons, R.drawable.item_cabbage, "양배추", "채소", "cabbage");
        add(icons, R.drawable.item_spinach, "시금치", "채소", "spinach");
        add(icons, R.drawable.item_jinger, "생강", "채소", "ginger");
        add(icons, R.drawable.item_lettuce, "양상추", "채소", "lettuce");
        add(icons, R.drawable.item_broccoli, "브로콜리", "채소", "broccoli");
        add(icons, R.drawable.item_paprika, "파프리카", "채소", "paprika");
        add(icons, R.drawable.item_oyster_mushrooms, "느타리버섯", "버섯류", "oyster_mushrooms");
        add(icons, R.drawable.item_pine_mushrooms, "새송이버섯", "버섯류", "pine_mushrooms");
        add(icons, R.drawable.item_shiitake_mushrooms, "표고버섯", "버섯류", "shiitake_mushrooms");
        add(icons, R.drawable.item_enoki_mushrooms, "팽이버섯", "버섯류", "enoki_mushrooms");

        add(icons, R.drawable.item_cheongyang_chili, "청양고추", "채소", "cheongyang_chili");
        add(icons, R.drawable.item_crown_daisy, "쑥갓", "채소", "crown_daisy");
        add(icons, R.drawable.item_lettuce_korean, "상추", "채소", "lettuce_korean");
        add(icons, R.drawable.item_perilla_leaf, "깻잎", "채소", "perilla_leaf");
        add(icons, R.drawable.item_garlic, "마늘", "채소", "garlic");
        add(icons, R.drawable.item_carrot, "당근", "채소", "carrot");
        add(icons, R.drawable.item_cucumber, "오이", "채소", "cucumber");
        add(icons, R.drawable.item_potato, "감자", "채소", "potato");
        add(icons, R.drawable.item_chive, "부추", "채소", "chive");
        add(icons, R.drawable.item_minari, "미나리", "채소", "minari");
        add(icons, R.drawable.item_coriander, "고수", "채소", "coriander");
        add(icons, R.drawable.item_red_chili, "홍고추", "채소", "red_chili");
        add(icons, R.drawable.item_siraegi, "시래기", "채소", "siraegi");

        // =========================
        // 과일
        // =========================
        add(icons, R.drawable.item_blueberry, "블루베리", "과일", "blueberry");
        add(icons, R.drawable.itme_melon, "멜론", "과일", "melon");
        add(icons, R.drawable.item_watermelon, "수박", "과일", "watermelon");
        add(icons, R.drawable.item_avocado, "아보카도", "과일", "avocado");
        add(icons, R.drawable.item_mango, "망고", "과일", "mango");
        add(icons, R.drawable.item_kiwi, "키위", "과일", "kiwi");
        add(icons, R.drawable.item_lemon, "레몬", "과일", "lemon");
        add(icons, R.drawable.item_lime, "라임", "과일", "lime");
        add(icons, R.drawable.item_pear, "배", "과일", "pear");
        add(icons, R.drawable.item_cherry, "체리", "과일", "cherry");
        add(icons, R.drawable.item_persimmon, "감", "과일", "persimmon");
        add(icons, R.drawable.item_peach, "복숭아", "과일", "peach");
        add(icons, R.drawable.item_pineapple, "파인애플", "과일", "pineapple");
        add(icons, R.drawable.item_mandarin, "귤", "과일", "mandarin");
        add(icons, R.drawable.item_orange, "오렌지", "과일", "orange");
        add(icons, R.drawable.item_shinemuscat, "샤인머스켓", "과일", "shinemuscat");
        add(icons, R.drawable.item_grapes, "포도", "과일", "grapes");
        add(icons, R.drawable.item_apple, "사과", "과일", "apple");
        add(icons, R.drawable.item_strawberry, "딸기", "과일", "strawberry");
        add(icons, R.drawable.item_plum, "자두", "과일", "plum");
        add(icons, R.drawable.item_olive, "올리브", "과일", "olive");

        // =========================
        // 육류 - 소
        // =========================
        add(icons, R.drawable.item_filet, "소안심", "육류", "filet");
        add(icons, R.drawable.item_beef_sirloin, "소등심", "육류", "beef_sirloin");
        add(icons, R.drawable.item_neck, "소목심", "육류", "neck");
        add(icons, R.drawable.item_bacon, "베이컨", "육류", "bacon");
        add(icons, R.drawable.item_beef_ribs, "소갈비살", "육류", "beef_ribs");
        add(icons, R.drawable.item_shortloin, "채끝", "육류", "shortloin");
        add(icons, R.drawable.item_brisket, "양지", "육류", "brisket");
        add(icons, R.drawable.item_round, "소우둔살", "육류", "round");
        add(icons, R.drawable.item_beef_sholder_clod, "소앞다리살", "육류", "beef_sholder_clod");
        add(icons, R.drawable.item_tomahawk_cut, "토마호크", "육류", "tomahawk_cut");
        add(icons, R.drawable.item_abomasum, "소막창", "육류", "abomasum");
        add(icons, R.drawable.item_beef_daechang, "소대창", "육류", "beef_daechang");
        add(icons, R.drawable.item_brisket_deckle, "차돌박이", "육류", "brisket_deckle");

        // =========================
        // 육류 - 돼지
        // =========================
        add(icons, R.drawable.item_pork_belly, "삼겹살", "육류", "pork_belly");
        add(icons, R.drawable.item_pork_bone, "돼지등뼈", "육류", "pork_bone");
        add(icons, R.drawable.item_pork_tenderloin, "돼지안심", "육류", "pork_tenderloin");
        add(icons, R.drawable.item_pork_loin, "돼지등심", "육류", "pork_loin");
        add(icons, R.drawable.item_blade_shoulde, "돼지목심", "육류", "blade_shoulde");
        add(icons, R.drawable.item_picnic, "돼지앞다리살", "육류", "picnic");
        add(icons, R.drawable.item_pork_leg, "돼지뒷다리살", "육류", "pork_leg");
        add(icons, R.drawable.item_thin_pork_belly, "대패삼겹살", "육류", "thin_pork_belly");

        // =========================
        // 육류 - 닭
        // =========================
        add(icons, R.drawable.item_chicken, "닭고기", "육류", "chicken");
        add(icons, R.drawable.item_chiken_breast, "닭가슴살", "육류", "chiken_breast");
        add(icons, R.drawable.item_chicken_feet, "닭발", "육류", "chicken_feet");
        add(icons, R.drawable.item_chicken_stew, "닭볶음탕용", "육류", "chicken_stew");

        // =========================
        // 육류 - 가공육
        // =========================
        add(icons, R.drawable.item_ham, "햄", "육류", "ham");
        add(icons, R.drawable.item_spam, "스팸", "육류", "spam");
        add(icons, R.drawable.item_recham, "리챔", "육류", "recham");

        // =========================
        // 해산물
        // =========================
        add(icons, R.drawable.item_salmon, "연어", "해산물", "salmon");
        add(icons, R.drawable.item_urchin_roe, "성게알", "해산물", "urchin_roe");
        add(icons, R.drawable.item_flounder, "도다리", "해산물", "flounder");
        add(icons, R.drawable.item_flatfish, "광어", "해산물", "flatfish");
        add(icons, R.drawable.item_cutlassfish, "갈치", "해산물", "cutlassfish");
        add(icons, R.drawable.item_mackerel, "고등어", "해산물", "mackerel");
        add(icons, R.drawable.item_herring, "청어", "해산물", "herring");
        add(icons, R.drawable.item_pacific_saury, "꽁치", "해산물", "pacific_saury");
        add(icons, R.drawable.item_eel, "장어", "해산물", "eel");
        add(icons, R.drawable.item_mullet, "숭어", "해산물", "mullet");
        add(icons, R.drawable.item_snapper, "도미", "해산물", "snapper");
        add(icons, R.drawable.item_octopus, "문어", "해산물", "octopus");
        add(icons, R.drawable.item_small_octopus, "낙지", "해산물", "small_octopus");
        add(icons, R.drawable.item_squid, "오징어", "해산물", "squid");
        add(icons, R.drawable.item_shrimp, "새우", "해산물", "shrimp");
        add(icons, R.drawable.item_crab, "게", "해산물", "crab");
        add(icons, R.drawable.item_snow_crab, "대게", "해산물", "snow_crab");
        add(icons, R.drawable.item_king_crab, "킹크랩", "해산물", "king_crab");
        add(icons, R.drawable.item_lobster, "랍스터", "해산물", "lobster");
        add(icons, R.drawable.item_sea_squirt, "멍게", "해산물", "sea_squirt");
        add(icons, R.drawable.item_scallop, "가리비", "해산물", "scallop");
        add(icons, R.drawable.item_mussel, "홍합", "해산물", "mussel");
        add(icons, R.drawable.item_oyster, "굴", "해산물", "oyster");
        add(icons, R.drawable.item_manila_clam, "바지락", "해산물", "manila_clam");
        add(icons, R.drawable.item_pen_shell, "키조개", "해산물", "pen_shell");
        add(icons, R.drawable.item_abalone, "전복", "해산물", "abalone");
        add(icons, R.drawable.item_rockfish, "우럭", "해산물", "rockfish");
        add(icons, R.drawable.item_sea_snail, "골뱅이", "해산물", "sea_snail");
        add(icons, R.drawable.item_seafood_mix, "해물믹스", "해산물", "seafood_mix");
        add(icons, R.drawable.item_seaweed, "미역", "해산물", "seaweed");

        // =========================
        // 유제품
        // =========================
        add(icons, R.drawable.item_milk, "우유", "유제품", "milk");
        add(icons, R.drawable.shredded_cheese, "슈레드치즈", "유제품", "shredded_cheese");
        add(icons, R.drawable.item_mozzarella_cheese, "모짜렐라치즈", "유제품", "mozzarella_cheese");
        add(icons, R.drawable.item_greek_yogurt, "그릭요거트", "유제품", "greek_yogurt");
        add(icons, R.drawable.item_yogurt, "요거트", "유제품", "yogurt");
        add(icons, R.drawable.item_yoplait, "요플레", "유제품", "yoplait");
        add(icons, R.drawable.item_yoguleuteu, "요구르트", "유제품", "yoguleuteu");
        add(icons, R.drawable.item_whipped_cream, "생크림", "유제품", "whipped_cream");
        add(icons, R.drawable.item_melona_flavored_milk, "메로나맛우유", "유제품", "melona_flavored_milk");
        add(icons, R.drawable.item_strawberry_milk, "딸기우유", "유제품", "strawberry_milk");
        add(icons, R.drawable.item_choco_milk, "초코우유", "유제품", "choco_milk");
        add(icons, R.drawable.item_cheddar_cheese, "체다치즈", "유제품", "cheddar_cheese");
        add(icons, R.drawable.item_together_icecream, "투게더", "유제품", "together_icecream");
        add(icons, R.drawable.item_butter, "버터", "유제품", "butter");

        // =========================
        // 견과류
        // =========================
        add(icons, R.drawable.item_chestnut, "밤", "견과류", "chestnut");
        add(icons, R.drawable.item_pine_nut, "잣", "견과류", "pine_nut");
        add(icons, R.drawable.item_hazelnut, "헤이즐넛", "견과류", "hazelnut");
        add(icons, R.drawable.item_peanut, "땅콩", "견과류", "peanut");
        add(icons, R.drawable.item_macadamia, "마카다미아", "견과류", "macadamia");
        add(icons, R.drawable.item_pistachio, "피스타치오", "견과류", "pistachio");
        add(icons, R.drawable.item_raisin, "건포도", "견과류", "raisin");
        add(icons, R.drawable.item_almond, "아몬드", "견과류", "almond");
        add(icons, R.drawable.item_walnut, "호두", "견과류", "walnut");

        // =========================
        // 기타(소스/양념/가공식품 등)
        // =========================
        add(icons, R.drawable.item_buldak_sauce, "불닭소스", "기타", "buldak_sauce");
        add(icons, R.drawable.item_barbecue_sauce, "바베큐소스", "기타", "barbecue_sauce");
        add(icons, R.drawable.item_teriyaki_sauce, "데리야끼소스", "기타", "teriyaki_sauce");
        add(icons, R.drawable.item_steak_sauce, "스테이크소스", "기타", "steak_sauce");
        add(icons, R.drawable.item_mustard, "머스타드", "기타", "mustard");
        add(icons, R.drawable.item_sriracha_sauce, "스리라차소스", "기타", "sriracha_sauce");
        add(icons, R.drawable.item_tartar_sauce, "타르타르소스", "기타", "tartar_sauce");
        add(icons, R.drawable.item_chili_sauce, "칠리소스", "기타", "chili_sauce");
        add(icons, R.drawable.item_ketchup, "케찹", "기타", "ketchup");
        add(icons, R.drawable.item_mayonnase, "마요네즈", "기타", "mayonnase");

        add(icons, R.drawable.item_rice_syrup, "조청", "기타", "rice_syrup");
        add(icons, R.drawable.item_corn_syrup, "요리당", "기타", "corn_syrup");
        add(icons, R.drawable.item_green_plum_syrup, "매실청", "기타", "green_plum_syrup");

        add(icons, R.drawable.item_oyster_sauce, "굴소스", "기타", "oyster_sauce");
        add(icons, R.drawable.item_lemon_juice, "레몬즙", "기타", "lemon_juice");

        add(icons, R.drawable.item_sesame_oil, "참기름", "기타", "sesame_oil");
        add(icons, R.drawable.item_perilla_oil, "들기름", "기타", "perilla_oil");
        add(icons, R.drawable.item_cooking_oil, "식용유", "기타", "cooking_oil");
        add(icons, R.drawable.item_canola_oil, "카놀라유", "기타", "canola_oil");
        add(icons, R.drawable.extra_virgin_olive_oil, "올리브유", "기타", "extra_virgin_olive_oil");

        add(icons, R.drawable.item_vinegar, "식초", "기타", "vinegar");
        add(icons, R.drawable.item_cooking_wine, "맛술", "기타", "cooking_wine");
        add(icons, R.drawable.item_fish_sauce, "까나리액젓", "기타", "fish_sauce");
        add(icons, R.drawable.item_anchovy_sauce, "멸치액젓", "기타", "anchovy_sauce");
        add(icons, R.drawable.item_tuna_sauce, "참치액", "기타", "tuna_sauce");
        add(icons, R.drawable.item_salted_shrimp, "새우젓", "기타", "salted_shrimp");

        add(icons, R.drawable.item_red_pepper_powder, "고춧가루", "기타", "red_pepper_powder");
        add(icons, R.drawable.item_pepper, "후추", "기타", "pepper");
        add(icons, R.drawable.item_sugar, "설탕", "기타", "sugar");
        add(icons, R.drawable.item_salt, "소금", "기타", "salt");
        add(icons, R.drawable.item_flavred_salt, "맛소금", "기타", "flavred_salt");
        add(icons, R.drawable.item_dasida, "다시다", "기타", "dasida");
        add(icons, R.drawable.item_miwon, "미원", "기타", "miwon");

        add(icons, R.drawable.item_chili_pepper_paste, "고추장", "기타", "chili_pepper_paste");
        add(icons, R.drawable.item_soybean_paste, "된장", "기타", "soybean_paste");
        add(icons, R.drawable.item_jinganjang, "진간장", "기타", "jinganjang");
        add(icons, R.drawable.item_gugganjang, "국간장", "기타", "gugganjang");
        add(icons, R.drawable.item_brewed_soy_sauce, "양조간장", "기타", "brewed_soy_sauce");
        add(icons, R.drawable.item_chogochujang, "초고추장", "기타", "chogochujang");
        add(icons, R.drawable.item_ssamjang, "쌈장", "기타", "ssamjang");
        add(icons, R.drawable.item_wasabi, "와사비", "기타", "wasabi");
        add(icons, R.drawable.item_tsuyu, "쯔유", "기타", "tsuyu");

        add(icons, R.drawable.item_basil_pesto, "바질페스토", "기타", "basil_pesto");

        add(icons, R.drawable.item_rice, "쌀", "기타", "rice");
        add(icons, R.drawable.item_instant_rice, "즉석밥", "기타", "instant_rice");
        add(icons, R.drawable.item_carding, "소면", "기타", "carding");
        add(icons, R.drawable.item_acorn_jelly, "도토리묵", "기타", "acorn_jelly");

        add(icons, R.drawable.item_shin_ramyun, "신라면", "기타", "shin_ramyun");
        add(icons, R.drawable.item_bottled_water, "생수", "기타", "bottled_water");

        add(icons, R.drawable.item_square_fishcake, "사각어묵", "기타", "square_fishcake");
        add(icons, R.drawable.item_sour_kimchi, "신김치", "기타", "sour_kimchi");
        add(icons, R.drawable.item_cabbage_kimchi, "배추김치", "기타", "cabbage_kimchi");
        add(icons, R.drawable.item_chonggak_kimchi, "총각김치", "기타", "chonggak_kimchi");
        add(icons, R.drawable.item_kkakdugi, "깍두기", "기타", "kkakdugi");
        add(icons, R.drawable.item_dongchimi, "동치미", "기타", "dongchimi");

        add(icons, R.drawable.item_honey, "꿀", "기타", "honey");
        add(icons, R.drawable.item_egg, "계란", "기타", "egg");
        add(icons, R.drawable.item_ginger_powder, "생강가루", "기타", "ginger_powder");
        add(icons, R.drawable.item_sesame_salt, "깨소금", "기타", "sesame_salt");
        add(icons, R.drawable.item_jajang_powder, "짜장가루", "기타", "jajang_powder");
        add(icons, R.drawable.item_chili_dried, "건고추", "기타", "chili_dried");
        add(icons, R.drawable.item_soba_noodle, "메밀면", "기타", "soba_noodle");
        add(icons, R.drawable.item_mustard_korean, "연겨자", "기타", "mustard_korean");
        add(icons, R.drawable.item_flour, "밀가루", "기타", "flour");
        add(icons, R.drawable.item_pancake_mix, "부침가루", "기타", "pancake_mix");
        add(icons, R.drawable.item_soju, "소주", "기타", "soju");
        add(icons, R.drawable.item_oligosaccharide, "올리고당", "기타", "oligosaccharide");
        add(icons, R.drawable.item_mirim, "미림", "기타", "mirim");
        add(icons, R.drawable.item_kalguksu_noodle, "칼국수면", "기타", "kalguksu_noodle");
        add(icons, R.drawable.item_black_pepper_whole, "통후추", "기타", "black_pepper_whole");
        add(icons, R.drawable.item_minced_ginger, "다진생강", "기타", "minced_ginger");
        add(icons, R.drawable.item_hondashi, "혼다시", "기타", "hondashi");
        add(icons, R.drawable.item_katsuobushi, "가쓰오부시", "기타", "katsuobushi");
        add(icons, R.drawable.item_black_sesame_dressing, "흑임자드레싱", "기타", "black_sesame_dressing");
        add(icons, R.drawable.item_cham_sauce, "참소스", "기타", "cham_sauce");
        add(icons, R.drawable.item_perilla_powder, "들깨가루", "기타", "perilla_powder");
        add(icons, R.drawable.item_udon_noodle, "우동사리", "기타", "udon_noodle");
        add(icons, R.drawable.item_anchovy_broth_coin, "멸치코인육수", "기타", "anchovy_broth_coin");

        // ✅ 레시피/직접등록 등 기본 아이콘
        add(icons, R.drawable.ic_custom_item, "기본재료", "기타", "custom_item");

        CACHED_ICONS = Collections.unmodifiableList(icons);
        buildRawKeyMapIfNeeded();

        return CACHED_ICONS;
    }

    private static void buildRawKeyMapIfNeeded() {
        if (RAWKEY_TO_RESID != null) return;
        if (CACHED_ICONS == null) return;

        RAWKEY_TO_RESID = new HashMap<>();

        for (IconItem icon : CACHED_ICONS) {
            if (icon == null) continue;

            String key = normalize(icon.getRawKey());
            if (key.isEmpty()) continue;

            if (!RAWKEY_TO_RESID.containsKey(key)) {
                RAWKEY_TO_RESID.put(key, icon.getResId());
            }
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.getDefault());
    }

    public static int findResIdByRawKey(String rawKey) {
        if (CACHED_ICONS == null) getAllIcons();
        buildRawKeyMapIfNeeded();

        String key = normalize(rawKey);
        Integer resId = RAWKEY_TO_RESID.get(key);

        return (resId != null) ? resId : 0;
    }

    public static String findDrawableNameByRawKey(String rawKey) {
        int resId = findResIdByRawKey(rawKey);

        if (resId == 0) return "";

        return ResourceNameHelper.getNameFromResId(resId);
    }
}