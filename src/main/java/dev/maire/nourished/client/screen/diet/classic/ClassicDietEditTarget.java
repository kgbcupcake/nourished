package dev.maire.nourished.client.screen.diet.classic;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Interaction-only target for Classic Diet Screen edit mode — owns drag/resize state and
 * persistence callbacks for the panel, Recent Meals box, and Eat More box (the historical
 * {@code DietScreenEditController}'s three independently resizable regions), reusing the shared
 * marie-ui {@link DraggableResizable} tracker instead of porting the old hand-rolled
 * grab-offset/diagonal-ratio math. Deliberately does not draw any Diet Screen content itself —
 * {@link ClassicDietScreen#renderForEdit} owns all panel rendering; this class only draws the
 * generic resize-handle affordance via {@link RenderContext}.
 *
 * <p>Each region's on-screen size is driven by a single scalar ({@code dietScale}/
 * {@code recentMealsBoxScale}/{@code eatMoreBoxScale}), not independent width/height — matching
 * {@link ClassicDietScreen}'s rendering, which only understands one scale per region. The box
 * regions' width never changes historically (only height does), so each box {@link Constraint}
 * pins min/max/preferred width to the same value — {@link DraggableResizable}'s corner-resize
 * then only ever moves height in practice.
 */
final class ClassicDietEditTarget implements MarieComponent {

    private static final String ID = "nourished.diet.classic.editwrapper";

    /** Matches the historical {@code DietScreenEditController}'s clamp range for all three regions. */
    private static final double MIN_SCALE = 0.5d;
    private static final double MAX_SCALE = 1.5d;

    private final Minecraft mc;
    private final DraggableResizable panelDrag;
    private final DraggableResizable recentMealsDrag;
    private final DraggableResizable eatMoreDrag;
    private final int baselinePanelW;

    private Bounds lastPanelBounds;
    private Bounds lastRecentMealsBounds;
    private Bounds lastEatMoreBounds;

    ClassicDietEditTarget(Minecraft mc) {
        this.mc = mc;

        DietLayout.Layout base1x = DietLayout.compute(mc, 1.0d, 0, 0, 1.0d, 1.0d);
        this.baselinePanelW = base1x.panelW();

        NourishedClientConfig cc = NourishedClientConfig.get();
        DietLayout.Layout persistedPanelOnly = DietLayout.compute(mc, cc.dietScale(), cc.dietOffsetX(), cc.dietOffsetY(), 1.0d, 1.0d);

        panelDrag = new DraggableResizable(this, panelConstraintFor(cc.dietScale()), (target, bounds) -> commitPanel(bounds));

        DietLayout.Layout panelForBoxes = new DietLayout.Layout(
                persistedPanelOnly.panelX(), persistedPanelOnly.panelY(), persistedPanelOnly.panelW(), persistedPanelOnly.panelH(),
                persistedPanelOnly.baseX(), persistedPanelOnly.baseY(),
                cc.dietScale(), cc.recentMealsBoxScale(), cc.eatMoreBoxScale(),
                0
        );
        int recentBaseline = recentMealsBaselineHeight(panelForBoxes);
        recentMealsDrag = new DraggableResizable(this, boxConstraintFor(panelForBoxes, recentBaseline, cc.recentMealsBoxScale(), MAX_SCALE),
                (target, bounds) -> commitRecentMeals(bounds));

        int eatMoreBaseline = eatMoreBaselineHeight(panelForBoxes);
        eatMoreDrag = new DraggableResizable(this, boxConstraintFor(panelForBoxes, eatMoreBaseline, cc.eatMoreBoxScale(), maxSafeEatMoreScale(panelForBoxes)),
                (target, bounds) -> commitEatMore(bounds));
    }

    private Constraint panelConstraintFor(double scale) {
        DietLayout.Layout min = DietLayout.compute(mc, MIN_SCALE, 0, 0, 1.0d, 1.0d);
        DietLayout.Layout max = DietLayout.compute(mc, MAX_SCALE, 0, 0, 1.0d, 1.0d);
        DietLayout.Layout pref = DietLayout.compute(mc, scale, 0, 0, 1.0d, 1.0d);
        return new Constraint(
                new Size(pref.panelW(), pref.panelH()),
                new Size(min.panelW(), min.panelH()),
                new Size(max.panelW(), max.panelH()),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
    }

    /**
     * Box width is fixed (pinned min==max==preferred) — only height responds to the corner drag.
     * {@code maxScale} bounds the upper end independently of {@link #MAX_SCALE} — the Eat More box
     * passes {@link #maxSafeEatMoreScale} here so growing it can never squeeze Active Effects out
     * of its draw budget; Recent Meals passes {@link #MAX_SCALE} unchanged.
     */
    private Constraint boxConstraintFor(DietLayout.Layout panelLayout, int baselineHeightAt1x, double currentScale, double maxScale) {
        int boxW = DietLayout.toScreenDim(panelLayout, DietLayout.SPLIT - DietLayout.PAD * 2 + 4);
        int minH = Math.max(1, (int) Math.round(baselineHeightAt1x * MIN_SCALE));
        int maxH = Math.max(minH, (int) Math.round(baselineHeightAt1x * maxScale));
        int prefH = Math.max(1, (int) Math.round(baselineHeightAt1x * Math.min(currentScale, maxScale)));
        return new Constraint(
                new Size(boxW, prefH),
                new Size(boxW, minH),
                new Size(boxW, maxH),
                false, false, false, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
    }

    /** Recent Meals box screen height at {@code recentMealsScale == 1.0}, panel scale/eatMoreScale held from {@code panelLayout}. */
    private int recentMealsBaselineHeight(DietLayout.Layout panelLayout) {
        DietLayout.Layout probe = DietLayout.compute(mc, panelLayout.scale(), 0, 0, 1.0d, panelLayout.eatMoreScale());
        ScreenBox box = recentMealsBox(probe);
        return box != null ? box.screenH() : 1;
    }

    /** Eat More box screen height at {@code eatMoreScale == 1.0}, panel scale/recentMealsScale held from {@code panelLayout}. */
    private int eatMoreBaselineHeight(DietLayout.Layout panelLayout) {
        DietLayout.Layout probe = DietLayout.compute(mc, panelLayout.scale(), 0, 0, panelLayout.recentMealsScale(), 1.0d);
        ScreenBox box = eatMoreBox(probe);
        return box != null ? box.screenH() : 1;
    }

    /** Screen-space rectangle of a section, ported from the historical {@code DietScreen#computeSectionBounds}. */
    private record ScreenBox(int screenX, int screenY, int screenW, int screenH) {}

    private static Bounds toBounds(ScreenBox box) {
        return new Bounds(box.screenX(), box.screenY(), box.screenW(), box.screenH());
    }

    /** Same stacked-Y math and visibility gating as the historical {@code computeSectionBounds}'s recent-meals half. */
    private ScreenBox recentMealsBox(DietLayout.Layout layout) {
        if (mc.player == null) {
            return null;
        }
        NourishedClientConfig cc = NourishedClientConfig.get();
        List<String> recentIds = MarieClientCache.getRecentSourceIds();
        if (!cc.showRecentMeals() || recentIds.isEmpty()) {
            return null;
        }
        int y = stackStartLocalY(cc);
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        int maxY = DietLayout.HEIGHT - DietLayout.PAD;
        int rowH = Math.max(1, (int) Math.round(14 * layout.recentMealsScale()));
        int recentContentH = 10 + Math.min(3, recentIds.size()) * rowH;
        int boxLocalH = recentContentH + 4;
        if (y + boxLocalH > maxY) {
            return null;
        }
        int localX = DietLayout.PAD - 2;
        int localY = y - 2;
        return new ScreenBox(
                DietLayout.toScreenX(layout, localX),
                DietLayout.toScreenY(layout, localY),
                DietLayout.toScreenDim(layout, bw + 4),
                DietLayout.toScreenDim(layout, boxLocalH)
        );
    }

    /** Same stacked-Y math and visibility gating as the historical {@code computeSectionBounds}'s eat-more half. */
    private ScreenBox eatMoreBox(DietLayout.Layout layout) {
        if (mc.player == null) {
            return null;
        }
        NourishedClientConfig cc = NourishedClientConfig.get();
        List<String> neglected = MarieClientCache.getNeglectedCategories();
        if (!cc.showEatMoreOf() || neglected.isEmpty()) {
            return null;
        }
        int y = eatMoreStartLocalY(layout, cc);
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        int maxY = DietLayout.HEIGHT - DietLayout.PAD;
        int eatBoxH = Math.max(1, (int) Math.round(46 * layout.eatMoreScale()));
        if (y + eatBoxH > maxY) {
            return null;
        }
        int localX = DietLayout.PAD - 2;
        int localY = y - 2;
        return new ScreenBox(
                DietLayout.toScreenX(layout, localX),
                DietLayout.toScreenY(layout, localY),
                DietLayout.toScreenDim(layout, bw + 4),
                DietLayout.toScreenDim(layout, eatBoxH)
        );
    }

    /**
     * Local Y just before the Eat More box itself — after the Today header/Calories/Balance stack
     * ({@link #stackStartLocalY}) and, if shown and it fits, the Recent Meals box — matching
     * {@code ClassicDietLeftPanel#drawLeftPanel}'s fixed draw order. Shared by {@link #eatMoreBox}
     * and {@link #maxSafeEatMoreScale} so the stacking math lives in exactly one place.
     */
    private int eatMoreStartLocalY(DietLayout.Layout layout, NourishedClientConfig cc) {
        int y = stackStartLocalY(cc);
        int maxY = DietLayout.HEIGHT - DietLayout.PAD;
        List<String> recentIds = MarieClientCache.getRecentSourceIds();
        if (cc.showRecentMeals() && !recentIds.isEmpty()) {
            int rowH = Math.max(1, (int) Math.round(14 * layout.recentMealsScale()));
            int recentContentH = 10 + Math.min(3, recentIds.size()) * rowH;
            int boxLocalH = recentContentH + 4;
            if (y + boxLocalH <= maxY) {
                y += boxLocalH;
            }
        }
        return y;
    }

    /**
     * Upper bound on {@code eatMoreScale} so growing the Eat More box can never push Active
     * Effects past its minimum draw budget — header plus at least one effect row, or its "none"
     * line, per {@code ClassicDietLeftPanel#drawActiveEffects} — within the fixed Calories ->
     * Balance -> Recent Meals -> Eat More -> Active Effects stack against {@code maxY}. Relaxes to
     * {@link #MAX_SCALE} when Active Effects won't draw at all, using the same
     * {@code showActiveEffects()}/no-player gate {@code ClassicDietLeftPanel} checks before calling
     * {@code drawActiveEffects}.
     */
    private double maxSafeEatMoreScale(DietLayout.Layout layout) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        if (!cc.showActiveEffects() || mc.player == null) {
            return MAX_SCALE;
        }
        int effectCount = mc.player.getActiveEffects().size();
        int activeEffectsMinFootprint = 10 + Math.min(1, effectCount) * 9;

        int y = eatMoreStartLocalY(layout, cc);
        int maxY = DietLayout.HEIGHT - DietLayout.PAD;
        int budgetLocal = maxY - y - activeEffectsMinFootprint;
        if (budgetLocal <= 0) {
            return MIN_SCALE;
        }
        return Mth.clamp(budgetLocal / 46.0d, MIN_SCALE, MAX_SCALE);
    }

    /** Local Y just past the Today header + whichever of Calories/Balance boxes are shown, matching {@code DietScreen}'s left-column stack. */
    private static int stackStartLocalY(NourishedClientConfig cc) {
        int y = 20 + 10;
        if (FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox()) {
            y += 45;
        }
        if (cc.showBalanceBox()) {
            y += 55;
        }
        return y;
    }

    private void commitPanel(Bounds bounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        double scale = Mth.clamp(bounds.width() / (double) baselinePanelW, MIN_SCALE, MAX_SCALE);
        DietLayout.Layout scaled = DietLayout.compute(mc, scale, 0, 0, 1.0d, 1.0d);
        cc.setDietScale(scale);
        cc.setDietOffsetX(bounds.x() - scaled.baseX());
        cc.setDietOffsetY(bounds.y() - scaled.baseY());
        NourishedClientConfig.saveNow();
    }

    private void commitRecentMeals(Bounds bounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        DietLayout.Layout panelForBoxes = DietLayout.compute(mc, cc.dietScale(), 0, 0, 1.0d, cc.eatMoreBoxScale());
        int baseline = recentMealsBaselineHeight(panelForBoxes);
        double scale = baseline > 0 ? Mth.clamp(bounds.height() / (double) baseline, MIN_SCALE, MAX_SCALE) : 1.0d;
        cc.setRecentMealsBoxScale(scale);
        NourishedClientConfig.saveNow();
    }

    private void commitEatMore(Bounds bounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        DietLayout.Layout panelForBoxes = DietLayout.compute(mc, cc.dietScale(), 0, 0, cc.recentMealsBoxScale(), 1.0d);
        int baseline = eatMoreBaselineHeight(panelForBoxes);
        double scale = baseline > 0 ? Mth.clamp(bounds.height() / (double) baseline, MIN_SCALE, MAX_SCALE) : 1.0d;
        cc.setEatMoreBoxScale(scale);
        NourishedClientConfig.saveNow();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        // Unused: DraggableResizable clamps against the Constraint passed to its own constructor,
        // never against target.constraint() — same as the dynamic DietScreenEditTarget.
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (lastRecentMealsBounds != null && recentMealsDrag.mouseClicked(mx, my, lastRecentMealsBounds)) {
            return true;
        }
        if (lastEatMoreBounds != null && eatMoreDrag.mouseClicked(mx, my, lastEatMoreBounds)) {
            return true;
        }
        return lastPanelBounds != null && panelDrag.mouseClicked(mx, my, lastPanelBounds);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        boolean any = false;
        if (recentMealsDrag.isDragging() || recentMealsDrag.isResizing()) {
            recentMealsDrag.mouseDragged(mx, my);
            any = true;
        }
        if (eatMoreDrag.isDragging() || eatMoreDrag.isResizing()) {
            eatMoreDrag.mouseDragged(mx, my);
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
        boolean any = recentMealsDrag.isDragging() || recentMealsDrag.isResizing()
                || eatMoreDrag.isDragging() || eatMoreDrag.isResizing()
                || panelDrag.isDragging() || panelDrag.isResizing();
        int mx = (int) mouseX;
        int my = (int) mouseY;
        recentMealsDrag.mouseReleased(mx, my);
        eatMoreDrag.mouseReleased(mx, my);
        panelDrag.mouseReleased(mx, my);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        if (!(context instanceof GuiGraphicsRenderContext guiContext)) {
            return;
        }
        int[] mouse = scaledMouse(mc);
        int mx = mouse[0];
        int my = mouse[1];
        NourishedClientConfig cc = NourishedClientConfig.get();

        DietLayout.Layout persistedPanelOnly = DietLayout.compute(mc, cc.dietScale(), cc.dietOffsetX(), cc.dietOffsetY(), 1.0d, 1.0d);
        Bounds panelDefault = new Bounds(persistedPanelOnly.panelX(), persistedPanelOnly.panelY(), persistedPanelOnly.panelW(), persistedPanelOnly.panelH());
        Bounds panelBounds = liveOrDefault(panelDrag, mx, my, panelDefault);
        double liveScale = Mth.clamp(panelBounds.width() / (double) baselinePanelW, MIN_SCALE, MAX_SCALE);
        panelDrag.setConstraint(panelConstraintFor(liveScale));

        DietLayout.Layout panelForBoxes = new DietLayout.Layout(
                panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(),
                persistedPanelOnly.baseX(), persistedPanelOnly.baseY(),
                liveScale, cc.recentMealsBoxScale(), cc.eatMoreBoxScale(),
                0
        );

        int recentBaseline = recentMealsBaselineHeight(panelForBoxes);
        recentMealsDrag.setConstraint(boxConstraintFor(panelForBoxes, recentBaseline, cc.recentMealsBoxScale(), MAX_SCALE));
        ScreenBox recentAtPersisted = recentMealsBox(panelForBoxes);
        Bounds recentDefault = recentAtPersisted != null ? toBounds(recentAtPersisted) : null;
        Bounds recentLive = recentDefault != null ? liveOrDefault(recentMealsDrag, mx, my, recentDefault) : null;
        double liveRecentScale = (recentLive != null && recentBaseline > 0)
                ? Mth.clamp(recentLive.height() / (double) recentBaseline, MIN_SCALE, MAX_SCALE)
                : cc.recentMealsBoxScale();

        DietLayout.Layout panelForEatMore = new DietLayout.Layout(
                panelForBoxes.panelX(), panelForBoxes.panelY(), panelForBoxes.panelW(), panelForBoxes.panelH(),
                panelForBoxes.baseX(), panelForBoxes.baseY(),
                liveScale, liveRecentScale, cc.eatMoreBoxScale(),
                0
        );
        int eatMoreBaseline = eatMoreBaselineHeight(panelForEatMore);
        eatMoreDrag.setConstraint(boxConstraintFor(panelForEatMore, eatMoreBaseline, cc.eatMoreBoxScale(), maxSafeEatMoreScale(panelForEatMore)));
        ScreenBox eatMoreAtPersisted = eatMoreBox(panelForEatMore);
        Bounds eatMoreDefault = eatMoreAtPersisted != null ? toBounds(eatMoreAtPersisted) : null;
        Bounds eatMoreLive = eatMoreDefault != null ? liveOrDefault(eatMoreDrag, mx, my, eatMoreDefault) : null;
        double liveEatMoreScale = (eatMoreLive != null && eatMoreBaseline > 0)
                ? Mth.clamp(eatMoreLive.height() / (double) eatMoreBaseline, MIN_SCALE, MAX_SCALE)
                : cc.eatMoreBoxScale();

        DietLayout.Layout matchedLayout = new DietLayout.Layout(
                panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(),
                panelForBoxes.baseX(), panelForBoxes.baseY(),
                liveScale, liveRecentScale, liveEatMoreScale,
                0
        );

        ClassicDietScreen.renderForEdit(guiContext.graphics(), mc, matchedLayout, mx, my);

        Bounds panelHandleBounds = new Bounds(matchedLayout.panelX(), matchedLayout.panelY(), matchedLayout.panelW(), matchedLayout.panelH());
        drawHandle(context, panelDrag, panelHandleBounds, mx, my);

        ScreenBox recentFinal = recentMealsBox(matchedLayout);
        Bounds recentFinalBounds = recentFinal != null ? toBounds(recentFinal) : null;
        if (recentFinalBounds != null) {
            drawHandle(context, recentMealsDrag, recentFinalBounds, mx, my);
        }

        ScreenBox eatMoreFinal = eatMoreBox(matchedLayout);
        Bounds eatMoreFinalBounds = eatMoreFinal != null ? toBounds(eatMoreFinal) : null;
        if (eatMoreFinalBounds != null) {
            drawHandle(context, eatMoreDrag, eatMoreFinalBounds, mx, my);
        }

        this.lastPanelBounds = panelHandleBounds;
        this.lastRecentMealsBounds = recentFinalBounds;
        this.lastEatMoreBounds = eatMoreFinalBounds;
    }

    private static void drawHandle(RenderContext context, DraggableResizable drag, Bounds bounds, int mx, int my) {
        Bounds handle = DraggableResizable.handleBounds(bounds);
        context.drawResizeHandle(handle.x(), handle.y(), drag.isHandleHovered(mx, my, bounds), drag.isHandleActive());
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

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
