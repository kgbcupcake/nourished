package dev.maire.nourished.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.config.HudAnchor;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NourishedHUD {

    // ── Layout constants ─────────────────────────────────────────────────────

    private static final int BAR_H = 5;
    private static final int ROW_GAP = 0;
    private static final int PANEL_PAD = 8;
    private static final int ICON_LABEL_GAP = 2;
    private static final int LABEL_BAR_GAP = 2;
    private static final int BAR_PCT_GAP = 4;
    private static final float BASE_LABEL_SCALE = 6f / 9f;
    private static final int MARGIN = 6;
    private static final int RESIZE_HANDLE_SIZE = 8;

    // ── Colors ───────────────────────────────────────────────────────────────

    private static final int COL_PANEL_BG       = 0xCC101010;
    private static final int COL_BAR_BG         = 0x99111111;
    private static final int COL_LABEL          = 0xFFAAAAAA;
    private static final int COL_PCT_GOOD       = 0xFF55FF55;
    private static final int COL_PCT_LOW        = 0xFFFFAA00;
    private static final int COL_PCT_CRIT       = 0xFFFF5555;
    private static final int COL_EDIT_OVERLAY   = 0x99000000;
    private static final int COL_HOVER_BORDER   = 0xFFFFFFAA;
    private static final int COL_EDIT_BANNER    = 0xFFFFFFFF;
    private static final int COL_EDIT_BANNER_BG = 0xCC000000;
    private static final int COL_HANDLE_BG      = 0xCC2A2A2A;
    private static final int COL_HANDLE_HOVER   = 0xFFEFEF7A;
    private static final int COL_HANDLE_ACTIVE  = 0xFF55FF55;
    private static final int COL_DASHED_PREVIEW = 0xFF6CFFD0;
    private static final int COL_RED            = 0xFFFF5555;
    private static final int COL_GOLD           = 0xFFFFD65C;

    // ── Animation state ──────────────────────────────────────────────────────

    private static final Map<String, Float> displayValues = new HashMap<>();
    private static long lastNano = 0;

    // ── Drag state ───────────────────────────────────────────────────────────

    private static boolean hudDragging;
    private static boolean hudResizing;
    private static int dragGrabOffsetX;
    private static int dragGrabOffsetY;
    private static int dragAnchorBaseX;
    private static int dragAnchorBaseY;
    private static int resizeOriginX;
    private static int resizeOriginY;
    private static double resizeBaseDiagonal;
    private static double resizePreviewScale;

    private NourishedHUD() {}

    // ── Game-event render (normal gameplay, no screen open) ──────────────────

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!ModuleCache.enableHUD) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // HUDEditScreen renders via its own render()
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) return;

        DietData data = ClientDietCache.get();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) return;

        advanceLerp(data, keys);
        List<String> visibleKeys = getVisibleKeys(data, keys);
        if (visibleKeys.isEmpty()) {
            return;
        }
        Layout layout = computeLayout(mc, visibleKeys);
        drawHUDPanel(event.getGuiGraphics(), mc, data, visibleKeys, layout, layout.panelX, layout.panelY);
    }

    // ── Screen-facing render (called by HUDEditScreen) ───────────────────────

    public static void renderForEditScreen(GuiGraphics g, Minecraft mc) {
        if (!ModuleCache.enableHUD) return;
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) return;

        DietData data = ClientDietCache.get();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) return;

        advanceLerp(data, keys);
        List<String> visibleKeys = getVisibleKeys(data, keys);
        if (visibleKeys.isEmpty()) {
            drawEditBanner(g, mc);
            return;
        }
        Layout layout = computeLayout(mc, visibleKeys);
        int[] m = scaledMouse(mc);
        if (hudResizing) {
            double dist = Math.hypot(m[0] - resizeOriginX, m[1] - resizeOriginY);
            resizePreviewScale = Mth.clamp(dist / Math.max(1.0d, resizeBaseDiagonal), 0.3d, 3.0d);
            layout = computeLayout(mc, visibleKeys, resizePreviewScale);
        }

        int panelX = layout.panelX;
        int panelY = layout.panelY;
        if (hudDragging) {
            panelX = Mth.clamp(m[0] - dragGrabOffsetX, 0, Math.max(0, mc.getWindow().getGuiScaledWidth() - layout.panelW));
            panelY = Mth.clamp(m[1] - dragGrabOffsetY, 0, Math.max(0, mc.getWindow().getGuiScaledHeight() - layout.panelH));
        } else if (hudResizing) {
            panelX = resizeOriginX;
            panelY = resizeOriginY;
        }

        // Edit overlay: dark shadow + hover border
        boolean hovered = m[0] >= panelX && m[1] >= panelY
                && m[0] < panelX + layout.panelW && m[1] < panelY + layout.panelH;
        g.fill(panelX - 2, panelY - 2, panelX + layout.panelW + 2, panelY + layout.panelH + 2, COL_EDIT_OVERLAY);
        if (hovered || hudDragging || hudResizing) {
            drawBorder(g, panelX - 1, panelY - 1, layout.panelW + 2, layout.panelH + 2, 1, COL_HOVER_BORDER);
        }
        if (hudResizing) {
            drawDashedBorder(g, panelX - 2, panelY - 2, layout.panelW + 4, layout.panelH + 4, COL_DASHED_PREVIEW);
        }

        drawHUDPanel(g, mc, data, visibleKeys, layout, panelX, panelY);
        boolean handleHovered = isOverResizeHandle(m[0], m[1], panelX, panelY, layout.panelW, layout.panelH);
        drawResizeHandle(g, mc, panelX, panelY, layout.panelW, layout.panelH, handleHovered, hudResizing, m[0], m[1]);
        if (hudResizing) {
            String scaleText = String.format("Scale: %.1fx", resizePreviewScale);
            int textX = panelX + Math.max(0, (layout.panelW - mc.font.width(scaleText)) / 2);
            g.drawString(mc.font, scaleText, textX, panelY + layout.panelH + 6, COL_HANDLE_ACTIVE, false);
        }
        drawEditBanner(g, mc);
    }

    // ── Screen-facing mouse handlers (called by HUDEditScreen) ───────────────

    public static void onEditMousePress(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        Minecraft mc = Minecraft.getInstance();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) return;
        DietData data = ClientDietCache.get();
        List<String> visibleKeys = getVisibleKeys(data, keys);
        if (visibleKeys.isEmpty()) return;

        Layout layout = computeLayout(mc, visibleKeys);
        int panelX = layout.panelX;
        int panelY = layout.panelY;
        boolean overHandle = isOverResizeHandle(mx, my, panelX, panelY, layout.panelW, layout.panelH);

        if (overHandle) {
            hudResizing = true;
            hudDragging = false;
            resizeOriginX = panelX;
            resizeOriginY = panelY;
            Layout baseScaleLayout = computeLayout(mc, visibleKeys, 1.0d);
            resizeBaseDiagonal = Math.hypot(baseScaleLayout.panelW, baseScaleLayout.panelH);
            resizePreviewScale = layout.scale();
            return;
        }

        boolean over = mx >= panelX && my >= panelY && mx < panelX + layout.panelW && my < panelY + layout.panelH;
        if (over) {
            hudDragging = true;
            hudResizing = false;
            dragGrabOffsetX = mx - panelX;
            dragGrabOffsetY = my - panelY;
            dragAnchorBaseX = layout.baseX;
            dragAnchorBaseY = layout.baseY;
        }
    }

    public static void onEditMouseRelease(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        Minecraft mc = Minecraft.getInstance();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) return;
        DietData data = ClientDietCache.get();
        List<String> visibleKeys = getVisibleKeys(data, keys);
        if (visibleKeys.isEmpty()) return;

        NourishedClientConfig cc = NourishedClientConfig.get();
        if (hudDragging) {
            Layout layout = computeLayout(mc, visibleKeys);
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            int px = Mth.clamp(mx - dragGrabOffsetX, 0, Math.max(0, sw - layout.panelW));
            int py = Mth.clamp(my - dragGrabOffsetY, 0, Math.max(0, sh - layout.panelH));
            cc.setHudOffsetX(px - dragAnchorBaseX);
            cc.setHudOffsetY(py - dragAnchorBaseY);
            NourishedClientConfig.saveNow();
            hudDragging = false;
            return;
        }
        if (hudResizing) {
            double finalScale = Mth.clamp(resizePreviewScale, 0.3d, 3.0d);
            cc.setHudScale(finalScale);
            Layout scaled = computeLayout(mc, visibleKeys, finalScale);
            cc.setHudOffsetX(resizeOriginX - scaled.baseX);
            cc.setHudOffsetY(resizeOriginY - scaled.baseY);
            NourishedClientConfig.saveNow();
            hudResizing = false;
        }
    }

    // ── Key input (enter edit mode only; exit is handled by HUDEditScreen) ───

    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (!ModuleCache.enableHUD) return;
        while (NourishedKeys.EDIT_HUD.consumeClick()) {
            HUDEditMode.setActive(true);
        }
    }

    // ── Shared panel drawing ─────────────────────────────────────────────────

    private static void drawHUDPanel(GuiGraphics g, Minecraft mc, DietData data,
                                     List<String> keys, Layout layout, int panelX, int panelY) {
        drawRoundedRect(g, panelX, panelY, layout.panelW, layout.panelH, 2, COL_PANEL_BG);

        int contentX = panelX + layout.scaledPad;
        int y = panelY + layout.scaledPad;

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            float displayPct = displayValues.getOrDefault(key, 0f);
            float truePct = data.nutrients.getOrDefault(key, 0f);

            boolean dimRow = !NourishedClientConfig.get().hideZeroNutrients() && truePct <= 1e-4f;
            if (dimRow) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f);
            }

            int rowH = layout.rowH;
            int rowCenterY = y + rowH / 2;
            int iconSize = layout.iconSize;

            renderIcon(g, key, contentX, rowCenterY - iconSize / 2, iconSize);

            int labelX = contentX + iconSize + ICON_LABEL_GAP;
            int labelY = rowCenterY - (int) Math.ceil(9 * layout.labelScale) / 2;
            String label = Component.translatable("nourished.screen.diet.bar." + key).getString();
            drawScaledLabel(g, mc, label, labelX, labelY, COL_LABEL, layout.labelScale);

            int barX = labelX + layout.maxLabelSw + LABEL_BAR_GAP;
            int barY = rowCenterY - BAR_H / 2;
            drawRoundedBar(g, barX, barY, layout.barW, BAR_H, displayPct, COL_BAR_BG, barFillColor(key, truePct));

            float flash = ClientDietCache.flashAlpha(key);
            if (flash > 0f) {
                int a = (int) (flash * 80);
                int flashColor = (a << 24) | 0xFFFFFF;
                g.fill(barX, barY, barX + layout.barW, barY + BAR_H, flashColor);
            }

            int pct = Math.round(truePct * 100f);
            int pctX = barX + layout.barW + BAR_PCT_GAP;
            int pctY = rowCenterY - (int) Math.ceil(9 * layout.labelScale) / 2;
            drawScaledLabel(g, mc, pct + "%", pctX, pctY, pctColor(key, truePct), layout.labelScale);

            if (dimRow) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            y += rowH;
            if (i < keys.size() - 1) y += ROW_GAP;
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private static Layout computeLayout(Minecraft mc, List<String> keys) {
        return computeLayout(mc, keys, NourishedClientConfig.get().hudScale());
    }

    private static Layout computeLayout(Minecraft mc, List<String> keys, double scale) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        float labelScale = (float) (BASE_LABEL_SCALE * scale);
        int scaledPad = Math.max(2, (int) Math.round(PANEL_PAD * scale));
        int barW = Mth.clamp((int) Math.round(cc.hudBarWidth() * scale), 20, 200);
        int iconSize = Math.max(8, (int) Math.round(16 * scale));

        int maxLabelSw = 0;
        for (String key : keys) {
            String label = Component.translatable("nourished.screen.diet.bar." + key).getString();
            maxLabelSw = Math.max(maxLabelSw, (int) Math.ceil(mc.font.width(label) * labelScale));
        }
        int pctW = (int) Math.ceil(mc.font.width("100%") * labelScale);
        int rowH = Math.max(iconSize, Math.max((int) Math.ceil(9 * labelScale), BAR_H));
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

    // ── Visibility filtering ─────────────────────────────────────────────────

    private static List<String> getVisibleKeys(DietData data, List<String> keys) {
        if (!NourishedClientConfig.get().hideZeroNutrients()) return keys;
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (data.nutrients.getOrDefault(key, 0f) > 0f) {
                result.add(key);
            }
        }
        return result;
    }

    // ── Animation ────────────────────────────────────────────────────────────

    private static void advanceLerp(DietData data, List<String> keys) {
        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0f : Math.min((now - lastNano) / 1_000_000_000f, 0.1f);
        lastNano = now;
        float step = dt <= 0 ? 1f : Math.min(1f, dt / 0.2f);
        for (String key : keys) {
            float target = data.nutrients.getOrDefault(key, 0f);
            float cur = displayValues.getOrDefault(key, target);
            displayValues.put(key, cur + (target - cur) * step);
        }
    }

    // ── Drawing helpers ──────────────────────────────────────────────────────

    private static void drawRoundedBar(GuiGraphics g, int x, int y, int w, int h, float pct, int bgColor, int fillColor) {
        g.fill(x, y + 1, x + w, y + h - 1, bgColor);
        g.fill(x + 1, y, x + w - 1, y + 1, bgColor);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, bgColor);

        int filled = Mth.clamp((int) (w * pct), 0, w);
        if (filled <= 0) return;
        g.fill(x, y + 1, Math.min(x + filled, x + w), y + h - 1, fillColor);
        g.fill(x + 1, y, Math.min(x + filled, x + w - 1), y + 1, fillColor);
        g.fill(x + 1, y + h - 1, Math.min(x + filled, x + w - 1), y + h, fillColor);
        if (filled >= w) {
            g.fill(x + w - 1, y, x + w, y + 1, fillColor);
            g.fill(x + w - 1, y + h - 1, x + w, y + h, fillColor);
        }
    }

    private static void drawRoundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int t, int color) {
        g.fill(x, y, x + w, y + t, color);
        g.fill(x, y + h - t, x + w, y + h, color);
        g.fill(x, y + t, x + t, y + h - t, color);
        g.fill(x + w - t, y + t, x + w, y + h - t, color);
    }

    private static void drawDashedBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        int step = 4;
        int seg = 2;
        for (int i = 0; i < w; i += step) {
            g.fill(x + i, y, x + Math.min(i + seg, w), y + 1, color);
            g.fill(x + i, y + h - 1, x + Math.min(i + seg, w), y + h, color);
        }
        for (int i = 0; i < h; i += step) {
            g.fill(x, y + i, x + 1, y + Math.min(i + seg, h), color);
            g.fill(x + w - 1, y + i, x + w, y + Math.min(i + seg, h), color);
        }
    }

    private static boolean isOverResizeHandle(int mx, int my, int panelX, int panelY, int panelW, int panelH) {
        int hx = panelX + panelW - RESIZE_HANDLE_SIZE;
        int hy = panelY + panelH - RESIZE_HANDLE_SIZE;
        return mx >= hx && my >= hy && mx < hx + RESIZE_HANDLE_SIZE && my < hy + RESIZE_HANDLE_SIZE;
    }

    private static void drawResizeHandle(GuiGraphics g, Minecraft mc, int panelX, int panelY, int panelW, int panelH,
                                         boolean hovered, boolean active, int mx, int my) {
        int hx = panelX + panelW - RESIZE_HANDLE_SIZE;
        int hy = panelY + panelH - RESIZE_HANDLE_SIZE;
        int handleColor = active ? COL_HANDLE_ACTIVE : (hovered ? COL_HANDLE_HOVER : COL_HANDLE_BG);
        g.fill(hx, hy, hx + RESIZE_HANDLE_SIZE, hy + RESIZE_HANDLE_SIZE, handleColor);
        g.drawString(mc.font, "◢", hx + 1, hy, 0xFF101010, false);
        if (hovered && !active) {
            g.renderTooltip(mc.font, Component.literal("Drag to resize"), mx, my);
        }
    }

    private static void drawEditBanner(GuiGraphics g, Minecraft mc) {
        String msg = "HUD Edit Mode — drag elements, press H to save";
        int sw = mc.getWindow().getGuiScaledWidth();
        int textW = mc.font.width(msg);
        int bx = (sw - textW) / 2 - 4;
        g.fill(bx, 4, bx + textW + 8, 17, COL_EDIT_BANNER_BG);
        g.drawString(mc.font, msg, bx + 4, 8, COL_EDIT_BANNER, false);
    }

    private static void renderIcon(GuiGraphics g, String key, int x, int y, int iconSize) {
        String iconId = NutrientRegistry.getIcon(key);
        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(iconId)).orElse(Items.APPLE);
        ItemStack stack = new ItemStack(item);
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
        NourishedConfig cfg = NourishedConfig.get();
        if (v < cfg.criticalThresholdFor(key)) return COL_RED;
        if (v < cfg.lowThreshold()) return COL_GOLD;
        return NutrientUiColors.baseColorArgb(key);
    }

    private static int pctColor(String key, float v) {
        NourishedConfig cfg = NourishedConfig.get();
        if (v < cfg.criticalThresholdFor(key)) return COL_PCT_CRIT;
        if (v < cfg.lowThreshold()) return COL_PCT_LOW;
        return COL_PCT_GOOD;
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }

    // ── Layout record ────────────────────────────────────────────────────────

    private record Layout(
            int panelX, int panelY, int panelW, int panelH,
            int baseX, int baseY,
            int barW, int rowH, int iconSize, int maxLabelSw,
            int scaledPad, float labelScale, double scale
    ) {}
}
