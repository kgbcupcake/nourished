package dev.maire.nourished.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.maire.nourished.nutrition.Nourished;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data-driven mod compatibility engine for NeoForge 1.21.1.
 * <p>
 * Three-tier priority system:
 * <ol>
 *   <li>Nourished built-in (data/nourished/compat/compat_registry.json)</li>
 *   <li>Mod-provided (data/&lt;modid&gt;/nourished_compat.json)</li>
 *   <li>Modpack override (config/nourished/compat_overrides.json)</li>
 * </ol>
 */
public final class ModCompat {

    private static final Logger LOGGER = Nourished.LOGGER;
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile boolean initialized = false;

    private static final Map<String, CompatEntry> ENTRIES_BY_MODID = new LinkedHashMap<>();
    private static final Map<String, String> NAMESPACE_TO_MODID = new HashMap<>();
    private static final Set<String> BUILT_IN_MODIDS = new LinkedHashSet<>();

    public static boolean ANY_EFFECTS_CONFLICT = false;
    public static boolean ANY_SURVIVAL_OVERHAUL_LOADED = false;
    public static boolean ANY_DECAY_CONFLICT = false;
    public static ConflictBehavior MERGED_CONFLICT_BEHAVIOR = ConflictBehavior.NONE;

    public static boolean LSO_LOADED;
    public static boolean CROPTOPIA_LOADED;
    public static boolean FARMERS_LOADED;
    public static boolean PAMS_LOADED;
    public static boolean MAMAS_LOADED;
    public static boolean SERENE_LOADED;

    private ModCompat() {}

    /**
     * Initialize the compat registry. Call once during mod initialization.
     * Loads all three tiers and resolves runtime data.
     */
    public static void initialize() {
        LSO_LOADED = ModList.get().isLoaded("legendarysurvivaloverhaul");
        CROPTOPIA_LOADED = ModList.get().isLoaded("croptopia");
        FARMERS_LOADED = ModList.get().isLoaded("farmersdelight");
        PAMS_LOADED = ModList.get().isLoaded("pamhc2foodcore");
        MAMAS_LOADED = ModList.get().isLoaded("mamasherbs");
        SERENE_LOADED = ModList.get().isLoaded("sereneseasons");

        if (initialized) return;
        initialized = true;

        LOGGER.info("[Nourished] Initializing mod compatibility registry...");

        Map<String, JsonCompatEntry> merged = new LinkedHashMap<>();

        loadTier1BuiltIn(merged);
        loadTier2ModProvided(merged);
        loadTier3ModpackOverride(merged);
        includeLoadedModsAsFallback(merged);

        resolveRuntimeData(merged);
        buildNamespaceMap();
        computeAggregateFlags();
        logCompatReport();
    }

    /**
     * Ensure every currently loaded mod appears in compat config, even if no explicit entry exists.
     * This keeps the compatibility screen complete for large modpacks.
     */
    private static void includeLoadedModsAsFallback(Map<String, JsonCompatEntry> merged) {
        for (IModInfo modInfo : ModList.get().getMods()) {
            String modId = modInfo.getModId();
            if ("minecraft".equals(modId) || "neoforge".equals(modId) || "nourished".equals(modId)) {
                continue;
            }
            if (!merged.containsKey(modId)) {
                merged.put(modId, new JsonCompatEntry(
                        modId,
                        modId,
                        CompatCategory.UNKNOWN,
                        List.of(modId),
                        false,
                        false,
                        Map.of(),
                        null,
                        false,
                        0
                ));
            }
        }
    }

    /**
     * Call after item registry is available to discover unknown food mods.
     * Safe to call multiple times; only runs discovery once.
     */
    public static void discoverUnknownMods() {
        if (!initialized) {
            LOGGER.warn("[Nourished] ModCompat.discoverUnknownMods() called before initialize()");
            return;
        }

        Set<String> knownNamespaces = new HashSet<>(NAMESPACE_TO_MODID.keySet());
        knownNamespaces.add("minecraft");
        knownNamespaces.add("nourished");

        Map<String, Set<String>> unknownFoodNamespaces = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            @SuppressWarnings("null")
            boolean isFood = item.components().has(DataComponents.FOOD);
            if (isFood) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                String namespace = id.getNamespace();

                if (!knownNamespaces.contains(namespace) && !unknownFoodNamespaces.containsKey(namespace)) {
                    String modId = findModIdForNamespace(namespace);
                    if (modId != null && !ENTRIES_BY_MODID.containsKey(modId)) {
                        unknownFoodNamespaces.computeIfAbsent(modId, k -> new HashSet<>()).add(namespace);
                    }
                }
            }
        }

        for (Map.Entry<String, Set<String>> entry : unknownFoodNamespaces.entrySet()) {
            String modId = entry.getKey();
            Set<String> namespaces = entry.getValue();

            CompatEntry unknown = CompatEntry.createUnknown(modId, new ArrayList<String>(namespaces));
            ENTRIES_BY_MODID.put(modId, unknown);

            for (String ns : namespaces) {
                NAMESPACE_TO_MODID.put(ns, modId);
            }

            LOGGER.warn("[Nourished] Unknown mod '{}' has food items but no compat entry — using fallback classification", modId);
        }

        if (!unknownFoodNamespaces.isEmpty()) {
            LOGGER.info("[Nourished] Unknown food mods (fallback): {} ({} mods)",
                    String.join(", ", unknownFoodNamespaces.keySet()),
                    unknownFoodNamespaces.size());
        }
    }

    @Nullable
    private static String findModIdForNamespace(String namespace) {
        return ModList.get().getMods().stream()
                .filter(mod -> mod.getModId().equals(namespace) ||
                        mod.getModId().replace("_", "").equals(namespace.replace("_", "")))
                .map(IModInfo::getModId)
                .findFirst()
                .orElse(namespace);
    }

    private static void loadTier1BuiltIn(Map<String, JsonCompatEntry> merged) {
        try (InputStream is = ModCompat.class.getResourceAsStream("/data/nourished/compat/compat_registry.json")) {
            if (is == null) {
                LOGGER.warn("[Nourished] Built-in compat_registry.json not found in jar");
                return;
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                CompatRegistry registry = GSON.fromJson(reader, CompatRegistry.class);
                if (registry != null && registry.entries() != null) {
                    for (JsonCompatEntry entry : registry.entries()) {
                        if (entry.modId() != null) {
                            merged.put(entry.modId(), entry);
                            BUILT_IN_MODIDS.add(entry.modId());
                        }
                    }
                    LOGGER.debug("[Nourished] Tier 1 (built-in): loaded {} entries", registry.entries().size());
                }
            }
        } catch (IOException e) {
            LOGGER.error("[Nourished] Failed to load built-in compat_registry.json", e);
        }
    }

    private static void loadTier2ModProvided(Map<String, JsonCompatEntry> merged) {
        int count = 0;
        for (IModInfo modInfo : ModList.get().getMods()) {
            String modId = modInfo.getModId();
            if ("minecraft".equals(modId) || "nourished".equals(modId) || "neoforge".equals(modId)) {
                continue;
            }

            String resourcePath = "/data/" + modId + "/nourished_compat.json";
            try (InputStream is = ModCompat.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        CompatRegistry registry = GSON.fromJson(reader, CompatRegistry.class);
                        if (registry != null && registry.entries() != null) {
                            for (JsonCompatEntry entry : registry.entries()) {
                                if (entry.modId() != null) {
                                    mergeEntry(merged, entry);
                                    count++;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("[Nourished] No compat file from mod {}", modId);
            }
        }
        if (count > 0) {
            LOGGER.debug("[Nourished] Tier 2 (mod-provided): merged {} entries", count);
        }
    }

    private static void loadTier3ModpackOverride(Map<String, JsonCompatEntry> merged) {
        Path overridePath = FMLPaths.CONFIGDIR.get().resolve("nourished").resolve("compat_overrides.json");
        if (!Files.exists(overridePath)) {
            LOGGER.debug("[Nourished] Tier 3 (modpack override): no compat_overrides.json found");
            return;
        }

        try (Reader reader = Files.newBufferedReader(overridePath, StandardCharsets.UTF_8)) {
            CompatRegistry registry = GSON.fromJson(reader, CompatRegistry.class);
            if (registry != null && registry.entries() != null) {
                for (JsonCompatEntry entry : registry.entries()) {
                    if (entry.modId() != null) {
                        mergeEntry(merged, entry);
                    }
                }
                LOGGER.debug("[Nourished] Tier 3 (modpack override): merged {} entries", registry.entries().size());
            }
        } catch (IOException e) {
            LOGGER.error("[Nourished] Failed to load compat_overrides.json", e);
        }
    }

    private static void mergeEntry(Map<String, JsonCompatEntry> merged, JsonCompatEntry newEntry) {
        JsonCompatEntry existing = merged.get(newEntry.modId());
        if (existing == null) {
            merged.put(newEntry.modId(), newEntry);
            return;
        }

        List<String> mergedNamespaces = new ArrayList<>(existing.namespaces() != null ? existing.namespaces() : List.of());
        if (newEntry.namespaces() != null) {
            for (String ns : newEntry.namespaces()) {
                if (!mergedNamespaces.contains(ns)) {
                    mergedNamespaces.add(ns);
                }
            }
        }

        JsonCompatEntry mergedEntry = new JsonCompatEntry(
                newEntry.modId(),
                newEntry.displayName() != null ? newEntry.displayName() : existing.displayName(),
                newEntry.category() != null ? newEntry.category() : existing.category(),
                mergedNamespaces,
                newEntry.providesFoodTags() || existing.providesFoodTags(),
                newEntry.handlesOwnNutrition() || existing.handlesOwnNutrition(),
                newEntry.versionRanges() != null && !newEntry.versionRanges().isEmpty()
                        ? newEntry.versionRanges() : existing.versionRanges(),
                newEntry.conflictBehavior() != null ? newEntry.conflictBehavior() : existing.conflictBehavior(),
                newEntry.softCompat() || existing.softCompat(),
                newEntry.priority() > 0 ? newEntry.priority() : existing.priority()
        );

        merged.put(newEntry.modId(), mergedEntry);
    }

    private static void resolveRuntimeData(Map<String, JsonCompatEntry> merged) {
        for (Map.Entry<String, JsonCompatEntry> entry : merged.entrySet()) {
            String modId = entry.getKey();
            JsonCompatEntry json = entry.getValue();

            boolean loaded = ModList.get().isLoaded(modId);
            String detectedVersion = null;
            ConflictLevel resolvedLevel = ConflictLevel.NONE;

            if (loaded) {
                Optional<? extends IModInfo> modInfo = ModList.get().getMods().stream()
                        .filter(m -> m.getModId().equals(modId))
                        .findFirst();

                if (modInfo.isPresent()) {
                    ArtifactVersion version = modInfo.get().getVersion();
                    detectedVersion = version.toString();
                    resolvedLevel = resolveVersionConflict(detectedVersion, json.versionRanges());
                }
            }

            CompatEntry resolved = json.toCompatEntry(loaded, detectedVersion, resolvedLevel);
            ENTRIES_BY_MODID.put(modId, resolved);
        }
    }

    private static ConflictLevel resolveVersionConflict(@Nullable String detectedVersion, @Nullable Map<String, ConflictLevel> versionRanges) {
        if (versionRanges == null || versionRanges.isEmpty()) {
            return ConflictLevel.NONE;
        }

        if (detectedVersion == null) {
            return ConflictLevel.PARTIAL_CONFLICT;
        }

        SemVer detected = SemVer.parse(detectedVersion);
        if (detected == null) {
            LOGGER.warn("[Nourished] Could not parse mod version '{}' as semver, assuming partial conflict", detectedVersion);
            return ConflictLevel.PARTIAL_CONFLICT;
        }

        for (Map.Entry<String, ConflictLevel> range : versionRanges.entrySet()) {
            if (detected.satisfies(range.getKey())) {
                return range.getValue();
            }
        }

        return ConflictLevel.NONE;
    }

    private static void buildNamespaceMap() {
        NAMESPACE_TO_MODID.clear();
        for (CompatEntry entry : ENTRIES_BY_MODID.values()) {
            for (String namespace : entry.namespaces()) {
                NAMESPACE_TO_MODID.put(namespace, entry.modId());
            }
        }
    }

    private static void computeAggregateFlags() {
        ConflictBehavior merged = ConflictBehavior.NONE;
        boolean anyEffects = false;
        boolean anySurvival = false;
        boolean anyDecay = false;

        for (CompatEntry entry : ENTRIES_BY_MODID.values()) {
            if (!entry.loaded()) continue;

            if (entry.category() == CompatCategory.SURVIVAL_OVERHAUL) {
                anySurvival = true;
            }

            if (entry.conflictBehavior() != null &&
                    entry.resolvedConflictLevel() != ConflictLevel.NONE) {
                merged = merged.merge(entry.conflictBehavior());

                if (entry.conflictBehavior().disableEffects()) {
                    anyEffects = true;
                }
                if (entry.conflictBehavior().disableDecay()) {
                    anyDecay = true;
                }
            }
        }

        ANY_EFFECTS_CONFLICT = anyEffects;
        ANY_SURVIVAL_OVERHAUL_LOADED = anySurvival;
        ANY_DECAY_CONFLICT = anyDecay;
        MERGED_CONFLICT_BEHAVIOR = merged;
    }

    private static void logCompatReport() {
        List<CompatEntry> survivalOverhauls = getLoadedByCategory(CompatCategory.SURVIVAL_OVERHAUL);
        List<CompatEntry> foodMods = getLoadedByCategory(CompatCategory.FOOD_MOD);
        List<CompatEntry> farmingMods = getLoadedByCategory(CompatCategory.FARMING_MOD);

        LOGGER.info("[Nourished] Compat Registry loaded — 3 tiers merged");

        if (!survivalOverhauls.isEmpty()) {
            String survivalList = survivalOverhauls.stream()
                    .map(e -> {
                        String name = e.modId();
                        if (e.detectedVersion() != null) {
                            name += " (v" + e.detectedVersion();
                            if (e.conflictBehavior() != null && e.conflictBehavior().disableEffects()) {
                                name += " — effects disabled";
                            }
                            name += ")";
                        }
                        return name;
                    })
                    .collect(Collectors.joining(", "));
            LOGGER.info("[Nourished] Survival overhauls detected: {}", survivalList);
        }

        if (!foodMods.isEmpty()) {
            String foodList = foodMods.stream()
                    .map(CompatEntry::modId)
                    .collect(Collectors.joining(", "));
            LOGGER.info("[Nourished] Food mods detected: {} ({} mods)", foodList, foodMods.size());
        }

        if (!farmingMods.isEmpty()) {
            String farmList = farmingMods.stream()
                    .map(CompatEntry::modId)
                    .collect(Collectors.joining(", "));
            LOGGER.info("[Nourished] Farming mods detected: {} ({} mods)", farmList, farmingMods.size());
        }

        LOGGER.info("[Nourished] Merged conflict behavior: disableEffects={}, disableDecay={}",
                MERGED_CONFLICT_BEHAVIOR.disableEffects(),
                MERGED_CONFLICT_BEHAVIOR.disableDecay());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Public Query API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a map of all registered mod IDs to their loaded status.
     * Derived dynamically from the registry — always in sync.
     */
    public static Map<String, Boolean> getDetected() {
        return ENTRIES_BY_MODID.values().stream()
                .collect(Collectors.toMap(CompatEntry::modId, CompatEntry::loaded));
    }

    /**
     * Maps an item namespace to the modid key used in compat config.
     * Returns the namespace as-is if it's a tracked mod, null if not tracked.
     */
    public static @Nullable String namespaceToModid(String namespace) {
        return NAMESPACE_TO_MODID.get(namespace);
    }

    /**
     * Get the full compat entry for a namespace if it exists.
     */
    public static Optional<CompatEntry> entryForNamespace(String namespace) {
        String modId = NAMESPACE_TO_MODID.get(namespace);
        return modId != null ? Optional.ofNullable(ENTRIES_BY_MODID.get(modId)) : Optional.empty();
    }

    /**
     * Get the full compat entry for a mod if it exists in the registry.
     */
    public static Optional<CompatEntry> getEntry(String modId) {
        return Optional.ofNullable(ENTRIES_BY_MODID.get(modId));
    }

    /**
     * Is this mod loaded?
     */
    public static boolean isLoaded(String modId) {
        CompatEntry entry = ENTRIES_BY_MODID.get(modId);
        return entry != null && entry.loaded();
    }

    /**
     * Is this mod loaded AND has a conflict at or above the given level?
     */
    public static boolean hasConflict(String modId, ConflictLevel minimum) {
        CompatEntry entry = ENTRIES_BY_MODID.get(modId);
        if (entry == null || !entry.loaded()) return false;
        return entry.resolvedConflictLevel().ordinal() >= minimum.ordinal();
    }

    /**
     * Get all loaded entries by category.
     */
    public static List<CompatEntry> getLoadedByCategory(CompatCategory category) {
        return ENTRIES_BY_MODID.values().stream()
                .filter(e -> e.loaded() && e.category() == category)
                .toList();
    }

    /**
     * Get all loaded entries that provide food tags.
     */
    public static List<CompatEntry> getFoodTagProviders() {
        return ENTRIES_BY_MODID.values().stream()
                .filter(e -> e.loaded() && e.providesFoodTags())
                .toList();
    }

    /**
     * Should Nourished disable effects given current loaded mods?
     */
    public static boolean shouldDisableEffects() {
        return MERGED_CONFLICT_BEHAVIOR.disableEffects();
    }

    /**
     * Should Nourished disable decay given current loaded mods?
     */
    public static boolean shouldDisableDecay() {
        return MERGED_CONFLICT_BEHAVIOR.disableDecay();
    }

    /**
     * Full compat report for config screen display.
     */
    public static List<CompatReportEntry> getCompatReport() {
        return ENTRIES_BY_MODID.values().stream()
                .sorted(Comparator
                        .comparing((CompatEntry e) -> !e.loaded())
                        .thenComparing(e -> e.category().ordinal())
                        .thenComparing(CompatEntry::modId))
                .map(e -> CompatReportEntry.from(e, MERGED_CONFLICT_BEHAVIOR))
                .toList();
    }

    /**
     * Get all registered entries (for debugging/inspection).
     */
    public static Collection<CompatEntry> getAllEntries() {
        return Collections.unmodifiableCollection(ENTRIES_BY_MODID.values());
    }

    /**
     * Built-in tier 1 entries shipped directly by Nourished (compat_registry.json).
     */
    public static List<CompatEntry> getBuiltInEntries() {
        return BUILT_IN_MODIDS.stream()
                .map(ENTRIES_BY_MODID::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((CompatEntry e) -> !e.loaded())
                        .thenComparing(e -> e.category().ordinal())
                        .thenComparing(CompatEntry::modId))
                .toList();
    }
}
