package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.layout.VerticalLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Only the "Today" header stays drawn directly here now (small static text/icon, not worth its own
 * class). Calories, Balance, Recent Meals, Eat more of..., and Active Effects are all independent
 * {@link MarieComponent}s — {@link CaloriesComponent}, {@link BalanceComponent}, {@link
 * RecentMealsComponent}, {@link EatMoreComponent}, {@link ActiveEffectsComponent} — built via {@link
 * DietScreenModules#build} from {@link dev.marie.framework.ui.component.ModuleRegistry} rather than
 * hardcoded fields, and looked up here by type via {@link DietScreenModules#find} rather than list
 * position. None of them are positioned by {@link #layout()} — a {@link Layout} recomputes child
 * position every {@code render()} call, which would silently override any future drag/resize commit
 * on the very next frame. Instead each resolves its own {@link Bounds} once at construction (an
 * offset from the panel's current position if the user has already committed a drag/resize,
 * otherwise today's default stacked position — see {@link DietScreenPersistence
 * #resolveRelativeToPanel}), and this container renders them directly against that Bounds.
 * {@code layout()}/{@code columnLayout} are kept only for {@link Container} structural conformance
 * ({@code children()}/{@code addChild()} etc.), not because anything still calls
 * {@code computeBounds()} on them. All coordinates for the "Today" header that stays inline here are
 * expressed in DietScreen's original local (pre-scale) pixel space and converted to absolute screen
 * pixels via {@link DietLayout}'s {@code toScreenX}/{@code toScreenY}/{@code toScreenDim} helpers,
 * same as {@link DietPanelContainer}.
 */
public final class DietLeftColumnComponent implements Container {

    private static final int COL_GOLD = 0xFFFFD65C;

    private final TrackingData data;
    private final DietLayout.Layout layout;
    private final int width;
    private final int height;
    private final int headerEndLocalY;
    private final List<MarieComponent> children;
    private final Layout columnLayout;
    private Bounds recentMealsRenderBounds;
    private Bounds eatMoreRenderBounds;
    private Bounds activeEffectsRenderBounds;
    private Bounds caloriesRenderBounds;
    private Bounds balanceRenderBounds;

    DietLeftColumnComponent(TrackingData data, DietLayout.Layout layout, int width, int height) {
        this.data = data;
        this.layout = layout;
        this.width = width;
        this.height = height;
        this.headerEndLocalY = computeHeaderEndLocalY();

        this.children = DietScreenModules.build(layout, headerEndLocalY);
        this.columnLayout = new VerticalLayout(0);
    }

    RecentMealsComponent recentMealsComponent() {
        return DietScreenModules.find(children, RecentMealsComponent.class);
    }

    EatMoreComponent eatMoreComponent() {
        return DietScreenModules.find(children, EatMoreComponent.class);
    }

    ActiveEffectsComponent activeEffectsComponent() {
        return DietScreenModules.find(children, ActiveEffectsComponent.class);
    }

    CaloriesComponent caloriesComponent() {
        return DietScreenModules.find(children, CaloriesComponent.class);
    }

    BalanceComponent balanceComponent() {
        return DietScreenModules.find(children, BalanceComponent.class);
    }

    /**
     * Overrides the bounds {@link #render} passes to the calories/balance/recent-meals/eat-more/
     * active-effects children instead of their own {@code resolvedBounds()} — for edit mode's live
     * drag/resize preview, so the single instance built here can be rendered at a live-tracked
     * position without a second, independently constructed copy. {@code null} for any param means
     * "use that child's own resolvedBounds()."
     */
    void setSubBoxRenderBounds(Bounds caloriesBounds, Bounds balanceBounds, Bounds recentMealsBounds, Bounds eatMoreBounds, Bounds activeEffectsBounds) {
        this.caloriesRenderBounds = caloriesBounds;
        this.balanceRenderBounds = balanceBounds;
        this.recentMealsRenderBounds = recentMealsBounds;
        this.eatMoreRenderBounds = eatMoreBounds;
        this.activeEffectsRenderBounds = activeEffectsBounds;
    }

    /**
     * Local (pre-scale) Y just past the "Today" header block (still drawn inline — see {@link
     * #render}) — the start position handed to the first module in {@link DietScreenModules#build}'s
     * chain. Calories/Balance are no longer pre-added here: as of their extraction into {@link
     * CaloriesComponent}/{@link BalanceComponent}, they're chained modules like RecentMeals/EatMore/
     * ActiveEffects, so their space is accounted for by the chain itself (via {@link
     * #nextSiblingStartLocalY}), not by this method precomputing their height in advance. Package-
     * private (not private) so {@link DietScreenEditTarget} can derive the same chain-start position
     * for its edit-mode overlay without duplicating this logic.
     */
    public static int computeHeaderEndLocalY() {
        return 20 + 10;
    }

    /**
     * Where the next stacked element should start, in local (pre-scale) units — advances by
     * {@code sibling}'s actual <em>footprint height</em> ({@code max(localHeight, resolvedBounds's
     * height converted to local units)}), not its content-only {@code localHeight()} alone. A box's
     * rendered height can now diverge from its content height once independently resized/persisted
     * (drag/resize, {@link DietScreenPersistence}), and the old {@code currentLocalY + localHeight}
     * formula didn't know that — so a resized-taller RecentMeals box wouldn't push EatMore/Active
     * Effects down, and they'd render overlapped by it.
     *
     * <p>Deliberately uses only {@code resolvedBounds.height()} — never {@code resolvedBounds.y()}
     * or {@code .x()} — so a box that's been <em>dragged</em> elsewhere (position changed, height
     * unchanged) doesn't drag its sibling's stacked position along with it; only an actual resize
     * (height genuinely larger than the natural content height) advances the next element. Using the
     * box's absolute Y previously coupled Active Effects' position to EatMore being moved, and fed a
     * position-inflated value back into EatMore's own {@code startLocalY} (used for its {@code
     * visible}/constraint-preferred-size computation), which could go degenerate whenever RecentMeals
     * had simply been dragged rather than resized.
     *
     * <p>Returns {@code currentLocalY} unchanged when {@code localHeight <= 0} (sibling hidden/not
     * fitting), matching the old formula's no-op in that case.
     *
     * <p>Delegates to {@link AutoGrowPanelContainer#nextSiblingStartLocalY} (marie-ui) — this used
     * to be the one implementation; it's now also reused by {@link
     * DietScreenModules#naturalContentEndLocalY} to sum the same modules' natural total height for
     * panel auto-grow, so the chaining formula itself lives in marie-ui rather than being
     * duplicated between the two callers.
     *
     * <p>One exception to "position never affects the advance": a sibling whose resolved X has
     * drifted away from the column's own left edge ({@code contentX}) by more than {@link
     * #OUT_OF_FLOW_X_TOLERANCE} is no longer sitting <em>in</em> the single-width vertical column at
     * all — the classic case being {@code EatMoreComponent} dragged to sit beside {@code
     * RecentMealsComponent} instead of below it, both still left-aligned to the panel edge in the
     * "natural" case but diverging in X the moment either is dragged sideways. Reserving that box's
     * full height for whoever comes next (as the height-only rule above does) then reserves dead
     * space nothing is actually rendered into — the next module (frequently {@code
     * ActiveEffectsComponent}) gets pushed down by a box's height despite that box no longer
     * occupying that vertical slot, which can push it (and its header/body fit checks) past the
     * live panel's bottom edge even though the box that would have caused that lives visibly
     * elsewhere on screen. This check only gates whether the advance happens at all — X is never fed
     * into <em>how far</em> it advances (still {@code resolvedBounds.height()} alone), so it can't
     * reintroduce the Y-feedback degeneracy the javadoc above already ruled out.
     */
    private static final int OUT_OF_FLOW_X_TOLERANCE = 6;

    public static int nextSiblingStartLocalY(int currentLocalY, int localHeight, Bounds resolvedBounds, DietLayout.Layout layout) {
        int contentX = layout.panelX() + layout.leftMargin();
        if (Math.abs(resolvedBounds.x() - contentX) > OUT_OF_FLOW_X_TOLERANCE) {
            return currentLocalY;
        }
        return AutoGrowPanelContainer.nextSiblingStartLocalY(currentLocalY, localHeight, resolvedBounds, layout.scale());
    }

    @Override
    public String id() {
        return "nourished.diet.panel.left";
    }

    @Override
    public List<MarieComponent> children() {
        return children;
    }

    @Override
    public void addChild(MarieComponent child) {
        children.add(child);
    }

    @Override
    public void removeChild(MarieComponent child) {
        children.remove(child);
    }

    @Override
    public Layout layout() {
        return columnLayout;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fixed(width, height);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        if (data == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        float scale = (float) layout.scale();

        int x = DietLayout.PAD;
        int y = 20;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;

        String todayText = Component.translatable("nourished.screen.diet.today").getString();
        int todayW = font.width(todayText);
        int todayGroupW = 16 + 4 + todayW;
        int todayStartX = x + (bw - todayGroupW) / 2;
        drawItem(context, "minecraft:sunflower", todayStartX, y - 8, scale);
        drawText(context, todayText, todayStartX + 20, y - 4, COL_GOLD, scale);

        CaloriesComponent calories = caloriesComponent();
        BalanceComponent balance = balanceComponent();
        RecentMealsComponent recentMeals = recentMealsComponent();
        EatMoreComponent eatMore = eatMoreComponent();
        ActiveEffectsComponent activeEffects = activeEffectsComponent();

        Bounds caloriesBounds = caloriesRenderBounds != null ? caloriesRenderBounds : calories.resolvedBounds();
        Bounds balanceBounds = balanceRenderBounds != null ? balanceRenderBounds : balance.resolvedBounds();
        Bounds recentMealsBounds = recentMealsRenderBounds != null ? recentMealsRenderBounds : recentMeals.resolvedBounds();
        Bounds eatMoreBounds = eatMoreRenderBounds != null ? eatMoreRenderBounds : eatMore.resolvedBounds();
        Bounds activeEffectsBounds = activeEffectsRenderBounds != null ? activeEffectsRenderBounds : activeEffects.resolvedBounds();

        if (calories.visibilityRule().isVisible()) {
            calories.render(context, caloriesBounds);
        }
        if (balance.visibilityRule().isVisible()) {
            balance.render(context, balanceBounds);
        }
        if (recentMeals.visibilityRule().isVisible()) {
            recentMeals.render(context, recentMealsBounds);
        }
        if (eatMore.visibilityRule().isVisible()) {
            eatMore.render(context, eatMoreBounds);
        }
        if (activeEffects.visibilityRule().isVisible()) {
            activeEffects.render(context, activeEffectsBounds);
        }
    }

    // ── Coordinate + drawing helpers ─────────────────────────────────────────

    private int sx(int localX) {
        return DietLayout.toScreenX(layout, localX);
    }

    private int sy(int localY) {
        return DietLayout.toScreenY(layout, localY);
    }

    private int sd(int localDim) {
        return DietLayout.toScreenDim(layout, localDim);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(localY), color, scale);
    }

    private void drawItem(RenderContext context, String itemId, int localX, int localY, float scale) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(net.minecraft.world.item.Items.APPLE);
        context.drawItem(new ItemStack(item), sx(localX), sy(localY), scale);
    }

}
