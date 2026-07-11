package dev.maire.nourished.client.hud.dynamic.modules;

import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.layout.HorizontalLayout;
import dev.marie.framework.ui.layout.VerticalLayout;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
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

    private final List<MarieComponent> children = new ArrayList<>();
    private final Layout layout;
    private final HudLayout.Layout hudLayout;

    public NutrientPanelContainer(List<String> keys, HudLayout.Layout hudLayout, Map<String, Float> displayValues) {
        this.hudLayout = hudLayout;
        boolean verticalMode = hudLayout.verticalLayout();
        for (String key : keys) {
            children.add(new NutrientBarComponent(key, verticalMode, hudLayout, displayValues));
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
        return "nourished.hud.panel";
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
            context.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), HudDrawHelpers.panelColor(bgOpacity));
        }
        int pad = hudLayout.scaledPad();
        Bounds content = new Bounds(
                bounds.x() + pad,
                bounds.y() + pad,
                Math.max(0, bounds.width() - 2 * pad),
                Math.max(0, bounds.height() - 2 * pad)
        );
        Container.super.render(context, content);
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
