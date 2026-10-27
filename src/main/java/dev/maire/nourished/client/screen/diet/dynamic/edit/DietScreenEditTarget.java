package dev.maire.nourished.client.screen.diet.dynamic.edit;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditCapabilities;
import dev.marie.framework.ui.api.EditGesture;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.api.PanelRelativePlacement;
import dev.marie.framework.ui.api.PlacementStrategy;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.hub.HubPanel;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.edit.HostedEditBox;
import dev.maire.nourished.client.screen.diet.DietEditModeToggle;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietPanelContainer;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietPanelLayoutResolver;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeBarComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeHeaderComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietSettings;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.SUMMARY_BOX_LOCAL_WIDTH;
import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.liveSubBoxConstraint;
import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.naturalPreferredSize;

/**
 * The dynamic Diet Screen's edit mode, on MariesLib's {@link ModuleEditHost}: the panel and every box
 * can be moved and resized, and Move Text/Icons/Bars/Header/All drag a box's content. Nourished only
 * adds what is specific to this panel: the two columns' placement regions, the panel's left margin,
 * and growing the panel to fit a box pulled past its bottom.
 */
public final class DietScreenEditTarget implements MarieComponent {

    private static final String ID = "nourished.diet.editwrapper";
    /** Gap kept between a box pulled past the panel's bottom and the panel's grown bottom edge. */
    private static final int GROW_PADDING_LOCAL = 4;
    public static final String PANEL_ID = DietSettings.PANEL_ID;

    private static final String PANEL_GROUP = "diet.panel";
    private static final String LEFT_GROUP = "diet.left";
    private static final String RIGHT_GROUP = "diet.right";
    private static final String HEADER_SIZE_GROUP = "diet.header";
    private static final String ROW_SIZE_GROUP = "diet.rows";

    private static final EditCapabilities PANEL_CAPS = EditCapabilities.full().withMoveModes().keepContentOnEdgeResize(false);
    private static final EditCapabilities LEFT_CAPS = EditCapabilities.full();
    // Header and rows draw everything straight off their live bounds, so shifting their content on a left/top resize would push it off the card.
    private static final EditCapabilities RIGHT_CAPS = EditCapabilities.full().keepContentOnEdgeResize(false);

    private final Minecraft mc;
    private final Runnable exitEditMode;
    /** The same hub {@link dev.maire.nourished.client.screen.diet.DietScreen} owns, so its sliders stay in sync while edit mode has the screen. */
    private final HubPanel scaleConfigPanel;
    private final BooleanSupplier scaleConfigVisible;
    private final PlacementStrategy leftColumn;
    private final PlacementStrategy rightColumn;
    private final PlacementStrategy panelPlacement = new DietPanelPlacement();
    private final ModuleEditHost host;

    public DietScreenEditTarget(Minecraft mc, Runnable exitEditMode, HubPanel scaleConfigPanel, BooleanSupplier scaleConfigVisible) {
        this.mc = mc;
        this.exitEditMode = exitEditMode;
        this.scaleConfigPanel = scaleConfigPanel;
        this.scaleConfigVisible = scaleConfigVisible;
        this.leftColumn = PanelRelativePlacement.of(() -> columnRegion(false),
                () -> layout().panelX() + layout().leftMargin(), () -> layout().panelY(), () -> layout().scale());
        this.rightColumn = PanelRelativePlacement.of(() -> columnRegion(true),
                () -> DietLayout.rightColumnContentX(layout()), () -> layout().panelY(), () -> layout().scale());
        this.host = ModuleEditHost.builder(DietScreenPersistence.get(), leftColumn)
                .outlineColor(() -> MarieColors.resolveColor(NourishedColors.EDIT_OUTLINE))
                .sizeLabelColors(() -> MarieColors.resolveColor(NourishedColors.EDIT_LABEL_TEXT),
                        () -> MarieColors.resolveColor(NourishedColors.EDIT_LABEL_SHADOW))
                // Before a box saves: its placement clamps against the saved panel, so the panel must already hold it.
                .beforeCommit(this::growPanelToFit)
                .build();
    }

    public static DietLayout.Layout resolvedPanelLayout(Minecraft mc) {
        return DietPanelLayoutResolver.resolvedPanelLayout(mc);
    }

    private DietLayout.Layout layout() {
        return resolvedPanelLayout(mc);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        DietLayout.Layout layout = layout();
        if (DietEditModeToggle.forPanel(() -> layout, () -> true, exitEditMode).mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return host.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return host.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible.getAsBoolean() && scaleConfigPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return host.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        DietLayout.Layout layout = layout();
        int[] mouse = scaledMouse(mc);

        Bounds panelPreview = host.previewBounds(PANEL_ID);
        // Snap against the panel as it was before growing to fit the box being dragged — snapping
        // to the grown bottom edge would pull the box down, grow the panel again, and creep forever.
        Bounds snapPanel = panelPreview != null ? panelPreview : panelBounds(layout);
        Bounds panelBounds = growToFitActiveBox(snapPanel, layout);
        DietLayout.Layout matched = matchedLayoutFor(panelBounds, layout);

        TrackingData data = mc.player != null ? MarieClientCache.get() : null;
        Map<String, Float> displayValues = data != null ? data.values : Map.of();
        host.livePreviews().forEach((id, preview) -> {
            if (!PANEL_ID.equals(id)) {
                DietScreenPersistence.setLiveOverride(id, preview);
            }
        });
        DietPanelContainer panel;
        try {
            panel = new DietPanelContainer(data, DietSettings.barOrder(), displayValues, matched);
        } finally {
            DietScreenPersistence.clearLiveOverrides();
        }

        host.beginFrame();
        host.addSnapTarget(LEFT_GROUP, snapPanel);
        host.addSnapTarget(RIGHT_GROUP, snapPanel);
        host.track(new HostedEditBox(PANEL_ID, panelBounds, panelConstraint(layout), PANEL_CAPS, PANEL_GROUP, PANEL_GROUP, panelPlacement), panelBounds);

        // Tracked bottom-to-top: the host hands a click to the last-tracked box under the pointer first.
        List<IntakeBarComponent> intakeBars = panel.intakeBarComponents();
        Map<String, Bounds> barBounds = new HashMap<>();
        for (int i = intakeBars.size() - 1; i >= 0; i--) {
            IntakeBarComponent bar = intakeBars.get(i);
            barBounds.put(bar.id(), trackRight(bar.id(), bar.resolvedBounds(), bar.isVisible(),
                    liveSubBoxConstraint(naturalPreferredSize(matched, bar.naturalLocalHeight(), IntakeBarComponent.LOCAL_WIDTH)), ROW_SIZE_GROUP, matched));
        }
        IntakeHeaderComponent header = panel.intakeHeaderComponent();
        Bounds headerAt = trackRight(header.id(), header.resolvedBounds(), header.isVisible(),
                liveSubBoxConstraint(naturalPreferredSize(matched, header.naturalLocalHeight(), IntakeHeaderComponent.LOCAL_WIDTH)), HEADER_SIZE_GROUP, matched);

        ActiveEffectsComponent effects = panel.activeEffectsComponent();
        Bounds effectsAt = trackLeft(effects.id(), effects.resolvedBounds(), effects.isVisible(),
                liveSubBoxConstraint(naturalPreferredSize(matched, effects.naturalLocalHeight())), matched);
        EatMoreComponent eatMore = panel.eatMoreComponent();
        Bounds eatMoreAt = trackLeft(eatMore.id(), eatMore.resolvedBounds(), eatMore.isVisible(),
                liveSubBoxConstraint(naturalPreferredSize(matched, eatMore.naturalLocalHeight())), matched);
        RecentMealsComponent recent = panel.recentMealsComponent();
        Bounds recentAt = trackLeft(recent.id(), recent.resolvedBounds(), recent.isVisible(),
                liveSubBoxConstraint(naturalPreferredSize(matched, recent.naturalLocalHeight())), matched);
        BalanceComponent balance = panel.balanceComponent();
        Bounds balanceAt = trackLeft(balance.id(), balance.resolvedBounds(), balance.isVisible(),
                liveSubBoxConstraint(naturalPreferredSize(matched, balance.naturalLocalHeight(), SUMMARY_BOX_LOCAL_WIDTH)), matched);
        CaloriesComponent calories = panel.caloriesComponent();
        Bounds caloriesAt = trackLeft(calories.id(), calories.resolvedBounds(), calories.isVisible(),
                liveSubBoxConstraint(naturalPreferredSize(matched, calories.naturalLocalHeight(), SUMMARY_BOX_LOCAL_WIDTH)), matched);

        panel.setSubBoxRenderBounds(caloriesAt, balanceAt, recentAt, eatMoreAt, effectsAt);
        panel.setIntakeRenderBounds(headerAt, barBounds);
        panel.render(MarieModuleSettings.withBrightness(context, DietSettings.textBrightness(), DietSettings.iconBrightness()), panelBounds);

        host.renderOverlay(context, mouse[0], mouse[1]);
        DietEditModeToggle.forPanel(() -> matched, () -> true, exitEditMode).render(context, mouse[0], mouse[1]);

        if (scaleConfigVisible.getAsBoolean()) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
    }

    private Bounds trackLeft(String id, Bounds resolved, boolean visible, Constraint resize, DietLayout.Layout matched) {
        Bounds drawn = DietPanelLayoutResolver.clampToParent(resolved, matched);
        if (!visible) {
            return drawn;
        }
        Bounds at = host.track(new HostedEditBox(id, drawn, resize, LEFT_CAPS, LEFT_GROUP, LEFT_GROUP, leftColumn), drawn);
        return DietPanelLayoutResolver.clampToParent(at, matched);
    }

    private Bounds trackRight(String id, Bounds resolved, boolean visible, Constraint resize, String sizeGroup, DietLayout.Layout matched) {
        Bounds drawn = DietPanelLayoutResolver.clampToRightColumn(resolved, matched);
        if (!visible) {
            return drawn;
        }
        Bounds at = host.track(new HostedEditBox(id, drawn, resize, RIGHT_CAPS, RIGHT_GROUP, sizeGroup, rightColumn), drawn);
        return DietPanelLayoutResolver.clampToRightColumn(at, matched);
    }

    /** The panel's resize clamp; a left-edge gesture zeroes the margin so the same drag can shrink it back down. */
    private Constraint panelConstraint(DietLayout.Layout layout) {
        if (!isPanelLeftEdgeResize(host.activeGesture())) {
            return DietPanelLayoutResolver.panelConstraint(layout);
        }
        DietLayout.Layout noMargin = new DietLayout.Layout(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(),
                layout.baseX(), layout.baseY(), layout.scale(), layout.recentMealsScale(), layout.eatMoreScale(), 0);
        return DietPanelLayoutResolver.leftEdgeConstraint(noMargin);
    }

    private static boolean isPanelLeftEdgeResize(EditGesture gesture) {
        return gesture != null && PANEL_ID.equals(gesture.moduleId())
                && gesture.kind() == EditGesture.Kind.RESIZE && gesture.leftEdge();
    }

    /**
     * Where a column's boxes may go while dragged: across their own column, and from the panel's top
     * all the way to the screen's bottom — the panel grows to hold a box pulled past it.
     */
    private Bounds columnRegion(boolean right) {
        DietLayout.Layout layout = layout();
        Bounds panel = panelBounds(layout);
        DietLayout.ColumnGeometry geometry = DietLayout.columnGeometry(layout, panel);
        int height = Math.max(panel.height(), mc.getWindow().getGuiScaledHeight() - panel.y());
        if (right) {
            return new Bounds(geometry.rightX(), panel.y(), Math.max(1, panel.x() + panel.width() - geometry.rightX()), height);
        }
        return new Bounds(panel.x(), panel.y(), Math.max(1, geometry.dividerX() - panel.x()), height);
    }

    /** {@code panel} stretched down so whichever box is mid-gesture fits inside with a little room, up to the screen's bottom. */
    private Bounds growToFitActiveBox(Bounds panel, DietLayout.Layout layout) {
        int bottom = panel.y() + panel.height();
        int padding = DietLayout.toScreenDim(layout, GROW_PADDING_LOCAL);
        for (Map.Entry<String, Bounds> entry : host.livePreviews().entrySet()) {
            if (!PANEL_ID.equals(entry.getKey())) {
                Bounds preview = entry.getValue();
                bottom = Math.max(bottom, preview.y() + preview.height() + padding);
            }
        }
        bottom = Math.max(panel.y() + panel.height(), Math.min(bottom, mc.getWindow().getGuiScaledHeight()));
        return bottom == panel.y() + panel.height() ? panel : new Bounds(panel.x(), panel.y(), panel.width(), bottom - panel.y());
    }

    /** Saves the panel taller (as a manual height) when the box being released needs more room than it has. */
    private void growPanelToFit(String id, Bounds committed) {
        if (PANEL_ID.equals(id)) {
            return;
        }
        DietLayout.Layout layout = layout();
        Bounds current = panelBounds(layout);
        int padding = DietLayout.toScreenDim(layout, GROW_PADDING_LOCAL);
        int bottom = Math.min(committed.y() + committed.height() + padding, mc.getWindow().getGuiScaledHeight());
        if (bottom <= current.y() + current.height()) {
            return;
        }
        var store = DietScreenPersistence.get();
        ComponentState base = store.load(PANEL_ID)
                .orElseGet(() -> new ComponentState(current.x(), current.y(), current.width(), current.height(), false, false, false, 0));
        store.save(PANEL_ID, new ComponentState(base.x(), base.y(), base.width(), bottom - current.y(),
                false, base.widthManual(), true, base.leftMargin(), base.contentScale(), base.paddingScale()));
    }

    /** The panel's layout at {@code panelBounds}; only a live left-edge resize changes its left margin, by the width it has gained. */
    private DietLayout.Layout matchedLayoutFor(Bounds panelBounds, DietLayout.Layout layout) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        EditGesture gesture = host.activeGesture();
        int leftMargin = isPanelLeftEdgeResize(gesture)
                ? Math.max(0, DietPanelLayoutResolver.persistedLeftMargin() + (panelBounds.width() - gesture.start().width()))
                : DietPanelLayoutResolver.persistedLeftMargin();
        return new DietLayout.Layout(panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(),
                panelBounds.x(), panelBounds.y(), cc.dietScale(), cc.recentMealsBoxScale(), cc.eatMoreBoxScale(), leftMargin);
    }

    private static Bounds panelBounds(DietLayout.Layout layout) {
        return new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
