package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tracking.DiminishingReturnsConfig;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

@ApiStatus.Internal
public final class NourishedMemoryConfig {

    private NourishedMemoryConfig() {}

    public static DiminishingReturnsConfig serverTrackingMemoryConfig() {
        SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
        if (snap != null) {
            if (!snap.enableDiminishingReturns()) {
                return new DiminishingReturnsConfig(
                        snap.memoryWindowMinutes(), 1.0, 1.0,
                        1.0, snap.startingNutrientValue());
            }
            return new DiminishingReturnsConfig(
                    snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                    snap.diminishingFloor(), snap.startingNutrientValue());
        }
        NourishedConfig cfg = NourishedConfig.get();
        if (!cfg.enableDiminishingReturns()) {
            return new DiminishingReturnsConfig(
                    cfg.memoryWindowMinutes(), 1.0, 1.0,
                    1.0, cfg.startingNutrientValue());
        }
        return new DiminishingReturnsConfig(
                cfg.memoryWindowMinutes(), cfg.noveltyBonus(), cfg.noveltyDecayCap(),
                cfg.diminishingFloor(), cfg.startingNutrientValue());
    }

    public static DiminishingReturnsConfig clientOrServerMemoryConfig() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DiminishingReturnsConfig base = NourishedClientMemoryConfig.get();
            if (!NourishedConfig.get().enableDiminishingReturns()) {
                return new DiminishingReturnsConfig(
                        base.memoryWindowMinutes(), 1.0, 1.0,
                        1.0, base.startingValueFill());
            }
            return base;
        }
        return serverTrackingMemoryConfig();
    }
}
