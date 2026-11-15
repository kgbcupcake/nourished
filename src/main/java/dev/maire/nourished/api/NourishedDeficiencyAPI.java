package dev.maire.nourished.api;

import dev.maire.nourished.core.deficiency.DeficiencyTracker;
import net.minecraft.server.level.ServerPlayer;

/** Read and override a player's diet deficiency state. Server-side only. */
public final class NourishedDeficiencyAPI {

    private NourishedDeficiencyAPI() {}

    /** Tier 0-3 for one food group; {@code "any"} gives the worst group. */
    public static int getTier(ServerPlayer player, String group) {
        return DeficiencyTracker.tier(player, group);
    }

    public static int getOverallTier(ServerPlayer player) {
        return DeficiencyTracker.overallTier(player);
    }

    /** Accumulated deficiency for a group, in ticks of play time. */
    public static int getPressure(ServerPlayer player, String group) {
        return DeficiencyTracker.pressure(player, group);
    }

    public static void setPressure(ServerPlayer player, String group, int ticks) {
        DeficiencyTracker.setPressure(player, group, ticks);
    }

    public static void setTier(ServerPlayer player, String group, int tier) {
        DeficiencyTracker.setPressure(player, group, DeficiencyTracker.pressureForTier(tier));
    }

    /** Clears one group, or all groups when {@code group} is null. */
    public static void clear(ServerPlayer player, String group) {
        DeficiencyTracker.clear(player, group);
    }

    /** Multiplier currently applied to activity-driven nutrient drain. */
    public static float getActivityMultiplier(ServerPlayer player) {
        return DeficiencyTracker.activityMultiplier(player);
    }
}
