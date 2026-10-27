package dev.maire.nourished.client.screen.diet;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.api.EditModeToggle;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** MariesLib's edit-mode switch in Nourished's own (recolorable) toggle colors, for the Diet Screens. */
public final class DietEditModeToggle {

    private DietEditModeToggle() {}

    public static EditModeToggle.Builder builder(Supplier<Bounds> window, BooleanSupplier on, Runnable onToggle) {
        return EditModeToggle.builder(window, on, onToggle)
                .lightColors(() -> MarieColors.resolveColor(NourishedColors.TOGGLE_ON),
                        () -> MarieColors.resolveColor(NourishedColors.TOGGLE_OFF))
                .bodyColors(() -> MarieColors.resolveColor(NourishedColors.TOGGLE_HOUSING),
                        () -> MarieColors.resolveColor(NourishedColors.TOGGLE_LEVER),
                        () -> MarieColors.resolveColor(NourishedColors.TOGGLE_BORDER));
    }

    /** The dynamic panel's switch: top-right of the panel, scaled with it. */
    public static EditModeToggle forPanel(Supplier<DietLayout.Layout> layout, BooleanSupplier on, Runnable onToggle) {
        return builder(() -> panelBounds(layout.get()), on, onToggle)
                .scale(() -> layout.get().scale())
                .build();
    }

    private static Bounds panelBounds(DietLayout.Layout layout) {
        return new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
    }
}
