package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.SliderOption;
import dev.marie.framework.ui.toolbox.ToggleOption;
import dev.maire.nourished.core.nutrition.CustomCaloriesOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoCaloriesOverrideRegistry;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * File &gt; Calories: every per-item setting that affects an item's calorie contribution, split out
 * from the generic Options page into its own category — "Calories Off" ({@link
 * NoCaloriesOverrideRegistry}, leaves nutrient application alone and just zeroes the item's calorie
 * contribution at the source) and "Use Custom Calories" plus its gated "Calories" slider ({@link
 * CustomCaloriesOverrideRegistry}, substitutes a fixed number for whatever the normal formula/
 * override would otherwise compute). "Calories Off" wins if both are set on the same item. This is
 * the pattern new setting categories follow going forward — their own File-menu page via a new
 * {@link ItemEditorScreenProvider}, not more rows piled onto Options — the same way "Exclude
 * Tooltip" already got its own page rather than living in Options. Registered with {@code
 * ItemEditorScreenProviderRegistry.register(Nourished.MODID, new CaloriesScreenProvider())} from
 * {@code ClientEventRegistrar}.
 *
 * <p>Has no Save/Revert controls of its own — {@link ItemEditorPage#save()}/{@link
 * ItemEditorPage#revert()} are called by the item editor's own header-level Save/Revert buttons
 * whenever this page is the one showing; the File dropdown itself is pure navigation.
 */
@ApiStatus.Internal
public final class CaloriesScreenProvider implements ItemEditorScreenProvider {

    @Override
    public String menuLabel() {
        return "Calories";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new CaloriesScreen(sourceId);
    }

    private static final class CaloriesScreen implements ItemEditorPage {

        private static final int CALORIES_MIN = 0;
        private static final int CALORIES_MAX = 5000;
        private static final int CALORIES_STEP = 10;
        private static final int DEFAULT_CUSTOM_CALORIES = 200;

        @Nullable
        private final String sourceId;
        private boolean caloriesOff;
        private boolean useCustomCalories;
        private int customCalories;
        private final OptionLayout layout;

        CaloriesScreen(@Nullable String sourceId) {
            this.sourceId = sourceId;
            this.caloriesOff = sourceId != null && NoCaloriesOverrideRegistry.isCaloriesOff(sourceId);
            Integer savedCustomCalories = sourceId != null ? CustomCaloriesOverrideRegistry.getOverride(sourceId) : null;
            this.useCustomCalories = savedCustomCalories != null;
            this.customCalories = savedCustomCalories != null ? savedCustomCalories : DEFAULT_CUSTOM_CALORIES;
            this.layout = new OptionLayout("nourished-calories");
            layout.addTab("");
            layout.addRow(new ToggleOption("Calories Off", () -> caloriesOff, v -> caloriesOff = v, () -> {}));
            layout.addRow(new ToggleOption("Use Custom Calories",
                    () -> useCustomCalories, v -> useCustomCalories = v, () -> {}));
            layout.addRow(SliderOption.ofInt("Calories", () -> customCalories, v -> customCalories = v,
                    CALORIES_MIN, CALORIES_MAX, CALORIES_STEP, "cal", () -> {}));
            layout.enabledWhenLast(() -> useCustomCalories);
        }

        @Override
        public void save() {
            if (sourceId == null) {
                return;
            }
            if (caloriesOff) {
                NoCaloriesOverrideRegistry.addCaloriesOff(sourceId);
            } else {
                NoCaloriesOverrideRegistry.removeCaloriesOff(sourceId);
            }
            NoCaloriesOverrideRegistry.save();

            if (useCustomCalories) {
                CustomCaloriesOverrideRegistry.setOverride(sourceId, customCalories);
            } else {
                CustomCaloriesOverrideRegistry.removeOverride(sourceId);
            }
            CustomCaloriesOverrideRegistry.save();
        }

        @Override
        public void revert() {
            // Discards the unsaved toggle flips and puts each row back to what's actually
            // persisted — re-reading from the registry rather than just flipping the fields back,
            // since "revert" means "match disk", and the registry is the only thing that knows what
            // that is.
            caloriesOff = sourceId != null && NoCaloriesOverrideRegistry.isCaloriesOff(sourceId);
            Integer savedCustomCalories = sourceId != null ? CustomCaloriesOverrideRegistry.getOverride(sourceId) : null;
            useCustomCalories = savedCustomCalories != null;
            customCalories = savedCustomCalories != null ? savedCustomCalories : DEFAULT_CUSTOM_CALORIES;
        }

        @Override
        public String id() {
            return "nourished-calories-screen";
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
