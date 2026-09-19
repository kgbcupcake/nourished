package dev.maire.nourished.client.hud.dynamic;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.maire.nourished.config.NourishedClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class HudDrawHelpers {

    /** Corner radius (pixels) of the dynamic HUD panels' rounded background. */
    public static final int PANEL_CORNER_RADIUS = 4;
    public static final int BAR_H = 5;
    public static final int VERTICAL_BAR_W = 6;
    public static final int VERTICAL_BAR_H = 36;
    public static final int VERTICAL_COLUMN_GAP = 4;
    public static final int ROW_GAP = 0;
    public static final int PANEL_PAD = 8;
    public static final int ICON_LABEL_GAP = 2;
    public static final int LABEL_BAR_GAP = 2;
    public static final int BAR_PCT_GAP = 4;
    public static final float BASE_LABEL_SCALE = 6f / 9f;
    public static final int MARGIN = 6;
    static final int RESIZE_HANDLE_SIZE = 8;

    private static final int PANEL_RGB = 0x00101010;
    private static final int COL_BAR_BG = 0x99111111;
    private static final int COL_LABEL = 0xFFAAAAAA;
    private static final int COL_PCT_GOOD = 0xFF55FF55;
    private static final int COL_PCT_LOW = 0xFFFFAA00;
    private static final int COL_PCT_CRIT = 0xFFFF5555;
    private static final int COL_EDIT_OVERLAY = 0x99000000;
    private static final int COL_HOVER_BORDER = 0xFFFFFFAA;
    private static final int COL_EDIT_BANNER = 0xFFFFFFFF;
    private static final int COL_EDIT_BANNER_BG = 0xCC000000;
    private static final int COL_HANDLE_BG = 0xCC2A2A2A;
    private static final int COL_HANDLE_HOVER = 0xFFEFEF7A;
    private static final int COL_HANDLE_ACTIVE = 0xFF55FF55;
    private static final int COL_DASHED_PREVIEW = 0xFF6CFFD0;
    private static final int COL_RED = 0xFFFF5555;
    private static final int COL_GOLD = 0xFFFFD65C;

    /** Calorie-accent color used wherever a calorie value is drawn (matches {@code COL_PCT_GOOD}/{@code CaloriesComponent}'s calorie text). */
    public static final int CALORIE_COLOR = 0xFF55FF55;

    private HudDrawHelpers() {}

    public static int panelColor(double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | PANEL_RGB;
    }

    /** Composes a panel background from a resolved RGB color and a separate opacity value. */
    public static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public static String nutrientLabel(String key) {
        return NutrientRegistry.getLabel(key);
    }

    /** Resolves a nutrient's effective color (user/datapack override, or its registered default). */
    public static int nutrientColorArgb(String key) {
        return MarieColors.resolveColor(ColorKey.of(
                ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "nutrient." + key)));
    }

    public static void drawRoundedBar(GuiGraphics g, int x, int y, int w, int h, float pct, int bgColor, int fillColor) {
        g.fill(x, y + 1, x + w, y + h - 1, bgColor);
        g.fill(x + 1, y, x + w - 1, y + 1, bgColor);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, bgColor);

        int filled = Mth.clamp((int) (w * pct), 0, w);
        if (filled <= 0) {
            return;
        }
        g.fill(x, y + 1, Math.min(x + filled, x + w), y + h - 1, fillColor);
        g.fill(x + 1, y, Math.min(x + filled, x + w - 1), y + 1, fillColor);
        g.fill(x + 1, y + h - 1, Math.min(x + filled, x + w - 1), y + h, fillColor);
        if (filled >= w) {
            g.fill(x + w - 1, y, x + w, y + 1, fillColor);
            g.fill(x + w - 1, y + h - 1, x + w, y + h, fillColor);
        }
    }

    /** Vertical bar: fill grows upward from the bottom edge. */
    public static void drawRoundedVerticalBar(GuiGraphics g, int x, int y, int w, int h, float pct, int bgColor, int fillColor) {
        g.fill(x + 1, y, x + w - 1, y + h, bgColor);
        g.fill(x, y + 1, x + 1, y + h - 1, bgColor);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, bgColor);

        int filled = Mth.clamp((int) (h * pct), 0, h);
        if (filled <= 0) {
            return;
        }
        int fillTop = y + h - filled;
        g.fill(x + 1, fillTop, x + w - 1, y + h, fillColor);
        g.fill(x, fillTop + 1, x + 1, y + h - 1, fillColor);
        g.fill(x + w - 1, fillTop + 1, x + w, y + h - 1, fillColor);
        if (filled >= h) {
            g.fill(x + 1, y, x + w - 1, y + 1, fillColor);
        }
    }

    public static void drawRoundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
    }

    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int t, int color) {
        g.fill(x, y, x + w, y + t, color);
        g.fill(x, y + h - t, x + w, y + h, color);
        g.fill(x, y + t, x + t, y + h - t, color);
        g.fill(x + w - t, y + t, x + w, y + h - t, color);
    }

    public static void drawDashedBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
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

    public static boolean isOverResizeHandle(int mx, int my, int panelX, int panelY, int panelW, int panelH) {
        int hx = panelX + panelW - RESIZE_HANDLE_SIZE;
        int hy = panelY + panelH - RESIZE_HANDLE_SIZE;
        return mx >= hx && my >= hy && mx < hx + RESIZE_HANDLE_SIZE && my < hy + RESIZE_HANDLE_SIZE;
    }

    public static void drawResizeHandle(GuiGraphics g, Minecraft mc, int panelX, int panelY, int panelW, int panelH,
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

    static void drawEditBanner(GuiGraphics g, Minecraft mc) {
        String msg = "HUD Edit Mode — drag elements, press H to save";
        int sw = mc.getWindow().getGuiScaledWidth();
        int textW = mc.font.width(msg);
        int bx = (sw - textW) / 2 - 4;
        g.fill(bx, 4, bx + textW + 8, 17, COL_EDIT_BANNER_BG);
        g.drawString(mc.font, msg, bx + 4, 8, COL_EDIT_BANNER, false);
    }

    public static void renderIcon(GuiGraphics g, String key, int x, int y, int iconSize) {
        // NutrientRegistry.getIconItem resolves/validates the icon id string once per distinct id and
        // caches the Item forever, instead of re-running ResourceLocation.tryParse and a
        // BuiltInRegistries.ITEM lookup on every HUD frame for every visible bar (this classic
        // renderer draws every row every frame, same as the MarieUI NutrientBarComponent path).
        ItemStack stack = new ItemStack(NutrientRegistry.getIconItem(key));
        // pushPose/popPose must be paired even if renderItem throws (e.g. a transiently-unbaked
        // item id resolved mid-sync while nutrient values are updating rapidly, as happens while
        // eating) — g's PoseStack is shared across every RenderGuiEvent.Post subscriber this frame.
        // An unmatched pushPose here leaves this translate+scale applied to every later
        // g.fill()/drawString()/renderItem() call for the rest of the frame (fill() draws its quad
        // through the current pose transform), which is how a small bar/icon fill elsewhere can end
        // up stretched into a full-screen quad — self-healing on the next successful call and
        // re-corrupting on the next throw, producing rapid full-screen flashing while the underlying
        // condition keeps retriggering.
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0);
            float s = iconSize / 16f;
            pose.scale(s, s, 1f);
            float tint = (float) NourishedClientConfig.get().hudContentBrightness();
            RenderSystem.setShaderColor(tint, tint, tint, 1f);
            try {
                g.renderItem(stack, 0, 0);
            } finally {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        } finally {
            pose.popPose();
        }
    }

    public static void drawScaledLabel(GuiGraphics g, Minecraft mc, String text, int x, int y, int color, float scale) {
        // See renderIcon's comment above — same pairing requirement, same shared-PoseStack corruption
        // risk if drawString throws partway through.
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1f);
            g.drawString(mc.font, text, 0, 0, scaleBrightness(color, NourishedClientConfig.get().hudContentBrightness()), false);
        } finally {
            pose.popPose();
        }
    }

    public static int barFillColor(String key, float v) {
        NourishedConfig cfg = NourishedConfig.get();
        boolean beneficial = NutrientRegistry.isBeneficial(key);
        if (beneficial) {
            if (v < cfg.criticalThresholdFor(key)) {
                return COL_RED;
            }
            if (v < cfg.lowThreshold()) {
                return COL_GOLD;
            }
            return nutrientColorArgb(key);
        }
        if (v > cfg.excessThreshold()) {
            return COL_RED;
        }
        if (v > cfg.lowThreshold()) {
            return COL_GOLD;
        }
        return nutrientColorArgb(key);
    }

    public static int pctColor(String key, float v) {
        NourishedConfig cfg = NourishedConfig.get();
        boolean beneficial = NutrientRegistry.isBeneficial(key);
        if (beneficial) {
            if (v < cfg.criticalThresholdFor(key)) {
                return COL_PCT_CRIT;
            }
            if (v < cfg.lowThreshold()) {
                return COL_PCT_LOW;
            }
            return COL_PCT_GOOD;
        }
        if (v > cfg.excessThreshold()) {
            return COL_PCT_CRIT;
        }
        if (v > cfg.lowThreshold()) {
            return COL_PCT_LOW;
        }
        return COL_PCT_GOOD;
    }

    public static int editOverlayColor() {
        return COL_EDIT_OVERLAY;
    }

    public static int hoverBorderColor() {
        return COL_HOVER_BORDER;
    }

    public static int dashedPreviewColor() {
        return COL_DASHED_PREVIEW;
    }

    public static int handleActiveColor() {
        return COL_HANDLE_ACTIVE;
    }

    /** {@code argb} with its RGB scaled by {@code brightness} (alpha untouched); 1.0 returns it unchanged. */
    public static int scaleBrightness(int argb, double brightness) {
        if (brightness >= 1.0d) {
            return argb;
        }
        int r = (int) Math.round(((argb >> 16) & 0xFF) * brightness);
        int g = (int) Math.round(((argb >> 8) & 0xFF) * brightness);
        int b = (int) Math.round((argb & 0xFF) * brightness);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static int labelColor() {
        return COL_LABEL;
    }

    public static int barBackgroundColor() {
        return COL_BAR_BG;
    }
}
