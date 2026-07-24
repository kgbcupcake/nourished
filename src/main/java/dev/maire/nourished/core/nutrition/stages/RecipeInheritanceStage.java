package dev.maire.nourished.core.nutrition.stages;

import dev.marie.framework.compat.ModCompat;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.runtime.ComponentClassifier;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.StageContext;
import dev.marie.framework.scanner.RecipeInheritanceResolver;
import dev.marie.framework.util.MarieRegistryUtils;
import dev.marie.framework.classification.ClassificationTraceStep;
import dev.marie.framework.classification.TraceStepId;
import dev.marie.framework.classification.TraceStepStatus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 3: inherits nutrient classification from confirmed (tag-based) recipe ingredients.
 * Requires at least 2 confirmed ingredient classifications. Ingredient discovery is
 * delegated to {@link RecipeInheritanceResolver}'s pre-built index.
 */
public final class RecipeInheritanceStage implements ResolutionStageHandler {

    private static final Set<ResourceLocation> RECIPE_SKIP = Set.of(
            ResourceLocation.withDefaultNamespace("air"),
            ResourceLocation.withDefaultNamespace("barrier")
    );

    private static final String[] PAMHC2_RECIPE_TOOL_PATH_SUFFIXES = {
            "bakewareitem", "cuttingboarditem", "potitem", "saucepanitem", "mixingbowlitem",
            "skilletitem", "juiceritem", "grinderitem", "rolleritem"
    };

    private static final Set<String> CROPTOPIA_RECIPE_TOOL_PATHS = Set.of(
            "frying_pan", "cooking_pot", "food_press", "knife"
    );

    private static final Set<String> NON_FOOD_RECIPE_INGREDIENT_IDS = Set.of(
            "butchery:salt",
            "farm_and_charm:yeast",
            "croptopia:water_bottle",
            "croptopia:tomato_seed",
            "croptopia:onion_seed",
            "croptopia:olive_oil",
            "pamhc2foodcore:cookingoilitem",
            "pamhc2foodextended:saltandpepperitem",
            "pamhc2foodextended:sesameoilitem",
            "pamhc2foodextended:blackpepperitem",
            "pamhc2foodextended:vanillaitem",
            "pamhc2foodcore:stockitem",
            "pamhc2foodcore:vinegaritem",
            "croptopia:soy_sauce",
            "createfood:taco_sauce_bottle",
            "createfood:chocolate_chips",
            "createfood:white_chocolate_chips",
            "createfood:dark_chocolate_chips",
            "createfood:butterscotch",
            "createfood:caramel_chips",
            "createfood:toffee_chips",
            "createfood:cocoa_powder",
            "createfood:powdered_sugar",
            "createfood:shredded_potato",
            "createfood:chocolate_donut_base",
            "rusticdelight:cooking_oil",
            "herbsandharvest:spice_jar_item",
            "herbsandharvest:salt",
            "alltheores:salt",
            "pamhc2trees:maplesyrupitem",
            "expandeddelight:peanut_butter",
            "croptopia:strawberry_seed",
            "croptopia:paprika",
            "brewery:hops"
    );

    private final RecipeInheritanceResolver recipeResolver;
    private final CommunityTagStage communityTagStage;
    private final KeywordSuffixStage keywordSuffixStage;

    public RecipeInheritanceStage(
            RecipeInheritanceResolver recipeResolver,
            CommunityTagStage communityTagStage,
            KeywordSuffixStage keywordSuffixStage
    ) {
        this.recipeResolver = recipeResolver;
        this.communityTagStage = communityTagStage;
        this.keywordSuffixStage = keywordSuffixStage;
    }

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        RecipeManager recipeManager = ctx.recipeManager();
        if (recipeManager == null) {
            ctx.setRecipeFailureReason("NULL_RECIPE_MANAGER");
            return null;
        }

        if (Nourished.LOGGER.isDebugEnabled()) {
            Nourished.LOGGER.debug("[RecipeInheritance] Resolving: {}", itemId);
        }

        try {
            List<ResourceLocation> ingredients = recipeResolver.getIngredients(itemId);

            if (ingredients.isEmpty()) {
                ctx.setRecipeFailureReason("NO_RECIPE_FOUND");
                return null;
            }

            Map<String, Float> totalContribs = new HashMap<>();
            int confirmed = 0;

            for (ResourceLocation ingredientId : ingredients) {
                if (shouldSkipRecipeIngredient(ingredientId)) {
                    if (Nourished.LOGGER.isDebugEnabled()) {
                        Nourished.LOGGER.debug("[RecipeInheritance]   ingredient {} → SKIPPED", ingredientId);
                    }
                    emitIngredientResolutionStep(ctx, ingredientId, TraceStepStatus.SKIPPED,
                            "INGREDIENT_SKIPPED", null, false, "tool/non-food ingredient");
                    continue;
                }

                Item ingredientItem = BuiltInRegistries.ITEM.get(ingredientId);
                if (ingredientItem == null) {
                    emitIngredientResolutionStep(ctx, ingredientId, TraceStepStatus.FAILURE,
                            "NONE", null, false, "item not found");
                    continue;
                }

                Map<String, Float> tagMatches = collectConfirmedNutrientTags(ingredientItem, ingredientId, ctx);
                if (Nourished.LOGGER.isDebugEnabled()) {
                    Nourished.LOGGER.debug("[RecipeInheritance]   ingredient {} → tags: {} (confirmed: {})",
                            ingredientId, tagMatches, confirmed);
                }
                if (!tagMatches.isEmpty()) {
                    for (Map.Entry<String, Float> e : tagMatches.entrySet()) {
                        totalContribs.merge(e.getKey(), e.getValue(), Float::sum);
                    }
                    confirmed++;
                    emitIngredientResolutionStep(ctx, ingredientId, TraceStepStatus.SUCCESS,
                            "TAG", tagMatches, false, null);
                } else {
                    emitIngredientResolutionStep(ctx, ingredientId, TraceStepStatus.FAILURE,
                            "NONE", null, false, "INGREDIENT_UNCLASSIFIED");
                }
            }

            if (Nourished.LOGGER.isDebugEnabled()) {
                Nourished.LOGGER.debug("[RecipeInheritance] {} — confirmed={}, contributions={}",
                        itemId, confirmed, totalContribs);
            }

            if (confirmed < 2 && !(confirmed == 1 && ingredients.size() == 1)) {
                if (Nourished.LOGGER.isDebugEnabled()) {
                    Nourished.LOGGER.debug("[RecipeInheritance] {} — FAILED: only {} confirmed ingredient(s), need 2",
                            itemId, confirmed);
                }
                ctx.setRecipeFailureReason("CONFIRMED_THRESHOLD_FAILED:confirmed=" + confirmed + "/total=" + ingredients.size());
                return null;
            }

            Map<String, Float> averaged = new HashMap<>();
            for (Map.Entry<String, Float> e : totalContribs.entrySet()) {
                averaged.put(e.getKey(), e.getValue() / confirmed);
            }

            Map<String, Float> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Float> e : averaged.entrySet()) {
                if (ctx.validKeys().contains(e.getKey()) && e.getValue() > 0f) {
                    filtered.put(e.getKey(), e.getValue());
                }
            }
            if (filtered.isEmpty()) return null;

            Map<String, Float> qualifying = filtered;

            if (Nourished.LOGGER.isDebugEnabled()) {
                Nourished.LOGGER.debug("[RecipeInheritance] {} — SUCCESS: {}", itemId, qualifying);
            }

            Map<String, String> rejectedSignals = new LinkedHashMap<>();
            for (String key : ctx.validKeys()) {
                if (!qualifying.containsKey(key)) {
                    rejectedSignals.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
                }
            }

            float totalScore = 0f;
            for (float v : filtered.values()) totalScore += v;
            return new ResolutionResult(
                    qualifying, Map.copyOf(averaged),
                    List.of(), Map.of(), rejectedSignals,
                    false, totalScore, RuntimeCascadeStage.RECIPE_INHERITANCE,
                    "recipe ingredient inheritance");

        } catch (Exception e) {
            Nourished.LOGGER.warn("[RuntimeFoodResolver] Recipe exception for {}: {}", itemId, e.getMessage());
            ctx.setRecipeFailureReason("RECIPE_EXCEPTION");
            return null;
        }
    }

    private static boolean shouldSkipRecipeIngredient(ResourceLocation id) {
        if (RECIPE_SKIP.contains(id)) return true;
        String path = id.getPath();
        if (path.contains("debug") || path.contains("test") || path.contains("internal")) return true;
        for (String suffix : PAMHC2_RECIPE_TOOL_PATH_SUFFIXES) {
            if (path.endsWith(suffix)) return true;
        }
        if ("croptopia".equals(id.getNamespace()) && CROPTOPIA_RECIPE_TOOL_PATHS.contains(path)) {
            return true;
        }
        if (NON_FOOD_RECIPE_INGREDIENT_IDS.contains(id.toString())) {
            return true;
        }
        if ("theurgy".equals(id.getNamespace())) {
            return true;
        }
        if ("minecraft".equals(id.getNamespace())) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null) {
                FoodProperties food = item.components().get(DataComponents.FOOD);
                if (food == null) return true;
            }
        }
        return false;
    }

    /**
     * Confirmed (tag-based) nutrient classifications only — no inference.
     * Mirrors {@link dev.maire.nourished.core.nutrition.FoodNutritionRegistry#collectNutrientTagMatches}
     * logic including compat toggles.
     */
    private Map<String, Float> collectConfirmedNutrientTags(Item item, ResourceLocation itemId, StageContext ctx) {
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = new ItemStack(item).getItemHolder();

        boolean missingSomeKey = false;
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            boolean found = false;
            for (String tagStr : def.tags()) {
                var tagKey = MarieRegistryUtils.itemTagKey(tagStr);
                if (holder.is(tagKey)) {
                    matches.put(def.key(), 1.0f);
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingSomeKey = true;
            }
        }

        if (missingSomeKey) {
            ResolutionResult classified = ComponentClassifier.classify(
                    new ItemStack(item),
                    null,
                    ctx.validKeys(),
                    List.of(communityTagStage, keywordSuffixStage)
            );
            if (classified != null && !classified.values().isEmpty()) {
                float spreadThreshold = (float) NourishedConfig.get().scannerConfidenceSpreadThreshold();
                boolean uncertain = spreadThreshold > 0f && classified.confidence() < spreadThreshold;
                if (!uncertain) {
                    for (Map.Entry<String, Float> e : classified.values().entrySet()) {
                        matches.putIfAbsent(e.getKey(), e.getValue());
                    }
                }
            }
        }

        matches.entrySet().removeIf(entry -> {
            String namespace = itemId.getNamespace();
            String modid = ModCompat.namespaceToModid(namespace);
            return modid != null && !NourishedConfig.get().isTagCompatEnabled(modid);
        });

        return matches;
    }

    private static void emitIngredientResolutionStep(
            StageContext ctx,
            ResourceLocation ingredientId,
            TraceStepStatus status,
            String source,
            @Nullable Map<String, Float> nutrients,
            boolean uncertain,
            @Nullable String reason
    ) {
        if (ctx.traceOut() == null) {
            return;
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("ingredientId", ingredientId.toString());
        detail.put("source", source);
        if (nutrients != null && !nutrients.isEmpty()) {
            detail.put("values", new LinkedHashMap<>(nutrients));
        }
        if (uncertain) {
            detail.put("uncertain", true);
        }
        if (status == TraceStepStatus.SKIPPED) {
            detail.put("skipped", true);
            if (reason != null) {
                detail.put("skipReason", reason);
            }
        }
        if (reason != null && status == TraceStepStatus.FAILURE) {
            detail.put("warningCode", "INGREDIENT_UNCLASSIFIED");
        }

        String message = switch (status) {
            case SUCCESS -> "Ingredient classified via " + source;
            case SKIPPED -> "Ingredient skipped: " + (reason != null ? reason : "unknown");
            case FAILURE -> "Ingredient unclassified: " + (reason != null ? reason : "unknown");
            default -> "Ingredient resolution";
        };

        ctx.addTraceStep(new ClassificationTraceStep(
                TraceStepId.INGREDIENT_RESOLUTION,
                status,
                message,
                detail));
    }
}
