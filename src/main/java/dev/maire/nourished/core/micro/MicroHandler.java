package dev.maire.nourished.core.micro;

import dev.maire.nourished.config.MicroConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/** Server-side gain, decay and sync of nutrient levels. */
public final class MicroHandler {

    /** Adds one bite's worth of {@code stack}'s nutrients to the player; called from the eat handler. */
    public static void onFoodEaten(ServerPlayer player, ItemStack stack, FoodProperties food) {
        if (!MicroRegistry.server().settings().enabled()) return;
        Map<String, Float> profile = MicroProfileResolver.resolve(stack, player.level());
        if (profile.isEmpty()) return;
        float bite = MicroProfileResolver.biteGain(food) * MicroConfig.get().gainScale();
        Map<String, Float> levels = new HashMap<>(MicroAttachment.get(player));
        profile.forEach((k, v) -> levels.put(k, Math.min(1f, levels.getOrDefault(k, 0f) + v * bite)));
        MicroAttachment.set(player, levels);
        MicroNetworking.syncLevels(player);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MicroRegistry.server().settings().enabled()) return;
        if (player.isCreative() || player.isSpectator()) return;
        long time = player.level().getGameTime();
        if (time % MicroEffects.CHECK_INTERVAL_TICKS == 0) MicroEffects.apply(player);
        if (time % MicroConfig.get().decayIntervalTicks() != 0) return;
        Map<String, Float> current = MicroAttachment.get(player);
        if (current.isEmpty()) return;
        Map<String, Float> next = new HashMap<>();
        boolean changed = false;
        for (var e : current.entrySet()) {
            MicroDef def = MicroRegistry.server().defs().get(e.getKey());
            float rate = def != null ? def.decayRate() : 0.01f;
            float v = Math.max(0f, e.getValue() - rate);
            changed |= v != e.getValue();
            if (v > 0f || def != null) next.put(e.getKey(), v);
        }
        if (changed) {
            MicroAttachment.set(player, next);
            MicroNetworking.syncLevels(player);
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(p -> {
            MicroNetworking.syncData(p);
            MicroNetworking.syncLevels(p);
        });
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MicroNetworking.syncLevels(player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MicroNetworking.syncLevels(player);
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        MicroProfileResolver.clearCache();
    }
}
