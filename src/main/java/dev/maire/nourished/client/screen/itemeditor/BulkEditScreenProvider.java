package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.runtime.SourceClassificationRegistry;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.ButtonOption;
import dev.marie.framework.ui.toolbox.CycleOption;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.OptionRow;
import dev.marie.framework.ui.toolbox.OptionStyle;
import dev.marie.framework.util.MarieRegistryUtils;
import dev.maire.nourished.core.nutrition.DiminishingReturnsOverrideRegistry;
import dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoCaloriesOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoRecentMealsOverrideRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.SilentEatOverrideRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * File &gt; Bulk Edit: applies a property change across several items at once instead of one at a
 * time. Unlike every other page, this one claims item drops for itself ({@link
 * #acceptDraggedItem}) and appends to a running selection list instead of retargeting the editor's
 * single slot — the slot/Values view are left exactly as they were the whole time this page is
 * open, so switching back to Values (or any other page) still shows whatever was targeted before
 * Bulk Edit started.
 * <p>
 * Each togglable property is a 3-way "No Change / On / Off" cycle rather than a single toggle, so
 * leaving a row at "No Change" genuinely skips that field for every selected item instead of
 * forcing one value onto all of them. "Nutrient Group" is the exception — add or remove one nutrient
 * across the whole selection, writing straight to {@code SourceClassificationRegistry} the same way
 * {@code NutrientGroupsScreenProvider} does for a single item.
 *
 * <p>Has no header Save/Revert behavior of its own (nothing here is edited-then-committed the way
 * other pages are) — "Apply to Selected" does the writing (and the registry saves) immediately when
 * clicked, and the selection itself is in-memory only, discarded when the editor closes or retargets
 * away from this page through any path other than a drag-drop.
 */
@ApiStatus.Internal
public final class BulkEditScreenProvider implements ItemEditorScreenProvider {

    @Override
    public String menuLabel() {
        return "Bulk Edit";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new BulkEditScreen(modId);
    }

    private static final class BulkEditScreen implements ItemEditorPage {

        private static final String[] TRI_STATE_LABELS = {"No Change", "On", "Off"};

        private final String modId;
        private final List<ItemStack> selected = new ArrayList<>();

        private int excludedIndex;
        private int caloriesOffIndex;
        private int silentEatIndex;
        private int excludeRecentMealsIndex;
        private int diminishingReturnsOffIndex;
        private int nutrientGroupIndex;
        private int groupActionIndex;

        private OptionLayout layout;
        private String[] nutrientGroupLabels;

        BulkEditScreen(String modId) {
            this.modId = modId;
            rebuildLayout();
        }

        @Override
        public boolean acceptDraggedItem(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return true;
            }
            boolean alreadySelected = selected.stream().anyMatch(s -> s.getItem() == stack.getItem());
            if (!alreadySelected) {
                selected.add(stack.copy());
                rebuildLayout();
            }
            return true;
        }

        private void removeSelected(ItemStack stack) {
            selected.removeIf(s -> s.getItem() == stack.getItem());
            rebuildLayout();
        }

        private void rebuildLayout() {
            List<NutrientRegistry.NutrientDef> nutrients = NutrientRegistry.getAll();
            nutrientGroupLabels = new String[nutrients.size() + 1];
            nutrientGroupLabels[0] = "No Change";
            for (int i = 0; i < nutrients.size(); i++) {
                nutrientGroupLabels[i + 1] = nutrients.get(i).displayName();
            }
            if (nutrientGroupIndex >= nutrientGroupLabels.length) {
                nutrientGroupIndex = 0;
            }

            OptionLayout next = new OptionLayout("nourished-bulk-edit");
            next.addTab("");
            for (ItemStack stack : List.copyOf(selected)) {
                next.addRow(new SelectedItemRow(stack, () -> removeSelected(stack)));
            }
            if (!selected.isEmpty()) {
                next.addRow(new ButtonOption("Clear Selection", "CLEAR", this::clearSelection, () -> {}));
            }
            next.addRow(new CycleOption("Excluded from Classification", TRI_STATE_LABELS,
                    () -> excludedIndex, i -> excludedIndex = i, () -> {}));
            next.addRow(new CycleOption("Calories Off", TRI_STATE_LABELS,
                    () -> caloriesOffIndex, i -> caloriesOffIndex = i, () -> {}));
            next.addRow(new CycleOption("Silent Eat", TRI_STATE_LABELS,
                    () -> silentEatIndex, i -> silentEatIndex = i, () -> {}));
            next.addRow(new CycleOption("Exclude from Recent Meals", TRI_STATE_LABELS,
                    () -> excludeRecentMealsIndex, i -> excludeRecentMealsIndex = i, () -> {}));
            next.addRow(new CycleOption("Diminishing Returns Off", TRI_STATE_LABELS,
                    () -> diminishingReturnsOffIndex, i -> diminishingReturnsOffIndex = i, () -> {}));
            next.addRow(new CycleOption("Nutrient Group", nutrientGroupLabels,
                    () -> nutrientGroupIndex, i -> nutrientGroupIndex = i, () -> {}));
            next.addRow(new CycleOption("Group Action", new String[]{"Add", "Remove"},
                    () -> groupActionIndex, i -> groupActionIndex = i, () -> {}));
            next.enabledWhenLast(() -> nutrientGroupIndex > 0);
            next.addRow(new ButtonOption("Apply to Selected",
                    () -> "APPLY (" + selected.size() + ")", this::applyBulkEdit, () -> {}));
            this.layout = next;
        }

        private void clearSelection() {
            selected.clear();
            rebuildLayout();
        }

        private void applyBulkEdit() {
            if (selected.isEmpty()) {
                return;
            }
            List<String> itemIds = new ArrayList<>();
            for (ItemStack stack : selected) {
                ResourceLocation id = MarieRegistryUtils.itemKey(stack.getItem());
                if (id != null) {
                    itemIds.add(id.toString());
                }
            }

            applyTriState(excludedIndex, itemIds, ExcludedFoodOverrideRegistry::addExcluded,
                    ExcludedFoodOverrideRegistry::removeExcluded, ExcludedFoodOverrideRegistry::save);
            applyTriState(caloriesOffIndex, itemIds, NoCaloriesOverrideRegistry::addCaloriesOff,
                    NoCaloriesOverrideRegistry::removeCaloriesOff, NoCaloriesOverrideRegistry::save);
            applyTriState(silentEatIndex, itemIds, SilentEatOverrideRegistry::addSilentEat,
                    SilentEatOverrideRegistry::removeSilentEat, SilentEatOverrideRegistry::save);
            applyTriState(excludeRecentMealsIndex, itemIds, NoRecentMealsOverrideRegistry::addExcluded,
                    NoRecentMealsOverrideRegistry::removeExcluded, NoRecentMealsOverrideRegistry::save);
            applyTriState(diminishingReturnsOffIndex, itemIds,
                    DiminishingReturnsOverrideRegistry::addDiminishingReturnsOff,
                    DiminishingReturnsOverrideRegistry::removeDiminishingReturnsOff,
                    DiminishingReturnsOverrideRegistry::save);

            if (nutrientGroupIndex > 0) {
                String nutrientKey = NutrientRegistry.getAll().get(nutrientGroupIndex - 1).key();
                boolean add = groupActionIndex == 0;
                MarieContext.runAs(MarieContext.forMod(modId), () -> {
                    for (String itemId : itemIds) {
                        SourceClassificationRegistry.SourceClassification existing =
                                SourceClassificationRegistry.get(itemId);
                        LinkedHashMap<String, Float> values = existing != null
                                ? new LinkedHashMap<>(existing.values()) : new LinkedHashMap<>();
                        int calories = existing != null ? existing.calories() : 0;
                        boolean enabled = existing == null || existing.enabled();
                        if (add) {
                            values.putIfAbsent(nutrientKey, 1.0f);
                        } else {
                            values.remove(nutrientKey);
                        }
                        SourceClassificationRegistry.setOverride(itemId, values, calories, enabled);
                    }
                    SourceClassificationRegistry.save();
                });
            }
        }

        /** 0 = No Change (skip), 1 = On (add to every item), 2 = Off (remove from every item). */
        private void applyTriState(int index, List<String> itemIds, java.util.function.Consumer<String> add,
                                    java.util.function.Consumer<String> remove, Runnable save) {
            if (index == 0) {
                return;
            }
            for (String itemId : itemIds) {
                if (index == 1) {
                    add.accept(itemId);
                } else {
                    remove.accept(itemId);
                }
            }
            save.run();
        }

        @Override
        public String id() {
            return "nourished-bulk-edit-screen";
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

    /** One selected item: icon, name, and a "Remove" action filling the row — clicking anywhere on it removes that item from the bulk selection. */
    private static final class SelectedItemRow implements OptionRow {

        private static final int HEIGHT = 18;
        private static final int REMOVE_COLOR = 0xFFE05A5A;

        private final ItemStack stack;
        private final Runnable onRemove;
        private BooleanSupplier enabled = () -> true;
        private Bounds bounds = new Bounds(0, 0, 0, 0);

        SelectedItemRow(ItemStack stack, Runnable onRemove) {
            this.stack = stack;
            this.onRemove = onRemove;
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
            int removeWidth = context.textWidth("Remove", OptionStyle.TEXT_SCALE);
            context.drawText("Remove", bounds.x() + bounds.width() - removeWidth, bounds.y() + 5,
                    on ? REMOVE_COLOR : OptionStyle.dimmed(REMOVE_COLOR), OptionStyle.TEXT_SCALE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY) {
            if (!bounds.contains((int) mouseX, (int) mouseY)) {
                return false;
            }
            if (enabled.getAsBoolean()) {
                onRemove.run();
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
}
