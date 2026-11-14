package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditHostRenderer;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.geometry.Bounds;

/**
 * Hosts {@link HomeBoxPane} as a Nutritional Panel tab exactly like {@link SoloModulePane} hosts
 * Calories/Balance/etc: draggable and resizable, pinned to the tab's top-left by default, floored by
 * {@code ResizeLimits.DEFAULT} so it can't be shrunk smaller than the player preview needs.
 */
public final class HomeTabRenderer implements EditHostRenderer {

    private final HomeBoxPane content = new HomeBoxPane();

    @Override
    public void render(RenderContext context, Bounds area, ModuleEditHost host) {
        TabModuleHosts.addTabSnap(host, area);
        Bounds at = TabModuleHosts.track(host, area, content, area.x() + TabModuleHosts.GLOW_ROOM, area.y() + TabModuleHosts.GLOW_ROOM);
        content.render(context, at);
    }
}
