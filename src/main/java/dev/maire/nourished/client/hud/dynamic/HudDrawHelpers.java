package dev.maire.nourished.client.hud.dynamic;

import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.maire.nourished.config.NourishedClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
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
    public static final int MARGIN = 6;
    static final int RESIZE_HANDLE_SIZE = 8;

    private static int panelRgb() {
        return NourishedColors.rgb(NourishedColors.HUD_PANEL);
    }
    /** The bar track is translucent: only its RGB is a registered color, the alpha is fixed here. */
    private static final int BAR_BACKGROUND_ALPHA = 0x99;

    /** Calorie-accent color used wherever a calorie value is drawn (the Diet calories box draws the same color). */
    public static int calorieColor() {
        return MarieColors.resolveColor(NourishedColors.CALORIE_VALUE);
    }

    private HudDrawHelpers() {}

    public static int panelColor(double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | panelRgb();
    }

    /** Composes a panel background from a resolved RGB color and a separate opacity value. */
    public static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    /**
     * The HUD's row label for a nutrient: a short HUD-only {@code nourished.hud.label.<key>} entry if
     * the language file has one (e.g. "Veggies" so the row isn't cut off), otherwise the same full
     * label the Diet screen uses.
     */
    public static String nutrientLabel(String key) {
        String shortKey = Nourished.MODID + ".hud.label." + key;
        String shortLabel = Component.translatable(shortKey).getString();
        return shortLabel.equals(shortKey) ? NutrientRegistry.getLabel(key) : shortLabel;
    }

    /** Resolves a nutrient's effective color (user/datapack override, or its registered default). */
    public static int nutrientColorArgb(String key) {
        return NourishedColors.nutrient(key);
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
            float tint = (float) NourishedClientConfig.get().hudIconBrightness();
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
            g.drawString(mc.font, text, 0, 0, MarieModuleSettings.scaleBrightness(color, NourishedClientConfig.get().hudTextBrightness()), false);
        } finally {
            pose.popPose();
        }
    }

    /**
     * A nutrient's bar fill: always the nutrient's own registered color, whatever its value — there is no
     * per-threshold tinting. The value is kept in the signature for the callers that still pass it.
     */
    public static int barFillColor(String key, float v) {
        return nutrientColorArgb(key);
    }

    /** A nutrient's percent text: its own color, same as the bar fill. */
    public static int pctColor(String key, float v) {
        return nutrientColorArgb(key);
    }

    public static int labelColor() {
        return MarieColors.resolveColor(NourishedColors.NUTRIENT_HUD_TEXT);
    }

    /** The empty track of every bar (nutrient, calorie, activity): the bar.track color at the fixed track alpha. */
    public static int barBackgroundColor() {
        return (BAR_BACKGROUND_ALPHA << 24) | NourishedColors.rgb(NourishedColors.BAR_TRACK);
    }
}
