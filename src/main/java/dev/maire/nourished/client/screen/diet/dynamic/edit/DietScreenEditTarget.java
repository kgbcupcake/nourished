package dev.maire.nourished.client.screen.diet.dynamic.edit;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.scaleconfig.ScaleConfigPanel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

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
    /**
     * Same {@link ScaleConfigPanel} instance and live visibility state {@link DietScreen} owns —
     * not a second panel — so the sliders shown here while edit mode has swapped {@code mc.screen}
     * to {@link dev.marie.framework.ui.edit.EditOverlayScreen} stay in sync with the persisted
     * state the player is actually editing, and with what {@link DietScreen#render} shows again
     * once edit mode exits.
     */
    private final ScaleConfigPanel scaleConfigPanel;
    private final BooleanSupplier scaleConfigVisible;
    private final DraggableResizable panelDrag;
    private final DraggableResizable caloriesDrag;
    private final DraggableResizable balanceDrag;
    private final DraggableResizable recentMealsDrag;
    private final DraggableResizable eatMoreDrag;
    private final DraggableResizable activeEffectsDrag;
    private final ContentScaleController scaleController = new ContentScaleController(DietScreenPersistence.get());

    /** scroll-step change applied to a box's active text-scale/padding adjustment per notch — see {@link #mouseScrolled}. */
    private static final double SCROLL_STEP = 0.05d;

    /** Every sub-box component id the scale controller can be double-clicked into, in no particular order. */
    private static final String[] SUB_BOX_IDS = {
            CaloriesComponent.ID, BalanceComponent.ID, RecentMealsComponent.ID, EatMoreComponent.ID, ActiveEffectsComponent.ID
    };

    private Bounds lastCaloriesResolvedBounds;
    private Bounds lastBalanceResolvedBounds;
    private Bounds lastRecentResolvedBounds;
    private Bounds lastEatMoreResolvedBounds;
    private Bounds lastActiveEffectsResolvedBounds;

    public DietScreenEditTarget(Minecraft mc, Runnable exitEditMode, ScaleConfigPanel scaleConfigPanel, BooleanSupplier scaleConfigVisible) {
        this.mc = mc;
        this.exitEditMode = exitEditMode;
        this.scaleConfigPanel = scaleConfigPanel;
        this.scaleConfigVisible = scaleConfigVisible;

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
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        DietLayout.Layout layout = resolvedPanelLayout(mc);

        if (button == 0 && DietScreen.isMouseOverEditModeToggle(layout, mouseX, mouseY)) {
            exitEditMode.run();
            return true;
        }

        if (lastCaloriesResolvedBounds != null) {
            if (scaleController.onClick(CaloriesComponent.ID, button, mx, my, lastCaloriesResolvedBounds)) {
                return true;
            }
            if (caloriesDrag.mouseClicked(mx, my, lastCaloriesResolvedBounds)) {
                return true;
            }
        }
        if (lastBalanceResolvedBounds != null) {
            if (scaleController.onClick(BalanceComponent.ID, button, mx, my, lastBalanceResolvedBounds)) {
                return true;
            }
            if (balanceDrag.mouseClicked(mx, my, lastBalanceResolvedBounds)) {
                return true;
            }
        }
        if (lastRecentResolvedBounds != null) {
            if (scaleController.onClick(RecentMealsComponent.ID, button, mx, my, lastRecentResolvedBounds)) {
                return true;
            }
            if (recentMealsDrag.mouseClicked(mx, my, lastRecentResolvedBounds)) {
                return true;
            }
        }
        if (lastEatMoreResolvedBounds != null) {
            if (scaleController.onClick(EatMoreComponent.ID, button, mx, my, lastEatMoreResolvedBounds)) {
                return true;
            }
            if (eatMoreDrag.mouseClicked(mx, my, lastEatMoreResolvedBounds)) {
                return true;
            }
        }
        if (lastActiveEffectsResolvedBounds != null) {
            if (scaleController.onClick(ActiveEffectsComponent.ID, button, mx, my, lastActiveEffectsResolvedBounds)) {
                return true;
            }
            if (activeEffectsDrag.mouseClicked(mx, my, lastActiveEffectsResolvedBounds)) {
                return true;
            }
        }
        Bounds panelBounds = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        return panelDrag.mouseClicked(mx, my, panelBounds);
    }

    /**
     * Feeds a scroll notch to whichever sub-box currently has an active {@link ContentScaleController}
     * mode (double-left-click for text scale, double-right-click for padding — see {@link
     * ContentScaleController}'s own javadoc). Deliberately does not delegate to {@link
     * ContentScaleController#handleScroll} directly: that method's own fallback for a never-persisted
     * box defaults x/y to 0, which would silently relocate it — {@link
     * DietScreenPersistence#adjustScale} derives a safe fallback from the box's live bounds instead,
     * same as the old DietZoomController did.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (scrollY == 0) {
            return false;
        }
        Map<String, Bounds> bounds = new HashMap<>();
        if (lastCaloriesResolvedBounds != null) bounds.put(CaloriesComponent.ID, lastCaloriesResolvedBounds);
        if (lastBalanceResolvedBounds != null) bounds.put(BalanceComponent.ID, lastBalanceResolvedBounds);
        if (lastRecentResolvedBounds != null) bounds.put(RecentMealsComponent.ID, lastRecentResolvedBounds);
        if (lastEatMoreResolvedBounds != null) bounds.put(EatMoreComponent.ID, lastEatMoreResolvedBounds);
        if (lastActiveEffectsResolvedBounds != null) bounds.put(ActiveEffectsComponent.ID, lastActiveEffectsResolvedBounds);

        double delta = scrollY > 0 ? SCROLL_STEP : -SCROLL_STEP;
        for (String id : SUB_BOX_IDS) {
            ContentScaleController.Mode mode = scaleController.activeMode(id);
            Bounds box = bounds.get(id);
            if (mode == ContentScaleController.Mode.NONE || box == null) {
                continue;
            }
            DietScreenPersistence.adjustScale(id, mode, resolvedPanelLayout(mc), box, delta);
            return true;
        }
        return false;
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
        // The left-edge/corner floor otherwise bakes in the *current* leftMargin, which grows in
        // lockstep with a left-edge drag — locking the panel at whatever width just created that
        // margin. Zeroing the margin here lets the same gesture shrink it back down again.
        boolean shrinkingLeftEdge = panelDrag.isEdgeActive(DraggableResizable.Edge.LEFT) || panelDrag.isBottomLeftCornerActive();
        DietLayout.Layout constraintLayout = shrinkingLeftEdge
                ? new DietLayout.Layout(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(),
                        layout.baseX(), layout.baseY(), layout.scale(), layout.recentMealsScale(), layout.eatMoreScale(), 0)
                : layout;
        panelDrag.setConstraint(DietPanelLayoutResolver.panelConstraint(constraintLayout));

        int[] mouse = scaledMouse(mc);
        int mx = mouse[0];
        int my = mouse[1];

        Bounds panelDefault = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        Bounds panelBounds = liveOrDefault(panelDrag, mx, my, panelDefault);

        TrackingData data = mc.player != null ? MarieClientCache.get() : null;
        List<String> bars = NourishedClientConfig.get().effectiveDietBarOrder();
        Map<String, Float> displayValues = data != null ? data.values : Map.of();

        DietLayout.Layout matchedPanelLayout = matchedLayoutFor(panelBounds);
        applyLiveSubBoxOverrides(mx, my);
        DietPanelContainer panel;
        try {
            panel = new DietPanelContainer(data, bars, displayValues, matchedPanelLayout);
        } finally {
            DietScreenPersistence.clearLiveOverrides();
        }
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
        drawSizeLabel(context, panelDrag, panelBounds);
        if (calories.isVisible()) {
            drawHandle(context, caloriesDrag, caloriesBounds, mx, my, false);
            drawSizeLabel(context, caloriesDrag, caloriesBounds);
            drawScaleOverlay(context, CaloriesComponent.ID, caloriesBounds);
        }
        if (balance.isVisible()) {
            drawHandle(context, balanceDrag, balanceBounds, mx, my, false);
            drawSizeLabel(context, balanceDrag, balanceBounds);
            drawScaleOverlay(context, BalanceComponent.ID, balanceBounds);
        }
        if (recent.isVisible()) {
            drawHandle(context, recentMealsDrag, recentBounds, mx, my, false);
            drawSizeLabel(context, recentMealsDrag, recentBounds);
            drawScaleOverlay(context, RecentMealsComponent.ID, recentBounds);
        }
        if (eatMore.isVisible()) {
            drawHandle(context, eatMoreDrag, eatMoreBounds, mx, my, false);
            drawSizeLabel(context, eatMoreDrag, eatMoreBounds);
            drawScaleOverlay(context, EatMoreComponent.ID, eatMoreBounds);
        }
        if (activeEffects.isVisible()) {
            drawHandle(context, activeEffectsDrag, activeEffectsBounds, mx, my, false);
            drawSizeLabel(context, activeEffectsDrag, activeEffectsBounds);
            drawScaleOverlay(context, ActiveEffectsComponent.ID, activeEffectsBounds);
        }

        boolean toggleHovered = DietScreen.isMouseOverEditModeToggle(matchedPanelLayout, mx, my);
        DietScreen.drawEditModeToggle(context, matchedPanelLayout, true, toggleHovered);

        if (scaleConfigVisible.getAsBoolean()) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
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

    /**
     * Debug aid: while {@code drag} is actively being dragged or resized, shows {@code bounds}'
     * live pixel size next to whichever handle/edge is driving the gesture, to correlate box size
     * with things like {@link ActiveEffectsComponent}'s visibility threshold.
     */
    private static void drawSizeLabel(RenderContext context, DraggableResizable drag, Bounds bounds) {
        if (!(drag.isDragging() || drag.isResizing())) {
            return;
        }
        String label = bounds.width() + " x " + bounds.height();
        int lx;
        int ly;
        if (drag.isCornerActive()) {
            Bounds handle = DraggableResizable.handleBounds(bounds);
            lx = handle.x() + DraggableResizable.RESIZE_HANDLE_SIZE + 4;
            ly = handle.y() + DraggableResizable.RESIZE_HANDLE_SIZE + 4;
        } else if (drag.isBottomLeftCornerActive()) {
            Bounds handle = DraggableResizable.handleBoundsBottomLeft(bounds);
            lx = handle.x();
            ly = handle.y() + DraggableResizable.RESIZE_HANDLE_SIZE + 4;
        } else {
            DraggableResizable.Edge activeEdge = null;
            for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
                if (drag.isEdgeActive(edge)) {
                    activeEdge = edge;
                    break;
                }
            }
            if (activeEdge != null) {
                switch (activeEdge) {
                    case LEFT -> {
                        lx = bounds.x() - 40;
                        ly = bounds.y() + bounds.height() / 2 - 4;
                    }
                    case RIGHT -> {
                        lx = bounds.x() + bounds.width() + 6;
                        ly = bounds.y() + bounds.height() / 2 - 4;
                    }
                    case TOP -> {
                        lx = bounds.x() + bounds.width() / 2 - 20;
                        ly = bounds.y() - 12;
                    }
                    default -> {
                        lx = bounds.x() + bounds.width() / 2 - 20;
                        ly = bounds.y() + bounds.height() + 6;
                    }
                }
            } else {
                // Plain reposition drag — no handle is active, so anchor near the box's top-left.
                lx = bounds.x() + 4;
                ly = bounds.y() - 12;
            }
        }
        drawShadowedText(context, label, lx, ly);
    }

    /** While {@code componentId} has an active {@link ContentScaleController} mode, shows its current text-scale or padding percentage below the box so scroll-wheel adjustments are visible — same look as {@code CalorieHudScreen#drawScaleOverlay}. */
    private void drawScaleOverlay(RenderContext context, String componentId, Bounds bounds) {
        ContentScaleController.Mode mode = scaleController.activeMode(componentId);
        String label = switch (mode) {
            case TEXT_SCALE -> "TEXT SCALE " + Math.round(DietScreenPersistence.contentScale(componentId) * 100) + "%";
            case PADDING -> "PADDING " + Math.round(DietScreenPersistence.paddingScale(componentId) * 100) + "%";
            case NONE -> null;
        };
        if (label == null) {
            return;
        }
        drawShadowedText(context, label, bounds.x() + 4, bounds.y() + bounds.height() + 6);
    }

    /** Light text over a dark 1px drop-shadow, for legibility against any background. */
    private static void drawShadowedText(RenderContext context, String text, int x, int y) {
        context.drawText(text, x + 1, y + 1, 0xFF000000, 0.75f);
        context.drawText(text, x, y, 0xFFFFFFFF, 0.75f);
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

    /**
     * Registers this frame's live drag/resize preview (if any) for each sub-box in {@link
     * DietScreenPersistence}'s override map, so the module chain built by the {@code
     * DietPanelContainer} constructed right after this call sees the box actually being dragged at
     * its live size/position instead of its last-committed one — see {@link
     * DietScreenPersistence#setLiveOverride}. Caller must pair this with {@link
     * DietScreenPersistence#clearLiveOverrides()} once that construction is done so the override
     * doesn't leak into the normal (non-edit-mode) render path.
     */
    private void applyLiveSubBoxOverrides(int mx, int my) {
        applyLiveOverride(caloriesDrag, CaloriesComponent.ID, mx, my);
        applyLiveOverride(balanceDrag, BalanceComponent.ID, mx, my);
        applyLiveOverride(recentMealsDrag, RecentMealsComponent.ID, mx, my);
        applyLiveOverride(eatMoreDrag, EatMoreComponent.ID, mx, my);
        applyLiveOverride(activeEffectsDrag, ActiveEffectsComponent.ID, mx, my);
    }

    private static void applyLiveOverride(DraggableResizable drag, String componentId, int mx, int my) {
        if (!drag.isDragging() && !drag.isResizing()) {
            return;
        }
        Bounds preview = drag.mouseDragged(mx, my);
        if (preview != null) {
            DietScreenPersistence.setLiveOverride(componentId, preview);
        }
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

    /**
     * Normalized against panelX + leftMargin, matching DietScreenPersistence#resolveRelativeToPanel's
     * read-back base. Read-modify-write against the box's existing persisted {@link ComponentState}
     * (falling back to record defaults only if nothing is persisted yet) rather than constructing a
     * fresh one — only x/y/width/height/collapsed/widthManual/heightManual come from this drag/resize;
     * every other field (e.g. {@code contentScale}, and anything the record gains later) is copied
     * through from the loaded state so a drag/resize commit can't silently wipe it out.
     */
    private ComponentState toRelativeState(Bounds bounds, String componentId, DraggableResizable drag) {
        DietLayout.Layout panelLayout = resolvedPanelLayout(mc);
        Bounds clamped = DietPanelLayoutResolver.clampToParent(bounds, panelLayout);
        double scale = panelLayout.scale();
        AutoGrowPanelContainer.ManualOverride existing = DietPanelLayoutResolver.existingManualOverride(componentId);
        AutoGrowPanelContainer.ManualOverride override = AutoGrowPanelContainer.withCommit(existing, drag);
        int contentX = panelLayout.panelX() + panelLayout.leftMargin();
        ComponentState base = DietScreenPersistence.get().load(componentId)
                .orElseGet(() -> new ComponentState(0, 0, 0, 0, false, false, false, 0));
        return new ComponentState(
                (int) Math.round((clamped.x() - contentX) / scale),
                (int) Math.round((clamped.y() - panelLayout.panelY()) / scale),
                (int) Math.round(clamped.width() / scale),
                (int) Math.round(clamped.height() / scale),
                false, override.widthManual(), override.heightManual(), base.leftMargin(), base.contentScale(), base.paddingScale()
        );
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
