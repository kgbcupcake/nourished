package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.CycleOption;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.ToggleOption;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.DiminishingReturnsOverrideRegistry;
import dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoRecentMealsOverrideRegistry;
import dev.maire.nourished.core.nutrition.SilentEatOverrideRegistry;
import dev.maire.nourished.modules.RawFood.core.RawFoodTierOverrideRegistry;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * File &gt; Options: per-item settings outside the nutrient sliders and outside their own dedicated
 * category pages (see {@code CaloriesScreenProvider} for calorie-related settings) — the "Excluded
 * from classification" toggle, writing to {@link ExcludedFoodOverrideRegistry} (the
 * editor-authored exclusion layer, separate from MariesLib's own escape-hatch {@code
 * ExcludedItemsRegistry}) — the "Silent Eat" toggle, writing to {@link SilentEatOverrideRegistry}
 * (suppresses just the "you consumed N calories" notification; nutrients and calories still apply)
 * — the "Exclude from Recent Meals" toggle, writing to {@link NoRecentMealsOverrideRegistry} (keeps
 * the item out of the Diet Screen's Recent Meals list; nutrients and calories still apply) — the
 * "Raw Food Tier" cycle, writing to {@link RawFoodTierOverrideRegistry} (forces {@code
 * RawFoodClassifier}'s severity for this item instead of its tag/cookedness/token heuristics, for
 * modded raw foods that get misclassified) — the "Diminishing Returns Off" toggle, writing to
 * {@link DiminishingReturnsOverrideRegistry} (exempts just this item from the novelty/repetition
 * multiplier, regardless of the mod-wide toggle below) — plus one mod-wide setting that doesn't
 * belong to any one item: "Diminishing Returns (All Food)", writing straight to {@link
 * NourishedConfig#setEnableDiminishingReturns}, the same master switch the Advanced config
 * category already exposes. It's surfaced here too since this editor is where players already go
 * to turn off diminishing returns for a single food (the per-item toggle above is the item-scoped
 * version of the same idea).
 *
 * <p>A setting belongs here only once it doesn't fit an existing or new dedicated category page —
 * a new <em>category</em> of settings (calories, raw food, …) gets its own File-menu page via a new
 * {@link ItemEditorScreenProvider} instead of being piled onto this one. Registered with {@code
 * ItemEditorScreenProviderRegistry.register(Nourished.MODID, new OptionsScreenProvider())} from
 * {@code ClientEventRegistrar}.
 *
 * <p>Has no Save/Revert controls of its own — {@link ItemEditorPage#save()}/{@link
 * ItemEditorPage#revert()} are called by the item editor's own header-level Save/Revert buttons
 * whenever this page is the one showing; the File dropdown itself is pure navigation.
 */
@ApiStatus.Internal
public final class OptionsScreenProvider implements ItemEditorScreenProvider {

    @Override
    public String menuLabel() {
        return "Options";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new OptionsScreen(sourceId);
    }

    private static final class OptionsScreen implements ItemEditorPage {

        /** {@code "Auto", "Fine", "Mild", "Medium", "Severe"} — index 0 is "no override"; indices 1-4 map to {@link RawSeverity#values()} in declaration order. */
        private static final String[] RAW_TIER_LABELS = {"Auto", "Fine", "Mild", "Medium", "Severe"};

        @Nullable
        private final String sourceId;
        private boolean excluded;
        private boolean silentEat;
        private boolean excludedFromRecentMeals;
        private int rawTierIndex;
        private boolean diminishingReturnsOff;
        private boolean diminishingReturnsEnabled;
        private final OptionLayout layout;

        OptionsScreen(@Nullable String sourceId) {
            this.sourceId = sourceId;
            this.excluded = sourceId != null && ExcludedFoodOverrideRegistry.isExcluded(sourceId);
            this.silentEat = sourceId != null && SilentEatOverrideRegistry.isSilentEat(sourceId);
            this.excludedFromRecentMeals = sourceId != null && NoRecentMealsOverrideRegistry.isExcluded(sourceId);
            this.rawTierIndex = rawTierIndexFromRegistry(sourceId);
            this.diminishingReturnsOff = sourceId != null && DiminishingReturnsOverrideRegistry.isDiminishingReturnsOff(sourceId);
            this.diminishingReturnsEnabled = NourishedConfig.get().enableDiminishingReturns();
            this.layout = new OptionLayout("nourished-options");
            layout.addTab("");
            layout.addRow(new ToggleOption("Excluded from classification", () -> excluded, v -> excluded = v, () -> {}));
            layout.addRow(new ToggleOption("Silent Eat", () -> silentEat, v -> silentEat = v, () -> {}));
            layout.addRow(new ToggleOption("Exclude from Recent Meals",
                    () -> excludedFromRecentMeals, v -> excludedFromRecentMeals = v, () -> {}));
            layout.addRow(new CycleOption("Raw Food Tier", RAW_TIER_LABELS,
                    () -> rawTierIndex, i -> rawTierIndex = i, () -> {}));
            layout.addRow(new ToggleOption("Diminishing Returns Off",
                    () -> diminishingReturnsOff, v -> diminishingReturnsOff = v, () -> {}));
            layout.addRow(new ToggleOption("Diminishing Returns (All Food)",
                    () -> diminishingReturnsEnabled, v -> diminishingReturnsEnabled = v, () -> {}));
        }

        private static int rawTierIndexFromRegistry(@Nullable String sourceId) {
            if (sourceId == null) {
                return 0;
            }
            RawSeverity override = RawFoodTierOverrideRegistry.getOverride(sourceId);
            return override == null ? 0 : override.ordinal() + 1;
        }

        @Override
        public void save() {
            if (sourceId != null) {
                if (excluded) {
                    ExcludedFoodOverrideRegistry.addExcluded(sourceId);
                } else {
                    ExcludedFoodOverrideRegistry.removeExcluded(sourceId);
                }
                ExcludedFoodOverrideRegistry.save();

                if (silentEat) {
                    SilentEatOverrideRegistry.addSilentEat(sourceId);
                } else {
                    SilentEatOverrideRegistry.removeSilentEat(sourceId);
                }
                SilentEatOverrideRegistry.save();

                if (excludedFromRecentMeals) {
                    NoRecentMealsOverrideRegistry.addExcluded(sourceId);
                } else {
                    NoRecentMealsOverrideRegistry.removeExcluded(sourceId);
                }
                NoRecentMealsOverrideRegistry.save();

                if (rawTierIndex == 0) {
                    RawFoodTierOverrideRegistry.removeOverride(sourceId);
                } else {
                    RawFoodTierOverrideRegistry.setOverride(sourceId, RawSeverity.values()[rawTierIndex - 1]);
                }
                RawFoodTierOverrideRegistry.save();

                if (diminishingReturnsOff) {
                    DiminishingReturnsOverrideRegistry.addDiminishingReturnsOff(sourceId);
                } else {
                    DiminishingReturnsOverrideRegistry.removeDiminishingReturnsOff(sourceId);
                }
                DiminishingReturnsOverrideRegistry.save();
            }
            if (diminishingReturnsEnabled != NourishedConfig.get().enableDiminishingReturns()) {
                NourishedConfig.get().setEnableDiminishingReturns(diminishingReturnsEnabled);
                NourishedConfig.saveNow();
            }
        }

        @Override
        public void revert() {
            // Discards the unsaved toggle flips and puts each row back to what's actually
            // persisted — re-reading from the registry/config rather than just flipping the fields
            // back, since "revert" means "match disk", and those are the only things that know what
            // that is.
            excluded = sourceId != null && ExcludedFoodOverrideRegistry.isExcluded(sourceId);
            silentEat = sourceId != null && SilentEatOverrideRegistry.isSilentEat(sourceId);
            excludedFromRecentMeals = sourceId != null && NoRecentMealsOverrideRegistry.isExcluded(sourceId);
            rawTierIndex = rawTierIndexFromRegistry(sourceId);
            diminishingReturnsOff = sourceId != null && DiminishingReturnsOverrideRegistry.isDiminishingReturnsOff(sourceId);
            diminishingReturnsEnabled = NourishedConfig.get().enableDiminishingReturns();
        }

        @Override
        public String id() {
            return "nourished-options-screen";
        }

        @Override
        public Constraint constraint() {
            return layout.constraint();
        }

        @Override
        public void render(RenderContext context, Bounds bounds) {
            layout.render(context, bounds);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return layout.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return layout.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return layout.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return layout.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }
}
