package dev.maire.nourished.client.hud;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.Anchor;
import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.Constraint;
import dev.marie.framework.ui.DraggableResizable;
import dev.marie.framework.ui.Insets;
import dev.marie.framework.ui.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Size;
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
 * <p>Position/size are backed directly by {@link NourishedClientConfig}'s existing
 * {@code hudOffsetX}/{@code hudOffsetY}/{@code hudScale} fields — the same ones the normal
 * (non-edit) HUD render path already reads via {@code HudLayout.compute} — rather than a
 * separate keyed persistence store, so there is exactly one source of truth for HUD position.
 */
final class HudEditTarget implements MarieComponent {

    private static final String ID = "nourished.hud.editwrapper";

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

    /** Resolves the HUD panel's current on-screen bounds: persisted offsets/scale, same as the normal render path. */
    static Bounds resolvedBounds(Minecraft mc, List<String> keys) {
        HudLayout.Layout layout = HudLayout.compute(mc, keys);
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
     * width, but with position pinned to {@code bounds} itself rather than anchor-recomputed — same
     * approach as {@code DietScreenEditTarget#matchedLayoutFor}, so the live preview rectangle and
     * the content drawn inside it always agree, even mid-drag.
     */
    private HudLayout.Layout matchedLayoutFor(List<String> keys, Bounds bounds) {
        double derivedScale = Mth.clamp(bounds.width() / (double) baselinePanelW, MIN_SCALE, MAX_SCALE);
        HudLayout.Layout computed = HudLayout.compute(mc, keys, derivedScale);
        return new HudLayout.Layout(
                bounds.x(), bounds.y(), computed.panelW(), computed.panelH(),
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
