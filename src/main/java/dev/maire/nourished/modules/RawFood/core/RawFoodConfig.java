package dev.maire.nourished.modules.RawFood.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedItemTags;
import dev.maire.nourished.core.util.NourishedJsonUtils;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import dev.maire.nourished.core.util.NourishedResourceLoader;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodResistanceConfig;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApiStatus.Internal
public class RawFoodConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE_PATH = "/data/nourished/config/raw_food.json";

    private static final Map<RawSeverity, RawFoodTierDef> TIERS = new EnumMap<>(RawSeverity.class);
    private static final Map<String, RawSeverity> TOKEN_MAP = new LinkedHashMap<>();
    private static final Map<RawSeverity, RawFoodResistanceConfig> RESISTANCE_CONFIGS = new EnumMap<>(RawSeverity.class);

    private static int memorySecs = 120;

    // ── Gut Config ────────────────────────────────────────────────────────────
    private static int gutTickInterval = 100;
    private static float baseRecoveryRate = 0.001f;
    private static float cookedFoodRecoveryRate = 0.02f;
    private static float diversityThreshold = 0.6f;
    private static float diversityBonusRate = 0.0005f;
    private static float sensitivityDecayRate = 0.0002f;
    private static float maxSensitivityMultiplier = 2.0f;
    private static float sensitivityIncrementPerRawEat = 0.05f;

    private RawFoodConfig() {}

    public static RawSeverity classify(ItemStack stack) {
        if (stack.is(NourishedItemTags.RAW_FOOD_FINE)) {
            return RawSeverity.FINE;
        }
        if (stack.is(NourishedItemTags.RAW_FOOD_MILD)) {
            return RawSeverity.MILD;
        }
        if (stack.is(NourishedItemTags.RAW_FOOD_MEDIUM)) {
            return RawSeverity.MEDIUM;
        }
        if (stack.is(NourishedItemTags.RAW_FOOD_SEVERE)) {
            return RawSeverity.SEVERE;
        }

        String itemPath = NourishedRegistryUtils.itemKey(stack).getPath().toLowerCase(Locale.ROOT);
        return classifyByTokens(itemPath, "");
    }

    /**
     * Classifies severity by token matching against item path and display name.
     * Returns FINE if no tokens match.
     * Called by RawFoodClassifier as a fallback for items not in the scanner cache.
     *
     * @param itemPath    the item registry path (e.g. "beef", "porkchop")
     * @param displayName lowercased display name (e.g. "raw beef")
     * @return the matched severity, or FINE if no match
     */
    public static RawSeverity classifyByTokens(String itemPath, String displayName) {
        for (Map.Entry<String, RawSeverity> entry : TOKEN_MAP.entrySet()) {
            String token = entry.getKey();
            if (displayName.contains(token) || itemPath.contains(token)) {
                return entry.getValue();
            }
        }
        return RawSeverity.FINE;
    }

    public static RawFoodTierDef getTier(RawSeverity severity) {
        return TIERS.getOrDefault(severity, fallbackTier(severity));
    }

    public static RawFoodResistanceConfig getResistanceConfig(RawSeverity severity) {
        return RESISTANCE_CONFIGS.getOrDefault(severity, RawFoodResistanceConfig.EMPTY);
    }

    public static int memorySecs() {
        return memorySecs;
    }

    public static void setMemorySecs(int value) {
        memorySecs = Math.max(0, value);
    }

    public static void setDurationTicks(RawSeverity severity, int durationTicks) {
        RawFoodTierDef tier = getTier(severity);
        setTier(severity, new RawFoodTierDef(
                tier.effectPool(),
                Math.max(0, durationTicks),
                tier.amplifier(),
                tier.nutrientPenalty(),
                tier.missedOpportunityMultiplier()
        ));
    }

    public static void setNutrientPenalty(RawSeverity severity, float nutrientPenalty) {
        RawFoodTierDef tier = getTier(severity);
        setTier(severity, new RawFoodTierDef(
                tier.effectPool(),
                tier.durationTicks(),
                tier.amplifier(),
                nutrientPenalty,
                tier.missedOpportunityMultiplier()
        ));
    }

    public static void setMissedOpportunityMultiplier(RawSeverity severity, float missedOpportunityMultiplier) {
        RawFoodTierDef tier = getTier(severity);
        setTier(severity, new RawFoodTierDef(
                tier.effectPool(),
                tier.durationTicks(),
                tier.amplifier(),
                tier.nutrientPenalty(),
                missedOpportunityMultiplier
        ));
    }

    private static void setTier(RawSeverity severity, RawFoodTierDef tier) {
        TIERS.put(severity, tier);
    }

    // ── Gut Config Getters ────────────────────────────────────────────────────

    public static int gutTickInterval() {
        return gutTickInterval;
    }

    public static float baseRecoveryRate() {
        return baseRecoveryRate;
    }

    public static float cookedFoodRecoveryRate() {
        return cookedFoodRecoveryRate;
    }

    public static float diversityThreshold() {
        return diversityThreshold;
    }

    public static float diversityBonusRate() {
        return diversityBonusRate;
    }

    public static float sensitivityDecayRate() {
        return sensitivityDecayRate;
    }

    public static float maxSensitivityMultiplier() {
        return maxSensitivityMultiplier;
    }

    public static float sensitivityIncrementPerRawEat() {
        return sensitivityIncrementPerRawEat;
    }

    /**
     * Returns true if any of the provided tokens maps to the given severity.
     * Tokens are matched case-insensitively.
     */
    public static boolean hasAnyToken(RawSeverity severity, Collection<String> tokens) {
        for (String token : tokens) {
            String normalized = token.toLowerCase(Locale.ROOT);
            RawSeverity mapped = TOKEN_MAP.get(normalized);
            if (mapped == severity) {
                return true;
            }
        }
        return false;
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("raw_food.json");

        resetToEmpty();
        try {
            parseBundledDefaults();
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[RawFoodConfig] Wrote default raw_food.json");
            } else {
                parse(file);
            }
            ensureAllTiers();
            Nourished.LOGGER.info("[RawFoodConfig] Loaded {} raw food tiers and {} tokens from config stack", TIERS.size(), TOKEN_MAP.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[RawFoodConfig] Failed to load raw_food.json, using built-in defaults", e);
            loadDefaults();
        }
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        NourishedResourceLoader.loadFromModConfig(
                resourceManager,
                "config/raw_food.json",
                reader -> {
                    load();
                    parseFromReader(reader);
                    ensureAllTiers();
                    return true;
                },
                RawFoodConfig::load,
                "[RawFoodConfig] Loaded from datapack override",
                "[RawFoodConfig] Failed to load from datapack, falling back to config folder",
                "[RawFoodConfig] Loaded from config folder"
        );
    }

    public static void reload() {
        Nourished.LOGGER.info("[RawFoodConfig] Reloading raw_food.json");
        load();
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("raw_food.json");
        ensureAllTiers();
        try {
            Files.createDirectories(configDir);
            writeCurrent(file);
            Nourished.LOGGER.info("[RawFoodConfig] Saved raw_food.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[RawFoodConfig] Failed to save raw_food.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            parseFromReader(reader);
        }
    }

    private static void parseBundledDefaults() throws IOException {
        try (InputStream in = RawFoodConfig.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in == null) {
                registerFallbackDefaults();
                return;
            }
            try (Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                parseFromReader(reader);
            }
        }
    }

    private static void parseFromReader(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            Nourished.LOGGER.warn("[RawFoodConfig] raw_food.json was empty, keeping lower-priority values");
            return;
        }
        if (root.has("memory_secs")) {
            memorySecs = Math.max(0, root.get("memory_secs").getAsInt());
        }
        if (root.has("tokens") && root.get("tokens").isJsonObject()) {
            parseTokens(root.getAsJsonObject("tokens"));
        }
        if (root.has("tiers") && root.get("tiers").isJsonObject()) {
            parseTiers(root.getAsJsonObject("tiers"));
        }
        if (root.has("gut") && root.get("gut").isJsonObject()) {
            parseGut(root.getAsJsonObject("gut"));
        }
    }

    private static void parseGut(JsonObject gut) {
        gutTickInterval = NourishedJsonUtils.getOptionalInt(gut, "tick_interval", gutTickInterval);
        baseRecoveryRate = NourishedJsonUtils.getOptionalFloat(gut, "base_recovery_rate", baseRecoveryRate);
        cookedFoodRecoveryRate = NourishedJsonUtils.getOptionalFloat(gut, "cooked_food_recovery_rate", cookedFoodRecoveryRate);
        diversityThreshold = NourishedJsonUtils.getOptionalFloat(gut, "diversity_threshold", diversityThreshold);
        diversityBonusRate = NourishedJsonUtils.getOptionalFloat(gut, "diversity_bonus_rate", diversityBonusRate);
        sensitivityDecayRate = NourishedJsonUtils.getOptionalFloat(gut, "sensitivity_decay_rate", sensitivityDecayRate);
        maxSensitivityMultiplier = NourishedJsonUtils.getOptionalFloat(gut, "max_sensitivity_multiplier", maxSensitivityMultiplier);
        sensitivityIncrementPerRawEat = NourishedJsonUtils.getOptionalFloat(gut, "sensitivity_increment_per_raw_eat", sensitivityIncrementPerRawEat);
    }

    private static void parseTokens(JsonObject tokens) {
        for (Map.Entry<String, JsonElement> entry : tokens.entrySet()) {
            RawSeverity severity = parseSeverity(entry.getKey());
            if (severity == null || !entry.getValue().isJsonArray()) {
                continue;
            }
            JsonArray arr = entry.getValue().getAsJsonArray();
            for (JsonElement element : arr) {
                String token = element.getAsString().toLowerCase(Locale.ROOT);
                if (!token.isBlank()) {
                    TOKEN_MAP.put(token, severity);
                }
            }
        }
    }

    private static void parseTiers(JsonObject tiers) {
        for (Map.Entry<String, JsonElement> entry : tiers.entrySet()) {
            RawSeverity severity = parseSeverity(entry.getKey());
            if (severity == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getValue().getAsJsonObject();
            RawFoodTierDef previous = getTier(severity);
            List<String> effectPool = obj.has("effect_pool") && obj.get("effect_pool").isJsonArray()
                    ? parseEffectPool(obj.getAsJsonArray("effect_pool"))
                    : previous.effectPool();
            int durationTicks = NourishedJsonUtils.getOptionalInt(obj, "duration_ticks", previous.durationTicks());
            int amplifier = NourishedJsonUtils.getOptionalInt(obj, "amplifier", previous.amplifier());
            float nutrientPenalty = NourishedJsonUtils.getOptionalFloat(obj, "nutrient_penalty", previous.nutrientPenalty());
            float missedOpportunityMultiplier = NourishedJsonUtils.getOptionalFloat(obj, "missed_opportunity_multiplier", previous.missedOpportunityMultiplier());
            TIERS.put(severity, new RawFoodTierDef(effectPool, durationTicks, amplifier, nutrientPenalty, missedOpportunityMultiplier));

            if (obj.has("resistance") && obj.get("resistance").isJsonObject()) {
                RESISTANCE_CONFIGS.put(severity, parseResistanceConfig(obj.getAsJsonObject("resistance")));
            }
        }
    }

    private static RawFoodResistanceConfig parseResistanceConfig(JsonObject resistance) {
        float threshold = NourishedJsonUtils.getOptionalFloat(resistance, "threshold", 1.0f);
        float maxResistance = NourishedJsonUtils.getOptionalFloat(resistance, "max_resistance", 0.0f);

        Map<String, Float> nutrientWeights = new LinkedHashMap<>();
        if (resistance.has("nutrient_weights") && resistance.get("nutrient_weights").isJsonObject()) {
            JsonObject weights = resistance.getAsJsonObject("nutrient_weights");
            List<String> validKeys = NutrientRegistry.getKeys();
            for (Map.Entry<String, JsonElement> entry : weights.entrySet()) {
                String nutrientKey = entry.getKey();
                if (!validKeys.contains(nutrientKey)) {
                    Nourished.LOGGER.warn("[RawFoodConfig] Unknown nutrient key '{}' in resistance config, skipping", nutrientKey);
                    continue;
                }
                float weight = entry.getValue().getAsFloat();
                nutrientWeights.put(nutrientKey, weight);
            }
        }

        return new RawFoodResistanceConfig(Map.copyOf(nutrientWeights), threshold, maxResistance);
    }

    private static List<String> parseEffectPool(JsonArray arr) {
        List<String> pool = new ArrayList<>();
        for (JsonElement element : arr) {
            String id = element.getAsString();
            if (!id.isBlank()) {
                pool.add(id);
            }
        }
        return List.copyOf(pool);
    }

    private static RawSeverity parseSeverity(String value) {
        try {
            return RawSeverity.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            Nourished.LOGGER.warn("[RawFoodConfig] Ignoring unknown raw food severity '{}'", value);
            return null;
        }
    }

    private static void loadDefaults() {
        resetToEmpty();
        try {
            parseBundledDefaults();
        } catch (IOException e) {
            registerFallbackDefaults();
        }
        ensureAllTiers();
    }

    private static void resetToEmpty() {
        TIERS.clear();
        TOKEN_MAP.clear();
        RESISTANCE_CONFIGS.clear();
        memorySecs = 120;
        // Reset gut config to defaults
        gutTickInterval = 100;
        baseRecoveryRate = 0.001f;
        cookedFoodRecoveryRate = 0.02f;
        diversityThreshold = 0.6f;
        diversityBonusRate = 0.0005f;
        sensitivityDecayRate = 0.0002f;
        maxSensitivityMultiplier = 2.0f;
        sensitivityIncrementPerRawEat = 0.05f;
    }

    private static void ensureAllTiers() {
        for (RawSeverity severity : RawSeverity.values()) {
            TIERS.putIfAbsent(severity, fallbackTier(severity));
        }
    }

    private static RawFoodTierDef fallbackTier(RawSeverity severity) {
        return switch (severity) {
            case FINE -> new RawFoodTierDef(List.of(), 0, 0, 0.0f, 0.0f);
            case MILD -> new RawFoodTierDef(List.of("minecraft:hunger"), 600, 0, -0.03f, 0.15f);
            case MEDIUM -> new RawFoodTierDef(List.of("minecraft:slowness", "minecraft:weakness"), 1200, 0, -0.05f, 0.35f);
            case SEVERE -> new RawFoodTierDef(List.of("minecraft:poison", "minecraft:nausea", "minecraft:blindness"), 1200, 1, -0.08f, 0.60f);
        };
    }

    private static void registerFallbackDefaults() {
        memorySecs = 120;
        TOKEN_MAP.put("fresh", RawSeverity.MILD);
        TOKEN_MAP.put("unripe", RawSeverity.MILD);
        TOKEN_MAP.put("raw", RawSeverity.MEDIUM);
        TOKEN_MAP.put("uncooked", RawSeverity.MEDIUM);
        TOKEN_MAP.put("rotten", RawSeverity.SEVERE);
        TOKEN_MAP.put("spoiled", RawSeverity.SEVERE);
        TOKEN_MAP.put("crude", RawSeverity.SEVERE);
        ensureAllTiers();
    }

    private static void writeDefaults(Path file) throws IOException {
        try (InputStream in = RawFoodConfig.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Missing bundled " + DEFAULT_RESOURCE_PATH);
            }
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeCurrent(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("memory_secs", memorySecs);
        root.add("tokens", tokensToJson());
        root.add("tiers", tiersToJson());
        root.add("gut", gutToJson());
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(root, writer);
        }
    }

    private static JsonObject tokensToJson() {
        JsonObject tokens = new JsonObject();
        for (RawSeverity severity : RawSeverity.values()) {
            if (severity == RawSeverity.FINE) {
                continue;
            }
            JsonArray arr = new JsonArray();
            for (Map.Entry<String, RawSeverity> entry : TOKEN_MAP.entrySet()) {
                if (entry.getValue() == severity) {
                    arr.add(entry.getKey());
                }
            }
            tokens.add(severity.name().toLowerCase(Locale.ROOT), arr);
        }
        return tokens;
    }

    private static JsonObject tiersToJson() {
        JsonObject tiers = new JsonObject();
        for (RawSeverity severity : RawSeverity.values()) {
            RawFoodTierDef tier = getTier(severity);
            JsonObject obj = new JsonObject();
            JsonArray effectPool = new JsonArray();
            for (String effect : tier.effectPool()) {
                effectPool.add(effect);
            }
            obj.add("effect_pool", effectPool);
            obj.addProperty("duration_ticks", tier.durationTicks());
            obj.addProperty("amplifier", tier.amplifier());
            obj.addProperty("nutrient_penalty", tier.nutrientPenalty());
            obj.addProperty("missed_opportunity_multiplier", tier.missedOpportunityMultiplier());

            RawFoodResistanceConfig resistance = RESISTANCE_CONFIGS.get(severity);
            if (resistance != null) {
                obj.add("resistance", resistanceToJson(resistance));
            }
            tiers.add(severity.name().toLowerCase(Locale.ROOT), obj);
        }
        return tiers;
    }

    private static JsonObject resistanceToJson(RawFoodResistanceConfig resistance) {
        JsonObject obj = new JsonObject();
        obj.addProperty("threshold", resistance.resistanceThreshold());
        obj.addProperty("max_resistance", resistance.maxResistance());
        JsonObject weights = new JsonObject();
        for (Map.Entry<String, Float> entry : resistance.nutrientWeights().entrySet()) {
            weights.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("nutrient_weights", weights);
        return obj;
    }

    private static JsonObject gutToJson() {
        JsonObject gut = new JsonObject();
        gut.addProperty("tick_interval", gutTickInterval);
        gut.addProperty("base_recovery_rate", baseRecoveryRate);
        gut.addProperty("cooked_food_recovery_rate", cookedFoodRecoveryRate);
        gut.addProperty("diversity_threshold", diversityThreshold);
        gut.addProperty("diversity_bonus_rate", diversityBonusRate);
        gut.addProperty("sensitivity_decay_rate", sensitivityDecayRate);
        gut.addProperty("max_sensitivity_multiplier", maxSensitivityMultiplier);
        gut.addProperty("sensitivity_increment_per_raw_eat", sensitivityIncrementPerRawEat);
        return gut;
    }
}
