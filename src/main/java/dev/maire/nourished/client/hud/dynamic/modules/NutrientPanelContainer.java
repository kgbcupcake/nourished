package dev.maire.nourished.client.hud.dynamic.modules;

import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.layout.HorizontalLayout;
import dev.marie.framework.ui.layout.VerticalLayout;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.hud.dynamic.edit.HudEditTarget;
import dev.maire.nourished.client.hud.dynamic.layout.HudLayout;
import dev.maire.nourished.config.NourishedClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * First MarieUI consumer for the nutrient HUD panel as a whole: one {@link NutrientBarComponent}
 * per visible nutrient key, arranged by whichever layout {@code cc.hudVerticalLayout()} currently
 * selects. "Vertical" bars (columns) are arranged left-to-right, so that mode uses
 * {@link HorizontalLayout}; "horizontal" bars (rows) stack top-to-bottom via {@link VerticalLayout} —
 * same crossed naming as {@link HudLayout}'s own vertical/horizontal terminology.
 */
public final class NutrientPanelContainer implements Container {

    /** Reference local padding value the user's persisted paddingScale multiplies, analogous to {@code CalorieHudScreen}'s own {@code PADDING} constant — reuses {@link HudDrawHelpers#PANEL_PAD}, the same base value {@link HudLayout#compute} derives its own (box-geometry-only) {@code scaledPad} from. */
    private static final int BASE_PAD = HudDrawHelpers.PANEL_PAD;

    /** The HUD panel's persisted UI-state key (same string {@code HudEditTarget} keys its scale/position under). */
    private static final String PANEL_ID = "nourished.hud.panel";

    private final List<MarieComponent> children = new ArrayList<>();
    private final Layout layout;
    private final HudLayout.Layout hudLayout;

    public NutrientPanelContainer(List<String> keys, HudLayout.Layout hudLayout, Map<String, Float> displayValues) {
        this.hudLayout = hudLayout;
        boolean verticalMode = hudLayout.verticalLayout();
        // Text/icon render scale is the user's persisted per-panel adjustment alone — hudLayout's own
        // scale (cc.hudScale()) stays in use for row/column geometry (rowH, iconSize positions, barW,
        // panel natural size) below and in HudLayout itself, same separation the other 7
        // ContentScaleController-managed modules maintain.
        float contentScale = (float) ContentScaleController.resolveContentScale(HudEditTarget.persistedContentScale());
        float iconScale = (float) ContentScaleController.resolveContentScale(MarieModuleSettings.iconScale(UiStatePersistence.get(), PANEL_ID));
        float barScale = (float) ContentScaleController.resolveContentScale(MarieModuleSettings.barScale(UiStatePersistence.get(), PANEL_ID));
        for (String key : keys) {
            children.add(new NutrientBarComponent(key, verticalMode, hudLayout, displayValues, contentScale, iconScale,
                    hudLayout.contentOffsetX(), hudLayout.contentOffsetY(),
                    MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID),
                    MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID),
                    barScale, MarieModuleSettings.isIconsHidden(UiStatePersistence.get(), PANEL_ID),
                    MarieModuleSettings.isBarsHidden(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.isTextHidden(UiStatePersistence.get(), PANEL_ID)));
        }
        if (verticalMode) {
            int columnGap = Math.max(2, (int) Math.round(HudDrawHelpers.VERTICAL_COLUMN_GAP * hudLayout.scale()));
            this.layout = new HorizontalLayout(columnGap);
        } else {
            this.layout = new VerticalLayout(HudDrawHelpers.ROW_GAP);
        }
    }

    @Override
    public String id() {
        return PANEL_ID;
    }

    @Override
    public List<MarieComponent> children() {
        return children;
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
        return layout;
    }

    @Override
    public Constraint constraint() {
        Anchor anchor = mapAnchor(NourishedClientConfig.get().hudAnchor());
        // Offsets (hudOffsetX/Y) aren't encoded into margin here: this is the tree's root, and
        // MarieUI has no root/screen anchoring resolver yet — the wiring code in NourishedHUD
        // resolves the panel's actual screen Bounds itself, reusing HudLayout's existing
        // anchor+offset math so this render path stays pixel-identical to the legacy one.
        return Constraint.fixed(hudLayout.panelW(), hudLayout.panelH()).withAnchor(anchor);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        double bgOpacity = cc.hudBackgroundOpacity();
        if (bgOpacity > 0d) {
            // Same single-color look as the classic renderer's HudDrawHelpers#drawRoundedRect (fill
            // and border matching) — this just swaps the square fillRect for RenderContext's built-in
            // rounded-rect primitive so the panel gets its notched corners back. At the default shade
            // (0) and border opacity (1.0) the border is the panel's own color, so the panel looks as
            // it always did; the Style sliders tint the fill and border from there.
            int panelRgb = MarieColors.shade(NourishedColors.rgb(NourishedColors.HUD_PANEL), cc.hudBackgroundShade());
            int panelColor = HudDrawHelpers.panelColorWithOpacity(panelRgb, bgOpacity);
            int borderRgb = MarieColors.shade(NourishedColors.rgb(NourishedColors.HUD_BORDER), cc.hudBorderShade());
            int borderColor = HudDrawHelpers.panelColorWithOpacity(borderRgb, bgOpacity * cc.hudBorderOpacity());
            context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, HudDrawHelpers.PANEL_CORNER_RADIUS, panelColor, borderColor);
        }
        // Content position offset is the user's persisted padding adjustment alone — hudLayout's own
        // scaledPad (box geometry, used for this panel's natural size in HudLayout#compute) plays no
        // part in it, same separation as contentScale above.
        int pad = Math.round(ContentScaleController.resolvePadding(BASE_PAD * HudEditTarget.persistedPaddingScale()));
        int leftMargin = hudLayout.leftMargin();
        int availableW = Math.max(0, bounds.width() - 2 * pad - leftMargin);
        int availableH = Math.max(0, bounds.height() - 2 * pad);

        // Content is pinned to the box's top-left plus padding, so it moves with the box on resize,
        // like the Calorie History and Activity Log boxes.
        Bounds content = new Bounds(bounds.x() + pad + leftMargin, bounds.y() + pad, availableW, availableH);
        // Clipped to the panel's own bounds — a defensive backstop against contentOffsetX/Y (the
        // "Move Text and Icons" toggle) pushing content outside the panel: HudEditTarget clamps that
        // offset already, but without this, any drift (e.g. from a stale offset the clamp hasn't
        // re-run against yet) would render content fully detached from the panel background rather
        // than simply getting cut off at its edge, same as CalorieHudScreen/ActivityLogHudPanel's
        // own pushClip/popClip around their row drawing.
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            Container.super.render(context, content);
            HudEditTarget.drawScrollIndicator(context, bounds);
        } finally {
            context.popClip();
        }
    }

    private static Anchor mapAnchor(dev.marie.framework.config.HudAnchor hudAnchor) {
        return switch (hudAnchor) {
            case TOP_LEFT -> Anchor.TOP_LEFT;
            case TOP_RIGHT -> Anchor.TOP_RIGHT;
            case BOTTOM_LEFT -> Anchor.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> Anchor.BOTTOM_RIGHT;
        };
    }
}
