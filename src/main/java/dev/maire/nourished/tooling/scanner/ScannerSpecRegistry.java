package dev.maire.nourished.tooling.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.registry.AbstractRegistry;
import dev.maire.nourished.core.util.NourishedJsonUtils;
import dev.maire.nourished.core.util.NourishedResourceLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the scanner classification spec from JSON.
 *
 * <p><b>Priority / Override Stack (lowest to highest):</b></p>
 * <ol>
 *   <li>Bundled defaults at {@code data/nourished/nourished/scanner/scanner_spec.json}</li>
 *   <li>{@code config/nourished/scanner_spec.json} (modpack creator override)</li>
 *   <li>{@code data/<ns>/nourished/scanner/scanner_spec.json} (datapack override)</li>
 * </ol>
 *
 * <p>The spec contains all signal multipliers, weight maps, archetype patterns, and
 * runtime food-property heuristics used by {@link FoodClassifier}. Nothing in the
 * scanner pipeline is hardcoded in Java.</p>
 */
@ApiStatus.Internal
public final class ScannerSpecRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUNDLED_RESOURCE_PATH = "/data/nourished/nourished/scanner/scanner_spec.json";
    private static final String CONFIG_FILE_NAME = "scanner_spec.json";
    private static final String SPEC_KEY = "active";

    private static final class Core extends AbstractRegistry<String, ScannerSpec> {
        Core() {
            super("ScannerSpecRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ScannerSpecRegistry() {}

    public static ScannerSpec get() {
        ScannerSpec spec = INSTANCE.get(SPEC_KEY);
        return spec != null ? spec : ScannerSpec.empty();
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve(CONFIG_FILE_NAME);
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeBundledTo(file);
                Nourished.LOGGER.info("[ScannerSpecRegistry] Wrote default scanner_spec.json");
            }
            ScannerSpec spec = parseFile(file);
            if (spec == null) {
                Nourished.LOGGER.warn("[ScannerSpecRegistry] scanner_spec.json was empty/invalid, falling back to bundled defaults");
                spec = parseBundled();
            }
            if (spec == null) {
                spec = ScannerSpec.empty();
            }
            INSTANCE.reset();
            INSTANCE.register(SPEC_KEY, spec);
            INSTANCE.freeze();
            Nourished.LOGGER.info("[ScannerSpecRegistry] Loaded scanner spec from {}", file);
        } catch (IOException e) {
            Nourished.LOGGER.error("[ScannerSpecRegistry] Failed to load scanner_spec.json, using bundled defaults", e);
            ScannerSpec bundled = parseBundled();
            INSTANCE.reset();
            INSTANCE.register(SPEC_KEY, bundled != null ? bundled : ScannerSpec.empty());
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[ScannerSpecRegistry] Reloading scanner_spec.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        NourishedResourceLoader.loadFromModConfig(
                resourceManager,
                "scanner/scanner_spec.json",
                reader -> {
                    ScannerSpec spec = parseReader(reader);
                    if (spec == null) {
                        return false;
                    }
                    INSTANCE.reset();
                    INSTANCE.register(SPEC_KEY, spec);
                    INSTANCE.freeze();
                    return true;
                },
                ScannerSpecRegistry::load,
                "[ScannerSpecRegistry] Loaded scanner_spec.json from datapack override",
                "[ScannerSpecRegistry] Failed to load datapack override, falling back to config folder",
                null
        );
    }

    private static ScannerSpec parseFile(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            return parseReader(r);
        }
    }

    private static ScannerSpec parseBundled() {
        try (InputStream in = ScannerSpecRegistry.class.getResourceAsStream(BUNDLED_RESOURCE_PATH)) {
            if (in == null) return null;
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parseReader(r);
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[ScannerSpecRegistry] Failed to read bundled scanner_spec.json", e);
            return null;
        }
    }

    private static void writeBundledTo(Path file) throws IOException {
        try (InputStream in = ScannerSpecRegistry.class.getResourceAsStream(BUNDLED_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Bundled scanner_spec.json missing at " + BUNDLED_RESOURCE_PATH);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 Writer writer = Files.newBufferedWriter(file)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                if (obj == null) {
                    throw new IOException("Bundled scanner_spec.json was empty/invalid");
                }
                GSON.toJson(obj, writer);
            }
        }
    }

    private static ScannerSpec parseReader(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) return null;

        Multipliers mult = parseMultipliers(getObj(root, "multipliers"));
        FoodPropertyHeuristics heur = parseHeuristics(getObj(root, "food_property_heuristics"));
        Map<String, Map<String, Float>> communityTags = parseStringFloatMap(getObj(root, "community_tags"));
        Map<String, Map<String, Float>> namespaces = parseStringFloatMap(getObj(root, "namespaces"));
        Map<String, Map<String, Float>> suffixes = parseStringFloatMap(getObj(root, "suffixes"));
        Map<String, Map<String, Float>> keywords = FoodTokenStemmer.stemMapKeys(parseStringFloatMap(getObj(root, "keywords")));
        Map<String, Map<String, Float>> negatives = FoodTokenStemmer.stemMapKeys(parseStringFloatMap(getObj(root, "negative_keywords")));
        List<ArchetypePattern> archetypes = parseArchetypes(getArr(root, "archetypes"));
        Set<String> excludedItems = parseStringSet(getArr(root, "excluded_items"));

        return new ScannerSpec(mult, heur, communityTags, namespaces, suffixes, keywords, negatives, archetypes, excludedItems);
    }

    private static Multipliers parseMultipliers(JsonObject obj) {
        if (obj == null) return Multipliers.defaults();
        return new Multipliers(
                getFloat(obj, "community_tag", 5.0f),
                getFloat(obj, "namespace", 4.0f),
                getFloat(obj, "suffix", 3.0f),
                getFloat(obj, "keyword", 2.0f),
                getFloat(obj, "archetype", 2.0f),
                getFloat(obj, "food_properties", 1.0f),
                getFloat(obj, "recipe_inheritance", 1.0f),
                getFloat(obj, "namespace_peer", 0.5f),
                getFloat(obj, "secondary_suffix", 0.5f),
                getFloat(obj, "namespace_peer_average_weight", 0.5f)
        );
    }

    private static FoodPropertyHeuristics parseHeuristics(JsonObject obj) {
        if (obj == null) return FoodPropertyHeuristics.defaults();

        JsonObject saturating = getObj(obj, "saturating_meal");
        SaturatingMealRule saturatingRule = saturating != null
                ? new SaturatingMealRule(
                        getFloat(saturating, "min_saturation", 1.2f),
                        getInt(saturating, "min_nutrition", 6),
                        parseFlatFloatMap(getObj(saturating, "contributions")))
                : SaturatingMealRule.empty();

        JsonObject snack = getObj(obj, "light_snack");
        LightSnackRule snackRule = snack != null
                ? new LightSnackRule(
                        getInt(snack, "max_nutrition", 2),
                        parseFlatFloatMap(getObj(snack, "contributions")))
                : LightSnackRule.empty();

        List<String> badEffects = new ArrayList<>();
        JsonArray badArr = getArr(obj, "bad_effect_keywords");
        if (badArr != null) {
            for (JsonElement el : badArr) {
                if (el != null && el.isJsonPrimitive()) {
                    badEffects.add(el.getAsString().toLowerCase());
                }
            }
        }
        float badMult = getFloat(obj, "bad_effect_multiplier", 0.5f);
        return new FoodPropertyHeuristics(saturatingRule, snackRule, Collections.unmodifiableList(badEffects), badMult);
    }

    private static Map<String, Map<String, Float>> parseStringFloatMap(JsonObject obj) {
        if (obj == null) return Map.of();
        Map<String, Map<String, Float>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            Map<String, Float> inner = parseFlatFloatMap(e.getValue().getAsJsonObject());
            if (!inner.isEmpty()) {
                result.put(e.getKey(), inner);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Float> parseFlatFloatMap(JsonObject obj) {
        if (obj == null) return Map.of();
        Map<String, Float> inner = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e2 : obj.entrySet()) {
            if (e2.getValue().isJsonPrimitive()) {
                inner.put(e2.getKey(), e2.getValue().getAsFloat());
            }
        }
        return Collections.unmodifiableMap(inner);
    }

    private static List<ArchetypePattern> parseArchetypes(JsonArray arr) {
        if (arr == null) return List.of();
        List<ArchetypePattern> out = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!o.has("pattern")) continue;
            String pattern = o.get("pattern").getAsString();
            Map<String, Float> contribs = parseFlatFloatMap(getObj(o, "contributions"));
            out.add(new ArchetypePattern(pattern, contribs));
        }
        return Collections.unmodifiableList(out);
    }

    private static Set<String> parseStringSet(JsonArray arr) {
        if (arr == null) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        for (JsonElement el : arr) {
            if (el != null && el.isJsonPrimitive()) {
                out.add(el.getAsString());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static JsonObject getObj(JsonObject parent, String key) {
        if (parent == null || !parent.has(key)) return null;
        JsonElement el = parent.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private static JsonArray getArr(JsonObject parent, String key) {
        if (parent == null || !parent.has(key)) return null;
        JsonElement el = parent.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
    }

    private static float getFloat(JsonObject obj, String key, float fallback) {
        if (obj == null) {
            return fallback;
        }
        return NourishedJsonUtils.getOptionalFloat(obj, key, fallback);
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        if (obj == null) {
            return fallback;
        }
        return NourishedJsonUtils.getOptionalInt(obj, key, fallback);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spec data classes
    // ─────────────────────────────────────────────────────────────────────────

    public record ScannerSpec(
            Multipliers multipliers,
            FoodPropertyHeuristics foodPropertyHeuristics,
            Map<String, Map<String, Float>> communityTagWeights,
            Map<String, Map<String, Float>> namespaceWeights,
            Map<String, Map<String, Float>> suffixWeights,
            Map<String, Map<String, Float>> keywordWeights,
            Map<String, Map<String, Float>> negativeKeywords,
            List<ArchetypePattern> archetypes,
            Set<String> excludedItems
    ) {
        public static ScannerSpec empty() {
            return new ScannerSpec(
                    Multipliers.defaults(),
                    FoodPropertyHeuristics.defaults(),
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    List.of(),
                    Set.of()
            );
        }
    }

    public record Multipliers(
            float communityTag,
            float namespace,
            float suffix,
            float keyword,
            float archetype,
            float foodProperties,
            float recipeInheritance,
            float namespacePeer,
            float secondarySuffix,
            float namespacePeerAverageWeight
    ) {
        public static Multipliers defaults() {
            return new Multipliers(5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 1.0f, 1.0f, 0.5f, 0.5f, 0.5f);
        }
    }

    public record FoodPropertyHeuristics(
            SaturatingMealRule saturatingMeal,
            LightSnackRule lightSnack,
            List<String> badEffectKeywords,
            float badEffectMultiplier
    ) {
        public static FoodPropertyHeuristics defaults() {
            return new FoodPropertyHeuristics(
                    SaturatingMealRule.empty(),
                    LightSnackRule.empty(),
                    List.of(),
                    1.0f
            );
        }
    }

    public record SaturatingMealRule(float minSaturation, int minNutrition, Map<String, Float> contributions) {
        public static SaturatingMealRule empty() {
            return new SaturatingMealRule(Float.POSITIVE_INFINITY, Integer.MAX_VALUE, Map.of());
        }
        public boolean matches(int nutrition, float saturation) {
            return saturation > minSaturation && nutrition > minNutrition;
        }
    }

    public record LightSnackRule(int maxNutrition, Map<String, Float> contributions) {
        public static LightSnackRule empty() {
            return new LightSnackRule(Integer.MIN_VALUE, Map.of());
        }
        public boolean matches(int nutrition) {
            return nutrition <= maxNutrition;
        }
    }
}
