package dev.maire.nourished.tooling.scanner;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
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
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Signal-based food classifier that analyzes items using 9 weighted signals.
 *
 * <p>All weights, multipliers, archetype patterns, and runtime food-property
 * heuristics are loaded from JSON via {@link ScannerSpecRegistry}. The classifier
 * itself contains no hardcoded scoring data — only the wiring between signals.</p>
 */
@ApiStatus.Internal
public final class FoodClassifier {

    private final RecipeInheritanceResolver recipeResolver;
    private final boolean enableRecipeInheritance;
    private final float confidenceSpreadThreshold;

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
    }

    /**
     * Classify a single item, returning the full classification result.
     */
    public ClassificationResult classify(
            Item item,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            Map<String, Map<String, Float>> namespaceAverages
    ) {
        ResourceLocation itemId = NourishedRegistryUtils.itemKey(item);
        if (itemId == null) {
            return ClassificationResult.empty(ResourceLocation.withDefaultNamespace("unknown"), nutrientKeys.get(0));
        }

        ScannerSpecRegistry.ScannerSpec spec = ScannerSpecRegistry.get();
        ScannerSpecRegistry.Multipliers mult = spec.multipliers();

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

        Map<String, Float> communityTagContribs = analyzeSignal1CommunityTags(holder, spec.communityTagWeights());
        applySignal(scores, communityTagContribs, mult.communityTag());
        if (!communityTagContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_COMMUNITY_TAG,
                    "c:foods/*",
                    scaleContributions(communityTagContribs, mult.communityTag())
            ));
        }

        Map<String, Float> namespaceContribs = analyzeSignal2Namespace(namespace, spec.namespaceWeights());
        applySignal(scores, namespaceContribs, mult.namespace());
        if (!namespaceContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_NAMESPACE,
                    namespace,
                    scaleContributions(namespaceContribs, mult.namespace())
            ));
        }

        Map<String, Float> suffixContribs = analyzeSignal3Suffix(path, spec.suffixWeights(), mult.secondarySuffix());
        applySignal(scores, suffixContribs, mult.suffix());
        if (!suffixContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_SUFFIX,
                    extractTrailingToken(path),
                    scaleContributions(suffixContribs, mult.suffix())
            ));
        }

        Map<String, Float> keywordContribs = analyzeSignal4Keywords(path, spec.keywordWeights());
        applySignal(scores, keywordContribs, mult.keyword());
        if (!keywordContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_KEYWORD,
                    path,
                    scaleContributions(keywordContribs, mult.keyword())
            ));
        }

        Map<String, Float> negativeContribs = analyzeSignal5NegativeKeywords(path, spec.negativeKeywords());
        applySignal(scores, negativeContribs, 1.0f);
        if (!negativeContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_NEGATIVE_KEYWORD,
                    path,
                    negativeContribs
            ));
        }

        Map<String, Float> archetypeContribs = analyzeSignal6Archetypes(path, spec.archetypes());
        applySignal(scores, archetypeContribs, mult.archetype());
        if (!archetypeContribs.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_ARCHETYPE,
                    path,
                    scaleContributions(archetypeContribs, mult.archetype())
            ));
        }

        if (food != null) {
            Map<String, Float> foodPropContribs = analyzeSignal7FoodProperties(food, spec.foodPropertyHeuristics());
            applySignal(scores, foodPropContribs, mult.foodProperties());
            if (!foodPropContribs.isEmpty()) {
                signals.add(new ClassificationSignal(
                        ClassificationSignal.TYPE_FOOD_PROPERTIES,
                        String.format("nutrition=%d,saturation=%.1f", food.nutrition(), food.saturation()),
                        foodPropContribs
                ));
            }
        }

        if (enableRecipeInheritance && recipeResolver != null) {
            Map<String, Float> recipeContribs = recipeResolver.resolve(item, classifiedLookup);
            applySignal(scores, recipeContribs, mult.recipeInheritance());
            if (!recipeContribs.isEmpty()) {
                signals.add(new ClassificationSignal(
                        ClassificationSignal.TYPE_RECIPE_INHERITANCE,
                        "recipe_ingredients",
                        scaleContributions(recipeContribs, mult.recipeInheritance())
                ));
            }
        }

        Map<String, Float> peerAvg = namespaceAverages.get(namespace);
        if (peerAvg != null && !peerAvg.isEmpty()) {
            Map<String, Float> peerContribs = analyzeSignal9NamespacePeers(peerAvg, mult.namespacePeerAverageWeight());
            applySignal(scores, peerContribs, mult.namespacePeer());
            if (!peerContribs.isEmpty()) {
                signals.add(new ClassificationSignal(
                        ClassificationSignal.TYPE_NAMESPACE_PEER,
                        namespace + "_peers",
                        scaleContributions(peerContribs, mult.namespacePeer())
                ));
            }
        }

        return buildResult(itemId, scores, signals);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Signal Analysis Methods
    // ─────────────────────────────────────────────────────────────────────────────

    private Map<String, Float> analyzeSignal1CommunityTags(
            Holder<Item> holder,
            Map<String, Map<String, Float>> communityTagWeights
    ) {
        Map<String, Float> contributions = new HashMap<>();
        for (Map.Entry<String, Map<String, Float>> entry : communityTagWeights.entrySet()) {
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

    private Map<String, Float> analyzeSignal2Namespace(
            String namespace,
            Map<String, Map<String, Float>> namespaceWeights
    ) {
        Map<String, Float> weights = namespaceWeights.get(namespace);
        return weights != null ? new HashMap<>(weights) : Map.of();
    }

    private Map<String, Float> analyzeSignal3Suffix(
            String path,
            Map<String, Map<String, Float>> suffixWeights,
            float secondarySuffixWeight
    ) {
        Map<String, Float> contributions = new HashMap<>();
        List<String> raw = FoodTokenStemmer.rawSegmentsForPath(path);
        String[] unders = path.split("_");
        if (raw.isEmpty() && unders.length == 0) {
            return contributions;
        }

        String lastToken = !raw.isEmpty()
            ? raw.get(raw.size() - 1).toLowerCase(Locale.ROOT)
            : unders[unders.length - 1].toLowerCase(Locale.ROOT);
        Map<String, Float> weights = suffixWeights.get(lastToken);
        if (weights != null) {
            contributions.putAll(weights);
        }

        String secondLast = null;
        if (raw.size() > 1) {
            secondLast = raw.get(raw.size() - 2).toLowerCase(Locale.ROOT);
        } else if (unders.length > 1) {
            secondLast = unders[unders.length - 2].toLowerCase(Locale.ROOT);
        }
        if (secondLast != null) {
            Map<String, Float> secondWeights = suffixWeights.get(secondLast);
            if (secondWeights != null) {
                for (Map.Entry<String, Float> e : secondWeights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue() * secondarySuffixWeight, Float::sum);
                }
            }
        }
        return contributions;
    }

    private Map<String, Float> analyzeSignal4Keywords(
            String path,
            Map<String, Map<String, Float>> keywordWeights
    ) {
        Map<String, Float> contributions = new HashMap<>();
        for (String root : FoodTokenStemmer.tokenizeForScoring(path)) {
            Map<String, Float> weights = keywordWeights.get(root);
            if (weights != null) {
                for (Map.Entry<String, Float> e : weights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }
        return contributions;
    }

    private Map<String, Float> analyzeSignal5NegativeKeywords(
            String path,
            Map<String, Map<String, Float>> negativeKeywords
    ) {
        Map<String, Float> contributions = new HashMap<>();
        for (String root : FoodTokenStemmer.tokenizeForScoring(path)) {
            Map<String, Float> weights = negativeKeywords.get(root);
            if (weights != null) {
                for (Map.Entry<String, Float> e : weights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }
        return contributions;
    }

    private Map<String, Float> analyzeSignal6Archetypes(
            String path,
            List<ArchetypePattern> archetypes
    ) {
        Map<String, Float> contributions = new HashMap<>();
        String lowerPath = path.toLowerCase();

        for (ArchetypePattern archetype : archetypes) {
            if (archetype.matches(lowerPath)) {
                for (Map.Entry<String, Float> e : archetype.contributions().entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }
        return contributions;
    }

    private Map<String, Float> analyzeSignal7FoodProperties(
            FoodProperties food,
            ScannerSpecRegistry.FoodPropertyHeuristics heur
    ) {
        Map<String, Float> contributions = new HashMap<>();

        int nutrition = food.nutrition();
        float saturation = food.saturation();

        if (nutrition == 0) {
            return contributions;
        }

        if (heur.saturatingMeal().matches(nutrition, saturation)) {
            for (Map.Entry<String, Float> e : heur.saturatingMeal().contributions().entrySet()) {
                contributions.merge(e.getKey(), e.getValue(), Float::sum);
            }
        }

        if (heur.lightSnack().matches(nutrition)) {
            for (Map.Entry<String, Float> e : heur.lightSnack().contributions().entrySet()) {
                contributions.merge(e.getKey(), e.getValue(), Float::sum);
            }
        }

        List<String> badEffectKeywords = heur.badEffectKeywords();
        boolean hasBadEffects = !badEffectKeywords.isEmpty() && food.effects().stream()
                .anyMatch(e -> {
                    ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(e.effect().getEffect().value());
                    if (effectId == null) return false;
                    String id = effectId.toString().toLowerCase();
                    for (String kw : badEffectKeywords) {
                        if (id.contains(kw)) return true;
                    }
                    return false;
                });

        if (hasBadEffects) {
            float dampen = heur.badEffectMultiplier();
            for (String key : contributions.keySet()) {
                contributions.put(key, contributions.get(key) * dampen);
            }
        }

        return contributions;
    }

    private Map<String, Float> analyzeSignal9NamespacePeers(
            Map<String, Float> peerAverages,
            float averageWeight
    ) {
        Map<String, Float> contributions = new HashMap<>();
        for (Map.Entry<String, Float> e : peerAverages.entrySet()) {
            contributions.put(e.getKey(), e.getValue() * averageWeight);
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
        List<String> raw = FoodTokenStemmer.rawSegmentsForPath(path);
        if (!raw.isEmpty()) {
            return raw.get(raw.size() - 1);
        }
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
        float topScore = sorted.get(0).getValue();
        float secondScore = sorted.size() > 1 ? sorted.get(1).getValue() : 0f;
        float spread = topScore - secondScore;
        boolean uncertain = spread < confidenceSpreadThreshold;

        return new ClassificationResult(itemId, scores, dominant, secondary, spread, signals, uncertain, false);
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
