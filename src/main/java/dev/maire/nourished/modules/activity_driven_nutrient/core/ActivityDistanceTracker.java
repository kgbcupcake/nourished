package dev.maire.nourished.modules.activity_driven_nutrient.core;

import dev.marie.framework.tracking.TrackerMilestoneTracker;
import dev.marie.framework.tracking.tracker.MarieTracking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player last-known-position cache backing the sprint/swim distance trackers. Updated once
 * per server player tick by {@code ActivityDistanceTickListener}.
 *
 * <p>A tick is treated as a teleport/discontinuity — position cached but no distance recorded —
 * when it's the first tick seen for that player, their dimension changed since last tick, or the
 * distance moved this tick exceeds {@link #MAX_BLOCKS_PER_TICK}. That cap is set well above
 * anything reachable through legitimate movement: even aggressively stacked elytra+firework
 * boosting tops out in the ballpark of 8-10 blocks/tick, so 20 blocks/tick leaves comfortable
 * headroom for that plus speed potions/soul speed/ice, while staying far below any real
 * teleport or portal jump (typically many blocks, if not a full dimension change).</p>
 */
public final class ActivityDistanceTracker {

    private static final double MAX_BLOCKS_PER_TICK = 20.0;
    private static final double MAX_BLOCKS_PER_TICK_SQ = MAX_BLOCKS_PER_TICK * MAX_BLOCKS_PER_TICK;

    private record PositionRecord(Vec3 pos, ResourceKey<Level> dimension) {}

    private static final Map<UUID, PositionRecord> LAST_POSITION = new ConcurrentHashMap<>();

    private ActivityDistanceTracker() {}

    /** Call once per server player tick to update the cache and accumulate sprint/swim distance. */
    public static void onPlayerTick(ServerPlayer player) {
        if (!ActivityDrivenNutrientConfig.get().enabled()) {
            // Drop the cache so re-enabling starts from a fresh cache-and-return
            // instead of computing a delta against a stale pre-disable position.
            LAST_POSITION.remove(player.getUUID());
            return;
        }

        UUID id = player.getUUID();
        Vec3 currentPos = player.position();
        ResourceKey<Level> currentDim = player.level().dimension();

        PositionRecord last = LAST_POSITION.get(id);
        if (last == null || !last.dimension().equals(currentDim)) {
            LAST_POSITION.put(id, new PositionRecord(currentPos, currentDim));
            return;
        }

        double distSq = currentPos.distanceToSqr(last.pos());
        if (distSq > MAX_BLOCKS_PER_TICK_SQ) {
            LAST_POSITION.put(id, new PositionRecord(currentPos, currentDim));
            return;
        }

        if (distSq > 0) {
            double delta = Math.sqrt(distSq);
            if (player.isSprinting() && ActivityDrivenNutrientConfig.get().sprintEnabled()) {
                MarieTracking.incrementTracker(player, ActivityTrackerIds.SPRINT_DISTANCE_ID, (float) delta);
                TrackerMilestoneTracker.onTrackerIncremented(player, ActivityTrackerIds.SPRINT_DISTANCE_ID, (float) delta);
            }
            if (player.isSwimming() && player.isInWater() && ActivityDrivenNutrientConfig.get().swimEnabled()) {
                MarieTracking.incrementTracker(player, ActivityTrackerIds.SWIM_DISTANCE_ID, (float) delta);
                TrackerMilestoneTracker.onTrackerIncremented(player, ActivityTrackerIds.SWIM_DISTANCE_ID, (float) delta);
            }
        }

        LAST_POSITION.put(id, new PositionRecord(currentPos, currentDim));
    }

    /** Drops a player's cached position, avoiding an unbounded map across logins/logouts. */
    public static void clear(UUID playerId) {
        LAST_POSITION.remove(playerId);
    }
}
