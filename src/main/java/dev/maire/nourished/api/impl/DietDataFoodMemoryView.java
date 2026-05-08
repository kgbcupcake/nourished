package dev.maire.nourished.api.impl;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.FoodMemoryView;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.diet.FoodMemoryEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of {@link FoodMemoryView} backed by a player's {@link DietData}.
 */
@ApiStatus.Internal
public final class DietDataFoodMemoryView implements FoodMemoryView {

    private final DietData dietData;

    public DietDataFoodMemoryView(DietData dietData) {
        this.dietData = dietData;
    }

    @Override
    public List<ResourceLocation> getRecentFoods() {
        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        long gameTimeMs = dietData.lastTickTime > 0 ? dietData.lastTickTime : System.currentTimeMillis();

        return dietData.foodMemory.entrySet().stream()
                .filter(e -> !e.getValue().isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f))
                .sorted(Comparator.<Map.Entry<String, FoodMemoryEntry>, Long>comparing(
                        e -> e.getValue().lastEatenTick()).reversed())
                .map(e -> ResourceLocation.parse(e.getKey()))
                .toList();
    }

    @Override
    public boolean hasEatenRecently(ResourceLocation foodId) {
        String key = foodId.toString();
        FoodMemoryEntry entry = dietData.foodMemory.get(key);
        if (entry == null) return false;

        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        long gameTimeMs = dietData.lastTickTime > 0 ? dietData.lastTickTime : System.currentTimeMillis();
        return !entry.isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f);
    }

    @Override
    public long getTimeSinceEaten(ResourceLocation foodId) {
        String key = foodId.toString();
        FoodMemoryEntry entry = dietData.foodMemory.get(key);
        if (entry == null) return -1L;

        long gameTimeMs = dietData.lastTickTime > 0 ? dietData.lastTickTime : System.currentTimeMillis();
        long elapsed = gameTimeMs - entry.lastEatenTick();
        if (elapsed < 0) return -1L;

        // Convert ms to ticks (1 tick = 50ms)
        return elapsed / 50L;
    }
}
