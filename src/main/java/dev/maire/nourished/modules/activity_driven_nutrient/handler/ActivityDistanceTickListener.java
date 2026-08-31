package dev.maire.nourished.modules.activity_driven_nutrient.handler;

import dev.marie.framework.tracking.TrackingAttachment;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDistanceTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Feeds {@link ActivityDistanceTracker} from the server player tick, and clears its per-player
 * cache on logout so it can't grow unbounded. A new, Nourished-owned listener — deliberately
 * separate from marie-core's {@code ValueEffectsListener}, which is that library's generic tick
 * dispatch and out of scope here. Cheap enough (a {@code Vec3} distance calc and a map lookup, no
 * allocation-heavy work) to run every tick for every online player rather than gating it behind
 * an interval like other tick handlers.
 */
public final class ActivityDistanceTickListener {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TrackingAttachment.isRegistered()) return;
        ActivityDistanceTracker.onPlayerTick(player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ActivityDistanceTracker.clear(player.getUUID());
    }
}
