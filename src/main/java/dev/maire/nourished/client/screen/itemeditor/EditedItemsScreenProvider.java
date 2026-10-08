package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.runtime.SourceClassificationRegistry;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.OptionRow;
import dev.marie.framework.ui.toolbox.OptionStyle;
import dev.maire.nourished.core.nutrition.CustomCaloriesOverrideRegistry;
import dev.maire.nourished.core.nutrition.DiminishingReturnsOverrideRegistry;
import dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoCaloriesOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoRecentMealsOverrideRegistry;
import dev.maire.nourished.core.nutrition.SilentEatOverrideRegistry;
import dev.maire.nourished.modules.RawFood.core.RawFoodTierOverrideRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * File &gt; Edited Items: every item with a saved override anywhere in Nourished's own item-editor
 * registries ({@link ExcludedFoodOverrideRegistry}, {@link NoCaloriesOverrideRegistry}, {@link
 * CustomCaloriesOverrideRegistry}, {@link SilentEatOverrideRegistry}, {@link
 * NoRecentMealsOverrideRegistry}, {@link DiminishingReturnsOverrideRegistry}, {@link
 * RawFoodTierOverrideRegistry}, and MariesLib's generic {@link SourceClassificationRegistry} for
 * the Values/Nutrient Groups pages) in one place, each tagged with which setting(s) it has, instead
 * of needing to already remember or re-discover what's been customized. Clicking an entry jumps the
 * editor straight to it — via MariesLib's new {@link ItemEditorPage#attachRetargetHandler}, so this
 * page can switch the editor's target the same way dragging an item in from JEI/EMI would, without
 * needing a reference to the host panel itself.
 *
 * <p>No Save/Revert of its own — nothing is edited on this page directly — but it's not a dead end
 * either: clicking an entry doesn't just jump to it, it opens the actual page that item's override
 * lives on (Options, Calories, or Nutrient Groups, by priority when it has more than one kind),
 * ready to change. Rebuilt fresh every time the page is opened so it always reflects whatever every
 * other page has actually saved.
 */
@ApiStatus.Internal
public final class EditedItemsScreenProvider implements ItemEditorScreenProvider {

    private static final Set<String> OPTIONS_TAGS =
            Set.of("Excluded", "Silent Eat", "No Recent Meals", "Raw Tier", "DR Off");
    private static final Set<String> CALORIES_TAGS = Set.of("Calories Off", "Custom Calories");

    @Override
    public String menuLabel() {
        return "Edited Items";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new EditedItemsScreen(modId);
    }

    /** Options wins over Calories wins over Nutrient Groups when an item has more than one kind of override — the single most likely place the player actually wants to land. */
    @Nullable
    private static ItemEditorScreenProvider routeFor(List<String> tags) {
        if (tags.stream().anyMatch(OPTIONS_TAGS::contains)) {
            return new OptionsScreenProvider();
        }
        if (tags.stream().anyMatch(CALORIES_TAGS::contains)) {
            return new CaloriesScreenProvider();
        }
        if (tags.contains("Values")) {
            return new NutrientGroupsScreenProvider();
        }
        return null;
    }

    private static final class EditedItemsScreen implements ItemEditorPage {

        private final OptionLayout layout;
        @Nullable
        private BiConsumer<ItemStack, ItemEditorScreenProvider> retargetHandler;

        EditedItemsScreen(String modId) {
            Map<String, List<String>> tagsByItem = new TreeMap<>();
            addTags(tagsByItem, ExcludedFoodOverrideRegistry.getAll(), "Excluded");
            addTags(tagsByItem, NoCaloriesOverrideRegistry.getAll(), "Calories Off");
            addTags(tagsByItem, CustomCaloriesOverrideRegistry.getAll().keySet(), "Custom Calories");
            addTags(tagsByItem, SilentEatOverrideRegistry.getAll(), "Silent Eat");
            addTags(tagsByItem, NoRecentMealsOverrideRegistry.getAll(), "No Recent Meals");
            addTags(tagsByItem, DiminishingReturnsOverrideRegistry.getAll(), "DR Off");
            addTags(tagsByItem, RawFoodTierOverrideRegistry.getAll().keySet(), "Raw Tier");
            MarieContext.runAs(MarieContext.forMod(modId), () ->
                    addTags(tagsByItem, SourceClassificationRegistry.getAll().keySet(), "Values"));

            this.layout = new OptionLayout("nourished-edited-items");
            layout.addTab("");
            if (tagsByItem.isEmpty()) {
                layout.addRow(new EmptyRow());
            }
            for (Map.Entry<String, List<String>> entry : tagsByItem.entrySet()) {
                ItemStack stack = resolveStack(entry.getKey());
                if (stack == null) {
                    continue;
                }
                String summary = String.join(", ", entry.getValue());
                ItemEditorScreenProvider target = routeFor(entry.getValue());
                layout.addRow(new EditedItemRow(stack, summary, () -> {
                    if (retargetHandler != null) {
                        retargetHandler.accept(stack, target);
                    }
                }));
            }
        }

        private static void addTags(Map<String, List<String>> tagsByItem, Iterable<String> itemIds, String tag) {
            for (String itemId : itemIds) {
                tagsByItem.computeIfAbsent(itemId, k -> new ArrayList<>()).add(tag);
            }
        }

        @Nullable
        private static ItemStack resolveStack(String itemId) {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null) {
                return null;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            return item == null ? null : new ItemStack(item);
        }

        @Override
        public void attachRetargetHandler(BiConsumer<ItemStack, ItemEditorScreenProvider> retarget) {
            this.retargetHandler = retarget;
        }

        @Override
        public String id() {
            return "nourished-edited-items-screen";
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

    /** One edited item: icon, name, and a comma-separated summary of which setting(s) it has — clicking anywhere jumps the editor to it. */
    private static final class EditedItemRow implements OptionRow {

        private static final int HEIGHT = 18;

        private final ItemStack stack;
        private final String summary;
        private final Runnable onClick;
        private BooleanSupplier enabled = () -> true;
        private Bounds bounds = new Bounds(0, 0, 0, 0);

        EditedItemRow(ItemStack stack, String summary, Runnable onClick) {
            this.stack = stack;
            this.summary = summary;
            this.onClick = onClick;
        }

        @Override
        public int height() {
            return HEIGHT;
        }

        @Override
        public void enabledWhen(BooleanSupplier enabled) {
            this.enabled = enabled;
        }

        @Override
        public void render(RenderContext context, Bounds bounds) {
            this.bounds = bounds;
            boolean on = enabled.getAsBoolean();
            context.drawItem(stack, bounds.x(), bounds.y() + 1, 1f);
            int labelColor = OptionStyle.labelColor(context, on);
            context.drawText(stack.getHoverName().getString(), bounds.x() + 20, bounds.y() + 5, labelColor, OptionStyle.TEXT_SCALE);
            int summaryWidth = context.textWidth(summary, OptionStyle.TEXT_SCALE);
            context.drawText(summary, bounds.x() + bounds.width() - summaryWidth, bounds.y() + 5,
                    OptionStyle.accentColor(on), OptionStyle.TEXT_SCALE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY) {
            if (!bounds.contains((int) mouseX, (int) mouseY)) {
                return false;
            }
            if (enabled.getAsBoolean()) {
                onClick.run();
            }
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
            return false;
        }
    }

    /** Shown in place of the list when nothing's been edited yet. */
    private static final class EmptyRow implements OptionRow {

        private static final int HEIGHT = 10;

        @Override
        public int height() {
            return HEIGHT;
        }

        @Override
        public void enabledWhen(BooleanSupplier enabled) {}

        @Override
        public void render(RenderContext context, Bounds bounds) {
            context.drawText("Nothing edited yet.", bounds.x(), bounds.y(), OptionStyle.labelColor(context, true), OptionStyle.TEXT_SCALE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
            return false;
        }
    }
}
