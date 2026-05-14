package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.core.nutrition.cache.RunningAverage;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Immutable context passed through the resolution pipeline to each stage handler.
 */
public record StageContext(
        Holder<Item> holder,
        ResourceLocation itemId,
        @Nullable RecipeManager recipeManager,
        Map<String, RunningAverage> namespacePeers,
        Set<String> validKeys
) {}
