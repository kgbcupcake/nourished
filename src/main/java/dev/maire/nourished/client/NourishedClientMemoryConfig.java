package dev.maire.nourished.client;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.client.MarieClientState;
import dev.marie.MariesLib.tracking.DiminishingReturnsConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;

@ApiStatus.Internal
public final class NourishedClientMemoryConfig {

    private static final java.util.concurrent.atomic.AtomicBoolean CLIENT_MEMORY_WARN_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private NourishedClientMemoryConfig() {}

    public static void resetClientMemoryDiagnostics() {
        CLIENT_MEMORY_WARN_ONCE.set(false);
    }

    public static DiminishingReturnsConfig get() {
        Object raw = MarieClientState.getConfig();
        if (raw instanceof SyncNourishedConfigSnapshot snap) {
            return new DiminishingReturnsConfig(
                    snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                    snap.diminishingFloor(), snap.startingNutrientValue());
        }
        if (CLIENT_MEMORY_WARN_ONCE.compareAndSet(false, true)) {
            Nourished.LOGGER.warn(
                    "[Nourished] MarieClientCache: config snapshot null, falling back to raw config. Will not warn again until disconnect.");
        }
        NourishedConfig cfg = NourishedConfig.get();
        return new DiminishingReturnsConfig(
                cfg.memoryWindowMinutes(), cfg.noveltyBonus(), cfg.noveltyDecayCap(),
                cfg.diminishingFloor(), cfg.startingNutrientValue());
    }
}
