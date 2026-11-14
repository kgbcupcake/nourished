package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.marie.framework.classification.ClassificationTraceStep;
import dev.marie.framework.classification.TraceStepId;
import dev.marie.framework.classification.TraceStepStatus;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.StageContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Synthetic-recipe slot of the cascade, only run by MariesLib when the item has no recipe at
 * all. Builds a recipe from the item's name instead: each name token (and adjacent pair, for
 * things like sweet_berries) is matched to registered items with that exact path, and their
 * tag classifications are averaged like real ingredients.
 */
public final class SyntheticRecipeStage implements ResolutionStageHandler {

    private static final int MIN_TOKEN_LENGTH = 3;
    // id suffixes some mods tack onto every item (pamhc2 "...item")
    private static final String[] PATH_NOISE_SUFFIXES = {"item"};

    private static volatile Map<String, List<ResourceLocation>> pathIndex;

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        try {
            ResolutionResult synthetic = resolveSynthetic(itemId, ctx);
            if (synthetic == null) ctx.setRecipeFailureReason("NO_RECIPE_FOUND+SYNTHETIC_NO_MATCH");
            return synthetic;
        } catch (Exception e) {
            Nourished.LOGGER.warn("[SyntheticRecipe] Failed for {}: {}", itemId, e.getMessage());
            ctx.setRecipeFailureReason("SYNTHETIC_EXCEPTION");
            return null;
        }
    }

    @Nullable
    private ResolutionResult resolveSynthetic(ResourceLocation itemId, StageContext ctx) {
        Map<String, Float> total = new HashMap<>();
        List<String> usedTokens = new ArrayList<>();

        for (String token : candidateTokens(itemId.getPath())) {
            Map<String, Float> tokenValues = classifyToken(token, itemId);
            emitTrace(ctx, token, tokenValues);
            if (tokenValues.isEmpty()) continue;
            tokenValues.forEach((k, v) -> total.merge(k, v, Float::sum));
            usedTokens.add(token);
        }
        if (usedTokens.isEmpty()) return null;

        Map<String, Float> averaged = new HashMap<>();
        total.forEach((k, v) -> averaged.put(k, v / usedTokens.size()));

        Map<String, Float> filtered = new LinkedHashMap<>();
        averaged.forEach((k, v) -> {
            if (ctx.validKeys().contains(k) && v > 0f) filtered.put(k, v);
        });
        if (filtered.isEmpty()) return null;

        Map<String, String> rejected = new LinkedHashMap<>();
        for (String key : ctx.validKeys()) {
            if (!filtered.containsKey(key)) rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
        }
        float score = 0f;
        for (float v : filtered.values()) score += v;

        if (Nourished.LOGGER.isDebugEnabled()) {
            Nourished.LOGGER.debug("[SyntheticRecipe] {} — tokens={} → {}", itemId, usedTokens, filtered);
        }
        return new ResolutionResult(
                filtered, Map.copyOf(averaged),
                List.copyOf(usedTokens), Map.of(), rejected,
                false, score, RuntimeCascadeStage.SYNTHETIC_RECIPE,
                "synthetic recipe from name " + usedTokens);
    }

    // a token can match several mods' items (every mod's "tomato"); take the strongest per nutrient
    private static Map<String, Float> classifyToken(String token, ResourceLocation self) {
        Map<String, Float> values = new LinkedHashMap<>();
        for (ResourceLocation id : index().getOrDefault(token, List.of())) {
            if (id.equals(self)) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            FoodNutritionRegistry.getNutrientTagScores(item).forEach((k, v) -> values.merge(k, v, Math::max));
        }
        return values;
    }

    static Set<String> candidateTokens(String path) {
        String cleaned = stripNoise(path.replaceAll("[^a-z_]", "_"));
        String[] parts = cleaned.split("_+");
        Set<String> tokens = new LinkedHashSet<>();
        for (int i = 0; i < parts.length; i++) {
            if (i + 1 < parts.length && !parts[i].isEmpty() && !parts[i + 1].isEmpty()) {
                tokens.add(parts[i] + "_" + parts[i + 1]);
            }
            if (parts[i].length() >= MIN_TOKEN_LENGTH) tokens.add(parts[i]);
        }
        tokens.remove(cleaned);
        // a pair hit already covers its parts; drop the parts so they aren't counted twice
        Set<String> result = new LinkedHashSet<>();
        Set<String> covered = new LinkedHashSet<>();
        for (String t : tokens) {
            if (t.contains("_") && index().containsKey(t)) {
                result.add(t);
                covered.addAll(List.of(t.split("_")));
            }
        }
        for (String t : tokens) {
            if (!t.contains("_") && !covered.contains(t)) result.add(t);
        }
        return result;
    }

    private static String stripNoise(String path) {
        for (String suffix : PATH_NOISE_SUFFIXES) {
            if (path.length() > suffix.length() + MIN_TOKEN_LENGTH && path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return path;
    }

    // item paths are fixed once registries freeze, so this is built once
    private static Map<String, List<ResourceLocation>> index() {
        Map<String, List<ResourceLocation>> idx = pathIndex;
        if (idx != null) return idx;
        synchronized (SyntheticRecipeStage.class) {
            if (pathIndex == null) {
                Map<String, List<ResourceLocation>> built = new HashMap<>();
                for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                    String key = stripNoise(id.getPath());
                    built.computeIfAbsent(key, k -> new ArrayList<>()).add(id);
                    // plural paths (sweet_berries, carrots) also answer to the singular token
                    if (key.endsWith("ies") && key.length() > MIN_TOKEN_LENGTH + 2) {
                        built.computeIfAbsent(key.substring(0, key.length() - 3) + "y", k -> new ArrayList<>()).add(id);
                    } else if (key.endsWith("s") && key.length() > MIN_TOKEN_LENGTH + 1) {
                        built.computeIfAbsent(key.substring(0, key.length() - 1), k -> new ArrayList<>()).add(id);
                    }
                }
                pathIndex = built;
            }
            return pathIndex;
        }
    }

    private static void emitTrace(StageContext ctx, String token, Map<String, Float> values) {
        if (ctx.traceOut() == null) return;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("ingredientId", "synthetic:" + token);
        detail.put("source", "SYNTHETIC");
        if (!values.isEmpty()) detail.put("values", new LinkedHashMap<>(values));
        boolean hit = !values.isEmpty();
        ctx.addTraceStep(new ClassificationTraceStep(
                TraceStepId.INGREDIENT_RESOLUTION,
                hit ? TraceStepStatus.SUCCESS : TraceStepStatus.FAILURE,
                hit ? "Synthetic ingredient '" + token + "' classified via TAG"
                        : "Synthetic ingredient '" + token + "' unclassified",
                detail));
    }
}
