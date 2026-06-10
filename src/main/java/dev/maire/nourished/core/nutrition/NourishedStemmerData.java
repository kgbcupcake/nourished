package dev.maire.nourished.core.nutrition;

import dev.marie.MariesLib.api.ApiStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public final class NourishedStemmerData {

    private static final Map<String, String> STEMMER_IRREGULAR_FORMS = Map.ofEntries(
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

    private static final Map<String, String[]> STEMMER_COMPOUND_SPLITS = Map.of(
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

    private static final Set<String> STEMMER_STOP_WORDS = Set.of(
            "with", "and", "of", "the", "in", "on", "a", "an",
            "raw", "fresh", "dried", "wild", "organic", "natural",
            "small", "large", "big", "mini", "mega", "super",
            "hot", "cold", "warm", "cool", "frozen",
            "old", "new", "young", "aged",
            "light", "dark", "deep", "extra",
            "plain", "simple", "basic", "classic"
    );

    private static String[] dictionaryCache;

    private NourishedStemmerData() {}

    public static String[] dictionary() {
        if (dictionaryCache == null) {
            dictionaryCache = buildStemmerDictionary();
        }
        return dictionaryCache;
    }

    public static Map<String, String[]> compoundSplits() {
        return STEMMER_COMPOUND_SPLITS;
    }

    public static Map<String, String> irregularForms() {
        return STEMMER_IRREGULAR_FORMS;
    }

    public static Set<String> stopWords() {
        return STEMMER_STOP_WORDS;
    }

    private static String[] buildStemmerDictionary() {
        List<String> words = new ArrayList<>(List.of(
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
                "melon", "milk", "mint", "muffin", "mushroom", "mussel", "mustard",
                "mutton", "noodle", "nut", "oat", "omelet", "onion", "orange",
                "oregano", "oyster", "paella", "pancake", "paprika", "parmesan", "parsley", "pasta",
                "pastry", "pea", "peach", "peanut", "pear", "pepper", "perch", "pickle", "pickled",
                "pie", "pilaf", "pineapple", "pistachio", "pizza", "plum", "poach", "poached",
                "polenta", "popcorn", "poppy", "popsicle", "pork", "porridge", "potato", "poutine",
                "praline", "prawn", "pretzel", "prosciutto", "pudding", "pumpkin", "quinoa", "radish",
                "raisin", "ramen", "raspberry", "ravioli", "rhubarb", "rib", "ribs", "rice", "ricotta",
                "roast", "roasted", "roll", "romaine", "rose", "saffron", "salad", "salami", "salmon",
                "salsa", "salt", "sandwich", "sardine", "sauce", "sausage", "scallop", "scone",
                "scramble", "scrambled", "seabass", "seed", "sesame", "shallot", "shell", "shrimp",
                "slice", "sliced", "smoke", "smoked", "smoothie", "snack", "soup", "sour", "soy",
                "spaghetti", "spinach", "steak", "steam", "steamed", "stew", "stir", "strawberry",
                "sugar", "sushi", "sweet", "syrup", "taco", "tart", "tempeh", "teriyaki", "thigh",
                "toast", "toffee", "tofu", "tomato", "tortilla", "trail", "truffle", "tuna", "turkey",
                "turnip", "vanilla", "veal", "venison", "vinegar", "waffle", "walnut", "wasabi",
                "watermelon", "wheat", "whip", "whiskey", "wing", "wings", "wrap", "yam", "yeast",
                "yogurt", "zest", "zucchini",
                // from IRREGULAR_FORMS keys and values
                "fry", "chip", "goose", "tooth", "mouse", "ox", "cactus", "fungus", "alum",
                "larva", "beef", "venison", "mutton", "poultry", "pork", "rice", "wheat", "oat", "pea",
                // from COMPOUND_SPLITS parts
                "crumb", "loaf", "dough", "butter", "ginger", "hot"
        ));
        words.removeIf(w -> w.length() < 3);
        words.sort(Comparator.comparingInt(String::length).reversed().thenComparing(w -> w));
        return words.toArray(String[]::new);
    }
}
