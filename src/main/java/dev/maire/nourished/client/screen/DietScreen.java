package dev.maire.nourished.client.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DietScreen extends Screen {

    // ── Panel dimensions ─────────────────────────────────────────────────────
    private static final int WIDTH       = 316;
    private static final int HEIGHT      = 248;
    private static final int SPLIT       = 108;    // left panel width
    private static final int PAD         = 10;

    // ── Right panel row geometry ─────────────────────────────────────────────
    private static final int ROW_STEP    = 22;
    private static final int BAR_H       = 9;      // nutrient bar height (+2px)

    private static final float FADE_DURATION_SEC = 0.15f;
    private static final float FADE_TOOLTIP_THRESHOLD = 0.9f;
    /** Bar fill display lerps toward server values over this duration (seconds). */
    private static final float ANIM_DURATION_SEC = 0.3f;


    // ── Colors ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xFF141414;  // outer background #141414
    private static final int COL_PANEL      = 0xFF1E1E1E;  // inner panel #1E1E1E
    private static final int COL_BORDER     = 0xFF3A3A3A;
    private static final int COL_BORDER_LT  = 0xFF555555;
    private static final int COL_SEG_EMPTY  = 0xFF2A2A2A;  // empty bar bg
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
    private static final int COL_ROW_BG     = 0xFF1E1E1E;  // matches inner card
    private static final int COL_LEGEND_TEXT = 0xFFE0E0E0; // base; legend uses dimLegend() ~12% darker
    /** Warm white/yellow (RGB) for nutrient bar flash overlay; alpha applied at draw time. */
    private static final int COL_FLASH_RGB  = 0xFFFFE0;

    // ── State ────────────────────────────────────────────────────────────────
    private int leftPos, topPos;
    private final Map<String, Float>     display = new LinkedHashMap<>();
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

        // Animate display values toward target using dt-based lerp (~300ms convergence)
        if (data != null) {
            float animStep = dt <= 0f ? 0f : Math.min(1f, dt / ANIM_DURATION_SEC);
            for (String k : visibleBars) {
                float target = data.nutrients.getOrDefault(k, 0f);
                float cur    = display.getOrDefault(k, 0f);
                display.put(k, cur + (target - cur) * animStep);
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
            drawRightPanel(g, data, fadeAlpha, mx, my);
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
        // Hard floor for left-panel content; sections that would overflow are skipped wholesale.
        int maxY = topPos + HEIGHT - PAD;

        // "Today" header
        String todayText = Component.translatable("nourished.screen.diet.today").getString();
        int todayW = font.width(todayText);
        int todayGroupW = 16 + 4 + todayW;
        int todayStartX = x + (bw - todayGroupW) / 2;
        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:sunflower"))), todayStartX, y - 8);
        g.drawString(font, Component.translatable("nourished.screen.diet.today"),
                todayStartX + 20, y - 4, COL_GOLD, false);
        y += 10;

        if (ModuleCache.enableCalorieTracking) {
            // ── Calories box ──────────────────────────────────────────────
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
        }

        // ── Balance box ───────────────────────────────────────────────────
        drawRoundedBox(g, x - 2, y - 2, bw + 4, 50);

        g.renderItem(new net.minecraft.world.item.ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:comparator"))),
                x, y + 3);

        g.drawString(font, Component.translatable("nourished.screen.diet.balance_label"),
                x + 22, y + 4, COL_WHITE, false);

        String balKey   = getBalanceKey(data);
        int    balColor = balanceColor(balKey);
        String balText  = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();

        // ~+1pt over prior 1.2x scale; subtle colored bg at ~20% alpha matching state
        float balanceScale = 1.2f * (10f / 9f);
        float balTextW = font.width(balText) * balanceScale;
        int bgAlpha = 51; // ~20% of 255
        int bgColor = (bgAlpha << 24) | (balColor & 0x00FFFFFF);
        g.fill(x + 22 - 2, y + 14, x + 22 + (int) balTextW + 3, y + 25, bgColor);

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + 22, y + 15, 0);
        pose.scale(balanceScale, balanceScale, 1f);
        g.drawString(font, balText, 0, 0, balColor, false);
        pose.popPose();

        float balScore = ClientDietCache.getBalanceScore();
        // 5 pips: 10px wide, 6px tall, 3px gap -> total 62px, centered in bw
        int filledPips = Math.round(balScore * 5);
        int pipTotalW = 5 * 10 + 4 * 3; // 62
        int pipStartX = x + (bw - pipTotalW) / 2;
        for (int i = 0; i < 5; i++) {
            int px = pipStartX + i * 13;
            g.fill(px, y + 38, px + 10, y + 44, i < filledPips ? balColor : COL_SEG_EMPTY);
        }
        y += 55;

        List<String> recentIds = ClientDietCache.getRecentFoodIds();
        if (!recentIds.isEmpty()) {
            int recentHeight = 10 + (recentIds.size() * 14);
            if (y + recentHeight <= maxY) {
                g.drawString(font, Component.translatable("nourished.screen.diet.recent_label"),
                        x, y, COL_HEADER, false);
                y += 10;

                for (String id : recentIds) {
                    ItemStack recent = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
                    PoseStack poseRecent = g.pose();
                    poseRecent.pushPose();
                    poseRecent.translate(x, y, 0);
                    poseRecent.scale(0.75f, 0.75f, 1f);
                    g.renderItem(recent, 0, 0);
                    poseRecent.popPose();

                    String name = recent.getHoverName().getString();
                    String truncated = font.plainSubstrByWidth(name, bw - 18);
                    g.drawString(font, truncated, x + 16, y + 3, COL_WHITE, false);
                    y += 14;
                }
            }
        }

        List<String> neglected = ClientDietCache.getNeglectedCategories();
        if (!neglected.isEmpty()) {
            int suggestionHeight = 50;
            if (y + suggestionHeight <= maxY) {
                drawRoundedBox(g, x - 2, y - 2, bw + 4, 46);
                String suggestionHeader = Component.translatable("nourished.screen.diet.suggestion_label").getString();
                g.drawString(font, font.plainSubstrByWidth(suggestionHeader, bw),
                        x, y, COL_HEADER, false);
                y += 10;

                for (int col = 0; col < Math.min(2, neglected.size()); col++) {
                    String categoryKey = neglected.get(col);
                    TagKey<Item> tag = TagKey.create(Registries.ITEM,
                            ResourceLocation.parse("nourished:nutrients/" + categoryKey));
                    Item exampleItem = null;
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (item.builtInRegistryHolder().is(tag)) {
                            exampleItem = item;
                            break;
                        }
                    }
                    if (exampleItem == null) continue;

                    int suggestionColW = (bw - 4) / 2;
                    int colX = x + col * suggestionColW;
                    g.renderItem(new ItemStack(exampleItem), colX, y);
                    String label = Component.translatable("nourished.screen.diet.bar." + categoryKey).getString();
                    int labelMaxWidth = Math.max(0, suggestionColW - 20);
                    g.drawString(font,
                            font.plainSubstrByWidth(label, labelMaxWidth),
                            colX + 18,
                            y + 4,
                            NutrientUiColors.baseColorArgb(categoryKey),
                            false);
                }
                y += 36 + 4;
            }
        }

        // ── Active effects ────────────────────────────────────────────────
        Minecraft mc = Minecraft.getInstance();
        int effectCount = (mc.player != null) ? mc.player.getActiveEffects().size() : 0;
        int effectsHeight = 10 + (Math.min(3, effectCount) * 9);
        if (y + effectsHeight <= maxY) {
            drawActiveEffects(g, x, y, bw, maxY);
        }

    }

    private void drawActiveEffects(GuiGraphics g, int x, int y, int bw, int maxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        g.drawString(font, Component.translatable("nourished.screen.diet.effects_label"),
                x, y, COL_HEADER, false);
        y += 10;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) {
            g.drawString(font, Component.translatable("nourished.screen.diet.effects_none"),
                    x, y, COL_GRAY, false);
            return;
        }

        int count = 0;
        for (MobEffectInstance effect : effects) {
            if (count >= 3) break; // cap at 3 to avoid overflow
            if (y + 9 > maxY) break; // stop if next line would clip past the panel
            MobEffect type = effect.getEffect().value();
            String name = Component.translatable(type.getDescriptionId()).getString();
            int amplifier = effect.getAmplifier();
            String label = (amplifier > 0 ? name + " " + (amplifier + 1) : name);
            int color = type.isBeneficial() ? COL_GREEN : COL_RED;
            String prefix = type.isBeneficial() ? "+ " : "- ";
            g.drawString(font, prefix + label, x, y, color, false);
            y += 9;
            count++;
        }
    }

    // ── Right panel ──────────────────────────────────────────────────────────

    private void drawRightPanel(GuiGraphics g, DietData data, float panelFadeAlpha, int mx, int my) {
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

        // Trend arrow slot; percentages share a fixed-width column with 4px gap after bar end
        int arrowSlot = 10;
        int arrowLeft = leftPos + WIDTH - PAD - arrowSlot;
        int pctColumnRight = arrowLeft - 4;
        int maxPctW = font.width("100%");
        int barLeft = rx + 24;
        int barW = Math.max(0, pctColumnRight - maxPctW - 4 - barLeft);

        for (String key : visibleBars) {
            float disp  = display.getOrDefault(key, 0f);
            float real  = data.nutrients.getOrDefault(key, 0f);
            float prev  = data.lastNutrients.getOrDefault(key, real);
            int   color = barColor(key, disp);
            int   pctColor = nutrientBaseColor(key);

            // ── Row hover highlight ────────────────────────────────────────
            int rowRight = leftPos + WIDTH - PAD;
            if (my >= y - 2 && my < y - 2 + ROW_STEP && mx >= rx - 2 && mx < rowRight) {
                // #FFFFFF at 5% alpha across full row
                g.fill(rx - 2, y - 2, rowRight, y - 2 + ROW_STEP, 0x0CFFFFFF);
            }

            // ── Row background ────────────────────────────────────────────
            drawRoundedBox(g, rx - 2, y - 2, rowRight - (rx - 2), 22);

            // ── Icon box 20x20 with 1px border ───────────────────────────
            int bx = rx;
            int by = y;
            drawRoundedPanel(g, bx, by, 20, 20, COL_PANEL, COL_BORDER_LT, COL_BORDER);
            String iconId = NutrientRegistry.getIcon(key);
            Item iconItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(iconId)).orElse(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:apple")));
            g.renderItem(new ItemStack(iconItem), bx + 2, by + 2);

            // ── Nutrient name ─────────────────────────────────────────────
            g.drawString(font,
                    Component.translatable("nourished.screen.diet.bar." + key),
                    rx + 24, y + 2, COL_WHITE, false);

            // ── Segmented bar (BAR_H = 7) ─────────────────────────────────
            drawSegmentedBar(g, rx + 24, y + 12, barW, BAR_H, disp, color);

            float flashA = ClientDietCache.flashAlpha(key);
            if (flashA > 0f) {
                int aByte = Mth.clamp(Mth.floor(flashA * panelFadeAlpha * 255f), 1, 255);
                g.fill(rx + 24, y + 12, rx + 24 + barW, y + 12 + BAR_H, (aByte << 24) | COL_FLASH_RGB);
            }

            // ── Percentage (right-aligned in column: 4px after bar end) ────
            String pctStr = Math.round(disp * 100) + "%";
            int pctX = pctColumnRight - font.width(pctStr);
            int dimmedPct = (pctColor & 0x00FFFFFF) | 0x99000000;
            g.drawString(font, pctStr, pctX, y + 2, dimmedPct, false);

            // ── Trend arrow ───────────────────────────────────────────────
            if (real > prev + 0.005f)
                g.drawString(font, "↑", arrowLeft, y + 2, COL_GREEN, false);
            else if (real < prev - 0.005f)
                g.drawString(font, "↓", arrowLeft, y + 2, COL_RED, false);

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
        g.drawCenteredString(font, Component.translatable("nourished.screen.diet.legend"), x + w / 2, y + 3, dimLegend(COL_HEADER));

        int colLeft = x + 6;
        int colW = (w - 12) / 4;
        int lineTop = y + 12;
        int lineBottom = y + h - 4;
        g.fill(colLeft + colW, lineTop, colLeft + colW + 1, lineBottom, COL_DIVIDER);
        g.fill(colLeft + colW * 2, lineTop, colLeft + colW * 2 + 1, lineBottom, COL_DIVIDER);
        g.fill(colLeft + colW * 3, lineTop, colLeft + colW * 3 + 1, lineBottom, COL_DIVIDER);

        drawLegendEntry(g, colLeft, y + 14, colW, dimLegend(COL_GREEN), "Good", "40 - 80%", 0, -3, -2, dimLegend(COL_GREEN));
        drawLegendEntry(g, colLeft + colW + 1, y + 14, colW, dimLegend(0xFFE8C24F), "Low", "25 - 40%", 0, -3, 0, dimLegend(0xFFE8C24F));
        drawLegendEntry(
                g,
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
        drawLegendEntry(
                g,
                colLeft + colW * 3 - 2,
                y + 14,
                colW,
                dimLegend(COL_ORANGE),
                Component.translatable("nourished.screen.diet.legend_excess").getString(),
                Component.translatable("nourished.screen.diet.legend_excess_range").getString(),
                0,
                0,
                0,
                dimLegend(COL_ORANGE)
        );
    }

    private void drawLegendEntry(GuiGraphics g, int x, int y, int w, int color, String line1, String line2, int line1Offset, int line2Offset, int squareOffset, int line2Color) {
        int squareX = x + (w / 2) - 18 + squareOffset;
        g.fill(squareX, y + 1, squareX + 8, y + 9, color);
        g.fill(squareX, y + 1, squareX + 8, y + 2, COL_BORDER_LT);
        g.fill(squareX, y + 8, squareX + 8, y + 9, COL_BORDER);

        int line1X = squareX + 11 + line1Offset;
        int line2X = x + ((w - font.width(line2)) / 2) + 6 + line2Offset;
        g.drawString(font, line1, line1X, y, dimLegend(COL_LEGEND_TEXT), false);
        g.drawString(font, line2, line2X, y + 10, line2Color, false);
    }

    /** ~12% darker legend text and swatches (brightness reduction). */
    private static int dimLegend(int argb) {
        float f = 0.88f;
        int a = (argb >>> 24) & 0xFF;
        int r = Mth.clamp((int) (((argb >> 16) & 0xFF) * f), 0, 255);
        int g = Mth.clamp((int) (((argb >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp((int) ((argb & 0xFF) * f), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ── Logic helpers ────────────────────────────────────────────────────────

    private static DietData getClientData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return ClientDietCache.get();
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
        return NutrientUiColors.baseColorArgb(key);
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
