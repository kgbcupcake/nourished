package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.StageContext;
import dev.marie.framework.scan.StageMath;
import dev.marie.framework.scanner.ArchetypePattern;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import dev.marie.framework.scanner.ScannerSpecRegistry.Multipliers;
import dev.marie.framework.scanner.ScannerSpecRegistry.ScannerSpec;
import dev.marie.framework.scanner.TokenStemmer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class KeywordSuffixStage implements ResolutionStageHandler {

    private static final Set<String> STOP_WORDS = Set.of(
            "item", "block", "ingot", "nugget",
            "with", "and", "of", "in", "on", "the", "a", "an",
            "or", "from", "de", "la", "le", "al"
    );

    private static final float PRIMARY_TOKEN_WEIGHT   = 1.0f;
    private static final float SECONDARY_TOKEN_WEIGHT = 0.6f;
    private static final float TERTIARY_TOKEN_WEIGHT  = 0.3f;

    private static final Set<String> PREPARATION_TOKENS = Set.of(
            "pie", "tart", "salad",
            "cake", "cookie", "smoothie", "juice", "jam",
            "bake", "baked", "fried", "smoked", "cooked"
    );

    private static final Set<String> AMBIGUOUS_TOKENS = Set.of(
            "deluxe", "hearty", "special", "meal", "feast", "platter",
            "dish", "plate", "bowl", "medley", "mix", "blend", "assorted",
            "classic", "traditional", "homemade", "rustic", "artisan",
            "premium", "supreme", "grand", "mega", "ultra", "epic",
            "exotic", "tropical", "seasonal", "gourmet", "fancy"
    );

    private static final float AMBIGUOUS_TOKEN_MULTIPLIER = 0.15f;

    private static float weightForPosition(int index) {
        return switch (index) {
            case 0 -> PRIMARY_TOKEN_WEIGHT;
            case 1 -> SECONDARY_TOKEN_WEIGHT;
            default -> TERTIARY_TOKEN_WEIGHT;
        };
    }

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        ScoreResult sr = scoreWithTokens(itemId, ctx);
        if (sr.scores.isEmpty()) return null;

        float compositeThreshold = compositeRatioThreshold();
        float spreadThreshold = StageMath.scannerConfidenceSpreadThreshold();
        Set<String> validKeys = ctx.validKeys();

        float top = 0f, second = 0f;
        String topKey = null;
        for (Map.Entry<String, Float> e : sr.scores.entrySet()) {
            float v = e.getValue();
            if (v > top) {
                second = top;
                top = v;
                topKey = e.getKey();
            } else if (v > second) {
                second = v;
            }
        }

        boolean isComposite = top > 0f && second > 0f && (second / top) >= compositeThreshold;

        if (isComposite) {
            float inclusionFloor = second * compositeThreshold;
            Map<String, Float> included = new LinkedHashMap<>();
            for (Map.Entry<String, Float> e : sr.scores.entrySet()) {
                if (validKeys.contains(e.getKey()) && e.getValue() >= inclusionFloor) {
                    included.put(e.getKey(), e.getValue());
                }
            }
            if (included.isEmpty()) return null;

            Map<String, Float> nutrients = normalizeScores(included);
            if (nutrients.isEmpty()) return null;

            Map<String, String> rejected = new LinkedHashMap<>();
            for (String key : validKeys) {
                if (!nutrients.containsKey(key)) {
                    Float rawScore = sr.scores.get(key);
                    if (rawScore == null || rawScore <= 0f) {
                        rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
                    } else {
                        rejected.put(key, ResolutionResult.REJECT_LOW_CONFIDENCE);
                    }
                }
            }

            float ratio = second / top;
            List<Map.Entry<String, Float>> sortedNutrients = new ArrayList<>(included.entrySet());
            sortedNutrients.sort(Comparator.<Map.Entry<String, Float>>comparingDouble(Map.Entry::getValue).reversed());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sortedNutrients.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(sortedNutrients.get(i).getKey());
            }
            String debugReason = String.format("composite ratio=%.2f nutrients=[%s]", ratio, sb);

            return new ResolutionResult(
                    nutrients, Map.copyOf(sr.scores),
                    sr.tokens, sr.tokenWeights, rejected,
                    false, top, RuntimeCascadeStage.COMPOSITE, debugReason);
        }

        float spread = StageMath.computeSpread(sr.scores);
        if (spread < spreadThreshold) return null;

        Map<String, Float> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : sr.scores.entrySet()) {
            if (validKeys.contains(e.getKey()) && e.getValue() > 0f) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        if (filtered.isEmpty()) return null;

        Map<String, Float> nutrients;
        Map<String, String> rejected = new LinkedHashMap<>();

        if (spread >= spreadThreshold && topKey != null && filtered.containsKey(topKey)) {
            nutrients = Map.of(topKey, 1.0f);
            for (String key : filtered.keySet()) {
                if (!key.equals(topKey)) {
                    rejected.put(key, ResolutionResult.REJECT_LOW_CONFIDENCE);
                }
            }
        } else {
            nutrients = normalizeScores(filtered);
        }

        for (String key : validKeys) {
            if (!filtered.containsKey(key)) {
                rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
            }
        }

        if (nutrients.isEmpty()) return null;

        String debugReason = String.format("tokens=%s dominant=%s spread=%.2f", sr.tokens, topKey != null ? topKey : "?", spread);

        return new ResolutionResult(
                nutrients, Map.copyOf(sr.scores),
                sr.tokens, sr.tokenWeights, rejected,
                false, spread, RuntimeCascadeStage.KEYWORD_SUFFIX, debugReason);
    }

    private static float compositeRatioThreshold() {
        try {
            return NourishedConfig.get().compositeRatioThreshold();
        } catch (IllegalStateException ignored) {
            return 0.50f;
        }
    }

    private static Map<String, Float> normalizeScores(Map<String, Float> scores) {
        float total = 0f;
        for (float v : scores.values()) total += v;
        if (total == 0f) return Collections.emptyMap();
        Map<String, Float> result = new HashMap<>();
        for (Map.Entry<String, Float> e : scores.entrySet()) {
            result.put(e.getKey(), e.getValue() / total);
        }
        return result;
    }

    private record ScoreResult(
            Map<String, Float> scores,
            List<String> tokens,
            Map<String, Float> tokenWeights
    ) {}

    Map<String, Float> score(ResourceLocation itemId) {
        return scoreWithTokens(itemId, new StageContext(null, itemId, null, Map.of(), Set.copyOf(NutrientRegistry.getKeys()))).scores;
    }

    private ScoreResult scoreWithTokens(ResourceLocation itemId, StageContext ctx) {
        ScannerSpec spec = ScannerSpecRegistry.get();
        Multipliers mult = spec.multipliers();
        Map<String, Map<String, Float>> suffixWeights = spec.suffixWeights();
        Map<String, Map<String, Float>> keywordWeights = spec.keywordWeights();
        Map<String, Map<String, Float>> negativeKeywords = spec.negativeKeywords();

        List<String> validKeys = NutrientRegistry.getKeys();
        Map<String, Float> scores = new HashMap<>();
        for (String key : validKeys) {
            scores.put(key, 0f);
        }

        for (Map.Entry<String, Float> e : ctx.communityTagSignal().entrySet()) {
            if (scores.containsKey(e.getKey())) {
                scores.merge(e.getKey(), e.getValue() * mult.communityTag(), Float::sum);
            }
        }

        String path = itemId.getPath();
        List<String> rawSegments = TokenStemmer.rawSegmentsForPath(path);
        List<String> tokens = new ArrayList<>(rawSegments.size());
        List<String> stemmedTokens = new ArrayList<>();
        Map<String, Float> tokenWeightMap = new LinkedHashMap<>();
        for (String raw : rawSegments) {
            String t = raw.toLowerCase(Locale.ROOT);
            if (STOP_WORDS.contains(t) || t.isEmpty()) continue;
            List<String> chunkStems = TokenStemmer.stemAll(raw);
            if (chunkStems.isEmpty()) continue;
            float positionalWeight = weightForPosition(stemmedTokens.size());
            for (String stemmed : chunkStems) {
                tokens.add(t);
                stemmedTokens.add(stemmed);
                float semanticMult = PREPARATION_TOKENS.contains(stemmed) ? 0.5f : 1.0f;
                float ambiguousMult = AMBIGUOUS_TOKENS.contains(stemmed) ? AMBIGUOUS_TOKEN_MULTIPLIER : 1.0f;
                float finalWeight = positionalWeight * semanticMult * ambiguousMult;
                tokenWeightMap.merge(stemmed, finalWeight, Float::max);
                // Record demotion flags for any token whose multipliers reduce weight below 1.0
                if (ctx.traceOut() != null && (semanticMult < 1.0f || ambiguousMult < 1.0f)) {
                    List<String> flags = new ArrayList<>();
                    if (semanticMult < 1.0f) flags.add("PREPARATION_HALVED");
                    if (ambiguousMult < 1.0f) flags.add("AMBIGUOUS_SUPPRESSED");
                    ctx.tokenDemotions().put(stemmed, flags);
                }
            }
        }

        if (!tokens.isEmpty()) {
            int lastIndex = stemmedTokens.size() - 1;
            String lastToken = stemmedTokens.get(lastIndex);
            Map<String, Float> sw = suffixWeights.get(lastToken);
            if (sw != null) {
                float positionalWeight = weightForPosition(lastIndex);
                float semanticMult = PREPARATION_TOKENS.contains(lastToken) ? 0.5f : 1.0f;
                float ambiguousMult = AMBIGUOUS_TOKENS.contains(lastToken) ? AMBIGUOUS_TOKEN_MULTIPLIER : 1.0f;
                float suffixWeight = positionalWeight * semanticMult * ambiguousMult;
                for (Map.Entry<String, Float> e : sw.entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        scores.merge(e.getKey(), e.getValue() * mult.suffix() * suffixWeight, Float::sum);
                    }
                }
            }
        }

        for (int i = 0; i < stemmedTokens.size(); i++) {
            String stemmed = stemmedTokens.get(i);
            float positionalWeight = weightForPosition(i);
            float semanticMult = PREPARATION_TOKENS.contains(stemmed) ? 0.5f : 1.0f;
            float ambiguousMult = AMBIGUOUS_TOKENS.contains(stemmed) ? AMBIGUOUS_TOKEN_MULTIPLIER : 1.0f;
            float finalWeight = positionalWeight * semanticMult * ambiguousMult;
            Map<String, Float> kw = keywordWeights.get(stemmed);
            if (kw != null) {
                for (Map.Entry<String, Float> e : kw.entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        scores.merge(e.getKey(), e.getValue() * mult.keyword() * finalWeight, Float::sum);
                    }
                }
            }
        }

        for (int i = 0; i < stemmedTokens.size(); i++) {
            String stemmed = stemmedTokens.get(i);
            float positionalWeight = weightForPosition(i);
            float semanticMult = PREPARATION_TOKENS.contains(stemmed) ? 0.5f : 1.0f;
            float ambiguousMult = AMBIGUOUS_TOKENS.contains(stemmed) ? AMBIGUOUS_TOKEN_MULTIPLIER : 1.0f;
            float finalWeight = positionalWeight * semanticMult * ambiguousMult;
            Map<String, Float> neg = negativeKeywords.get(stemmed);
            if (neg != null) {
                for (Map.Entry<String, Float> e : neg.entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        float negAmount = e.getValue() * finalWeight;
                        scores.merge(e.getKey(), negAmount, Float::sum);
                        // Accumulate negative contributions separately so the trace can show suppression
                        if (ctx.traceOut() != null) {
                            ctx.negativeContributions().merge(e.getKey(), negAmount, Float::sum);
                        }
                    }
                }
            }
        }

        // Archetype pass — substring match on full item path, bypasses tokenization
        List<ArchetypePattern> archetypes = spec.archetypes();
        for (ArchetypePattern archetype : archetypes) {
            if (archetype.matches(path)) {
                Map<String, Float> weighted = new LinkedHashMap<>();
                for (Map.Entry<String, Float> e : archetype.contributions().entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        float contribution = e.getValue() * mult.archetype();
                        scores.merge(e.getKey(), contribution, Float::sum);
                        weighted.put(e.getKey(), contribution);
                    }
                }
                if (ctx.traceOut() != null && !weighted.isEmpty()) {
                    Map<String, Object> archetypeEntry = new LinkedHashMap<>();
                    archetypeEntry.put("pattern", archetype.pattern());
                    archetypeEntry.put("contributions", weighted);
                    ctx.archetypeMatches().add(archetypeEntry);
                }
            }
        }

        scores.entrySet().removeIf(e -> e.getValue() <= 0f);
        return new ScoreResult(scores, List.copyOf(stemmedTokens), Map.copyOf(tokenWeightMap));
    }
}
