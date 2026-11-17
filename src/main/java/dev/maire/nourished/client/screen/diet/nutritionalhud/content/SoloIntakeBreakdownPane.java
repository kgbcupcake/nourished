package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditHostRenderer;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;

import java.util.List;

/**
 * Draws the whole Intake Breakdown column (header plus one row per nutrient) as one Nutritional Panel
 * tab: rows start stacked in order, and each can be moved and resized on its own. Mouse-wheel scrolls
 * the whole stack when the tab is too short to show every row (see {@link ScrollableTabPane}, which
 * feeds {@link #scrollState()} from outside the window this renderer only sees per-frame).
 */
public final class SoloIntakeBreakdownPane implements EditHostRenderer {

    public static final String ID = "nourished.diet.hub.intake";

    private final ScrollState scroll = new ScrollState();

    /** Shared with the {@link ScrollableTabPane} wrapping this tab's content, so wheel input here moves these rows. */
    public ScrollState scrollState() {
        return scroll;
    }

    @Override
    public void render(RenderContext context, Bounds area, ModuleEditHost host) {
        DietLayout.Layout layout = SoloModulePane.referenceLayout();
        List<MarieComponent> items = DietScreenModules.build(DietScreenModules.RIGHT_COLUMN_KEY, layout, 0,
                DietLayout.rightColumnContentX(layout));
        TabModuleHosts.addTabSnap(host, area);

        // Each row's default spot is stacked under the previous row's default, shifted up by the
        // current scroll offset; a moved or resized row keeps its own place relative to that, so rows
        // can be spaced apart freely and still scroll together. The step between rows (and the content
        // height #setMaxOffset below needs) is each row's own GROWN height, not its fixed natural size
        // or its tracked box alone: a row's own "Text size"/"Row size" slider (see DietScreen's
        // "Intake" group options) scales how tall it actually draws independently of that box, and
        // undercounting that both let rows overlap instead of stacking and left the scrollbar unable
        // to reach the last ones.
        int homeY = area.y() + TabModuleHosts.GLOW_ROOM - scroll.offset();
        int contentHeight = 0;
        for (MarieComponent item : items) {
            int grownHeight = grownRowHeight(item);
            Bounds at = TabModuleHosts.track(host, area, item, area.x() + TabModuleHosts.GLOW_ROOM, homeY);
            Bounds drawAt = at.height() >= grownHeight ? at : new Bounds(at.x(), at.y(), at.width(), grownHeight);
            item.render(context, drawAt);
            int rowHeight = Math.max(at.height(), grownHeight);
            homeY += rowHeight;
            contentHeight += rowHeight;
        }
        int viewportHeight = Math.max(0, area.height() - TabModuleHosts.GLOW_ROOM * 2);
        // A small safety margin on top: padding/spacing this estimate still doesn't account for is
        // better covered by a little extra, harmless blank space at the bottom than by coming up short
        // of the last row again.
        int safetyMargin = 32;
        scroll.setMaxOffset(Math.max(0, contentHeight - viewportHeight) + (contentHeight > viewportHeight ? safetyMargin : 0));
    }

    /** {@code item}'s natural height scaled by its own persisted content scale, if any — see the render() javadoc above. */
    private static int grownRowHeight(MarieComponent item) {
        int natural = TabModuleHosts.naturalSize(item).height();
        double scale = ContentScaleController.resolveContentScale(DietScreenPersistence.contentScale(item.id()));
        return Math.max(natural, (int) Math.round(natural * scale));
    }
}
