package dev.maire.nourished.client.micro;

import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Small bar chart of a saved daily intake tracker: oldest day on the left, today on the right. */
public final class IntakeHistoryChart {

    static final int DAYS = 7;
    static final int HEIGHT = 22;
    // fixed so resizing the box clips instead of squeezing the chart
    static final int WIDTH = 140;
    private static final int TRACK = 0xFF2A3038;

    private IntakeHistoryChart() {}

    /** Oldest first, today last; empty if history is off or nothing is saved yet. */
    static List<Float> days(@Nullable ResourceLocation trackerId) {
        var player = Minecraft.getInstance().player;
        if (trackerId == null || player == null) return List.of();
        List<TrackerHistoryEntry> history = MarieTracking.getTrackerHistory(player, trackerId);
        float today = MarieTracking.getCurrentTrackerValue(player, trackerId);
        if (history.isEmpty() && today <= 0f) return List.of();
        List<Float> out = new ArrayList<>();
        // history is newest first
        for (int i = Math.min(DAYS - 1, history.size()) - 1; i >= 0; i--) out.add(history.get(i).value());
        out.add(today);
        return out;
    }

    /**
     * Days since anything counted toward {@code trackerId}: 0 = today. {@code -1} when history is off
     * or empty; otherwise {@code -(saved days + 2)} when nothing was eaten in the saved window.
     */
    public static int daysSinceEaten(@Nullable ResourceLocation trackerId) {
        var player = Minecraft.getInstance().player;
        if (trackerId == null || player == null) return -1;
        if (MarieTracking.getCurrentTrackerValue(player, trackerId) > 0f) return 0;
        List<TrackerHistoryEntry> history = MarieTracking.getTrackerHistory(player, trackerId);
        if (history.isEmpty()) return -1;
        // history is newest first
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).value() > 0f) return i + 1;
        }
        return -(history.size() + 2);
    }

    /** Draws the bars in {@link #WIDTH} x {@link #HEIGHT}; {@code percent} labels today as a % of a full bar. */
    static void draw(GuiGraphics g, Font font, List<Float> days, int x, int y, int color, boolean percent) {
        int chartW = WIDTH;
        g.fill(x, y, x + chartW, y + HEIGHT, TRACK);
        float max = 0f;
        for (float v : days) max = Math.max(max, v);
        int slot = chartW / DAYS;
        // right-align so today always sits in the last slot
        int start = x + chartW - slot * days.size();
        for (int i = 0; i < days.size(); i++) {
            float v = days.get(i);
            if (v <= 0f || max <= 0f) continue;
            int h = Math.max(1, Math.round((HEIGHT - 2) * v / max));
            int bx = start + i * slot + 1;
            int barColor = i == days.size() - 1 ? color : (color & 0x00FFFFFF) | 0x99000000;
            g.fill(bx, y + HEIGHT - 1 - h, bx + Math.max(1, slot - 2), y + HEIGHT - 1, barColor);
        }
        float today = days.isEmpty() ? 0f : days.get(days.size() - 1);
        String value = percent ? Math.round(today * 100) + "%" : String.valueOf(Math.round(today * 10) / 10f);
        g.drawString(font, Component.translatable("nourished.screen.nutrients.history.today", value),
                x + chartW + 6, y + 3, NutrientDetailView.TEXT);
        g.drawString(font, Component.translatable("nourished.screen.nutrients.history.days", days.size()),
                x + chartW + 6, y + 13, NutrientDetailView.MUTED);
    }
}
