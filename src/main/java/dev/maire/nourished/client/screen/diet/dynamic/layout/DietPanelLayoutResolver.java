package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import net.minecraft.client.Minecraft;

import java.util.Optional;

/** Resolves the diet panel's committed screen layout and resize constraint from persisted state. */
public final class DietPanelLayoutResolver {

    private DietPanelLayoutResolver() {}

    public static DietLayout.Layout resolvedPanelLayout(Minecraft mc) {
        DietLayout.Layout baseLayout = DietLayout.compute(mc);
        Bounds resolved = resolvePanelBounds(baseLayout);
        return new DietLayout.Layout(
                resolved.x(), resolved.y(), resolved.width(), resolved.height(),
                resolved.x(), resolved.y(),
                baseLayout.scale(), baseLayout.recentMealsScale(), baseLayout.eatMoreScale(),
                persistedLeftMargin()
        );
    }

    /** Only grown/shrunk by left-edge (or bottom-left-corner) gestures — right-edge/corner resizes never touch it. */
    public static int persistedLeftMargin() {
        return DietScreenPersistence.get().load(DietScreenEditTarget.PANEL_ID)
                .map(ComponentState::leftMargin)
                .orElse(0);
    }

    public static AutoGrowPanelContainer.ManualOverride existingManualOverride() {
        return existingManualOverride(DietScreenEditTarget.PANEL_ID);
    }

    public static AutoGrowPanelContainer.ManualOverride existingManualOverride(String componentId) {
        return DietScreenPersistence.get().load(componentId)
                .map(s -> new AutoGrowPanelContainer.ManualOverride(s.widthManual(), s.heightManual()))
                .orElse(AutoGrowPanelContainer.ManualOverride.NONE);
    }

    private static Bounds resolvePanelBounds(DietLayout.Layout baseLayout) {
        Optional<ComponentState> saved = DietScreenPersistence.get().load(DietScreenEditTarget.PANEL_ID);
        AutoGrowPanelContainer.ManualOverride override = saved
                .map(s -> new AutoGrowPanelContainer.ManualOverride(s.widthManual(), s.heightManual()))
                .orElse(AutoGrowPanelContainer.ManualOverride.NONE);

        int naturalWidth = baseLayout.panelW();
        int naturalHeight = naturalHeight(baseLayout);

        int x = saved.map(ComponentState::x).orElse(baseLayout.panelX());
        int y = saved.map(ComponentState::y).orElse(baseLayout.panelY());
        int width = saved.isPresent() ? AutoGrowPanelContainer.resolveWidth(override, saved.get().width(), naturalWidth) : naturalWidth;
        int height = saved.isPresent() ? AutoGrowPanelContainer.resolveHeight(override, saved.get().height(), naturalHeight) : naturalHeight;

        return new Bounds(x, y, width, height);
    }

    static int naturalHeight(DietLayout.Layout baseLayout) {
        int naturalContentEndLocalY = DietScreenModules.naturalContentEndLocalY(baseLayout, DietLeftColumnComponent.computeHeaderEndLocalY());
        int naturalHeightLocal = Math.max(DietLayout.PANEL_MIN_LOCAL_HEIGHT, naturalContentEndLocalY + DietLayout.PANEL_BOTTOM_MARGIN_LOCAL);
        return DietLayout.toScreenDim(baseLayout, naturalHeightLocal);
    }

    /**
     * Width floors at {@link DietSubBoxConstraints#MIN_SIZE_MULTIPLIER} of natural (the same shrink
     * floor every individual sub-box already uses), not the full natural width — a narrower panel just
     * clips its fixed-size right-column content the same way a shorter panel already clips its bottom
     * content (see every sub-box's own "extra room stays empty, less room is clipped" comment); it used
     * to floor at 100% of natural, which made the panel's own corner-resize handle stop shrinking width
     * at all while height could still shrink to its much lower {@link DietLayout#PANEL_MIN_LOCAL_HEIGHT}
     * floor.
     */
    public static Constraint panelConstraint(DietLayout.Layout baseLayout) {
        int naturalWidth = DietLayout.scaledDim(DietLayout.WIDTH, baseLayout.scale()) + baseLayout.leftMargin();
        int minWidth = Math.max(1, (int) Math.round(naturalWidth * DietSubBoxConstraints.MIN_SIZE_MULTIPLIER));
        int maxHeight = Math.max(DietLayout.scaledDim(DietLayout.HEIGHT, 1.5d), naturalHeight(baseLayout));
        return DietSubBoxConstraints.bounded(
                baseLayout.panelW(), baseLayout.panelH(),
                minWidth, DietLayout.scaledDim(DietLayout.PANEL_MIN_LOCAL_HEIGHT, 1.0d),
                Math.max(DietLayout.scaledDim(DietLayout.WIDTH, 1.5d) + baseLayout.leftMargin(),
                        Minecraft.getInstance().getWindow().getGuiScaledWidth() - baseLayout.panelX()), maxHeight
        );
    }

    /**
     * The constraint for a left-edge gesture: identical to {@link #panelConstraint} except width may keep growing
     * leftward until the panel reaches the screen's left edge, so the left side stretches as freely as the right.
     */
    public static Constraint leftEdgeConstraint(DietLayout.Layout baseLayout) {
        Constraint c = panelConstraint(baseLayout);
        int maxWidth = Math.max(c.maxSize().width(), baseLayout.panelX() + baseLayout.panelW());
        return DietSubBoxConstraints.bounded(
                baseLayout.panelW(), baseLayout.panelH(),
                c.minSize().width(), c.minSize().height(),
                maxWidth, c.maxSize().height()
        );
    }

    /** Confines a left-column sub-box to the panel, including the left-edge margin (draggable, not dead space), stopping at the column divider. */
    public static Bounds clampToParent(Bounds child, DietLayout.Layout panelLayout) {
        Bounds parent = new Bounds(panelLayout.panelX(), panelLayout.panelY(), panelLayout.panelW(), panelLayout.panelH());
        int w = Math.min(child.width(), parent.width());
        int h = Math.min(child.height(), parent.height());
        int x = Math.max(parent.x(), Math.min(child.x(), parent.x() + parent.width() - w));
        int y = Math.max(parent.y(), Math.min(child.y(), parent.y() + parent.height() - h));

        int dividerX = DietLayout.columnGeometry(panelLayout, parent).dividerX();
        w = Math.min(w, Math.max(1, dividerX - parent.x()));
        x = Math.max(parent.x(), Math.min(x, dividerX - w));

        return new Bounds(x, y, w, h);
    }

    /**
     * Same as {@link #clampToParent}, but for the right ("Intake Breakdown") column — confines
     * between the column divider and the panel's right edge instead of the panel's left edge and the
     * divider. Without this, the Intake Breakdown header/rows were being run through {@link
     * #clampToParent} in edit mode, which forces a box to stay entirely left of the divider — i.e.
     * squeezed into the left column's space instead of its own, dragging the whole right column over
     * on top of Calories/Balance/etc. the moment edit mode resolved its live bounds.
     */
    public static Bounds clampToRightColumn(Bounds child, DietLayout.Layout panelLayout) {
        Bounds parent = new Bounds(panelLayout.panelX(), panelLayout.panelY(), panelLayout.panelW(), panelLayout.panelH());
        DietLayout.ColumnGeometry geometry = DietLayout.columnGeometry(panelLayout, parent);
        int rightEdge = parent.x() + parent.width();

        int w = Math.min(child.width(), Math.max(1, rightEdge - geometry.rightX()));
        int h = Math.min(child.height(), parent.height());
        int x = Math.max(geometry.rightX(), Math.min(child.x(), rightEdge - w));
        int y = Math.max(parent.y(), Math.min(child.y(), parent.y() + parent.height() - h));

        return new Bounds(x, y, w, h);
    }
}
