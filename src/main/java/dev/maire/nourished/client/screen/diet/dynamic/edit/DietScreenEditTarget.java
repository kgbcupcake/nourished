package dev.maire.nourished.client.screen.diet.dynamic.edit;

import dev.marie.framework.color.MarieColors;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
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

    /** Grab state for a sub-box "Move Text"/"Move Icons"/"Move All" drag, and which box it is repositioning content in. */
    private final MarieModuleSettings.MoveDrag moveDrag = new MarieModuleSettings.MoveDrag();
    private String movingBoxId;
    private Bounds movingBoxBounds;
    /** Per box being resized from its left/top edge: which axes, the box's start x/y, and its four content offsets at the press. */
    private final java.util.Map<String, int[]> contentAnchors = new java.util.HashMap<>();

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

        if (button == 0 && startMoveDrag(mouseX, mouseY)) {
            return true;
        }

        if (lastCaloriesResolvedBounds != null && clickBox(CaloriesComponent.ID, caloriesDrag, lastCaloriesResolvedBounds, mx, my)) {
            return true;
        }
        if (lastBalanceResolvedBounds != null && clickBox(BalanceComponent.ID, balanceDrag, lastBalanceResolvedBounds, mx, my)) {
            return true;
        }
        if (lastRecentResolvedBounds != null && clickBox(RecentMealsComponent.ID, recentMealsDrag, lastRecentResolvedBounds, mx, my)) {
            return true;
        }
        if (lastEatMoreResolvedBounds != null && clickBox(EatMoreComponent.ID, eatMoreDrag, lastEatMoreResolvedBounds, mx, my)) {
            return true;
        }
        if (lastActiveEffectsResolvedBounds != null && clickBox(ActiveEffectsComponent.ID, activeEffectsDrag, lastActiveEffectsResolvedBounds, mx, my)) {
            return true;
        }
        Bounds panelBounds = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        return panelDrag.mouseClicked(mx, my, panelBounds);
    }

    /** Starts a box's drag/resize gesture, remembering (for a left/top-edge resize) where its content started so it can stay put on screen. */
    private boolean clickBox(String id, DraggableResizable drag, Bounds bounds, int mx, int my) {
        if (!drag.mouseClicked(mx, my, bounds)) {
            return false;
        }
        boolean left = drag.isEdgeActive(DraggableResizable.Edge.LEFT) || drag.isBottomLeftCornerActive();
        boolean top = drag.isEdgeActive(DraggableResizable.Edge.TOP);
        if (drag.isResizing() && (left || top)) {
            var st = DietScreenPersistence.get();
            contentAnchors.put(id, new int[]{left ? 1 : 0, top ? 1 : 0, bounds.x(), bounds.y(),
                    MarieModuleSettings.textOffsetX(st, id), MarieModuleSettings.textOffsetY(st, id),
                    MarieModuleSettings.iconOffsetX(st, id), MarieModuleSettings.iconOffsetY(st, id),
                    MarieModuleSettings.barOffsetX(st, id), MarieModuleSettings.barOffsetY(st, id),
                    MarieModuleSettings.headerOffsetX(st, id), MarieModuleSettings.headerOffsetY(st, id)});
        }
        return true;
    }

    /** While a left/top edge is dragged, shifts the box's content offsets by the edge's movement so the content stays where it was on screen. */
    private void keepContentInPlace(String id, Bounds live) {
        int[] a = contentAnchors.get(id);
        if (a == null) {
            return;
        }
        int dx = a[0] == 1 ? a[2] - live.x() : 0;
        int dy = a[1] == 1 ? a[3] - live.y() : 0;
        var st = DietScreenPersistence.get();
        MarieModuleSettings.setTextOffset(st, id, a[4] + dx, a[5] + dy);
        MarieModuleSettings.setIconOffset(st, id, a[6] + dx, a[7] + dy);
        MarieModuleSettings.setBarOffset(st, id, a[8] + dx, a[9] + dy);
        MarieModuleSettings.setHeaderOffset(st, id, a[10] + dx, a[11] + dy);
    }

    private void commitContentAnchors() {
        var st = DietScreenPersistence.get();
        for (String id : contentAnchors.keySet()) {
            MarieModuleSettings.commitTextOffset(st, id);
            MarieModuleSettings.commitIconOffset(st, id);
            MarieModuleSettings.commitBarOffset(st, id);
            MarieModuleSettings.commitHeaderOffset(st, id);
        }
        contentAnchors.clear();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (moveDrag.isActive()) {
            applyMoveDrag(mouseX, mouseY);
            return true;
        }
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
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (moveDrag.isActive()) {
            finishMoveDrag();
            return true;
        }
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
        commitContentAnchors();
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
        panelDrag.setConstraint(shrinkingLeftEdge
                ? DietPanelLayoutResolver.leftEdgeConstraint(constraintLayout)
                : DietPanelLayoutResolver.panelConstraint(constraintLayout));

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
        keepContentInPlace(CaloriesComponent.ID, caloriesBounds);
        keepContentInPlace(BalanceComponent.ID, balanceBounds);
        keepContentInPlace(RecentMealsComponent.ID, recentBounds);
        keepContentInPlace(EatMoreComponent.ID, eatMoreBounds);
        keepContentInPlace(ActiveEffectsComponent.ID, activeEffectsBounds);
        panel.setSubBoxRenderBounds(caloriesBounds, balanceBounds, recentBounds, eatMoreBounds, activeEffectsBounds);

        lastCaloriesResolvedBounds = calories.resolvedBounds();
        lastBalanceResolvedBounds = balance.resolvedBounds();
        lastRecentResolvedBounds = recent.resolvedBounds();
        lastEatMoreResolvedBounds = eatMore.resolvedBounds();
        lastActiveEffectsResolvedBounds = activeEffects.resolvedBounds();

        panel.render(MarieModuleSettings.withBrightness(context, NourishedClientConfig.get().dietTextBrightness(), NourishedClientConfig.get().dietIconBrightness()), panelBounds);

        drawHandle(context, panelDrag, panelBounds, mx, my, true);
        drawSizeLabel(context, panelDrag, panelBounds);
        if (calories.isVisible()) {
            drawBoxHandles(context, CaloriesComponent.ID, caloriesDrag, caloriesBounds, mx, my);
            drawSizeLabel(context, caloriesDrag, caloriesBounds);
        }
        if (balance.isVisible()) {
            drawBoxHandles(context, BalanceComponent.ID, balanceDrag, balanceBounds, mx, my);
            drawSizeLabel(context, balanceDrag, balanceBounds);
        }
        if (recent.isVisible()) {
            drawBoxHandles(context, RecentMealsComponent.ID, recentMealsDrag, recentBounds, mx, my);
            drawSizeLabel(context, recentMealsDrag, recentBounds);
        }
        if (eatMore.isVisible()) {
            drawBoxHandles(context, EatMoreComponent.ID, eatMoreDrag, eatMoreBounds, mx, my);
            drawSizeLabel(context, eatMoreDrag, eatMoreBounds);
        }
        if (activeEffects.isVisible()) {
            drawBoxHandles(context, ActiveEffectsComponent.ID, activeEffectsDrag, activeEffectsBounds, mx, my);
            drawSizeLabel(context, activeEffectsDrag, activeEffectsBounds);
        }

        boolean toggleHovered = DietScreen.isMouseOverEditModeToggle(matchedPanelLayout, mx, my);
        DietScreen.drawEditModeToggle(context, matchedPanelLayout, true, toggleHovered);

        if (scaleConfigVisible.getAsBoolean()) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
    }

    /** Starts a move drag in whichever sub-box the pointer is over, if that box has a move mode switched on. */
    private boolean startMoveDrag(double mouseX, double mouseY) {
        String[] ids = {CaloriesComponent.ID, BalanceComponent.ID, RecentMealsComponent.ID, EatMoreComponent.ID, ActiveEffectsComponent.ID};
        Bounds[] boxes = {lastCaloriesResolvedBounds, lastBalanceResolvedBounds, lastRecentResolvedBounds,
                lastEatMoreResolvedBounds, lastActiveEffectsResolvedBounds};
        for (int i = 0; i < ids.length; i++) {
            if (boxes[i] == null || !boxes[i].contains((int) mouseX, (int) mouseY)) {
                continue;
            }
            MarieModuleSettings.MoveDrag.Mode mode = MarieModuleSettings.activeMoveMode(DietScreenPersistence.get(), ids[i]);
            if (mode == null) {
                continue;
            }
            movingBoxId = ids[i];
            movingBoxBounds = boxes[i];
            switch (mode) {
                case TEXT -> moveDrag.start(mode, mouseX, mouseY,
                        MarieModuleSettings.textOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.textOffsetY(DietScreenPersistence.get(), ids[i]));
                case ICONS -> moveDrag.start(mode, mouseX, mouseY,
                        MarieModuleSettings.iconOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.iconOffsetY(DietScreenPersistence.get(), ids[i]));
                case BARS -> moveDrag.start(mode, mouseX, mouseY,
                        MarieModuleSettings.barOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.barOffsetY(DietScreenPersistence.get(), ids[i]));
                case HEADER -> moveDrag.start(mode, mouseX, mouseY,
                        MarieModuleSettings.headerOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.headerOffsetY(DietScreenPersistence.get(), ids[i]));
                default -> moveDrag.startAll(mouseX, mouseY,
                        MarieModuleSettings.textOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.textOffsetY(DietScreenPersistence.get(), ids[i]),
                        MarieModuleSettings.iconOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.iconOffsetY(DietScreenPersistence.get(), ids[i]),
                        MarieModuleSettings.barOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.barOffsetY(DietScreenPersistence.get(), ids[i]),
                        MarieModuleSettings.headerOffsetX(DietScreenPersistence.get(), ids[i]), MarieModuleSettings.headerOffsetY(DietScreenPersistence.get(), ids[i]));
            }
            return true;
        }
        return false;
    }

    /** Applies the active move drag, keeping each offset within the box's own size. */
    private void applyMoveDrag(double mouseX, double mouseY) {
        int maxX = movingBoxBounds.width();
        int maxY = movingBoxBounds.height();
        switch (moveDrag.mode()) {
            case TEXT -> MarieModuleSettings.setTextOffset(DietScreenPersistence.get(), movingBoxId,
                    clamp(moveDrag.offsetX(mouseX), maxX), clamp(moveDrag.offsetY(mouseY), maxY));
            case ICONS -> MarieModuleSettings.setIconOffset(DietScreenPersistence.get(), movingBoxId,
                    clamp(moveDrag.offsetX(mouseX), maxX), clamp(moveDrag.offsetY(mouseY), maxY));
            case BARS -> MarieModuleSettings.setBarOffset(DietScreenPersistence.get(), movingBoxId,
                    clamp(moveDrag.offsetX(mouseX), maxX), clamp(moveDrag.offsetY(mouseY), maxY));
            case HEADER -> MarieModuleSettings.setHeaderOffset(DietScreenPersistence.get(), movingBoxId,
                    clamp(moveDrag.offsetX(mouseX), maxX), clamp(moveDrag.offsetY(mouseY), maxY));
            default -> {
                // Move All: text and icons shift together by the pointer's movement since the press.
                int dx = moveDrag.offsetX(mouseX);
                int dy = moveDrag.offsetY(mouseY);
                MarieModuleSettings.setTextOffset(DietScreenPersistence.get(), movingBoxId,
                        clamp(moveDrag.baseX(MarieModuleSettings.MoveDrag.Mode.TEXT) + dx, maxX),
                        clamp(moveDrag.baseY(MarieModuleSettings.MoveDrag.Mode.TEXT) + dy, maxY));
                MarieModuleSettings.setIconOffset(DietScreenPersistence.get(), movingBoxId,
                        clamp(moveDrag.baseX(MarieModuleSettings.MoveDrag.Mode.ICONS) + dx, maxX),
                        clamp(moveDrag.baseY(MarieModuleSettings.MoveDrag.Mode.ICONS) + dy, maxY));
                MarieModuleSettings.setBarOffset(DietScreenPersistence.get(), movingBoxId,
                        clamp(moveDrag.baseX(MarieModuleSettings.MoveDrag.Mode.BARS) + dx, maxX),
                        clamp(moveDrag.baseY(MarieModuleSettings.MoveDrag.Mode.BARS) + dy, maxY));
                MarieModuleSettings.setHeaderOffset(DietScreenPersistence.get(), movingBoxId,
                        clamp(moveDrag.baseX(MarieModuleSettings.MoveDrag.Mode.HEADER) + dx, maxX),
                        clamp(moveDrag.baseY(MarieModuleSettings.MoveDrag.Mode.HEADER) + dy, maxY));
            }
        }
    }

    private void finishMoveDrag() {
        MarieModuleSettings.MoveDrag.Mode mode = moveDrag.mode();
        moveDrag.stop();
        boolean all = mode == MarieModuleSettings.MoveDrag.Mode.ALL;
        if (all || mode == MarieModuleSettings.MoveDrag.Mode.TEXT) {
            MarieModuleSettings.commitTextOffset(DietScreenPersistence.get(), movingBoxId);
        }
        if (all || mode == MarieModuleSettings.MoveDrag.Mode.ICONS) {
            MarieModuleSettings.commitIconOffset(DietScreenPersistence.get(), movingBoxId);
        }
        if (all || mode == MarieModuleSettings.MoveDrag.Mode.BARS) {
            MarieModuleSettings.commitBarOffset(DietScreenPersistence.get(), movingBoxId);
        }
        if (all || mode == MarieModuleSettings.MoveDrag.Mode.HEADER) {
            MarieModuleSettings.commitHeaderOffset(DietScreenPersistence.get(), movingBoxId);
        }
        movingBoxId = null;
        movingBoxBounds = null;
    }

    private static int clamp(int value, int limit) {
        return Math.max(-limit, Math.min(limit, value));
    }

    /** A sub-box's edit handles, or — while it has a move mode on — a dashed outline instead (dragging inside then moves its content, not the box). */
    private static void drawBoxHandles(RenderContext context, String boxId, DraggableResizable drag, Bounds bounds, int mx, int my) {
        if (MarieModuleSettings.activeMoveMode(DietScreenPersistence.get(), boxId) != null) {
            context.drawDashedBorder(bounds.x() + 2, bounds.y() + 2, bounds.width() - 4, bounds.height() - 4, MarieColors.resolveColor(NourishedColors.EDIT_OUTLINE));
        } else {
            drawHandle(context, drag, bounds, mx, my, true);
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

    /** Light text over a dark 1px drop-shadow, for legibility against any background. */
    private static void drawShadowedText(RenderContext context, String text, int x, int y) {
        context.drawText(text, x + 1, y + 1, MarieColors.resolveColor(NourishedColors.EDIT_LABEL_SHADOW), 0.75f);
        context.drawText(text, x, y, MarieColors.resolveColor(NourishedColors.EDIT_LABEL_TEXT), 0.75f);
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
