package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.component.widgets.TitleBarComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import net.minecraft.network.chat.Component;

/**
 * The Intake Breakdown panel's "✧✧ Intake Breakdown ✧✧" title block — a thin
 * Nourished-specific adapter wrapping the generic marie-ui {@link TitleBarComponent}, following the
 * same self-positioning module pattern as {@link CaloriesComponent}/{@link BalanceComponent} for the
 * left column, but chained into the right column's registry key instead (see
 * {@link DietScreenModules#registerAll}).
 */
public final class IntakeHeaderComponent implements MarieComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.intake.header";
    /** Local height of the title + divider block, matching the classic hand-drawn header's y=30..44 span. */
    public static final int HEADER_LOCAL_HEIGHT = 14;
    public static final int LOCAL_WIDTH = DietLayout.WIDTH - DietLayout.SPLIT - DietLayout.PAD * 2;

    private static int headerTextColor() {
        return MarieColors.resolveColor(NourishedColors.TEXT_HEADER);
    }

    private static int dividerColor() {
        return MarieColors.resolveColor(NourishedColors.DIVIDER);
    }

    private final TitleBarComponent delegate;
    private final Bounds resolvedBounds;
    private final int localHeight;

    IntakeHeaderComponent(DietLayout.Layout layout, int startLocalY) {
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToRightColumn(ID, layout, startLocalY, LOCAL_WIDTH, HEADER_LOCAL_HEIGHT);
        this.localHeight = HEADER_LOCAL_HEIGHT;
        this.delegate = new TitleBarComponent(
                ID,
                DietScreenPersistence.get(),
                resolvedBounds,
                true,
                HEADER_LOCAL_HEIGHT,
                layout.scale(),
                () -> Component.translatable("nourished.screen.diet.intake").getString(),
                () -> "✧✧",
                () -> "✧✧",
                IntakeHeaderComponent::headerTextColor,
                IntakeHeaderComponent::dividerColor
        );
    }

    @Override
    public int localHeight() {
        return localHeight;
    }

    public int naturalLocalHeight() {
        return HEADER_LOCAL_HEIGHT;
    }

    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

    public boolean isVisible() {
        return true;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        return delegate.constraint();
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        delegate.render(context, bounds);
    }
}
