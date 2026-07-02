package dev.maire.nourished.client.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.maire.nourished.client.hud.HudDrawHelpers;
import dev.maire.nourished.client.NourishedKeys;
import dev.marie.framework.client.MarieClientCache;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.client.MarieValueColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.api.ApiStatus;
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

@ApiStatus.Internal
public class DietScreen extends Screen {

    // ── Panel dimensions (see DietLayout) ─────────────────────────────────────
    private static final int WIDTH       = DietLayout.WIDTH;
    private static final int HEIGHT      = DietLayout.HEIGHT;
    private static final int SPLIT       = DietLayout.SPLIT;
    private static final int PAD         = DietLayout.PAD;

    // ── Right panel row geometry ─────────────────────────────────────────────
    private static final int ROW_STEP    = 26;
    private static final int BAR_H       = 9;      // nutrient bar height (+2px)

    private static final float FADE_DURATION_SEC = 0.15f;
    private static final float FADE_TOOLTIP_THRESHOLD = 0.9f;
    /** Bar fill display lerps toward server values over this duration (seconds). */
    private static final float ANIM_DURATION_SEC = 0.3f;


    // ── Colors ───────────────────────────────────────────────────────────────
    private static final int COL_BG_RGB     = 0x00141414;  // outer background #141414
    private static final int COL_PANEL_RGB  = 0x001E1E1E;  // inner panel #1E1E1E
    private static final int COL_ROW_BG_RGB = 0x001E1E1E;  // matches inner card
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
    private static final int COL_LEGEND_TEXT = 0xFFE0E0E0; // base; legend uses dimLegend() ~12% darker
    /** Warm white/yellow (RGB) for nutrient bar flash overlay; alpha applied at draw time. */
    private static final int COL_FLASH_RGB  = 0xFFFFE0;

    // ── State ────────────────────────────────────────────────────────────────
    private int leftPos, topPos;
    private final Map<String, Float>     display = new LinkedHashMap<>();
    private final List<String> visibleBars        = new ArrayList<>();
    /** Index of nutrient row whose icon is being dragged; null when not dragging. */
    private Integer dragBarFromIndex;

    private static final List<String> EDIT_VISIBLE_BARS = new ArrayList<>();

    public record ScreenBox(int screenX, int screenY, int screenW, int screenH) {}

    public record SectionBounds(ScreenBox recentMeals, ScreenBox eatMore) {}

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
        DietLayout.Layout layout = DietLayout.compute(minecraft);
        leftPos = layout.panelX();
        topPos = layout.panelY();

        visibleBars.clear();
        dragBarFromIndex = null;
        for (String key : NourishedClientConfig.get().effectiveDietBarOrder()) {
            display.putIfAbsent(key, 0f);
            visibleBars.add(key);
        }

        int closeW = DietLayout.scaledDim(118, layout.scale());
        int closeH = DietLayout.scaledDim(18, layout.scale());
        int closeX = leftPos + DietLayout.scaledDim((WIDTH - 118) / 2, layout.scale());
        int closeY = topPos + DietLayout.scaledDim(HEIGHT - 22, layout.scale());
        addRenderableWidget(
                Button.builder(
                        Component.translatable("nourished.screen.diet.close"),
                        b -> onClose()
                ).bounds(closeX, closeY, closeW, closeH).build()
        );
    }

    private DietLayout.Layout currentLayout() {
        return DietLayout.compute(minecraft);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && NourishedClientConfig.get().dietBarDragEnabled()) {
            DietLayout.Layout layout = currentLayout();
            double s = layout.scale();
            int rx = layout.panelX() + (int) Math.round((SPLIT + PAD) * s);
            int y0 = layout.panelY() + (int) Math.round(44 * s);
            int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
            int iconSize = Math.max(1, (int) Math.round(20 * s));
            for (int i = 0; i < visibleBars.size(); i++) {
                int by = y0 + i * rowStep;
                if (mouseX >= rx && mouseX <= rx + iconSize && mouseY >= by && mouseY <= by + iconSize) {
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
            DietLayout.Layout layout = currentLayout();
            double s = layout.scale();
            int y0 = layout.panelY() + (int) Math.round(44 * s);
            int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
            int to = Mth.clamp((int) ((mouseY - y0 + rowStep / 2.0) / rowStep), 0, visibleBars.size() - 1);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.EDIT_DIET_SCREEN.matches(keyCode, scanCode)) {
            DietScreenEditMode.setActive(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // intentionally empty — suppress vanilla menu blur; panel draws its own backdrop
    }

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

        TrackingData data = getClientData();

        // Animate display values toward target using dt-based lerp (~300ms convergence)
        if (data != null) {
            float animStep = dt <= 0f ? 0f : Math.min(1f, dt / ANIM_DURATION_SEC);
            for (String k : visibleBars) {
                float target = data.values.getOrDefault(k, 0f);
                float cur    = display.getOrDefault(k, 0f);
                display.put(k, cur + (target - cur) * animStep);
            }
        }

        DietLayout.Layout layout = currentLayout();
        drawPanelAt(g, minecraft, layout, data, fadeAlpha, mx, my, visibleBars);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (showIconTooltips && data != null) {
            drawDietIconTooltips(g, layout, mx, my);
        }
        super.render(g, mx, my, pt);
    }

    private void drawPanelAt(
            GuiGraphics g,
            Minecraft mc,
            DietLayout.Layout layout,
            TrackingData data,
            float panelFadeAlpha,
            int mx,
            int my,
            List<String> bars
    ) {
        leftPos = layout.panelX();
        topPos = layout.panelY();
        int localMx = layout.scale() > 0 ? (int) ((mx - layout.panelX()) / layout.scale()) : mx;
        int localMy = layout.scale() > 0 ? (int) ((my - layout.panelY()) / layout.scale()) : my;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(layout.panelX(), layout.panelY(), 0);
        pose.scale((float) layout.scale(), (float) layout.scale(), 1f);

        double bgOpacity = NourishedClientConfig.get().dietBackgroundOpacity();
        int outerFill = panelColorWithOpacity(COL_BG_RGB, bgOpacity);
        drawRoundedPanel(g, 0, 0, WIDTH, HEIGHT, outerFill, COL_BORDER_LT, COL_BORDER);

        String titleText = "☘ Diet ☘";
        int titleW = mc.font.width(titleText);
        g.drawString(mc.font, titleText, (WIDTH / 2) - (titleW / 2), 9, 0xFF9BD36A, false);

        g.fill(SPLIT, 26, SPLIT + 1, HEIGHT - 34, COL_DIVIDER);

        if (data == null) {
            g.drawCenteredString(mc.font,
                    Component.translatable("nourished.screen.diet.no_player"),
                    WIDTH / 2, HEIGHT / 2, COL_GRAY);
        } else {
            drawLeftPanel(g, mc, data, 0, 0, layout.recentMealsScale(), layout.eatMoreScale());
            drawRightPanel(g, mc, data, panelFadeAlpha, localMx, localMy, 0, 0, bars);
        }

        pose.popPose();
    }

    // ── Left panel ───────────────────────────────────────────────────────────

    private void drawLeftPanel(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            int leftPos,
            int topPos,
            double recentMealsScale,
            double eatMoreScale
    ) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        int x  = leftPos + PAD;
        int y  = topPos + 20;
        int bw = SPLIT - PAD * 2;
        int maxY = topPos + HEIGHT - PAD;

        String todayText = Component.translatable("nourished.screen.diet.today").getString();
        int todayW = mc.font.width(todayText);
        int todayGroupW = 16 + 4 + todayW;
        int todayStartX = x + (bw - todayGroupW) / 2;
        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:sunflower"))), todayStartX, y - 8);
        g.drawString(mc.font, Component.translatable("nourished.screen.diet.today"),
                todayStartX + 20, y - 4, COL_GOLD, false);
        y += 10;

        if (FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox()) {
            drawRoundedBox(g, x - 2, y - 2, bw + 4, 40);

            g.renderItem(new net.minecraft.world.item.ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:fire_charge"))),
                    x, y + 3);

            g.drawString(mc.font, Component.translatable("nourished.screen.diet.calories_label"),
                    x + 22, y + 4, COL_WHITE, false);

            String calStr = (int) data.total + " / " + (int) data.maxTotal;
            g.drawString(mc.font, calStr, x + 22, y + 15, COL_GREEN, false);

            float calPct = data.maxTotal > 0 ? Mth.clamp(data.total / data.maxTotal, 0f, 1f) : 0f;
            drawSolidBar(g, x, y + 31, bw, 4, calPct, COL_GREEN);
            y += 45;
        }

        if (cc.showBalanceBox()) {
            drawRoundedBox(g, x - 2, y - 2, bw + 4, 50);

            g.renderItem(new net.minecraft.world.item.ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:comparator"))),
                    x, y + 3);

            g.drawString(mc.font, Component.translatable("nourished.screen.diet.balance_label"),
                    x + 22, y + 4, COL_WHITE, false);

            String balKey   = getBalanceKey(data);
            int    balColor = balanceColor(balKey);
            String balText  = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();

            float balanceScale = 1.2f * (10f / 9f);
            float balTextW = mc.font.width(balText) * balanceScale;
            int bgAlpha = 51;
            int bgColor = (bgAlpha << 24) | (balColor & 0x00FFFFFF);
            g.fill(x + 22 - 2, y + 14, x + 22 + (int) balTextW + 3, y + 25, bgColor);

            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x + 22, y + 15, 0);
            pose.scale(balanceScale, balanceScale, 1f);
            g.drawString(mc.font, balText, 0, 0, balColor, false);
            pose.popPose();

            float balScore = MarieClientCache.getBalanceScore();
            int filledPips = Math.round(balScore * 5);
            int pipTotalW = 5 * 10 + 4 * 3;
            int pipStartX = x + (bw - pipTotalW) / 2;
            for (int i = 0; i < 5; i++) {
                int px = pipStartX + i * 13;
                g.fill(px, y + 38, px + 10, y + 44, i < filledPips ? balColor : COL_SEG_EMPTY);
            }
            y += 55;
        }

        List<String> recentIds = MarieClientCache.getRecentSourceIds();
        if (cc.showRecentMeals() && !recentIds.isEmpty()) {
            int rowH = Math.max(1, (int) Math.round(14 * recentMealsScale));
            int recentHeight = 10 + (Math.min(3, recentIds.size()) * rowH);
            if (y + recentHeight <= maxY) {
                drawRoundedBox(g, x - 2, y - 2, bw + 4, recentHeight + 4);
                g.drawString(mc.font, Component.translatable("nourished.screen.diet.recent_label"),
                        x, y, COL_HEADER, false);
                y += 10;

                int count = 0;
                float iconScale = 0.75f * (float) recentMealsScale;
                int nameOffset = (int) Math.round(16 * recentMealsScale);
                for (String id : recentIds) {
                    if (count >= 3) break;
                    count++;

                    ResourceLocation itemId = ResourceLocation.tryParse(id);
                    if (itemId == null) {
                        continue;
                    }
                    ItemStack recent = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
                    PoseStack poseRecent = g.pose();
                    poseRecent.pushPose();
                    poseRecent.translate(x, y, 0);
                    poseRecent.scale(iconScale, iconScale, 1f);
                    g.renderItem(recent, 0, 0);
                    poseRecent.popPose();

                    String name = recent.getHoverName().getString();
                    String truncated = mc.font.plainSubstrByWidth(name, bw - nameOffset);
                    Map<String, Float> bars = NutrientClassificationLookup.resolveBars(recent.getItem());
                    String nutrientKey = bars.entrySet().stream()
                            .max(Comparator.comparingDouble(entry -> entry.getValue()))
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    int nameColor = nutrientKey != null
                            ? MarieValueColors.baseColorArgb(nutrientKey)
                            : COL_WHITE;
                    g.drawString(mc.font, truncated, x + nameOffset, y + 3, nameColor, false);
                    y += rowH;
                }
                y += 4;
            }
        }

        List<String> neglected = MarieClientCache.getNeglectedCategories();
        if (cc.showEatMoreOf() && !neglected.isEmpty()) {
            int eatBoxH = Math.max(1, (int) Math.round(46 * eatMoreScale));
            if (y + eatBoxH <= maxY) {
                drawRoundedBox(g, x - 2, y - 2, bw + 4, eatBoxH);
                String suggestionHeader = Component.translatable("nourished.screen.diet.suggestion_label").getString();
                g.drawString(mc.font, mc.font.plainSubstrByWidth(suggestionHeader, bw),
                        x, y, COL_HEADER, false);
                y += 10;

                for (int col = 0; col < Math.min(2, neglected.size()); col++) {
                    String categoryKey = neglected.get(col);
                    TagKey<Item> tag = TagKey.create(Registries.ITEM,
                            ResourceLocation.parse(Nourished.MODID + ":nutrients/" + categoryKey));
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
                }
                y += eatBoxH - 10 + 4;
            }
        }

        if (cc.showActiveEffects()) {
            Minecraft playerMc = mc;
            int effectCount = (playerMc.player != null) ? playerMc.player.getActiveEffects().size() : 0;
            int effectsHeight = 10 + (Math.min(3, effectCount) * 9);
            if (y + effectsHeight <= maxY) {
                drawActiveEffects(g, mc, x, y, bw, maxY);
            }
        }
    }

    private void drawActiveEffects(GuiGraphics g, Minecraft mc, int x, int y, int bw, int maxY) {
        if (mc.player == null) return;

        g.drawString(mc.font, Component.translatable("nourished.screen.diet.effects_label"),
                x, y, COL_HEADER, false);
        y += 10;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) {
            g.drawString(mc.font, Component.translatable("nourished.screen.diet.effects_none"),
                    x, y, COL_GRAY, false);
            return;
        }

        int count = 0;
        for (MobEffectInstance effect : effects) {
            if (count >= 3) break;
            if (y + 9 > maxY) break;
            MobEffect type = effect.getEffect().value();
            String name = Component.translatable(type.getDescriptionId()).getString();
            int amplifier = effect.getAmplifier();
            String label = (amplifier > 0 ? name + " " + (amplifier + 1) : name);
            int color = type.isBeneficial() ? COL_GREEN : COL_RED;
            String prefix = type.isBeneficial() ? "+ " : "- ";
            g.drawString(mc.font, prefix + label, x, y, color, false);
            y += 9;
            count++;
        }
    }

    // ── Right panel ──────────────────────────────────────────────────────────

    private void drawRightPanel(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            float panelFadeAlpha,
            int mx,
            int my,
            int leftPos,
            int topPos,
            List<String> bars
    ) {
        int rx = leftPos + SPLIT + PAD;
        int y  = topPos + 30;

        String hdr    = Component.translatable("nourished.screen.diet.intake").getString();
        int    hdrW   = mc.font.width(hdr);
        int    hdrCX  = rx + (WIDTH - SPLIT - PAD * 2) / 2;
        g.drawString(mc.font, "✧✧", rx + 2, y, COL_HEADER, false);
        g.drawString(mc.font, "✧✧", leftPos + WIDTH - PAD - 14, y, COL_HEADER, false);
        g.drawString(mc.font, hdr, hdrCX - hdrW / 2, y, COL_HEADER, false);
        int lineY = y + 4;
        g.fill(rx,                        lineY, hdrCX - hdrW / 2 - 3, lineY + 1, COL_BORDER_LT);
        g.fill(hdrCX + hdrW / 2 + 3,     lineY, leftPos + WIDTH - PAD, lineY + 1, COL_BORDER_LT);
        y += 14;

        int arrowSlot = 10;
        int arrowLeft = leftPos + WIDTH - PAD - arrowSlot;
        int pctColumnRight = arrowLeft - 4;
        int maxPctW = mc.font.width("100%");
        int barLeft = rx + 24;
        int barW = Math.max(0, pctColumnRight - maxPctW - 4 - barLeft);

        for (String key : bars) {
            float disp  = display.getOrDefault(key, data.values.getOrDefault(key, 0f));
            float real  = data.values.getOrDefault(key, 0f);
            float prev  = data.lastValues.getOrDefault(key, real);
            int   color = barColor(key, disp);
            int   pctColor = nutrientBaseColor(key);

            int rowRight = leftPos + WIDTH - PAD;
            if (my >= y - 2 && my < y - 2 + ROW_STEP && mx >= rx - 2 && mx < rowRight) {
                g.fill(rx - 2, y - 2, rowRight, y - 2 + ROW_STEP, 0x0CFFFFFF);
            }

            drawRoundedBox(g, rx - 2, y - 2, rowRight - (rx - 2), 26);

            int bx = rx;
            int by = y;
            drawRoundedPanel(
                    g,
                    bx,
                    by,
                    20,
                    20,
                    panelColorWithOpacity(COL_PANEL_RGB, NourishedClientConfig.get().dietBackgroundOpacity()),
                    COL_BORDER_LT,
                    COL_BORDER
            );
            String iconId = NutrientRegistry.getIcon(key);
            Item iconItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(iconId)).orElse(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:apple")));
            g.renderItem(new ItemStack(iconItem), bx + 2, by + 2);

            g.drawString(mc.font,
                    NutrientRegistry.getLabelComponent(key),
                    rx + 24, y + 2, COL_WHITE, false);

            drawSegmentedBar(g, rx + 24, y + 12, barW, BAR_H, disp, color);

            float flashA = MarieClientCache.flashAlpha(key);
            if (flashA > 0f) {
                int aByte = Mth.clamp(Mth.floor(flashA * panelFadeAlpha * 255f), 1, 255);
                g.fill(rx + 24, y + 12, rx + 24 + barW, y + 12 + BAR_H, (aByte << 24) | COL_FLASH_RGB);
            }

            String pctStr = Math.round(disp * 100) + "%";
            int pctX = pctColumnRight - mc.font.width(pctStr);
            int dimmedPct = (pctColor & 0x00FFFFFF) | 0x99000000;
            g.drawString(mc.font, pctStr, pctX, y + 2, dimmedPct, false);

            if (real > prev + 0.005f)
                g.drawString(mc.font, "↑", arrowLeft, y + 2, COL_GREEN, false);
            else if (real < prev - 0.005f)
                g.drawString(mc.font, "↓", arrowLeft, y + 2, COL_RED, false);

            y += ROW_STEP;
        }

        drawLegendBar(g, mc, rx, topPos + HEIGHT - 66, leftPos + WIDTH - PAD - rx, 34);
    }

    private void drawDietIconTooltips(GuiGraphics g, DietLayout.Layout layout, int mx, int my) {
        double s = layout.scale();
        int rx = layout.panelX() + (int) Math.round((SPLIT + PAD) * s);
        int y = layout.panelY() + (int) Math.round((30 + 14) * s);
        int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
        int iconSize = Math.max(1, (int) Math.round(20 * s));
        for (String key : visibleBars) {
            if (mx >= rx && mx <= rx + iconSize && my >= y && my <= y + iconSize) {
                g.renderTooltip(font,
                        Component.translatable("nourished.screen.diet.tooltip." + key),
                        mx, my);
                return;
            }
            y += rowStep;
        }
    }

    // ── Drawing helpers ──────────────────────────────────────────────────────

    private void drawRoundedBox(GuiGraphics g, int x, int y, int w, int h) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, NourishedClientConfig.get().dietBackgroundOpacity());
        drawRoundedPanel(g, x, y, w, h, fill, COL_BORDER_LT, COL_BORDER);
    }

    private static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
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

    private void drawLegendBar(GuiGraphics g, Minecraft mc, int x, int y, int w, int h) {
        drawRoundedBox(g, x, y, w, h);
        g.drawCenteredString(mc.font, Component.translatable("nourished.screen.diet.legend"), x + w / 2, y + 3, dimLegend(COL_HEADER));

        int colLeft = x + 6;
        int colW = (w - 12) / 3;
        int lineTop = y + 12;
        int lineBottom = y + h - 4;
        g.fill(colLeft + colW, lineTop, colLeft + colW + 1, lineBottom, COL_DIVIDER);
        g.fill(colLeft + colW * 2, lineTop, colLeft + colW * 2 + 1, lineBottom, COL_DIVIDER);

        drawLegendEntry(g, mc, colLeft, y + 14, colW, dimLegend(COL_GREEN), "Good", "40 - 80%", 0, -3, -2, dimLegend(COL_GREEN));
        drawLegendEntry(g, mc, colLeft + colW + 1, y + 14, colW, dimLegend(0xFFE8C24F), "Low", "25 - 40%", 0, -3, 0, dimLegend(0xFFE8C24F));
        drawLegendEntry(
                g,
                mc,
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

    private void drawLegendEntry(GuiGraphics g, Minecraft mc, int x, int y, int w, int color, String line1, String line2, int line1Offset, int line2Offset, int squareOffset, int line2Color) {
        int squareX = x + (w / 2) - 18 + squareOffset;
        g.fill(squareX, y + 1, squareX + 8, y + 9, color);
        g.fill(squareX, y + 1, squareX + 8, y + 2, COL_BORDER_LT);
        g.fill(squareX, y + 8, squareX + 8, y + 9, COL_BORDER);

        int line1X = squareX + 11 + line1Offset;
        int line2X = x + ((w - mc.font.width(line2)) / 2) + 6 + line2Offset;
        g.drawString(mc.font, line1, line1X, y, dimLegend(COL_LEGEND_TEXT), false);
        g.drawString(mc.font, line2, line2X, y + 10, line2Color, false);
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

    private static TrackingData getClientData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return MarieClientCache.get();
    }

    private static String getBalanceKey(TrackingData data) {
        NourishedConfig config = NourishedConfig.get();
        float critical = (float) config.criticalThreshold();
        float excessThreshold = (float) config.excessThreshold();
        boolean low = data.values.values().stream().anyMatch(v -> v < critical);
        boolean excess = data.values.values().stream().anyMatch(v -> v > excessThreshold);
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
        return MarieValueColors.baseColorArgb(key);
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

    private static final DietScreen EDIT_DRAW = new DietScreen();

    public static SectionBounds computeSectionBounds(DietLayout.Layout layout) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        TrackingData data = getClientData();
        if (data == null) {
            return new SectionBounds(null, null);
        }

        int y = 20 + 10;
        int bw = SPLIT - PAD * 2;
        int maxY = HEIGHT - PAD;

        if (FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox()) {
            y += 45;
        }
        if (cc.showBalanceBox()) {
            y += 55;
        }

        ScreenBox recent = null;
        List<String> recentIds = MarieClientCache.getRecentSourceIds();
        if (cc.showRecentMeals() && !recentIds.isEmpty()) {
            int rowH = Math.max(1, (int) Math.round(14 * layout.recentMealsScale()));
            int recentContentH = 10 + Math.min(3, recentIds.size()) * rowH;
            int boxLocalH = recentContentH + 4;
            if (y + boxLocalH <= maxY) {
                int localX = PAD - 2;
                int localY = y - 2;
                recent = new ScreenBox(
                        DietLayout.toScreenX(layout, localX),
                        DietLayout.toScreenY(layout, localY),
                        DietLayout.toScreenDim(layout, bw + 4),
                        DietLayout.toScreenDim(layout, boxLocalH)
                );
                y += boxLocalH;
            }
        }

        ScreenBox eatMore = null;
        List<String> neglected = MarieClientCache.getNeglectedCategories();
        if (cc.showEatMoreOf() && !neglected.isEmpty()) {
            int eatBoxH = Math.max(1, (int) Math.round(46 * layout.eatMoreScale()));
            if (y + eatBoxH <= maxY) {
                int localX = PAD - 2;
                int localY = y - 2;
                eatMore = new ScreenBox(
                        DietLayout.toScreenX(layout, localX),
                        DietLayout.toScreenY(layout, localY),
                        DietLayout.toScreenDim(layout, bw + 4),
                        DietLayout.toScreenDim(layout, eatBoxH)
                );
            }
        }

        return new SectionBounds(recent, eatMore);
    }

    public static void renderForEditScreen(GuiGraphics g, Minecraft mc) {
        TrackingData data = getClientData();
        DietLayout.Layout layout = DietScreenEditController.previewLayout(mc);

        EDIT_VISIBLE_BARS.clear();
        EDIT_VISIBLE_BARS.addAll(NourishedClientConfig.get().effectiveDietBarOrder());

        int panelX = layout.panelX();
        int panelY = layout.panelY();
        int panelW = layout.panelW();
        int panelH = layout.panelH();

        double s = mc.getWindow().getGuiScale();
        int mx = (int) (mc.mouseHandler.xpos() / s);
        int my = (int) (mc.mouseHandler.ypos() / s);

        boolean hovered = mx >= panelX && my >= panelY && mx < panelX + panelW && my < panelY + panelH;
        boolean active = DietScreenEditController.isDietDragging()
                || DietScreenEditController.isDietResizing()
                || DietScreenEditController.isRecentMealsResizing()
                || DietScreenEditController.isEatMoreResizing();

        g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, HudDrawHelpers.editOverlayColor());
        if (hovered || active) {
            HudDrawHelpers.drawBorder(g, panelX - 1, panelY - 1, panelW + 2, panelH + 2, 1, HudDrawHelpers.hoverBorderColor());
        }
        if (DietScreenEditController.isDietResizing()) {
            HudDrawHelpers.drawDashedBorder(g, panelX - 2, panelY - 2, panelW + 4, panelH + 4, HudDrawHelpers.dashedPreviewColor());
        }

        EDIT_DRAW.drawPanelAt(g, mc, layout, data, 1f, mx, my, EDIT_VISIBLE_BARS);

        boolean panelHandleHovered = HudDrawHelpers.isOverResizeHandle(mx, my, panelX, panelY, panelW, panelH);
        HudDrawHelpers.drawResizeHandle(
                g, mc, panelX, panelY, panelW, panelH,
                panelHandleHovered, DietScreenEditController.isDietResizing(), mx, my
        );

        SectionBounds sections = computeSectionBounds(layout);
        if (sections.recentMeals() != null) {
            ScreenBox box = sections.recentMeals();
            boolean handleHovered = HudDrawHelpers.isOverResizeHandle(mx, my, box.screenX(), box.screenY(), box.screenW(), box.screenH());
            HudDrawHelpers.drawResizeHandle(
                    g, mc, box.screenX(), box.screenY(), box.screenW(), box.screenH(),
                    handleHovered, DietScreenEditController.isRecentMealsResizing(), mx, my
            );
        }
        if (sections.eatMore() != null) {
            ScreenBox box = sections.eatMore();
            boolean handleHovered = HudDrawHelpers.isOverResizeHandle(mx, my, box.screenX(), box.screenY(), box.screenW(), box.screenH());
            HudDrawHelpers.drawResizeHandle(
                    g, mc, box.screenX(), box.screenY(), box.screenW(), box.screenH(),
                    handleHovered, DietScreenEditController.isEatMoreResizing(), mx, my
            );
        }

        if (DietScreenEditController.isDietResizing()) {
            String scaleText = String.format("Scale: %.1fx", layout.scale());
            int textX = panelX + Math.max(0, (panelW - mc.font.width(scaleText)) / 2);
            g.drawString(mc.font, scaleText, textX, panelY + panelH + 6, HudDrawHelpers.handleActiveColor(), false);
        }
    }
}
