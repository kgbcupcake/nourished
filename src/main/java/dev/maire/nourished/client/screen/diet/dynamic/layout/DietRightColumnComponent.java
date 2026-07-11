package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.client.MarieValueColors;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * Static-rendering port of {@link DietScreen#drawRightPanel} plus its legend bar. Not built on top
 * of the HUD pass's {@code NutrientBarComponent}: that component's constructor is shaped around
 * {@code HudLayout.Layout} (icon+gap+label+bar+pct row geometry, threshold-tiered fill colors) and
 * a plain rounded bar, whereas the Diet Screen row is a fundamentally different visual — a bordered
 * icon box, a fixed nutrient-color percentage (not threshold-tiered), a segmented decorative bar,
 * and up/down trend arrows against the previous tick's value. Reusing it would mean bending its
 * constructor to a shape it wasn't designed for, so this pass ports {@code drawRightPanel}'s row
 * logic directly instead, same principle as {@link DietLeftColumnComponent}.
 */
final class DietRightColumnComponent implements MarieComponent, HeaderCollapsibleComponent {

    private static final int BAR_H = 9;
    private static final int ROW_STEP = 26;
    /** Local height of the "Intake Breakdown" title + separator line block, before the row list starts (y=30 to y=44). */
    private static final int HEADER_LOCAL_HEIGHT = 14;
    private static final int COL_PANEL_RGB = 0x001E1E1E;
    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER = 0xFF3A3A3A;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_HEADER = 0xFF888888;
    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_RED = 0xFFFF5555;
    private static final int COL_ORANGE = 0xFFFFAA00;
    private static final int COL_SEG_EMPTY = 0xFF2A2A2A;
    private static final int COL_LEGEND_TEXT = 0xFFE0E0E0;
    private static final int COL_FLASH_RGB = 0xFFFFE0;

    private final TrackingData data;
    private final List<String> bars;
    private final Map<String, Float> display;
    private final DietLayout.Layout layout;
    private final int width;
    private final int height;

    DietRightColumnComponent(TrackingData data, List<String> bars, Map<String, Float> display, DietLayout.Layout layout, int width, int height) {
        this.data = data;
        this.bars = bars;
        this.display = display;
        this.layout = layout;
        this.width = width;
        this.height = height;
    }

    @Override
    public String id() {
        return "nourished.diet.panel.right";
    }

    @Override
    public Constraint constraint() {
        return Constraint.fixed(width, height);
    }

    @Override
    public int headerLocalHeight() {
        return HEADER_LOCAL_HEIGHT;
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        if (data == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        float scale = (float) layout.scale();

        int rx = DietLayout.SPLIT + DietLayout.PAD;
        int y = 30;

        String hdr = Component.translatable("nourished.screen.diet.intake").getString();
        int hdrW = font.width(hdr);
        int hdrCX = rx + (DietLayout.WIDTH - DietLayout.SPLIT - DietLayout.PAD * 2) / 2;
        drawText(context, "✧✧", rx + 2, y, COL_HEADER, scale);
        drawText(context, "✧✧", DietLayout.WIDTH - DietLayout.PAD - 14, y, COL_HEADER, scale);
        drawText(context, hdr, hdrCX - hdrW / 2, y, COL_HEADER, scale);
        int lineY = y + 4;
        fillRect(context, rx, lineY, hdrCX - hdrW / 2 - 3 - rx, 1, COL_BORDER_LT);
        fillRect(context, hdrCX + hdrW / 2 + 3, lineY, (DietLayout.WIDTH - DietLayout.PAD) - (hdrCX + hdrW / 2 + 3), 1, COL_BORDER_LT);
        y += 14;

        int arrowSlot = 10;
        int arrowLeft = DietLayout.WIDTH - DietLayout.PAD - arrowSlot;
        int pctColumnRight = arrowLeft - 4;
        int maxPctW = font.width("100%");
        int barLeft = rx + 24;
        int barW = Math.max(0, pctColumnRight - maxPctW - 4 - barLeft);

        // Height can shrink below natural (minimize), so rows/legend hide as they stop fitting.
        // Width can't (panelConstraint floors it at natural), so it needs no such handling.
        int liveLocalHeight = (int) Math.round(height / layout.scale());
        int maxRowY = liveLocalHeight - DietLayout.PAD;

        Bounds rowProbeBounds = new Bounds(0, 0, width, DietLayout.toScreenDim(layout, Math.max(0, maxRowY - 30)));
        int rowsToShow = bodyUnitsFit(rowProbeBounds, layout.scale(), bars.size(), ROW_STEP);

        int rowCount = 0;
        for (String key : bars) {
            if (rowCount >= rowsToShow) {
                break;
            }
            rowCount++;
            float disp = display.getOrDefault(key, data.values.getOrDefault(key, 0f));
            float real = data.values.getOrDefault(key, 0f);
            float prev = data.lastValues.getOrDefault(key, real);
            int color = barColor(key, disp);
            int pctColor = nutrientBaseColor(key);

            int rowRight = DietLayout.WIDTH - DietLayout.PAD;
            drawRoundedBox(context, rx - 2, y - 2, rowRight - (rx - 2), 26);

            int bx = rx;
            int by = y;
            int panelFill = panelColorWithOpacity(COL_PANEL_RGB, NourishedClientConfig.get().dietBackgroundOpacity());
            drawRoundedRect(context, bx, by, 20, 20, panelFill, COL_BORDER_LT);

            String iconId = NutrientRegistry.getIcon(key);
            Item iconItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(iconId)).orElse(Items.APPLE);
            drawItem(context, new ItemStack(iconItem), bx + 2, by + 2, scale);

            drawText(context, NutrientRegistry.getLabelComponent(key).getString(), rx + 24, y + 2, COL_WHITE, scale);

            context.drawBar(sx(rx + 24), sy(y + 12), sd(barW), sd(BAR_H), disp, COL_SEG_EMPTY, color);

            float flashA = MarieClientCache.flashAlpha(key);
            if (flashA > 0f) {
                int aByte = Mth.clamp(Mth.floor(flashA * 255f), 1, 255);
                fillRect(context, rx + 24, y + 12, barW, BAR_H, (aByte << 24) | COL_FLASH_RGB);
            }

            String pctStr = Math.round(disp * 100) + "%";
            int pctX = pctColumnRight - font.width(pctStr);
            int dimmedPct = (pctColor & 0x00FFFFFF) | 0x99000000;
            drawText(context, pctStr, pctX, y + 2, dimmedPct, scale);

            if (real > prev + 0.005f)
                drawText(context, "↑", arrowLeft, y + 2, COL_GREEN, scale);
            else if (real < prev - 0.005f)
                drawText(context, "↓", arrowLeft, y + 2, COL_RED, scale);

            y += ROW_STEP;
        }

        int legendY = liveLocalHeight - 66;
        int legendH = 34;
        if (legendY >= y && legendY + legendH <= liveLocalHeight - DietLayout.PAD) {
            drawLegendBar(context, font, rx, legendY, (DietLayout.WIDTH - DietLayout.PAD) - rx, legendH);
        }
    }

    private void drawLegendBar(RenderContext context, Font font, int x, int y, int w, int h) {
        drawRoundedBox(context, x, y, w, h);
        String legend = Component.translatable("nourished.screen.diet.legend").getString();
        int legendW = font.width(legend);
        drawText(context, legend, x + w / 2 - legendW / 2, y + 3, dimLegend(COL_HEADER), (float) layout.scale());

        int colLeft = x + 6;
        int colW = (w - 12) / 3;
        int lineTop = y + 12;
        int lineBottom = y + h - 4;
        fillRect(context, colLeft + colW, lineTop, 1, lineBottom - lineTop, 0xFF2E2E2E);
        fillRect(context, colLeft + colW * 2, lineTop, 1, lineBottom - lineTop, 0xFF2E2E2E);

        drawLegendEntry(context, font, colLeft, y + 14, colW, dimLegend(COL_GREEN), "Good", "40 - 80%", 0, -3, -2, dimLegend(COL_GREEN));
        drawLegendEntry(context, font, colLeft + colW + 1, y + 14, colW, dimLegend(0xFFE8C24F), "Low", "25 - 40%", 0, -3, 0, dimLegend(0xFFE8C24F));
        drawLegendEntry(
                context,
                font,
                colLeft + colW * 2 - 2,
                y + 14,
                colW,
                dimLegend(COL_RED),
                Component.translatable("nourished.screen.diet.legend_bad").getString(),
                Component.translatable("nourished.screen.diet.legend_bad_range").getString(),
                0,
                0,
                0,
                dimLegend(COL_RED)
        );
    }

    private void drawLegendEntry(RenderContext context, Font font, int x, int y, int w, int color, String line1, String line2, int line1Offset, int line2Offset, int squareOffset, int line2Color) {
        int squareX = x + (w / 2) - 18 + squareOffset;
        fillRect(context, squareX, y + 1, 8, 8, color);

        int line1X = squareX + 11 + line1Offset;
        int line2X = x + ((w - font.width(line2)) / 2) + 6 + line2Offset;
        float scale = (float) layout.scale();
        drawText(context, line1, line1X, y, dimLegend(COL_LEGEND_TEXT), scale);
        drawText(context, line2, line2X, y + 10, line2Color, scale);
    }

    /** ~12% darker legend text and swatches (brightness reduction), matching legacy {@code dimLegend}. */
    private static int dimLegend(int argb) {
        float f = 0.88f;
        int a = (argb >>> 24) & 0xFF;
        int r = Mth.clamp((int) (((argb >> 16) & 0xFF) * f), 0, 255);
        int g = Mth.clamp((int) (((argb >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp((int) ((argb & 0xFF) * f), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int barColor(String key, float v) {
        NourishedConfig config = NourishedConfig.get();
        float critical = (float) config.criticalThresholdFor(key);
        float low = (float) config.lowThreshold();
        if (v < critical) return COL_RED;
        if (v < low) return COL_ORANGE;
        return nutrientBaseColor(key);
    }

    private static int nutrientBaseColor(String key) {
        return MarieValueColors.baseColorArgb(key);
    }

    private static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    // ── Coordinate + drawing helpers ─────────────────────────────────────────

    /**
     * The divider's live screen X — {@code toScreenX(layout, SPLIT)} — is the one position that
     * must NEVER move due to this column's own content-scale (it's owned by {@link
     * DietLayout#columnGeometry}, driven only by {@link DietLayout.Layout#leftMargin}, same as
     * everywhere else the divider is drawn/clamped against). Every local X here is anchored relative
     * to {@code SPLIT} and scaled by {@link #contentScale} from that fixed point, so shrinking this
     * column's content never drags the divider along with it.
     */
    private int sx(int localX) {
        return DietLayout.toScreenX(layout, localX);
    }

    private int sy(int localY) {
        return DietLayout.toScreenY(layout, localY);
    }

    private int sd(int localDim) {
        return DietLayout.toScreenDim(layout, localDim);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(localY), color, scale);
    }

    private void drawItem(RenderContext context, ItemStack stack, int localX, int localY, float scale) {
        context.drawItem(stack, sx(localX), sy(localY), scale);
    }

    private void fillRect(RenderContext context, int localX, int localY, int localW, int localH, int color) {
        context.fillRect(sx(localX), sy(localY), sd(localW), sd(localH), color);
    }

    private void drawRoundedRect(RenderContext context, int localX, int localY, int localW, int localH, int fillColor, int borderColor) {
        context.drawRoundedRect(sx(localX), sy(localY), sd(localW), sd(localH), 1, fillColor, borderColor);
    }

    private void drawRoundedBox(RenderContext context, int localX, int localY, int localW, int localH) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, NourishedClientConfig.get().dietBackgroundOpacity());
        drawRoundedRect(context, localX, localY, localW, localH, fill, COL_BORDER_LT);
    }
}
