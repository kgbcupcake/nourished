package dev.maire.nourished.core.diet;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;

public record DietMemoryConfig(
    long memoryWindowMinutes,
    double noveltyBonus,
    double noveltyDecayCap,
    double diminishingFloor,
    double startingNutrientValue
) {
    /**
     * Create from server-authoritative snapshot.
     * Used at sync boundary (server-side injection after snapshot arrives).
     */
    public static DietMemoryConfig fromSnapshot(SyncNourishedConfigSnapshot snapshot) {
        return new DietMemoryConfig(
            snapshot.memoryWindowMinutes(),
            snapshot.noveltyBonus(),
            snapshot.noveltyDecayCap(),
            snapshot.diminishingFloor(),
            snapshot.startingNutrientValue()
        );
    }

    /**
     * Create from raw config.
     * Used as fallback when snapshot is unavailable (startup race, test code).
     * Must never be called on client; strictly server-only path.
     */
    public static DietMemoryConfig fromRawConfig(NourishedConfig config) {
        return new DietMemoryConfig(
            config.memoryWindowMinutes(),
            config.noveltyBonus(),
            config.noveltyDecayCap(),
            config.diminishingFloor(),
            config.startingNutrientValue()
        );
    }
}
