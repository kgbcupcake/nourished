package dev.maire.nourished.core.history;

import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.micro.MicroRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Saved daily intake per food group and per nutrient, on MariesLib's daily trackers (same as
 * calorie history, same retention and on/off switch). Each day keeps what was gained that day.
 */
public final class IntakeHistory {

    private IntakeHistory() {}

    @Nullable
    public static ResourceLocation groupId(String key) {
        return id("intake/group/" + key);
    }

    @Nullable
    public static ResourceLocation microId(String key) {
        return id("intake/micro/" + key);
    }

    /** Called at mod init and on every reload/server start, once datapack groups and nutrients are known. */
    public static void registerAll() {
        if (!NourishedConfig.get().enableCalorieHistory()) return;
        int retention = NourishedConfig.get().calorieHistoryRetentionDays();
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            register(groupId(def.key()), retention);
        }
        for (String key : MicroRegistry.server().defs().keySet()) {
            register(microId(key), retention);
        }
    }

    public static void recordGroups(ServerPlayer player, Map<String, Float> deltas) {
        deltas.forEach((key, amount) -> add(player, groupId(key), amount));
    }

    public static void recordMicros(ServerPlayer player, Map<String, Float> gains) {
        gains.forEach((key, amount) -> add(player, microId(key), amount));
    }

    private static void register(@Nullable ResourceLocation id, int retention) {
        if (id != null) MarieTracking.registerTracker(TrackerDefinition.daily(id, retention));
    }

    private static void add(ServerPlayer player, @Nullable ResourceLocation id, float amount) {
        // only gains count; decay isn't intake. Unregistered means history is off or the key is new since the last reload
        if (id == null || amount <= 0f || TrackerRegistry.get(id) == null) return;
        MarieTracking.incrementTracker(player, id, amount);
    }

    @Nullable
    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild(Nourished.MODID, path.toLowerCase(Locale.ROOT));
    }
}
