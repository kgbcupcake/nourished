package dev.maire.nourished.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.effect.NutritionEffectApplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps nutrition-linked mob effects in sync with diet data independently of nutrient decay.
 * Decay being disabled must not strand effects or skip {@link NutritionEffectApplier#apply}.
 */
@ApiStatus.Internal
public class NutritionEffectsHandler {

    private static final int APPLY_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % APPLY_INTERVAL_TICKS != 0) return;

        if (ModuleCache.enableEffects) {
            DietData data = player.getData(DietAttachment.DIET.get());
            NutritionEffectApplier.apply(player, data);
            for (String oldId : EffectRegistry.getPreviousEffectIds()) {
                boolean stillRegistered = EffectRegistry.getAll().stream()
                        .anyMatch(d -> d.effect().equals(oldId));
                if (!stillRegistered) {
                    BuiltInRegistries.MOB_EFFECT
                            .getHolder(ResourceLocation.parse(oldId))
                            .ifPresent(player::removeEffect);
                }
            }
        } else {
            NutritionEffectApplier.clearAll(player);
        }
    }
}
