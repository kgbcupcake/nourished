package dev.maire.nourished.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.api.CompatDefinition;
import dev.maire.nourished.api.DietProfileDefinition;
import dev.maire.nourished.api.EffectDefinition;
import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NourishedAPIState;
import dev.maire.nourished.api.NutrientDefinition;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.api.NutrientSynergyDefinition;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.registry.NourishedApiDefinitionRegistries;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads Nourished datapack definitions from {@code data/<namespace>/nourished/**}.
 */
public final class NourishedDataLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    private volatile Set<ResourceLocation> loadedNutrients = Set.of();
    private volatile Set<ResourceLocation> loadedFoodClassifications = Set.of();
    private volatile Set<ResourceLocation> loadedEffects = Set.of();
    private volatile Set<ResourceLocation> loadedSynergies = Set.of();
    private volatile Set<ResourceLocation> loadedFoodSynergies = Set.of();
    private volatile Set<ResourceLocation> loadedMilestones = Set.of();
    private volatile Set<ResourceLocation> loadedProfiles = Set.of();
    private volatile Set<ResourceLocation> loadedCompatEntries = Set.of();

    public NourishedDataLoader() {
        super(GSON, DatapackSchema.ROOT);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> allJson, net.minecraft.server.packs.resources.ResourceManager resourceManager, ProfilerFiller profiler) {
        try (NourishedAPIState.DatapackReloadScope scope = NourishedAPIState.openForDatapackReload()) {
            DatapackDiagnostics diagnostics = DatapackDiagnostics.getInstance();
            diagnostics.clear();

            NourishedApiDefinitionRegistries.onDatapackApplyBegin();

            Set<ResourceLocation> nextNutrients = new LinkedHashSet<>();
            Set<ResourceLocation> nextFoodClassifications = new LinkedHashSet<>();
            Set<ResourceLocation> nextEffects = new LinkedHashSet<>();
            Set<ResourceLocation> nextSynergies = new LinkedHashSet<>();
            Set<ResourceLocation> nextFoodSynergies = new LinkedHashSet<>();
            Set<ResourceLocation> nextMilestones = new LinkedHashSet<>();
            Set<ResourceLocation> nextProfiles = new LinkedHashSet<>();
            Set<ResourceLocation> nextCompatEntries = new LinkedHashSet<>();

            Map<ResourceLocation, JsonObject> nutrients = filterDirectory(allJson, DatapackSchema.NUTRIENTS_DIR);
            Map<ResourceLocation, JsonObject> foodClassifications = filterDirectory(allJson, DatapackSchema.FOOD_CLASSIFICATIONS_DIR);
            Map<ResourceLocation, JsonObject> effects = filterDirectory(allJson, DatapackSchema.EFFECTS_DIR);
            Map<ResourceLocation, JsonObject> synergies = filterDirectory(allJson, DatapackSchema.SYNERGIES_DIR);
            Map<ResourceLocation, JsonObject> foodSynergies = filterDirectory(allJson, DatapackSchema.FOOD_SYNERGIES_DIR);
            Map<ResourceLocation, JsonObject> milestones = filterDirectory(allJson, DatapackSchema.MILESTONES_DIR);
            Map<ResourceLocation, JsonObject> profiles = filterDirectory(allJson, DatapackSchema.DIET_PROFILES_DIR);
            Map<ResourceLocation, JsonObject> compat = filterDirectory(allJson, DatapackSchema.COMPAT_DIR);
            Map<ResourceLocation, JsonObject> foodFamilies = filterDirectory(allJson, DatapackSchema.FOOD_FAMILIES_DIR);
            Map<ResourceLocation, JsonObject> moduleLocks = filterDirectory(allJson, DatapackSchema.MODULE_LOCKS_DIR);

        for (Map.Entry<ResourceLocation, JsonObject> entry : nutrients.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.NUTRIENTS_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forNutrient(), filePath, diagnostics)) {
                continue;
            }
            try {
                NutrientDefinition def = parseNutrient(fileId, json);
                NourishedAPI.registerNutrient(def);
                nextNutrients.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : foodClassifications.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.FOOD_CLASSIFICATIONS_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forFoodClassification(), filePath, diagnostics)) {
                continue;
            }
            try {
                registerFoodClassification(fileId, json);
                nextFoodClassifications.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : effects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.EFFECTS_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forEffect(), filePath, diagnostics)) {
                continue;
            }
            try {
                EffectDefinition def = parseEffect(fileId, json);
                NourishedAPI.registerCustomEffect(def);
                nextEffects.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : synergies.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.SYNERGIES_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forSynergy(), filePath, diagnostics)) {
                continue;
            }
            try {
                NutrientSynergyDefinition def = parseSynergy(fileId, json);
                NourishedAPI.registerNutrientSynergy(def);
                nextSynergies.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : foodSynergies.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.FOOD_SYNERGIES_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forFoodSynergy(), filePath, diagnostics)) {
                continue;
            }
            try {
                FoodSynergyDefinition def = parseFoodSynergy(fileId, json);
                NourishedAPI.registerFoodSynergy(def);
                nextFoodSynergies.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : milestones.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.MILESTONES_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forMilestone(), filePath, diagnostics)) {
                continue;
            }
            try {
                NutrientMilestoneDefinition def = parseMilestone(fileId, json);
                NourishedAPI.registerMilestone(def);
                nextMilestones.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : profiles.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.DIET_PROFILES_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forDietProfile(), filePath, diagnostics)) {
                continue;
            }
            try {
                DietProfileDefinition def = parseDietProfile(fileId, json);
                NourishedAPI.registerDietProfile(def);
                nextProfiles.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : compat.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.COMPAT_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forCompat(), filePath, diagnostics)) {
                continue;
            }
            try {
                CompatDefinition def = parseCompat(fileId, json);
                NourishedAPI.registerCompatEntry(def);
                nextCompatEntries.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : foodFamilies.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.FOOD_FAMILIES_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forFoodFamilies(), filePath, diagnostics)) {
                continue;
            }
            try {
                applyFoodFamilies(json);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : moduleLocks.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();
            String filePath = datapackFilePath(DatapackSchema.MODULE_LOCKS_DIR, fileId);
            if (!isValidForRegistration(json, SchemaDefinition.forModuleLocks(), filePath, diagnostics)) {
                continue;
            }
            try {
                applyModuleLocks(json);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

            NourishedApiDefinitionRegistries.onDatapackApplyEnd();

            loadedNutrients = Collections.unmodifiableSet(nextNutrients);
            loadedFoodClassifications = Collections.unmodifiableSet(nextFoodClassifications);
            loadedEffects = Collections.unmodifiableSet(nextEffects);
            loadedSynergies = Collections.unmodifiableSet(nextSynergies);
            loadedFoodSynergies = Collections.unmodifiableSet(nextFoodSynergies);
            loadedMilestones = Collections.unmodifiableSet(nextMilestones);
            loadedProfiles = Collections.unmodifiableSet(nextProfiles);
            loadedCompatEntries = Collections.unmodifiableSet(nextCompatEntries);
        }
    }

    private static boolean isValidForRegistration(JsonObject json, SchemaDefinition schema, String filePath, DatapackDiagnostics diagnostics) {
        List<DatapackDiagnostic> fileDiagnostics = DatapackValidator.validate(json, schema, filePath);
        boolean hasErrors = false;
        for (DatapackDiagnostic diagnostic : fileDiagnostics) {
            diagnostics.record(diagnostic);
            if (diagnostic.severity() == DatapackDiagnostic.Severity.ERROR) {
                hasErrors = true;
            }
        }
        return !hasErrors;
    }

    private static String datapackFilePath(String directory, ResourceLocation fileId) {
        return "data/" + fileId.getNamespace() + "/" + DatapackSchema.ROOT + "/" + directory + "/" + fileId.getPath() + ".json";
    }

    public Set<ResourceLocation> getLoadedNutrients() {
        return loadedNutrients;
    }

    public Set<ResourceLocation> getLoadedFoodClassifications() {
        return loadedFoodClassifications;
    }

    public Set<ResourceLocation> getLoadedEffects() {
        return loadedEffects;
    }

    public Set<ResourceLocation> getLoadedSynergies() {
        return loadedSynergies;
    }

    public Set<ResourceLocation> getLoadedFoodSynergies() {
        return loadedFoodSynergies;
    }

    public Set<ResourceLocation> getLoadedMilestones() {
        return loadedMilestones;
    }

    public Set<ResourceLocation> getLoadedProfiles() {
        return loadedProfiles;
    }

    public Set<ResourceLocation> getLoadedCompatEntries() {
        return loadedCompatEntries;
    }

    private static Map<ResourceLocation, JsonObject> filterDirectory(Map<ResourceLocation, JsonElement> allJson, String directory) {
        String prefix = directory + "/";
        Map<ResourceLocation, JsonObject> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : allJson.entrySet()) {
            if (!entry.getKey().getPath().startsWith(prefix)) {
                continue;
            }
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("Expected JSON object");
            }
            String withoutPrefix = entry.getKey().getPath().substring(prefix.length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), withoutPrefix);
            result.put(id, entry.getValue().getAsJsonObject());
        }
        return result;
    }

    private static NutrientDefinition parseNutrient(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        String displayName = getRequiredString(json, DatapackSchema.KEY_DISPLAY_NAME);
        NutrientDefinition.Builder builder = NutrientDefinition.builder(id).displayName(displayName);
        if (json.has("color")) {
            builder.color(json.get("color").getAsInt());
        }
        if (json.has("default_decay_rate")) {
            builder.defaultDecayRate(json.get("default_decay_rate").getAsFloat());
        }
        if (json.has("critical_threshold")) {
            builder.criticalThreshold(json.get("critical_threshold").getAsFloat());
        }
        if (json.has("low_threshold")) {
            builder.lowThreshold(json.get("low_threshold").getAsFloat());
        }
        if (json.has("excess_threshold")) {
            builder.excessThreshold(json.get("excess_threshold").getAsFloat());
        }
        return builder.build();
    }

    private static void registerFoodClassification(ResourceLocation fileId, JsonObject json) {
        String nutrientKey = getRequiredString(json, DatapackSchema.KEY_NUTRIENT_KEY);
        float amount = getRequiredFloat(json, DatapackSchema.KEY_AMOUNT);

        if (json.has(DatapackSchema.KEY_ITEM)) {
            ResourceLocation itemId = ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_ITEM));
            NourishedAPI.registerFoodClassification(itemId, nutrientKey, amount);
            return;
        }

        if (json.has(DatapackSchema.KEY_TAG)) {
            String rawTag = getRequiredString(json, DatapackSchema.KEY_TAG);
            String normalized = rawTag.startsWith("#") ? rawTag.substring(1) : rawTag;
            ResourceLocation tagId = ResourceLocation.parse(normalized);
            TagKey<net.minecraft.world.item.Item> key = ItemTags.create(tagId);
            Iterable<Holder<net.minecraft.world.item.Item>> tagged = BuiltInRegistries.ITEM.getTagOrEmpty(key);
            int matched = 0;
            for (Holder<net.minecraft.world.item.Item> holder : tagged) {
                ResourceLocation itemId = NourishedRegistryUtils.itemKey(holder.value());
                if (itemId != null) {
                    NourishedAPI.registerFoodClassification(itemId, nutrientKey, amount);
                    matched++;
                }
            }
            if (matched == 0) {
                throw new IllegalArgumentException("Tag has no registered items: " + rawTag);
            }
            return;
        }

        throw new IllegalArgumentException("Entry must include either 'item' or 'tag'");
    }

    private static EffectDefinition parseEffect(ResourceLocation fileId, JsonObject json) throws Exception {
        String nutrientKey = getRequiredString(json, DatapackSchema.KEY_NUTRIENT_KEY);
        float threshold = getRequiredFloat(json, DatapackSchema.KEY_THRESHOLD);
        String thresholdTypeName = getRequiredString(json, DatapackSchema.KEY_THRESHOLD_TYPE);
        ResourceLocation effectId = ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_EFFECT_ID));
        int amplifier = getOptionalInt(json, DatapackSchema.KEY_AMPLIFIER, 0);
        int duration = getOptionalInt(json, DatapackSchema.KEY_DURATION, 200);
        EffectDefinition.ThresholdType thresholdType = EffectDefinition.ThresholdType.valueOf(thresholdTypeName.toUpperCase(Locale.ROOT));
        return EffectDefinition.builder()
                .nutrientKey(nutrientKey)
                .threshold(threshold)
                .thresholdType(thresholdType)
                .effectId(effectId)
                .amplifier(amplifier)
                .duration(duration)
                .build();
    }

    private static NutrientSynergyDefinition parseSynergy(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        String nutrientA = getRequiredString(json, DatapackSchema.KEY_NUTRIENT_A_KEY);
        String nutrientB = getRequiredString(json, DatapackSchema.KEY_NUTRIENT_B_KEY);
        NutrientSynergyDefinition.LevelCondition conditionA = NutrientSynergyDefinition.LevelCondition.valueOf(
                getRequiredString(json, DatapackSchema.KEY_NUTRIENT_A_CONDITION).toUpperCase(Locale.ROOT));
        NutrientSynergyDefinition.LevelCondition conditionB = NutrientSynergyDefinition.LevelCondition.valueOf(
                getRequiredString(json, DatapackSchema.KEY_NUTRIENT_B_CONDITION).toUpperCase(Locale.ROOT));
        NutrientSynergyDefinition.Builder builder = NutrientSynergyDefinition.builder(id)
                .nutrientA(nutrientA, conditionA)
                .nutrientB(nutrientB, conditionB);
        if (json.has(DatapackSchema.KEY_BONUS_EFFECT_ID)) {
            builder.bonusEffect(ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_BONUS_EFFECT_ID)));
        }
        builder.effectAmplifier(getOptionalInt(json, DatapackSchema.KEY_AMPLIFIER, 0));
        builder.effectDuration(getOptionalInt(json, DatapackSchema.KEY_EFFECT_DURATION, 200));
        builder.penalty(getOptionalBoolean(json, DatapackSchema.KEY_IS_PENALTY, false));
        return builder.build();
    }

    private static FoodSynergyDefinition parseFoodSynergy(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        return FoodSynergyDefinition.builder(id)
                .foodA(ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_FOOD_A)))
                .foodB(ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_FOOD_B)))
                .timeWindowTicks(getOptionalInt(json, DatapackSchema.KEY_TIME_WINDOW_TICKS, 100))
                .bonusNutrientKey(getRequiredString(json, DatapackSchema.KEY_BONUS_NUTRIENT_KEY))
                .bonusAmount(getRequiredFloat(json, DatapackSchema.KEY_BONUS_AMOUNT))
                .build();
    }

    private static NutrientMilestoneDefinition parseMilestone(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        NutrientMilestoneDefinition.Builder builder = NutrientMilestoneDefinition.builder(id)
                .nutrientKey(getRequiredString(json, DatapackSchema.KEY_NUTRIENT_KEY))
                .cumulativeGoal(getRequiredFloat(json, DatapackSchema.KEY_CUMULATIVE_GOAL));
        if (json.has(DatapackSchema.KEY_REWARD_EFFECT_ID)) {
            builder.rewardEffect(ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_REWARD_EFFECT_ID)));
        }
        builder.rewardAmplifier(getOptionalInt(json, DatapackSchema.KEY_AMPLIFIER, 0));
        builder.rewardDuration(getOptionalInt(json, DatapackSchema.KEY_REWARD_DURATION, 200));
        if (json.has(DatapackSchema.KEY_ADVANCEMENT_ID)) {
            builder.advancement(ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_ADVANCEMENT_ID)));
        }
        return builder.build();
    }

    private static DietProfileDefinition parseDietProfile(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        DietProfileDefinition.Builder builder = DietProfileDefinition.builder(id)
                .displayName(getRequiredString(json, DatapackSchema.KEY_DISPLAY_NAME));
        if (json.has(DatapackSchema.KEY_DESCRIPTION)) {
            builder.description(json.get(DatapackSchema.KEY_DESCRIPTION).getAsString());
        }
        if (json.has(DatapackSchema.KEY_CUSTOM_THRESHOLDS) && json.get(DatapackSchema.KEY_CUSTOM_THRESHOLDS).isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject(DatapackSchema.KEY_CUSTOM_THRESHOLDS).entrySet()) {
                builder.customThreshold(e.getKey(), e.getValue().getAsFloat());
            }
        }
        if (json.has(DatapackSchema.KEY_CUSTOM_DECAY_RATES) && json.get(DatapackSchema.KEY_CUSTOM_DECAY_RATES).isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject(DatapackSchema.KEY_CUSTOM_DECAY_RATES).entrySet()) {
                builder.customDecayRate(e.getKey(), e.getValue().getAsFloat());
            }
        }
        if (json.has(DatapackSchema.KEY_BONUS_EFFECTS) && json.get(DatapackSchema.KEY_BONUS_EFFECTS).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(DatapackSchema.KEY_BONUS_EFFECTS);
            for (JsonElement element : array) {
                builder.addBonusEffect(ResourceLocation.parse(element.getAsString()));
            }
        }
        return builder.build();
    }

    private static CompatDefinition parseCompat(ResourceLocation fileId, JsonObject json) throws Exception {
        String modId = json.has(DatapackSchema.KEY_MOD_ID) ? json.get(DatapackSchema.KEY_MOD_ID).getAsString() : fileId.getPath();
        String categoryRaw = json.has(DatapackSchema.KEY_CATEGORY) ? json.get(DatapackSchema.KEY_CATEGORY).getAsString() : "FOOD_MOD";
        CompatDefinition.CompatCategory category = CompatDefinition.CompatCategory.valueOf(categoryRaw.toUpperCase(Locale.ROOT));
        Map<ResourceLocation, String> mappings = new LinkedHashMap<>();
        if (json.has(DatapackSchema.KEY_MAPPINGS) && json.get(DatapackSchema.KEY_MAPPINGS).isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject(DatapackSchema.KEY_MAPPINGS).entrySet()) {
                mappings.put(ResourceLocation.parse(e.getKey()), e.getValue().getAsString());
            }
        }
        return CompatDefinition.builder(modId)
                .category(category)
                .addAllFoodTagMappings(mappings)
                .build();
    }

    private static void applyFoodFamilies(JsonObject json) {
        Map<String, List<String>> configured = new LinkedHashMap<>();
        JsonObject families = json.getAsJsonObject(DatapackSchema.KEY_FAMILIES);
        for (Map.Entry<String, JsonElement> entry : families.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            List<String> keywords = new ArrayList<>();
            for (JsonElement keyword : entry.getValue().getAsJsonArray()) {
                keywords.add(keyword.getAsString());
            }
            configured.put(entry.getKey(), keywords);
        }
        FoodFamilyResolver.replaceFamilies(configured);
    }

    private static void applyModuleLocks(JsonObject json) {
        Set<String> locked = new LinkedHashSet<>();
        Set<String> serverOnly = new LinkedHashSet<>();
        if (json.has(DatapackSchema.KEY_LOCKED) && json.get(DatapackSchema.KEY_LOCKED).isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray(DatapackSchema.KEY_LOCKED)) {
                locked.add(element.getAsString());
            }
        }
        if (json.has(DatapackSchema.KEY_SERVER_ONLY) && json.get(DatapackSchema.KEY_SERVER_ONLY).isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray(DatapackSchema.KEY_SERVER_ONLY)) {
                serverOnly.add(element.getAsString());
            }
        }
        LockRegistry.replaceFromDatapack(locked, serverOnly);
    }

    private static String getRequiredString(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return json.get(key).getAsString();
    }

    private static float getRequiredFloat(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return json.get(key).getAsFloat();
    }

    private static int getOptionalInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static boolean getOptionalBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static void warnMalformed(ResourceLocation fileId, Exception ex) {
        Nourished.LOGGER.warn("[Nourished] Skipping malformed datapack entry at data/{}/{}/{}.json: {}", fileId.getNamespace(), DatapackSchema.ROOT, fileId.getPath(), ex.getMessage());
    }
}
