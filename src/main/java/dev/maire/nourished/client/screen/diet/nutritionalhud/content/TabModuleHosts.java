package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.api.EditCapabilities;
import dev.marie.framework.ui.api.ResizeLimits;
import dev.maire.nourished.client.edit.HostedEditBox;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.api.PinnedPlacement;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.geometry.Size;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeBarComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeHeaderComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;

import java.util.function.BooleanSupplier;

import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.SUMMARY_BOX_LOCAL_WIDTH;

/** Edit hosts for the Nutritional Panel's tabs: every box can be moved, resized from any edge, and have its content moved. */
public final class TabModuleHosts {

    /** Gap kept between a box and the tab's top-left edge, so the box's glow (4px rings) is never clipped there. */
    public static final int GLOW_ROOM = 6;

    private static final String TAB_GROUP = "nutritionalhud.tab";
    private static final EditCapabilities BOX_CAPS = EditCapabilities.full();
    // Intake rows draw straight off their live bounds, so shifting their content on a left/top resize would push it off the card.
    private static final EditCapabilities ROW_CAPS = EditCapabilities.full().keepContentOnEdgeResize(false);

    private TabModuleHosts() {}

    /** A host saving into the current profile's store; build it inside the Nutritional Panel's scope. */
    public static ModuleEditHost newHost(BooleanSupplier editing) {
        return ModuleEditHost.builder(DietScreenPersistence.profile(), PinnedPlacement.of())
                .activeWhen(editing)
                .outlineBoxes(true)
                .outlineColor(() -> MarieColors.resolveColor(NourishedColors.EDIT_OUTLINE))
                .build();
    }

    /** The tab's own edges, for boxes to snap to; call once per frame before tracking its boxes. */
    public static void addTabSnap(ModuleEditHost host, Bounds area) {
        host.addSnapTarget(TAB_GROUP, region(area));
    }

    /**
     * Places {@code module} in its tab — its saved offset from {@code (homeX, homeY)} and its saved size,
     * else its natural size there — and registers it with {@code host}; returns where to draw it.
     */
    public static Bounds track(ModuleEditHost host, Bounds area, MarieComponent module, int homeX, int homeY) {
        Size natural = naturalSize(module);
        Bounds home = new Bounds(homeX, homeY, natural.width(), natural.height());
        TabPlacement placement = new TabPlacement(home, region(area));
        Bounds placed = placement.resolve(host.store(), module.id(), home);
        EditCapabilities caps = module instanceof IntakeBarComponent || module instanceof IntakeHeaderComponent ? ROW_CAPS : BOX_CAPS;
        return host.track(new HostedEditBox(module.id(), placed, ResizeLimits.DEFAULT.constraintFor(natural), caps,
                TAB_GROUP, TAB_GROUP, placement), placed);
    }

    private static Bounds region(Bounds area) {
        return new Bounds(area.x() + GLOW_ROOM, area.y() + GLOW_ROOM,
                Math.max(1, area.width() - 2 * GLOW_ROOM), Math.max(1, area.height() - 2 * GLOW_ROOM));
    }

    /** A box's untouched size at scale 1.0, the base its resize min/max are measured from (same as the dynamic screen's). */
    public static Size naturalSize(MarieComponent module) {
        Bounds fallback = ((SelfPositioningModule) module).resolvedBounds();
        int columnWidth = DietLayout.SPLIT - DietLayout.PAD * 2;
        return switch (module) {
            case CaloriesComponent c -> new Size(SUMMARY_BOX_LOCAL_WIDTH, c.naturalLocalHeight());
            case BalanceComponent b -> new Size(SUMMARY_BOX_LOCAL_WIDTH, b.naturalLocalHeight());
            case RecentMealsComponent r -> new Size(columnWidth, r.naturalLocalHeight());
            case EatMoreComponent e -> new Size(columnWidth, e.naturalLocalHeight());
            case ActiveEffectsComponent a -> new Size(columnWidth, a.naturalLocalHeight());
            case IntakeHeaderComponent h -> new Size(IntakeHeaderComponent.LOCAL_WIDTH, h.naturalLocalHeight());
            case IntakeBarComponent bar -> new Size(IntakeBarComponent.LOCAL_WIDTH, bar.naturalLocalHeight());
            default -> new Size(fallback.width(), fallback.height());
        };
    }
}
