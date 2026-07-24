package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.ui.geometry.Bounds;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class DietLayout {

    public static final int WIDTH = 316;
    public static final int HEIGHT = 268;
    public static final int SPLIT = 108;
    public static final int PAD = 10;

    /** Floor for the panel's height resize — small enough to shrink to a title-bar-only minimized state. */
    public static final int PANEL_MIN_LOCAL_HEIGHT = 40;
    public static final int PANEL_MINIMIZED_THRESHOLD_LOCAL_HEIGHT = 50;
    public static final int PANEL_BOTTOM_MARGIN_LOCAL = 34;

    /** {@code leftMargin} is dead space between the panel's left edge and the left column's content, from widening the panel via its left edge — zero by default. */
    public record Layout(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int baseX,
            int baseY,
            double scale,
            double recentMealsScale,
            double eatMoreScale,
            int leftMargin
    ) {}

    private DietLayout() {}

    public static Layout compute(Minecraft mc) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        return compute(
                mc,
                cc.dietScale(),
                cc.dietOffsetX(),
                cc.dietOffsetY(),
                cc.recentMealsBoxScale(),
                cc.eatMoreBoxScale()
        );
    }

    public static Layout compute(
            Minecraft mc,
            double scale,
            int offsetX,
            int offsetY,
            double recentMealsScale,
            double eatMoreScale
    ) {
        int panelW = scaledDim(WIDTH, scale);
        int panelH = scaledDim(HEIGHT, scale);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int baseX = (sw - panelW) / 2;
        int baseY = (sh - panelH) / 2;
        int panelX = Mth.clamp(baseX + offsetX, 0, Math.max(0, sw - panelW));
        int panelY = Mth.clamp(baseY + offsetY, 0, Math.max(0, sh - panelH));
        return new Layout(
                panelX,
                panelY,
                panelW,
                panelH,
                baseX,
                baseY,
                scale,
                recentMealsScale,
                eatMoreScale,
                0
        );
    }

    public static int scaledDim(int base, double scale) {
        return Math.max(1, (int) Math.round(base * scale));
    }

    /**
     * Whether a block starting at {@code startLocalY} and {@code blockLocalHeight} tall still fits
     * within the panel's live vertical space, leaving {@link #PAD} clearance above the bottom edge —
     * the single fit-check every stacked left-column sub-box (Calories/Balance/RecentMeals/EatMore/
     * ActiveEffects) and the right column's intake legend share, replacing what used to be five
     * near-identical hand-rolled copies (plus a sixth, differently-anchored one for the legend) that
     * could each drift out of sync with each other.
     */
    public static boolean fitsInPanel(Layout layout, int startLocalY, int blockLocalHeight) {
        int liveLocalHeight = (int) Math.round(layout.panelH() / layout.scale());
        int maxY = liveLocalHeight - PAD;
        return startLocalY + blockLocalHeight <= maxY;
    }

    /**
     * How much of a block naturally {@code naturalLocalHeight} tall, starting at {@code
     * startLocalY}, still fits — continuous {@code 0..naturalLocalHeight}, not the all-or-nothing
     * floor {@link #fitsInPanel} gives. A sub-box collapsing from its full
     * size down to nothing is jarring precisely at the boundary where it flips from "renders in
     * full" to "renders nothing at all" (a 1px panel-height change removing an entire header's
     * worth of content) — callers that want a smooth, continuous shrink instead (a box's whole
     * content block scaling down together as room tightens, only actually vanishing once there's no
     * meaningful room left) should divide their render-time content scale by {@code
     * naturalLocalHeight} against this room, not gate on a boolean.
     */
    public static int roomInPanel(Layout layout, int startLocalY, int naturalLocalHeight) {
        int liveLocalHeight = (int) Math.round(layout.panelH() / layout.scale());
        int maxY = liveLocalHeight - PAD;
        int room = maxY - startLocalY;
        return Math.max(0, Math.min(naturalLocalHeight, room));
    }

    public static int toScreenX(Layout layout, int localX) {
        return layout.panelX() + layout.leftMargin() + (int) Math.round(localX * layout.scale());
    }

    public static int toScreenY(Layout layout, int localY) {
        return layout.panelY() + (int) Math.round(localY * layout.scale());
    }

    public static int toScreenDim(Layout layout, int localDim) {
        return Math.max(1, (int) Math.round(localDim * layout.scale()));
    }

    /** Single source of truth for the column split — every caller (render, drag preview, clamps) must go through this. */
    public record ColumnGeometry(int leftX, int leftWidth, int dividerX, int rightX, int rightWidth) {}

    public static ColumnGeometry columnGeometry(Layout layout, Bounds panelBounds) {
        int gap = toScreenDim(layout, 1);
        int leftX = panelBounds.x() + layout.leftMargin();
        int leftWidth = toScreenDim(layout, SPLIT);
        int dividerX = leftX + leftWidth;
        int rightX = dividerX + gap;
        int rightWidth = Math.max(0, (panelBounds.x() + panelBounds.width()) - rightX);
        return new ColumnGeometry(leftX, leftWidth, dividerX, rightX, rightWidth);
    }
}
