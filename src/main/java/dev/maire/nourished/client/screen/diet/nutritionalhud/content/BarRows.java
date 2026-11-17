package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Icon, label, bar and trailing value text, one row each — the layout the Calorie History and Activity Log tabs share. */
public final class BarRows {

    public static final int ROW_H = 20;
    public static final int MUTED = 0xFF9AA4AE;
    private static final int TRACK = 0xFF2A3038;
    private static final int VALUE_W = 60;

    /** One row; {@code fill} is clamped to 0..1. */
    public record Row(ItemStack icon, String label, int labelColor, float fill, int barColor, String value, int valueColor) {}

    private BarRows() {}

    /** Draws {@code rows} at {@code (x, y)} and returns the height used; shows {@code empty} if there are none. */
    public static int draw(GuiGraphics g, Font font, int x, int y, int w, List<Row> rows, Component empty) {
        if (rows.isEmpty()) {
            g.drawString(font, empty, x, y, MUTED);
            return 12;
        }
        int labelW = 0;
        for (Row row : rows) {
            labelW = Math.max(labelW, font.width(row.label()));
        }
        int barX = x + 20 + labelW + 8;
        int barW = Math.max(30, w - (barX - x) - VALUE_W - 6);
        int top = y;
        for (Row row : rows) {
            g.renderItem(row.icon(), x, y + 2);
            g.drawString(font, row.label(), x + 20, y + 6, row.labelColor());
            float fill = Math.max(0f, Math.min(1f, row.fill()));
            g.fill(barX, y + 6, barX + barW, y + 14, TRACK);
            g.fill(barX, y + 6, barX + Math.round(barW * fill), y + 14, row.barColor());
            g.drawString(font, row.value(), barX + barW + 6, y + 6, row.valueColor());
            y += ROW_H;
        }
        return y - top;
    }

    /** A wrapped muted line, for the "module is off" message. */
    public static int drawNote(GuiGraphics g, Font font, Component text, int x, int y, int w) {
        int top = y;
        for (var line : font.split(text, Math.max(40, w))) {
            g.drawString(font, line, x, y, MUTED);
            y += 10;
        }
        return y - top;
    }
}
