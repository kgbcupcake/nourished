package dev.maire.nourished.core.impl;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.FoodMemoryView;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Null-object {@link FoodMemoryView} returned when API queries receive a null player.
 */
@ApiStatus.Internal
public final class EmptyFoodMemoryView implements FoodMemoryView {

    public static final EmptyFoodMemoryView INSTANCE = new EmptyFoodMemoryView();

    private EmptyFoodMemoryView() {}

    @Override
    public List<ResourceLocation> getRecentFoods() {
        return List.of();
    }

    @Override
    public boolean hasEatenRecently(ResourceLocation foodId) {
        return false;
    }

    @Override
    public long getTimeSinceEaten(ResourceLocation foodId) {
        return -1L;
    }
}
