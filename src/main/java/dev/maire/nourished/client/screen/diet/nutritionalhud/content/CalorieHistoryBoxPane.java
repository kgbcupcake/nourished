package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** The Calorie History tab: today's calories, then each retained past day, as a share of the daily goal. */
public final class CalorieHistoryBoxPane extends ScrollingBoxPane {

    public static final String ID = "nourished.diet.calorie_history_tab";

    private static final ItemStack ICON = new ItemStack(Items.FIRE_CHARGE);

    @Override
    public String id() {
        return ID;
    }

    @Override
    protected ColorKey borderColor() {
        return NourishedColors.CALORIE_HUD_BORDER;
    }

    @Override
    protected int renderContent(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY) {
        if (!FeatureFlagCache.enableCalorieHistory()) {
            return BarRows.drawNote(g, font, Component.translatable("nourished.screen.diet.calorie_history.disabled"), x, y, w);
        }
        return BarRows.draw(g, font, x, y, w, rows(), Component.translatable("nourished.screen.diet.calorie_history.empty"));
    }

    private static List<BarRows.Row> rows() {
        List<BarRows.Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        TrackingData data = MarieClientCache.get();
        if (mc.player == null || data == null) {
            return rows;
        }
        float goal = data.maxTotal;
        rows.add(row(Component.translatable("nourished.hud.calorieHistory.today").getString(),
                MarieTracking.getCurrentTrackerValue(mc.player, NourishedAPI.CALORIES_TRACKER_ID), goal));
        List<TrackerHistoryEntry> history = data.trackingHistory.get(NourishedAPI.CALORIES_TRACKER_ID);
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                String label = Component.translatable(i == 0 ? "nourished.hud.calorieHistory.oneDay" : "nourished.hud.calorieHistory.days", i + 1).getString();
                rows.add(row(label, history.get(i).value(), goal));
            }
        }
        return rows;
    }

    private static BarRows.Row row(String label, float value, float goal) {
        float pct = goal > 0 ? value / goal : 0f;
        // calories have no graded colors, so going over the goal is the only shift
        int color = pct > 1f ? MarieColors.resolveColor(NourishedColors.CALORIE_OVER_GOAL) : HudDrawHelpers.calorieColor();
        return new BarRows.Row(ICON, label, MarieColors.resolveColor(NourishedColors.TEXT), pct, color,
                Math.round(pct * 100) + "%", color);
    }
}
