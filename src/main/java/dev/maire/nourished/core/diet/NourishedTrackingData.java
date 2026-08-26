package dev.maire.nourished.core.diet;

import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.core.network.ModNetworking.SyncDietDeltaPayload;
import dev.marie.framework.tracking.TrackingData;

import java.util.List;
import java.util.Map;

/**
 * Nourished-specific {@link TrackingData} with network delta payload support.
 */
public final class NourishedTrackingData extends TrackingData {

    /**
     * {@code recentFoodIds} comes from {@link DietAttachment#RECENT_MEALS} rather than being
     * tracked on this class, since that attachment has its own persisted codec (this class's
     * extra fields would not survive relogin — {@code TrackingData}'s codec only knows about its
     * own declared fields) and is only ever updated by {@link
     * dev.maire.nourished.core.handler.NourishedFoodTriggerHandler} when a meal actually changed
     * something, never on a no-op eat.
     */
    public SyncDietDeltaPayload toDeltaPayload(
            List<String> recentFoodIds,
            SyncDietDeltaPayload.FoodEatenDelta foodEatenDelta
    ) {
        List<String> neglectedCategories = getMostNeglectedCategories(2);
        List<String> fatiguedFamilies = getMostFatiguedFamilies(2, lastTickTime);
        return new SyncDietDeltaPayload(
                Map.copyOf(values),
                Map.copyOf(lastValues),
                total,
                maxTotal,
                trackingAccumulators.getOrDefault(NourishedAPI.CALORIES_TRACKER_ID, 0f),
                getBalanceScore(),
                recentFoodIds,
                neglectedCategories,
                fatiguedFamilies,
                Map.copyOf(sourceMemory),
                Map.copyOf(categoryMemory),
                Map.copyOf(familyMemory),
                lastTickTime,
                foodEatenDelta
        );
    }
}
