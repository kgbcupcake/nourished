package dev.maire.nourished.handler;

import dev.maire.nourished.attachment.NutritionAttachment;
import dev.maire.nourished.attachment.NutritionData;
import dev.maire.nourished.effect.NutritionEffectApplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class NutritionDecayHandler {

    /** Decay rate per tick (~0.00001 per tick = ~0.0006 per second at 20 TPS). Tune as needed. */
    private static final float DECAY_RATE = 0.00001f;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 100 != 0) return; // every 5 seconds

        NutritionData data = player.getData(NutritionAttachment.NUTRITION);
        data.decay(DECAY_RATE * 100);
        NutritionEffectApplier.apply(player, data);
    }
}
