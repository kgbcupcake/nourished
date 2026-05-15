package dev.maire.nourished.core.handler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.NourishedSeasonHook;
import dev.maire.nourished.api.NutrientAbsorptionModifier;
import dev.maire.nourished.api.NutrientModifierEvent;
import dev.maire.nourished.api.registry.AbsorptionModifierRegistry;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.debug.NutritionDebugLogger;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry.NutrientResolutionDiagnostic;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.registry.NourishedAttributes;
import dev.maire.nourished.core.util.NourishedItemTags;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import dev.maire.nourished.core.util.NourishedValidation;
import dev.maire.nourished.tooling.scanner.ClassificationResult;
import dev.maire.nourished.tooling.scanner.ClassificationSignal;
import dev.maire.nourished.tooling.scanner.RecipeInheritanceResolver.RecipeInheritanceStep;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * Shared Nourished nutrient application for a food {@link ItemStack} after diet tick bookkeeping.
 */
@ApiStatus.Internal
final class FoodNutrientPipeline {

    private FoodNutrientPipeline() {}

    static void process(ServerPlayer player, ItemStack stack, DietData diet, long gameTimeMs) {
        boolean debugEatLog = ModuleCache.enableDebugLogging;
        Map<String, Float> nutrientsBefore = debugEatLog ? snapshotNutrients(diet) : Map.of();

        String itemId = NourishedRegistryUtils.itemKey(stack).toString();
        Optional<FoodOverrideRegistry.FoodOverride> override = FoodOverrideRegistry.getOverride(itemId);

        float caloriesAdded;
        Map<String, Float> nutrientDeltas;
        Map<String, Float> matchedBars;
        ResourceLocation foodResourceId = NourishedRegistryUtils.itemKey(stack);
        NutrientResolutionDiagnostic diagnostic = null;
        FoodProperties foodProps = FoodNutritionRegistry.foodPropertiesForNutrition(stack, player);

        if (override.isPresent()) {
            FoodOverrideRegistry.FoodOverride ov = override.get();
            caloriesAdded = ov.calories();
            nutrientDeltas = new HashMap<>(ov.nutrients());
            matchedBars = new HashMap<>(ov.nutrients());
            Nourished.LOGGER.debug("Nourished: using override for {} (calories={}, nutrients={})",
                    itemId, caloriesAdded, nutrientDeltas);
        } else {
            if (foodProps == null) {
                return;
            }
            if (debugEatLog) {
                diagnostic = FoodNutritionRegistry.resolveNutrientBarsDiagnostic(stack, player.level());
                matchedBars = new LinkedHashMap<>(diagnostic.matchedBars());
            } else {
                matchedBars = FoodNutritionRegistry.resolveNutrientBars(stack, false, player.level());
            }
            FoodNutritionRegistry.DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                    stack, player.level(), foodProps.nutrition(), foodProps.saturation(), matchedBars);
            caloriesAdded = delta.calories();
            nutrientDeltas = new HashMap<>(delta.nutrients());
        }

        Map<String, Float> matchedBarWeights = new LinkedHashMap<>(matchedBars);

        Map<String, Float> externalClassification = FoodNutritionRegistry.getExternalClassification(foodResourceId);
        if (externalClassification != null) {
            externalClassification.forEach((key, value) -> nutrientDeltas.merge(key, value, Float::sum));
        }

        String dominantCategory = matchedBars.isEmpty()
                ? null
                : matchedBars.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
        String familyKey = FoodFamilyResolver.resolve(
                NourishedRegistryUtils.itemKey(stack));

        float multiplier = diet.recordEat(itemId, dominantCategory, familyKey, gameTimeMs);
        DietData.MultiplierBreakdown multiplierBreakdown = debugEatLog
                ? diet.getMultiplierBreakdown(itemId, dominantCategory, familyKey, gameTimeMs)
                : null;

        if (ModuleCache.enableCalorieTracking) {
            Nourished.LOGGER.debug("Nourished calories: adding {} * {} for {}", caloriesAdded, multiplier,
                    stack.getItem().getDescriptionId());
            diet.addCalories(caloriesAdded * multiplier);
        }

        Map<String, Float> afterMultiplierOnly = new HashMap<>();
        Map<String, Float> finalApplied = new HashMap<>();

        for (String key : NutrientRegistry.getKeys()) {
            float nutrientDelta = nutrientDeltas.getOrDefault(key, 0f);
            if (nutrientDelta != 0f) {
                float adjustedDelta = nutrientDelta * multiplier;
                afterMultiplierOnly.put(key, adjustedDelta);
                adjustedDelta = applySeasonalAbsorption(player, key, adjustedDelta);
                adjustedDelta = applyAbsorptionModifiers(player, key, adjustedDelta);
                adjustedDelta *= NourishedAttributes.nutrientRegenMultiplier(player);

                NutrientModifierEvent modifierEvent = new NutrientModifierEvent(
                        player, foodResourceId, key, adjustedDelta);
                NeoForge.EVENT_BUS.post(modifierEvent);

                if (modifierEvent.isCanceled()) {
                    continue;
                }

                float finalDelta = modifierEvent.getAmount();
                if (!Float.isFinite(finalDelta) || finalDelta < -1f || finalDelta > 1f) {
                    Nourished.LOGGER.warn("[Nourished] Invalid finalDelta {} for player={} item={} nutrient={} — skipping",
                            finalDelta, player.getName().getString(), itemId, key);
                    continue;
                }
                finalApplied.put(key, finalDelta);
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

        if (debugEatLog) {
            Map<String, Float> nutrientsAfter = snapshotNutrients(diet);
            submitNutritionEatDebug(
                    player,
                    stack,
                    gameTimeMs,
                    itemId,
                    foodResourceId,
                    override.isPresent(),
                    diagnostic,
                    matchedBarWeights,
                    nutrientDeltas,
                    afterMultiplierOnly,
                    finalApplied,
                    nutrientsBefore,
                    nutrientsAfter,
                    multiplier,
                    multiplierBreakdown,
                    foodProps
            );
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

    private static Map<String, Float> snapshotNutrients(DietData diet) {
        Map<String, Float> m = new HashMap<>();
        for (String key : NutrientRegistry.getKeys()) {
            m.put(key, diet.nutrients.getOrDefault(key, 0f));
        }
        return m;
    }

    private static void submitNutritionEatDebug(
            ServerPlayer player,
            ItemStack stack,
            long gameTimeMs,
            String itemIdStr,
            ResourceLocation foodResourceId,
            boolean foodOverride,
            @Nullable NutrientResolutionDiagnostic diagnostic,
            Map<String, Float> matchedBarWeights,
            Map<String, Float> rawNutrientDelta,
            Map<String, Float> afterMultiplierOnly,
            Map<String, Float> finalApplied,
            Map<String, Float> nutrientsBefore,
            Map<String, Float> nutrientsAfter,
            float multiplier,
            DietData.MultiplierBreakdown breakdown,
            FoodProperties food
    ) {
        JsonObject root = new JsonObject();
        root.addProperty("timestamp", NutritionDebugLogger.isoTimestamp());
        root.addProperty("classifier_path", foodOverride
                ? "FOOD_OVERRIDE"
                : (diagnostic != null ? diagnostic.classifierPath() : "UNCLASSIFIED"));
        root.addProperty("pipeline_stage", foodOverride
                ? "FOOD_OVERRIDE"
                : (diagnostic != null ? diagnostic.pipelineStage().name() : "UNCLASSIFIED"));

        JsonObject playerJo = new JsonObject();
        playerJo.addProperty("name", player.getName().getString());
        playerJo.addProperty("uuid", player.getUUID().toString());
        root.add("player", playerJo);

        JsonObject itemJo = new JsonObject();
        itemJo.addProperty("id", itemIdStr);
        itemJo.addProperty("namespace", foodResourceId.getNamespace());
        root.add("item", itemJo);

        JsonArray tagMatch = new JsonArray();
        if (foodOverride) {
            tagMatch.add("nourished:food_override");
        } else if (diagnostic == null || diagnostic.matchedNutrientTags().isEmpty()) {
            tagMatch.add("none");
        } else {
            for (String t : diagnostic.matchedNutrientTags()) {
                tagMatch.add(t);
            }
        }
        root.add("tag_match", tagMatch);

        root.add("classifier_signals", buildSignalsJson(foodOverride, diagnostic));
        root.add("matched_bars", NutritionDebugLogger.floatMapToJson(matchedBarWeights));
        root.add("recipe_inheritance", buildRecipeInheritanceJson(foodOverride, diagnostic));
        root.add("raw_nutrient_delta", NutritionDebugLogger.floatMapToJson(rawNutrientDelta));
        root.addProperty("multiplier", NutritionDebugLogger.round4(multiplier));
        root.add("multiplier_breakdown", buildMultiplierBreakdownJson(breakdown));
        root.add("after_multiplier_nutrient_delta", NutritionDebugLogger.floatMapToJson(afterMultiplierOnly));
        root.add("final_nutrient_delta", NutritionDebugLogger.floatMapToJson(finalApplied));
        root.add("nutrients_before", NutritionDebugLogger.floatMapToJson(nutrientsBefore));
        root.add("nutrients_after", NutritionDebugLogger.floatMapToJson(nutrientsAfter));

        boolean lightBypass = food != null
                && (food.nutrition() <= 2 || stack.is(NourishedItemTags.LIGHT_FOOD))
                && !stack.is(NourishedItemTags.MEAL);
        root.addProperty("light_snack_bypass_eligible", lightBypass);

        root.addProperty("game_time_ms", gameTimeMs);
        root.addProperty("game_time_ticks", player.level().getGameTime());
        root.addProperty("dimension", player.level().dimension().location().toString());

        NutritionDebugLogger.submitEatLog(root);
    }

    private static JsonArray buildSignalsJson(boolean foodOverride, @Nullable NutrientResolutionDiagnostic diagnostic) {
        JsonArray arr = new JsonArray();
        if (foodOverride || diagnostic == null) {
            return arr;
        }
        ClassificationResult cr = diagnostic.classification();
        if (cr == null) {
            return arr;
        }
        for (ClassificationSignal s : cr.signals()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", s.signalType());
            o.addProperty("source", s.source());
            o.add("contributions", NutritionDebugLogger.floatMapToJson(s.contributions()));
            o.addProperty("total_magnitude", NutritionDebugLogger.round4(s.totalMagnitude()));
            arr.add(o);
        }
        return arr;
    }

    private static JsonArray buildRecipeInheritanceJson(boolean foodOverride, @Nullable NutrientResolutionDiagnostic diagnostic) {
        JsonArray arr = new JsonArray();
        if (foodOverride || diagnostic == null) {
            return arr;
        }
        for (RecipeInheritanceStep step : diagnostic.recipeInheritance()) {
            JsonObject o = new JsonObject();
            o.addProperty("ingredient_id", step.ingredientId().toString());
            o.addProperty("depth", step.depth());
            o.addProperty("decay_factor", NutritionDebugLogger.round4(step.decayFactor()));
            o.add("nutrient_contributions", NutritionDebugLogger.floatMapToJson(step.nutrientContributions()));
            arr.add(o);
        }
        return arr;
    }

    private static JsonObject buildMultiplierBreakdownJson(DietData.MultiplierBreakdown b) {
        JsonObject o = new JsonObject();
        if (b == null) {
            return o;
        }
        float fin = b.finalMultiplier();
        o.addProperty("item_contribution", NutritionDebugLogger.round4(b.itemContribution()));
        o.addProperty("category_contribution", NutritionDebugLogger.round4(b.categoryContribution()));
        o.addProperty("family_contribution", NutritionDebugLogger.round4(b.familyContribution()));
        o.addProperty("novelty_contribution", NutritionDebugLogger.round4(b.noveltyContribution()));
        o.addProperty("final_multiplier", NutritionDebugLogger.round4(fin));
        o.addProperty("item_weight", NutritionDebugLogger.round4(b.itemWeight()));
        o.addProperty("category_weight", NutritionDebugLogger.round4(b.categoryWeight()));
        o.addProperty("family_weight", NutritionDebugLogger.round4(b.familyWeight()));
        o.addProperty("item_percent_of_final", NutritionDebugLogger.pctOfFinal(b.itemContribution(), fin));
        o.addProperty("category_percent_of_final", NutritionDebugLogger.pctOfFinal(b.categoryContribution(), fin));
        o.addProperty("family_percent_of_final", NutritionDebugLogger.pctOfFinal(b.familyContribution(), fin));
        return o;
    }

    private static void checkThresholdCrossings(ServerPlayer player, DietData diet) {
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

    private static float applySeasonalAbsorption(ServerPlayer player, String nutrientKey, float baseAmount) {
        var hooks = SeasonHookRegistry.getAll();
        if (!ModuleCache.enableSeasonHooks || hooks.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (NourishedSeasonHook hook : hooks) {
            float mult = hook.getSeasonalAbsorptionModifier(nutrientKey, NourishedSeasonHook.Season.SPRING);
            if (!Float.isFinite(mult)) {
                Nourished.LOGGER.warn("[Nourished] Seasonal modifier returned non-finite value {} for nutrient={} — using 0", mult, nutrientKey);
                mult = 0f;
            }
            amount *= Math.max(0f, mult);
        }
        if (!Float.isFinite(amount)) {
            Nourished.LOGGER.warn("[Nourished] Seasonal absorption result non-finite for nutrient={} — using 0", nutrientKey);
            return 0f;
        }
        return amount;
    }

    private static float applyAbsorptionModifiers(ServerPlayer player, String nutrientKey, float baseAmount) {
        var modifiers = AbsorptionModifierRegistry.getAll();
        if (!ModuleCache.enableAbsorptionModifiers || modifiers.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (NutrientAbsorptionModifier modifier : modifiers) {
            float factor = modifier.getAbsorptionMultiplier(player, nutrientKey, amount);
            if (!Float.isFinite(factor)) {
                Nourished.LOGGER.warn("[Nourished] Absorption modifier returned non-finite value {} for nutrient={} — using 0", factor, nutrientKey);
                factor = 0f;
            }
            amount *= Math.max(0f, factor);
        }
        if (!Float.isFinite(amount)) {
            Nourished.LOGGER.warn("[Nourished] Absorption modifier result non-finite for nutrient={} — using 0", nutrientKey);
            return 0f;
        }
        return amount;
    }
}
