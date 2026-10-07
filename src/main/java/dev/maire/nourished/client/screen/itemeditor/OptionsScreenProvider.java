package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.ToggleOption;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * File &gt; Options: per-item settings outside the nutrient sliders, for the item currently loaded
 * in MariesLib's generic item editor — the "Excluded from classification" toggle, writing to
 * {@link ExcludedFoodOverrideRegistry} (the editor-authored exclusion layer, separate from
 * MariesLib's own escape-hatch {@code ExcludedItemsRegistry}) — plus one mod-wide setting that
 * doesn't belong to any one item: "Diminishing Returns (All Food)", writing straight to {@link
 * NourishedConfig#setEnableDiminishingReturns}, the same master switch the Advanced config
 * category already exposes. It's surfaced here too since this editor is where players already go
 * to turn off diminishing returns for a single food (the per-item exclude toggle above); this row
 * is the same idea at the "all food" scope instead of "this food" scope. More per-item settings
 * belong here as rows rather than as further File-menu screens. Registered with {@code
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

        @Nullable
        private final String sourceId;
        private boolean excluded;
        private boolean diminishingReturnsEnabled;
        private final OptionLayout layout;

        OptionsScreen(@Nullable String sourceId) {
            this.sourceId = sourceId;
            this.excluded = sourceId != null && ExcludedFoodOverrideRegistry.isExcluded(sourceId);
            this.diminishingReturnsEnabled = NourishedConfig.get().enableDiminishingReturns();
            this.layout = new OptionLayout("nourished-options");
            layout.addTab("");
            layout.addRow(new ToggleOption("Excluded from classification", () -> excluded, v -> excluded = v, () -> {}));
            layout.addRow(new ToggleOption("Diminishing Returns (All Food)",
                    () -> diminishingReturnsEnabled, v -> diminishingReturnsEnabled = v, () -> {}));
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
