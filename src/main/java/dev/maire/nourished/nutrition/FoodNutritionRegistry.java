package dev.maire.nourished.nutrition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.config.NourishedConfig;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Food nutrient values and diet-bar classification are driven only by datapack item tags under
 * {@code data/nourished/tags/item/nutrients/} (see {@code nourished:nutrients/*}).
 */
public class FoodNutritionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> WARNED_ITEMS = new HashSet<>();

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    /** Diet UI deltas; nutrient values are driven by NutrientRegistry keys. */
    public record DietDelta(float calories, Map<String, Float> nutrients) {}

    /**
     * Called after {@link NutrientRegistry#load()} (and on reload). Classification uses only datapack tags;
     * nothing is rebuilt here.
     */
    public static void init() {
        // Intentionally empty — kept for API compatibility with {@link NutrientRegistry#reload()}.
    }

    /**
     * Resolves all matching diet nutrient keys for an item stack using {@code nourished:nutrients/*} tags.
     * Iterates over NutrientRegistry.getAll() dynamically.
     * If none match, defaults to first registered nutrient.
     *
     * @param warnIfUnmatched when true, logs a WARN for modpack authors when defaulting
     * @return map of nutrient bar key -> match weight
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched) {
        Item item = stack.getItem();
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = stack.getItemHolder();

        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagStr));
                if (holder.is(tagKey)) {
                    matches.put(def.key(), 1.0f);
                    break;
                }
            }
        }

        matches.entrySet().removeIf(entry -> {
            String namespace = itemId.getNamespace();
            String modid = ModCompat.namespaceToModid(namespace);
            return modid != null && !NourishedConfig.get().isTagCompatEnabled(modid);
        });

        if (!matches.isEmpty()) {
            return matches;
        }

        if (warnIfUnmatched) {
            String id = item.getDescriptionId();
            if (WARNED_ITEMS.add(id)) {
                LOGGER.warn(
                        "Nourished: no nutrient tag for {} — attempting name-based guess. Add it to data/nourished/tags/item/nutrients/*.json for accurate classification.",
                        id);
            }
        }

        // Attempt name-based guess from item registry ID before hard fallback
        String itemPath = itemId.getPath();
        String guessed = guessNutrientFromId(itemPath);
        List<String> keys = NutrientRegistry.getKeys();
        if (guessed != null && keys.contains(guessed)) {
            matches.put(guessed, 1.0f);
            return matches;
        }

        // Hard fallback to first registered nutrient
        String defaultKey = keys.stream().findFirst().orElse("grains");
        matches.put(defaultKey, 1.0f);
        return matches;
    }

    /**
     * @param silent when {@code true}, skips {@link #resolvePrimaryNutrientBar} unmatched-item warnings (use on client
     *               tooltips / JEI where tags may not be committed yet).
     */
    public static NutrientValues getNutrients(ItemStack stack, Level level, boolean silent) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        String bar = resolvePrimaryNutrientBar(stack, !silent);
        float totalPoints = Math.max(1.0f, food.nutrition() + Math.max(0f, food.saturation()) * 0.5f);
        return nutrientValuesForBar(bar, totalPoints);
    }

    /** Same as {@link #getNutrients(ItemStack, Level, boolean)} with {@code silent == false}. */
    public static NutrientValues getNutrients(ItemStack stack, Level level) {
        return getNutrients(stack, level, false);
    }

    /** NutrientValues with primary macro weighting for the given diet bar, driven by FoodValueRegistry. */
    private static NutrientValues nutrientValuesForBar(String barKey, float pts) {
        return FoodValueRegistry.getValuesForCategory(barKey, pts);
    }

    public static DietDelta computeDietDelta(
            ItemStack stack,
            Level level,
            int foodNutrition,
            float foodSaturation,
            Map<String, Float> matchedBars) {
        Objects.requireNonNull(level, "level");
        int calories = Math.max(0, Math.round(foodNutrition * 25f));
        Objects.requireNonNull(matchedBars, "matchedBars");

        float matchedWeightTotal = 0f;
        for (float weight : matchedBars.values()) {
            matchedWeightTotal += Math.max(0f, weight);
        }
        String defaultKey = NutrientRegistry.getKeys().stream().findFirst().orElse("grains");
        if (matchedWeightTotal <= 0f) {
            matchedBars = Map.of(defaultKey, 1.0f);
            matchedWeightTotal = 1.0f;
        }

        float burst = foodNutrition * 0.003f + foodSaturation * 0.004f + 0.001f;
        float totalBurst = burst * matchedWeightTotal;

        Map<String, Float> nutrients = new HashMap<>();
        List<String> keys = NutrientRegistry.getKeys();

        for (String key : keys) {
            nutrients.put(key, 0f);
        }

        for (Map.Entry<String, Float> entry : matchedBars.entrySet()) {
            float weight = Math.max(0f, entry.getValue());
            if (weight <= 0f) {
                continue;
            }
            float contribution = totalBurst * (weight / matchedWeightTotal);
            String key = entry.getKey();
            if (nutrients.containsKey(key)) {
                nutrients.put(key, nutrients.get(key) + contribution);
            }
        }

        final float scale = 10f;
        Map<String, Float> scaledNutrients = new HashMap<>();
        for (Map.Entry<String, Float> e : nutrients.entrySet()) {
            scaledNutrients.put(e.getKey(), e.getValue() * scale);
        }

        return new DietDelta(calories, scaledNutrients);
    }

    public static DietDelta computeDietDelta(ItemStack stack, Level level, int foodNutrition, float foodSaturation) {
        return computeDietDelta(
                stack,
                level,
                foodNutrition,
                foodSaturation,
                resolveNutrientBars(stack, false));
    }

    private static String resolvePrimaryNutrientBar(ItemStack stack, boolean warnIfUnmatched) {
        Map<String, Float> bars = resolveNutrientBars(stack, warnIfUnmatched);
        if (!bars.isEmpty()) return bars.keySet().iterator().next();
        return NutrientRegistry.getKeys().stream().findFirst().orElse("grains");
    }

    private static String guessNutrientFromId(String itemId) {
        String id = itemId.toLowerCase();

        if (containsAny(
                id,
                "steak",
                "beef",
                "pork",
                "chicken",
                "mutton",
                "rabbit",
                "fish",
                "salmon",
                "cod",
                "meat",
                "sausage",
                "bacon",
                "ham",
                "egg",
                "shrimp",
                "crab",
                "lobster",
                "turkey",
                "lamb",
                "venison",
                "tuna",
                "anchovy",
                "calamari",
                "clam",
                "oyster",
                "roe",
                "frog",
                "shulker",
                "dragon",
                "enderman",
                "nugget",
                "jerky",
                "meatball",
                "meatloaf",
                "patty",
                "burger",
                "hotdog",
                "bun_meat")) {
            return "proteins";
        }

        if (containsAny(
                id,
                "apple",
                "berry",
                "berries",
                "fruit",
                "juice",
                "cherry",
                "mango",
                "banana",
                "orange",
                "lemon",
                "lime",
                "grape",
                "peach",
                "pear",
                "plum",
                "apricot",
                "melon",
                "pineapple",
                "coconut",
                "avocado",
                "fig",
                "date",
                "papaya",
                "guava",
                "lychee",
                "rambutan",
                "passionfruit",
                "dragonfruit",
                "starfruit",
                "pomegranate",
                "tamarind",
                "gooseberry",
                "blueberry",
                "strawberry",
                "raspberry",
                "blackberry",
                "cranberry",
                "elderberry",
                "mulberry",
                "acorn",
                "chestnut",
                "walnut",
                "hazelnut",
                "almond",
                "cashew",
                "pecan",
                "pistachio",
                "pinenut",
                "smoothie",
                "cider",
                "lemonade")) {
            return "fruits";
        }

        if (containsAny(
                id,
                "carrot",
                "potato",
                "cabbage",
                "salad",
                "vegetable",
                "veggie",
                "onion",
                "tomato",
                "pepper",
                "broccoli",
                "cauliflower",
                "celery",
                "cucumber",
                "lettuce",
                "spinach",
                "kale",
                "radish",
                "turnip",
                "parsnip",
                "leek",
                "artichoke",
                "asparagus",
                "eggplant",
                "zucchini",
                "squash",
                "pumpkin",
                "beet",
                "beetroot",
                "yam",
                "mushroom",
                "truffle",
                "seaweed",
                "kelp",
                "nori",
                "herb",
                "basil",
                "oregano",
                "thyme",
                "rosemary",
                "sage",
                "mint",
                "garlic",
                "ginger",
                "ratatouille",
                "stew_veg",
                "mixed_greens")) {
            return "vegetables";
        }

        if (containsAny(
                id,
                "bread",
                "grain",
                "wheat",
                "rice",
                "pasta",
                "noodle",
                "sandwich",
                "toast",
                "cracker",
                "pretzel",
                "bagel",
                "muffin",
                "biscuit",
                "scone",
                "waffle",
                "pancake",
                "crepe",
                "dumpling",
                "wrap",
                "tortilla",
                "taco",
                "burrito",
                "pizza",
                "calzone",
                "dough",
                "flour",
                "oat",
                "barley",
                "rye",
                "corn",
                "maize",
                "cereal",
                "granola",
                "crouton",
                "stuffing",
                "roll_bread")) {
            return "grains";
        }

        if (containsAny(
                id,
                "sugar",
                "candy",
                "chocolate",
                "cake",
                "cookie",
                "pie",
                "tart",
                "dessert",
                "sweet",
                "fudge",
                "toffee",
                "caramel",
                "marshmallow",
                "gelatin",
                "pudding",
                "custard",
                "ice_cream",
                "icecream",
                "popsicle",
                "sorbet",
                "brownie",
                "donut",
                "doughnut",
                "eclair",
                "macaron",
                "truffle_sweet",
                "bonbon",
                "lollipop",
                "gummy",
                "jelly",
                "jam",
                "honey",
                "syrup",
                "frosting",
                "glaze",
                "sprinkle",
                "wafer")) {
            return "sugars";
        }

        if (containsAny(
                id,
                "milk",
                "cheese",
                "butter",
                "cream",
                "yogurt",
                "dairy",
                "whey",
                "curd",
                "brie",
                "cheddar",
                "gouda",
                "mozzarella",
                "parmesan",
                "ricotta",
                "cottage",
                "milkshake",
                "latte",
                "kefir")) {
            return "dairy";
        }

        return null;
    }

    private static boolean containsAny(String id, String... keywords) {
        // Pass 1: strip trailing "item" suffix (Pam's HC convention) and check contains
        String stripped = id.replaceAll("item$", "");
        for (String keyword : keywords) {
            if (stripped.contains(keyword)) return true;
        }

        // Pass 2: for underscore-separated IDs, check each token exactly
        // This prevents short keywords matching inside longer unrelated tokens
        // e.g. "mite" inside "creamite" on non-Pam items
        String[] tokens = id.split("_");
        if (tokens.length > 1) {
            for (String token : tokens) {
                String cleaned = token.replaceAll("item$", "");
                for (String keyword : keywords) {
                    if (cleaned.equals(keyword) || cleaned.startsWith(keyword)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
