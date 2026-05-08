package dev.maire.nourished.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.effect.NutritionEffectApplier;
import dev.maire.nourished.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@ApiStatus.Internal
public class DietPlayerEvents {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        ModNetworking.syncDiet(player, diet);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        ModNetworking.syncDiet(player, diet);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        ModNetworking.syncDietDelta(player, diet);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }
}
