package dev.maire.nourished.handler;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.effect.NutritionEffectApplier;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.FoodFamilyResolver;
import dev.maire.nourished.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.Map;
import java.util.Optional;

public class FoodEatenHandler {

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) return;

        DietData diet = player.getData(DietAttachment.DIET.get());
        long gameTimeMs = player.level().getGameTime() * 50L;
        diet.tickTime(gameTimeMs);
        diet.tick();

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Optional<FoodOverrideRegistry.FoodOverride> override = FoodOverrideRegistry.getOverride(itemId);

        float caloriesAdded;
        Map<String, Float> nutrientDeltas;
        Map<String, Float> matchedBars;

        if (override.isPresent()) {
            FoodOverrideRegistry.FoodOverride ov = override.get();
            caloriesAdded = ov.calories();
            nutrientDeltas = ov.nutrients();
            matchedBars = nutrientDeltas; // Use override nutrients for category detection
            Nourished.LOGGER.debug("Nourished: using override for {} (calories={}, nutrients={})",
                    itemId, caloriesAdded, nutrientDeltas);
        } else {
            matchedBars = FoodNutritionRegistry.resolveNutrientBars(stack, false);
            FoodNutritionRegistry.DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                    stack, player.level(), food.nutrition(), food.saturation(), matchedBars);
            caloriesAdded = delta.calories();
            nutrientDeltas = delta.nutrients();
        }

        // Resolve dominant category (first/highest match) and food family
        String dominantCategory = matchedBars.isEmpty() ? "grains" :
                matchedBars.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("grains");
        String familyKey = FoodFamilyResolver.resolve(
                BuiltInRegistries.ITEM.getKey(stack.getItem()));

        float multiplier = diet.recordEat(itemId, dominantCategory, familyKey, gameTimeMs);

        if (NourishedConfig.get().enableCalorieTracking()) {
            Nourished.LOGGER.debug("Nourished calories: adding {} * {} for {}", caloriesAdded, multiplier,
                    stack.getItem().getDescriptionId());
            diet.addCalories(caloriesAdded * multiplier);
        }

        for (String key : NutrientRegistry.getKeys()) {
            float nutrientDelta = nutrientDeltas.getOrDefault(key, 0f);
            if (nutrientDelta != 0f) {
                diet.addNutrient(key, nutrientDelta * multiplier);
            }
        }

        ModNetworking.syncDietDelta(player, diet);

        if (NourishedConfig.get().enableEffects()) {
            NutritionEffectApplier.apply(player, diet);
        }

        Nourished.LOGGER.debug("{} ate {} -> {}",
                player.getName().getString(),
                stack.getItem().getDescriptionId(),
                diet);
    }
}
