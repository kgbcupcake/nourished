package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditHostRenderer;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Draws one of the Diet Screen's left-column boxes (Calories/Balance/Recent Meals/Eat More/Active
 * Effects) as a Nutritional Panel tab, at its own size, pinned to the tab's top-left. Rebuilt every
 * frame through {@link DietScreenModules#build} so it always shows live data and settings.
 */
public final class SoloModulePane implements EditHostRenderer {

    /** Panel height handed to every build, tall enough that no box's own "does it fit?" check ever trims it. */
    static final int REFERENCE_PANEL_HEIGHT_LOCAL = DietLayout.HEIGHT * 8;

    private final String moduleId;

    public SoloModulePane(String moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public void render(RenderContext context, Bounds area, ModuleEditHost host) {
        List<MarieComponent> built = DietScreenModules.build(referenceLayout(), 0);
        MarieComponent module = find(built, moduleId);
        TabModuleHosts.addTabSnap(host, area);
        Bounds at = TabModuleHosts.track(host, area, module, area.x() + TabModuleHosts.GLOW_ROOM, area.y() + TabModuleHosts.GLOW_ROOM);
        module.render(context, at);
    }

    private static MarieComponent find(List<MarieComponent> built, String id) {
        for (MarieComponent module : built) {
            if (module.id().equals(id)) {
                return module;
            }
        }
        throw new IllegalStateException("No module with id " + id);
    }

    /** The scale-1 layout every tab builds its boxes in, so the dynamic panel's own size never leaks in. */
    static DietLayout.Layout referenceLayout() {
        DietLayout.Layout layout = DietLayout.compute(Minecraft.getInstance(), 1.0d, 0, 0, 1.0d, 1.0d);
        int referencePanelHeight = DietLayout.toScreenDim(layout, REFERENCE_PANEL_HEIGHT_LOCAL);
        return new DietLayout.Layout(layout.panelX(), layout.panelY(), layout.panelW(), referencePanelHeight,
                layout.baseX(), layout.baseY(), layout.scale(), layout.recentMealsScale(), layout.eatMoreScale(), layout.leftMargin());
    }
}
