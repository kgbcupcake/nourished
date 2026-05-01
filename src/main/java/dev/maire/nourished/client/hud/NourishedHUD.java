package dev.maire.nourished.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.maire.nourished.Nourished;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.config.HudAnchor;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Nourished.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class NourishedHUD {

    // ── Layout constants ─────────────────────────────────────────────────────

    private static final int BAR_H = 5;
    private static final int ROW_GAP = 4;
    private static final int PANEL_PAD = 8;
    private static final int ICON_LABEL_GAP = 4;
    private static final int LABEL_BAR_GAP = 4;
    private static final int BAR_PCT_GAP = 4;
    private static final float BASE_LABEL_SCALE = 6f / 9f;
    private static final int MARGIN = 6;
    private static final int PCT_MAX_CHARS = 4; // "100%"

    // ── Colors ───────────────────────────────────────────────────────────────

    private static final int COL_PANEL_BG     = 0xCC101010; // ~80% alpha
    private static final int COL_BAR_BG       = 0x99111111; // ~60% alpha
    private static final int COL_LABEL        = 0xFFAAAAAA;
    private static final int COL_PCT_GOOD     = 0xFF55FF55;
    private static final int COL_PCT_LOW      = 0xFFFFAA00;
    private static final int COL_PCT_CRIT     = 0xFFFF5555;

    // Edit mode
    private static final int COL_EDIT_OVERLAY = 0x99000000;
    private static final int COL_HOVER_BORDER = 0xFFFFFFAA;
    private static final int COL_EDIT_BANNER  = 0xFFFFFFFF;
    private static final int COL_EDIT_BANNER_BG = 0xCC000000;

    // Nutrient accent colors
    private static final int COL_GREEN  = 0xFF55FF55;
    private static final int COL_CYAN   = 0xFF4DD9D9;
    private static final int COL_RED    = 0xFFFF5555;
    private static final int COL_GOLD   = 0xFFFFD65C;
    private static final int COL_PURPLE = 0xFFA95FFF;
    private static final int COL_WHITE  = 0xFFEEEEEE;

    // ── Animation state ──────────────────────────────────────────────────────

    private static final Map<String, Float> displayValues = new HashMap<>();
    private static long lastNano = 0;

    // ── Drag state ───────────────────────────────────────────────────────────

    private static boolean hudDragging;
    private static int dragGrabOffsetX;
    private static int dragGrabOffsetY;
    private static int dragAnchorBaseX;
    private static int dragAnchorBaseY;

    // ── Icon cache ───────────────────────────────────────────────────────────

    private static final Map<String, ItemStack> iconCache = new HashMap<>();

    private NourishedHUD() {}

    // ── Render ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!NourishedConfig.get().enableHUD()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            hudDragging = false;
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) return;

        DietData data = player.getData(DietAttachment.DIET.get());
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) return;

        // Advance lerp animation
        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0f : (now - lastNano) / 1_000_000_000f;
        lastNano = now;
        dt = Math.min(dt, 0.1f);
        float lerpStep = dt <= 0 ? 1f : Math.min(1f, dt / 0.2f);
        for (String key : keys) {
            float target = data.nutrients.getOrDefault(key, 0f);
            float cur = displayValues.getOrDefault(key, target);
            displayValues.put(key, cur + (target - cur) * lerpStep);
        }

        Layout layout = computeLayout(mc, keys);

        int panelX = layout.panelX;
        int panelY = layout.panelY;
        if (hudDragging && HUDEditMode.isActive) {
            int[] m = scaledMouse(mc);
            panelX = Mth.clamp(m[0] - dragGrabOffsetX, 0, Math.max(0, mc.getWindow().getGuiScaledWidth() - layout.panelW));
            panelY = Mth.clamp(m[1] - dragGrabOffsetY, 0, Math.max(0, mc.getWindow().getGuiScaledHeight() - layout.panelH));
        }

        GuiGraphics g = event.getGuiGraphics();

        // Edit mode: dark overlay + hover border behind panel
        if (HUDEditMode.isActive) {
            int[] m = scaledMouse(mc);
            boolean hovered = m[0] >= panelX && m[1] >= panelY
                    && m[0] < panelX + layout.panelW && m[1] < panelY + layout.panelH;

            g.fill(panelX - 2, panelY - 2, panelX + layout.panelW + 2, panelY + layout.panelH + 2, COL_EDIT_OVERLAY);

            if (hovered || hudDragging) {
                drawBorder(g, panelX - 1, panelY - 1, layout.panelW + 2, layout.panelH + 2, 1, COL_HOVER_BORDER);
            }

            // Top-center banner
            drawEditBanner(g, mc);
        }

        // Panel background
        drawRoundedRect(g, panelX, panelY, layout.panelW, layout.panelH, 2, COL_PANEL_BG);

        // Rows
        int contentX = panelX + layout.scaledPad;
        int y = panelY + layout.scaledPad;

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            float displayPct = displayValues.getOrDefault(key, 0f);
            float truePct = data.nutrients.getOrDefault(key, 0f);

            int iconSize = layout.iconSize;
            int rowH = layout.rowH;
            int rowCenterY = y + rowH / 2;

            // Icon
            renderIcon(g, mc, key, contentX, rowCenterY - iconSize / 2, iconSize, layout.scale);

            // Label
            int labelX = contentX + iconSize + ICON_LABEL_GAP;
            int labelY = rowCenterY - (int) Math.ceil(9 * layout.labelScale) / 2;
            drawScaledLabel(g, mc, NutrientRegistry.getAll().stream()
                    .filter(d -> d.key().equals(key)).findFirst()
                    .map(d -> Component.translatable("nourished.screen.diet.bar." + d.key()).getString())
                    .orElse(key),
                    labelX, labelY, COL_LABEL, layout.labelScale);

            // Bar
            int barX = labelX + layout.maxLabelSw + LABEL_BAR_GAP;
            int barY = rowCenterY - BAR_H / 2;
            int accentColor = nutrientAccentColor(key);
            int fillColor = barFillColor(key, truePct);
            drawRoundedBar(g, barX, barY, layout.barW, BAR_H, displayPct, COL_BAR_BG, fillColor);

            // Percentage
            int pct = Math.round(truePct * 100f);
            String pctStr = pct + "%";
            int pctColor = pctColor(key, truePct);
            int pctX = barX + layout.barW + BAR_PCT_GAP;
            int pctY = rowCenterY - (int) Math.ceil(9 * layout.labelScale) / 2;
            drawScaledLabel(g, mc, pctStr, pctX, pctY, pctColor, layout.labelScale);

            y += rowH;
            if (i < keys.size() - 1) y += ROW_GAP;
        }
    }

    // ── Key input ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (!NourishedConfig.get().enableHUD()) return;

        while (NourishedKeys.EDIT_HUD.consumeClick()) {
            if (HUDEditMode.isActive) {
                // Exiting edit mode — save any in-progress drag position
                if (hudDragging) {
                    int[] m = scaledMouse(mc);
                    List<String> keys = NutrientRegistry.getKeys();
                    if (!keys.isEmpty()) {
                        Layout layout = computeLayout(mc, keys);
                        int sw = mc.getWindow().getGuiScaledWidth();
                        int sh = mc.getWindow().getGuiScaledHeight();
                        int px = Mth.clamp(m[0] - dragGrabOffsetX, 0, Math.max(0, sw - layout.panelW));
                        int py = Mth.clamp(m[1] - dragGrabOffsetY, 0, Math.max(0, sh - layout.panelH));
                        NourishedClientConfig cc = NourishedClientConfig.get();
                        cc.setHudOffsetX(px - dragAnchorBaseX);
                        cc.setHudOffsetY(py - dragAnchorBaseY);
                        NourishedClientConfig.saveNow();
                    }
                    hudDragging = false;
                }
                HUDEditMode.isActive = false;
            } else {
                HUDEditMode.isActive = true;
            }
        }
    }

    // ── Mouse input ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!NourishedConfig.get().enableHUD()) return;
        if (!HUDEditMode.isActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            hudDragging = false;
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) return;

        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) return;

        Layout layout = computeLayout(mc, keys);
        int[] m = scaledMouse(mc);
        int mx = m[0], my = m[1];

        int panelX = layout.panelX;
        int panelY = layout.panelY;
        if (hudDragging) {
            panelX = Mth.clamp(mx - dragGrabOffsetX, 0, Math.max(0, mc.getWindow().getGuiScaledWidth() - layout.panelW));
            panelY = Mth.clamp(my - dragGrabOffsetY, 0, Math.max(0, mc.getWindow().getGuiScaledHeight() - layout.panelH));
        }

        boolean over = mx >= panelX && my >= panelY && mx < panelX + layout.panelW && my < panelY + layout.panelH;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (event.getAction() == GLFW.GLFW_PRESS && over) {
                event.setCanceled(true);
                hudDragging = true;
                dragGrabOffsetX = mx - panelX;
                dragGrabOffsetY = my - panelY;
                dragAnchorBaseX = layout.baseX;
                dragAnchorBaseY = layout.baseY;
            } else if (event.getAction() == GLFW.GLFW_RELEASE && hudDragging) {
                event.setCanceled(true);
                int sw = mc.getWindow().getGuiScaledWidth();
                int sh = mc.getWindow().getGuiScaledHeight();
                int px = Mth.clamp(mx - dragGrabOffsetX, 0, Math.max(0, sw - layout.panelW));
                int py = Mth.clamp(my - dragGrabOffsetY, 0, Math.max(0, sh - layout.panelH));
                NourishedClientConfig cc = NourishedClientConfig.get();
                cc.setHudOffsetX(px - dragAnchorBaseX);
                cc.setHudOffsetY(py - dragAnchorBaseY);
                NourishedClientConfig.saveNow();
                hudDragging = false;
            }
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private static Layout computeLayout(Minecraft mc, List<String> keys) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        double scale = cc.hudScale();
        float labelScale = (float) (BASE_LABEL_SCALE * scale);
        int scaledPad = Math.max(2, (int) Math.round(PANEL_PAD * scale));
        int barW = Mth.clamp((int) Math.round(cc.hudBarWidth() * scale), 20, 200);
        int iconSize = Math.max(8, (int) Math.round(16 * scale));

        int maxLabelSw = 0;
        for (String key : keys) {
            String label = Component.translatable("nourished.screen.diet.bar." + key).getString();
            maxLabelSw = Math.max(maxLabelSw, (int) Math.ceil(mc.font.width(label) * labelScale));
        }
        int pctW = (int) Math.ceil(mc.font.width(PCT_MAX_CHARS + "%") * labelScale);
        int labelH = (int) Math.ceil(9 * labelScale);
        int rowH = Math.max(iconSize, Math.max(labelH, BAR_H));
        int innerH = keys.size() * rowH + (keys.size() - 1) * ROW_GAP;

        int panelW = scaledPad * 2 + iconSize + ICON_LABEL_GAP + maxLabelSw + LABEL_BAR_GAP + barW + BAR_PCT_GAP + pctW;
        int panelH = innerH + scaledPad * 2;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        HudAnchor anchor = cc.hudAnchor();
        int baseX = switch (anchor) {
            case BOTTOM_LEFT, TOP_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> sw - panelW - MARGIN;
        };
        int baseY = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> sh - cc.hudReservedBottom() - panelH;
        };
        baseY = Math.max(MARGIN, baseY);

        int panelX = Mth.clamp(baseX + cc.hudOffsetX(), 0, Math.max(0, sw - panelW));
        int panelY = Mth.clamp(baseY + cc.hudOffsetY(), 0, Math.max(0, sh - panelH));

        return new Layout(panelX, panelY, panelW, panelH, baseX, baseY, barW, rowH, iconSize, maxLabelSw, scaledPad, labelScale, scale);
    }

    // ── Drawing helpers ──────────────────────────────────────────────────────

    /** Capsule-shaped bar using 3 fill calls to simulate rounded ends. */
    private static void drawRoundedBar(GuiGraphics g, int x, int y, int w, int h, float pct, int bgColor, int fillColor) {
        // Background capsule
        g.fill(x, y + 1, x + w, y + h - 1, bgColor);
        g.fill(x + 1, y, x + w - 1, y + 1, bgColor);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, bgColor);

        // Filled portion
        int filled = Mth.clamp((int) (w * pct), 0, w);
        if (filled <= 0) return;

        // Left-rounded fill
        g.fill(x + 1, y, Math.min(x + filled, x + w - 1), y + 1, fillColor);
        g.fill(x, y + 1, Math.min(x + filled, x + w), y + h - 1, fillColor);
        g.fill(x + 1, y + h - 1, Math.min(x + filled, x + w - 1), y + h, fillColor);

        // Right-round cap only when bar is (nearly) full
        if (filled >= w) {
            g.fill(x + w - 1, y, x + w, y + 1, fillColor);
            g.fill(x + w - 1, y + h - 1, x + w, y + h, fillColor);
        }
    }

    /** Rounded panel rectangle (corner radius in px, approximated with fills). */
    private static void drawRoundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int thickness, int color) {
        g.fill(x, y, x + w, y + thickness, color);
        g.fill(x, y + h - thickness, x + w, y + h, color);
        g.fill(x, y + thickness, x + thickness, y + h - thickness, color);
        g.fill(x + w - thickness, y + thickness, x + w, y + h - thickness, color);
    }

    private static void drawEditBanner(GuiGraphics g, Minecraft mc) {
        String msg = "HUD Edit Mode — drag elements, press H to save";
        int sw = mc.getWindow().getGuiScaledWidth();
        int textW = mc.font.width(msg);
        int bx = (sw - textW) / 2 - 4;
        int by = 6;
        g.fill(bx, by - 2, bx + textW + 8, by + 11, COL_EDIT_BANNER_BG);
        g.drawString(mc.font, msg, bx + 4, by, COL_EDIT_BANNER, false);
    }

    private static void renderIcon(GuiGraphics g, Minecraft mc, String key, int x, int y, int iconSize, double scale) {
        ItemStack stack = iconCache.computeIfAbsent(key, k -> {
            String iconId = NutrientRegistry.getIcon(k);
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(iconId)).orElse(Items.APPLE);
            return new ItemStack(item);
        });

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        float s = iconSize / 16f;
        pose.scale(s, s, 1f);
        g.renderItem(stack, 0, 0);
        pose.popPose();
    }

    private static void drawScaledLabel(GuiGraphics g, Minecraft mc, String text, int x, int y, int color, float scale) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        g.drawString(mc.font, text, 0, 0, color, false);
        pose.popPose();
    }

    // ── Color helpers ────────────────────────────────────────────────────────

    private static int barFillColor(String key, float v) {
        NourishedConfig config = NourishedConfig.get();
        if (v < config.criticalThresholdFor(key)) return COL_RED;
        if (v < config.lowThreshold()) return COL_GOLD;
        return nutrientAccentColor(key);
    }

    private static int pctColor(String key, float v) {
        NourishedConfig config = NourishedConfig.get();
        if (v < config.criticalThresholdFor(key)) return COL_PCT_CRIT;
        if (v < config.lowThreshold()) return COL_PCT_LOW;
        return COL_PCT_GOOD;
    }

    private static int nutrientAccentColor(String key) {
        return switch (key) {
            case "fruits"     -> COL_GREEN;
            case "vegetables" -> COL_CYAN;
            case "proteins"   -> COL_RED;
            case "grains"     -> COL_GOLD;
            case "sugars"     -> COL_PURPLE;
            case "dairy"      -> COL_WHITE;
            default           -> COL_GREEN;
        };
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static int[] scaledMouse(Minecraft mc) {
        double guiScale = mc.getWindow().getGuiScale();
        return new int[]{
                (int) (mc.mouseHandler.xpos() / guiScale),
                (int) (mc.mouseHandler.ypos() / guiScale)
        };
    }

    // ── Layout record ────────────────────────────────────────────────────────

    private record Layout(
            int panelX, int panelY, int panelW, int panelH,
            int baseX, int baseY,
            int barW, int rowH, int iconSize, int maxLabelSw,
            int scaledPad, float labelScale, double scale
    ) {}
}
