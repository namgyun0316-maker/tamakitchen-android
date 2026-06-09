    package com.namgyun.tamakitchen.ui.home;

    import androidx.annotation.NonNull;
    import androidx.fragment.app.Fragment;

    import com.namgyun.tamakitchen.pet.IngredientCharacter;
    import com.namgyun.tamakitchen.pet.PetPrefs;
    import com.namgyun.tamakitchen.ui.collection.CharacterCollectionCategory;
    import com.namgyun.tamakitchen.ui.collection.CharacterCollectionItem;
    import com.namgyun.tamakitchen.ui.collection.CollectionCatalog;
    import com.namgyun.tamakitchen.ui.collection.CollectionDisplayPrefs;
    import com.namgyun.tamakitchen.ui.collection.CollectionInventoryPrefs;
    import com.namgyun.tamakitchen.ui.collection.CollectionPetStatePrefs;

    import java.util.List;

    public class HomeStateProvider {

        private final Fragment fragment;

        public HomeStateProvider(@NonNull Fragment fragment) {
            this.fragment = fragment;
        }

        public void initPetState() {
            if (!fragment.isAdded()) return;

            PetPrefs.ensureEggGranted(fragment.requireContext());
            PetPrefs.migrateLegacyUnlockedCharacterCountsIfNeeded(fragment.requireContext());
            PetPrefs.applyScheduledHunger(fragment.requireContext());
            PetPrefs.syncStageWithLevel(fragment.requireContext());
            PetPrefs.ensureIngredientSelectedIfNeeded(fragment.requireContext());
            PetPrefs.unlockSelectedCharacterIfNeeded(fragment.requireContext());

            // ✅ 예전 base state가 새 인스턴스에 덮어써지는 문제를 막기 위해 비활성화
            // migrateLegacyBaseStateToInstancesIfNeeded();

            normalizeSelectedCollectionKeyIfNeeded();
            ensureSelectedCollectionStateIfNeeded();
        }

        public void ensureSelectedCollectionStateIfNeeded() {
            if (!fragment.isAdded()) return;

            String key = getSelectedCollectionKey();
            if (key != null) {
                CollectionPetStatePrefs.ensureState(fragment.requireContext(), key);
            }
        }

        public void syncHomeDisplayPriority() {
            if (!fragment.isAdded()) return;

            normalizeSelectedCollectionKeyIfNeeded();

            String selectedKey = getSelectedCollectionKey();
            if (selectedKey == null || selectedKey.trim().isEmpty()) {
                return;
            }

            CharacterCollectionItem selectedItem =
                    CollectionCatalog.findByKey(fragment.requireContext(), selectedKey);

            if (selectedItem == null || !selectedItem.isUnlocked() || !selectedItem.isAvailableToUse()) {
                CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());
            }
        }

        public boolean shouldPrioritizeMainPetHatching() {
            if (!fragment.isAdded()) return false;

            normalizeSelectedCollectionKeyIfNeeded();

            String selectedKey = getSelectedCollectionKey();

            if (selectedKey != null && !selectedKey.trim().isEmpty()) {
                CharacterCollectionItem selectedItem =
                        CollectionCatalog.findByKey(fragment.requireContext(), selectedKey);

                if (selectedItem != null && selectedItem.isUnlocked() && selectedItem.isAvailableToUse()) {
                    return false;
                }
            }

            return PetPrefs.getStage(fragment.requireContext()) == PetPrefs.STAGE_EGG
                    || PetPrefs.shouldPlayHatchAnimation(fragment.requireContext());
        }

        public boolean isCollectionMode() {
            if (!fragment.isAdded()) return false;

            normalizeSelectedCollectionKeyIfNeeded();

            String selectedKey = getSelectedCollectionKey();
            if (selectedKey == null) return false;

            CharacterCollectionItem item = CollectionCatalog.findByKey(fragment.requireContext(), selectedKey);
            return item != null && item.isUnlocked() && item.isAvailableToUse();
        }

        public String getSelectedCollectionKey() {
            if (!fragment.isAdded()) return null;
            return CollectionDisplayPrefs.getSelectedItemKey(fragment.requireContext());
        }

        public boolean shouldPlaySelectedEggHatchAnimation() {
            if (!fragment.isAdded()) return false;

            String selectedKey = getSelectedCollectionKey();
            if (!CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)) return false;
            if (!isCollectionMode()) return false;

            return CollectionPetStatePrefs.getLevel(fragment.requireContext(), CollectionCatalog.KEY_EGG_BASIC)
                    >= PetPrefs.HATCH_LEVEL;
        }

        public boolean isSelectedCollectionEggAtMaxBeforeHatch() {
            if (!fragment.isAdded()) return false;
            if (!isCollectionMode()) return false;

            String selectedKey = getSelectedCollectionKey();
            if (!CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)) return false;

            return CollectionPetStatePrefs.getLevel(fragment.requireContext(), CollectionCatalog.KEY_EGG_BASIC)
                    >= PetPrefs.HATCH_LEVEL;
        }

        public int getCurrentStage() {
            if (!fragment.isAdded()) return PetPrefs.STAGE_EGG;

            if (isCollectionMode() && !shouldPrioritizeMainPetHatching()) {
                String key = getSelectedCollectionKey();

                if (CollectionCatalog.KEY_EGG_BASIC.equals(key)) {
                    return PetPrefs.STAGE_EGG;
                }

                CharacterCollectionItem item = CollectionCatalog.findByKey(fragment.requireContext(), key);
                if (item != null && item.getCategory() == CharacterCollectionCategory.FOOD) {
                    return PetPrefs.STAGE_FOOD;
                }
                return PetPrefs.STAGE_INGREDIENT;
            }

            return PetPrefs.getStage(fragment.requireContext());
        }

        public int getCurrentLevel() {
            if (!fragment.isAdded()) return 1;

            if (isCollectionMode() && !shouldPrioritizeMainPetHatching()) {
                String selectedKey = getSelectedCollectionKey();
                if (selectedKey == null || selectedKey.trim().isEmpty()) return 1;
                return CollectionPetStatePrefs.getLevel(fragment.requireContext(), selectedKey);
            }

            return PetPrefs.getLevel(fragment.requireContext());
        }

        public int getCurrentHunger() {
            if (!fragment.isAdded()) return 0;

            if (isCollectionMode() && !shouldPrioritizeMainPetHatching()) {
                String selectedKey = getSelectedCollectionKey();
                if (selectedKey == null || selectedKey.trim().isEmpty()) return 0;
                return CollectionPetStatePrefs.getHunger(fragment.requireContext(), selectedKey);
            }

            return PetPrefs.getHunger(fragment.requireContext());
        }

        public int getCurrentExpPercent() {
            if (!fragment.isAdded()) return 0;

            if (isCollectionMode() && !shouldPrioritizeMainPetHatching()) {
                String selectedKey = getSelectedCollectionKey();
                if (selectedKey == null || selectedKey.trim().isEmpty()) return 0;

                if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)
                        && CollectionPetStatePrefs.getLevel(fragment.requireContext(), selectedKey) >= PetPrefs.HATCH_LEVEL) {
                    return 100;
                }

                return CollectionPetStatePrefs.getExpPercent(fragment.requireContext(), selectedKey);
            }

            return PetPrefs.getCurrentLevelExpPercent(fragment.requireContext());
        }

        public int getCoins() {
            if (!fragment.isAdded()) return 0;
            return PetPrefs.getCoins(fragment.requireContext());
        }

        public int getFeedCount() {
            if (!fragment.isAdded()) return 0;
            return PetPrefs.getFeedCount(fragment.requireContext());
        }

        private void normalizeSelectedCollectionKeyIfNeeded() {
            if (!fragment.isAdded()) return;

            String selectedKey = CollectionDisplayPrefs.getSelectedItemKey(fragment.requireContext());
            if (selectedKey == null || selectedKey.trim().isEmpty()) return;

            if (CollectionCatalog.KEY_EGG_BASIC.equals(selectedKey)
                    || CollectionCatalog.KEY_FOOD_STRAWBERRY_CAKE.equals(selectedKey)
                    || CollectionInventoryPrefs.isIngredientInstanceKey(selectedKey)) {
                return;
            }

            IngredientCharacter character = parseIngredientCharacter(selectedKey);
            if (character == null) {
                return;
            }

            List<String> instanceKeys = CollectionInventoryPrefs.getIngredientInstanceKeys(
                    fragment.requireContext(),
                    character.name()
            );

            if (instanceKeys == null || instanceKeys.isEmpty()) {
                CollectionDisplayPrefs.clearSelectedItem(fragment.requireContext());
                return;
            }

            String bestKey = pickBestInstanceKey(instanceKeys);
            if (bestKey != null && !bestKey.equals(selectedKey)) {
                CollectionDisplayPrefs.saveSelectedItem(fragment.requireContext(), bestKey);
                CollectionPetStatePrefs.ensureState(fragment.requireContext(), bestKey);
            }
        }

        /**
         * ✅ 비활성화
         * 예전 base state를 새 인스턴스에 덮어쓰는 문제를 막기 위해 사용하지 않음.
         * 현재는 legacy state -> instance 복사를 여기서 하지 않는다.
         */
        @SuppressWarnings("unused")
        private void migrateLegacyBaseStateToInstancesIfNeeded() {
            // no-op
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean hasState(String key) {
            return key != null && CollectionPetStatePrefs.hasState(fragment.requireContext(), key);
        }

        private int getSafeLevel(String key) {
            if (key == null) return 0;
            return CollectionPetStatePrefs.getLevel(fragment.requireContext(), key);
        }

        private int getSafeExp(String key) {
            if (key == null) return 0;
            return CollectionPetStatePrefs.getExpPercent(fragment.requireContext(), key);
        }

        private String findBestLegacyStateKey(@NonNull IngredientCharacter character) {
            String nameKey = character.name();
            String idKey = character.getId();

            boolean hasName = hasState(nameKey);
            boolean hasId = hasState(idKey);

            if (!hasName && !hasId) return null;
            if (hasName && !hasId) return nameKey;
            if (!hasName) return idKey;

            int nameLevel = getSafeLevel(nameKey);
            int idLevel = getSafeLevel(idKey);

            if (nameLevel > idLevel) return nameKey;
            if (idLevel > nameLevel) return idKey;

            int nameExp = getSafeExp(nameKey);
            int idExp = getSafeExp(idKey);

            return nameExp >= idExp ? nameKey : idKey;
        }

        private IngredientCharacter parseIngredientCharacter(String rawKey) {
            if (rawKey == null || rawKey.trim().isEmpty()) return null;

            try {
                return IngredientCharacter.valueOf(rawKey);
            } catch (Exception ignored) {
            }

            for (IngredientCharacter character : IngredientCharacter.values()) {
                if (rawKey.equals(character.getId())) {
                    return character;
                }
            }

            return null;
        }

        private String pickBestInstanceKey(List<String> instanceKeys) {
            if (instanceKeys == null || instanceKeys.isEmpty()) return null;

            String bestKey = instanceKeys.get(0);
            int bestLevel = CollectionPetStatePrefs.getLevel(fragment.requireContext(), bestKey);
            int bestExp = CollectionPetStatePrefs.getExpPercent(fragment.requireContext(), bestKey);

            for (String key : instanceKeys) {
                int level = CollectionPetStatePrefs.getLevel(fragment.requireContext(), key);
                int exp = CollectionPetStatePrefs.getExpPercent(fragment.requireContext(), key);

                if (level > bestLevel || (level == bestLevel && exp > bestExp)) {
                    bestLevel = level;
                    bestExp = exp;
                    bestKey = key;
                }
            }

            return bestKey;
        }
    }