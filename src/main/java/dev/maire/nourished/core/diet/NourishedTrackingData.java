package dev.maire.nourished.core.diet;

import dev.maire.nourished.core.network.ModNetworking.SyncDietDeltaPayload;
import dev.marie.framework.tracking.TrackingData;

import java.util.List;
import java.util.Map;

/**
 * Nourished-specific {@link TrackingData} with network delta payload support.
 */
public final class NourishedTrackingData extends TrackingData {

    @Override
    public SyncDietDeltaPayload toDeltaPayload() {
        List<String> recentFoodIds = sourceMemory.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().lastAppliedTick(), a.getValue().lastAppliedTick()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        List<String> neglectedCategories = getMostNeglectedCategories(2);
        List<String> fatiguedFamilies = getMostFatiguedFamilies(2, lastTickTime);
        return new SyncDietDeltaPayload(
                Map.copyOf(values),
                Map.copyOf(lastValues),
                total,
                maxTotal,
                getBalanceScore(),
                recentFoodIds,
                neglectedCategories,
                fatiguedFamilies,
                Map.copyOf(sourceMemory),
                Map.copyOf(categoryMemory),
                Map.copyOf(familyMemory),
                lastTickTime
        );
    }
}
