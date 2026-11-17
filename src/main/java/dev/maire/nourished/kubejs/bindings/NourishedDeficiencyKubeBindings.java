package dev.maire.nourished.kubejs.bindings;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.deficiency.DeficiencyConfig;
import dev.maire.nourished.core.deficiency.DeficiencyTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** KubeJS {@code NourishedDeficiency} binding. Server side only; client players read as tier 0. */
@ApiStatus.Experimental
public final class NourishedDeficiencyKubeBindings {

    private NourishedDeficiencyKubeBindings() {}

    /** Tier 0-3 for a group, or the worst group with {@code 'any'}. */
    public static int getTier(Player player, String group) {
        return player instanceof ServerPlayer sp ? DeficiencyTracker.tier(sp, group) : 0;
    }

    public static int getOverallTier(Player player) {
        return player instanceof ServerPlayer sp ? DeficiencyTracker.overallTier(sp) : 0;
    }

    public static Map<String, Integer> getTiers(Player player) {
        Map<String, Integer> out = new HashMap<>();
        if (player instanceof ServerPlayer sp) {
            DeficiencyTracker.pressures(sp).forEach((k, p) -> out.put(k, DeficiencyConfig.get().tierFor(p)));
        }
        return out;
    }

    /** Accumulated deficiency in minutes of play time. */
    public static float getMinutes(Player player, String group) {
        return player instanceof ServerPlayer sp ? DeficiencyTracker.pressure(sp, group) / 1200f : 0f;
    }

    public static void setMinutes(Player player, String group, float minutes) {
        if (player instanceof ServerPlayer sp) DeficiencyTracker.setPressure(sp, group, Math.round(minutes * 1200f));
    }

    public static void setTier(Player player, String group, int tier) {
        if (player instanceof ServerPlayer sp) {
            DeficiencyTracker.setPressure(sp, group, DeficiencyTracker.pressureForTier(tier));
        }
    }

    /** Clears one group, or every group when {@code group} is null. */
    public static void clear(Player player, String group) {
        if (player instanceof ServerPlayer sp) DeficiencyTracker.clear(sp, group);
    }

    public static float getActivityMultiplier(Player player) {
        return player instanceof ServerPlayer sp ? DeficiencyTracker.activityMultiplier(sp) : 1f;
    }
}
