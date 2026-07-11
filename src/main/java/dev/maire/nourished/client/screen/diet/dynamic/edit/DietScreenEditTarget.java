package dev.maire.nourished.client.screen.diet.dynamic.edit;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.client.screen.diet.DietScreen;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietPanelLayoutResolver;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLeftColumnComponent;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietPanelContainer;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.SUMMARY_BOX_LOCAL_WIDTH;
import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.liveSubBoxConstraint;
import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.naturalPreferredSize;

/**
 * Long-lived {@link MarieComponent} target for Diet Screen edit mode — holds the drag/resize
 * trackers across the multiple frames a gesture spans, unlike the normal render path's components
 * which are rebuilt fresh each frame.
 */
public final class DietScreenEditTarget implements MarieComponent {

    private static final String ID = "nourished.diet.editwrapper";
    public static final String PANEL_ID = "nourished.diet.panel";

    private final Minecraft mc;
    private final Runnable exitEditMode;
    private final DraggableResizable panelDrag;
    private final DraggableResizable caloriesDrag;
    private final DraggableResizable balanceDrag;
    private final DraggableResizable recentMealsDrag;
    private final DraggableResizable eatMoreDrag;
    private final DraggableResizable activeEffectsDrag;

    private Bounds lastCaloriesResolvedBounds;
    private Bounds lastBalanceResolvedBounds;
    private Bounds lastRecentResolvedBounds;
    private Bounds lastEatMoreResolvedBounds;
    private Bounds lastActiveEffectsResolvedBounds;

    public DietScreenEditTarget(Minecraft mc, Runnable exitEditMode) {
        this.mc = mc;
        this.exitEditMode = exitEditMode;

        DietLayout.Layout baseLayout = DietLayout.compute(mc);
        List<MarieComponent> baseModules = DietScreenModules.build(baseLayout, DietLeftColumnComponent.computeHeaderEndLocalY());
        CaloriesComponent defaultCalories = DietScreenModules.find(baseModules, CaloriesComponent.class);
        BalanceComponent defaultBalance = DietScreenModules.find(baseModules, BalanceComponent.class);
        RecentMealsComponent defaultRecent = DietScreenModules.find(baseModules, RecentMealsComponent.class);
        EatMoreComponent defaultEatMore = DietScreenModules.find(baseModules, EatMoreComponent.class);
        ActiveEffectsComponent defaultActiveEffects = DietScreenModules.find(baseModules, ActiveEffectsComponent.class);

        Constraint panelConstraint = DietPanelLayoutResolver.panelConstraint(baseLayout);
        DraggableResizable[] panelDragRef = new DraggableResizable[1];
        panelDrag = new DraggableResizable(this, panelConstraint,
                (target, bounds) -> {
                    AutoGrowPanelContainer.ManualOverride existing = DietPanelLayoutResolver.existingManualOverride();
                    AutoGrowPanelContainer.ManualOverride override = AutoGrowPanelContainer.withCommit(existing, panelDragRef[0]);
                    int leftMargin = panelDragRef[0].lastCommitWasLeftEdge()
                            ? Math.max(0, DietPanelLayoutResolver.persistedLeftMargin() + (bounds.width() - resolvedPanelLayout(mc).panelW()))
                            : DietPanelLayoutResolver.persistedLeftMargin();

                    DietScreenPersistence.get().save(PANEL_ID, new ComponentState(
                            bounds.x(), bounds.y(), bounds.width(), bounds.height(), false,
                            override.widthManual(), override.heightManual(), leftMargin));
                });
        panelDragRef[0] = panelDrag;

        // Forward-reference indirection: each drag reads its own lastCommitAffected* from inside its
        // own onCommit lambda, so it needs a ref cell like panelDrag does.
        DraggableResizable[] caloriesDragRef = new DraggableResizable[1];
        DraggableResizable[] balanceDragRef = new DraggableResizable[1];
        DraggableResizable[] recentMealsDragRef = new DraggableResizable[1];
        DraggableResizable[] eatMoreDragRef = new DraggableResizable[1];
        DraggableResizable[] activeEffectsDragRef = new DraggableResizable[1];

        caloriesDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultCalories.naturalLocalHeight(), SUMMARY_BOX_LOCAL_WIDTH)),
                (target, bounds) -> {
                    if (mc.player == null) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultCalories.id(), toRelativeState(bounds, defaultCalories.id(), caloriesDragRef[0]));
                });
        caloriesDragRef[0] = caloriesDrag;

        balanceDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultBalance.naturalLocalHeight(), SUMMARY_BOX_LOCAL_WIDTH)),
                (target, bounds) -> {
                    if (mc.player == null) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultBalance.id(), toRelativeState(bounds, defaultBalance.id(), balanceDragRef[0]));
                });
        balanceDragRef[0] = balanceDrag;

        recentMealsDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultRecent.naturalLocalHeight())),
                (target, bounds) -> {
                    if (MarieClientCache.getRecentSourceIds().isEmpty()) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultRecent.id(), toRelativeState(bounds, defaultRecent.id(), recentMealsDragRef[0]));
                });
        recentMealsDragRef[0] = recentMealsDrag;

        eatMoreDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultEatMore.naturalLocalHeight())),
                (target, bounds) -> {
                    if (MarieClientCache.getNeglectedCategories().isEmpty()) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultEatMore.id(), toRelativeState(bounds, defaultEatMore.id(), eatMoreDragRef[0]));
                });
        eatMoreDragRef[0] = eatMoreDrag;

        activeEffectsDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultActiveEffects.naturalLocalHeight())),
                (target, bounds) -> {
                    if (mc.player == null) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultActiveEffects.id(), toRelativeState(bounds, defaultActiveEffects.id(), activeEffectsDragRef[0]));
                });
        activeEffectsDragRef[0] = activeEffectsDrag;
    }

    public static DietLayout.Layout resolvedPanelLayout(Minecraft mc) {
        return DietPanelLayoutResolver.resolvedPanelLayout(mc);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        DietLayout.Layout layout = resolvedPanelLayout(mc);

        if (button == 0 && DietScreen.isMouseOverEditModeToggle(layout, mouseX, mouseY)) {
            exitEditMode.run();
            return true;
        }

        if (lastCaloriesResolvedBounds != null && caloriesDrag.mouseClicked(mx, my, lastCaloriesResolvedBounds)) {
            return true;
        }
        if (lastBalanceResolvedBounds != null && balanceDrag.mouseClicked(mx, my, lastBalanceResolvedBounds)) {
            return true;
        }
        if (lastRecentResolvedBounds != null && recentMealsDrag.mouseClicked(mx, my, lastRecentResolvedBounds)) {
            return true;
        }
        if (lastEatMoreResolvedBounds != null && eatMoreDrag.mouseClicked(mx, my, lastEatMoreResolvedBounds)) {
            return true;
        }
        if (lastActiveEffectsResolvedBounds != null && activeEffectsDrag.mouseClicked(mx, my, lastActiveEffectsResolvedBounds)) {
            return true;
        }
        Bounds panelBounds = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        return panelDrag.mouseClicked(mx, my, panelBounds);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        boolean any = false;
        if (caloriesDrag.isDragging() || caloriesDrag.isResizing()) {
            caloriesDrag.mouseDragged(mx, my);
            any = true;
        }
        if (balanceDrag.isDragging() || balanceDrag.isResizing()) {
            balanceDrag.mouseDragged(mx, my);
            any = true;
        }
        if (recentMealsDrag.isDragging() || recentMealsDrag.isResizing()) {
            recentMealsDrag.mouseDragged(mx, my);
            any = true;
        }
        if (eatMoreDrag.isDragging() || eatMoreDrag.isResizing()) {
            eatMoreDrag.mouseDragged(mx, my);
            any = true;
        }
        if (activeEffectsDrag.isDragging() || activeEffectsDrag.isResizing()) {
            activeEffectsDrag.mouseDragged(mx, my);
            any = true;
        }
        if (panelDrag.isDragging() || panelDrag.isResizing()) {
            panelDrag.mouseDragged(mx, my);
            any = true;
        }
        return any;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean any = caloriesDrag.isDragging() || caloriesDrag.isResizing()
                || balanceDrag.isDragging() || balanceDrag.isResizing()
                || recentMealsDrag.isDragging() || recentMealsDrag.isResizing()
                || eatMoreDrag.isDragging() || eatMoreDrag.isResizing()
                || activeEffectsDrag.isDragging() || activeEffectsDrag.isResizing()
                || panelDrag.isDragging() || panelDrag.isResizing();
        int mx = (int) mouseX;
        int my = (int) mouseY;
        caloriesDrag.mouseReleased(mx, my);
        balanceDrag.mouseReleased(mx, my);
        recentMealsDrag.mouseReleased(mx, my);
        eatMoreDrag.mouseReleased(mx, my);
        activeEffectsDrag.mouseReleased(mx, my);
        panelDrag.mouseReleased(mx, my);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        DietLayout.Layout layout = resolvedPanelLayout(mc);
        panelDrag.setConstraint(DietPanelLayoutResolver.panelConstraint(layout));

        int[] mouse = scaledMouse(mc);
        int mx = mouse[0];
        int my = mouse[1];

        Bounds panelDefault = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        Bounds panelBounds = liveOrDefault(panelDrag, mx, my, panelDefault);

        TrackingData data = mc.player != null ? MarieClientCache.get() : null;
        List<String> bars = NourishedClientConfig.get().effectiveDietBarOrder();
        Map<String, Float> displayValues = data != null ? data.values : Map.of();

        DietLayout.Layout matchedPanelLayout = matchedLayoutFor(panelBounds);
        DietPanelContainer panel = new DietPanelContainer(data, bars, displayValues, matchedPanelLayout);
        CaloriesComponent calories = panel.caloriesComponent();
        BalanceComponent balance = panel.balanceComponent();
        RecentMealsComponent recent = panel.recentMealsComponent();
        EatMoreComponent eatMore = panel.eatMoreComponent();
        ActiveEffectsComponent activeEffects = panel.activeEffectsComponent();

        caloriesDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, calories.naturalLocalHeight(), SUMMARY_BOX_LOCAL_WIDTH)));
        balanceDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, balance.naturalLocalHeight(), SUMMARY_BOX_LOCAL_WIDTH)));
        recentMealsDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, recent.naturalLocalHeight())));
        eatMoreDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, eatMore.naturalLocalHeight())));
        activeEffectsDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, activeEffects.naturalLocalHeight())));

        Bounds caloriesR = calories.resolvedBounds();
        Bounds balanceR = balance.resolvedBounds();
        Bounds recentR = recent.resolvedBounds();
        Bounds eatMoreR = eatMore.resolvedBounds();
        Bounds activeEffectsR = activeEffects.resolvedBounds();
        caloriesDrag.setSnapTargets(xEdges(panelBounds, balanceR, recentR, eatMoreR, activeEffectsR), yEdges(panelBounds, balanceR, recentR, eatMoreR, activeEffectsR));
        balanceDrag.setSnapTargets(xEdges(panelBounds, caloriesR, recentR, eatMoreR, activeEffectsR), yEdges(panelBounds, caloriesR, recentR, eatMoreR, activeEffectsR));
        recentMealsDrag.setSnapTargets(xEdges(panelBounds, caloriesR, balanceR, eatMoreR, activeEffectsR), yEdges(panelBounds, caloriesR, balanceR, eatMoreR, activeEffectsR));
        eatMoreDrag.setSnapTargets(xEdges(panelBounds, caloriesR, balanceR, recentR, activeEffectsR), yEdges(panelBounds, caloriesR, balanceR, recentR, activeEffectsR));
        activeEffectsDrag.setSnapTargets(xEdges(panelBounds, caloriesR, balanceR, recentR, eatMoreR), yEdges(panelBounds, caloriesR, balanceR, recentR, eatMoreR));

        Bounds caloriesBounds = DietPanelLayoutResolver.clampToParent(liveOrDefault(caloriesDrag, mx, my, calories.resolvedBounds()), matchedPanelLayout);
        Bounds balanceBounds = DietPanelLayoutResolver.clampToParent(liveOrDefault(balanceDrag, mx, my, balance.resolvedBounds()), matchedPanelLayout);
        Bounds recentBounds = DietPanelLayoutResolver.clampToParent(liveOrDefault(recentMealsDrag, mx, my, recent.resolvedBounds()), matchedPanelLayout);
        Bounds eatMoreBounds = DietPanelLayoutResolver.clampToParent(liveOrDefault(eatMoreDrag, mx, my, eatMore.resolvedBounds()), matchedPanelLayout);
        Bounds activeEffectsBounds = DietPanelLayoutResolver.clampToParent(liveOrDefault(activeEffectsDrag, mx, my, activeEffects.resolvedBounds()), matchedPanelLayout);
        panel.setSubBoxRenderBounds(caloriesBounds, balanceBounds, recentBounds, eatMoreBounds, activeEffectsBounds);

        lastCaloriesResolvedBounds = calories.resolvedBounds();
        lastBalanceResolvedBounds = balance.resolvedBounds();
        lastRecentResolvedBounds = recent.resolvedBounds();
        lastEatMoreResolvedBounds = eatMore.resolvedBounds();
        lastActiveEffectsResolvedBounds = activeEffects.resolvedBounds();

        panel.render(context, panelBounds);

        drawHandle(context, panelDrag, panelBounds, mx, my, true);
        if (calories.isVisible()) {
            drawHandle(context, caloriesDrag, caloriesBounds, mx, my, false);
        }
        if (balance.isVisible()) {
            drawHandle(context, balanceDrag, balanceBounds, mx, my, false);
        }
        if (recent.isVisible()) {
            drawHandle(context, recentMealsDrag, recentBounds, mx, my, false);
        }
        if (eatMore.isVisible()) {
            drawHandle(context, eatMoreDrag, eatMoreBounds, mx, my, false);
        }
        if (activeEffects.isVisible()) {
            drawHandle(context, activeEffectsDrag, activeEffectsBounds, mx, my, false);
        }

        boolean toggleHovered = DietScreen.isMouseOverEditModeToggle(matchedPanelLayout, mx, my);
        DietScreen.drawEditModeToggle(context, matchedPanelLayout, true, toggleHovered);
    }

    private static void drawHandle(RenderContext context, DraggableResizable drag, Bounds bounds, int mx, int my, boolean withBottomLeftCorner) {
        Bounds handle = DraggableResizable.handleBounds(bounds);
        context.drawResizeHandle(handle.x(), handle.y(), drag.isHandleHovered(mx, my, bounds), drag.isCornerActive());
        if (withBottomLeftCorner) {
            Bounds handleBL = DraggableResizable.handleBoundsBottomLeft(bounds);
            context.drawResizeHandle(handleBL.x(), handleBL.y(), drag.isHandleBottomLeftHovered(mx, my, bounds), drag.isBottomLeftCornerActive());
        }
        for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
            Bounds strip = DraggableResizable.edgeHandleBounds(bounds, edge);
            context.drawEdgeHandle(strip.x(), strip.y(), strip.width(), strip.height(), mx, my,
                    drag.isEdgeHovered(mx, my, bounds, edge), drag.isEdgeActive(edge));
        }
    }

    private static List<Integer> xEdges(Bounds... boxes) {
        List<Integer> lines = new ArrayList<>(boxes.length * 2);
        for (Bounds b : boxes) {
            lines.add(b.x());
            lines.add(b.x() + b.width());
        }
        return lines;
    }

    private static List<Integer> yEdges(Bounds... boxes) {
        List<Integer> lines = new ArrayList<>(boxes.length * 2);
        for (Bounds b : boxes) {
            lines.add(b.y());
            lines.add(b.y() + b.height());
        }
        return lines;
    }

    private static Bounds liveOrDefault(DraggableResizable drag, int mx, int my, Bounds fallback) {
        if (drag.isDragging() || drag.isResizing()) {
            Bounds preview = drag.mouseDragged(mx, my);
            if (preview != null) {
                return preview;
            }
        }
        return fallback;
    }

    /** Live left-edge drag -> margin source; live right-edge/corner -> right column; otherwise -> persisted. */
    /**
     * Margin only ever reacts to a LEFT-edge (or bottom-left-corner) drag, as this frame's live
     * width delta added onto the persisted margin. A right-edge/corner drag never touches it — the
     * right column absorbs that delta instead, exactly like it always did. This is what keeps
     * grabbing the opposite handle from instantly snapping the other side: the margin simply isn't
     * read from "which handle is active" at all unless that handle is the one that owns it.
     */
    private DietLayout.Layout matchedLayoutFor(Bounds panelBounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        int leftMargin;
        if (panelDrag.isEdgeActive(DraggableResizable.Edge.LEFT) || panelDrag.isBottomLeftCornerActive()) {
            int widthBeforeGesture = resolvedPanelLayout(mc).panelW();
            leftMargin = Math.max(0, DietPanelLayoutResolver.persistedLeftMargin() + (panelBounds.width() - widthBeforeGesture));
        } else {
            leftMargin = DietPanelLayoutResolver.persistedLeftMargin();
        }
        return new DietLayout.Layout(
                panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(),
                panelBounds.x(), panelBounds.y(),
                cc.dietScale(), cc.recentMealsBoxScale(), cc.eatMoreBoxScale(),
                leftMargin
        );
    }

    /** Normalized against panelX + leftMargin, matching DietScreenPersistence#resolveRelativeToPanel's read-back base. */
    private ComponentState toRelativeState(Bounds bounds, String componentId, DraggableResizable drag) {
        DietLayout.Layout panelLayout = resolvedPanelLayout(mc);
        Bounds clamped = DietPanelLayoutResolver.clampToParent(bounds, panelLayout);
        double scale = panelLayout.scale();
        AutoGrowPanelContainer.ManualOverride existing = DietPanelLayoutResolver.existingManualOverride(componentId);
        AutoGrowPanelContainer.ManualOverride override = AutoGrowPanelContainer.withCommit(existing, drag);
        int contentX = panelLayout.panelX() + panelLayout.leftMargin();
        return new ComponentState(
                (int) Math.round((clamped.x() - contentX) / scale),
                (int) Math.round((clamped.y() - panelLayout.panelY()) / scale),
                (int) Math.round(clamped.width() / scale),
                (int) Math.round(clamped.height() / scale),
                false, override.widthManual(), override.heightManual(), 0
        );
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
