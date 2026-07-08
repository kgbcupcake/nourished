package dev.maire.nourished.client.hud;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Size;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

/**
 * Single stable {@link MarieComponent} target for the HUD's MarieUI edit mode. Unlike
 * {@link dev.maire.nourished.client.screen.DietScreenEditTarget} this wraps exactly one
 * draggable/resizable region (the whole nutrient panel), so it holds a single
 * {@link DraggableResizable} rather than one per sub-region.
 *
 * <p>Position AND size are persisted independently via {@link UiStatePersistence} (same
 * {@link ComponentState} model {@code DietScreenPersistence} uses for the Diet Screen's panel/
 * sub-boxes), not derived from a single {@code hudScale} value the way this used to work. A
 * single derived scale (previously {@code hudScale = bounds.width() / baselinePanelW}, with
 * {@code panelH} always recomputed from that same scale) cannot represent an independent-axis
 * edge-resize: a height-only resize would compute an unchanged {@code hudScale} and then discard
 * the resized height entirely on the next read. {@code hudScale}/{@code hudOffsetX}/{@code
 * hudOffsetY} are still written on commit and still drive content sizing (icon/label/bar scale)
 * and the default (never-resized) anchor position — only the outer panel's actual on-screen
 * {@code panelX/Y/W/H} now come from the independently-persisted {@link ComponentState} once one
 * exists, via {@link #resolvedLayout}.
 */
final class HudEditTarget implements MarieComponent {

    private static final String ID = "nourished.hud.editwrapper";
    private static final String PANEL_ID = "nourished.hud.panel";

    /** Matches the live preview clamp in {@code NourishedHUD#onEditMouseRelease}'s resize handling. */
    private static final double MIN_SCALE = 0.3d;
    private static final double MAX_SCALE = 3.0d;

    private final Minecraft mc;
    private final DraggableResizable panelDrag;
    private final int baselinePanelW;

    HudEditTarget(Minecraft mc) {
        this.mc = mc;

        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            keys = NutrientRegistry.getKeys();
        }
        HudLayout.Layout base1x = HudLayout.compute(mc, keys, 1.0d);
        this.baselinePanelW = base1x.panelW();

        HudLayout.Layout preferred = HudLayout.compute(mc, keys);
        HudLayout.Layout min = HudLayout.compute(mc, keys, MIN_SCALE);
        HudLayout.Layout max = HudLayout.compute(mc, keys, MAX_SCALE);

        Constraint constraint = new Constraint(
                new Size(preferred.panelW(), preferred.panelH()),
                new Size(min.panelW(), min.panelH()),
                new Size(max.panelW(), max.panelH()),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
        panelDrag = new DraggableResizable(this, constraint, (target, bounds) -> commit(bounds));
    }

    /**
     * Resolves the HUD panel's current full {@link HudLayout.Layout}: persisted independent
     * position/size if the user has committed a main-panel drag/resize (via {@link #commit}),
     * otherwise today's content-driven default. Content-scale fields (barW, rowH, iconSize,
     * labelScale, scale, etc.) always come from the freshly-computed {@code computed} layout — only
     * panelX/Y/W/H are substituted wholesale from the persisted {@link ComponentState}, mirroring
     * {@code DietScreenEditTarget#resolvedPanelLayout}. Called by both the normal (non-edit) HUD
     * render path and this class's own edit-mode rendering, so a committed resize is reflected
     * consistently everywhere — not just while {@link HudEditTarget} itself is active.
     */
    static HudLayout.Layout resolvedLayout(Minecraft mc, List<String> keys) {
        HudLayout.Layout computed = HudLayout.compute(mc, keys);
        return UiStatePersistence.get().load(PANEL_ID)
                .map(state -> new HudLayout.Layout(
                        state.x(), state.y(), state.width(), state.height(),
                        computed.baseX(), computed.baseY(),
                        computed.barW(), computed.rowH(), computed.iconSize(), computed.maxLabelSw(),
                        computed.scaledPad(), computed.labelScale(), computed.scale(), computed.verticalLayout(),
                        computed.verticalBarW(), computed.verticalBarH(), computed.verticalColumnW()
                ))
                .orElse(computed);
    }

    /** Resolves the HUD panel's current on-screen bounds — see {@link #resolvedLayout}. */
    static Bounds resolvedBounds(Minecraft mc, List<String> keys) {
        HudLayout.Layout layout = resolvedLayout(mc, keys);
        return new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
    }

    private void commit(Bounds bounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            return;
        }
        double derivedScale = Mth.clamp(bounds.width() / (double) baselinePanelW, MIN_SCALE, MAX_SCALE);
        HudLayout.Layout scaled = HudLayout.compute(mc, keys, derivedScale);
        cc.setHudScale(derivedScale);
        cc.setHudOffsetX(bounds.x() - scaled.baseX());
        cc.setHudOffsetY(bounds.y() - scaled.baseY());
        NourishedClientConfig.saveNow();
        UiStatePersistence.get().save(PANEL_ID, new ComponentState(bounds.x(), bounds.y(), bounds.width(), bounds.height(), false));
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
        NutrientPanelContainer panel = new NutrientPanelContainer(keys, matchedLayout, displayValues);
        panel.render(context, bounds);

        Bounds handle = DraggableResizable.handleBounds(bounds);
        context.drawResizeHandle(handle.x(), handle.y(), panelDrag.isHandleHovered(mouse[0], mouse[1], bounds), panelDrag.isHandleActive());
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
     * Rebuilds a full {@link HudLayout.Layout} for the derived scale implied by {@code bounds}'
     * width, but with position AND size pinned to {@code bounds} itself rather than anchor/
     * formula-recomputed — same approach as {@code DietScreenEditTarget#matchedLayoutFor}, so the
     * live preview rectangle and the content drawn inside it always agree, even mid-drag on a
     * height-only edge (previously this used {@code computed.panelW()/panelH()}, the content-formula
     * size at the derived scale, which stays locked to the old height during a height-only resize
     * since the derived scale itself never changes when only height moves).
     */
    private HudLayout.Layout matchedLayoutFor(List<String> keys, Bounds bounds) {
        double derivedScale = Mth.clamp(bounds.width() / (double) baselinePanelW, MIN_SCALE, MAX_SCALE);
        HudLayout.Layout computed = HudLayout.compute(mc, keys, derivedScale);
        return new HudLayout.Layout(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                computed.baseX(), computed.baseY(),
                computed.barW(), computed.rowH(), computed.iconSize(), computed.maxLabelSw(),
                computed.scaledPad(), computed.labelScale(), computed.scale(), computed.verticalLayout(),
                computed.verticalBarW(), computed.verticalBarH(), computed.verticalColumnW()
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
