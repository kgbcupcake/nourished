package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.api.registry.ValueRegistry;
import dev.marie.framework.config.FeatureFlagCache;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.runtime.SourceRegistry;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads nutrient definitions from config/nourished/nutrients.json.
 * Writes a default file on first run. Call {@link #loadDefinitions()} before config spec
 * construction; call {@link #syncAndFreeze()} during common setup to publish into
 * {@link ValueRegistry}. {@link #load()} runs both phases for lifecycle/reload callers.
 */
@ApiStatus.Internal
public class NutrientRegistry {

    public record NutrientDef(
            String key,
            String displayName,
            int color,
            float defaultDecayRate,
            float criticalThreshold,
            float lowThreshold,
            float excessThreshold,
            String icon,
            List<String> tags,
            boolean beneficial
    ) {
        public static NutrientDef fromDefinition(ValueDefinition definition) {
            String key = Objects.requireNonNull(definition.getId(), "definition id");
            String icon = resolveIcon(key);
            List<String> tags = List.of(Nourished.MODID + ":nutrients/" + key);
            return new NutrientDef(
                    key,
                    definition.getDisplayName(),
                    definition.getColor(),
                    definition.getDefaultDecayRate(),
                    definition.getCriticalThreshold(),
                    definition.getLowThreshold(),
                    definition.getExcessThreshold(),
                    icon,
                    tags,
                    definition.isBeneficial()
            );
        }

        public static NutrientDef fromConfig(String key, String icon, List<String> tags) {
            return new NutrientDef(
                    key,
                    key,
                    DEFAULT_COLOR,
                    DEFAULT_DECAY_RATE,
                    DEFAULT_CRITICAL_THRESHOLD,
                    DEFAULT_LOW_THRESHOLD,
                    DEFAULT_EXCESS_THRESHOLD,
                    icon,
                    tags,
                    true
            );
        }

        public ValueDefinition toValueDefinition() {
            return ValueDefinition.builder(key)
                    .displayName(displayName)
                    .color(color)
                    .defaultDecayRate(defaultDecayRate)
                    .criticalThreshold(criticalThreshold)
                    .lowThreshold(lowThreshold)
                    .excessThreshold(excessThreshold)
                    .beneficial(beneficial)
                    .build();
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_ICON = "minecraft:apple";
    private static final Map<String, String> ICON_FALLBACKS = Map.of(
            "fruits", "minecraft:apple",
            "vegetables", "minecraft:carrot",
            "proteins", "minecraft:cooked_beef",
            "grains", "minecraft:wheat",
            "dairy", "minecraft:milk_bucket"
    );
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final Set<String> BUILTIN_NUTRIENT_KEYS = Set.of(
            "fruits",
            "vegetables",
            "proteins",
            "grains",
            "dairy"
    );
    private static volatile Map<String, Integer> bundledBuiltinColorCache;
    private static final float DEFAULT_DECAY_RATE = 0f;
    private static final float DEFAULT_CRITICAL_THRESHOLD = 0f;
    private static final float DEFAULT_LOW_THRESHOLD = 0f;
    private static final float DEFAULT_EXCESS_THRESHOLD = 1f;
    private static final Set<String> REQUIRED_FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
            "key",
            "display_name",
            "color",
            "default_decay_rate",
            "critical_threshold",
            "low_threshold",
            "excess_threshold",
            "beneficial",
            "icon",
            "tags"
    )));

    private static final class Core extends AbstractRegistry<String, NutrientDef> {
        Core() {
            super("NutrientRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private static volatile boolean bootstrapped = false;

    private static final Map<String, NutrientDef> EXTERNALLY_REGISTERED = new java.util.concurrent.ConcurrentHashMap<>();

    private static final String[] DEFAULT_NUTRIENT_RESOURCES = {
            "fruits",
            "vegetables",
            "proteins",
            "grains",
            "dairy"
    };

    // ── Public API ────────────────────────────────────────────────────────────

    /** Ordered list of nutrient keys (insertion order = display order). */
    public static List<String> getKeys() {
        return INSTANCE.keys();
    }

    /** Item ID string for the nutrient's icon, e.g. {@code "minecraft:carrot"}. */
    public static String getIcon(String key) {
        NutrientDef def = INSTANCE.get(key);
        if (def != null && def.icon() != null && !def.icon().isBlank()) {
            return def.icon();
        }
        return fallbackIconForKey(key);
    }

    /** Food tags that map to this nutrient. */
    public static List<String> getTags(String key) {
        NutrientDef def = INSTANCE.get(key);
        return def != null ? def.tags() : List.of();
    }

    /** Display name from nutrients.json, or the key if unknown. */
    public static String getDisplayName(String key) {
        NutrientDef def = INSTANCE.get(key);
        if (def != null && def.displayName() != null && !def.displayName().isBlank()) {
            return def.displayName();
        }
        return key;
    }

    /**
     * Localized HUD label when a lang entry exists; otherwise {@link #getDisplayName(String)}.
     */
    public static String getLabel(String key) {
        String translationKey = Nourished.MODID + ".screen.diet.bar." + key;
        String translated = Component.translatable(translationKey).getString();
        if (!translated.equals(translationKey)) {
            return translated;
        }
        return getDisplayName(key);
    }

    /** Same resolution as {@link #getLabel(String)}, as a chat component. */
    public static Component getLabelComponent(String key) {
        String translationKey = Nourished.MODID + ".screen.diet.bar." + key;
        String translated = Component.translatable(translationKey).getString();
        if (!translated.equals(translationKey)) {
            return Component.translatable(translationKey);
        }
        return Component.literal(getDisplayName(key));
    }

    /** All registered nutrient definitions in order. */
    public static List<NutrientDef> getAll() {
        return INSTANCE.values();
    }

    /**
     * Returns whether a nutrient is beneficial (high values are good).
     * When false, high values are treated as bad.
     * <p>
     * Note: This flag affects display/threshold interpretation only (HUD colors,
     * critical toasts, tooltips). It does NOT affect effect triggers — modpack makers
     * control those via explicit trigger types (above, below, all_above, etc.) in effects JSON.
     *
     * @param key the nutrient key
     * @return {@code true} if high values are good, {@code false} if high values are bad;
     *         defaults to {@code true} if the nutrient is not found
     */
    public static boolean isBeneficial(String key) {
        NutrientDef def = INSTANCE.get(key);
        return def == null || def.beneficial();
    }

    public static void registerExternal(ValueDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String key = Objects.requireNonNull(definition.getId(), "definition id");
        if (INSTANCE.contains(key)) {
            throw new IllegalArgumentException("Nutrient already registered: " + key);
        }
        if (!INSTANCE.isFrozen()) {
            NutrientDef def = NutrientDef.fromDefinition(definition);
            INSTANCE.register(key, def);
            EXTERNALLY_REGISTERED.put(key, def);
            syncValueRegistryEntry(definition);
            Nourished.LOGGER.info("[NutrientRegistry] Registered external nutrient: {}", key);
            return;
        }
        INSTANCE.runWrite(() -> {
            List<NutrientDef> prior = new ArrayList<>(INSTANCE.valuesUnlocked());
            INSTANCE.resetUnlocked();
            for (NutrientDef d : prior) {
                INSTANCE.registerUnlocked(d.key(), d);
            }
            INSTANCE.registerUnlocked(key, NutrientDef.fromDefinition(definition));
            INSTANCE.freezeUnlocked();
        });
        EXTERNALLY_REGISTERED.put(key, NutrientDef.fromDefinition(definition));
        syncValueRegistryEntry(definition);
        Nourished.LOGGER.info("[NutrientRegistry] Registered external nutrient: {}", key);
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    /** Loads and parses nutrients.json into this registry without touching {@link ValueRegistry}. */
    public static void loadDefinitions() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        doLoadDefinitions();
    }

    /** Publishes loaded nutrients into {@link ValueRegistry} and freezes it. */
    public static void syncAndFreeze() {
        syncToValueRegistry();
    }

    /**
     * Publishes currently-loaded nutrient definitions into {@link ValueRegistry} without
     * freezing it. Call immediately after {@link #loadDefinitions()} in the mod constructor so
     * KubeJS startup scripts (which run before FMLCommonSetupEvent) can pass requireValueKey
     * checks for built-in and previously-persisted nutrients. syncAndFreeze() still runs later
     * at common setup and is idempotent against this — it resets and re-populates from the same
     * NutrientRegistry source before freezing.
     */
    public static void syncToValueRegistryUnfrozen() {
        for (NutrientDef def : getAll()) {
            if (ValueRegistry.get(def.key()) == null) {
                ValueRegistry.register(def.toValueDefinition());
            }
        }
    }

    public static void load() {
        loadDefinitions();
        syncAndFreeze();
        writeReadme();
    }

    private static void writeReadme() {
        try {
            Path readmeDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("Read_Me");
            Files.createDirectories(readmeDir);
            writeReadmeIfAbsent(readmeDir);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NutrientRegistry] Failed to write NUTRIENTS_README.md", e);
        }
    }

    private static void writeReadmeIfAbsent(Path readmeDir) throws IOException {
        Path readme = readmeDir.resolve("NUTRIENTS_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + Nourished.MODID + "/config/NUTRIENTS_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                Nourished.LOGGER.warn("[NutrientRegistry] No bundled NUTRIENTS_README.md, skipping write");
                return;
            }
            Files.copy(in, readme);
        }
    }

    private static void doLoadDefinitions() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("nutrients.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                if (writeDefaultsIfPossible(file)) {
                    Nourished.LOGGER.info("[NutrientRegistry] Wrote default nutrients.json");
                }
            }
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    JsonArray arr = GSON.fromJson(r, JsonArray.class);
                    ensureValidConfigFile(file, arr);
                }
            }
            boolean usedBundledFallback = parse(file);
            FoodNutritionRegistry.clearTagKeyCache();
            if (usedBundledFallback) {
                repairConfigFile(file);
            }
            Nourished.LOGGER.info("[NutrientRegistry] Loaded {} nutrients from {}", INSTANCE.size(), file);
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientRegistry] Failed to load nutrients.json, using built-in defaults", e);
            loadDefaults();
            repairConfigFile(file);
        }
    }

    /**
     * Re-reads nutrients.json from disk. Tag-based classifications are synced separately on
     * {@link net.neoforged.neoforge.event.TagsUpdatedEvent} once item tags are bound.
     */
    public static void reload() {
        Nourished.LOGGER.info("[NutrientRegistry] Reloading nutrients.json");
        doLoadDefinitions();
        syncAndFreeze();
    }

    /** Persists the in-memory registry to config/nourished/nutrients.json. */
    public static void save() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("nutrients.json");
        try {
            Files.createDirectories(file.getParent());
            writeNutrientsToPath(file, INSTANCE.values());
            Nourished.LOGGER.info("[NutrientRegistry] Saved nutrients.json ({} entries)", INSTANCE.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientRegistry] Failed to save nutrients.json", e);
        }
    }

    /**
     * Registers nutrient tag matches into {@link dev.marie.framework.runtime.SourceRegistry}.
     * Must be called after item tags are bound (see {@code NourishedTagsHandler}).
     */
    public static void registerClassificationsFromTags() {
        if (FeatureFlagCache.enableDebugLogging()) {
            Nourished.LOGGER.debug("registerClassificationsFromTags starting");
        }
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Map<String, Float> scores = FoodNutritionRegistry.getNutrientTagScores(item);
            if (scores.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = item.builtInRegistryHolder().key().location();
            for (Map.Entry<String, Float> entry : scores.entrySet()) {
                SourceRegistry.registerClassification(itemId, entry.getKey(), entry.getValue());
                count++;
            }
        }
        if (FeatureFlagCache.enableDebugLogging()) {
            Nourished.LOGGER.debug("registerClassificationsFromTags complete — {} classifications registered", count);
        }
    }

    private static void syncToValueRegistry() {
        ValueRegistry.resetInternal();
        for (NutrientDef def : getAll()) {
            ValueRegistry.register(def.toValueDefinition());
        }
        ValueRegistry.freezeInternal();
    }

    /**
     * Publishes a single external nutrient into {@link ValueRegistry} so it passes
     * requireValueKey checks immediately — including mid-script (before syncAndFreeze runs)
     * and mid-game (after the registry is already frozen).
     */
    private static void syncValueRegistryEntry(ValueDefinition definition) {
        if (ValueRegistry.get(definition.getId()) != null) {
            return;
        }
        if (!ValueRegistry.isFrozen()) {
            ValueRegistry.register(definition);
            return;
        }
        List<ValueDefinition> prior = new ArrayList<>(ValueRegistry.getAll());
        ValueRegistry.resetInternal();
        for (ValueDefinition d : prior) {
            ValueRegistry.register(d);
        }
        ValueRegistry.register(definition);
        ValueRegistry.freezeInternal();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static void ensureValidConfigFile(Path file, JsonArray arr) throws IOException {
        if (!isInvalidConfig(arr)) {
            return;
        }
        if (arr == null) {
            Nourished.LOGGER.info("[NutrientRegistry] nutrients.json is missing or unreadable — regenerating");
        } else if (arr.isEmpty()) {
            Nourished.LOGGER.info("[NutrientRegistry] nutrients.json is empty — regenerating");
        } else {
            Nourished.LOGGER.info("[NutrientRegistry] nutrients.json schema mismatch detected — regenerating");
        }
        Files.deleteIfExists(file);
        if (writeDefaultsIfPossible(file)) {
            Nourished.LOGGER.info("[NutrientRegistry] Wrote updated nutrients.json");
        }
    }

    private static boolean isInvalidConfig(JsonArray arr) {
        if (arr == null || arr.isEmpty()) {
            return true;
        }
        return isSchemaMismatch(arr);
    }

    private static boolean isSchemaMismatch(JsonArray arr) {
        List<String> mismatches = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            List<String> missing = REQUIRED_FIELDS.stream()
                    .filter(field -> !obj.has(field))
                    .toList();
            if (missing.isEmpty()) {
                continue;
            }

            String key = obj.has("key") ? obj.get("key").getAsString() : "<missing key>";
            mismatches.add("entry '" + key + "': missing " + missing);
        }

        if (FeatureFlagCache.enableDebugLogging()) {
            for (String mismatch : mismatches) {
                Nourished.LOGGER.debug("[NutrientRegistry] nutrients.json schema mismatch: {}", mismatch);
            }
        }
        return !mismatches.isEmpty();
    }

    private static boolean parse(Path file) throws IOException {
        JsonArray arr = null;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                arr = GSON.fromJson(r, JsonArray.class);
            }
        }
        final JsonArray configArray = arr;
        boolean[] usedBundledFallback = {false};
        boolean[] legacyBuiltinColorsRepaired = {false};
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (configArray == null) {
                Nourished.LOGGER.warn("[NutrientRegistry] nutrients.json was empty, using built-in defaults");
                usedBundledFallback[0] = true;
                loadDefaultsUnlocked();
                return;
            }
            int limit = 2000;
            if (configArray.size() > limit) {
                Nourished.LOGGER.warn("[NutrientRegistry] nutrients.json has {} entries, exceeding {} — truncating", configArray.size(), limit);
            }
            for (int i = 0; i < Math.min(configArray.size(), limit); i++) {
                JsonElement el = configArray.get(i);
                JsonObject obj = el.getAsJsonObject();
                com.google.gson.JsonElement keyEl = obj.get("key");
                if (keyEl == null || !keyEl.isJsonPrimitive() || !keyEl.getAsJsonPrimitive().isString()) {
                    Nourished.LOGGER.warn("[NutrientRegistry] Skipping entry at index {} missing or invalid 'key'", i);
                    continue;
                }
                String key = keyEl.getAsString();
                String icon = obj.has("icon") ? obj.get("icon").getAsString() : fallbackIconForKey(key);
                String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : key;
                int color = obj.has("color") ? obj.get("color").getAsInt() : DEFAULT_COLOR;
                int repairedColor = repairLegacyBuiltinColor(key, color);
                if (repairedColor != color) {
                    legacyBuiltinColorsRepaired[0] = true;
                    color = repairedColor;
                }
                float defaultDecayRate = obj.has("default_decay_rate") ? obj.get("default_decay_rate").getAsFloat() : DEFAULT_DECAY_RATE;
                float criticalThreshold = obj.has("critical_threshold") ? obj.get("critical_threshold").getAsFloat() : DEFAULT_CRITICAL_THRESHOLD;
                float lowThreshold = obj.has("low_threshold") ? obj.get("low_threshold").getAsFloat() : DEFAULT_LOW_THRESHOLD;
                float excessThreshold = obj.has("excess_threshold") ? obj.get("excess_threshold").getAsFloat() : DEFAULT_EXCESS_THRESHOLD;
                boolean beneficial = !obj.has("beneficial") || obj.get("beneficial").getAsBoolean();
                List<String> tags = new ArrayList<>();
                if (obj.has("tags")) {
                    for (JsonElement t : obj.getAsJsonArray("tags")) {
                        tags.add(t.getAsString());
                    }
                }
                INSTANCE.registerUnlocked(key, new NutrientDef(
                        key,
                        displayName,
                        color,
                        defaultDecayRate,
                        criticalThreshold,
                        lowThreshold,
                        excessThreshold,
                        icon,
                        Collections.unmodifiableList(tags),
                        beneficial
                ));
            }
            if (INSTANCE.valuesUnlocked().isEmpty()) {
                Nourished.LOGGER.warn("[NutrientRegistry] nutrients.json was empty, using built-in defaults");
                usedBundledFallback[0] = true;
                loadDefaultsUnlocked();
            } else {
                reapplyExternallyRegisteredUnlocked();
                INSTANCE.freezeUnlocked();
            }
        });
        if (legacyBuiltinColorsRepaired[0]) {
            save();
            Nourished.LOGGER.info(
                    "[NutrientRegistry] Repaired legacy white (0xFFFFFFFF) colors for built-in nutrients in nutrients.json");
        }
        return usedBundledFallback[0];
    }

    /**
     * Replaces the old broken bundled default ({@code 0xFFFFFFFF}) with the corrected bundled color
     * for the five built-in nutrient keys only; all other keys and colors are unchanged.
     */
    private static int repairLegacyBuiltinColor(String key, int color) {
        if (color != DEFAULT_COLOR || !BUILTIN_NUTRIENT_KEYS.contains(key)) {
            return color;
        }
        Integer bundledColor = bundledBuiltinColorsByKey().get(key);
        return bundledColor != null ? bundledColor : color;
    }

    private static Map<String, Integer> bundledBuiltinColorsByKey() {
        Map<String, Integer> cached = bundledBuiltinColorCache;
        if (cached != null) {
            return cached;
        }
        Map<String, Integer> colors = new java.util.HashMap<>();
        for (NutrientDef def : loadBundledDefaults()) {
            colors.put(def.key(), def.color());
        }
        bundledBuiltinColorCache = Map.copyOf(colors);
        return bundledBuiltinColorCache;
    }

    private static void loadDefaultsUnlocked() {
        INSTANCE.resetUnlocked();
        for (NutrientDef def : loadBundledDefaults()) {
            INSTANCE.registerUnlocked(def.key(), def);
        }
        reapplyExternallyRegisteredUnlocked();
        INSTANCE.freezeUnlocked();
    }

    private static void reapplyExternallyRegisteredUnlocked() {
        for (Map.Entry<String, NutrientDef> entry : EXTERNALLY_REGISTERED.entrySet()) {
            if (!INSTANCE.keysUnlocked().contains(entry.getKey())) {
                INSTANCE.registerUnlocked(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void loadDefaults() {
        INSTANCE.runWrite(NutrientRegistry::loadDefaultsUnlocked);
    }

    private static boolean writeDefaultsIfPossible(Path file) throws IOException {
        List<NutrientDef> defaults = loadBundledDefaults();
        if (defaults.isEmpty()) {
            Nourished.LOGGER.error(
                    "[NutrientRegistry] Bundled nutrient definitions unavailable — refusing to write empty nutrients.json");
            return false;
        }
        writeNutrientsToPath(file, defaults);
        return true;
    }

    private static void writeNutrientsToPath(Path file, List<NutrientDef> nutrients) throws IOException {
        if (nutrients.isEmpty()) {
            throw new IOException("refusing to write empty nutrients.json");
        }
        JsonArray arr = new JsonArray();
        for (NutrientDef def : nutrients) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", def.key());
            obj.addProperty("display_name", def.displayName());
            obj.addProperty("color", def.color());
            obj.addProperty("default_decay_rate", def.defaultDecayRate());
            obj.addProperty("critical_threshold", def.criticalThreshold());
            obj.addProperty("low_threshold", def.lowThreshold());
            obj.addProperty("excess_threshold", def.excessThreshold());
            obj.addProperty("beneficial", def.beneficial());
            obj.addProperty("icon", def.icon());
            JsonArray tags = new JsonArray();
            for (String t : def.tags()) {
                tags.add(t);
            }
            obj.add("tags", tags);
            arr.add(obj);
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "NutrientRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void repairConfigFile(Path file) {
        if (INSTANCE.size() == 0) {
            return;
        }
        try {
            if (Files.exists(file) && !isInvalidConfig(readConfigArray(file))) {
                return;
            }
            writeNutrientsToPath(file, INSTANCE.values());
            Nourished.LOGGER.info("[NutrientRegistry] Repaired nutrients.json on disk ({} entries)", INSTANCE.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientRegistry] Failed to repair nutrients.json", e);
        }
    }

    private static JsonArray readConfigArray(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            return GSON.fromJson(r, JsonArray.class);
        }
    }

    private static String resolveIcon(String key) {
        String normalizedKey = Objects.requireNonNull(key, "key");
        ResourceLocation candidate = ResourceLocation.tryParse(normalizedKey);
        if (candidate != null && BuiltInRegistries.ITEM.containsKey(candidate)) {
            return candidate.toString();
        }
        ResourceLocation minecraftCandidate = ResourceLocation.tryParse("minecraft:" + normalizedKey);
        if (minecraftCandidate != null && BuiltInRegistries.ITEM.containsKey(minecraftCandidate)) {
            return minecraftCandidate.toString();
        }
        return fallbackIconForKey(normalizedKey);
    }

    private static String fallbackIconForKey(String key) {
        return ICON_FALLBACKS.getOrDefault(key, DEFAULT_ICON);
    }

    private static String bundledNutrientResource(String nutrientId) {
        return "/data/" + Nourished.MODID + "/" + Nourished.MODID + "/nutrients/" + nutrientId + ".json";
    }

    private static List<NutrientDef> loadBundledDefaults() {
        List<NutrientDef> defaults = new ArrayList<>();
        for (String path : DEFAULT_NUTRIENT_RESOURCES) {
            String resource = bundledNutrientResource(path);
            try (InputStream in = NutrientRegistry.class.getResourceAsStream(resource)) {
                if (in == null) {
                    Nourished.LOGGER.warn("[NutrientRegistry] Missing bundled nutrient resource: {}", resource);
                    continue;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                    if (obj == null) {
                        continue;
                    }
                    String key = path;
                    String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : key;
                    int color = obj.has("color") ? obj.get("color").getAsInt() : DEFAULT_COLOR;
                    float defaultDecayRate = obj.has("default_decay_rate") ? obj.get("default_decay_rate").getAsFloat() : DEFAULT_DECAY_RATE;
                    float criticalThreshold = obj.has("critical_threshold") ? obj.get("critical_threshold").getAsFloat() : DEFAULT_CRITICAL_THRESHOLD;
                    float lowThreshold = obj.has("low_threshold") ? obj.get("low_threshold").getAsFloat() : DEFAULT_LOW_THRESHOLD;
                    float excessThreshold = obj.has("excess_threshold") ? obj.get("excess_threshold").getAsFloat() : DEFAULT_EXCESS_THRESHOLD;
                    boolean beneficial = !obj.has("beneficial") || obj.get("beneficial").getAsBoolean();
                    String icon = obj.has("icon") ? obj.get("icon").getAsString() : resolveIcon(key);
                    List<String> tags = List.of(Nourished.MODID + ":nutrients/" + key);
                    defaults.add(new NutrientDef(key, displayName, color, defaultDecayRate, criticalThreshold, lowThreshold, excessThreshold, icon, tags, beneficial));
                }
            } catch (IOException e) {
                Nourished.LOGGER.warn("[NutrientRegistry] Failed to load bundled nutrient {}: {}", path, e.getMessage());
            }
        }
        return defaults;
    }
}
