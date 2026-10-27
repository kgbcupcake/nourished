package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditHostRenderer;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;

import java.util.List;

/**
 * Draws the whole Intake Breakdown column (header plus one row per nutrient) as one Nutritional Panel
 * tab: rows start stacked in order, and each can be moved and resized on its own.
 */
public final class SoloIntakeBreakdownPane implements EditHostRenderer {

    public static final String ID = "nourished.diet.hub.intake";

    @Override
    public void render(RenderContext context, Bounds area, ModuleEditHost host) {
        DietLayout.Layout layout = SoloModulePane.referenceLayout();
        List<MarieComponent> items = DietScreenModules.build(DietScreenModules.RIGHT_COLUMN_KEY, layout, 0,
                DietLayout.rightColumnContentX(layout));
        TabModuleHosts.addTabSnap(host, area);
        // Each row's default spot is stacked under the previous row's default; a moved or resized row
        // keeps its own place, so rows can be spaced apart freely.
        int homeY = area.y() + TabModuleHosts.GLOW_ROOM;
        for (MarieComponent item : items) {
            Bounds at = TabModuleHosts.track(host, area, item, area.x() + TabModuleHosts.GLOW_ROOM, homeY);
            item.render(context, at);
            homeY += TabModuleHosts.naturalSize(item).height();
        }
    }
}
