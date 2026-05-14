package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.ResolutionStage;
import dev.maire.nourished.core.nutrition.StageContext;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry.Multipliers;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry.ScannerSpec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 2: scores the item path tokens against scanner-spec keyword, suffix,
 * and negative-keyword weight tables. Owns the irregular-plural stem map.
 */
public final class KeywordSuffixStage implements ResolutionStageHandler {

    private static final Set<String> STOP_WORDS = Set.of(
            "item", "block", "ingot", "nugget"
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
        STEMS = Collections.unmodifiableMap(m);
    }

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        Map<String, Float> scores = score(itemId);
        if (scores.isEmpty()) return null;

        float spread = StageMath.computeSpread(scores);
        float threshold = StageMath.scannerConfidenceSpreadThreshold();
        if (spread < threshold) return null;

        Map<String, Float> normalized = StageMath.normalizeToBarMap(scores, ctx.validKeys());
        if (normalized.isEmpty()) return null;

        return new ResolutionResult(normalized, spread, ResolutionStage.KEYWORD_SUFFIX,
                "keyword/suffix spread=" + String.format("%.2f", spread));
    }

    /**
     * Raw keyword/suffix scoring without normalization or threshold gating.
     * Package-private for test access.
     */
    Map<String, Float> score(ResourceLocation itemId) {
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

        String path = itemId.getPath();
        String[] rawTokens = path.split("_");
        List<String> tokens = new ArrayList<>(rawTokens.length);
        for (int i = 0; i < rawTokens.length; i++) {
            String t = rawTokens[i].toLowerCase();
            if (STOP_WORDS.contains(t)) continue;
            if (!t.isEmpty()) tokens.add(t);
        }

        if (!tokens.isEmpty()) {
            String lastToken = stem(tokens.get(tokens.size() - 1));
            Map<String, Float> sw = suffixWeights.get(lastToken);
            if (sw != null) {
                for (Map.Entry<String, Float> e : sw.entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        scores.merge(e.getKey(), e.getValue() * mult.suffix(), Float::sum);
                    }
                }
            }
        }

        for (String token : tokens) {
            String stemmed = stem(token);
            Map<String, Float> kw = keywordWeights.get(stemmed);
            if (kw != null) {
                for (Map.Entry<String, Float> e : kw.entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        scores.merge(e.getKey(), e.getValue() * mult.keyword(), Float::sum);
                    }
                }
            }
        }

        for (String token : tokens) {
            String stemmed = stem(token);
            Map<String, Float> neg = negativeKeywords.get(stemmed);
            if (neg != null) {
                for (Map.Entry<String, Float> e : neg.entrySet()) {
                    if (scores.containsKey(e.getKey())) {
                        scores.merge(e.getKey(), e.getValue(), Float::sum);
                    }
                }
            }
        }

        scores.entrySet().removeIf(e -> e.getValue() <= 0f);
        return scores;
    }

    private static String stem(String token) {
        String irregular = STEMS.get(token);
        return irregular != null ? irregular : token;
    }

    /** Unmodifiable snapshot of the stem map for test assertions. */
    static Map<String, String> stemsSnapshot() {
        return STEMS;
    }
}
