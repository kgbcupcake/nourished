package dev.maire.nourished.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

@ApiStatus.Internal
public class SleepBonusHandler {
    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableSleepBonus) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        if (diet.nutrients.values().stream().allMatch(v -> v >= 0.5f)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, true));
        }
    }
}
