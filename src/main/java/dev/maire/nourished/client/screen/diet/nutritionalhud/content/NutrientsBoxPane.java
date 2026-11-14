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
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.micro.NutrientDetailView;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import net.minecraft.client.Minecraft;

/**
 * The Nutrients tab's box: {@link NutrientDetailView} inside a box styled by its "Nutrients" editor
 * entry (background, border, shadow, glow, pulse), same as the Home box. Content taller than the box
 * scrolls with the mouse wheel or the scrollbar on its right edge.
 */
public final class NutrientsBoxPane implements MarieComponent {

    public static final String ID = "nourished.diet.nutrients_tab";
    private static final int PAD = 6;
    private static final int NATURAL_WIDTH = 340;
    private static final int SCROLL_STEP = 20;
    private static final int BAR_W = 3;
    private static final int BAR_GAP = 3;
    private static final int TRACK = 0x30FFFFFF;
    private static final int THUMB = 0xFF6A747E;
    private static final int THUMB_HOT = 0xFFB0BAC4;
    private static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private final NutrientDetailView view = new NutrientDetailView(0);
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

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(naturalWidth(), naturalHeight());
    }

    public int naturalWidth() {
        return NATURAL_WIDTH;
    }

    /** Follows the open page's content, so an untouched box fits whatever page is showing. */
    public int naturalHeight() {
        return contentHeight + PAD * 2;
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        lastBounds = bounds;
        PersistenceProvider store = DietScreenPersistence.get();
        if (MarieModuleSettings.isWindowHidden(store, ID)) {
            return;
        }
        int baseFill = MarieStoredStyle.applyOpacity(NourishedColors.surfaceRgb(), store, ID, DEFAULT_OPACITY);
        int fill = MarieModuleSettings.styledBackground(baseFill, store, ID);
        int border = MarieModuleSettings.styledBorder(MarieColors.resolveColor(NourishedColors.NUTRIENTS_BORDER), store, ID);
        MarieModuleSettings.drawBoxGlow(context, store, ID, bounds.x(), bounds.y(), bounds.width(), bounds.height());
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
        context.pushClip(bounds.x() + PAD, bounds.y() + PAD, innerW, innerH);
        try {
            contentHeight = view.render(gc.graphics(), mc.font, bounds.x() + PAD, bounds.y() + PAD - scroll,
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
        return lastBounds.contains((int) mouseX, (int) mouseY) && view.mouseClicked(mouseX, mouseY);
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
        return view.keyPressed(keyCode);
    }
}
