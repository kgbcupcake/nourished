package dev.maire.nourished.nutrition;

import dev.maire.nourished.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves food item IDs to logical food families based on path keyword matching.
 * Used by both {@link FoodNutritionRegistry} and {@link UnassignedFoodScanner}.
 * All resolutions are cached for O(1) repeated lookups.
 */
@ApiStatus.Internal
public final class FoodFamilyResolver {

    // ConcurrentHashMap: resolve() may be hit from multiple threads during gameplay; computeIfAbsent is safe here.
    private static final Map<ResourceLocation, String> CACHE = new ConcurrentHashMap<>();

    private static final Map<String, String[]> FAMILY_KEYWORDS = new LinkedHashMap<>();

    static {
        FAMILY_KEYWORDS.put("berry",       new String[]{"berry", "berries", "strawberry", "blueberry", "raspberry", "blackberry", "cranberry", "goji"});
        FAMILY_KEYWORDS.put("citrus",      new String[]{"orange", "lemon", "lime", "grapefruit", "citrus", "tangerine", "mandarin"});
        FAMILY_KEYWORDS.put("apple",       new String[]{"apple", "cider"});
        FAMILY_KEYWORDS.put("melon",       new String[]{"melon", "watermelon", "cantaloupe", "honeydew"});
        FAMILY_KEYWORDS.put("tropical",    new String[]{"banana", "mango", "pineapple", "coconut", "papaya", "kiwi", "passion"});
        FAMILY_KEYWORDS.put("stone_fruit", new String[]{"peach", "plum", "cherry", "apricot", "nectarine"});
        FAMILY_KEYWORDS.put("fish",        new String[]{"fish", "salmon", "cod", "tuna", "trout", "bass", "carp", "mackerel", "sardine", "anchov"});
        FAMILY_KEYWORDS.put("shellfish",   new String[]{"shrimp", "crab", "lobster", "clam", "mussel", "oyster", "scallop", "prawn"});
        FAMILY_KEYWORDS.put("poultry",     new String[]{"chicken", "turkey", "duck", "goose", "poultry", "fowl"});
        FAMILY_KEYWORDS.put("red_meat",    new String[]{"beef", "steak", "pork", "lamb", "mutton", "venison", "bison"});
        FAMILY_KEYWORDS.put("mushroom",    new String[]{"mushroom", "fungus", "truffle", "shroom", "chanterelle", "morel", "portobello"});
        FAMILY_KEYWORDS.put("bread",       new String[]{"bread", "loaf", "baguette", "roll", "bun", "toast", "sourdough"});
        FAMILY_KEYWORDS.put("pasta",       new String[]{"pasta", "noodle", "spaghetti", "macaroni", "lasagna", "ravioli", "ramen", "udon"});
        FAMILY_KEYWORDS.put("rice",        new String[]{"rice", "risotto", "sushi"});
        FAMILY_KEYWORDS.put("leafy_green", new String[]{"lettuce", "spinach", "kale", "cabbage", "chard", "arugula", "salad"});
    }

    private FoodFamilyResolver() {}

    /**
     * Resolves an item to its food family, or null if no match.
     * Results are cached permanently for the session.
     */
    public static String resolve(ResourceLocation itemId) {
        if (itemId == null) return null;
        return CACHE.computeIfAbsent(itemId, FoodFamilyResolver::doResolve);
    }

    private static String doResolve(ResourceLocation itemId) {
        String path = itemId.getPath().toLowerCase();
        for (Map.Entry<String, String[]> family : FAMILY_KEYWORDS.entrySet()) {
            for (String keyword : family.getValue()) {
                if (path.contains(keyword)) return family.getKey();
            }
        }
        return null;
    }

    /** Clears the resolution cache. Call on hot-reload only. */
    public static void clearCache() {
        CACHE.clear();
    }

    public static void replaceFamilies(Map<String, List<String>> configuredFamilies) {
        FAMILY_KEYWORDS.clear();
        for (Map.Entry<String, List<String>> entry : configuredFamilies.entrySet()) {
            FAMILY_KEYWORDS.put(entry.getKey(), entry.getValue().toArray(String[]::new));
        }
        clearCache();
    }
}
