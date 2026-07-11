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

    /** Width floors at natural + the current left margin, so the right column's fixed-size content never clips even with an existing margin; height keeps a much lower floor for the minimize-to-title-bar state. */
    public static Constraint panelConstraint(DietLayout.Layout baseLayout) {
        int naturalWidth = DietLayout.scaledDim(DietLayout.WIDTH, baseLayout.scale()) + baseLayout.leftMargin();
        int maxHeight = Math.max(DietLayout.scaledDim(DietLayout.HEIGHT, 1.5d), naturalHeight(baseLayout));
        return DietSubBoxConstraints.bounded(
                baseLayout.panelW(), baseLayout.panelH(),
                naturalWidth, DietLayout.scaledDim(DietLayout.PANEL_MIN_LOCAL_HEIGHT, 1.0d),
                DietLayout.scaledDim(DietLayout.WIDTH, 1.5d) + baseLayout.leftMargin(), maxHeight
        );
    }

    /** Confines a sub-box to the panel, including the left-edge margin (draggable, not dead space), stopping at the column divider. */
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
}
