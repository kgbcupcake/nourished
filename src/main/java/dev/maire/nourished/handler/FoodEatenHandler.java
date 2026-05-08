package dev.maire.nourished.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedSeasonHook;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.NutrientAbsorptionModifier;
import dev.maire.nourished.api.NutrientModifierEvent;
import dev.maire.nourished.api.registry.AbsorptionModifierRegistry;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.ModuleCache;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.Map;
import java.util.Optional;

@ApiStatus.Internal
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
        ResourceLocation foodResourceId = BuiltInRegistries.ITEM.getKey(stack.getItem());

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

        Map<String, Float> externalClassification = FoodNutritionRegistry.getExternalClassification(foodResourceId);
        if (externalClassification != null) {
            externalClassification.forEach((key, value) -> nutrientDeltas.merge(key, value, Float::sum));
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

        if (ModuleCache.enableCalorieTracking) {
            Nourished.LOGGER.debug("Nourished calories: adding {} * {} for {}", caloriesAdded, multiplier,
                    stack.getItem().getDescriptionId());
            diet.addCalories(caloriesAdded * multiplier);
        }

        for (String key : NutrientRegistry.getKeys()) {
            float nutrientDelta = nutrientDeltas.getOrDefault(key, 0f);
            if (nutrientDelta != 0f) {
                float adjustedDelta = nutrientDelta * multiplier;
                adjustedDelta = applySeasonalAbsorption(player, key, adjustedDelta);
                adjustedDelta = applyAbsorptionModifiers(player, key, adjustedDelta);

                NutrientModifierEvent modifierEvent = new NutrientModifierEvent(
                        player, foodResourceId, key, adjustedDelta);
                NeoForge.EVENT_BUS.post(modifierEvent);

                if (modifierEvent.isCanceled()) {
                    continue;
                }

                float finalDelta = modifierEvent.getAmount();
                float oldValue = diet.nutrients.getOrDefault(key, 0f);
                diet.addNutrient(key, finalDelta);
                float newValue = diet.nutrients.getOrDefault(key, 0f);

                if (oldValue != newValue) {
                    NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientChangedEvent(
                            player, key, oldValue, newValue));
                }

                NeoForge.EVENT_BUS.post(new NourishedEvents.FoodEatenEvent(
                        player, foodResourceId, key, finalDelta));
            }
        }

        checkThresholdCrossings(player, diet);

        player.setData(DietAttachment.DIET.get(), diet);
        ModNetworking.syncDietDelta(player, diet);

        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }

        Nourished.LOGGER.debug("{} ate {} -> {}",
                player.getName().getString(),
                stack.getItem().getDescriptionId(),
                diet);
    }

    private void checkThresholdCrossings(ServerPlayer player, DietData diet) {
        NourishedConfig config = NourishedConfig.get();
        float excessThreshold = (float) config.excessThreshold();
        for (String key : NutrientRegistry.getKeys()) {
            float current = diet.nutrients.getOrDefault(key, 0f);
            float previous = diet.lastNutrients.getOrDefault(key, 0f);

            float criticalThreshold = (float) config.criticalThresholdFor(key);

            if (current <= criticalThreshold && previous > criticalThreshold) {
                NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientCriticalEvent(player, key));
            }
            if (current >= excessThreshold && previous < excessThreshold) {
                NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientExcessEvent(player, key));
            }
        }
    }

    private float applySeasonalAbsorption(ServerPlayer player, String nutrientKey, float baseAmount) {
        var hooks = SeasonHookRegistry.getAll();
        if (!ModuleCache.enableSeasonHooks || hooks.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (NourishedSeasonHook hook : hooks) {
            float multiplier = Math.max(0f, hook.getSeasonalAbsorptionModifier(nutrientKey, NourishedSeasonHook.Season.SPRING));
            amount *= multiplier;
        }
        return amount;
    }

    private float applyAbsorptionModifiers(ServerPlayer player, String nutrientKey, float baseAmount) {
        var modifiers = AbsorptionModifierRegistry.getAll();
        if (!ModuleCache.enableAbsorptionModifiers || modifiers.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (NutrientAbsorptionModifier modifier : modifiers) {
            float factor = Math.max(0f, modifier.getAbsorptionMultiplier(player, nutrientKey, amount));
            amount *= factor;
        }
        return amount;
    }
}
