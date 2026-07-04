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
 * Single stable {@link MarieComponent} target for the Diet Screen's MarieUI edit mode. Owns three
 * {@link DraggableResizable} trackers (main panel, recent-meals, eat-more) as long-lived fields —
 * unlike {@link DietPanelContainer}/{@link DietLeftColumnComponent}/{@link RecentMealsComponent}/
 * {@link EatMoreComponent}, which are rebuilt every frame for the normal render path, this object
 * must survive across the many frames a single drag/resize gesture spans (mouseClicked on frame N
 * through mouseReleased on frame N+k), per the confirmed cross-frame-survival requirement — so
 * {@link DietScreen} holds exactly one instance, lazily constructed on first entry into edit mode.
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
    private final DraggableResizable panelDrag;
    private final DraggableResizable recentMealsDrag;
    private final DraggableResizable eatMoreDrag;

    DietScreenEditTarget(Minecraft mc) {
        this.mc = mc;

        DietLayout.Layout baseLayout = DietLayout.compute(mc);
        RecentMealsComponent defaultRecent = freshRecentMeals(baseLayout);
        EatMoreComponent defaultEatMore = freshEatMore(baseLayout, defaultRecent);

        Constraint panelConstraint = boundedConstraint(
                baseLayout.panelW(), baseLayout.panelH(),
                DietLayout.scaledDim(DietLayout.WIDTH, 0.5d), DietLayout.scaledDim(DietLayout.HEIGHT, 0.5d),
                DietLayout.scaledDim(DietLayout.WIDTH, 1.5d), DietLayout.scaledDim(DietLayout.HEIGHT, 1.5d)
        );
        panelDrag = new DraggableResizable(this, panelConstraint,
                (target, bounds) -> DietScreenPersistence.get().save(PANEL_ID, toState(bounds)));

        Size recentPreferred = defaultRecent.constraint().preferredSize();
        Constraint recentConstraint = boundedConstraint(
                recentPreferred.width(), recentPreferred.height(),
                recentPreferred.width(), (int) Math.round(recentPreferred.height() * 0.5d),
                recentPreferred.width(), (int) Math.round(recentPreferred.height() * 1.5d)
        );
        recentMealsDrag = new DraggableResizable(this, recentConstraint,
                (target, bounds) -> DietScreenPersistence.get().save(defaultRecent.id(), toState(bounds)));

        Size eatMorePreferred = defaultEatMore.constraint().preferredSize();
        Constraint eatMoreConstraint = boundedConstraint(
                eatMorePreferred.width(), eatMorePreferred.height(),
                eatMorePreferred.width(), (int) Math.round(eatMorePreferred.height() * 0.5d),
                eatMorePreferred.width(), (int) Math.round(eatMorePreferred.height() * 1.5d)
        );
        eatMoreDrag = new DraggableResizable(this, eatMoreConstraint,
                (target, bounds) -> DietScreenPersistence.get().save(defaultEatMore.id(), toState(bounds)));
    }

    /**
     * Resolves the main panel's current {@link DietLayout.Layout}: persisted position/size if the
     * user has committed a main-panel drag/resize, otherwise today's default centered position —
     * used by both this class's own edit-mode rendering and {@link DietScreen}'s normal (non-edit)
     * MarieUI render path, so a committed resize is reflected consistently everywhere (background,
     * columns, and internal font/icon/bar scale all derive from one matched Layout, avoiding a
     * mismatch between the outer resized rectangle and content sized for the un-resized default).
     */
    static DietLayout.Layout resolvedPanelLayout(Minecraft mc) {
        DietLayout.Layout baseLayout = DietLayout.compute(mc);
        Bounds resolved = DietScreenPersistence.resolve(PANEL_ID, baseLayout, 0, DietLayout.WIDTH, DietLayout.HEIGHT);
        NourishedClientConfig cc = NourishedClientConfig.get();
        double derivedScale = resolved.width() / (double) DietLayout.WIDTH;
        int matchedW = DietLayout.scaledDim(DietLayout.WIDTH, derivedScale);
        int matchedH = DietLayout.scaledDim(DietLayout.HEIGHT, derivedScale);
        return new DietLayout.Layout(
                resolved.x(), resolved.y(), matchedW, matchedH,
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
        DietLayout.Layout layout = DietLayout.compute(mc);
        RecentMealsComponent recent = freshRecentMeals(layout);
        EatMoreComponent eatMore = freshEatMore(layout, recent);

        if (recentMealsDrag.mouseClicked(mx, my, recent.resolvedBounds())) {
            return true;
        }
        if (eatMoreDrag.mouseClicked(mx, my, eatMore.resolvedBounds())) {
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
        DietLayout.Layout layout = DietLayout.compute(mc);
        RecentMealsComponent recent = freshRecentMeals(layout);
        EatMoreComponent eatMore = freshEatMore(layout, recent);

        int[] mouse = scaledMouse(mc);
        int mx = mouse[0];
        int my = mouse[1];

        Bounds panelDefault = DietScreenPersistence.resolve(PANEL_ID, layout, 0, DietLayout.WIDTH, DietLayout.HEIGHT);
        Bounds panelBounds = liveOrDefault(panelDrag, mx, my, panelDefault);
        Bounds recentBounds = liveOrDefault(recentMealsDrag, mx, my, recent.resolvedBounds());
        Bounds eatMoreBounds = liveOrDefault(eatMoreDrag, mx, my, eatMore.resolvedBounds());

        TrackingData data = mc.player != null ? MarieClientCache.get() : null;
        List<String> bars = NourishedClientConfig.get().effectiveDietBarOrder();
        Map<String, Float> displayValues = data != null ? data.values : Map.of();

        DietLayout.Layout matchedPanelLayout = matchedLayoutFor(panelBounds);
        DietPanelContainer panel = new DietPanelContainer(data, bars, displayValues, matchedPanelLayout);
        panel.render(context, panelBounds);

        // Draggable/resizable overlay copies, drawn on top of the panel backdrop so they're what
        // the user actually sees moving/resizing live — see class javadoc on DietPanelContainer's
        // own recent-meals/eat-more (still drawn as part of its normal descendant tree above) for
        // the accepted tradeoff: while one of these two is actively being dragged, its old default
        // position briefly still shows underneath from the backdrop until the drag commits.
        recent.render(context, recentBounds);
        eatMore.render(context, eatMoreBounds);

        drawHandle(context, panelDrag, panelBounds, mx, my);
        drawHandle(context, recentMealsDrag, recentBounds, mx, my);
        drawHandle(context, eatMoreDrag, eatMoreBounds, mx, my);
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

    private static DietLayout.Layout matchedLayoutFor(Bounds panelBounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        double derivedScale = panelBounds.width() / (double) DietLayout.WIDTH;
        int matchedW = DietLayout.scaledDim(DietLayout.WIDTH, derivedScale);
        int matchedH = DietLayout.scaledDim(DietLayout.HEIGHT, derivedScale);
        return new DietLayout.Layout(
                panelBounds.x(), panelBounds.y(), matchedW, matchedH,
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
        return new EatMoreComponent(layout, headerEndLocalY + recent.localHeight());
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

    private static ComponentState toState(Bounds bounds) {
        return new ComponentState(bounds.x(), bounds.y(), bounds.width(), bounds.height(), false);
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
