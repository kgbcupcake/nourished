package dev.maire.nourished.modules.activity_driven_nutrient.client;

import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.nutritionalhud.content.BarRows;
import dev.maire.nourished.client.screen.diet.nutritionalhud.content.ScrollingBoxPane;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientRegistry;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityTrackerIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** The Activity Log tab: today's total for each activity tracker, each bar scaled against that tracker's own best day. */
public final class ActivityLogBoxPane extends ScrollingBoxPane {

    public static final String ID = "nourished.diet.activity_log_tab";

    private record Tracker(String moduleId, ResourceLocation trackerId, String langKey, boolean distance, ItemStack icon) {}

    private static final List<Tracker> TRACKERS = List.of(
            new Tracker("mining", ActivityTrackerIds.MINING_BLOCKS_ID, "nourished.screen.diet.activity_log.mining", false, new ItemStack(Items.IRON_PICKAXE)),
            new Tracker("combat", ActivityTrackerIds.COMBAT_KILLS_ID, "nourished.screen.diet.activity_log.combat", false, new ItemStack(Items.IRON_SWORD)),
            new Tracker("sprint", ActivityTrackerIds.SPRINT_DISTANCE_ID, "nourished.screen.diet.activity_log.sprint", true, new ItemStack(Items.LEATHER_BOOTS)),
            new Tracker("swim", ActivityTrackerIds.SWIM_DISTANCE_ID, "nourished.screen.diet.activity_log.swim", true, new ItemStack(Items.WATER_BUCKET)),
            new Tracker("starvation", ActivityTrackerIds.STARVATION_CROSSINGS_ID, "nourished.screen.diet.activity_log.starvation", false, new ItemStack(Items.ROTTEN_FLESH))
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    protected ColorKey borderColor() {
        return NourishedColors.ACTIVITY_LOG_BORDER;
    }

    @Override
    protected int renderContent(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY) {
        if (!ActivityDrivenNutrientRegistry.enabled()) {
            return BarRows.drawNote(g, font, Component.translatable("nourished.screen.diet.activity_log.disabled"), x, y, w);
        }
        return BarRows.draw(g, font, x, y, w, rows(), Component.translatable("nourished.screen.diet.activity_log.empty"));
    }

    private static List<BarRows.Row> rows() {
        LocalPlayer player = Minecraft.getInstance().player;
        List<BarRows.Row> rows = new ArrayList<>(TRACKERS.size());
        if (player == null) {
            return rows;
        }
        int fallback = MarieColors.resolveColor(NourishedColors.TEXT);
        for (Tracker tracker : TRACKERS) {
            float value = MarieTracking.getCurrentTrackerValue(player, tracker.trackerId());
            float best = value;
            for (TrackerHistoryEntry entry : MarieTracking.getTrackerHistory(player, tracker.trackerId())) {
                best = Math.max(best, entry.value());
            }
            // each tracker against its own history, since counts and distances aren't comparable
            float fill = best > 0f ? value / best : 0f;
            int color = ActivityDrivenNutrientRegistry.getColor(tracker.moduleId()).orElse(fallback);
            String text = tracker.distance() ? String.format("%.1f", value) : String.valueOf((int) value);
            rows.add(new BarRows.Row(tracker.icon(), Component.translatable(tracker.langKey()).getString(), color, fill, color, text, color));
        }
        return rows;
    }
}
