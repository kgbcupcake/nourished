package dev.maire.nourished.tooling.scanner;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain-specific food vocabulary normalizer.
 *
 * <p>Collapses morphological variants of food terms into canonical roots so the
 * classifier never needs duplicate entries in scanner_spec.json. Rules are
 * narrow and food-context justified — this is NOT a generic NLP stemmer.</p>
 */
@ApiStatus.Internal
public final class FoodTokenStemmer {

    private static final boolean DEBUG = Boolean.getBoolean("nourished.stemmer.debug");

    // Phase 1 — Irregular Forms
    // Checked before any suffix stripping. Covers plurals/forms that suffix rules would mangle.
    private static final Map<String, String> IRREGULAR_FORMS = Map.ofEntries(
        Map.entry("fries",   "fry"),
        Map.entry("chips",   "chip"),
        Map.entry("geese",   "goose"),
        Map.entry("teeth",   "tooth"),
        Map.entry("mice",    "mouse"),
        Map.entry("oxen",    "ox"),
        Map.entry("cacti",   "cactus"),
        Map.entry("fungi",   "fungus"),
        Map.entry("alumni",  "alum"),
        Map.entry("larvae",  "larva"),
        Map.entry("beef",    "beef"),
        Map.entry("venison", "venison"),
        Map.entry("mutton",  "mutton"),
        Map.entry("poultry", "poultry"),
        Map.entry("pork",    "pork"),
        Map.entry("rice",    "rice"),
        Map.entry("wheat",   "wheat"),
        Map.entry("oats",    "oat"),
        Map.entry("peas",    "pea"),
        Map.entry("seas",    "sea")
    );

    // Phase 2 — Compound Splits
    // Food compound words that carry two classification signals. Splitting both
    // allows cheesecake to score dairy AND sugars simultaneously.
    private static final Map<String, String[]> COMPOUND_SPLITS = Map.of(
        "breadcrumb",  new String[]{"bread", "crumb"},
        "cheesecake",  new String[]{"cheese", "cake"},
        "beefsteak",   new String[]{"beef", "steak"},
        "meatloaf",    new String[]{"meat", "loaf"},
        "cornbread",   new String[]{"corn", "bread"},
        "sourdough",   new String[]{"sour", "dough"},
        "buttermilk",  new String[]{"butter", "milk"},
        "shortcake",   new String[]{"short", "cake"},
        "gingerbread", new String[]{"ginger", "bread"},
        "hotdog",      new String[]{"hot", "dog"}
    );

    // Phase 4 — Stop Words
    // Tokens that carry zero nutritional signal and should never be spec-matched.
    private static final Set<String> STOP_WORDS = Set.of(
        "with", "and", "of", "the", "in", "on", "a", "an",
        "raw", "fresh", "dried", "wild", "organic", "natural",
        "small", "large", "big", "mini", "mega", "super",
        "hot", "cold", "warm", "cool", "frozen",
        "old", "new", "young", "aged",
        "light", "dark", "deep", "extra",
        "plain", "simple", "basic", "classic"
    );

    private FoodTokenStemmer() {}

    /**
     * Stem a single token to its canonical root.
     * Returns the first root when a compound split fires.
     */
    public static String stem(String token) {
        List<String> roots = stemAll(token);
        return roots.isEmpty() ? token : roots.get(0);
    }

    /**
     * Stem a token, returning all canonical roots.
     * Returns multiple roots when a compound split fires.
     * Returns an empty list when the token is a stop word.
     */
    public static List<String> stemAll(String token) {
        if (token == null || token.isEmpty()) return List.of();

        // Lowercase + strip "item" suffix
        String t = token.toLowerCase();
        if (t.endsWith("item") && t.length() > 4) {
            t = t.substring(0, t.length() - 4);
        }

        // Phase 4 — Stop words checked early to short-circuit
        if (STOP_WORDS.contains(t)) {
            debugLog(token, t, "stop-word-discarded");
            return List.of();
        }

        // Phase 1 — Irregular forms (must precede suffix stripping)
        String irregular = IRREGULAR_FORMS.get(t);
        if (irregular != null) {
            debugLog(token, irregular, "irregular-form");
            return List.of(irregular);
        }

        // Phase 2 — Compound splits
        String[] parts = COMPOUND_SPLITS.get(t);
        if (parts != null) {
            debugLog(token, String.join(", ", parts), "compound-split");
            return List.of(parts);
        }

        // Phase 3 — Suffix stripping
        String stripped = stripSuffixes(t, token);

        // Phase 4 — Stop word re-check after stripping
        if (STOP_WORDS.contains(stripped)) {
            debugLog(token, stripped, "stop-word-discarded");
            return List.of();
        }

        return List.of(stripped);
    }

    /**
     * Stem all keys in a map, merging collisions by summing float values.
     * Called at spec load time so stems never need to happen at scan time.
     */
    public static Map<String, Map<String, Float>> stemMapKeys(Map<String, Map<String, Float>> input) {
        Map<String, Map<String, Float>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Float>> entry : input.entrySet()) {
            List<String> roots = stemAll(entry.getKey());
            if (roots.isEmpty()) continue; // stop word key — discard
            for (String root : roots) {
                result.merge(root, entry.getValue(), FoodTokenStemmer::mergeFloatMaps);
            }
        }
        return result;
    }

    /**
     * Returns true when the token carries no nutritional signal.
     */
    public static boolean isStopWord(String token) {
        if (token == null) return false;
        String t = token.toLowerCase();
        if (t.endsWith("item") && t.length() > 4) t = t.substring(0, t.length() - 4);
        return STOP_WORDS.contains(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Suffix Stripping (Phase 3)
    // ─────────────────────────────────────────────────────────────────────────

    private static String stripSuffixes(String t, String original) {
        String result = t;

        // 1. ies → y (berries → berry, pastries → pastry)
        if (result.endsWith("ies") && result.length() > 4) {
            String candidate = result.substring(0, result.length() - 3) + "y";
            debugLog(original, candidate, "ies→y");
            return candidate;
        }

        // 2. ves → f (loaves → loaf, halves → half)
        if (result.endsWith("ves")) {
            String candidate = result.substring(0, result.length() - 3) + "f";
            if (candidate.length() >= 3) {
                debugLog(original, candidate, "ves→f");
                return candidate;
            }
        }

        // 3. es → "" (tomatoes → tomato, peaches → peach)
        // Guard: skip sses, xes, zes, -us endings, -ss roots
        if (result.endsWith("es") && result.length() > 3) {
            String withoutEs = result.substring(0, result.length() - 2);
            boolean skipEs = result.endsWith("sses")
                || result.endsWith("xes")
                || result.endsWith("zes")
                || result.endsWith("uses")
                || withoutEs.endsWith("ss");
            if (!skipEs && withoutEs.length() >= 3) {
                debugLog(original, withoutEs, "es-strip");
                return withoutEs;
            }
        }

        // 4. s → "" (carrots → carrot, mushrooms → mushroom)
        // Guard: skip ss, us, is, as, os endings; minimum root length 3
        if (result.endsWith("s") && result.length() > 3) {
            String root = result.substring(0, result.length() - 1);
            boolean skipS = result.endsWith("ss")
                || result.endsWith("us")
                || result.endsWith("is")
                || result.endsWith("as")
                || result.endsWith("os");
            if (!skipS && root.length() >= 3) {
                debugLog(original, root, "s-strip");
                return root;
            }
        }

        // 5. ing → "" (cooking → cook, grilling → grill)
        if (result.endsWith("ing")) {
            String root = result.substring(0, result.length() - 3);
            if (root.length() >= 4) {
                // Re-apply s/es strip for -ing forms that end in a vowel+consonant doubling
                // (e.g. slicing → slic — leave as is, not worth guessing the silent e)
                debugLog(original, root, "ing-strip");
                return root;
            }
        }

        // 6. ed → "" (roasted → roast, smoked → smoke)
        if (result.endsWith("ed")) {
            String root = result.substring(0, result.length() - 2);
            if (root.length() >= 4) {
                debugLog(original, root, "ed-strip");
                return root;
            }
        }

        // 7. er → "" (smoker → smoke, roaster → roast, grinder → grind)
        if (result.endsWith("er")) {
            String root = result.substring(0, result.length() - 2);
            if (root.length() >= 4) {
                debugLog(original, root, "er-strip");
                return root;
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static Map<String, Float> mergeFloatMaps(Map<String, Float> a, Map<String, Float> b) {
        Map<String, Float> merged = new HashMap<>(a);
        b.forEach((k, v) -> merged.merge(k, v, Float::sum));
        return merged;
    }

    private static void debugLog(String original, String result, String rule) {
        if (DEBUG) {
            if (rule.contains("discard") || rule.contains("stop-word")) {
                Nourished.LOGGER.debug("[Stemmer] \"{}\" discarded ({})", original, rule);
            } else if (rule.equals("compound-split")) {
                Nourished.LOGGER.debug("[Stemmer] {} → [{}] (rule: {})", original, result, rule);
            } else {
                Nourished.LOGGER.debug("[Stemmer] {} → {} (rule: {})", original, result, rule);
            }
        }
    }

    // Overload that accepts a List for compound split debug output
    private static void debugLog(String original, List<String> roots, String rule) {
        if (DEBUG) {
            Nourished.LOGGER.debug("[Stemmer] {} → [{}] (rule: {})", original, String.join(", ", roots), rule);
        }
    }
}
