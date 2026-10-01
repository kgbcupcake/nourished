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

import java.util.List;

/**
 * The Diet Screen's left column. Calories, Balance, Recent Meals, Eat more of..., and Active Effects
 * are all independent {@link MarieComponent}s — {@link CaloriesComponent}, {@link BalanceComponent},
 * {@link RecentMealsComponent}, {@link EatMoreComponent}, {@link ActiveEffectsComponent} — built via
 * {@link DietScreenModules#build} from {@link dev.marie.framework.ui.component.ModuleRegistry} rather
 * than hardcoded fields, and looked up here by type via {@link DietScreenModules#find} rather than
 * list position. None of them are positioned by {@link #layout()} — a {@link Layout} recomputes child
 * position every {@code render()} call, which would silently override any future drag/resize commit
 * on the very next frame. Instead each resolves its own {@link Bounds} once at construction (an
 * offset from the panel's current position if the user has already committed a drag/resize,
 * otherwise the default stacked position — see {@link DietScreenPersistence
 * #resolveRelativeToPanel}), and this container renders them directly against that Bounds.
 * {@code layout()}/{@code columnLayout} are kept only for {@link Container} structural conformance
 * ({@code children()}/{@code addChild()} etc.), not because anything still calls
 * {@code computeBounds()} on them. (The column used to draw a "Today" header with a sunflower icon
 * above the boxes; it was removed, but the boxes still start at the same default height so they stay
 * level with the Intake Breakdown column.)
 */
public final class DietLeftColumnComponent implements Container {

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
     * Local (pre-scale) Y where the column's boxes start (where the removed "Today" header used to
     * end, kept so the boxes stay level with the Intake Breakdown column) — the start position handed to the first module in {@link DietScreenModules#build}'s
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
     * Where the next stacked element should start, in local (pre-scale) units — see {@link DietStacking#nextSiblingStartLocalY}
     * for the rule (footprint-height advance, and the out-of-flow exception for a box dragged out of the column).
     */
    public static int nextSiblingStartLocalY(int currentLocalY, int localHeight, Bounds resolvedBounds, DietLayout.Layout layout) {
        return DietStacking.nextSiblingStartLocalY(currentLocalY, localHeight, resolvedBounds, layout);
    }

    /** Same as above, against an arbitrary expected content X — see {@link DietStacking#nextSiblingStartLocalY(int, int, Bounds, DietLayout.Layout, int)}. */
    public static int nextSiblingStartLocalY(int currentLocalY, int localHeight, Bounds resolvedBounds, DietLayout.Layout layout, int expectedContentX) {
        return DietStacking.nextSiblingStartLocalY(currentLocalY, localHeight, resolvedBounds, layout, expectedContentX);
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
}
