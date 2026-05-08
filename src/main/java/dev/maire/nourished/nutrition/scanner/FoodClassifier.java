package dev.maire.nourished.nutrition.scanner;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Signal-based food classifier that analyzes items using 9 weighted signals.
 * Each signal method returns nutrient contributions that are summed together.
 */
public final class FoodClassifier {

    private final RecipeInheritanceResolver recipeResolver;
    private final boolean enableRecipeInheritance;
    private final float confidenceSpreadThreshold;

    private final float[] preAllocatedScores;
    private final List<String> nutrientKeys;

    public FoodClassifier(
            List<String> nutrientKeys,
            boolean enableRecipeInheritance,
            float confidenceSpreadThreshold,
            @Nullable RecipeInheritanceResolver recipeResolver
    ) {
        this.nutrientKeys = List.copyOf(nutrientKeys);
        this.enableRecipeInheritance = enableRecipeInheritance;
        this.confidenceSpreadThreshold = confidenceSpreadThreshold;
        this.recipeResolver = recipeResolver;
        this.preAllocatedScores = new float[nutrientKeys.size()];
    }

    /**
     * Classify a single item, returning the full classification result.
     *
     * @param item The item to classify
     * @param classifiedLookup Lookup function for already-classified items (for recipe inheritance)
     * @param namespaceAverages Pre-computed namespace averages for peer signal
     * @return The classification result
     */
    public ClassificationResult classify(
            Item item,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            Map<String, Map<String, Float>> namespaceAverages
    ) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return ClassificationResult.empty(ResourceLocation.withDefaultNamespace("unknown"), nutrientKeys.get(0));
        }

        ItemStack stack = new ItemStack(item);
        Holder<Item> holder = stack.getItemHolder();
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        FoodProperties food = item.components().get(DataComponents.FOOD);

        Map<String, Float> scores = new HashMap<>();
        for (String key : nutrientKeys) {
            scores.put(key, 0f);
        }

        List<ClassificationSignal> signals = new ArrayList<>();

        // Signal 1: Community tags (5x)
        Map<String, Float> communityTagContribs = analyzeSignal1CommunityTags(holder);
        applySignal(scores, communityTagContribs, SignalWeights.COMMUNITY_TAG_MULTIPLIER);
        if (!communityTagContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_COMMUNITY_TAG,
                    "c:foods/*",
                    scaleContributions(communityTagContribs, SignalWeights.COMMUNITY_TAG_MULTIPLIER)
            ));
        }

        // Signal 2: Namespace heuristics (4x)
        Map<String, Float> namespaceContribs = analyzeSignal2Namespace(namespace);
        applySignal(scores, namespaceContribs, SignalWeights.NAMESPACE_MULTIPLIER);
        if (!namespaceContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_NAMESPACE,
                    namespace,
                    scaleContributions(namespaceContribs, SignalWeights.NAMESPACE_MULTIPLIER)
            ));
        }

        // Signal 3: Suffix patterns (3x)
        Map<String, Float> suffixContribs = analyzeSignal3Suffix(path);
        applySignal(scores, suffixContribs, SignalWeights.SUFFIX_MULTIPLIER);
        if (!suffixContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_SUFFIX,
                    extractTrailingToken(path),
                    scaleContributions(suffixContribs, SignalWeights.SUFFIX_MULTIPLIER)
            ));
        }

        // Signal 4: Positive keywords (2x)
        Map<String, Float> keywordContribs = analyzeSignal4Keywords(path);
        applySignal(scores, keywordContribs, SignalWeights.KEYWORD_MULTIPLIER);
        if (!keywordContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_KEYWORD,
                    path,
                    scaleContributions(keywordContribs, SignalWeights.KEYWORD_MULTIPLIER)
            ));
        }

        // Signal 5: Negative keywords (suppression, no multiplier)
        Map<String, Float> negativeContribs = analyzeSignal5NegativeKeywords(path);
        applySignal(scores, negativeContribs, 1.0f);
        if (!negativeContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_NEGATIVE_KEYWORD,
                    path,
                    negativeContribs
            ));
        }

        // Signal 6: Archetypes (2x)
        Map<String, Float> archetypeContribs = analyzeSignal6Archetypes(path);
        applySignal(scores, archetypeContribs, SignalWeights.ARCHETYPE_MULTIPLIER);
        if (!archetypeContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_ARCHETYPE,
                    path,
                    scaleContributions(archetypeContribs, SignalWeights.ARCHETYPE_MULTIPLIER)
            ));
        }

        // Signal 7: FoodProperties (1x)
        if (food != null) {
            Map<String, Float> foodPropContribs = analyzeSignal7FoodProperties(food);
            applySignal(scores, foodPropContribs, SignalWeights.FOOD_PROPERTIES_MULTIPLIER);
            if (!foodPropContribs.isEmpty()) {
                signals.add(new ClassificationSignal(
                        ClassificationSignal.TYPE_FOOD_PROPERTIES,
                        String.format("nutrition=%d,saturation=%.1f", food.nutrition(), food.saturation()),
                        foodPropContribs
                ));
            }
        }

        // Signal 8: Recipe inheritance (1x) - only if enabled and resolver available
        if (enableRecipeInheritance && recipeResolver != null) {
            Map<String, Float> recipeContribs = recipeResolver.resolve(item, classifiedLookup);
            applySignal(scores, recipeContribs, SignalWeights.RECIPE_INHERITANCE_MULTIPLIER);
            if (!recipeContribs.isEmpty()) {
                signals.add(new ClassificationSignal(
                        ClassificationSignal.TYPE_RECIPE_INHERITANCE,
                        "recipe_ingredients",
                        scaleContributions(recipeContribs, SignalWeights.RECIPE_INHERITANCE_MULTIPLIER)
                ));
            }
        }

        // Signal 9: Namespace peers (0.5x)
        Map<String, Float> peerAvg = namespaceAverages.get(namespace);
        if (peerAvg != null && !peerAvg.isEmpty()) {
            Map<String, Float> peerContribs = analyzeSignal9NamespacePeers(peerAvg);
            applySignal(scores, peerContribs, SignalWeights.NAMESPACE_PEER_MULTIPLIER);
            if (!peerContribs.isEmpty()) {
                signals.add(new ClassificationSignal(
                        ClassificationSignal.TYPE_NAMESPACE_PEER,
                        namespace + "_peers",
                        scaleContributions(peerContribs, SignalWeights.NAMESPACE_PEER_MULTIPLIER)
                ));
            }
        }

        return buildResult(itemId, scores, signals);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Signal Analysis Methods
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Signal 1: Check for existing c:foods/* community tags.
     */
    private Map<String, Float> analyzeSignal1CommunityTags(Holder<Item> holder) {
        Map<String, Float> contributions = new HashMap<>();

        for (Map.Entry<String, Map<String, Float>> entry : SignalWeights.COMMUNITY_TAG_WEIGHTS.entrySet()) {
            String tagSuffix = entry.getKey();
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/" + tagSuffix));
            if (holder.is(tagKey)) {
                for (Map.Entry<String, Float> contrib : entry.getValue().entrySet()) {
                    contributions.merge(contrib.getKey(), contrib.getValue(), Float::sum);
                }
            }
        }

        return contributions;
    }

    /**
     * Signal 2: Namespace heuristics from known mod patterns.
     */
    private Map<String, Float> analyzeSignal2Namespace(String namespace) {
        Map<String, Float> weights = SignalWeights.NAMESPACE_WEIGHTS.get(namespace);
        return weights != null ? new HashMap<>(weights) : Map.of();
    }

    /**
     * Signal 3: Suffix pattern recognition on trailing tokens.
     */
    private Map<String, Float> analyzeSignal3Suffix(String path) {
        Map<String, Float> contributions = new HashMap<>();
        String[] tokens = path.split("_");

        if (tokens.length > 0) {
            String lastToken = tokens[tokens.length - 1].toLowerCase();
            Map<String, Float> weights = SignalWeights.SUFFIX_WEIGHTS.get(lastToken);
            if (weights != null) {
                contributions.putAll(weights);
            }

            if (tokens.length > 1) {
                String secondLast = tokens[tokens.length - 2].toLowerCase();
                Map<String, Float> secondWeights = SignalWeights.SUFFIX_WEIGHTS.get(secondLast);
                if (secondWeights != null) {
                    for (Map.Entry<String, Float> e : secondWeights.entrySet()) {
                        contributions.merge(e.getKey(), e.getValue() * 0.5f, Float::sum);
                    }
                }
            }
        }

        return contributions;
    }

    /**
     * Signal 4: Positive keyword scoring across all tokens.
     */
    private Map<String, Float> analyzeSignal4Keywords(String path) {
        Map<String, Float> contributions = new HashMap<>();
        String[] tokens = path.toLowerCase().split("_");

        for (String token : tokens) {
            String cleaned = token.replaceAll("item$", "");
            Map<String, Float> weights = SignalWeights.KEYWORD_WEIGHTS.get(cleaned);
            if (weights != null) {
                for (Map.Entry<String, Float> e : weights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }

        return contributions;
    }

    /**
     * Signal 5: Negative keyword suppression.
     */
    private Map<String, Float> analyzeSignal5NegativeKeywords(String path) {
        Map<String, Float> contributions = new HashMap<>();
        String[] tokens = path.toLowerCase().split("_");

        for (String token : tokens) {
            String cleaned = token.replaceAll("item$", "");
            Map<String, Float> weights = SignalWeights.NEGATIVE_KEYWORDS.get(cleaned);
            if (weights != null) {
                for (Map.Entry<String, Float> e : weights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }

        return contributions;
    }

    /**
     * Signal 6: Compound food archetype detection.
     */
    private Map<String, Float> analyzeSignal6Archetypes(String path) {
        Map<String, Float> contributions = new HashMap<>();
        String lowerPath = path.toLowerCase();

        for (SignalWeights.ArchetypePattern archetype : SignalWeights.ARCHETYPE_PATTERNS) {
            if (archetype.matches(lowerPath)) {
                for (Map.Entry<String, Float> e : archetype.contributions().entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }

        return contributions;
    }

    /**
     * Signal 7: FoodProperties runtime data analysis.
     */
    private Map<String, Float> analyzeSignal7FoodProperties(FoodProperties food) {
        Map<String, Float> contributions = new HashMap<>();

        int nutrition = food.nutrition();
        float saturation = food.saturation();

        if (nutrition == 0) {
            return contributions;
        }

        if (saturation > 1.2f && nutrition > 6) {
            contributions.put("proteins", 2f);
            contributions.put("grains", 1f);
        }

        if (nutrition <= 2) {
            contributions.put("vegetables", 1f);
            contributions.put("sugars", 1f);
        }

        boolean hasBadEffects = food.effects().stream()
                .anyMatch(e -> {
                    ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(e.effect().getEffect().value());
                    if (effectId == null) return false;
                    String id = effectId.toString();
                    return id.contains("poison") || id.contains("nausea") || id.contains("hunger");
                });

        if (hasBadEffects) {
            for (String key : contributions.keySet()) {
                contributions.put(key, contributions.get(key) * 0.5f);
            }
        }

        return contributions;
    }

    /**
     * Signal 9: Namespace peer cross-reference using pre-computed averages.
     */
    private Map<String, Float> analyzeSignal9NamespacePeers(Map<String, Float> peerAverages) {
        Map<String, Float> contributions = new HashMap<>();
        for (Map.Entry<String, Float> e : peerAverages.entrySet()) {
            contributions.put(e.getKey(), e.getValue() * 0.5f);
        }
        return contributions;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────────

    private void applySignal(Map<String, Float> scores, Map<String, Float> contributions, float multiplier) {
        for (Map.Entry<String, Float> e : contributions.entrySet()) {
            String key = e.getKey();
            if (scores.containsKey(key)) {
                scores.put(key, scores.get(key) + e.getValue() * multiplier);
            }
        }
    }

    private Map<String, Float> scaleContributions(Map<String, Float> contributions, float multiplier) {
        Map<String, Float> scaled = new HashMap<>();
        for (Map.Entry<String, Float> e : contributions.entrySet()) {
            scaled.put(e.getKey(), e.getValue() * multiplier);
        }
        return scaled;
    }

    private String extractTrailingToken(String path) {
        String[] tokens = path.split("_");
        return tokens.length > 0 ? tokens[tokens.length - 1] : path;
    }

    private ClassificationResult buildResult(
            ResourceLocation itemId,
            Map<String, Float> scores,
            List<ClassificationSignal> signals
    ) {
        List<Map.Entry<String, Float>> sorted = scores.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .toList();

        if (sorted.isEmpty()) {
            return ClassificationResult.empty(itemId, nutrientKeys.get(0));
        }

        String dominant = sorted.get(0).getKey();
        String secondary = sorted.size() > 1 ? sorted.get(1).getKey() : null;
        float spread = sorted.get(0).getValue() - (secondary != null ? sorted.get(1).getValue() : 0f);
        boolean uncertain = spread < confidenceSpreadThreshold;

        return new ClassificationResult(itemId, scores, dominant, secondary, spread, signals, uncertain);
    }

    /**
     * Compute average scores for all items in a given namespace.
     * Used for Signal 9 (namespace peer cross-reference).
     */
    public static Map<String, Map<String, Float>> computeNamespaceAverages(
            Map<ResourceLocation, ClassificationResult> results
    ) {
        Map<String, List<ClassificationResult>> byNamespace = new HashMap<>();

        for (Map.Entry<ResourceLocation, ClassificationResult> e : results.entrySet()) {
            String ns = e.getKey().getNamespace();
            byNamespace.computeIfAbsent(ns, k -> new ArrayList<>()).add(e.getValue());
        }

        Map<String, Map<String, Float>> averages = new HashMap<>();

        for (Map.Entry<String, List<ClassificationResult>> e : byNamespace.entrySet()) {
            List<ClassificationResult> list = e.getValue();
            if (list.isEmpty()) continue;

            Map<String, Float> summed = new HashMap<>();
            for (ClassificationResult r : list) {
                for (Map.Entry<String, Float> score : r.scores().entrySet()) {
                    summed.merge(score.getKey(), score.getValue(), Float::sum);
                }
            }

            Map<String, Float> avg = new HashMap<>();
            for (Map.Entry<String, Float> s : summed.entrySet()) {
                avg.put(s.getKey(), s.getValue() / list.size());
            }

            averages.put(e.getKey(), avg);
        }

        return averages;
    }
}
