package dev.maire.nourished.modules.activity_driven_nutrient.core;

import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import net.minecraft.resources.ResourceLocation;

/**
 * MarieLib tracker ids for the activity-driven nutrient module's daily count/distance trackers,
 * plus their registration. Follows the exact same pattern as {@code Nourished#registerCalorieTracker()}
 * — same retention ({@code NourishedConfig#calorieHistoryRetentionDays()}), same
 * {@link TrackerDefinition#daily} throttled-sync tracker, and registered from the same two call
 * sites (mod init and {@code Nourished#reregisterReloadableDefinitions()}) so these survive a
 * {@code /reload} the same way the calorie tracker does.
 */
public final class ActivityTrackerIds {

    public static final ResourceLocation MINING_BLOCKS_ID =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "activity/mining_blocks");
    public static final ResourceLocation COMBAT_KILLS_ID =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "activity/combat_kills");
    public static final ResourceLocation STARVATION_CROSSINGS_ID =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "activity/starvation_crossings");
    public static final ResourceLocation SPRINT_DISTANCE_ID =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "activity/sprint_distance");
    public static final ResourceLocation SWIM_DISTANCE_ID =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "activity/swim_distance");

    private ActivityTrackerIds() {}

    public static void registerAll() {
        int retention = NourishedConfig.get().calorieHistoryRetentionDays();
        MarieTracking.registerTracker(TrackerDefinition.daily(MINING_BLOCKS_ID, retention));
        MarieTracking.registerTracker(TrackerDefinition.daily(COMBAT_KILLS_ID, retention));
        MarieTracking.registerTracker(TrackerDefinition.daily(STARVATION_CROSSINGS_ID, retention));
        MarieTracking.registerTracker(TrackerDefinition.daily(SPRINT_DISTANCE_ID, retention));
        MarieTracking.registerTracker(TrackerDefinition.daily(SWIM_DISTANCE_ID, retention));
    }
}
