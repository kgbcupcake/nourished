package dev.maire.nourished.nutrition;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map;

public class FoodNutritionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    /**
     * Diet UI deltas. {@link #resolveDietBarKey(ItemStack, Level, NutrientValues)} picks a primary bar.
     */
    public record DietDelta(float calories, float fruits, float vegetables, float proteins, float grains, float sugars, float dairy) {}

    // ── Pre-created TagKey constants (never recreated per-lookup) ─────────────

    // Primary category tags (user-specified names)
    private static final TagKey<Item> TAG_FRUITS     = tag("c:foods/fruits");
    private static final TagKey<Item> TAG_VEGETABLES = tag("c:foods/vegetables");
    private static final TagKey<Item> TAG_PROTEINS   = tag("c:foods/meats");
    private static final TagKey<Item> TAG_GRAINS     = tag("c:foods/bread");
    private static final TagKey<Item> TAG_SUGARS     = tag("c:foods/sweets");
    private static final TagKey<Item> TAG_DAIRY      = tag("c:foods/dairy");

    // Additional food-category tags for priority fallback
    private static final TagKey<Item> TAG_SALAD       = tag("c:foods/salad");
    private static final TagKey<Item> TAG_BERRIES     = tag("c:foods/berries");
    private static final TagKey<Item> TAG_JUICE       = tag("c:foods/juice");
    private static final TagKey<Item> TAG_COOKED_MEAT = tag("c:foods/cooked_meat");
    private static final TagKey<Item> TAG_RAW_MEAT    = tag("c:foods/raw_meat");
    private static final TagKey<Item> TAG_COOKED_FISH = tag("c:foods/cooked_fish");
    private static final TagKey<Item> TAG_RAW_FISH    = tag("c:foods/raw_fish");
    private static final TagKey<Item> TAG_EGGS        = tag("c:foods/eggs");
    private static final TagKey<Item> TAG_GRAIN       = tag("c:foods/grain");
    private static final TagKey<Item> TAG_FOODS_CROPS = tag("c:foods/crops");
    private static final TagKey<Item> TAG_PASTA       = tag("c:foods/pasta");
    private static final TagKey<Item> TAG_CANDY       = tag("c:foods/candy");
    private static final TagKey<Item> TAG_COOKIES     = tag("c:foods/cookies");
    private static final TagKey<Item> TAG_CAKE        = tag("c:foods/cake");
    private static final TagKey<Item> TAG_PIE         = tag("c:foods/pie");

    private static final TagKey<Item> TAG_PROTEINS_FISH = tag("c:foods/fish");
    private static final TagKey<Item> TAG_PROTEINS_MEAT = tag("c:foods/meat");
    private static final TagKey<Item> TAG_FISH_FOOD     = tag("c:foods/fish_food");
    private static final TagKey<Item> TAG_CROPS        = tag("c:crops");
    private static final TagKey<Item> TAG_GRAIN_CROPS  = tag("c:crops/grain");
    private static final TagKey<Item> TAG_FRUIT_CROPS  = tag("c:crops/fruit");
    private static final TagKey<Item> TAG_VEGGIE_CROPS = tag("c:crops/vegetable");

    private static TagKey<Item> tag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    // ── Diet-bar lookup maps ──────────────────────────────────────────────────

    private static final Map<String, String>        TAG_DIET_BAR  = new LinkedHashMap<>();
    private static final Map<String, String>        ITEM_DIET_BAR = new LinkedHashMap<>();
    private static final Map<String, TagKey<Item>>  TAG_DIET_BAR_KEYS = new LinkedHashMap<>();

    /** Populate TAG_DIET_BAR from NutrientRegistry. Must be called after NutrientRegistry.load(). */
    public static void init() {
        TAG_DIET_BAR.clear();
        TAG_DIET_BAR_KEYS.clear();
        for (String key : NutrientRegistry.getKeys()) {
            for (String tagId : NutrientRegistry.getTags(key)) {
                TAG_DIET_BAR.put(tagId, key);
                TAG_DIET_BAR_KEYS.put(tagId, tag(tagId));
            }
        }
        registerNamespaceScanningCompat();
    }

    /**
     * Registers food items from optional mods by scanning {@link BuiltInRegistries#ITEM}
     * (must run from {@link #init()} after registries are populated).
     */
    private static void registerNamespaceScanningCompat() {
        if (ModList.get().isLoaded("bakery")) {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
                if (loc == null || !"bakery".equals(loc.getNamespace())) continue;
                if (!item.components().has(DataComponents.FOOD)) continue;
                String bar = classifyBakeryItemPath(loc.getPath());
                if (bar == null) continue;
                registerItem(loc.toString(), nutrientsForBarFromItem(item, bar));
                registerItemDietBar(loc.toString(), bar);
            }
        }
        if (ModList.get().isLoaded("wildernature")) {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
                if (loc == null || !"wildernature".equals(loc.getNamespace())) continue;
                if (!item.components().has(DataComponents.FOOD)) continue;
                String bar = classifyWilderNatureItemPath(loc.getPath());
                if (bar == null) continue;
                registerItem(loc.toString(), nutrientsForBarFromItem(item, bar));
                registerItemDietBar(loc.toString(), bar);
            }
        }
    }

    private static NutrientValues nutrientsForBarFromItem(Item item, String barKey) {
        FoodProperties fp = item.components().get(DataComponents.FOOD);
        float pts = fp != null
                ? Math.max(1.0f, fp.nutrition() + Math.max(0f, fp.saturation()) * 0.5f)
                : 4.0f;
        return nutrientValuesForBar(barKey, pts);
    }

    private static boolean pathContainsAny(String pathLower, String... needles) {
        for (String n : needles) {
            if (pathLower.contains(n)) return true;
        }
        return false;
    }

    /** Bakery (Farm&Charm compat): path-only rules; namespace must already be {@code bakery}. */
    private static String classifyBakeryItemPath(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (pathContainsAny(p, "cream", "custard", "pudding", "yogurt", "buttermilk") || "cheese_danish".equals(p)) return "dairy";
        if (pathContainsAny(p, "fruit")) return "fruits";
        if (pathContainsAny(p, "cake", "cookie", "donut", "doughnut", "muffin", "tart", "eclair", "macaron", "waffle", "pancake")) return "sugars";
        if (pathContainsAny(p, "bread", "baguette", "roll", "toast", "cracker", "pretzel", "croissant")) return "grains";
        return null;
    }

    /** WilderNature: path-only rules; namespace must already be {@code wildernature}. */
    private static String classifyWilderNatureItemPath(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (pathContainsAny(p, "honey", "donut", "doughnut")) return "sugars";
        if (pathContainsAny(p, "berry", "berries", "rosehip", "elder")) return "fruits";
        if (pathContainsAny(p, "mushroom", "fungus")) return "vegetables";
        if (pathContainsAny(p, "nut", "acorn", "seed")) return "grains";
        return null;
    }

    /**
     * Last-resort diet bar from item id path for non-vanilla namespaces when tags did not classify the item.
     */
    private static String classifyModItemPathPattern(ResourceLocation id) {
        if (id == null || "minecraft".equals(id.getNamespace())) return null;
        String p = id.getPath().toLowerCase(Locale.ROOT);
        if (pathContainsAny(p, "berry", "berries", "rosehip", "elder", "grape", "melon", "citrus")
                || pathContainsAny(p, "fruit")
                || (p.contains("apple") && (p.contains("strudel") || p.contains("pie") || p.contains("crumble")))) return "fruits";
        if (pathContainsAny(p, "mushroom", "fungus", "sauerkraut", "seaweed", "kelp", "lettuce", "spinach", "kale", "herb")) return "vegetables";
        if (pathContainsAny(p, "fish", "meat", "beef", "pork", "chicken", "mutton", "bacon", "sausage", "wurst", "ham", "steckerl", "hendl", "obatzda", "prawn", "shrimp", "turkey", "duck", "venison")) return "proteins";
        // Sugars before grains so "doughnut" does not match substring "nut"
        if (pathContainsAny(p, "cake", "cookie", "donut", "doughnut", "muffin", "tart", "eclair", "macaron", "waffle", "pancake", "honey", "jam", "syrup", "candy", "sweet", "sugar", "chocolate", "beer", "mead", "wine", "cider", "ale", "liquor", "soda", "juice")) return "sugars";
        if (pathContainsAny(p, "nut", "acorn", "seed", "bread", "baguette", "roll", "toast", "cracker", "pretzel", "croissant", "grain", "flour", "oat", "rice", "pasta", "noodle", "cereal")) return "grains";
        if (pathContainsAny(p, "milk", "cream", "cheese", "butter", "yogurt", "custard", "pudding", "curd")) return "dairy";
        return null;
    }

    // ── NutrientValues tag/item maps ──────────────────────────────────────────

    private static final Map<String, NutrientValues> TAG_MAP  = new LinkedHashMap<>();
    private static final Map<String, NutrientValues> ITEM_MAP = new LinkedHashMap<>();
    private static final Map<String, TagKey<Item>>   TAG_MAP_KEYS = new LinkedHashMap<>();

    static {
        registerItem("minecraft:cooked_beef",      new NutrientValues(15, 2,  5,  1,  0));
        registerItem("minecraft:cooked_porkchop",  new NutrientValues(14, 2,  6,  1,  0));
        registerItem("minecraft:cooked_chicken",   new NutrientValues(12, 2,  3,  2,  1));
        registerItem("minecraft:cooked_mutton",    new NutrientValues(13, 2,  4,  1,  0));
        registerItem("minecraft:cooked_rabbit",    new NutrientValues(11, 2,  2,  3,  1));
        registerItem("minecraft:cooked_cod",       new NutrientValues(10, 1,  2,  4,  3));
        registerItem("minecraft:cooked_salmon",    new NutrientValues(11, 1,  4,  4,  3));
        registerItem("minecraft:beef",             new NutrientValues(8,  1,  4,  0,  0));
        registerItem("minecraft:porkchop",         new NutrientValues(7,  1,  5,  0,  0));
        registerItem("minecraft:chicken",          new NutrientValues(6,  1,  2,  1,  0));
        registerItem("minecraft:mutton",           new NutrientValues(7,  1,  3,  0,  0));
        registerItem("minecraft:rabbit",           new NutrientValues(6,  1,  2,  2,  0));
        registerItem("minecraft:cod",              new NutrientValues(5,  0,  1,  3,  2));
        registerItem("minecraft:salmon",           new NutrientValues(6,  0,  3,  3,  2));
        registerItem("minecraft:egg",              new NutrientValues(8,  1,  5,  2,  0));
        registerItem("minecraft:bread",            new NutrientValues(3,  15, 2,  1,  0));
        registerItem("minecraft:baked_potato",     new NutrientValues(2,  12, 1,  4,  2));
        registerItem("minecraft:potato",           new NutrientValues(1,  8,  0,  3,  1));
        registerItem("minecraft:carrot",           new NutrientValues(0,  3,  0,  10, 3));
        registerItem("minecraft:golden_carrot",    new NutrientValues(0,  5,  0,  14, 5));
        registerItem("minecraft:pumpkin_pie",      new NutrientValues(2,  14, 4,  3,  1));
        registerItem("minecraft:cookie",           new NutrientValues(1,  10, 3,  0,  0));
        registerItem("minecraft:cake",             new NutrientValues(3,  12, 5,  1,  2));
        registerItem("minecraft:apple",            new NutrientValues(0,  6,  0,  10, 5));
        registerItem("minecraft:golden_apple",     new NutrientValues(0,  8,  0,  15, 8));
        registerItem("minecraft:melon_slice",      new NutrientValues(0,  4,  0,  8,  10));
        registerItem("minecraft:sweet_berries",    new NutrientValues(0,  3,  0,  10, 4));
        registerItem("minecraft:glow_berries",     new NutrientValues(0,  3,  0,  12, 4));
        registerItem("minecraft:beetroot",         new NutrientValues(0,  4,  0,  10, 2));
        registerItem("minecraft:mushroom_stew",    new NutrientValues(4,  6,  2,  8,  8));
        registerItem("minecraft:rabbit_stew",      new NutrientValues(10, 8,  4,  6,  6));
        registerItem("minecraft:beetroot_soup",    new NutrientValues(2,  5,  1,  12, 6));
        registerItem("minecraft:suspicious_stew",  new NutrientValues(2,  4,  1,  10, 5));
        registerItem("minecraft:honey_bottle",     new NutrientValues(0,  8,  0,  2,  12));
        registerItem("minecraft:rotten_flesh",     new NutrientValues(2,  0,  1,  0,  0));
        registerItem("minecraft:spider_eye",       new NutrientValues(1,  0,  0,  1,  0));
        registerItem("minecraft:poisonous_potato", new NutrientValues(1,  4,  0,  0,  0));
        registerItem("minecraft:sugar",            new NutrientValues(0,  12, 0,  0,  0));

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
        registerTag("c:foods/meat",                new NutrientValues(10, 2,  4,  2,  1));
        registerTag("c:foods/fish",                new NutrientValues(9,  1,  3,  4,  3));
        registerTag("c:foods/fish_food",           new NutrientValues(9,  1,  3,  4,  3));
        registerTag("c:crops",                     new NutrientValues(1,  8,  0,  5,  2));
        registerTag("c:crops/grain",               new NutrientValues(2,  12, 1,  2,  0));
        registerTag("c:crops/fruit",               new NutrientValues(0,  5,  0,  10, 6));
        registerTag("c:crops/vegetable",           new NutrientValues(1,  4,  0,  10, 3));
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

        // ── Explicit diet-bar assignments for common vanilla items ────────────

        registerItemDietBar("minecraft:bread",           "grains");
        registerItemDietBar("minecraft:baked_potato",    "grains");
        registerItemDietBar("minecraft:potato",          "grains");
        registerItemDietBar("minecraft:apple",           "fruits");
        registerItemDietBar("minecraft:golden_apple",    "fruits");
        registerItemDietBar("minecraft:melon_slice",     "fruits");
        registerItemDietBar("minecraft:sweet_berries",   "fruits");
        registerItemDietBar("minecraft:glow_berries",    "fruits");
        registerItemDietBar("minecraft:carrot",          "vegetables");
        registerItemDietBar("minecraft:golden_carrot",   "vegetables");
        registerItemDietBar("minecraft:beetroot",        "vegetables");
        registerItemDietBar("minecraft:mushroom_stew",   "vegetables");
        registerItemDietBar("minecraft:beetroot_soup",   "vegetables");
        registerItemDietBar("minecraft:beef",            "proteins");
        registerItemDietBar("minecraft:cooked_beef",     "proteins");
        registerItemDietBar("minecraft:porkchop",        "proteins");
        registerItemDietBar("minecraft:cooked_porkchop", "proteins");
        registerItemDietBar("minecraft:chicken",         "proteins");
        registerItemDietBar("minecraft:cooked_chicken",  "proteins");
        registerItemDietBar("minecraft:mutton",          "proteins");
        registerItemDietBar("minecraft:cooked_mutton",   "proteins");
        registerItemDietBar("minecraft:rabbit",          "proteins");
        registerItemDietBar("minecraft:cooked_rabbit",   "proteins");
        registerItemDietBar("minecraft:cod",             "proteins");
        registerItemDietBar("minecraft:cooked_cod",      "proteins");
        registerItemDietBar("minecraft:salmon",          "proteins");
        registerItemDietBar("minecraft:cooked_salmon",   "proteins");
        registerItemDietBar("minecraft:sugar",           "sugars");
        registerItemDietBar("minecraft:cake",            "sugars");
        registerItemDietBar("minecraft:pumpkin_pie",     "sugars");
        registerItemDietBar("minecraft:cookie",          "sugars");
        registerItemDietBar("minecraft:milk_bucket",     "dairy");
        registerItemDietBar("minecraft:egg",             "proteins");

        if (ModList.get().isLoaded("farm_and_charm")) {
            registerItem("farm_and_charm:tomato",           new NutrientValues(0,  3,  0,  10, 3));
            registerItem("farm_and_charm:lettuce",          new NutrientValues(0,  2,  0,  12, 4));
            registerItem("farm_and_charm:corn",            new NutrientValues(2,  10, 1,  3,  1));
            registerItem("farm_and_charm:oats",            new NutrientValues(3,  12, 1,  2,  0));
            registerItem("farm_and_charm:rye",             new NutrientValues(3,  12, 1,  2,  0));
            registerItem("farm_and_charm:tomato_soup",     new NutrientValues(3,  5,  1,  10, 7));
            registerItem("farm_and_charm:vegetable_soup",  new NutrientValues(3,  5,  1,  11, 7));
            registerItem("farm_and_charm:pancakes",        new NutrientValues(2,  12, 3,  1,  1));
            registerItem("farm_and_charm:bread_slice",     new NutrientValues(3,  12, 2,  1,  0));
            registerItem("farm_and_charm:toast",           new NutrientValues(3,  12, 2,  2,  0));
            registerItem("farm_and_charm:egg_and_bacon",   new NutrientValues(12, 2,  6,  2,  1));
            registerItem("farm_and_charm:roast_chicken",   new NutrientValues(12, 2,  3,  2,  1));
            registerItem("farm_and_charm:beef_stew",       new NutrientValues(10, 8,  4,  6,  6));
            registerItem("farm_and_charm:apple_pie",       new NutrientValues(2,  14, 4,  3,  1));
            registerItem("farm_and_charm:strawberry_jam",  new NutrientValues(0,  8,  0,  6,  4));
            registerItem("farm_and_charm:butter",          new NutrientValues(1,  0,  9,  0,  1));
            registerItem("farm_and_charm:cheese",          new NutrientValues(6,  1,  8,  2,  2));
            registerItem("farm_and_charm:milk_bottle",     new NutrientValues(5,  3,  6,  3,  4));

            registerItemDietBar("farm_and_charm:tomato",          "vegetables");
            registerItemDietBar("farm_and_charm:lettuce",         "vegetables");
            registerItemDietBar("farm_and_charm:corn",            "grains");
            registerItemDietBar("farm_and_charm:oats",            "grains");
            registerItemDietBar("farm_and_charm:rye",             "grains");
            registerItemDietBar("farm_and_charm:tomato_soup",     "vegetables");
            registerItemDietBar("farm_and_charm:vegetable_soup",  "vegetables");
            registerItemDietBar("farm_and_charm:pancakes",        "grains");
            registerItemDietBar("farm_and_charm:bread_slice",     "grains");
            registerItemDietBar("farm_and_charm:toast",           "grains");
            registerItemDietBar("farm_and_charm:egg_and_bacon",   "proteins");
            registerItemDietBar("farm_and_charm:roast_chicken",   "proteins");
            registerItemDietBar("farm_and_charm:beef_stew",       "proteins");
            registerItemDietBar("farm_and_charm:apple_pie",       "fruits");
            registerItemDietBar("farm_and_charm:strawberry_jam",  "fruits");
            registerItemDietBar("farm_and_charm:butter",          "dairy");
            registerItemDietBar("farm_and_charm:cheese",          "dairy");
            registerItemDietBar("farm_and_charm:milk_bottle",     "dairy");
        }

        if (ModList.get().isLoaded("brewery")) {
            registerItem("brewery:beer",             new NutrientValues(0,  6,  0,  1,  8));
            registerItem("brewery:wheat_beer",       new NutrientValues(0,  6,  0,  1,  8));
            registerItem("brewery:dark_beer",        new NutrientValues(0,  6,  0,  1,  8));
            registerItem("brewery:mead",             new NutrientValues(0,  10, 0,  0,  6));
            registerItem("brewery:pretzel",          new NutrientValues(3,  12, 2,  1,  0));
            registerItem("brewery:pretzel_stick",    new NutrientValues(2,  10, 1,  1,  0));
            registerItem("brewery:obatzda",          new NutrientValues(7,  5,  10, 2,  2));
            registerItem("brewery:weisswurst",       new NutrientValues(12, 2,  6,  1,  0));
            registerItem("brewery:hendl",            new NutrientValues(12, 2,  3,  2,  1));
            registerItem("brewery:steckerlfisch",    new NutrientValues(11, 1,  4,  4,  3));
            registerItem("brewery:kaiserschmarrn",   new NutrientValues(2,  12, 3,  1,  2));
            registerItem("brewery:apple_strudel",    new NutrientValues(1,  10, 3,  6,  2));
            registerItem("brewery:sauerkraut",       new NutrientValues(1,  4,  0,  8,  4));

            registerItemDietBar("brewery:beer",          "sugars");
            registerItemDietBar("brewery:wheat_beer",    "sugars");
            registerItemDietBar("brewery:dark_beer",     "sugars");
            registerItemDietBar("brewery:mead",          "sugars");
            registerItemDietBar("brewery:pretzel",       "grains");
            registerItemDietBar("brewery:pretzel_stick", "grains");
            registerItemDietBar("brewery:obatzda",       "proteins");
            registerItemDietBar("brewery:weisswurst",    "proteins");
            registerItemDietBar("brewery:hendl",         "proteins");
            registerItemDietBar("brewery:steckerlfisch", "proteins");
            registerItemDietBar("brewery:kaiserschmarrn", "sugars");
            registerItemDietBar("brewery:apple_strudel", "fruits");
            registerItemDietBar("brewery:sauerkraut",    "vegetables");
        }
    }

    // ── Public registration API ───────────────────────────────────────────────

    public static void registerItem(String itemId, NutrientValues values) {
        ITEM_MAP.put(itemId, values);
    }

    public static void registerTag(String tagId, NutrientValues values) {
        TAG_MAP.put(tagId, values);
        TAG_MAP_KEYS.put(tagId, tag(tagId));
    }

    public static void registerItemDietBar(String itemId, String dietBarKey) {
        ITEM_DIET_BAR.put(itemId, dietBarKey);
    }

    // ── Nutrient lookup ───────────────────────────────────────────────────────

    public static NutrientValues getNutrients(ItemStack stack, Level level) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && ITEM_MAP.containsKey(id.toString())) {
            return ITEM_MAP.get(id.toString());
        }
        // Use builtInRegistryHolder().is() for reliable tag resolution
        var holder = stack.getItem().builtInRegistryHolder();
        for (Map.Entry<String, NutrientValues> entry : TAG_MAP.entrySet()) {
            TagKey<Item> tagKey = TAG_MAP_KEYS.get(entry.getKey());
            if (tagKey != null && holder.is(tagKey)) return entry.getValue();
        }
        return deriveFallbackNutrients(stack, level);
    }

    // ── Fallback nutrient derivation ──────────────────────────────────────────

    /**
     * Fallback for items not in ITEM_MAP or TAG_MAP.
     * Checks food-category tags via builtInRegistryHolder first; heuristic is last resort only.
     */
    private static NutrientValues deriveFallbackNutrients(ItemStack stack, Level level) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        float nutrition  = food.nutrition();
        float totalPoints = Math.max(1.0f, nutrition + Math.max(0f, food.saturation()) * 0.5f);

        // Debug: log what tags this item actually carries so misclassifications can be diagnosed
        if (LOGGER.isDebugEnabled()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            var tagList = stack.getItem().builtInRegistryHolder().tags()
                    .map(t -> t.location().toString())
                    .toList();
            LOGGER.debug("Nourished fallback for {}: tags={}", itemId, tagList);
        }

        // Tag-first: classify by food-category tag, return NutrientValues biased 100% to that bar
        String barKey = resolveFoodTagDietBar(stack);
        if (barKey != null) {
            return nutrientValuesForBar(barKey, totalPoints);
        }

        ResourceLocation itemLoc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemLoc != null) {
            String pathBar = classifyModItemPathPattern(itemLoc);
            if (pathBar != null) {
                return nutrientValuesForBar(pathBar, totalPoints);
            }
        }

        // Heuristic last resort — only for items with no food category tags at all
        float saturation = Math.max(0f, food.saturation());

        float fruitsW     = 0.12f;
        float vegetablesW = 0.12f;
        float proteinsW   = 0.22f;
        float grainsW     = 0.22f;
        float sugarsW     = 0.20f;
        float dairyW      = 0.12f;

        if (saturation > 0.5f) {
            float b = Math.min(0.35f, (saturation - 0.5f) * 0.5f);
            proteinsW += b * 0.55f; grainsW += b * 0.45f; dairyW += b * 0.30f; sugarsW -= b * 0.85f;
        } else if (saturation < 0.3f) {
            float b = Math.min(0.35f, (0.3f - saturation) * 1.2f);
            sugarsW += b; proteinsW -= b * 0.50f; grainsW -= b * 0.35f; dairyW -= b * 0.15f;
        }

        fruitsW     = Math.max(0.02f, fruitsW);
        vegetablesW = Math.max(0.02f, vegetablesW);
        proteinsW   = Math.max(0.02f, proteinsW);
        grainsW     = Math.max(0.02f, grainsW);
        sugarsW     = Math.max(0.02f, sugarsW);
        dairyW      = Math.max(0.02f, dairyW);

        float ws      = fruitsW + vegetablesW + proteinsW + grainsW + sugarsW + dairyW;
        float fruits  = totalPoints * (fruitsW     / ws);
        float vegs    = totalPoints * (vegetablesW / ws);
        float prots   = totalPoints * (proteinsW   / ws);
        float grains  = totalPoints * (grainsW     / ws);
        float sugars  = totalPoints * (sugarsW     / ws);
        float dairy   = totalPoints * (dairyW      / ws);

        return new NutrientValues(
                Mth.clamp(prots  + dairy  * 0.35f,                              0.1f, 20.0f),
                Mth.clamp(grains + sugars * 0.60f + fruits * 0.20f,             0.1f, 20.0f),
                Mth.clamp(dairy  * 0.45f  + sugars * 0.30f + prots * 0.15f,    0.1f, 20.0f),
                Mth.clamp(vegs   + fruits * 0.70f,                              0.1f, 20.0f),
                Mth.clamp(fruits * 0.60f  + vegs   * 0.35f + dairy * 0.15f,    0.1f, 20.0f)
        );
    }

    /** NutrientValues with 100% weight on the primary macro for the given diet bar. */
    private static NutrientValues nutrientValuesForBar(String barKey, float pts) {
        return switch (barKey) {
            case "fruits"     -> new NutrientValues(0,           pts * 0.4f,  0,           pts * 0.8f, pts);
            case "vegetables" -> new NutrientValues(pts * 0.1f, pts * 0.35f, 0,           pts,        pts * 0.2f);
            case "proteins"   -> new NutrientValues(pts,         pts * 0.1f,  pts * 0.3f, 0,          0);
            case "grains"     -> new NutrientValues(pts * 0.2f, pts,          pts * 0.5f, pts * 0.1f, 0);
            case "sugars"     -> new NutrientValues(0,           pts * 0.8f,  pts * 0.15f,0,          0);
            case "dairy"      -> new NutrientValues(pts * 0.5f, 0,            pts * 0.7f, 0,          pts * 0.3f);
            default           -> new NutrientValues(pts * 0.2f, pts,          pts * 0.1f, pts * 0.1f, 0);
        };
    }

    /**
     * Checks food-category tags using builtInRegistryHolder().is() with pre-created TagKey constants.
     * Priority: fruits (incl. fruit crops), vegetables (incl. veggie crops), proteins, grains, sugars, dairy.
     * Returns null only if the item carries none of the known food category tags.
     */
    private static String resolveFoodTagDietBar(ItemStack stack) {
        var holder = stack.getItem().builtInRegistryHolder();
        // 1. Fruits
        if (holder.is(TAG_FRUITS))       return "fruits";
        if (holder.is(TAG_FRUIT_CROPS))  return "fruits";
        if (holder.is(TAG_BERRIES))      return "fruits";
        if (holder.is(TAG_JUICE))        return "fruits";
        // 2. Vegetables
        if (holder.is(TAG_VEGETABLES))   return "vegetables";
        if (holder.is(TAG_VEGGIE_CROPS)) return "vegetables";
        if (holder.is(TAG_SALAD))        return "vegetables";
        // 3. Proteins
        if (holder.is(TAG_COOKED_MEAT))  return "proteins";
        if (holder.is(TAG_RAW_MEAT))     return "proteins";
        if (holder.is(TAG_PROTEINS))     return "proteins";
        if (holder.is(TAG_PROTEINS_MEAT)) return "proteins";
        if (holder.is(TAG_PROTEINS_FISH)) return "proteins";
        if (holder.is(TAG_FISH_FOOD))    return "proteins";
        if (holder.is(TAG_COOKED_FISH))  return "proteins";
        if (holder.is(TAG_RAW_FISH))     return "proteins";
        if (holder.is(TAG_EGGS))         return "proteins";
        // 4. Grains
        if (holder.is(TAG_GRAINS))       return "grains";
        if (holder.is(TAG_GRAIN))        return "grains";
        if (holder.is(TAG_GRAIN_CROPS))  return "grains";
        if (holder.is(TAG_FOODS_CROPS))  return "grains";
        if (holder.is(TAG_CROPS))        return "grains"; // c:crops root (subtype tags win above)
        if (holder.is(TAG_PASTA))        return "grains";
        // 5. Sugars
        if (holder.is(TAG_SUGARS))       return "sugars";
        if (holder.is(TAG_CANDY))        return "sugars";
        if (holder.is(TAG_COOKIES))      return "sugars";
        if (holder.is(TAG_CAKE))         return "sugars";
        if (holder.is(TAG_PIE))          return "sugars";
        // 6. Dairy
        if (holder.is(TAG_DAIRY))        return "dairy";
        return null;
    }

    // ── Diet delta computation ────────────────────────────────────────────────

    public static DietDelta computeDietDelta(ItemStack stack, Level level, NutrientValues values, int foodNutrition, float foodSaturation) {
        Objects.requireNonNull(level, "level");
        float calories = foodNutrition * 18f + foodSaturation * 12f + values.carbs() * 0.8f;
        String primary = resolveDietBarKey(stack, level, values);
        float burst    = foodNutrition * 0.022f + foodSaturation * 0.03f + 0.012f;

        float fruits     = values.hydration() * 0.004f + values.vitamins() * 0.002f;
        float vegetables = values.vitamins()  * 0.006f;
        float proteins   = values.protein()   * 0.008f;
        float grains     = values.carbs()     * 0.005f;
        float sugars     = values.fats()      * 0.003f + values.carbs() * 0.004f;
        float dairy      = values.fats()      * 0.004f + values.protein() * 0.002f;

        switch (primary) {
            case "fruits"     -> fruits     += burst;
            case "vegetables" -> vegetables += burst;
            case "proteins"   -> proteins   += burst;
            case "grains"     -> grains     += burst;
            case "sugars"     -> sugars     += burst;
            case "dairy"      -> dairy      += burst;
            default           -> grains     += burst * 0.35f;
        }

        return new DietDelta(calories, fruits, vegetables, proteins, grains, sugars, dairy);
    }

    private static String resolveDietBarKey(ItemStack stack, Level level, NutrientValues values) {
        // 1. Explicit per-item override
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            String explicit = ITEM_DIET_BAR.get(id.toString());
            if (explicit != null) return explicit;
        }

        // 2. NutrientRegistry tag map (populated from nutrients.json) — builtInRegistryHolder lookup
        var holder = stack.getItem().builtInRegistryHolder();
        for (Map.Entry<String, String> entry : TAG_DIET_BAR.entrySet()) {
            TagKey<Item> tagKey = TAG_DIET_BAR_KEYS.get(entry.getKey());
            if (tagKey != null && holder.is(tagKey)) return entry.getValue();
        }

        // 3. Broad food-category tags (pre-created constants, builtInRegistryHolder)
        String tagBased = resolveFoodTagDietBar(stack);
        if (tagBased != null) return tagBased;

        // 3b. Mod id path keyword patterns (non-vanilla namespaces), before macro heuristic
        if (id != null) {
            String pathBar = classifyModItemPathPattern(id);
            if (pathBar != null) return pathBar;
        }

        // 4. Heuristic — only for items with zero food category tags
        float p = values.protein();
        float c = values.carbs();
        float v = values.vitamins();
        float h = values.hydration();
        float maxMacro = Math.max(p, Math.max(c, v));
        if (maxMacro == p && p >= c * 1.1f) return "proteins";
        if (maxMacro == v && h >= c * 0.4f) return "fruits";
        if (maxMacro == v) return "vegetables";
        if (c >= p && c >= v) return values.fats() > 4f ? "grains" : "sugars";
        return "grains";
    }
}
