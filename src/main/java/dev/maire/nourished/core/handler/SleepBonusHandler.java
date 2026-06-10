package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
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
        TrackingData diet = player.getData(TrackingAttachment.TRACKING.get());
        if (diet.values.values().stream().allMatch(v -> v >= 0.5f)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, true));
        }
    }
}
