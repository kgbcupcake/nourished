package dev.maire.nourished.client;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.client.config.state.MarieClientState;
import dev.marie.framework.tracking.DiminishingReturnsConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.DiminishingReturnsOverrideRegistry;

import javax.annotation.Nullable;

@ApiStatus.Internal
public final class NourishedClientMemoryConfig {

    private static final java.util.concurrent.atomic.AtomicBoolean CLIENT_MEMORY_WARN_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private NourishedClientMemoryConfig() {}

    public static void resetClientMemoryDiagnostics() {
        CLIENT_MEMORY_WARN_ONCE.set(false);
    }

    public static DiminishingReturnsConfig get() {
        return get(null);
    }

    /**
     * Source-aware variant consulted by MariesLib's {@code MarieTooltipHelper} for the hovered
     * item's "Diminished (N%)" preview — mirrors {@link
     * dev.maire.nourished.core.context.NourishedMemoryConfig#serverTrackingMemoryConfig(String)}'s
     * per-item exemption so the tooltip preview agrees with what actually happens on eat, instead of
     * this client-side curve only ever reflecting the mod-wide setting.
     */
    public static DiminishingReturnsConfig get(@Nullable String sourceKey) {
        boolean perItemOff = sourceKey != null && DiminishingReturnsOverrideRegistry.isDiminishingReturnsOff(sourceKey);
        Object raw = MarieClientState.getConfig();
        if (raw instanceof SyncNourishedConfigSnapshot snap) {
            if (perItemOff) {
                return new DiminishingReturnsConfig(
                        snap.memoryWindowMinutes(), 1.0, 1.0,
                        1.0, snap.startingNutrientValue());
            }
            return new DiminishingReturnsConfig(
                    snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                    snap.diminishingFloor(), snap.startingNutrientValue());
        }
        if (CLIENT_MEMORY_WARN_ONCE.compareAndSet(false, true)) {
            Nourished.LOGGER.warn(
                    "[Nourished] MarieClientCache: config snapshot null, falling back to raw config. Will not warn again until disconnect.");
        }
        NourishedConfig cfg = NourishedConfig.get();
        if (perItemOff) {
            return new DiminishingReturnsConfig(
                    cfg.memoryWindowMinutes(), 1.0, 1.0,
                    1.0, cfg.startingNutrientValue());
        }
        return new DiminishingReturnsConfig(
                cfg.memoryWindowMinutes(), cfg.noveltyBonus(), cfg.noveltyDecayCap(),
                cfg.diminishingFloor(), cfg.startingNutrientValue());
    }
}
