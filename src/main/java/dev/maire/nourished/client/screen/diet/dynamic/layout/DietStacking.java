package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.geometry.Bounds;

/**
 * The Diet left column's stacking rule, kept free of any Minecraft dependency so it can be unit-tested
 * (see {@code DietLayoutMathTest}) and reused by whatever hosts the column.
 */
public final class DietStacking {

    private DietStacking() {}

    /**
     * Where the next stacked element should start, in local (pre-scale) units — advances by
     * {@code sibling}'s actual <em>footprint height</em> ({@code max(localHeight, resolvedBounds's
     * height converted to local units)}), not its content-only {@code localHeight()} alone. A box's
     * rendered height can now diverge from its content height once independently resized/persisted
     * (drag/resize, {@link DietScreenPersistence}), and the old {@code currentLocalY + localHeight}
     * formula didn't know that — so a resized-taller RecentMeals box wouldn't push EatMore/Active
     * Effects down, and they'd render overlapped by it.
     *
     * <p>Deliberately uses only {@code resolvedBounds.height()} — never {@code resolvedBounds.y()}
     * or {@code .x()} — so a box that's been <em>dragged</em> elsewhere (position changed, height
     * unchanged) doesn't drag its sibling's stacked position along with it; only an actual resize
     * (height genuinely larger than the natural content height) advances the next element. Using the
     * box's absolute Y previously coupled Active Effects' position to EatMore being moved, and fed a
     * position-inflated value back into EatMore's own {@code startLocalY} (used for its {@code
     * visible}/constraint-preferred-size computation), which could go degenerate whenever RecentMeals
     * had simply been dragged rather than resized.
     *
     * <p>Returns {@code currentLocalY} unchanged when {@code localHeight <= 0} (sibling hidden/not
     * fitting), matching the old formula's no-op in that case.
     *
     * <p>Delegates to {@link AutoGrowPanelContainer#nextSiblingStartLocalY} (marie-ui) — this used
     * to be the one implementation; it's now also reused by {@link
     * DietScreenModules#naturalContentEndLocalY} to sum the same modules' natural total height for
     * panel auto-grow, so the chaining formula itself lives in marie-ui rather than being
     * duplicated between the two callers.
     *
     * <p>One exception to "position never affects the advance": a sibling whose resolved X has
     * drifted away from the column's own left edge ({@code contentX}) by more than {@link
     * #OUT_OF_FLOW_X_TOLERANCE} is no longer sitting <em>in</em> the single-width vertical column at
     * all — the classic case being {@code EatMoreComponent} dragged to sit beside {@code
     * RecentMealsComponent} instead of below it, both still left-aligned to the panel edge in the
     * "natural" case but diverging in X the moment either is dragged sideways. Reserving that box's
     * full height for whoever comes next (as the height-only rule above does) then reserves dead
     * space nothing is actually rendered into — the next module (frequently {@code
     * ActiveEffectsComponent}) gets pushed down by a box's height despite that box no longer
     * occupying that vertical slot, which can push it (and its header/body fit checks) past the
     * live panel's bottom edge even though the box that would have caused that lives visibly
     * elsewhere on screen. This check only gates whether the advance happens at all — X is never fed
     * into <em>how far</em> it advances (still {@code resolvedBounds.height()} alone), so it can't
     * reintroduce the Y-feedback degeneracy the javadoc above already ruled out.
     */
    private static final int OUT_OF_FLOW_X_TOLERANCE = 6;

    public static int nextSiblingStartLocalY(int currentLocalY, int localHeight, Bounds resolvedBounds, DietLayout.Layout layout) {
        int contentX = layout.panelX() + layout.leftMargin();
        if (Math.abs(resolvedBounds.x() - contentX) > OUT_OF_FLOW_X_TOLERANCE) {
            return currentLocalY;
        }
        return AutoGrowPanelContainer.nextSiblingStartLocalY(currentLocalY, localHeight, resolvedBounds, layout.scale());
    }
}
