package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.runtime.SourceClassificationRegistry;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.itemeditor.recipe.RecipeDisplayLookup;
import dev.marie.framework.ui.toolbox.CycleOption;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.OptionRow;
import dev.marie.framework.ui.toolbox.OptionStyle;
import dev.maire.nourished.core.context.NourishedItems;
import dev.maire.nourished.core.nutrition.CustomCaloriesOverrideRegistry;
import dev.maire.nourished.core.nutrition.DiminishingReturnsOverrideRegistry;
import dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoCaloriesOverrideRegistry;
import dev.maire.nourished.core.nutrition.NoRecentMealsOverrideRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.maire.nourished.core.nutrition.SilentEatOverrideRegistry;
import dev.maire.nourished.modules.RawFood.core.RawFoodTierOverrideRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * File &gt; Browse Items: an editor-specific filter over every candidate item instead of a full
 * search engine — "Show" picks one of {@link Show}'s seven views and the list rebuilds for it.
 * Clicking an entry retargets the editor to it (the built-in Values view, via MariesLib's {@link
 * ItemEditorPage#attachRetargetHandler}), the same as picking it up from JEI/EMI would.
 * <p>
 * The candidate universe (before "Show" narrows it) is every item {@link
 * NourishedItems#isNutritiousFood} already considers food, unioned with every item that has a
 * saved override anywhere (so something deliberately classified despite not looking like vanilla
 * food — e.g. via a {@code nourished:nutrients/*} tag — still shows up). Paged at {@value
 * #PAGE_SIZE} rows: {@link OptionLayout} draws every row it holds regardless of scroll position,
 * so an unpaged "All"/"Unmodified" on a large modpack would mean thousands of per-frame draws — a
 * "Page" cycle steps through the rest, so every match stays reachable.
 */
@ApiStatus.Internal
public final class BrowseItemsScreenProvider implements ItemEditorScreenProvider {

    private static final int PAGE_SIZE = 150;

    private enum Show {
        ALL("All"),
        MODIFIED("Modified"),
        UNMODIFIED("Unmodified"),
        MISSING_NUTRITION("Missing Nutrition"),
        EXCLUDED("Excluded"),
        FOODS("Foods"),
        WITH_RECIPES("Items with Recipes");

        final String label;

        Show(String label) {
            this.label = label;
        }

        static String[] labels() {
            String[] out = new String[values().length];
            for (int i = 0; i < out.length; i++) {
                out[i] = values()[i].label;
            }
            return out;
        }
    }

    @Override
    public String menuLabel() {
        return "Browse Items";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new BrowseItemsScreen(modId);
    }

    private static final class BrowseItemsScreen implements ItemEditorPage {

        /** Fixed window height this page asks for, regardless of how many rows actually matched — see {@link #constraint}. */
        private static final int PREFERRED_HEIGHT = 220;

        private final String modId;
        private int showIndex;
        private int pageIndex;
        private OptionLayout layout;
        @Nullable
        private BiConsumer<ItemStack, ItemEditorScreenProvider> retargetHandler;

        BrowseItemsScreen(String modId) {
            this.modId = modId;
            rebuild();
        }

        private void rebuild() {
            Show show = Show.values()[showIndex];
            List<ItemStack> results = candidatesFor(modId, show);

            OptionLayout next = new OptionLayout("nourished-browse-items");
            next.addTab("");
            next.addRow(new CycleOption("Show", Show.labels(), () -> showIndex, i -> {
                showIndex = i;
                pageIndex = 0;
                rebuild();
            }, () -> {}));
            next.addRow(new CountRow(results.size()));
            int pageCount = Math.max(1, (results.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            pageIndex = Math.min(pageIndex, pageCount - 1);
            if (pageCount > 1) {
                next.addRow(new CycleOption("Page", pageLabels(pageCount), () -> pageIndex, i -> {
                    pageIndex = i;
                    rebuild();
                }, () -> {}));
            }
            int start = pageIndex * PAGE_SIZE;
            int end = Math.min(results.size(), start + PAGE_SIZE);
            for (int i = start; i < end; i++) {
                ItemStack stack = results.get(i);
                next.addRow(new BrowseItemRow(stack, () -> {
                    if (retargetHandler != null) {
                        retargetHandler.accept(stack, null);
                    }
                }));
            }
            this.layout = next;
        }

        private static String[] pageLabels(int pageCount) {
            String[] labels = new String[pageCount];
            for (int i = 0; i < pageCount; i++) {
                labels[i] = (i + 1) + " / " + pageCount;
            }
            return labels;
        }

        private static List<ItemStack> candidatesFor(String modId, Show show) {
            RecipeManager recipeManager = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getRecipeManager() : null;
            AtomicReference<Set<String>> box = new AtomicReference<>();
            MarieContext.runAs(MarieContext.forMod(modId), () -> box.set(SourceClassificationRegistry.getAll().keySet()));
            Set<String> valuesOverridden = box.get();

            List<ItemStack> results = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) {
                    continue;
                }
                String itemId = id.toString();
                ItemStack stack = new ItemStack(item);
                boolean isFood = NourishedItems.isNutritiousFood(stack);
                boolean modified = isModified(itemId, valuesOverridden);
                if (!isFood && !modified) {
                    continue;
                }
                boolean excluded = NutrientClassificationLookup.isExcluded(itemId);
                boolean matches = switch (show) {
                    case ALL -> true;
                    case MODIFIED -> modified;
                    case UNMODIFIED -> !modified;
                    case MISSING_NUTRITION -> isFood && !excluded && NutrientClassificationLookup.resolveBars(item).isEmpty();
                    case EXCLUDED -> excluded;
                    case FOODS -> isFood;
                    case WITH_RECIPES -> RecipeDisplayLookup.find(stack, recipeManager) != null;
                };
                if (matches) {
                    results.add(stack);
                }
            }
            results.sort(Comparator.comparing(s -> s.getHoverName().getString()));
            return results;
        }

        private static boolean isModified(String itemId, Set<String> valuesOverridden) {
            return ExcludedFoodOverrideRegistry.isExcluded(itemId)
                    || NoCaloriesOverrideRegistry.isCaloriesOff(itemId)
                    || CustomCaloriesOverrideRegistry.getOverride(itemId) != null
                    || SilentEatOverrideRegistry.isSilentEat(itemId)
                    || NoRecentMealsOverrideRegistry.isExcluded(itemId)
                    || DiminishingReturnsOverrideRegistry.isDiminishingReturnsOff(itemId)
                    || RawFoodTierOverrideRegistry.getOverride(itemId) != null
                    || valuesOverridden.contains(itemId);
        }

        @Override
        public void attachRetargetHandler(BiConsumer<ItemStack, ItemEditorScreenProvider> retarget) {
            this.retargetHandler = retarget;
        }

        @Override
        public String id() {
            return "nourished-browse-items-screen";
        }

        @Override
        public Constraint constraint() {
            // Deliberately NOT layout.constraint(): that reports the natural height of every row
            // combined, and with up to PAGE_SIZE+3 rows that would balloon the editor window to
            // an enormous size (and un-resizable, since the window's own floor already sits at this
            // reported preferred size). A fixed, page-sized constraint instead keeps the window the
            // same size switching to/from this page that every other page already has, and relies on
            // OptionLayout's own documented behavior — given less room than its rows need, they clip
            // and scroll with the mouse wheel — exactly the existing scroll feature, not a new one.
            return Constraint.preferred(OptionStyle.PREFERRED_WIDTH, PREFERRED_HEIGHT);
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

    /** One matching item: icon + name, clickable to retarget the editor to it. */
    private static final class BrowseItemRow implements OptionRow {

        private static final int HEIGHT = 18;

        private final ItemStack stack;
        private final Runnable onClick;
        private BooleanSupplier enabled = () -> true;
        private Bounds bounds = new Bounds(0, 0, 0, 0);

        BrowseItemRow(ItemStack stack, Runnable onClick) {
            this.stack = stack;
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
            context.drawText(stack.getHoverName().getString(), bounds.x() + 20, bounds.y() + 5,
                    OptionStyle.labelColor(context, on), OptionStyle.TEXT_SCALE);
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

    /** A plain info line — the match count up top. */
    private static final class CountRow implements OptionRow {

        private static final int HEIGHT = 10;

        private final String text;

        CountRow(int count) {
            this.text = count + " match" + (count == 1 ? "" : "es");
        }

        @Override
        public int height() {
            return HEIGHT;
        }

        @Override
        public void enabledWhen(BooleanSupplier enabled) {}

        @Override
        public void render(RenderContext context, Bounds bounds) {
            context.drawText(text, bounds.x(), bounds.y(), OptionStyle.labelColor(context, true), OptionStyle.TEXT_SCALE);
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
