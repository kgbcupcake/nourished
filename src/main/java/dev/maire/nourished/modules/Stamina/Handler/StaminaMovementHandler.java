package dev.maire.nourished.modules.Stamina.Handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.config.NourishedModuleCache;
import dev.maire.nourished.modules.Stamina.Action.StaminaActionType;
import dev.maire.nourished.modules.Stamina.Action.StaminaDrainPipeline;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class StaminaMovementHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!NourishedModuleCache.enableStamina) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.isSprinting() && player.onGround()) {
            StaminaDrainPipeline.apply(player, StaminaActionType.SPRINT);
        }

        if (player.isSwimming() || (player.isInWater() && !player.onGround())) {
            StaminaDrainPipeline.apply(player, StaminaActionType.SWIM);
        }

        if (player.onClimbable() && !player.onGround()) {
            StaminaDrainPipeline.apply(player, StaminaActionType.CLIMB);
        }

        if (player.isFallFlying()) {
            StaminaDrainPipeline.apply(player, StaminaActionType.ELYTRA);
        }
    }
}
