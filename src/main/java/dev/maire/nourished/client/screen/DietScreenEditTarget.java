package dev.maire.nourished.client.screen;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.Anchor;
import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.ComponentState;
import dev.marie.framework.ui.Constraint;
import dev.marie.framework.ui.DraggableResizable;
import dev.marie.framework.ui.Insets;
import dev.marie.framework.ui.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Size;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;

/**
 * Single stable {@link MarieComponent} target for the Diet Screen's MarieUI edit mode. Owns four
 * {@link DraggableResizable} trackers (main panel, recent-meals, eat-more, active-effects) as
 * long-lived fields — unlike {@link DietPanelContainer}/{@link DietLeftColumnComponent}/{@link
 * RecentMealsComponent}/{@link EatMoreComponent}/{@link ActiveEffectsComponent}, which are rebuilt
 * every frame for the normal render path, this object must survive across the many frames a single
 * drag/resize gesture spans (mouseClicked on frame N through mouseReleased on frame N+k), per the
 * confirmed cross-frame-survival requirement — so {@link DietScreen} holds exactly one instance,
 * lazily constructed on first entry into edit mode.
 *
 * <p>Per-frame data (current {@link TrackingData}, bar order) is re-fetched fresh on every
 * {@link #render}/{@link #mouseClicked} call rather than cached, matching how the normal path
 * already re-fetches every frame. Nutrient bar values shown while editing are the raw/live values
 * (no lerp/animation) — edit mode never animates values.
 */
final class DietScreenEditTarget implements MarieComponent {

    private static final String ID = "nourished.diet.editwrapper";
    static final String PANEL_ID = "nourished.diet.panel";

    private final Minecraft mc;
    private final Runnable exitEditMode;
    private final DraggableResizable panelDrag;
    private final DraggableResizable recentMealsDrag;
    private final DraggableResizable eatMoreDrag;
    private final DraggableResizable activeEffectsDrag;

    /**
     * The exact {@code resolvedBounds()} render() last drew the sub-boxes at, cached here so
     * mouseClicked's hit-test reads the literal on-screen truth instead of reconstructing a second,
     * independent {@link RecentMealsComponent}/{@link EatMoreComponent}/{@link
     * ActiveEffectsComponent} to approximate it — closing off the class of bug fixed for the render
     * path (two independently-derived Bounds for what should be one source of truth), this time for
     * the click path. Null until the first render().
     */
    private Bounds lastRecentResolvedBounds;
    private Bounds lastEatMoreResolvedBounds;
    private Bounds lastActiveEffectsResolvedBounds;

    DietScreenEditTarget(Minecraft mc, Runnable exitEditMode) {
        this.mc = mc;
        this.exitEditMode = exitEditMode;

        DietLayout.Layout baseLayout = DietLayout.compute(mc);
        RecentMealsComponent defaultRecent = freshRecentMeals(baseLayout);
        EatMoreComponent defaultEatMore = freshEatMore(baseLayout, defaultRecent);
        ActiveEffectsComponent defaultActiveEffects = freshActiveEffects(baseLayout, defaultRecent, defaultEatMore);

        // Height floor is PANEL_MIN_LOCAL_HEIGHT (title-bar only), not HEIGHT*0.5 — the panel can now
        // be dragged all the way down to a minimized state (see DietPanelContainer) instead of
        // stopping at a two-column content view. Width floor is unchanged.
        Constraint panelConstraint = boundedConstraint(
                baseLayout.panelW(), baseLayout.panelH(),
                DietLayout.scaledDim(DietLayout.WIDTH, 0.5d), DietLayout.scaledDim(DietLayout.PANEL_MIN_LOCAL_HEIGHT, 1.0d),
                DietLayout.scaledDim(DietLayout.WIDTH, 1.5d), DietLayout.scaledDim(DietLayout.HEIGHT, 1.5d)
        );
        panelDrag = new DraggableResizable(this, panelConstraint,
                (target, bounds) -> DietScreenPersistence.get().save(PANEL_ID, toState(bounds)));

        // Each starting constraint below is only a placeholder good for this construction instant
        // (baseLayout's scale) — render() overwrites it every frame via setConstraint(...) once the
        // live panel scale is known, so it never actually goes stale mid-session.
        recentMealsDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultRecent.naturalLocalHeight())),
                (target, bounds) -> {
                    // Guards the commit itself, not just the clamp's reference size: MarieClientCache
                    // starts every session with an empty recentSourceIds list until the first server
                    // sync arrives (confirmed via logs — real, lasted 12+s after the last restart), and
                    // a resize/drag that happens to release during that window would otherwise persist
                    // a bogus size/position derived from that momentarily-degenerate (empty) reference.
                    if (MarieClientCache.getRecentSourceIds().isEmpty()) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultRecent.id(), toRelativeState(bounds));
                });

        eatMoreDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultEatMore.naturalLocalHeight())),
                (target, bounds) -> {
                    // Same guard for consistency, though EatMore's own naturalLocalHeight() doesn't
                    // depend on neglected's size (eatBoxH is a fixed value) — this only blocks
                    // committing a gesture while the box isn't actually visible/interactive at all.
                    if (MarieClientCache.getNeglectedCategories().isEmpty()) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultEatMore.id(), toRelativeState(bounds));
                });

        activeEffectsDrag = new DraggableResizable(this, liveSubBoxConstraint(naturalPreferredSize(baseLayout, defaultActiveEffects.naturalLocalHeight())),
                (target, bounds) -> {
                    // Same guard for consistency, though ActiveEffectsComponent's naturalLocalHeight()
                    // already floors its line count at 1 even with zero active effects — this only
                    // blocks committing while there's no player to read effects from at all.
                    if (mc.player == null) {
                        return;
                    }
                    DietScreenPersistence.get().save(defaultActiveEffects.id(), toRelativeState(bounds));
                });
    }

    /**
     * Resolves the main panel's current {@link DietLayout.Layout}: persisted position/size if the
     * user has committed a main-panel drag/resize, otherwise today's default centered position —
     * used by both this class's own edit-mode rendering and {@link DietScreen}'s normal (non-edit)
     * MarieUI render path, so a committed resize is reflected consistently everywhere (background,
     * columns, and internal font/icon/bar scale all derive from one matched Layout, avoiding a
     * mismatch between the outer resized rectangle and content sized for the un-resized default).
     *
     * <p>{@code panelW}/{@code panelH} are the independently-resolved screen pixels from {@code
     * resolved} directly — NOT recomputed from a single width-derived scale (the bug that made a
     * height-only edge-resize revert on the next read: {@code resolved.height()} was being read from
     * persistence and then immediately discarded in favor of {@code HEIGHT * derivedScale}, silently
     * forcing the height back into a fixed ratio with the width). {@code derivedScale} itself is
     * still width-only, matching {@link RecentMealsComponent}/{@link EatMoreComponent}'s own
     * {@code contentScale} convention — it drives font/icon/row content scaling via {@code
     * DietLayout.toScreenX/Y/Dim}, but must never be used to reconstruct the outer panel's height.
     */
    static DietLayout.Layout resolvedPanelLayout(Minecraft mc) {
        DietLayout.Layout baseLayout = DietLayout.compute(mc);
        Bounds resolved = DietScreenPersistence.resolve(PANEL_ID, baseLayout, 0, DietLayout.WIDTH, DietLayout.HEIGHT);
        NourishedClientConfig cc = NourishedClientConfig.get();
        double derivedScale = resolved.width() / (double) DietLayout.WIDTH;
        return new DietLayout.Layout(
                resolved.x(), resolved.y(), resolved.width(), resolved.height(),
                resolved.x(), resolved.y(),
                derivedScale, cc.recentMealsBoxScale(), cc.eatMoreBoxScale()
        );
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Unused: {@link DraggableResizable} clamps against the {@link Constraint} passed to its own
     * constructor, never against {@code target.constraint()} — confirmed by prior investigation
     * (its {@code clampWidth}/{@code clampHeight} read the constructor-supplied field, not the
     * target). {@link dev.marie.framework.ui.EditModeController}/{@link
     * dev.marie.framework.ui.EditOverlayScreen} never call {@code target.constraint()} either.
     */
    @Override
    public Constraint constraint() {
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        DietLayout.Layout layout = resolvedPanelLayout(mc);

        // Checked first: the toggle sits inside the panel's own bounds, which
        // DraggableResizable.mouseClicked treats as the whole drag-start region, so it must claim
        // the click before panelDrag ever sees it.
        if (button == 0 && DietScreen.isMouseOverEditModeToggle(layout, mouseX, mouseY)) {
            exitEditMode.run();
            return true;
        }

        // Hit-test against the exact Bounds render() last drew (cached below), not a freshly
        // reconstructed instance that only algebraically ought to match — see class-level field
        // javadoc. Falls through to the next check (rather than throwing) on the one frame this can
        // be null: a click landing before render() has ever run once for this edit session.
        if (lastRecentResolvedBounds != null && recentMealsDrag.mouseClicked(mx, my, lastRecentResolvedBounds)) {
            return true;
        }
        if (lastEatMoreResolvedBounds != null && eatMoreDrag.mouseClicked(mx, my, lastEatMoreResolvedBounds)) {
            return true;
        }
        if (lastActiveEffectsResolvedBounds != null && activeEffectsDrag.mouseClicked(mx, my, lastActiveEffectsResolvedBounds)) {
            return true;
        }
        Bounds panelBounds = DietScreenPersistence.resolve(PANEL_ID, layout, 0, DietLayout.WIDTH, DietLayout.HEIGHT);
        return panelDrag.mouseClicked(mx, my, panelBounds);
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
        boolean any = recentMealsDrag.isDragging() || recentMealsDrag.isResizing()
                || eatMoreDrag.isDragging() || eatMoreDrag.isResizing()
                || activeEffectsDrag.isDragging() || activeEffectsDrag.isResizing()
                || panelDrag.isDragging() || panelDrag.isResizing();
        int mx = (int) mouseX;
        int my = (int) mouseY;
        recentMealsDrag.mouseReleased(mx, my);
        eatMoreDrag.mouseReleased(mx, my);
        activeEffectsDrag.mouseReleased(mx, my);
        panelDrag.mouseReleased(mx, my);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        DietLayout.Layout layout = resolvedPanelLayout(mc);

        int[] mouse = scaledMouse(mc);
        int mx = mouse[0];
        int my = mouse[1];

        Bounds panelDefault = DietScreenPersistence.resolve(PANEL_ID, layout, 0, DietLayout.WIDTH, DietLayout.HEIGHT);
        Bounds panelBounds = liveOrDefault(panelDrag, mx, my, panelDefault);

        TrackingData data = mc.player != null ? MarieClientCache.get() : null;
        List<String> bars = NourishedClientConfig.get().effectiveDietBarOrder();
        Map<String, Float> displayValues = data != null ? data.values : Map.of();

        // Exactly one RecentMealsComponent/EatMoreComponent instance is constructed per frame —
        // the one DietPanelContainer builds internally — read back via its getters. The previous
        // design also constructed a second, independent "overlay" copy from a different Layout
        // (resolvedPanelLayout, the last-committed state) than the container's own
        // (matchedPanelLayout, the live drag preview); those two diverged whenever the main panel
        // was actively being dragged, producing two different resolved Bounds for what should be
        // one box — confirmed via debug logging. Fixed by resolving the live-or-default bounds from
        // the container's own instance, then feeding it back as a render-bounds override instead of
        // rendering a second copy.
        DietLayout.Layout matchedPanelLayout = matchedLayoutFor(panelBounds);
        DietPanelContainer panel = new DietPanelContainer(data, bars, displayValues, matchedPanelLayout);
        RecentMealsComponent recent = panel.recentMealsComponent();
        EatMoreComponent eatMore = panel.eatMoreComponent();
        ActiveEffectsComponent activeEffects = panel.activeEffectsComponent();

        // Refreshed every frame from the live panel scale (matchedPanelLayout), not just once at
        // edit-mode entry — a resize-handle clamp frozen at whatever scale was active when this
        // DietScreenEditTarget was constructed converts wrong once the panel's actual scale drifts
        // away from that (e.g. re-entering edit mode later at a different persisted panel size): a
        // screen-pixel minimum computed at the old scale, divided back down by a very different live
        // scale at commit time, can produce a persisted local-unit size near zero. See
        // DraggableResizable#setConstraint.
        //
        // Deliberately uses naturalLocalHeight() (content-driven, always positive), NOT
        // component.constraint().preferredSize() (visibility-gated, floors to a 1px-tall preferred
        // size whenever the box isn't showing this exact frame) — the latter caused this exact class
        // of bug: a clamp rebuilt every frame from a preferred size that can momentarily collapse to
        // ~1px, committed on the wrong frame, produces a persisted size stuck near zero.
        recentMealsDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, recent.naturalLocalHeight())));
        eatMoreDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, eatMore.naturalLocalHeight())));
        activeEffectsDrag.setConstraint(liveSubBoxConstraint(naturalPreferredSize(matchedPanelLayout, activeEffects.naturalLocalHeight())));

        Bounds recentBounds = clampToParent(liveOrDefault(recentMealsDrag, mx, my, recent.resolvedBounds()), panelBounds);
        Bounds eatMoreBounds = clampToParent(liveOrDefault(eatMoreDrag, mx, my, eatMore.resolvedBounds()), panelBounds);
        Bounds activeEffectsBounds = clampToParent(liveOrDefault(activeEffectsDrag, mx, my, activeEffects.resolvedBounds()), panelBounds);
        panel.setSubBoxRenderBounds(recentBounds, eatMoreBounds, activeEffectsBounds);

        // Cache the canonical (not-currently-dragging) resolvedBounds() for mouseClicked to hit-test
        // against next click — see the field javadoc. Intentionally the committed resolvedBounds(),
        // not the live-preview recentBounds/eatMoreBounds/activeEffectsBounds, matching what a fresh
        // click should compare against (DraggableResizable.mouseClicked's own contract: "current
        // committed bounds").
        lastRecentResolvedBounds = recent.resolvedBounds();
        lastEatMoreResolvedBounds = eatMore.resolvedBounds();
        lastActiveEffectsResolvedBounds = activeEffects.resolvedBounds();

        panel.render(context, panelBounds);

        // Each sub-box's handle is gated on its own isVisible() — false when the box is fully hidden
        // (config-disabled, no data, or its stacked position no longer fits the live panel height,
        // e.g. the panel has been minimized — see DietPanelContainer's minimized short-circuit). Not
        // the same thing as EatMoreComponent's older per-content contentFits gate removed last round
        // (that tracked its own icon-row collapse, a state that still renders a box+header); this is
        // "does this section exist on screen AT ALL right now" — without it, a hidden section's
        // handle rendered as a leftover icon with nothing behind it. The panel's own handle is never
        // gated: it must stay clickable even when minimized, since dragging it is how the panel is
        // un-minimized.
        drawHandle(context, panelDrag, panelBounds, mx, my);
        if (recent.isVisible()) {
            drawHandle(context, recentMealsDrag, recentBounds, mx, my);
        }
        if (eatMore.isVisible()) {
            drawHandle(context, eatMoreDrag, eatMoreBounds, mx, my);
        }
        if (activeEffects.isVisible()) {
            drawHandle(context, activeEffectsDrag, activeEffectsBounds, mx, my);
        }

        // Anchored to matchedPanelLayout (derived from the live panelBounds preview), not the
        // static resolvedPanelLayout, so it tracks the panel's actual on-screen rectangle while
        // it's being dragged/resized, not just its last-committed position. Always drawn active:
        // if this is rendering at all, edit mode is definitionally on.
        boolean toggleHovered = DietScreen.isMouseOverEditModeToggle(matchedPanelLayout, mx, my);
        DietScreen.drawEditModeToggle(context, matchedPanelLayout, true, toggleHovered);
    }

    private static void drawHandle(RenderContext context, DraggableResizable drag, Bounds bounds, int mx, int my) {
        Bounds handle = DraggableResizable.handleBounds(bounds);
        context.drawResizeHandle(handle.x(), handle.y(), drag.isHandleHovered(mx, my, bounds), drag.isHandleActive());
        for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
            Bounds strip = DraggableResizable.edgeHandleBounds(bounds, edge);
            context.drawEdgeHandle(strip.x(), strip.y(), strip.width(), strip.height(), mx, my,
                    drag.isEdgeHovered(mx, my, bounds, edge), drag.isEdgeActive(edge));
        }
    }

    /**
     * Confines {@code child} to {@code parent}'s rectangle: shrinks width/height down to the
     * parent's own dimensions first, then slides x/y so the whole box sits within the parent's
     * bounds. Applied to the live drag/resize preview (not just the settled/persisted value handled
     * by {@link DietScreenPersistence#resolveRelativeToPanel}) so a sub-box can't visibly leave the
     * main panel mid-gesture either.
     */
    private static Bounds clampToParent(Bounds child, Bounds parent) {
        int w = Math.min(child.width(), parent.width());
        int h = Math.min(child.height(), parent.height());
        int x = Math.max(parent.x(), Math.min(child.x(), parent.x() + parent.width() - w));
        int y = Math.max(parent.y(), Math.min(child.y(), parent.y() + parent.height() - h));
        return new Bounds(x, y, w, h);
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

    /**
     * Same fix as {@link #resolvedPanelLayout}: {@code panelBounds}' own width/height are used
     * directly, not recomputed from a width-only {@code derivedScale} — this is the live drag/resize
     * preview path, so during an active height-only edge-resize this must reflect the live height
     * immediately, not just after commit.
     */
    private static DietLayout.Layout matchedLayoutFor(Bounds panelBounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        double derivedScale = panelBounds.width() / (double) DietLayout.WIDTH;
        return new DietLayout.Layout(
                panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(),
                panelBounds.x(), panelBounds.y(),
                derivedScale, cc.recentMealsBoxScale(), cc.eatMoreBoxScale()
        );
    }

    private static RecentMealsComponent freshRecentMeals(DietLayout.Layout layout) {
        int headerEndLocalY = DietLeftColumnComponent.computeHeaderEndLocalY();
        return new RecentMealsComponent(layout, headerEndLocalY);
    }

    private static EatMoreComponent freshEatMore(DietLayout.Layout layout, RecentMealsComponent recent) {
        int headerEndLocalY = DietLeftColumnComponent.computeHeaderEndLocalY();
        int eatMoreStartLocalY = DietLeftColumnComponent.nextSiblingStartLocalY(
                headerEndLocalY, recent.localHeight(), recent.resolvedBounds(), layout);
        return new EatMoreComponent(layout, eatMoreStartLocalY);
    }

    private static ActiveEffectsComponent freshActiveEffects(DietLayout.Layout layout, RecentMealsComponent recent, EatMoreComponent eatMore) {
        int headerEndLocalY = DietLeftColumnComponent.computeHeaderEndLocalY();
        int eatMoreStartLocalY = DietLeftColumnComponent.nextSiblingStartLocalY(
                headerEndLocalY, recent.localHeight(), recent.resolvedBounds(), layout);
        int activeEffectsStartLocalY = DietLeftColumnComponent.nextSiblingStartLocalY(
                eatMoreStartLocalY, eatMore.localHeight(), eatMore.resolvedBounds(), layout);
        return new ActiveEffectsComponent(layout, activeEffectsStartLocalY);
    }

    private static Constraint boundedConstraint(int prefW, int prefH, int minW, int minH, int maxW, int maxH) {
        return new Constraint(
                new Size(prefW, prefH),
                new Size(minW, minH),
                new Size(maxW, maxH),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
    }

    /** Width stays at the same [0.5x, 1.5x] range as the main panel's own resize constraint. */
    private static final double SUBBOX_MIN_WIDTH_MULTIPLIER = 0.5d;
    private static final double SUBBOX_MAX_WIDTH_MULTIPLIER = 1.5d;
    private static final double SUBBOX_MIN_HEIGHT_MULTIPLIER = 0.5d;

    /**
     * Height max is deliberately much larger than width's 1.5x: these boxes' natural content height
     * is small (recent-meals/eat-more/active-effects are ~40-56 local units by default, versus the
     * main panel's 268), so 1.5x of that is only a handful of extra pixels — nowhere near enough
     * downward room for RecentMeals to show meaningfully more than its natural 3 rows, or
     * ActiveEffects more than its natural handful of lines, now that both collapse cleanly instead of
     * overflowing when they don't fit. 4x gives real headroom (e.g. RecentMeals' natural ~56 local
     * units -> up to ~224, room for a dozen-plus rows) while still being clamped to the panel's own
     * bounds by {@link #clampToParent}/{@code DietScreenPersistence#clampToPanel} regardless.
     */
    private static final double SUBBOX_MAX_HEIGHT_MULTIPLIER = 4.0d;

    /**
     * Builds the bounded {@link Constraint} for a recent-meals/eat-more/active-effects resize
     * handle from {@code pref} — the box's current-frame preferred screen size, i.e. already
     * expressed at whatever panel scale is live <em>this frame</em>, unlike the one-time {@code
     * recentPreferred}/{@code eatMorePreferred}/{@code activeEffectsPreferred} captured in the
     * constructor from {@code baseLayout}. Called fresh every {@link #render} so the clamp never
     * goes stale relative to the panel's current scale.
     */
    private static Constraint liveSubBoxConstraint(Size pref) {
        return boundedConstraint(
                pref.width(), pref.height(),
                (int) Math.round(pref.width() * SUBBOX_MIN_WIDTH_MULTIPLIER), (int) Math.round(pref.height() * SUBBOX_MIN_HEIGHT_MULTIPLIER),
                (int) Math.round(pref.width() * SUBBOX_MAX_WIDTH_MULTIPLIER), (int) Math.round(pref.height() * SUBBOX_MAX_HEIGHT_MULTIPLIER)
        );
    }

    /**
     * The box's reference screen size at {@code layout}'s current scale — width from the fixed
     * structural constant {@code bw} (the left column's width), height from {@code naturalLocalHeight}
     * (content-driven, always positive; see {@link RecentMealsComponent#naturalLocalHeight()}). Used
     * as {@link #liveSubBoxConstraint}'s input instead of the component's own visibility-gated
     * {@code constraint().preferredSize()}.
     */
    private static Size naturalPreferredSize(DietLayout.Layout layout, int naturalLocalHeight) {
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        return new Size(DietLayout.toScreenDim(layout, bw), DietLayout.toScreenDim(layout, naturalLocalHeight));
    }

    private static ComponentState toState(Bounds bounds) {
        return new ComponentState(bounds.x(), bounds.y(), bounds.width(), bounds.height(), false);
    }

    /**
     * Converts a committed absolute-screen {@link Bounds} into an offset+size relative to the
     * panel's current resolved position for persistence — see {@link
     * DietScreenPersistence#resolveRelativeToPanel}. Both the position offset and the width/height
     * are normalized into the panel's local (pre-scale) unit space by dividing by the current
     * {@code panelLayout.scale()}, mirroring {@code resolveRelativeToPanel}'s own
     * {@code startLocalY * scale}/{@code localWidth * scale} default-position/size formulas, so a
     * committed drag/resize keeps tracking proportionally with the panel if it's later resized —
     * rather than freezing as an absolute pixel delta/size that falls out of sync as the panel grows
     * or shrinks (per the user's explicit ask: these boxes should scale with the panel like a normal
     * child widget, while still allowing an independent size/position on top of that). Safe to read
     * the panel's position fresh here rather than from an in-progress drag preview: {@link
     * #mouseDragged} only ever updates one of {@code panelDrag}/{@code recentMealsDrag}/{@code
     * eatMoreDrag} at a time, so the panel is never simultaneously mid-drag when a recent-meals/
     * eat-more commit fires.
     */
    private ComponentState toRelativeState(Bounds bounds) {
        DietLayout.Layout panelLayout = resolvedPanelLayout(mc);
        Bounds panelBounds = new Bounds(panelLayout.panelX(), panelLayout.panelY(), panelLayout.panelW(), panelLayout.panelH());
        Bounds clamped = clampToParent(bounds, panelBounds);
        double scale = panelLayout.scale();
        return new ComponentState(
                (int) Math.round((clamped.x() - panelLayout.panelX()) / scale),
                (int) Math.round((clamped.y() - panelLayout.panelY()) / scale),
                (int) Math.round(clamped.width() / scale),
                (int) Math.round(clamped.height() / scale),
                false
        );
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
