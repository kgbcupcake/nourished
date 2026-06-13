package dev.maire.nourished.modules.RawFood.Gut;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.config.NourishedModuleCache;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.core.NourishedKubeIntegration;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles passive gut health recovery and sensitivity decay.
 *
 * <p>Subscribes to {@link PlayerTickEvent.Post} and fires at a configurable
 * tick interval. Applies base recovery, diversity bonus (from diet balance),
 * and sensitivity decay.</p>
 */
@ApiStatus.Internal
public class GutHealthTickHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!NourishedModuleCache.enableGutHealth) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int tickInterval = RawFoodConfig.gutTickInterval();
        if (player.level().getGameTime() % tickInterval != 0) return;

        GutHealthData gut = player.getData(GutHealthAttachment.GUT.get());
        float oldGutHealth = gut.getGutHealth();
        TrackingData diet = player.getData(TrackingAttachment.TRACKING.get());

        gut.applyRecovery(RawFoodConfig.baseRecoveryRate());

        gut.applyDiversityBonus(
                diet.getBalanceScore(),
                RawFoodConfig.diversityThreshold(),
                RawFoodConfig.diversityBonusRate()
        );

        gut.applySensitivityDecay(RawFoodConfig.sensitivityDecayRate());

        gut.setLastUpdateMs(player.level().getGameTime() * 50L);

        player.setData(GutHealthAttachment.GUT.get(), gut);
        ModNetworking.syncGutHealth(player, gut);
        NourishedKubeIntegration.fireGutHealthChanged(
                player.getUUID().toString(), oldGutHealth, gut.getGutHealth(), "variety");
    }
}
