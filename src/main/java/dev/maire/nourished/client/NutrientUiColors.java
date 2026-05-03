package dev.maire.nourished.client;

/**
 * Shared ARGB palette for diet nutrient keys. Matches {@code DietScreen} base bar colors.
 */
public final class NutrientUiColors {

    private static final int COL_CYAN = 0xFF4DD9D9;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_GOLD = 0xFFFFD65C;
    private static final int COL_PURPLE = 0xFFA95FFF;
    private static final int COL_WHITE = 0xFFFFFFFF;

    private NutrientUiColors() {}

    /** ARGB accent color for a nutrient registry key (fruits, vegetables, …). */
    public static int baseColorArgb(String key) {
        return switch (key) {
            case "fruits" -> COL_GREEN;
            case "vegetables" -> COL_CYAN;
            // Meat / protein identity without using alert red (that tier is reserved for critical-low bars).
            case "proteins" -> COL_GREEN;
            case "grains" -> COL_GOLD;
            case "sugars" -> COL_PURPLE;
            case "dairy" -> COL_WHITE;
            default -> COL_GREEN;
        };
    }
}
