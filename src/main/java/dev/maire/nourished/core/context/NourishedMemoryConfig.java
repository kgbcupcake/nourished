package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tracking.DiminishingReturnsConfig;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.DiminishingReturnsOverrideRegistry;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

import javax.annotation.Nullable;

@ApiStatus.Internal
public final class NourishedMemoryConfig {

    private NourishedMemoryConfig() {}

    public static DiminishingReturnsConfig serverTrackingMemoryConfig() {
        return serverTrackingMemoryConfig((String) null);
    }

    /**
     * Source-aware variant consulted by MariesLib's {@code SourceApplicationPipeline} per eat/use.
     * When {@code sourceKey} (the eaten item's id) has been flagged via the item editor's
     * "Diminishing Returns Off" row ({@link DiminishingReturnsOverrideRegistry}), that one item gets
     * the same neutral/no-op curve the mod-wide {@code enableDiminishingReturns} toggle produces,
     * regardless of that mod-wide setting — exempting a single food without turning the system off
     * for everything else.
     */
    public static DiminishingReturnsConfig serverTrackingMemoryConfig(@Nullable String sourceKey) {
        boolean perItemOff = sourceKey != null && DiminishingReturnsOverrideRegistry.isDiminishingReturnsOff(sourceKey);
        SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
        if (snap != null) {
            if (perItemOff || !snap.enableDiminishingReturns()) {
                return new DiminishingReturnsConfig(
                        snap.memoryWindowMinutes(), 1.0, 1.0,
                        1.0, snap.startingNutrientValue());
            }
            return new DiminishingReturnsConfig(
                    snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                    snap.diminishingFloor(), snap.startingNutrientValue());
        }
        NourishedConfig cfg = NourishedConfig.get();
        if (perItemOff || !cfg.enableDiminishingReturns()) {
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
