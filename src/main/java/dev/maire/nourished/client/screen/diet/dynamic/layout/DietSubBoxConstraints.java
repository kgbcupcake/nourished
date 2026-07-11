package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.geometry.Size;

/** Resize-handle constraints for the left column's draggable sub-boxes. */
public final class DietSubBoxConstraints {

    /** Calories/Balance render 4 local units wider than the other sub-boxes. */
    public static final int SUMMARY_BOX_LOCAL_WIDTH = (DietLayout.SPLIT - DietLayout.PAD * 2) + 4;

    private static final double MIN_SIZE_MULTIPLIER = 0.6d;
    private static final double MAX_WIDTH_MULTIPLIER = 1.5d;
    private static final double MAX_HEIGHT_MULTIPLIER = 4.0d;

    private DietSubBoxConstraints() {}

    public static Constraint liveSubBoxConstraint(Size pref) {
        return bounded(
                pref.width(), pref.height(),
                (int) Math.round(pref.width() * MIN_SIZE_MULTIPLIER), (int) Math.round(pref.height() * MIN_SIZE_MULTIPLIER),
                (int) Math.round(pref.width() * MAX_WIDTH_MULTIPLIER), (int) Math.round(pref.height() * MAX_HEIGHT_MULTIPLIER)
        );
    }

    public static Size naturalPreferredSize(DietLayout.Layout layout, int naturalLocalHeight) {
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        return naturalPreferredSize(layout, naturalLocalHeight, bw);
    }

    public static Size naturalPreferredSize(DietLayout.Layout layout, int naturalLocalHeight, int localWidth) {
        return new Size(DietLayout.toScreenDim(layout, localWidth), DietLayout.toScreenDim(layout, naturalLocalHeight));
    }

    static Constraint bounded(int prefW, int prefH, int minW, int minH, int maxW, int maxH) {
        return new Constraint(
                new Size(prefW, prefH),
                new Size(minW, minH),
                new Size(maxW, maxH),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
    }
}
