package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.MarieStoredStyle;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.marie.framework.color.ColorKey;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.edit.BoxContentStyle;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A Nutritional Panel tab box styled by its own editor entry (background, border, shadow, glow,
 * pulse), same as the Home box. Content taller than the box scrolls with the mouse wheel or the
 * scrollbar on its right edge.
 */
public abstract class ScrollingBoxPane implements MarieComponent {

    private static final int PAD = 6;
    private static final int NATURAL_WIDTH = 340;
    private static final int SCROLL_STEP = 20;
    private static final int BAR_W = 3;
    private static final int BAR_GAP = 3;
    private static final int TRACK = 0x30FFFFFF;
    private static final int THUMB = 0xFF6A747E;
    private static final int THUMB_HOT = 0xFFB0BAC4;
    private static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private Bounds lastBounds = new Bounds(0, 0, 0, 0);
    private int contentHeight = 200;
    private int scroll;
    // scrollbar geometry from the last frame; barH == 0 means nothing overflows
    private int barX;
    private int barY;
    private int barH;
    private int thumbY;
    private int thumbH;
    private int dragGrab = -1;
    private int maxHeight = Integer.MAX_VALUE;
    private BoxContentStyle style = BoxContentStyle.NONE;

    protected abstract ColorKey borderColor();

    /** This frame's Move/Hide settings, for subclasses whose content honors them. */
    protected BoxContentStyle style() {
        return style;
    }

    /** Draws the content at {@code (x, y)} and returns its height. */
    protected abstract int renderContent(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY);

    protected boolean contentClicked(double mouseX, double mouseY) {
        return false;
    }

    protected boolean contentKey(int keyCode) {
        return false;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(naturalWidth(), naturalHeight());
    }

    public int naturalWidth() {
        return NATURAL_WIDTH;
    }

    /** Follows the open page's content, capped to the tab so taller content scrolls instead of overflowing. */
    public int naturalHeight() {
        return Math.min(contentHeight + PAD * 2, maxHeight);
    }

    /** Tallest the box may be in its tab; set every frame by {@link BoxTabRenderer}. */
    void setMaxHeight(int maxHeight) {
        this.maxHeight = Math.max(PAD * 2 + 20, maxHeight);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        lastBounds = bounds;
        PersistenceProvider store = DietScreenPersistence.get();
        if (MarieModuleSettings.isWindowHidden(store, id())) {
            return;
        }
        int baseFill = MarieStoredStyle.applyOpacity(NourishedColors.surfaceRgb(), store, id(), DEFAULT_OPACITY);
        int fill = MarieModuleSettings.styledBackground(baseFill, store, id());
        int border = MarieModuleSettings.styledBorder(MarieColors.resolveColor(borderColor()), store, id());
        MarieModuleSettings.drawBoxGlow(context, store, id(), bounds.x(), bounds.y(), bounds.width(), bounds.height());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, fill, border);
        if (!(context instanceof GuiGraphicsRenderContext gc)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double gui = mc.getWindow().getGuiScale();
        int mx = (int) (mc.mouseHandler.xpos() / gui);
        int my = (int) (mc.mouseHandler.ypos() / gui);
        int innerH = Math.max(0, bounds.height() - PAD * 2);
        int maxScroll = Math.max(0, contentHeight - innerH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int innerW = Math.max(0, bounds.width() - PAD * 2);
        int viewW = maxScroll > 0 ? innerW - BAR_W - BAR_GAP : innerW;
        // resets this box's recorded extents for the editor's Move outlines
        MarieModuleSettings.withDisplaySettings(context, store, id());
        style = BoxContentStyle.of(store, id());
        context.pushClip(bounds.x() + PAD, bounds.y() + PAD, innerW, innerH);
        try {
            contentHeight = renderContent(gc.graphics(), mc.font, bounds.x() + PAD, bounds.y() + PAD - scroll,
                    Math.max(40, viewW), mx, my);
        } finally {
            context.popClip();
        }
        drawScrollbar(gc, bounds, innerH, maxScroll, mx, my);
    }

    private void drawScrollbar(GuiGraphicsRenderContext gc, Bounds bounds, int innerH, int maxScroll, int mx, int my) {
        if (maxScroll <= 0 || innerH <= 0) {
            barH = 0;
            dragGrab = -1;
            return;
        }
        barX = bounds.x() + bounds.width() - PAD - BAR_W;
        barY = bounds.y() + PAD;
        barH = innerH;
        thumbH = Math.max(12, innerH * innerH / (innerH + maxScroll));
        thumbY = barY + Math.round((float) (innerH - thumbH) * scroll / maxScroll);
        boolean hot = dragGrab >= 0 || overBar(mx, my);
        gc.graphics().fill(barX, barY, barX + BAR_W, barY + barH, TRACK);
        gc.graphics().fill(barX, thumbY, barX + BAR_W, thumbY + thumbH, hot ? THUMB_HOT : THUMB);
    }

    private boolean overBar(double mx, double my) {
        return barH > 0 && mx >= barX - 2 && mx < barX + BAR_W + 2 && my >= barY && my < barY + barH;
    }

    private void scrollThumbTo(double mouseY) {
        int travel = barH - thumbH;
        int maxScroll = Math.max(0, contentHeight - barH);
        if (travel <= 0) return;
        double top = Math.max(0, Math.min(travel, mouseY - barY - dragGrab));
        scroll = (int) Math.round(top / travel * maxScroll);
    }

    boolean clickContent(double mouseX, double mouseY) {
        if (overBar(mouseX, mouseY)) {
            // grab the thumb where clicked; a click on the track centres the thumb there
            boolean onThumb = mouseY >= thumbY && mouseY < thumbY + thumbH;
            dragGrab = onThumb ? (int) (mouseY - thumbY) : thumbH / 2;
            scrollThumbTo(mouseY);
            return true;
        }
        return lastBounds.contains((int) mouseX, (int) mouseY) && contentClicked(mouseX, mouseY);
    }

    boolean dragContent(double mouseY) {
        if (dragGrab < 0) return false;
        scrollThumbTo(mouseY);
        return true;
    }

    boolean releaseContent() {
        boolean was = dragGrab >= 0;
        dragGrab = -1;
        return was;
    }

    boolean scrollContent(double mouseX, double mouseY, double scrollY) {
        if (scrollY == 0 || !lastBounds.contains((int) mouseX, (int) mouseY)) return false;
        scroll += (int) (-Math.signum(scrollY) * SCROLL_STEP);
        return true;
    }

    boolean keyContent(int keyCode) {
        return contentKey(keyCode);
    }
}
