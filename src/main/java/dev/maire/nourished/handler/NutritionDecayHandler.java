package dev.maire.nourished.handler;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class NutritionDecayHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NourishedConfig config = NourishedConfig.get();
        if (!config.enableDecay()) return;
        int interval = Math.max(1, config.decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;

        DietData data = player.getData(DietAttachment.DIET.get());
        boolean changed = false;
        for (String key : NutrientRegistry.getKeys()) {
            float rate = (float) config.decayRateFor(key);
            float current = data.nutrients.getOrDefault(key, 0f);
            if (current > 0f) {
                data.nutrients.put(key, Math.max(0f, current - rate));
                changed = true;
            }
        }

        if (changed) {
            player.setData(DietAttachment.DIET.get(), data);
            ModNetworking.syncDietDelta(player, data);
        }
    }
}
