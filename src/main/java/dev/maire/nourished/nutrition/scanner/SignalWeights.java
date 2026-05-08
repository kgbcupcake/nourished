package dev.maire.nourished.nutrition.scanner;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Static final weight maps for food classification signals.
 * All maps are built once at class load time for performance.
 */
public final class SignalWeights {

    private SignalWeights() {}

    // Signal multipliers as specified
    public static final float COMMUNITY_TAG_MULTIPLIER = 5.0f;
    public static final float NAMESPACE_MULTIPLIER = 4.0f;
    public static final float SUFFIX_MULTIPLIER = 3.0f;
    public static final float KEYWORD_MULTIPLIER = 2.0f;
    public static final float ARCHETYPE_MULTIPLIER = 2.0f;
    public static final float FOOD_PROPERTIES_MULTIPLIER = 1.0f;
    public static final float RECIPE_INHERITANCE_MULTIPLIER = 1.0f;
    public static final float NAMESPACE_PEER_MULTIPLIER = 0.5f;

    /**
     * Community tag mappings: c:foods/* tag suffix -> nutrient contributions.
     * Signal 1 - highest weight (5x multiplier).
     */
    public static final Map<String, Map<String, Float>> COMMUNITY_TAG_WEIGHTS = Map.ofEntries(
            Map.entry("fruit", Map.of("fruits", 10f)),
            Map.entry("fruits", Map.of("fruits", 10f)),
            Map.entry("vegetable", Map.of("vegetables", 10f)),
            Map.entry("vegetables", Map.of("vegetables", 10f)),
            Map.entry("meat", Map.of("proteins", 10f)),
            Map.entry("meats", Map.of("proteins", 10f)),
            Map.entry("fish", Map.of("proteins", 10f)),
            Map.entry("seafood", Map.of("proteins", 10f)),
            Map.entry("protein", Map.of("proteins", 10f)),
            Map.entry("proteins", Map.of("proteins", 10f)),
            Map.entry("grain", Map.of("grains", 10f)),
            Map.entry("grains", Map.of("grains", 10f)),
            Map.entry("bread", Map.of("grains", 10f)),
            Map.entry("breads", Map.of("grains", 10f)),
            Map.entry("sugar", Map.of("sugars", 10f)),
            Map.entry("sugars", Map.of("sugars", 10f)),
            Map.entry("candy", Map.of("sugars", 10f)),
            Map.entry("candies", Map.of("sugars", 10f)),
            Map.entry("dessert", Map.of("sugars", 8f, "grains", 2f)),
            Map.entry("desserts", Map.of("sugars", 8f, "grains", 2f)),
            Map.entry("dairy", Map.of("dairy", 10f)),
            Map.entry("milk", Map.of("dairy", 10f)),
            Map.entry("cheese", Map.of("dairy", 10f)),
            Map.entry("egg", Map.of("proteins", 8f, "dairy", 2f)),
            Map.entry("eggs", Map.of("proteins", 8f, "dairy", 2f))
    );

    /**
     * Namespace heuristics: mod namespace -> nutrient contributions.
     * Signal 2 - (4x multiplier).
     */
    public static final Map<String, Map<String, Float>> NAMESPACE_WEIGHTS = Map.ofEntries(
            Map.entry("pamhc2trees", Map.of("fruits", 4f)),
            Map.entry("pamhc2crops", Map.of("vegetables", 3f, "fruits", 2f)),
            Map.entry("pamhc2foods", Map.of("proteins", 2f, "grains", 2f)),
            Map.entry("pamhc2foodcore", Map.of("proteins", 2f, "grains", 2f)),
            Map.entry("aquaculture", Map.of("proteins", 4f)),
            Map.entry("vinery", Map.of("fruits", 3f, "sugars", 2f)),
            Map.entry("herbalbrews", Map.of("fruits", 3f)),
            Map.entry("farmersdelight", Map.of("proteins", 2f, "grains", 2f)),
            Map.entry("fruitsdelight", Map.of("fruits", 4f)),
            Map.entry("neapolitan", Map.of("sugars", 3f, "dairy", 2f)),
            Map.entry("brewinandchewin", Map.of("grains", 3f, "proteins", 2f)),
            Map.entry("culturaldelights", Map.of("grains", 2f, "vegetables", 2f)),
            Map.entry("ends_delight", Map.of("proteins", 3f)),
            Map.entry("nethers_delight", Map.of("proteins", 3f)),
            Map.entry("oceansdelight", Map.of("proteins", 4f)),
            Map.entry("delightful", Map.of("sugars", 2f, "fruits", 2f)),
            Map.entry("miners_delight", Map.of("proteins", 2f, "grains", 2f)),
            Map.entry("twilightdelight", Map.of("proteins", 2f, "fruits", 2f)),
            Map.entry("alexsdelight", Map.of("proteins", 3f)),
            Map.entry("collectorsreap", Map.of("fruits", 3f, "vegetables", 2f)),
            Map.entry("abnormals_delight", Map.of("proteins", 2f, "vegetables", 2f)),
            Map.entry("autumnity", Map.of("fruits", 3f, "sugars", 2f)),
            Map.entry("environmental", Map.of("vegetables", 3f)),
            Map.entry("upgrade_aquatic", Map.of("proteins", 3f)),
            Map.entry("atmospheric", Map.of("fruits", 3f)),
            Map.entry("buzzier_bees", Map.of("sugars", 4f)),
            Map.entry("berry_good", Map.of("fruits", 4f)),
            Map.entry("some_assembly_required", Map.of("grains", 3f, "proteins", 2f))
    );

    /**
     * Suffix patterns: trailing token patterns -> nutrient contributions.
     * Signal 3 - (3x multiplier).
     */
    public static final Map<String, Map<String, Float>> SUFFIX_WEIGHTS = Map.ofEntries(
            Map.entry("juice", Map.of("fruits", 3f)),
            Map.entry("juiceitem", Map.of("fruits", 3f)),
            Map.entry("smoothie", Map.of("fruits", 3f)),
            Map.entry("smoothieitem", Map.of("fruits", 3f)),
            Map.entry("jelly", Map.of("fruits", 3f)),
            Map.entry("jellyitem", Map.of("fruits", 3f)),
            Map.entry("jam", Map.of("fruits", 3f)),
            Map.entry("jamitem", Map.of("fruits", 3f)),
            Map.entry("popsicle", Map.of("fruits", 2f, "sugars", 1f)),
            Map.entry("popsicleitem", Map.of("fruits", 2f, "sugars", 1f)),
            Map.entry("yogurt", Map.of("dairy", 3f)),
            Map.entry("yogurtitem", Map.of("dairy", 3f)),
            Map.entry("butter", Map.of("dairy", 3f)),
            Map.entry("butteritem", Map.of("dairy", 3f)),
            Map.entry("cheese", Map.of("dairy", 3f)),
            Map.entry("cheeseitem", Map.of("dairy", 3f)),
            Map.entry("pie", Map.of("sugars", 2f, "grains", 2f)),
            Map.entry("pieitem", Map.of("sugars", 2f, "grains", 2f)),
            Map.entry("soup", Map.of("vegetables", 2f, "proteins", 1f)),
            Map.entry("soupitem", Map.of("vegetables", 2f, "proteins", 1f)),
            Map.entry("stew", Map.of("vegetables", 2f, "proteins", 1f)),
            Map.entry("stewitem", Map.of("vegetables", 2f, "proteins", 1f)),
            Map.entry("seed", Map.of("grains", 3f)),
            Map.entry("seeditem", Map.of("grains", 3f)),
            Map.entry("seeds", Map.of("grains", 3f)),
            Map.entry("bread", Map.of("grains", 3f)),
            Map.entry("breaditem", Map.of("grains", 3f)),
            Map.entry("cake", Map.of("sugars", 3f)),
            Map.entry("cakeitem", Map.of("sugars", 3f)),
            Map.entry("cookie", Map.of("sugars", 3f)),
            Map.entry("cookieitem", Map.of("sugars", 3f)),
            Map.entry("candy", Map.of("sugars", 3f)),
            Map.entry("candyitem", Map.of("sugars", 3f)),
            Map.entry("icecream", Map.of("sugars", 2f, "dairy", 2f)),
            Map.entry("ice_cream", Map.of("sugars", 2f, "dairy", 2f)),
            Map.entry("milkshake", Map.of("dairy", 2f, "sugars", 1f)),
            Map.entry("salad", Map.of("vegetables", 3f)),
            Map.entry("saladitem", Map.of("vegetables", 3f)),
            Map.entry("steak", Map.of("proteins", 3f)),
            Map.entry("steakitem", Map.of("proteins", 3f)),
            Map.entry("roast", Map.of("proteins", 3f)),
            Map.entry("roastitem", Map.of("proteins", 3f)),
            Map.entry("fillet", Map.of("proteins", 3f)),
            Map.entry("filletitem", Map.of("proteins", 3f))
    );

    /**
     * Positive keyword weights: token -> nutrient contributions.
     * Signal 4 - (2x multiplier).
     */
    public static final Map<String, Map<String, Float>> KEYWORD_WEIGHTS = Map.ofEntries(
            // Fruits
            Map.entry("apple", Map.of("fruits", 3f)),
            Map.entry("berry", Map.of("fruits", 3f)),
            Map.entry("berries", Map.of("fruits", 3f)),
            Map.entry("fruit", Map.of("fruits", 3f)),
            Map.entry("grape", Map.of("fruits", 3f)),
            Map.entry("mango", Map.of("fruits", 3f)),
            Map.entry("cherry", Map.of("fruits", 3f)),
            Map.entry("orange", Map.of("fruits", 3f)),
            Map.entry("lemon", Map.of("fruits", 3f)),
            Map.entry("lime", Map.of("fruits", 3f)),
            Map.entry("banana", Map.of("fruits", 3f)),
            Map.entry("peach", Map.of("fruits", 3f)),
            Map.entry("pear", Map.of("fruits", 3f)),
            Map.entry("plum", Map.of("fruits", 3f)),
            Map.entry("apricot", Map.of("fruits", 3f)),
            Map.entry("melon", Map.of("fruits", 3f)),
            Map.entry("pineapple", Map.of("fruits", 3f)),
            Map.entry("coconut", Map.of("fruits", 3f)),
            Map.entry("avocado", Map.of("fruits", 3f)),
            Map.entry("fig", Map.of("fruits", 3f)),
            Map.entry("date", Map.of("fruits", 3f)),
            Map.entry("papaya", Map.of("fruits", 3f)),
            Map.entry("guava", Map.of("fruits", 3f)),
            Map.entry("kiwi", Map.of("fruits", 3f)),
            Map.entry("pomegranate", Map.of("fruits", 3f)),
            Map.entry("blueberry", Map.of("fruits", 3f)),
            Map.entry("strawberry", Map.of("fruits", 3f)),
            Map.entry("raspberry", Map.of("fruits", 3f)),
            Map.entry("blackberry", Map.of("fruits", 3f)),
            Map.entry("cranberry", Map.of("fruits", 3f)),
            Map.entry("elderberry", Map.of("fruits", 3f)),
            Map.entry("mulberry", Map.of("fruits", 3f)),
            Map.entry("gooseberry", Map.of("fruits", 3f)),

            // Proteins
            Map.entry("steak", Map.of("proteins", 3f)),
            Map.entry("meat", Map.of("proteins", 3f)),
            Map.entry("chicken", Map.of("proteins", 3f)),
            Map.entry("fish", Map.of("proteins", 3f)),
            Map.entry("egg", Map.of("proteins", 3f)),
            Map.entry("bacon", Map.of("proteins", 3f)),
            Map.entry("beef", Map.of("proteins", 3f)),
            Map.entry("pork", Map.of("proteins", 3f)),
            Map.entry("mutton", Map.of("proteins", 3f)),
            Map.entry("lamb", Map.of("proteins", 3f)),
            Map.entry("venison", Map.of("proteins", 3f)),
            Map.entry("turkey", Map.of("proteins", 3f)),
            Map.entry("duck", Map.of("proteins", 3f)),
            Map.entry("rabbit", Map.of("proteins", 3f)),
            Map.entry("salmon", Map.of("proteins", 3f)),
            Map.entry("cod", Map.of("proteins", 3f)),
            Map.entry("tuna", Map.of("proteins", 3f)),
            Map.entry("shrimp", Map.of("proteins", 3f)),
            Map.entry("crab", Map.of("proteins", 3f)),
            Map.entry("lobster", Map.of("proteins", 3f)),
            Map.entry("sausage", Map.of("proteins", 3f)),
            Map.entry("ham", Map.of("proteins", 3f)),
            Map.entry("jerky", Map.of("proteins", 3f)),
            Map.entry("meatball", Map.of("proteins", 3f)),
            Map.entry("patty", Map.of("proteins", 3f)),
            Map.entry("drumstick", Map.of("proteins", 3f)),
            Map.entry("wing", Map.of("proteins", 2f)),
            Map.entry("tofu", Map.of("proteins", 3f)),

            // Dairy
            Map.entry("milk", Map.of("dairy", 3f)),
            Map.entry("cheese", Map.of("dairy", 3f)),
            Map.entry("yogurt", Map.of("dairy", 3f)),
            Map.entry("butter", Map.of("dairy", 3f)),
            Map.entry("cream", Map.of("dairy", 3f)),
            Map.entry("curd", Map.of("dairy", 3f)),
            Map.entry("whey", Map.of("dairy", 3f)),
            Map.entry("mozzarella", Map.of("dairy", 3f)),
            Map.entry("cheddar", Map.of("dairy", 3f)),
            Map.entry("parmesan", Map.of("dairy", 3f)),
            Map.entry("ricotta", Map.of("dairy", 3f)),
            Map.entry("brie", Map.of("dairy", 3f)),
            Map.entry("gouda", Map.of("dairy", 3f)),

            // Grains
            Map.entry("bread", Map.of("grains", 3f)),
            Map.entry("wheat", Map.of("grains", 3f)),
            Map.entry("grain", Map.of("grains", 3f)),
            Map.entry("rice", Map.of("grains", 3f)),
            Map.entry("pasta", Map.of("grains", 3f)),
            Map.entry("noodle", Map.of("grains", 3f)),
            Map.entry("oat", Map.of("grains", 3f)),
            Map.entry("barley", Map.of("grains", 3f)),
            Map.entry("rye", Map.of("grains", 3f)),
            Map.entry("corn", Map.of("grains", 3f)),
            Map.entry("maize", Map.of("grains", 3f)),
            Map.entry("flour", Map.of("grains", 3f)),
            Map.entry("dough", Map.of("grains", 3f)),
            Map.entry("cereal", Map.of("grains", 3f)),
            Map.entry("toast", Map.of("grains", 3f)),
            Map.entry("cracker", Map.of("grains", 3f)),
            Map.entry("bagel", Map.of("grains", 3f)),
            Map.entry("roll", Map.of("grains", 2f)),
            Map.entry("bun", Map.of("grains", 3f)),
            Map.entry("tortilla", Map.of("grains", 3f)),
            Map.entry("waffle", Map.of("grains", 3f)),
            Map.entry("pancake", Map.of("grains", 3f)),

            // Sugars
            Map.entry("sugar", Map.of("sugars", 3f)),
            Map.entry("candy", Map.of("sugars", 3f)),
            Map.entry("chocolate", Map.of("sugars", 3f)),
            Map.entry("cake", Map.of("sugars", 3f)),
            Map.entry("cookie", Map.of("sugars", 3f)),
            Map.entry("pie", Map.of("sugars", 2f, "grains", 1f)),
            Map.entry("tart", Map.of("sugars", 2f, "grains", 1f)),
            Map.entry("dessert", Map.of("sugars", 3f)),
            Map.entry("sweet", Map.of("sugars", 3f)),
            Map.entry("fudge", Map.of("sugars", 3f)),
            Map.entry("caramel", Map.of("sugars", 3f)),
            Map.entry("toffee", Map.of("sugars", 3f)),
            Map.entry("marshmallow", Map.of("sugars", 3f)),
            Map.entry("pudding", Map.of("sugars", 3f)),
            Map.entry("custard", Map.of("sugars", 3f)),
            Map.entry("brownie", Map.of("sugars", 3f)),
            Map.entry("donut", Map.of("sugars", 3f)),
            Map.entry("doughnut", Map.of("sugars", 3f)),
            Map.entry("lollipop", Map.of("sugars", 3f)),
            Map.entry("gummy", Map.of("sugars", 3f)),
            Map.entry("honey", Map.of("sugars", 3f)),
            Map.entry("syrup", Map.of("sugars", 3f)),
            Map.entry("jam", Map.of("sugars", 2f, "fruits", 1f)),
            Map.entry("jelly", Map.of("sugars", 2f, "fruits", 1f)),

            // Vegetables
            Map.entry("carrot", Map.of("vegetables", 3f)),
            Map.entry("potato", Map.of("vegetables", 3f)),
            Map.entry("cabbage", Map.of("vegetables", 3f)),
            Map.entry("salad", Map.of("vegetables", 3f)),
            Map.entry("vegetable", Map.of("vegetables", 3f)),
            Map.entry("veggie", Map.of("vegetables", 3f)),
            Map.entry("onion", Map.of("vegetables", 3f)),
            Map.entry("tomato", Map.of("vegetables", 3f)),
            Map.entry("pepper", Map.of("vegetables", 3f)),
            Map.entry("broccoli", Map.of("vegetables", 3f)),
            Map.entry("cauliflower", Map.of("vegetables", 3f)),
            Map.entry("celery", Map.of("vegetables", 3f)),
            Map.entry("cucumber", Map.of("vegetables", 3f)),
            Map.entry("lettuce", Map.of("vegetables", 3f)),
            Map.entry("spinach", Map.of("vegetables", 3f)),
            Map.entry("kale", Map.of("vegetables", 3f)),
            Map.entry("radish", Map.of("vegetables", 3f)),
            Map.entry("turnip", Map.of("vegetables", 3f)),
            Map.entry("leek", Map.of("vegetables", 3f)),
            Map.entry("artichoke", Map.of("vegetables", 3f)),
            Map.entry("asparagus", Map.of("vegetables", 3f)),
            Map.entry("eggplant", Map.of("vegetables", 3f)),
            Map.entry("zucchini", Map.of("vegetables", 3f)),
            Map.entry("squash", Map.of("vegetables", 3f)),
            Map.entry("pumpkin", Map.of("vegetables", 3f)),
            Map.entry("beet", Map.of("vegetables", 3f)),
            Map.entry("beetroot", Map.of("vegetables", 3f)),
            Map.entry("mushroom", Map.of("vegetables", 3f)),
            Map.entry("garlic", Map.of("vegetables", 3f)),
            Map.entry("ginger", Map.of("vegetables", 3f)),
            Map.entry("herb", Map.of("vegetables", 2f)),
            Map.entry("basil", Map.of("vegetables", 2f)),
            Map.entry("mint", Map.of("vegetables", 2f)),
            Map.entry("parsley", Map.of("vegetables", 2f)),
            Map.entry("seaweed", Map.of("vegetables", 3f)),
            Map.entry("kelp", Map.of("vegetables", 3f))
    );

    /**
     * Negative keyword weights: token -> nutrient suppressions (negative values).
     * Signal 5 - applied as suppression.
     */
    public static final Map<String, Map<String, Float>> NEGATIVE_KEYWORDS = Map.ofEntries(
            Map.entry("steak", Map.of("fruits", -4f, "vegetables", -4f, "dairy", -2f)),
            Map.entry("beef", Map.of("fruits", -4f, "vegetables", -4f, "dairy", -2f)),
            Map.entry("meat", Map.of("fruits", -4f, "vegetables", -4f, "dairy", -2f)),
            Map.entry("pork", Map.of("fruits", -4f, "vegetables", -4f, "dairy", -2f)),
            Map.entry("chicken", Map.of("fruits", -3f, "vegetables", -2f)),
            Map.entry("fish", Map.of("fruits", -3f, "vegetables", -2f, "dairy", -2f)),
            Map.entry("milk", Map.of("proteins", -2f)),
            Map.entry("cream", Map.of("proteins", -2f)),
            Map.entry("cake", Map.of("proteins", -3f)),
            Map.entry("cookie", Map.of("proteins", -3f)),
            Map.entry("candy", Map.of("proteins", -3f, "vegetables", -3f)),
            Map.entry("chocolate", Map.of("proteins", -2f, "vegetables", -3f)),
            Map.entry("raw", Map.of("sugars", -3f, "dairy", -2f)),
            Map.entry("seed", Map.of("fruits", -1f, "proteins", -1f)),
            Map.entry("seeds", Map.of("fruits", -1f, "proteins", -1f)),
            Map.entry("bone", Map.of("fruits", -4f, "vegetables", -4f, "sugars", -4f)),
            Map.entry("rotten", Map.of("fruits", -2f, "vegetables", -2f, "dairy", -2f))
    );

    /**
     * Archetype patterns: compound food patterns for detection.
     * Signal 6 - (2x multiplier).
     */
    public static final List<ArchetypePattern> ARCHETYPE_PATTERNS = List.of(
            new ArchetypePattern("pie", Map.of("grains", 2f, "sugars", 2f, "fruits", 1f)),
            new ArchetypePattern("burger", Map.of("grains", 2f, "proteins", 2f)),
            new ArchetypePattern("sandwich", Map.of("grains", 2f, "proteins", 2f)),
            new ArchetypePattern("pizza", Map.of("grains", 3f, "dairy", 2f, "proteins", 1f)),
            new ArchetypePattern("smoothie", Map.of("fruits", 3f)),
            new ArchetypePattern("salad", Map.of("vegetables", 3f)),
            new ArchetypePattern("ramen", Map.of("grains", 3f, "proteins", 1f)),
            new ArchetypePattern("noodle", Map.of("grains", 3f, "proteins", 1f)),
            new ArchetypePattern("taco", Map.of("grains", 2f, "proteins", 2f)),
            new ArchetypePattern("burrito", Map.of("grains", 2f, "proteins", 2f)),
            new ArchetypePattern("stew", Map.of("vegetables", 2f, "proteins", 1f)),
            new ArchetypePattern("soup", Map.of("vegetables", 2f, "proteins", 1f)),
            new ArchetypePattern("curry", Map.of("vegetables", 2f, "proteins", 1f, "grains", 1f)),
            new ArchetypePattern("wrap", Map.of("grains", 2f, "vegetables", 1f, "proteins", 1f)),
            new ArchetypePattern("roll", Map.of("grains", 2f, "proteins", 1f)),
            new ArchetypePattern("sushi", Map.of("grains", 2f, "proteins", 2f)),
            new ArchetypePattern("roast", Map.of("proteins", 3f, "vegetables", 1f)),
            new ArchetypePattern("casserole", Map.of("vegetables", 2f, "proteins", 1f, "dairy", 1f)),
            new ArchetypePattern("quiche", Map.of("proteins", 2f, "dairy", 2f, "grains", 1f)),
            new ArchetypePattern("omelette", Map.of("proteins", 3f, "dairy", 1f)),
            new ArchetypePattern("omelet", Map.of("proteins", 3f, "dairy", 1f)),
            new ArchetypePattern("frittata", Map.of("proteins", 3f, "dairy", 1f)),
            new ArchetypePattern("parfait", Map.of("dairy", 2f, "fruits", 2f, "sugars", 1f)),
            new ArchetypePattern("sundae", Map.of("dairy", 2f, "sugars", 3f)),
            new ArchetypePattern("milkshake", Map.of("dairy", 3f, "sugars", 2f)),
            new ArchetypePattern("cobbler", Map.of("fruits", 2f, "grains", 1f, "sugars", 2f)),
            new ArchetypePattern("crumble", Map.of("fruits", 2f, "grains", 1f, "sugars", 1f)),
            new ArchetypePattern("pastry", Map.of("grains", 2f, "sugars", 2f))
    );

    /**
     * A pattern for compound food archetype detection.
     */
    public record ArchetypePattern(String pattern, Map<String, Float> contributions) {
        private static final Map<String, Pattern> COMPILED_PATTERNS = new java.util.HashMap<>();

        public boolean matches(String path) {
            return path.contains(pattern);
        }

        public Pattern compiledPattern() {
            return COMPILED_PATTERNS.computeIfAbsent(pattern, p -> Pattern.compile(".*" + Pattern.quote(p) + ".*"));
        }
    }
}
