package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.layout.HorizontalLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MarieUI consumer for the Diet Screen panel as a whole. Title, divider, and two column leaves
 * (left summary column, right nutrient-intake column) have every draw call's local
 * (pre-PoseStack-scale) coordinate converted to an absolute screen pixel via
 * {@link DietLayout#toScreenX}/{@link DietLayout#toScreenY}/{@link DietLayout#toScreenDim} instead
 * of relying on a global PoseStack transform, mirroring how the HUD pass converted
 * {@code HudLayout}'s pre-scaled ints into {@code NutrientBarComponent} draw calls.
 *
 * <p>The main panel's own drag/resize is handled by {@link DietScreenEditTarget}; the
 * independently-resizable recent-meals and eat-more sub-boxes are flattened into the two column
 * leaves here rather than becoming their own draggable/resizable {@link MarieComponent}s.
 */
public final class DietPanelContainer implements Container {

    private static final int COL_BG_RGB = 0x00141414;
    private static final int COL_BORDER = 0xFF3A3A3A;
    private static final int COL_DIVIDER = 0xFF2E2E2E;
    private static final int COL_TITLE = 0xFF9BD36A;
    private static final int COL_GRAY = 0xFF666666;

    private final DietLayout.Layout layout;
    private final TrackingData data;
    private final List<MarieComponent> children;
    private final Layout columnLayout;

    public DietPanelContainer(TrackingData data, List<String> bars, Map<String, Float> displayValues, DietLayout.Layout layout) {
        this.layout = layout;
        this.data = data;

        int gap = DietLayout.toScreenDim(layout, 1);
        Bounds panelBounds = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        DietLayout.ColumnGeometry geometry = DietLayout.columnGeometry(layout, panelBounds);
        int columnHeight = layout.panelH();

        DietLeftColumnComponent left = new DietLeftColumnComponent(data, layout, geometry.leftWidth(), columnHeight);
        DietRightColumnComponent right = new DietRightColumnComponent(data, bars, displayValues, layout, geometry.rightWidth(), columnHeight);
        this.children = new ArrayList<>(List.of(left, right));
        this.columnLayout = new HorizontalLayout(gap);
    }

    @Override
    public String id() {
        return "nourished.diet.panel";
    }

    @Override
    public List<MarieComponent> children() {
        return children;
    }

    /**
     * The single {@link RecentMealsComponent}/{@link EatMoreComponent}/{@link
     * ActiveEffectsComponent} instances this container already constructed — for callers (edit mode)
     * that need to read their {@code resolvedBounds()} or override their render bounds without
     * constructing a second, independent copy.
     */
    public RecentMealsComponent recentMealsComponent() {
        return leftColumn().recentMealsComponent();
    }

    public EatMoreComponent eatMoreComponent() {
        return leftColumn().eatMoreComponent();
    }

    public ActiveEffectsComponent activeEffectsComponent() {
        return leftColumn().activeEffectsComponent();
    }

    public CaloriesComponent caloriesComponent() {
        return leftColumn().caloriesComponent();
    }

    public BalanceComponent balanceComponent() {
        return leftColumn().balanceComponent();
    }

    /**
     * Overrides the bounds {@link #render} will use for the calories/balance/recent-meals/eat-more/
     * active-effects sub-boxes instead of their own {@code resolvedBounds()} — for edit mode's live
     * drag/resize preview. {@code null} for any param means "use that child's own resolvedBounds(),"
     * i.e. the normal (non-edit) render path is unaffected.
     */
    public void setSubBoxRenderBounds(Bounds caloriesBounds, Bounds balanceBounds, Bounds recentMealsBounds, Bounds eatMoreBounds, Bounds activeEffectsBounds) {
        leftColumn().setSubBoxRenderBounds(caloriesBounds, balanceBounds, recentMealsBounds, eatMoreBounds, activeEffectsBounds);
    }

    private DietLeftColumnComponent leftColumn() {
        return (DietLeftColumnComponent) children.get(0);
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
        return Constraint.fixed(layout.panelW(), layout.panelH());
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        double opacity = NourishedClientConfig.get().dietBackgroundOpacity();
        int alpha = Math.max(0, Math.min(255, (int) Math.round(opacity * 255.0d)));
        int fill = (alpha << 24) | (COL_BG_RGB & 0x00FFFFFF);
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, fill, COL_BORDER);

        float scale = (float) layout.scale();
        Font font = Minecraft.getInstance().font;
        String title = "☘ Diet ☘";
        int titleW = (int) Math.ceil(font.width(title) * scale);
        int titleX = bounds.x() + (bounds.width() - titleW) / 2;
        context.drawText(title, titleX, DietLayout.toScreenY(layout, 9), COL_TITLE, scale);

        // Panel-level minimize: below this threshold, the panel shows title-bar only — divider, both
        // columns (and everything under them, including every sub-box's own collapse behavior), and
        // the "no player" message all short-circuit here. This is a coarser, higher-priority check
        // than any per-sub-box HeaderCollapsibleComponent fit-check: those never even get evaluated
        // once minimized, since Container.super.render (which is what invokes
        // DietLeftColumnComponent/DietRightColumnComponent, and in turn RecentMeals/EatMore/
        // ActiveEffects/Calories/Balance) is never reached. The edit-mode toggle and this panel's own
        // resize handle are drawn by the caller (DietScreenEditTarget/DietScreen), not here, and are
        // never gated on this — the handle must stay grabbable to un-minimize.
        if (bounds.height() < DietLayout.toScreenDim(layout, DietLayout.PANEL_MINIMIZED_THRESHOLD_LOCAL_HEIGHT)) {
            return;
        }

        // dividerBottom/textY anchor to the panel's live bottom edge (bounds.y() + bounds.height())
        // instead of DietLayout.toScreenY(layout, DietLayout.HEIGHT - ...) — the latter recomputes a
        // screen position from the fixed DietLayout.HEIGHT constant via layout.scale() (a width-only
        // derived value), which silently assumes the panel's actual height always equals
        // HEIGHT * scale. That's only true under proportional (corner) resize; a panel independently
        // shrunk shorter via its top/bottom edge has a real bounds.height() smaller than that
        // assumption, so the divider kept extending past the panel's actual (now shorter) bottom
        // edge instead of shrinking with it. bounds is this render() call's live, current-frame
        // Bounds — not cached — so anchoring to its bottom edge directly stays correct regardless of
        // which axis was independently resized.
        int dividerX = DietLayout.columnGeometry(layout, bounds).dividerX();
        int dividerTop = DietLayout.toScreenY(layout, 26);
        int dividerBottom = bounds.y() + bounds.height() - DietLayout.toScreenDim(layout, DietLayout.PANEL_BOTTOM_MARGIN_LOCAL);
        context.fillRect(dividerX, dividerTop, DietLayout.toScreenDim(layout, 1), Math.max(0, dividerBottom - dividerTop), COL_DIVIDER);

        if (data == null) {
            String noPlayer = Component.translatable("nourished.screen.diet.no_player").getString();
            int textW = (int) Math.ceil(font.width(noPlayer) * scale);
            int textX = bounds.x() + (bounds.width() - textW) / 2;
            int textY = bounds.y() + bounds.height() / 2;
            context.drawText(noPlayer, textX, textY, COL_GRAY, scale);
            return;
        }

        Container.super.render(context, bounds);
    }
}
