package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.registry.AbstractRegistry;
import net.minecraft.resources.ResourceLocation;

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

    private static final class Core extends AbstractRegistry<String, String[]> {
        Core() {
            super("FoodFamilyResolver");
        }
    }

    private static final Core INSTANCE = new Core();

    static {
        INSTANCE.reset();
        INSTANCE.register("berry",       new String[]{"berry", "berries", "strawberry", "blueberry", "raspberry", "blackberry", "cranberry", "goji"});
        INSTANCE.register("citrus",      new String[]{"orange", "lemon", "lime", "grapefruit", "citrus", "tangerine", "mandarin"});
        INSTANCE.register("apple",       new String[]{"apple", "cider"});
        INSTANCE.register("melon",       new String[]{"melon", "watermelon", "cantaloupe", "honeydew"});
        INSTANCE.register("tropical",    new String[]{"banana", "mango", "pineapple", "coconut", "papaya", "kiwi", "passion"});
        INSTANCE.register("stone_fruit", new String[]{"peach", "plum", "cherry", "apricot", "nectarine"});
        INSTANCE.register("fish",        new String[]{"fish", "salmon", "cod", "tuna", "trout", "bass", "carp", "mackerel", "sardine", "anchov"});
        INSTANCE.register("shellfish",   new String[]{"shrimp", "crab", "lobster", "clam", "mussel", "oyster", "scallop", "prawn"});
        INSTANCE.register("poultry",     new String[]{"chicken", "turkey", "duck", "goose", "poultry", "fowl"});
        INSTANCE.register("red_meat",    new String[]{"beef", "steak", "pork", "lamb", "mutton", "venison", "bison"});
        INSTANCE.register("mushroom",    new String[]{"mushroom", "fungus", "truffle", "shroom", "chanterelle", "morel", "portobello"});
        INSTANCE.register("bread",       new String[]{"bread", "loaf", "baguette", "roll", "bun", "toast", "sourdough"});
        INSTANCE.register("pasta",       new String[]{"pasta", "noodle", "spaghetti", "macaroni", "lasagna", "ravioli", "ramen", "udon"});
        INSTANCE.register("rice",        new String[]{"rice", "risotto", "sushi"});
        INSTANCE.register("leafy_green", new String[]{"lettuce", "spinach", "kale", "cabbage", "chard", "arugula", "salad"});
        INSTANCE.freeze();
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
        for (String family : INSTANCE.keys()) {
            String[] keywords = INSTANCE.get(family);
            if (keywords == null) {
                continue;
            }
            for (String keyword : keywords) {
                if (path.contains(keyword)) return family;
            }
        }
        return null;
    }

    /** Clears the resolution cache. Call on hot-reload only. */
    public static void clearCache() {
        CACHE.clear();
    }

    public static void replaceFamilies(Map<String, List<String>> configuredFamilies) {
        INSTANCE.reset();
        for (Map.Entry<String, List<String>> entry : configuredFamilies.entrySet()) {
            INSTANCE.register(entry.getKey(), entry.getValue().toArray(String[]::new));
        }
        INSTANCE.freeze();
        clearCache();
    }
}
