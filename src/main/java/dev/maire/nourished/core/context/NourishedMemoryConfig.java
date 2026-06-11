package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

@ApiStatus.Internal
public final class NourishedMemoryConfig {

    private NourishedMemoryConfig() {}

    public static TrackingMemoryConfig serverTrackingMemoryConfig() {
        SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
        if (snap != null) {
            return new TrackingMemoryConfig(
                    snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                    snap.diminishingFloor(), snap.startingNutrientValue());
        }
        NourishedConfig cfg = NourishedConfig.get();
        return new TrackingMemoryConfig(
                cfg.memoryWindowMinutes(), cfg.noveltyBonus(), cfg.noveltyDecayCap(),
                cfg.diminishingFloor(), cfg.startingNutrientValue());
    }

    public static TrackingMemoryConfig clientOrServerMemoryConfig() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return NourishedClientMemoryConfig.get();
        }
        return serverTrackingMemoryConfig();
    }
}
