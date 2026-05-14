package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.ResolutionStage;
import dev.maire.nourished.core.nutrition.StageContext;
import dev.maire.nourished.tooling.scanner.ArchetypePattern;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry.Multipliers;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry.ScannerSpec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeywordSuffixStage implements ResolutionStageHandler {

    private static final Set<String> STOP_WORDS = Set.of(
            "item", "block", "ingot", "nugget",
            "with", "and", "of", "in", "on", "the", "a", "an",
            "or", "from", "de", "la", "le", "al"
    );

    private static final Map<String, String> STEMS;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("tomatoes", "tomato");
        m.put("berries", "berry");
        m.put("meatballs", "meatball");
        m.put("potatoes", "potato");
        m.put("cherries", "cherry");
        m.put("strawberries", "strawberry");
        m.put("blueberries", "blueberry");
        m.put("raspberries", "raspberry");
        m.put("blackberries", "blackberry");
        m.put("cranberries", "cranberry");
        m.put("carrots", "carrot");
        m.put("apples", "apple");
        m.put("oranges", "orange");
        m.put("lemons", "lemon");
        m.put("melons", "melon");
        m.put("onions", "onion");
        m.put("mushrooms", "mushroom");
        m.put("peppers", "pepper");
        m.put("cookies", "cookie");
        m.put("pies", "pie");
        m.put("cakes", "cake");
        m.put("steaks", "steak");
        m.put("chops", "chop");
        m.put("drumsticks", "drumstick");
        m.put("loaves", "loaf");
        m.put("rolls", "roll");
        m.put("noodles", "noodle");
        m.put("soups", "soup");
        m.put("stews", "stew");
        m.put("salads", "salad");
        m.put("roasts", "roast");
        m.put("fillets", "fillet");
        m.put("slices", "slice");
        m.put("grapes", "grape");
        m.put("olives", "olive");
        m.put("beans", "bean");
        m.put("beets", "beet");
        m.put("turnips", "turnip");
        m.put("radishes", "radish");
        m.put("peaches", "peach");
        m.put("mangoes", "mango");
        m.put("bananas", "banana");
        m.put("oats", "oat");
        m.put("dumplings", "dumpling");
        m.put("prawns", "prawn");
        m.put("clams", "clam");
        m.put("oysters", "oyster");
        m.put("tamales", "tamale");
        m.put("churros", "churro");
        m.put("scones", "scone");
        m.put("carnitas", "carnita");
        m.put("fajitas", "fajita");
        m.put("enchiladas", "enchilada");
        m.put("macarons", "macaron");
        m.put("hamburger", "burger");
        STEMS = Collections.unmodifiableMap(m);
    }

    private static final float PRIMARY_TOKEN_WEIGHT   = 1.0f;
    private static final float SECONDARY_TOKEN_WEIGHT = 0.6f;
    private static final float TERTIARY_TOKEN_WEIGHT  = 0.3f;

    private static final Set<String> PREPARATION_TOKENS = Set.of(
            "pie", "tart", "salad", "sandwich",
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
                    false, top, ResolutionStage.COMPOSITE, debugReason);
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
                false, spread, ResolutionStage.KEYWORD_SUFFIX, debugReason);
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
        String[] rawTokens = path.split("_");
        List<String> tokens = new ArrayList<>(rawTokens.length);
        List<String> stemmedTokens = new ArrayList<>(rawTokens.length);
        Map<String, Float> tokenWeightMap = new LinkedHashMap<>();
        for (int i = 0; i < rawTokens.length; i++) {
            String t = rawTokens[i].toLowerCase();
            if (STOP_WORDS.contains(t)) continue;
            if (!t.isEmpty()) {
                String stemmed = stem(t);
                tokens.add(t);
                stemmedTokens.add(stemmed);
                float positionalWeight = weightForPosition(tokens.size() - 1);
                float semanticMult = PREPARATION_TOKENS.contains(stemmed) ? 0.5f : 1.0f;
                float ambiguousMult = AMBIGUOUS_TOKENS.contains(stemmed) ? AMBIGUOUS_TOKEN_MULTIPLIER : 1.0f;
                float finalWeight = positionalWeight * semanticMult * ambiguousMult;
                tokenWeightMap.put(stemmed, finalWeight);
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
                        scores.merge(e.getKey(), e.getValue() * finalWeight, Float::sum);
                    }
                }
            }
        }

        // Archetype pass — substring match on full item path, bypasses tokenization
        List<ArchetypePattern> archetypes = spec.archetypes();
        for (ArchetypePattern archetype : archetypes) {
            if (archetype.matches(path)) {
                for (Map.Entry<String, Float> e : archetype.contributions().entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        scores.merge(e.getKey(), e.getValue() * mult.archetype(), Float::sum);
                    }
                }
            }
        }

        scores.entrySet().removeIf(e -> e.getValue() <= 0f);
        return new ScoreResult(scores, List.copyOf(stemmedTokens), Map.copyOf(tokenWeightMap));
    }

    private static String stem(String token) {
        String irregular = STEMS.get(token);
        return irregular != null ? irregular : token;
    }

    static Map<String, String> stemsSnapshot() {
        return STEMS;
    }
}
