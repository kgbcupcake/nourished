package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.component.widgets.LegendComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Intake Breakdown column's bottom "Good / Low / Critical" swatch legend — a thin
 * Nourished-specific adapter wrapping the generic marie-ui {@link LegendComponent}, following the
 * same self-positioning module pattern as every other Diet Screen sub-box.
 */
public final class IntakeLegendComponent implements MarieComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.legend";
    public static final int LOCAL_WIDTH = (DietLayout.WIDTH - DietLayout.PAD) - (DietLayout.SPLIT + DietLayout.PAD);
    /** Taller than the classic renderer's fixed {@code 34} box — see the legacy {@code LEGEND_H} javadoc it replaces. */
    public static final int LEGEND_LOCAL_HEIGHT = 38;

    private static int headerTextColor() {
        return MarieColors.resolveColor(NourishedColors.TEXT_HEADER);
    }

    private static int textColor() {
        return MarieColors.resolveColor(NourishedColors.TEXT);
    }

    private static int dividerColor() {
        return MarieColors.resolveColor(NourishedColors.DIVIDER);
    }

    private static int good() {
        return MarieColors.resolveColor(NourishedColors.DIET_LEGEND_GOOD);
    }

    private static int low() {
        return MarieColors.resolveColor(NourishedColors.DIET_LEGEND_LOW);
    }

    private static int critical() {
        return MarieColors.resolveColor(NourishedColors.DIET_LEGEND_CRITICAL);
    }

    private static int panelFillColor() {
        int alpha = Math.max(0, Math.min(255, (int) Math.round(NourishedClientConfig.get().dietBackgroundOpacity() * 255.0d)));
        return (alpha << 24) | (NourishedColors.surfaceRgb() & 0x00FFFFFF);
    }

    private static int borderColor() {
        return MarieColors.resolveColor(NourishedColors.BORDER);
    }

    private final LegendComponent delegate;
    private final Bounds resolvedBounds;
    private final boolean visible;

    IntakeLegendComponent(DietLayout.Layout layout, int startLocalY) {
        int room = DietLayout.roomInPanel(layout, startLocalY, LEGEND_LOCAL_HEIGHT);
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToRightColumn(ID, layout, startLocalY, LOCAL_WIDTH, LEGEND_LOCAL_HEIGHT);

        List<LegendComponent.LegendEntry> entries = List.of(
                new LegendComponent.LegendEntry(IntakeLegendComponent::good,
                        () -> "Good", () -> "40 - 80%", -3, -2),
                new LegendComponent.LegendEntry(IntakeLegendComponent::low,
                        () -> "Low", () -> "25 - 40%", -3, 0),
                new LegendComponent.LegendEntry(IntakeLegendComponent::critical,
                        () -> Component.translatable("nourished.screen.diet.legend_bad").getString(),
                        () -> Component.translatable("nourished.screen.diet.legend_bad_range").getString(), 0, 0)
        );

        this.delegate = new LegendComponent(
                ID,
                DietScreenPersistence.get(),
                resolvedBounds,
                visible,
                LEGEND_LOCAL_HEIGHT,
                () -> Component.translatable("nourished.screen.diet.legend").getString(),
                IntakeLegendComponent::headerTextColor,
                IntakeLegendComponent::textColor,
                IntakeLegendComponent::dividerColor,
                IntakeLegendComponent::panelFillColor,
                IntakeLegendComponent::borderColor,
                entries
        );
    }

    @Override
    public int localHeight() {
        return visible ? LEGEND_LOCAL_HEIGHT : 0;
    }

    public int naturalLocalHeight() {
        return LEGEND_LOCAL_HEIGHT;
    }

    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(resolvedBounds.width(), resolvedBounds.height());
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        delegate.render(context, bounds);
    }
}
