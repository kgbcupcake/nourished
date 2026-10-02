package dev.maire.nourished.client.screen.diet.classic;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Right-column rendering for the classic Diet Screen (nutrient intake bars) plus the diet-icon
 * tooltips that share its row geometry — extracted verbatim from the historical monolithic {@code
 * ClassicDietScreen}; no logic changes, only relocated (aside from the intake legend box, since
 * removed — see the changelog).
 */
final class ClassicDietRightPanel {

    private ClassicDietRightPanel() {}

    // ── Right panel row geometry ─────────────────────────────────────────────
    /** Package-visible: also used by {@code ClassicDietScreen}'s bar-drag-reorder hit-testing. */
    static final int ROW_STEP = 26;
    private static final int BAR_H = 9;      // nutrient bar height (+2px)

    static void drawRightPanel(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            Map<String, Float> display,
            float panelFadeAlpha,
            int mx,
            int my,
            int leftPos,
            int topPos,
            List<String> bars
    ) {
        int rx = leftPos + DietLayout.SPLIT + DietLayout.PAD;
        int y  = topPos + 30;
        String hdr    = Component.translatable("nourished.screen.diet.intake").getString();
        int    hdrW   = mc.font.width(hdr);
        int    hdrCX  = rx + (DietLayout.WIDTH - DietLayout.SPLIT - DietLayout.PAD * 2) / 2;
        g.drawString(mc.font, "✧✧", rx + 2, y, ClassicDietDrawHelpers.COL_HEADER, false);
        g.drawString(mc.font, "✧✧", leftPos + DietLayout.WIDTH - DietLayout.PAD - 14, y, ClassicDietDrawHelpers.COL_HEADER, false);
        g.drawString(mc.font, hdr, hdrCX - hdrW / 2, y, ClassicDietDrawHelpers.COL_HEADER, false);
        int lineY = y + 4;
        g.fill(rx,                        lineY, hdrCX - hdrW / 2 - 3, lineY + 1, ClassicDietDrawHelpers.COL_BORDER_LT);
        g.fill(hdrCX + hdrW / 2 + 3,     lineY, leftPos + DietLayout.WIDTH - DietLayout.PAD, lineY + 1, ClassicDietDrawHelpers.COL_BORDER_LT);
        y += 14;
        int arrowSlot = 10;
        int arrowLeft = leftPos + DietLayout.WIDTH - DietLayout.PAD - arrowSlot;
        int pctColumnRight = arrowLeft - 4;
        int maxPctW = mc.font.width("100%");
        int barLeft = rx + 24;
        int barW = Math.max(0, pctColumnRight - maxPctW - 4 - barLeft);
        for (String key : bars) {
            float disp  = display.getOrDefault(key, data.values.getOrDefault(key, 0f));
            float real  = data.values.getOrDefault(key, 0f);
            float prev  = data.lastValues.getOrDefault(key, real);
            int   color = ClassicDietColorLogic.barColor(key, disp);
            int   pctColor = ClassicDietColorLogic.nutrientBaseColor(key);
            int rowRight = leftPos + DietLayout.WIDTH - DietLayout.PAD;
            if (my >= y - 2 && my < y - 2 + ROW_STEP && mx >= rx - 2 && mx < rowRight) {
                g.fill(rx - 2, y - 2, rowRight, y - 2 + ROW_STEP, 0x0CFFFFFF);
            }
            ClassicDietDrawHelpers.drawRoundedBox(g, rx - 2, y - 2, rowRight - (rx - 2), 26);
            int bx = rx;
            int by = y;
            ClassicDietDrawHelpers.drawRoundedPanel(
                    g,
                    bx,
                    by,
                    20,
                    20,
                    ClassicDietDrawHelpers.panelColorWithOpacity(ClassicDietDrawHelpers.COL_PANEL_RGB, NourishedClientConfig.get().dietBackgroundOpacity()),
                    ClassicDietDrawHelpers.COL_BORDER_LT,
                    ClassicDietDrawHelpers.COL_BORDER
            );
            String iconId = NutrientRegistry.getIcon(key);
            Item iconItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(iconId)).orElse(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:apple")));
            g.renderItem(new ItemStack(iconItem), bx + 2, by + 2);
            g.drawString(mc.font,
                    NutrientRegistry.getLabelComponent(key),
                    rx + 24, y + 2, ClassicDietDrawHelpers.COL_WHITE, false);
            ClassicDietDrawHelpers.drawSegmentedBar(g, rx + 24, y + 12, barW, BAR_H, disp, color);
            float flashA = MarieClientCache.flashAlpha(key);
            if (flashA > 0f) {
                int aByte = Mth.clamp(Mth.floor(flashA * panelFadeAlpha * 255f), 1, 255);
                g.fill(rx + 24, y + 12, rx + 24 + barW, y + 12 + BAR_H, (aByte << 24) | ClassicDietDrawHelpers.COL_FLASH_RGB);
            }
            String pctStr = Math.round(disp * 100) + "%";
            int pctX = pctColumnRight - mc.font.width(pctStr);
            int dimmedPct = (pctColor & 0x00FFFFFF) | 0x99000000;
            g.drawString(mc.font, pctStr, pctX, y + 2, dimmedPct, false);
            if (real > prev + 0.005f)
                g.drawString(mc.font, "↑", arrowLeft, y + 2, ClassicDietDrawHelpers.COL_GREEN, false);
            else if (real < prev - 0.005f)
                g.drawString(mc.font, "↓", arrowLeft, y + 2, ClassicDietDrawHelpers.COL_RED, false);
            y += ROW_STEP;
        }
    }

    static void drawDietIconTooltips(GuiGraphics g, Minecraft mc, DietLayout.Layout layout, List<String> visibleBars, int mx, int my) {
        double s = layout.scale();
        int rx = layout.panelX() + (int) Math.round((DietLayout.SPLIT + DietLayout.PAD) * s);
        int y = layout.panelY() + (int) Math.round((30 + 14) * s);
        int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
        int iconSize = Math.max(1, (int) Math.round(20 * s));
        for (String key : visibleBars) {
            if (mx >= rx && mx <= rx + iconSize && my >= y && my <= y + iconSize) {
                g.renderTooltip(mc.font,
                        NutrientRegistry.getTooltipComponent(key),
                        mx, my);
                return;
            }
            y += rowStep;
        }
    }

}
