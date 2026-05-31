package dev.maire.nourished.client.hud;

import dev.maire.nourished.config.HudAnchor;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.List;

final class HudLayout {

    record Layout(
            int panelX, int panelY, int panelW, int panelH,
            int baseX, int baseY,
            int barW, int rowH, int iconSize, int maxLabelSw,
            int scaledPad, float labelScale, double scale,
            boolean verticalLayout,
            int verticalBarW, int verticalBarH, int verticalColumnW
    ) {}

    private HudLayout() {}

    static Layout compute(Minecraft mc, List<String> keys) {
        return compute(mc, keys, NourishedClientConfig.get().hudScale());
    }

    static Layout compute(Minecraft mc, List<String> keys, double scale) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        boolean verticalLayout = cc.hudVerticalLayout();
        float labelScale = (float) (HudDrawHelpers.BASE_LABEL_SCALE * scale);
        int scaledPad = Math.max(2, (int) Math.round(HudDrawHelpers.PANEL_PAD * scale));
        int barW = Mth.clamp((int) Math.round(cc.hudBarWidth() * scale), 20, 200);
        int iconSize = Math.max(8, (int) Math.round(16 * scale));
        int verticalBarW = Math.max(4, (int) Math.round(HudDrawHelpers.VERTICAL_BAR_W * scale));
        int verticalBarH = Math.max(16, (int) Math.round(HudDrawHelpers.VERTICAL_BAR_H * scale));
        int columnGap = Math.max(2, (int) Math.round(HudDrawHelpers.VERTICAL_COLUMN_GAP * scale));

        int maxLabelSw = 0;
        for (String key : keys) {
            String label = HudDrawHelpers.nutrientLabel(key);
            maxLabelSw = Math.max(maxLabelSw, (int) Math.ceil(mc.font.width(label) * labelScale));
        }
        int pctW = (int) Math.ceil(mc.font.width("100%") * labelScale);
        int rowH = Math.max(iconSize, Math.max((int) Math.ceil(9 * labelScale), HudDrawHelpers.BAR_H));
        int innerH = keys.size() * rowH + (keys.size() - 1) * HudDrawHelpers.ROW_GAP;

        int verticalColumnW = Math.max(maxLabelSw, Math.max(verticalBarW, pctW));
        int panelW;
        int panelH;
        if (verticalLayout) {
            panelW = scaledPad * 2 + keys.size() * verticalColumnW + Math.max(0, keys.size() - 1) * columnGap;
            panelH = scaledPad * 2
                    + (int) Math.ceil(9 * labelScale)
                    + 2
                    + verticalBarH
                    + 2
                    + (int) Math.ceil(9 * labelScale);
        } else {
            panelW = scaledPad * 2 + iconSize + HudDrawHelpers.ICON_LABEL_GAP + maxLabelSw
                    + HudDrawHelpers.LABEL_BAR_GAP + barW + HudDrawHelpers.BAR_PCT_GAP + pctW;
            panelH = innerH + scaledPad * 2;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        HudAnchor anchor = cc.hudAnchor();
        int baseX = switch (anchor) {
            case BOTTOM_LEFT, TOP_LEFT -> HudDrawHelpers.MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> sw - panelW - HudDrawHelpers.MARGIN;
        };
        int baseY = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> HudDrawHelpers.MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> sh - cc.hudReservedBottom() - panelH;
        };
        baseY = Math.max(HudDrawHelpers.MARGIN, baseY);

        int panelX = Mth.clamp(baseX + cc.hudOffsetX(), 0, Math.max(0, sw - panelW));
        int panelY = Mth.clamp(baseY + cc.hudOffsetY(), 0, Math.max(0, sh - panelH));
        return new Layout(
                panelX, panelY, panelW, panelH, baseX, baseY,
                barW, rowH, iconSize, maxLabelSw, scaledPad, labelScale, scale, verticalLayout,
                verticalBarW, verticalBarH, verticalColumnW
        );
    }
}
