package dev.maire.nourished.tooling.scanner;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
    // Food compound words that carry two classification signals.
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

    private static final Pattern CAMEL_BOUNDARY_LOWER_UPPER = Pattern.compile("(?<=[a-z])(?=[A-Z])");
    private static final Pattern CAMEL_BOUNDARY_ACRONYM = Pattern.compile("(?<=[A-Z])(?=[A-Z][a-z])");

    /** Longest-first greedy dictionary for ungluing lowercase run-on segments ({@code honeyglazedham}). */
    private static final String[] FOOD_DICT_LONGEST_FIRST;

    static {
        Set<String> words = new HashSet<>();
        for (var e : IRREGULAR_FORMS.entrySet()) {
            if (e.getKey().length() >= 3) words.add(e.getKey());
            if (e.getValue().length() >= 3) words.add(e.getValue());
        }
        for (var e : COMPOUND_SPLITS.entrySet()) {
            if (e.getKey().length() >= 3) words.add(e.getKey());
            for (String p : e.getValue()) {
                if (p.length() >= 3) words.add(p);
            }
        }
        String[] extra = {
            "almond", "apple", "apricot", "avocado", "bacon", "bake", "baked", "banana", "barley",
            "basil", "bbq", "bean", "beef", "beet", "berry", "biscuit", "blackberry", "blueberry",
            "boil", "boiled", "bowl", "bread", "breast", "brownie", "burger", "burrito", "butter",
            "cabbage", "cake", "candied", "caramel", "carrot", "cashew", "cauliflower", "celery",
            "cheese", "cherry", "chicken", "chili", "chocolate", "chop", "chunk", "chunks",
            "cilantro", "cinnamon", "clam", "cod", "cookie", "coriander", "corn", "crab",
            "cranberry", "cream", "crisp", "croissant", "cucumber", "cumin", "curry", "cupcake",
            "donut", "dough", "dried", "drumstick", "duck", "dumpling", "egg", "enchilada",
            "fajita", "fillet", "fish", "flour", "fries", "fry", "fried", "fudge", "garlic",
            "ginger", "glaze", "glazed", "goose", "grape", "gratin", "gravy", "grill", "grilled",
            "ground", "guacamole", "halibut", "ham", "hamburger", "hash", "hazelnut", "herb",
            "honey", "icing", "jalapeno", "jam", "julienne", "kebab", "ketchup", "lamb", "lasagna",
            "leek", "lemon", "lentil", "lime", "lingon", "lobster", "lox", "macaroni",
            "mango", "marinade", "marinated", "mashed", "mayo", "mead", "meat", "meatball",
            "melon",    "milk",     "mint",     "muffin",   "mushroom", "mussel", "mustard",
            "mutton",   "noodle",   "nut",      "oat",      "omelet",   "onion",  "orange",
            "oregano",  "oyster",   "paella",   "pancake",  "paprika",  "parmesan", "parsley", "pasta", "pastry", "pea", "peach",
            "peanut", "pear", "pepper", "perch", "pickle", "pickled", "pie", "pilaf", "pineapple",
            "pistachio", "pizza", "plum", "poach", "poached", "polenta", "popcorn", "poppy",
            "popsicle", "pork", "porridge", "potato", "poutine", "praline", "prawn", "pretzel",
            "prosciutto", "pudding", "pumpkin", "quinoa", "radish", "raisin", "ramen", "raspberry",
            "ravioli", "rhubarb", "rib", "ribs", "rice", "ricotta", "roast", "roasted", "roll",
            "romaine", "rose", "saffron", "salad", "salami", "salmon", "salsa", "salt", "sandwich",
            "sardine", "sauce", "sausage", "scallop", "scone", "scramble", "scrambled", "seabass",
            "seed", "sesame", "shallot", "shell", "shrimp", "slice", "sliced", "smoke", "smoked",
            "smoothie", "snack", "soup", "sour", "soy", "spaghetti", "spinach", "steak", "steam",
            "steamed", "stew", "stir", "strawberry", "sugar", "sushi", "sweet", "syrup", "taco",
            "tart", "tempeh", "teriyaki", "thigh", "toast", "toffee", "tofu", "tomato", "tortilla",
            "trail", "truffle", "tuna", "turkey", "turnip", "vanilla", "veal", "venison", "vinegar",
            "waffle", "walnut", "wasabi", "watermelon", "wheat", "whip", "whiskey", "wing", "wings",
            "wrap", "yam", "yeast", "yogurt", "zest", "zucchini",
        };
        for (String x : extra) {
            if (x.length() >= 3) words.add(x);
        }
        List<String> list = new ArrayList<>(words);
        list.sort(Comparator.comparingInt(String::length).reversed().thenComparing(a -> a));
        FOOD_DICT_LONGEST_FIRST = list.toArray(String[]::new);
    }

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
        String t = token.toLowerCase(Locale.ROOT);
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
        String t = token.toLowerCase(Locale.ROOT);
        if (t.endsWith("item") && t.length() > 4) t = t.substring(0, t.length() - 4);
        return STOP_WORDS.contains(t);
    }

    /**
     * Resource path segment only: drops {@code namespace:} when present.
     */
    public static String localNamePart(String itemId) {
        if (itemId == null || itemId.isEmpty()) return "";
        int c = itemId.lastIndexOf(':');
        return c >= 0 ? itemId.substring(c + 1) : itemId;
    }

    /**
     * Raw path tokens for suffix-style signals (underscored paths split on underscores; CamelCase paths
     * expanded and noise-stripped, not stemmed). Keys in unscored JSON maps match these segments.
     */
    public static List<String> rawSegmentsForPath(String itemId) {
        return expandRawScoringTokens(localNamePart(itemId));
    }

    /**
     * Single entry for keyword-style scoring: expands path tokens (underscore vs camel + food dictionary),
     * then runs {@link #stemAll(String)} on each segment so classifier keywords align with spec stems.
     */
    public static List<String> tokenizeForScoring(String itemId) {
        return runScoringStemPipeline(expandRawScoringTokens(localNamePart(itemId)));
    }

    private static boolean shouldApplyCamelPreprocessor(String localPath) {
        return localPath != null && !localPath.contains("_");
    }

    private static boolean isResourceNoiseToken(String s) {
        if (s == null || s.isEmpty()) return false;
        String t = s.toLowerCase(Locale.ROOT);
        return t.equals("item") || t.equals("block") || t.equals("food");
    }

    private static boolean hasUpperCaseAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') return true;
        }
        return false;
    }

    /**
     * Strips trailing {@code item}, {@code block}, {@code food} (case-insensitive, repeatable).
     */
    private static String stripTrailingResourceNoise(String s) {
        String cur = s;
        while (!cur.isEmpty()) {
            String lower = cur.toLowerCase(Locale.ROOT);
            int trim = 0;
            if (lower.endsWith("item") && cur.length() > 4) trim = 4;
            else if (lower.endsWith("block") && cur.length() > 5) trim = 5;
            else if (lower.endsWith("food") && cur.length() > 4) trim = 4;
            if (trim == 0) break;
            cur = cur.substring(0, cur.length() - trim);
        }
        return cur;
    }

    private static List<String> splitCamelCase(String s) {
        List<String> out = new ArrayList<>();
        for (String chunk : CAMEL_BOUNDARY_LOWER_UPPER.split(s)) {
            if (chunk.isEmpty()) continue;
            for (String piece : CAMEL_BOUNDARY_ACRONYM.split(chunk)) {
                if (!piece.isEmpty()) out.add(piece);
            }
        }
        return out;
    }

    private static List<String> expandRawScoringTokens(String local) {
        if (local == null || local.isEmpty()) return List.of();
        if (!shouldApplyCamelPreprocessor(local)) {
            List<String> out = new ArrayList<>();
            for (String p : local.split("_")) {
                if (!p.isEmpty() && !isResourceNoiseToken(p)) out.add(p);
            }
            return out;
        }
        String stripped = stripTrailingResourceNoise(local);
        if (stripped.isEmpty()) return List.of();
        List<String> pieces = hasUpperCaseAscii(stripped) ? splitCamelCase(stripped) : List.of(stripped);
        List<String> expanded = new ArrayList<>();
        for (String seg : pieces) {
            if (seg.isEmpty() || isResourceNoiseToken(seg)) continue;
            String low = seg.toLowerCase(Locale.ROOT);
            if (low.length() >= 6 && low.chars().allMatch(Character::isLetter)) {
                expanded.addAll(greedyDictionarySplit(low));
            } else {
                expanded.add(seg);
            }
        }
        expanded.removeIf(FoodTokenStemmer::isResourceNoiseToken);
        return List.copyOf(expanded);
    }

    private static boolean startsWithDictWordAt(String segment, int i) {
        int n = segment.length();
        for (String w : FOOD_DICT_LONGEST_FIRST) {
            if (n - i >= w.length() && segment.startsWith(w, i)) return true;
        }
        return false;
    }

    private static List<String> greedyDictionarySplit(String segment) {
        List<String> parts = new ArrayList<>();
        int i = 0;
        int n = segment.length();
        while (i < n) {
            String matched = null;
            for (String w : FOOD_DICT_LONGEST_FIRST) {
                if (n - i >= w.length() && segment.startsWith(w, i)) {
                    matched = w;
                    break;
                }
            }
            if (matched != null) {
                parts.add(matched);
                i += matched.length();
            } else {
                int j = i + 1;
                while (j < n && !startsWithDictWordAt(segment, j)) {
                    j++;
                }
                parts.add(segment.substring(i, j));
                i = j;
            }
        }
        return parts;
    }

    private static List<String> runScoringStemPipeline(List<String> rawTokens) {
        List<String> out = new ArrayList<>();
        for (String raw : rawTokens) {
            if (raw == null || raw.isEmpty()) continue;
            out.addAll(stemAll(raw));
        }
        return out;
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
}
