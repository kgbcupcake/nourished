package dev.maire.nourished.client.screen.diet;

import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietStacking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the Diet screen's pure layout maths (local-unit to screen conversion, how much of
 * a box fits, how siblings stack). They pin today's behavior so the module refactor can't silently change it.
 */
class DietLayoutMathTest {

    private static DietLayout.Layout layout(int panelX, int panelY, int panelW, int panelH, double scale, int leftMargin) {
        return new DietLayout.Layout(panelX, panelY, panelW, panelH, panelX, panelY, scale, 1.0, 1.0, leftMargin);
    }

    @Test
    @DisplayName("scaledDim rounds, and never returns less than 1")
    void scaledDim() {
        assertEquals(316, DietLayout.scaledDim(316, 1.0));
        assertEquals(158, DietLayout.scaledDim(316, 0.5));
        assertEquals(1, DietLayout.scaledDim(1, 0.1));
    }

    @Test
    @DisplayName("toScreenDim scales a local size and floors at 1")
    void toScreenDim() {
        DietLayout.Layout full = layout(0, 0, 316, 268, 1.0, 0);
        DietLayout.Layout half = layout(0, 0, 158, 134, 0.5, 0);
        assertEquals(10, DietLayout.toScreenDim(full, 10));
        assertEquals(2, DietLayout.toScreenDim(half, 3));
        assertEquals(1, DietLayout.toScreenDim(half, 0));
    }

    @Test
    @DisplayName("toScreenX adds the panel origin and left margin; toScreenY only the panel origin")
    void toScreenXY() {
        DietLayout.Layout l = layout(100, 50, 632, 536, 2.0, 5);
        assertEquals(100 + 5 + 20, DietLayout.toScreenX(l, 10));
        assertEquals(50 + 20, DietLayout.toScreenY(l, 10));
    }

    @Test
    @DisplayName("fitsInPanel leaves PAD (10) clear above the bottom edge, independent of scale")
    void fitsInPanel() {
        DietLayout.Layout l = layout(0, 0, 316, 268, 1.0, 0);   // live local height 268 -> max start+height 258
        assertTrue(DietLayout.fitsInPanel(l, 200, 58));
        assertFalse(DietLayout.fitsInPanel(l, 200, 59));

        DietLayout.Layout doubled = layout(0, 0, 632, 536, 2.0, 0);  // same local height at scale 2
        assertTrue(DietLayout.fitsInPanel(doubled, 200, 58));
        assertFalse(DietLayout.fitsInPanel(doubled, 200, 59));
    }

    @Test
    @DisplayName("roomInPanel is the continuous 0..natural version of fitsInPanel")
    void roomInPanel() {
        DietLayout.Layout l = layout(0, 0, 316, 268, 1.0, 0);
        assertEquals(40, DietLayout.roomInPanel(l, 0, 40), "plenty of room: the full natural height");
        assertEquals(58, DietLayout.roomInPanel(l, 200, 100), "clamped to what's left above the bottom pad");
        assertEquals(0, DietLayout.roomInPanel(l, 300, 40), "no room left never goes negative");
    }

    @Test
    @DisplayName("columnGeometry splits at SPLIT with a one-unit gap, honoring the left margin")
    void columnGeometry() {
        DietLayout.Layout l = layout(100, 50, 316, 268, 1.0, 4);
        DietLayout.ColumnGeometry g = DietLayout.columnGeometry(l, new Bounds(100, 50, 316, 268));
        assertEquals(104, g.leftX());
        assertEquals(108, g.leftWidth());
        assertEquals(212, g.dividerX());
        assertEquals(213, g.rightX());
        assertEquals(416 - 213, g.rightWidth());
    }

    @Test
    @DisplayName("a sibling in the column advances the cursor by its own resolved height")
    void siblingInColumnAdvances() {
        DietLayout.Layout l = layout(100, 50, 316, 268, 1.0, 4);          // content X = 104
        Bounds inColumn = new Bounds(104, 80, 108, 58);
        assertEquals(10 + 58, DietStacking.nextSiblingStartLocalY(10, 58, inColumn, l));

        DietLayout.Layout doubled = layout(100, 50, 632, 536, 2.0, 4);
        Bounds tall = new Bounds(104, 80, 216, 116);
        assertEquals(10 + 58, DietStacking.nextSiblingStartLocalY(10, 58, tall, doubled), "height is converted back to local units");
    }

    @Test
    @DisplayName("a hidden sibling (local height 0) does not advance the cursor")
    void hiddenSiblingDoesNotAdvance() {
        DietLayout.Layout l = layout(100, 50, 316, 268, 1.0, 0);
        assertEquals(10, DietStacking.nextSiblingStartLocalY(10, 0, new Bounds(100, 80, 108, 58), l));
    }

    @Test
    @DisplayName("a sibling dragged more than 6px out of the column stops reserving vertical space")
    void outOfFlowSiblingDoesNotAdvance() {
        DietLayout.Layout l = layout(100, 50, 316, 268, 1.0, 4);          // content X = 104
        assertEquals(10 + 58, DietStacking.nextSiblingStartLocalY(10, 58, new Bounds(110, 80, 108, 58), l), "exactly at the tolerance still counts as in the column");
        assertEquals(10, DietStacking.nextSiblingStartLocalY(10, 58, new Bounds(111, 80, 108, 58), l));
        assertEquals(10, DietStacking.nextSiblingStartLocalY(10, 58, new Bounds(97, 80, 108, 58), l));
    }
}
