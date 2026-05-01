package dev.maire.nourished.client.screen;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.nutrition.NutrientRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class DietScreen extends Screen {

    // ── Panel dimensions ─────────────────────────────────────────────────────
    private static final int WIDTH       = 316;
    private static final int HEIGHT      = 248;
    private static final int SPLIT       = 108;    // left panel width
    private static final int PAD         = 10;

    // ── Right panel row geometry ─────────────────────────────────────────────
    private static final int ROW_STEP    = 22;

    private static final float FADE_DURATION_SEC = 0.15f;
    private static final float FADE_TOOLTIP_THRESHOLD = 0.9f;


    // ── Colors ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xF01A1A1A;
    private static final int COL_PANEL      = 0xFF161616;
    private static final int COL_BORDER     = 0xFF3A3A3A;
    private static final int COL_BORDER_LT  = 0xFF555555;
    private static final int COL_SEG_EMPTY  = 0xFF2A2A2A;
    private static final int COL_DIVIDER    = 0xFF2E2E2E;
    private static final int COL_HEADER     = 0xFF888888;
    private static final int COL_WHITE      = 0xFFFFFFFF;
    private static final int COL_GRAY       = 0xFF666666;
    private static final int COL_GOLD       = 0xFFFFD65C;
    private static final int COL_CYAN       = 0xFF4DD9D9;
    private static final int COL_GREEN      = 0xFF55FF55;
    private static final int COL_ORANGE     = 0xFFFFAA00;
    private static final int COL_RED        = 0xFFFF5555;
    private static final int COL_PURPLE     = 0xFFA95FFF;
    private static final int COL_ROW_BG     = 0xFF1D1D1D;
    /** Warm white/yellow (RGB) for nutrient bar flash overlay; alpha applied at draw time. */
    private static final int COL_FLASH_RGB  = 0xFFFFE0;

    // ── State ────────────────────────────────────────────────────────────────
    private int leftPos, topPos;
    private final Map<String, Float>     display = new LinkedHashMap<>();
    private final Map<String, ItemStack> icons   = new LinkedHashMap<>();
    private final List<String> visibleBars        = new ArrayList<>();
    /** Index of nutrient row whose icon is being dragged; null when not dragging. */
    private Integer dragBarFromIndex;

    /** 0..1 fade-in over {@link #FADE_DURATION_SEC}; updated each render from frame delta. */
    private float fadeAlpha;
    private long fadeLastFrameNanos;
    private boolean fadeClockStarted;

    public DietScreen() {
        super(Component.translatable("nourished.screen.diet"));
        fadeAlpha = 0f;
        fadeClockStarted = false;
    }

    @Override
    public void onClose() {
        dragBarFromIndex = null;
        super.onClose();
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        leftPos = (width  - WIDTH)  / 2;
        topPos  = (height - HEIGHT) / 2;

        visibleBars.clear();
        dragBarFromIndex = null;
        for (String key : NourishedClientConfig.get().effectiveDietBarOrder()) {
            display.putIfAbsent(key, 0f);
            icons.put(key, new ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(NutrientRegistry.getIcon(key)))));
            visibleBars.add(key);
        }

        // Compact close button (player can still press E)
        addRenderableWidget(
                Button.builder(
                        Component.translatable("nourished.screen.diet.close"),
                        b -> onClose()
                ).bounds(leftPos + (WIDTH - 118) / 2, topPos + HEIGHT - 22, 118, 18).build()
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && NourishedClientConfig.get().dietBarDragEnabled()) {
            int rx = leftPos + SPLIT + PAD;
            int y0 = topPos + 44;
            for (int i = 0; i < visibleBars.size(); i++) {
                int by = y0 + i * ROW_STEP;
                if (mouseX >= rx && mouseX <= rx + 20 && mouseY >= by && mouseY <= by + 20) {
                    dragBarFromIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragBarFromIndex != null && button == 0 && NourishedClientConfig.get().dietBarDragEnabled()) {
            int y0 = topPos + 44;
            int to = Mth.clamp((int) ((mouseY - y0 + ROW_STEP / 2) / ROW_STEP), 0, visibleBars.size() - 1);
            int from = dragBarFromIndex;
            dragBarFromIndex = null;
            if (from >= 0 && from < visibleBars.size() && from != to) {
                String moved = visibleBars.remove(from);
                visibleBars.add(to, moved);
                NourishedClientConfig.get().setDietBarOrder(new ArrayList<>(visibleBars));
                NourishedClientConfig.saveNow();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragBarFromIndex != null && button == 0) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        if (!fadeClockStarted) {
            fadeLastFrameNanos = now;
            fadeClockStarted = true;
        }
        float dt = (now - fadeLastFrameNanos) / 1_000_000_000f;
        fadeLastFrameNanos = now;
        fadeAlpha = Mth.clamp(fadeAlpha + dt / FADE_DURATION_SEC, 0f, 1f);

        boolean showIconTooltips = fadeAlpha > FADE_TOOLTIP_THRESHOLD;

        RenderSystem.setShaderColor(1f, 1f, 1f, fadeAlpha);
        renderBackground(g, mx, my, pt);

        DietData data = getClientData();

        // Animate display values
        if (data != null) {
            for (String k : visibleBars) {
                float target = data.nutrients.getOrDefault(k, 0f);
                float cur    = display.getOrDefault(k, 0f);
                display.put(k, cur + (target - cur) * 0.18f);
            }
        }

        // ── Outer panel ──────────────────────────────────────────────────────
        drawRoundedPanel(g, leftPos, topPos, WIDTH, HEIGHT, COL_BG, COL_BORDER_LT, COL_BORDER);

        // ── Title ─────────────────────────────────────────────────────────────
        String titleText = "☘ Diet ☘";
        int titleW = font.width(titleText);
        g.drawString(font, titleText, leftPos + (WIDTH / 2) - (titleW / 2), topPos + 9, 0xFF9BD36A, false);

        // Vertical divider between left and right panel
        g.fill(leftPos + SPLIT, topPos + 26, leftPos + SPLIT + 1, topPos + HEIGHT - 34, COL_DIVIDER);

        if (data == null) {
            g.drawCenteredString(font,
                Component.translatable("nourished.screen.diet.no_player"),
                leftPos + WIDTH / 2, topPos + HEIGHT / 2, COL_GRAY);
        } else {
            drawLeftPanel(g, data);
            drawRightPanel(g, data, fadeAlpha);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (showIconTooltips && data != null) {
            drawDietIconTooltips(g, mx, my);
        }
        super.render(g, mx, my, pt);
    }

    // ── Left panel ───────────────────────────────────────────────────────────

    private void drawLeftPanel(GuiGraphics g, DietData data) {
        int x  = leftPos + PAD;
        int y  = topPos + 20;
        int bw = SPLIT - PAD * 2;   // box inner width

        // "Today" header
        String todayText = Component.translatable("nourished.screen.diet.today").getString();
        int todayW = font.width(todayText);
        int todayGroupW = 16 + 4 + todayW;
        int todayStartX = x + (bw - todayGroupW) / 2;
        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:sunflower"))), todayStartX, y - 8);
        g.drawString(font, Component.translatable("nourished.screen.diet.today"),
                todayStartX + 20, y - 4, COL_GOLD, false);
        y += 10;

        // ── Calories box ──────────────────────────────────────────────────
        drawRoundedBox(g, x - 2, y - 2, bw + 4, 40);

        // Fire charge icon
        g.renderItem(new net.minecraft.world.item.ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:fire_charge"))),
                x, y + 3);

        g.drawString(font, Component.translatable("nourished.screen.diet.calories_label"),
                x + 22, y + 4, COL_WHITE, false);

        String calStr = (int) data.calories + " / " + (int) data.maxCalories;
        g.drawString(font, calStr, x + 22, y + 15, COL_GREEN, false);

        float calPct = data.maxCalories > 0 ? Mth.clamp(data.calories / data.maxCalories, 0f, 1f) : 0f;
        drawSolidBar(g, x, y + 31, bw, 4, calPct, COL_GREEN);
        y += 45;

        // ── Balance box ───────────────────────────────────────────────────
        drawRoundedBox(g, x - 2, y - 2, bw + 4, 40);

        g.renderItem(new net.minecraft.world.item.ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:comparator"))),
                x, y + 3);

        g.drawString(font, Component.translatable("nourished.screen.diet.balance_label"),
                x + 22, y + 4, COL_WHITE, false);

        String balKey   = getBalanceKey(data);
        int    balColor = balanceColor(balKey);
        g.drawString(font,
                Component.translatable("nourished.screen.diet.balance_state." + balKey),
                x + 22, y + 16, balColor, false);
        y += 45;

        // ── Tip text ──────────────────────────────────────────────────────
        drawRoundedBox(g, x - 2, y - 3, bw + 4, 50);
        g.drawWordWrap(font,
                Component.translatable("nourished.screen.diet.tip"),
                x, y, bw, COL_GRAY);

        // ── Reset timer ───────────────────────────────────────────────────
        LocalTime now        = LocalTime.now();
        int secondsLeft      = (23 - now.getHour()) * 3600
                             + (59 - now.getMinute()) * 60
                            + (60 - now.getSecond());
        int hLeft = secondsLeft / 3600;
        int mLeft = (secondsLeft % 3600) / 60;
        String resetStr = Component.translatable("nourished.screen.diet.reset_in", hLeft, mLeft).getString();
        int resetX = x + (bw - font.width(resetStr)) / 2;
        g.drawString(font, resetStr, resetX, topPos + HEIGHT - 46, COL_CYAN, false);
    }

    // ── Right panel ──────────────────────────────────────────────────────────

    private void drawRightPanel(GuiGraphics g, DietData data, float panelFadeAlpha) {
        int rx = leftPos + SPLIT + PAD;   // right panel left edge
        int y  = topPos + 30;

        // ── "Intake Breakdown" header with decorative lines ───────────────
        String hdr    = Component.translatable("nourished.screen.diet.intake").getString();
        int    hdrW   = font.width(hdr);
        int    hdrCX  = rx + (WIDTH - SPLIT - PAD * 2) / 2;
        g.drawString(font, "✧✧", rx + 2, y, COL_HEADER, false);
        g.drawString(font, "✧✧", leftPos + WIDTH - PAD - 14, y, COL_HEADER, false);
        g.drawString(font, hdr, hdrCX - hdrW / 2, y, COL_HEADER, false);
        int lineY = y + 4;
        g.fill(rx,                        lineY, hdrCX - hdrW / 2 - 3, lineY + 1, COL_BORDER_LT);
        g.fill(hdrCX + hdrW / 2 + 3,     lineY, leftPos + WIDTH - PAD, lineY + 1, COL_BORDER_LT);
        y += 14;

        int barW = (leftPos + WIDTH - PAD - 43) - (rx + 24);

        for (String key : visibleBars) {
            float disp  = display.getOrDefault(key, 0f);
            float real  = data.nutrients.getOrDefault(key, 0f);
            float prev  = data.lastNutrients.getOrDefault(key, real);
            int   color = barColor(key, disp);
            int   pctColor = nutrientBaseColor(key);

            // ── Row background ────────────────────────────────────────────
            drawRoundedBox(g, rx - 2, y - 2, (leftPos + WIDTH - PAD) - (rx - 2), 22);

            // ── Icon box 24x24 with 1px border ───────────────────────────
            int bx = rx;
            int by = y;
            drawRoundedPanel(g, bx, by, 20, 20, COL_PANEL, COL_BORDER_LT, COL_BORDER);
            // item icon at rx+4, y+4 size 16x16
            ItemStack icon = icons.get(key);
            if (icon != null && !icon.isEmpty()) {
                g.renderItem(icon, bx + 2, by + 2);
            }

            // ── Nutrient name ─────────────────────────────────────────────
            g.drawString(font,
                    Component.translatable("nourished.screen.diet.bar." + key),
                    rx + 24, y + 2, COL_WHITE, false);

            // ── Segmented bar ─────────────────────────────────────────────
            drawSegmentedBar(g, rx + 24, y + 12, barW, 5, disp, color);

            float flashA = ClientDietCache.flashAlpha(key);
            if (flashA > 0f) {
                int aByte = Mth.clamp(Mth.floor(flashA * panelFadeAlpha * 255f), 1, 255);
                g.fill(rx + 24, y + 12, rx + 24 + barW, y + 12 + 5, (aByte << 24) | COL_FLASH_RGB);
            }

            // ── Percentage ────────────────────────────────────────────────
            String pctStr = (int)(disp * 100) + "%";
            g.drawString(font, pctStr, leftPos + WIDTH - PAD - 30, y + 2, pctColor, false);

            // ── Trend arrow ───────────────────────────────────────────────
            if (real > prev + 0.005f)
                g.drawString(font, "↑", leftPos + WIDTH - PAD - 10, y + 2, COL_GREEN, false);
            else if (real < prev - 0.005f)
                g.drawString(font, "↓", leftPos + WIDTH - PAD - 10, y + 2, COL_RED, false);

            y += ROW_STEP;
        }

        drawLegendBar(g, rx, topPos + HEIGHT - 66, leftPos + WIDTH - PAD - rx, 34);
    }

    /** Icon tooltips at full opacity after fade-in (see {@link #FADE_TOOLTIP_THRESHOLD}). */
    private void drawDietIconTooltips(GuiGraphics g, int mx, int my) {
        int rx = leftPos + SPLIT + PAD;
        int y = topPos + 30 + 14;
        for (String key : visibleBars) {
            int bx = rx;
            int by = y;
            if (mx >= bx && mx <= bx + 20 && my >= by && my <= by + 20) {
                g.renderTooltip(font,
                        Component.translatable("nourished.screen.diet.tooltip." + key),
                        mx, my);
                return;
            }
            y += ROW_STEP;
        }
    }

    // ── Drawing helpers ──────────────────────────────────────────────────────

    private void drawRoundedBox(GuiGraphics g, int x, int y, int w, int h) {
        drawRoundedPanel(g, x, y, w, h, COL_ROW_BG, COL_BORDER_LT, COL_BORDER);
    }

    private void drawRoundedPanel(GuiGraphics g, int x, int y, int w, int h, int fill, int borderLight, int borderDark) {
        int x2 = x + w;
        int y2 = y + h;

        g.fill(x + 2, y, x2 - 2, y2, fill);
        g.fill(x, y + 2, x2, y2 - 2, fill);

        // Strong rounded corners
        g.fill(x + 1, y + 1, x + 2, y + 2, fill);
        g.fill(x2 - 2, y + 1, x2 - 1, y + 2, fill);
        g.fill(x + 1, y2 - 2, x + 2, y2 - 1, fill);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, fill);

        // Borders with rounded corner cuts
        g.fill(x + 2, y, x2 - 2, y + 1, borderLight);
        g.fill(x + 2, y2 - 1, x2 - 2, y2, borderDark);
        g.fill(x, y + 2, x + 1, y2 - 2, borderLight);
        g.fill(x2 - 1, y + 2, x2, y2 - 2, borderDark);

        g.fill(x + 1, y + 1, x + 2, y + 2, borderLight);
        g.fill(x2 - 2, y + 1, x2 - 1, y + 2, borderLight);
        g.fill(x + 1, y2 - 2, x + 2, y2 - 1, borderDark);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, borderDark);
    }

    private void drawSolidBar(GuiGraphics g, int x, int y, int w, int h, float pct, int color) {
        int filled = (int)(w * Mth.clamp(pct, 0f, 1f));
        g.fill(x, y, x + w,      y + h, COL_SEG_EMPTY);
        g.fill(x, y, x + filled, y + h, color);
    }

    private void drawSegmentedBar(GuiGraphics g, int x, int y, int w, int h, float pct, int color) {
        int segs   = 10;
        int sw     = w / segs;
        int filled = Math.round(Mth.clamp(pct, 0f, 1f) * segs);
        for (int i = 0; i < segs; i++) {
            int sx = x + i * sw;
            g.fill(sx, y, sx + sw - 1, y + h, i < filled ? color : COL_SEG_EMPTY);
        }
    }

    private void drawLegendBar(GuiGraphics g, int x, int y, int w, int h) {
        drawRoundedBox(g, x, y, w, h);
        g.drawCenteredString(font, Component.translatable("nourished.screen.diet.legend"), x + w / 2, y + 3, COL_HEADER);

        int colLeft = x + 6;
        int colW = (w - 12) / 3;
        int lineTop = y + 12;
        int lineBottom = y + h - 4;
        g.fill(colLeft + colW, lineTop, colLeft + colW + 1, lineBottom, COL_DIVIDER);
        g.fill(colLeft + colW * 2, lineTop, colLeft + colW * 2 + 1, lineBottom, COL_DIVIDER);

        drawLegendEntry(g, colLeft, y + 14, colW, COL_GREEN, "Good", "40 - 80%", 0, -3, -2, COL_GREEN);
        drawLegendEntry(g, colLeft + colW + 1, y + 14, colW, 0xFFE8C24F, "Low", "25 - 40%", 0, -3, 0, 0xFFE8C24F);
        drawLegendEntry(
                g,
                colLeft + colW * 2 - 2,
                y + 14,
                colW,
                COL_RED,
                Component.translatable("nourished.screen.diet.legend_bad").getString(),
                "0-25 / 80+",
                0,
                0,
                0,
                COL_RED
        );
    }

    private void drawLegendEntry(GuiGraphics g, int x, int y, int w, int color, String line1, String line2, int line1Offset, int line2Offset, int squareOffset, int line2Color) {
        int squareX = x + (w / 2) - 18 + squareOffset;
        g.fill(squareX, y + 1, squareX + 8, y + 9, color);
        g.fill(squareX, y + 1, squareX + 8, y + 2, COL_BORDER_LT);
        g.fill(squareX, y + 8, squareX + 8, y + 9, COL_BORDER);

        int line1X = squareX + 11 + line1Offset;
        int line2X = x + ((w - font.width(line2)) / 2) + 6 + line2Offset;
        g.drawString(font, line1, line1X, y, COL_WHITE, false);
        g.drawString(font, line2, line2X, y + 10, line2Color, false);
    }

    // ── Logic helpers ────────────────────────────────────────────────────────

    private static DietData getClientData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return mc.player.getData(DietAttachment.DIET.get());
    }

    private static String getBalanceKey(DietData data) {
        NourishedConfig config = NourishedConfig.get();
        float critical = (float) config.criticalThreshold();
        float excessThreshold = (float) config.excessThreshold();
        boolean low = data.nutrients.values().stream().anyMatch(v -> v < critical);
        boolean excess = data.nutrients.values().stream().anyMatch(v -> v > excessThreshold);
        if (low)    return "low";
        if (excess) return "excess";
        return "balanced";
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
        return switch (key) {
            case "fruits" -> COL_GREEN;
            case "vegetables" -> COL_CYAN;
            case "proteins" -> COL_RED;
            case "grains" -> COL_GOLD;
            case "sugars" -> COL_PURPLE;
            case "dairy" -> COL_WHITE;
            default -> COL_GREEN;
        };
    }

    private static int balanceColor(String key) {
        return switch (key) {
            case "balanced" -> COL_GREEN;
            case "low"      -> COL_ORANGE;
            case "excess"   -> COL_RED;
            default         -> COL_WHITE;
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
