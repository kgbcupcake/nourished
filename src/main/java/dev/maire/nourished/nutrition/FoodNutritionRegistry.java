package dev.maire.nourished.nutrition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map;

public class FoodNutritionRegistry {

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    /**
     * Diet UI deltas (tunable heuristics). {@link #resolveDietBarKey(ItemStack, Level, NutrientValues)} picks a primary bar from tags / macros.
     */
    public record DietDelta(float calories, float fruits, float vegetables, float proteins, float grains, float sugars, float dairy) {}

    private static final Map<String, String> TAG_DIET_BAR = new LinkedHashMap<>();

    /** Populate TAG_DIET_BAR from NutrientRegistry. Must be called after NutrientRegistry.load(). */
    public static void init() {
        TAG_DIET_BAR.clear();
        for (String key : NutrientRegistry.getKeys()) {
            for (String tag : NutrientRegistry.getTags(key)) {
                TAG_DIET_BAR.put(tag, key);
            }
        }
    }

    private static final Map<String, NutrientValues> TAG_MAP  = new LinkedHashMap<>();
    private static final Map<String, NutrientValues> ITEM_MAP = new LinkedHashMap<>();

    static {
        registerItem("minecraft:cooked_beef",      new NutrientValues(15, 2,  5,  1,  0));
        registerItem("minecraft:cooked_porkchop",  new NutrientValues(14, 2,  6,  1,  0));
        registerItem("minecraft:cooked_chicken",   new NutrientValues(12, 2,  3,  2,  1));
        registerItem("minecraft:cooked_mutton",    new NutrientValues(13, 2,  4,  1,  0));
        registerItem("minecraft:cooked_rabbit",    new NutrientValues(11, 2,  2,  3,  1));
        registerItem("minecraft:cooked_cod",       new NutrientValues(10, 1,  2,  4,  3));
        registerItem("minecraft:cooked_salmon",    new NutrientValues(11, 1,  4,  4,  3));
        registerItem("minecraft:egg",              new NutrientValues(8,  1,  5,  2,  0));
        registerItem("minecraft:bread",            new NutrientValues(3,  15, 2,  1,  0));
        registerItem("minecraft:baked_potato",     new NutrientValues(2,  12, 1,  4,  2));
        registerItem("minecraft:pumpkin_pie",      new NutrientValues(2,  14, 4,  3,  1));
        registerItem("minecraft:cookie",           new NutrientValues(1,  10, 3,  0,  0));
        registerItem("minecraft:cake",             new NutrientValues(3,  12, 5,  1,  2));
        registerItem("minecraft:apple",            new NutrientValues(0,  6,  0,  10, 5));
        registerItem("minecraft:golden_apple",     new NutrientValues(0,  8,  0,  15, 8));
        registerItem("minecraft:melon_slice",      new NutrientValues(0,  4,  0,  8,  10));
        registerItem("minecraft:sweet_berries",    new NutrientValues(0,  3,  0,  10, 4));
        registerItem("minecraft:glow_berries",     new NutrientValues(0,  3,  0,  12, 4));
        registerItem("minecraft:mushroom_stew",    new NutrientValues(4,  6,  2,  8,  8));
        registerItem("minecraft:rabbit_stew",      new NutrientValues(10, 8,  4,  6,  6));
        registerItem("minecraft:beetroot_soup",    new NutrientValues(2,  5,  1,  12, 6));
        registerItem("minecraft:suspicious_stew",  new NutrientValues(2,  4,  1,  10, 5));
        registerItem("minecraft:honey_bottle",     new NutrientValues(0,  8,  0,  2,  12));
        registerItem("minecraft:rotten_flesh",     new NutrientValues(2,  0,  1,  0,  0));
        registerItem("minecraft:spider_eye",       new NutrientValues(1,  0,  0,  1,  0));
        registerItem("minecraft:poisonous_potato", new NutrientValues(1,  4,  0,  0,  0));

        registerTag("c:foods/cooked_meat",         new NutrientValues(13, 2,  4,  1,  0));
        registerTag("c:foods/cooked_fish",         new NutrientValues(10, 1,  3,  4,  3));
        registerTag("c:foods/raw_meat",            new NutrientValues(5,  0,  3,  0,  0));
        registerTag("c:foods/raw_fish",            new NutrientValues(4,  0,  2,  2,  2));
        registerTag("c:foods/vegetables",          new NutrientValues(1,  4,  0,  10, 3));
        registerTag("c:foods/fruits",              new NutrientValues(0,  5,  0,  10, 6));
        registerTag("c:foods/berries",             new NutrientValues(0,  3,  0,  12, 4));
        registerTag("c:foods/bread",               new NutrientValues(3,  14, 2,  1,  0));
        registerTag("c:foods/grain",               new NutrientValues(2,  12, 1,  2,  0));
        registerTag("c:foods/crops",               new NutrientValues(1,  8,  0,  5,  2));
        registerTag("c:foods/eggs",                new NutrientValues(8,  1,  5,  2,  0));
        registerTag("c:foods/dairy",               new NutrientValues(5,  3,  6,  3,  4));
        registerTag("c:foods/sweets",              new NutrientValues(0,  12, 3,  0,  0));
        registerTag("c:foods/candy",               new NutrientValues(0,  10, 2,  0,  0));
        registerTag("c:foods/cookies",             new NutrientValues(1,  10, 3,  0,  0));
        registerTag("c:foods/cake",                new NutrientValues(2,  12, 5,  1,  1));
        registerTag("c:foods/pie",                 new NutrientValues(2,  13, 4,  2,  1));
        registerTag("c:foods/drinks",              new NutrientValues(0,  2,  0,  2,  10));
        registerTag("c:foods/tea",                 new NutrientValues(0,  1,  0,  4,  12));
        registerTag("c:foods/coffee",              new NutrientValues(0,  2,  0,  1,  8));
        registerTag("c:foods/juice",               new NutrientValues(0,  6,  0,  8,  10));
        registerTag("c:foods/soup",                new NutrientValues(5,  6,  2,  7,  8));
        registerTag("c:foods/stew",                new NutrientValues(7,  6,  3,  6,  7));
        registerTag("c:foods/salad",               new NutrientValues(2,  4,  1,  12, 3));
        registerTag("c:foods/pasta",               new NutrientValues(4,  14, 3,  3,  1));
        registerTag("c:foods/sandwich",            new NutrientValues(6,  10, 4,  3,  1));
        registerTag("c:foods/mushroom",            new NutrientValues(3,  3,  1,  8,  2));
        registerTag("c:foods/nuts",                new NutrientValues(6,  4,  8,  3,  0));
        registerTag("farmersdelight:foods/pasta",        new NutrientValues(4,  14, 3,  3,  1));
        registerTag("farmersdelight:foods/soup",         new NutrientValues(5,  6,  2,  7,  8));
        registerTag("farmersdelight:foods/salad",        new NutrientValues(2,  4,  1,  12, 3));
        registerTag("farmersdelight:foods/stew",         new NutrientValues(7,  6,  3,  6,  7));
        registerTag("farmersdelight:foods/comfort_food", new NutrientValues(5,  8,  4,  5,  5));
        registerTag("farmersdelight:vegetables",         new NutrientValues(1,  4,  0,  10, 3));
        registerTag("farmersdelight:mushrooms",          new NutrientValues(3,  3,  1,  8,  2));
        registerTag("pamhc2food:fruititem",          new NutrientValues(0,  5,  0,  10, 6));
        registerTag("pamhc2food:veggiefooditem",     new NutrientValues(1,  4,  0,  10, 3));
        registerTag("pamhc2food:grainitem",          new NutrientValues(2,  12, 1,  2,  0));
        registerTag("pamhc2food:proteinitem",        new NutrientValues(10, 2,  4,  2,  1));
        registerTag("pamhc2food:sweetitem",          new NutrientValues(0,  10, 2,  1,  0));
        registerTag("pamhc2food:herbitem",           new NutrientValues(0,  1,  0,  6,  1));
        registerTag("pamhc2food:fishitem",           new NutrientValues(9,  1,  3,  4,  3));
        registerTag("pamhc2food:nutitem",            new NutrientValues(6,  4,  8,  3,  0));
        registerTag("pamhc2food:juiceitem",          new NutrientValues(0,  5,  0,  8,  10));
        registerTag("pamhc2food:smoothieitem",       new NutrientValues(1,  6,  1,  9,  8));
        registerTag("pamhc2food:toasteditem",        new NutrientValues(3,  12, 2,  2,  0));
        registerTag("pamhc2food:saladitem",          new NutrientValues(2,  4,  1,  12, 3));
        registerTag("pamhc2food:soupitem",           new NutrientValues(5,  6,  2,  7,  8));
        registerTag("pamhc2food:stewitem",           new NutrientValues(7,  6,  3,  6,  7));
        registerTag("pamhc2food:pieitem",            new NutrientValues(2,  13, 4,  2,  1));
        registerTag("pamhc2food:cakeitem",           new NutrientValues(2,  12, 5,  1,  1));
        registerTag("pamhc2food:cheeseitem",         new NutrientValues(6,  1,  8,  2,  2));
        registerTag("pamhc2food:dairyitem",          new NutrientValues(5,  3,  6,  3,  4));
    }

    public static void registerItem(String itemId, NutrientValues values) {
        ITEM_MAP.put(itemId, values);
    }

    public static void registerTag(String tagId, NutrientValues values) {
        TAG_MAP.put(tagId, values);
    }

    public static NutrientValues getNutrients(ItemStack stack, Level level) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && ITEM_MAP.containsKey(id.toString())) {
            return ITEM_MAP.get(id.toString());
        }
        for (Map.Entry<String, NutrientValues> entry : TAG_MAP.entrySet()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    ResourceLocation.parse(entry.getKey()));
            if (stack.is(tag)) return entry.getValue();
        }
        return deriveFallbackNutrients(stack, level);
    }

    private static NutrientValues deriveFallbackNutrients(ItemStack stack, Level level) {
        // Heuristic fallback for unregistered foods: infer nutrient profile from hunger/saturation.
        FoodProperties food = stack.getItem().components().get(net.minecraft.core.component.DataComponents.FOOD);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        float nutrition = food.nutrition();
        float saturation = Math.max(0f, food.saturation());
        float totalPoints = Math.max(0.5f, nutrition * 0.4f);

        float fruitsW = 0.12f;
        float vegetablesW = 0.12f;
        float proteinsW = 0.22f;
        float grainsW = 0.22f;
        float sugarsW = 0.20f;
        float dairyW = 0.12f;

        if (saturation > 0.5f) {
            float satBoost = Math.min(0.35f, (saturation - 0.5f) * 0.5f);
            proteinsW += satBoost * 0.55f;
            grainsW += satBoost * 0.45f;
            dairyW += satBoost * 0.30f;
            sugarsW -= satBoost * 0.85f;
        } else if (saturation < 0.3f) {
            float sugarBoost = Math.min(0.35f, (0.3f - saturation) * 1.2f);
            sugarsW += sugarBoost;
            proteinsW -= sugarBoost * 0.50f;
            grainsW -= sugarBoost * 0.35f;
            dairyW -= sugarBoost * 0.15f;
        }

        fruitsW = Math.max(0.02f, fruitsW);
        vegetablesW = Math.max(0.02f, vegetablesW);
        proteinsW = Math.max(0.02f, proteinsW);
        grainsW = Math.max(0.02f, grainsW);
        sugarsW = Math.max(0.02f, sugarsW);
        dairyW = Math.max(0.02f, dairyW);

        float weightSum = fruitsW + vegetablesW + proteinsW + grainsW + sugarsW + dairyW;
        float fruits = totalPoints * (fruitsW / weightSum);
        float vegetables = totalPoints * (vegetablesW / weightSum);
        float proteins = totalPoints * (proteinsW / weightSum);
        float grains = totalPoints * (grainsW / weightSum);
        float sugars = totalPoints * (sugarsW / weightSum);
        float dairy = totalPoints * (dairyW / weightSum);

        float protein = proteins + dairy * 0.35f;
        float carbs = grains + sugars * 0.60f + fruits * 0.20f;
        float fats = dairy * 0.45f + sugars * 0.30f + proteins * 0.15f;
        float vitamins = vegetables + fruits * 0.70f;
        float hydration = fruits * 0.60f + vegetables * 0.35f + dairy * 0.15f;

        return new NutrientValues(
                Mth.clamp(protein, 0.1f, 2.0f),
                Mth.clamp(carbs, 0.1f, 2.0f),
                Mth.clamp(fats, 0.1f, 2.0f),
                Mth.clamp(vitamins, 0.1f, 2.0f),
                Mth.clamp(hydration, 0.1f, 2.0f)
        );
    }

    /**
     * Heuristic diet contribution from food properties, nutrition registry values, and item tags.
     * Tunable: scales and tag→bar map drive how eating moves the five bars.
     */
    public static DietDelta computeDietDelta(ItemStack stack, Level level, NutrientValues values, int foodNutrition, float foodSaturation) {
        Objects.requireNonNull(level, "level");
        float calories = foodNutrition * 18f + foodSaturation * 12f + values.carbs() * 0.8f;
        String primary = resolveDietBarKey(stack, level, values);
        float burst = foodNutrition * 0.022f + foodSaturation * 0.03f + 0.012f;

        float fruits = values.hydration() * 0.004f + values.vitamins() * 0.002f;
        float vegetables = values.vitamins() * 0.006f;
        float proteins = values.protein() * 0.008f;
        float grains = values.carbs() * 0.005f;
        float sugars = values.fats() * 0.003f + values.carbs() * 0.004f;
        float dairy = values.fats() * 0.004f + values.protein() * 0.002f;

        switch (primary) {
            case "fruits" -> fruits += burst;
            case "vegetables" -> vegetables += burst;
            case "proteins" -> proteins += burst;
            case "grains" -> grains += burst;
            case "sugars" -> sugars += burst;
            case "dairy" -> dairy += burst;
            default -> grains += burst * 0.35f;
        }

        return new DietDelta(calories, fruits, vegetables, proteins, grains, sugars, dairy);
    }

    private static String resolveDietBarKey(ItemStack stack, Level level, NutrientValues values) {
        for (Map.Entry<String, String> entry : TAG_DIET_BAR.entrySet()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.getKey()));
            if (stack.is(tag)) return entry.getValue();
        }
        float p = values.protein();
        float c = values.carbs();
        float v = values.vitamins();
        float h = values.hydration();
        float maxMacro = Math.max(p, Math.max(c, v));
        if (maxMacro == p && p >= c * 1.1f) return "proteins";
        if (maxMacro == v && h >= c * 0.4f) return "fruits";
        if (maxMacro == v) return "vegetables";
        if (c >= p && c >= v) {
            return values.fats() > 4f ? "grains" : "sugars";
        }
        return "grains";
    }
}
