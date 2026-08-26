package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.api.SnapRegistry;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.client.hud.classic.ClassicHudPanelRenderer;
import dev.maire.nourished.client.hud.dynamic.layout.HudLayout;
import dev.maire.nourished.client.hud.dynamic.modules.NutrientPanelContainer;
import dev.maire.nourished.client.hud.dynamic.visibility.HudVisibility;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;

/**
 * Single stable {@link MarieComponent} target for the HUD's MarieUI edit mode. Unlike
 * {@link dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget} this wraps exactly one
 * draggable/resizable region (the whole nutrient panel), so it holds a single
 * {@link DraggableResizable} rather than one per sub-region.
 *
 * <p>Dragging is purely spatial now, on every edge and both corners: content size (icon/bar/text
 * scale) always comes from {@link HudLayout#compute(Minecraft, List)}'s own {@code cc.hudScale()} —
 * the same single config value classic mode already renders from — never from the on-screen box's
 * dimensions. Resizing the box only changes how much reserved blank room surrounds that
 * fixed-size content: {@code leftMargin} on the left (grown only by a LEFT-edge/bottom-left-corner
 * drag), and free trailing space on the right/bottom for any other resize, since content no longer
 * stretches to fill the box (see {@code NutrientBarComponent#constraint}'s {@code
 * expandHorizontal = false} and {@link HudLayout.Layout#naturalPanelW}). Width and height each
 * follow {@link AutoGrowPanelContainer}'s existing manual/auto split (the same one {@code
 * DietPanelLayoutResolver} uses): auto-sized to content's natural size until the player explicitly
 * drags a handle affecting that axis, at which point {@link #commit} marks it manual and the
 * dragged size is respected — not silently overwritten — until they resize that axis again.
 *
 * <p>Position AND size are persisted independently via {@link UiStatePersistence} (same
 * {@link ComponentState} model {@code DietScreenPersistence} uses for the Diet Screen's panel/
 * sub-boxes).
 */
public final class HudEditTarget implements MarieComponent {

    private static final String ID = "nourished.hud.editwrapper";
    private static final String PANEL_ID = "nourished.hud.panel";

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    private final Minecraft mc;
    private final DraggableResizable panelDrag;

    public HudEditTarget(Minecraft mc) {
        this.mc = mc;

        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            keys = NutrientRegistry.getKeys();
        }
        HudLayout.Layout natural = HudLayout.compute(mc, keys);

        Constraint constraint = new Constraint(
                new Size(natural.panelW(), natural.panelH()),
                new Size(natural.panelW(), natural.panelH()),
                new Size((int) (natural.panelW() * MAX_MARGIN_MULTIPLIER), (int) (natural.panelH() * MAX_MARGIN_MULTIPLIER)),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
        panelDrag = new DraggableResizable(this, constraint, (target, bounds) -> commit(bounds));
        panelDrag.setSnapRegistryId(PANEL_ID);
        SnapRegistry.register(PANEL_ID, () -> resolvedBounds(this.mc, currentVisibleKeysOrFallback()));
    }

    /** Same "empty visible keys -> fall back to every registered nutrient" reasoning the constructor uses above, so the {@link SnapRegistry} bounds supplier never resolves an empty-key layout while other components still expect this panel's real on-screen box. */
    private static List<String> currentVisibleKeysOrFallback() {
        List<String> keys = currentVisibleKeys();
        return keys.isEmpty() ? NutrientRegistry.getKeys() : keys;
    }

    /**
     * Resolves the HUD panel's current full {@link HudLayout.Layout}: persisted independent
     * position/size if the user has committed a main-panel drag/resize (via {@link #commit}),
     * otherwise today's content-driven default. Content-scale fields (barW, rowH, iconSize,
     * labelScale, scale, etc.) always come from {@code natural} — {@code cc.hudScale()}-derived,
     * never from the box — so they're identical whether or not a resize was ever committed.
     *
     * <p>Width and height each auto-follow {@code natural}'s content-driven size unless the player
     * explicitly resized that axis ({@link AutoGrowPanelContainer}'s manual/auto split, same one
     * {@code DietPanelLayoutResolver} uses) — this is what lets a third-party mod register more
     * nutrients via {@link dev.maire.nourished.api.NourishedAPI} and have the box grow to fit them
     * instead of clipping into a stale persisted size the player never touched.
     *
     * <p>Called by both the normal (non-edit) HUD render path and this class's own edit-mode
     * rendering, so a committed resize is reflected consistently everywhere — not just while
     * {@link HudEditTarget} itself is active.
     */
    public static HudLayout.Layout resolvedLayout(Minecraft mc, List<String> keys) {
        HudLayout.Layout natural = HudLayout.compute(mc, keys);
        return UiStatePersistence.get().load(PANEL_ID)
                .map(state -> {
                    AutoGrowPanelContainer.ManualOverride override =
                            new AutoGrowPanelContainer.ManualOverride(state.widthManual(), state.heightManual());
                    int width = AutoGrowPanelContainer.resolveWidth(override, state.width(), natural.panelW());
                    int height = AutoGrowPanelContainer.resolveHeight(override, state.height(), natural.panelH());
                    return new HudLayout.Layout(
                            state.x(), state.y(), width, height,
                            natural.baseX(), natural.baseY(),
                            natural.barW(), natural.rowH(), natural.iconSize(), natural.maxLabelSw(),
                            natural.scaledPad(), natural.labelScale(), natural.scale(), natural.verticalLayout(),
                            natural.verticalBarW(), natural.verticalBarH(), natural.verticalColumnW(),
                            natural.panelW(), natural.panelH(), state.leftMargin()
                    );
                })
                .orElse(natural);
    }

    /** Only grown/shrunk by a left-edge (or bottom-left-corner) gesture — any other resize/reposition leaves it untouched. */
    private static int persistedLeftMargin() {
        return UiStatePersistence.get().load(PANEL_ID)
                .map(ComponentState::leftMargin)
                .orElse(0);
    }

    /** Resolves the HUD panel's current on-screen bounds — see {@link #resolvedLayout}. */
    static Bounds resolvedBounds(Minecraft mc, List<String> keys) {
        HudLayout.Layout layout = resolvedLayout(mc, keys);
        return new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
    }

    private static AutoGrowPanelContainer.ManualOverride existingManualOverride() {
        return UiStatePersistence.get().load(PANEL_ID)
                .map(s -> new AutoGrowPanelContainer.ManualOverride(s.widthManual(), s.heightManual()))
                .orElse(AutoGrowPanelContainer.ManualOverride.NONE);
    }

    private void commit(Bounds bounds) {
        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            return;
        }
        // Read before this commit's own save below overwrites it — same "pre-gesture" width the
        // margin delta needs, mirroring DietScreenEditTarget's panelDrag onCommit.
        int widthBeforeGesture = resolvedBounds(mc, keys).width();
        int leftMargin = panelDrag.lastCommitWasLeftEdge()
                ? Math.max(0, persistedLeftMargin() + (bounds.width() - widthBeforeGesture))
                : persistedLeftMargin();
        AutoGrowPanelContainer.ManualOverride existing = existingManualOverride();
        AutoGrowPanelContainer.ManualOverride override = AutoGrowPanelContainer.withCommit(existing, panelDrag);
        UiStatePersistence.get().save(PANEL_ID, new ComponentState(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), false,
                override.widthManual(), override.heightManual(), leftMargin));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        // Unused: DraggableResizable clamps against the Constraint passed to its own constructor,
        // never against target.constraint() — same as DietScreenEditTarget's constraint() override.
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            return false;
        }
        Bounds bounds = resolvedBounds(mc, keys);
        return panelDrag.mouseClicked((int) mouseX, (int) mouseY, bounds);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panelDrag.isDragging() || panelDrag.isResizing()) {
            panelDrag.mouseDragged((int) mouseX, (int) mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean any = panelDrag.isDragging() || panelDrag.isResizing();
        panelDrag.mouseReleased((int) mouseX, (int) mouseY);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            return;
        }

        int[] mouse = scaledMouse(mc);
        Bounds defaultBounds = resolvedBounds(mc, keys);
        Bounds bounds = liveOrDefault(mouse[0], mouse[1], defaultBounds);

        Map<String, Float> displayValues = NourishedHUD.currentDisplayValues();
        HudLayout.Layout matchedLayout = matchedLayoutFor(keys, bounds);
        if (NourishedClientConfig.get().hudClassicMode() && context instanceof GuiGraphicsRenderContext guiContext) {
            TrackingData data = MarieClientCache.get();
            ClassicHudPanelRenderer.drawPanel(
                    guiContext.graphics(), mc, data, keys, matchedLayout, bounds.x(), bounds.y(), displayValues
            );
        } else {
            NutrientPanelContainer panel = new NutrientPanelContainer(keys, matchedLayout, displayValues);
            panel.render(context, bounds);
        }

        Bounds handle = DraggableResizable.handleBounds(bounds);
        context.drawResizeHandle(handle.x(), handle.y(), panelDrag.isHandleHovered(mouse[0], mouse[1], bounds), panelDrag.isHandleActive());
        Bounds handleBL = DraggableResizable.handleBoundsBottomLeft(bounds);
        context.drawResizeHandle(handleBL.x(), handleBL.y(), panelDrag.isHandleBottomLeftHovered(mouse[0], mouse[1], bounds), panelDrag.isBottomLeftCornerActive());
        for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
            Bounds strip = DraggableResizable.edgeHandleBounds(bounds, edge);
            context.drawEdgeHandle(strip.x(), strip.y(), strip.width(), strip.height(), mouse[0], mouse[1],
                    panelDrag.isEdgeHovered(mouse[0], mouse[1], bounds, edge), panelDrag.isEdgeActive(edge));
        }
    }

    private Bounds liveOrDefault(int mx, int my, Bounds fallback) {
        if (panelDrag.isDragging() || panelDrag.isResizing()) {
            Bounds preview = panelDrag.mouseDragged(mx, my);
            if (preview != null) {
                return preview;
            }
        }
        return fallback;
    }

    /**
     * Rebuilds a full {@link HudLayout.Layout} for the live preview {@code bounds}, with position
     * AND size pinned to {@code bounds} itself rather than anchor/formula-recomputed — same
     * approach as {@code DietScreenEditTarget#matchedLayoutFor}, so the live preview rectangle and
     * the content drawn inside it always agree. Content-scale fields come from {@code natural}
     * ({@code cc.hudScale()}-only) regardless of {@code bounds} — dragging never rescales content,
     * only {@link #resolvedLayout}'s doc explains why width/height are still independently tracked.
     */
    private HudLayout.Layout matchedLayoutFor(List<String> keys, Bounds bounds) {
        // Live left-edge (or bottom-left-corner) drag -> grows the margin by this gesture's width
        // delta; any other gesture leaves it exactly as last persisted. Mirrors
        // DietScreenEditTarget#matchedLayoutFor's leftMargin handling.
        int widthBeforeGesture = resolvedBounds(mc, keys).width();
        int leftMargin = (panelDrag.isEdgeActive(DraggableResizable.Edge.LEFT) || panelDrag.isBottomLeftCornerActive())
                ? Math.max(0, persistedLeftMargin() + (bounds.width() - widthBeforeGesture))
                : persistedLeftMargin();
        HudLayout.Layout natural = HudLayout.compute(mc, keys);
        return new HudLayout.Layout(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                natural.baseX(), natural.baseY(),
                natural.barW(), natural.rowH(), natural.iconSize(), natural.maxLabelSw(),
                natural.scaledPad(), natural.labelScale(), natural.scale(), natural.verticalLayout(),
                natural.verticalBarW(), natural.verticalBarH(), natural.verticalColumnW(),
                natural.panelW(), natural.panelH(), leftMargin
        );
    }

    private static List<String> currentVisibleKeys() {
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) {
            return keys;
        }
        TrackingData data = MarieClientCache.get();
        if (data == null) {
            return List.of();
        }
        NourishedClientConfig cc = NourishedClientConfig.get();
        return HudVisibility.visibleKeys(data, cc.effectiveDietBarOrder(), cc);
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
