package dev.maire.nourished.handler;

import dev.maire.nourished.attachment.NutritionAttachment;
import dev.maire.nourished.attachment.NutritionData;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.effect.NutritionEffectApplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class NutritionDecayHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NourishedConfig config = NourishedConfig.get();
        int interval = Math.max(1, config.decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;

        NutritionData data = player.getData(NutritionAttachment.NUTRITION);
        data.decay((float) config.decayRate());
        if (config.enableEffects()) {
            NutritionEffectApplier.apply(player, data);
        }
    }
}
