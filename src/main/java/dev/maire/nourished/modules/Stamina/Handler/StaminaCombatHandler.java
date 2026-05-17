package dev.maire.nourished.modules.Stamina.Handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.modules.Stamina.Action.StaminaActionType;
import dev.maire.nourished.modules.Stamina.Action.StaminaDrainPipeline;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@ApiStatus.Internal
public class StaminaCombatHandler {

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableStamina) return;
        StaminaDrainPipeline.apply(player, StaminaActionType.ATTACK);
    }

    // TODO: Detect missed attacks with a dedicated mixin; NeoForge 1.21.1 does not expose a precise event.
    @SubscribeEvent
    public void onTakeDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableStamina) return;
        if (!StaminaConfig.enableTakeDamage()) return;

        float cost = StaminaConfig.takeDamageCost() * Math.min(event.getNewDamage(), 10f);
        StaminaDrainPipeline.applyRaw(player, StaminaActionType.TAKE_DAMAGE, cost);
    }
}
