package dev.maire.nourished.client.deficiency;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.deficiency.DeficiencyCalories;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.core.deficiency.DeficiencyConfig;
import dev.maire.nourished.core.deficiency.DeficiencySettings;
import dev.maire.nourished.core.deficiency.DeficiencyTracker;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Deficiency tab content: overall status and one pressure bar per tracked food group. */
public final class DeficiencyView {

    private static final int ROW_H = 20;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int MUTED = 0xFF9AA4AE;
    private static final int TRACK = 0xFF2A3038;
    private static final int TICK = 0xAAFFFFFF;

    /** Draws at {@code (x, y)} and returns the height used. */
    public int render(GuiGraphics g, Font font, int x, int y, int w) {
        int top = y;
        DeficiencySettings s = DeficiencyConfig.get();
        if (!s.enabled() || s.tiers().isEmpty()) {
            g.drawString(font, Component.translatable("nourished.deficiency.tab.disabled"), x, y, MUTED);
            return 12;
        }
        int overall = DeficiencyClient.overallTier();
        g.drawString(font, Component.translatable("nourished.deficiency.tab.status"), x, y, MUTED);
        g.drawString(font, tierName(overall), x + font.width(Component.translatable("nourished.deficiency.tab.status")) + 4,
                y, DeficiencyClient.tierColor(overall));
        y += 12;
        if (overall > 0) {
            y = drawPenalties(g, font, s, overall, x, y, w);
        }
        y += 6;
        y = drawWrapped(g, font, Component.translatable("nourished.deficiency.tab.hint"), x, y, w, MUTED) + 6;

        TrackingData data = MarieClientCache.get();
        int barX = x + 100;
        int barW = Math.max(40, w - 100 - 110);
        boolean any = false;
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            if (!DeficiencyTracker.tracked(s, def) || DeficiencyCalories.KEY.equals(def.key())) continue;
            any = true;
            Float value = data == null ? null : data.values.get(def.key());
            Integer state = value == null ? null : DeficiencyTracker.groupState(s, def, value);
            drawRow(g, font, s, def.key(), new ItemStack(NutrientRegistry.getIconItem(def.key())),
                    def.displayName(), 0xFF000000 | def.color(), state, x, y, barX, barW);
            y += ROW_H;
        }
        if (s.calories().enabled() && NourishedConfig.get().enableCalorieTracking()) {
            any = true;
            drawRow(g, font, s, DeficiencyCalories.KEY, new ItemStack(Items.BREAD),
                    Component.translatable("nourished.deficiency.calories").getString(),
                    MarieColors.resolveColor(NourishedColors.CALORIE_VALUE), calorieState(s, data), x, y, barX, barW);
            y += ROW_H;
        }
        if (!any) {
            g.drawString(font, Component.translatable("nourished.deficiency.tab.none"), x, y, MUTED);
            y += 12;
        }
        return y - top;
    }

    private int drawPenalties(GuiGraphics g, Font font, DeficiencySettings s, int tier, int x, int y, int w) {
        DeficiencySettings.Tier t = s.tiers().get(tier - 1);
        List<String> parts = new ArrayList<>();
        if (t.activityMultiplier() != 1f) {
            parts.add(Component.translatable("nourished.deficiency.tab.drain",
                    String.format("%.2f", t.activityMultiplier())).getString());
        }
        if (t.regenMultiplier() != 1f) {
            parts.add(Component.translatable("nourished.deficiency.tab.regen", percent(t.regenMultiplier() - 1f)).getString());
        }
        if (t.saturationMultiplier() != 1f) {
            parts.add(Component.translatable("nourished.deficiency.tab.saturation", percent(t.saturationMultiplier() - 1f)).getString());
        }
        t.attributes().forEach((key, penalty) -> {
            ResourceLocation id = ResourceLocation.tryParse(key);
            var attr = id == null ? null : BuiltInRegistries.ATTRIBUTE.get(id);
            if (attr == null) return;
            String amount = penalty.operation().startsWith("add_value") || penalty.operation().equals("add")
                    ? String.format("%+.2f", penalty.amount()) : percent((float) penalty.amount());
            parts.add(Component.translatable(attr.getDescriptionId()).getString() + " " + amount);
        });
        for (DeficiencySettings.TierEffect e : t.effects()) {
            ResourceLocation id = ResourceLocation.tryParse(e.effect());
            var effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect == null) continue;
            String name = effect.getDisplayName().getString();
            parts.add(e.amplifier() > 0 ? name + " " + Component.translatable("enchantment.level." + (e.amplifier() + 1)).getString() : name);
        }
        if (parts.isEmpty()) return y;
        return drawWrapped(g, font, Component.literal(String.join(" · ", parts)), x, y, w, TEXT);
    }

    private static Integer calorieState(DeficiencySettings s, TrackingData data) {
        var player = Minecraft.getInstance().player;
        if (player == null || data == null) return null;
        float today = MarieTracking.getCurrentTrackerValue(player, NourishedAPI.CALORIES_TRACKER_ID);
        float yesterday = -1f;
        List<TrackerHistoryEntry> history = data.trackingHistory.get(NourishedAPI.CALORIES_TRACKER_ID);
        if (NourishedConfig.get().enableCalorieHistory() && history != null && !history.isEmpty()) {
            yesterday = history.get(0).value();
        }
        return DeficiencyCalories.state(s.calories(), yesterday, today, data.maxTotal);
    }

    private void drawRow(GuiGraphics g, Font font, DeficiencySettings s, String key, ItemStack icon, String name,
                         int nameColor, Integer state, int x, int y, int barX, int barW) {
        int pressure = DeficiencyClient.pressure(key);
        int tier = s.tierFor(pressure);
        int max = Math.max(1, s.maxPressure());
        g.renderItem(icon, x, y);
        g.drawString(font, name, x + 20, y + 4, nameColor);

        g.fill(barX, y + 4, barX + barW, y + 12, TRACK);
        g.fill(barX, y + 4, barX + Math.round(barW * Math.min(1f, pressure / (float) max)), y + 12,
                DeficiencyClient.tierColor(Math.max(1, tier)));
        for (DeficiencySettings.Tier t : s.tiers()) {
            int tx = barX + Math.round(barW * t.afterTicks() / (float) max);
            g.fill(tx, y + 2, tx + 1, y + 14, TICK);
        }

        int labelX = barX + barW + 6;
        String level = tier == 0 ? Component.translatable("nourished.deficiency.tab.ok").getString() : "L" + tier;
        g.drawString(font, level, labelX, y + 4, DeficiencyClient.tierColor(tier));
        g.drawString(font, status(s, state, pressure, tier), labelX + 22, y + 4, MUTED);
    }

    /** {@code state}: +1 building, -1 recovering, 0 holding, null unknown. */
    private static Component status(DeficiencySettings s, Integer state, int pressure, int tier) {
        if (state == null) return Component.empty();
        if (state > 0) {
            if (tier >= s.tiers().size()) return Component.translatable("nourished.deficiency.tab.worsening");
            int ticks = s.tiers().get(tier).afterTicks() - pressure;
            return Component.translatable("nourished.deficiency.tab.next", tier + 1, duration(ticks));
        }
        if (pressure > 0 && state < 0 && s.recoverySpeed() > 0) {
            int floor = tier == 0 ? 0 : s.tiers().get(tier - 1).afterTicks() - 1;
            int ticks = Math.round((pressure - Math.max(0, floor)) / s.recoverySpeed());
            if (tier == 0) return Component.translatable("nourished.deficiency.tab.clears", duration(ticks));
            if (tier == 1) return Component.translatable("nourished.deficiency.tab.ok_in", duration(ticks));
            return Component.translatable("nourished.deficiency.tab.down", tier - 1, duration(ticks));
        }
        return pressure > 0 ? Component.translatable("nourished.deficiency.tab.holding") : Component.empty();
    }

    private static String percent(float fraction) {
        return String.format("%+d%%", Math.round(fraction * 100));
    }

    private static String duration(int ticks) {
        int minutes = (int) Math.ceil(Math.max(0, ticks) / 1200.0);
        if (minutes < 1) return "<1m";
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }

    private static Component tierName(int tier) {
        return Component.translatable("nourished.deficiency.tier." + Math.min(3, tier));
    }

    private static int drawWrapped(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        for (var line : font.split(text, Math.max(40, w))) {
            g.drawString(font, line, x, y, color);
            y += 10;
        }
        return y;
    }
}
