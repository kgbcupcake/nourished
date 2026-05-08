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
import dev.maire.nourished.api.NutrientDefinition;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.api.NutrientSynergyDefinition;
import dev.maire.nourished.nutrition.Nourished;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

        for (Map.Entry<ResourceLocation, JsonObject> entry : nutrients.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                NutrientDefinition def = parseNutrient(fileId, entry.getValue());
                NourishedAPI.registerNutrient(def);
                nextNutrients.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : foodClassifications.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                registerFoodClassification(fileId, entry.getValue());
                nextFoodClassifications.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : effects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                EffectDefinition def = parseEffect(fileId, entry.getValue());
                NourishedAPI.registerCustomEffect(def);
                nextEffects.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : synergies.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                NutrientSynergyDefinition def = parseSynergy(fileId, entry.getValue());
                NourishedAPI.registerNutrientSynergy(def);
                nextSynergies.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : foodSynergies.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                FoodSynergyDefinition def = parseFoodSynergy(fileId, entry.getValue());
                NourishedAPI.registerFoodSynergy(def);
                nextFoodSynergies.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : milestones.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                NutrientMilestoneDefinition def = parseMilestone(fileId, entry.getValue());
                NourishedAPI.registerMilestone(def);
                nextMilestones.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : profiles.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                DietProfileDefinition def = parseDietProfile(fileId, entry.getValue());
                NourishedAPI.registerDietProfile(def);
                nextProfiles.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : compat.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                CompatDefinition def = parseCompat(fileId, entry.getValue());
                NourishedAPI.registerCompatEntry(def);
                nextCompatEntries.add(fileId);
            } catch (Exception ex) {
                warnMalformed(fileId, ex);
            }
        }

        loadedNutrients = Collections.unmodifiableSet(nextNutrients);
        loadedFoodClassifications = Collections.unmodifiableSet(nextFoodClassifications);
        loadedEffects = Collections.unmodifiableSet(nextEffects);
        loadedSynergies = Collections.unmodifiableSet(nextSynergies);
        loadedFoodSynergies = Collections.unmodifiableSet(nextFoodSynergies);
        loadedMilestones = Collections.unmodifiableSet(nextMilestones);
        loadedProfiles = Collections.unmodifiableSet(nextProfiles);
        loadedCompatEntries = Collections.unmodifiableSet(nextCompatEntries);
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

        Map<String, Object> fields = new HashMap<>();
        fields.put("id", id);
        fields.put("displayName", displayName);
        if (json.has("color")) {
            fields.put("color", json.get("color").getAsInt());
        }
        if (json.has("default_decay_rate")) {
            fields.put("defaultDecayRate", json.get("default_decay_rate").getAsFloat());
        }
        if (json.has("critical_threshold")) {
            fields.put("criticalThreshold", json.get("critical_threshold").getAsFloat());
        }
        if (json.has("low_threshold")) {
            fields.put("lowThreshold", json.get("low_threshold").getAsFloat());
        }
        if (json.has("excess_threshold")) {
            fields.put("excessThreshold", json.get("excess_threshold").getAsFloat());
        }
        return instantiateFromBuilder(NutrientDefinition.class, "dev.maire.nourished.api.NutrientDefinition$Builder", Class.forName("dev.maire.nourished.api.NutrientDefinition$Builder"), new Class<?>[]{String.class}, new Object[]{id}, fields);
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
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(holder.value());
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

        Map<String, Object> fields = new HashMap<>();
        fields.put("nutrientKey", nutrientKey);
        fields.put("threshold", threshold);
        fields.put("thresholdType", thresholdType);
        fields.put("effectId", effectId);
        fields.put("amplifier", amplifier);
        fields.put("duration", duration);
        return instantiateFromBuilder(EffectDefinition.class, "dev.maire.nourished.api.EffectDefinition$Builder", Class.forName("dev.maire.nourished.api.EffectDefinition$Builder"), new Class<?>[]{}, new Object[]{}, fields);
    }

    private static NutrientSynergyDefinition parseSynergy(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        String nutrientA = getRequiredString(json, DatapackSchema.KEY_NUTRIENT_A_KEY);
        String nutrientB = getRequiredString(json, DatapackSchema.KEY_NUTRIENT_B_KEY);
        NutrientSynergyDefinition.LevelCondition conditionA = NutrientSynergyDefinition.LevelCondition.valueOf(
                getRequiredString(json, DatapackSchema.KEY_NUTRIENT_A_CONDITION).toUpperCase(Locale.ROOT));
        NutrientSynergyDefinition.LevelCondition conditionB = NutrientSynergyDefinition.LevelCondition.valueOf(
                getRequiredString(json, DatapackSchema.KEY_NUTRIENT_B_CONDITION).toUpperCase(Locale.ROOT));

        Map<String, Object> fields = new HashMap<>();
        fields.put("id", id);
        fields.put("nutrientKeyA", nutrientA);
        fields.put("conditionA", conditionA);
        fields.put("nutrientKeyB", nutrientB);
        fields.put("conditionB", conditionB);
        if (json.has(DatapackSchema.KEY_BONUS_EFFECT_ID)) {
            fields.put("bonusEffectId", ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_BONUS_EFFECT_ID)));
        }
        fields.put("effectAmplifier", getOptionalInt(json, DatapackSchema.KEY_AMPLIFIER, 0));
        fields.put("effectDuration", getOptionalInt(json, DatapackSchema.KEY_EFFECT_DURATION, 200));
        fields.put("isPenalty", getOptionalBoolean(json, DatapackSchema.KEY_IS_PENALTY, false));
        return instantiateFromBuilder(NutrientSynergyDefinition.class, "dev.maire.nourished.api.NutrientSynergyDefinition$Builder", Class.forName("dev.maire.nourished.api.NutrientSynergyDefinition$Builder"), new Class<?>[]{String.class}, new Object[]{id}, fields);
    }

    private static FoodSynergyDefinition parseFoodSynergy(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", id);
        fields.put("foodA", ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_FOOD_A)));
        fields.put("foodB", ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_FOOD_B)));
        fields.put("timeWindowTicks", getOptionalInt(json, DatapackSchema.KEY_TIME_WINDOW_TICKS, 100));
        fields.put("bonusNutrientKey", getRequiredString(json, DatapackSchema.KEY_BONUS_NUTRIENT_KEY));
        fields.put("bonusAmount", getRequiredFloat(json, DatapackSchema.KEY_BONUS_AMOUNT));
        return instantiateFromBuilder(FoodSynergyDefinition.class, "dev.maire.nourished.api.FoodSynergyDefinition$Builder", Class.forName("dev.maire.nourished.api.FoodSynergyDefinition$Builder"), new Class<?>[]{String.class}, new Object[]{id}, fields);
    }

    private static NutrientMilestoneDefinition parseMilestone(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", id);
        fields.put("nutrientKey", getRequiredString(json, DatapackSchema.KEY_NUTRIENT_KEY));
        fields.put("cumulativeGoal", getRequiredFloat(json, DatapackSchema.KEY_CUMULATIVE_GOAL));
        if (json.has(DatapackSchema.KEY_REWARD_EFFECT_ID)) {
            fields.put("rewardEffectId", ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_REWARD_EFFECT_ID)));
        }
        fields.put("rewardAmplifier", getOptionalInt(json, DatapackSchema.KEY_AMPLIFIER, 0));
        fields.put("rewardDuration", getOptionalInt(json, DatapackSchema.KEY_REWARD_DURATION, 200));
        if (json.has(DatapackSchema.KEY_ADVANCEMENT_ID)) {
            fields.put("advancementId", ResourceLocation.parse(getRequiredString(json, DatapackSchema.KEY_ADVANCEMENT_ID)));
        }
        return instantiateFromBuilder(NutrientMilestoneDefinition.class, "dev.maire.nourished.api.NutrientMilestoneDefinition$Builder", Class.forName("dev.maire.nourished.api.NutrientMilestoneDefinition$Builder"), new Class<?>[]{String.class}, new Object[]{id}, fields);
    }

    private static DietProfileDefinition parseDietProfile(ResourceLocation fileId, JsonObject json) throws Exception {
        String id = fileId.getPath();
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", id);
        fields.put("displayName", getRequiredString(json, DatapackSchema.KEY_DISPLAY_NAME));
        if (json.has(DatapackSchema.KEY_DESCRIPTION)) {
            fields.put("description", json.get(DatapackSchema.KEY_DESCRIPTION).getAsString());
        }
        if (json.has(DatapackSchema.KEY_CUSTOM_THRESHOLDS) && json.get(DatapackSchema.KEY_CUSTOM_THRESHOLDS).isJsonObject()) {
            Map<String, Float> thresholdMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject(DatapackSchema.KEY_CUSTOM_THRESHOLDS).entrySet()) {
                thresholdMap.put(e.getKey(), e.getValue().getAsFloat());
            }
            fields.put("customThresholds", thresholdMap);
        }
        if (json.has(DatapackSchema.KEY_CUSTOM_DECAY_RATES) && json.get(DatapackSchema.KEY_CUSTOM_DECAY_RATES).isJsonObject()) {
            Map<String, Float> decayMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject(DatapackSchema.KEY_CUSTOM_DECAY_RATES).entrySet()) {
                decayMap.put(e.getKey(), e.getValue().getAsFloat());
            }
            fields.put("customDecayRates", decayMap);
        }
        if (json.has(DatapackSchema.KEY_BONUS_EFFECTS) && json.get(DatapackSchema.KEY_BONUS_EFFECTS).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(DatapackSchema.KEY_BONUS_EFFECTS);
            List<ResourceLocation> effectIds = new ArrayList<>();
            for (JsonElement element : array) {
                effectIds.add(ResourceLocation.parse(element.getAsString()));
            }
            fields.put("bonusEffects", effectIds);
        }
        return instantiateFromBuilder(DietProfileDefinition.class, "dev.maire.nourished.api.DietProfileDefinition$Builder", Class.forName("dev.maire.nourished.api.DietProfileDefinition$Builder"), new Class<?>[]{String.class}, new Object[]{id}, fields);
    }

    private static CompatDefinition parseCompat(ResourceLocation fileId, JsonObject json) throws Exception {
        String modId = json.has(DatapackSchema.KEY_MOD_ID) ? json.get(DatapackSchema.KEY_MOD_ID).getAsString() : fileId.getPath();
        String categoryRaw = json.has(DatapackSchema.KEY_CATEGORY) ? json.get(DatapackSchema.KEY_CATEGORY).getAsString() : CompatDefinition.CompatCategory.FOOD_MOD.name();
        CompatDefinition.CompatCategory category = CompatDefinition.CompatCategory.valueOf(categoryRaw.toUpperCase(Locale.ROOT));

        Map<ResourceLocation, String> mappings = new HashMap<>();
        if (json.has(DatapackSchema.KEY_MAPPINGS) && json.get(DatapackSchema.KEY_MAPPINGS).isJsonObject()) {
            JsonObject mappingObj = json.getAsJsonObject(DatapackSchema.KEY_MAPPINGS);
            for (Map.Entry<String, JsonElement> e : mappingObj.entrySet()) {
                mappings.put(ResourceLocation.parse(e.getKey()), e.getValue().getAsString());
            }
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("modId", modId);
        fields.put("category", category);
        fields.put("foodTagMappings", mappings);
        return instantiateFromBuilder(CompatDefinition.class, "dev.maire.nourished.api.CompatDefinition$Builder", Class.forName("dev.maire.nourished.api.CompatDefinition$Builder"), new Class<?>[]{String.class}, new Object[]{modId}, fields);
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiateFromBuilder(
            Class<T> targetClass,
            String builderClassName,
            Class<?> builderClass,
            Class<?>[] builderCtorArgTypes,
            Object[] builderCtorArgs,
            Map<String, Object> builderFieldValues
    ) throws Exception {
        Constructor<?> builderCtor = builderClass.getDeclaredConstructor(builderCtorArgTypes);
        builderCtor.setAccessible(true);
        Object builder = builderCtor.newInstance(builderCtorArgs);

        for (Map.Entry<String, Object> e : builderFieldValues.entrySet()) {
            Field field = builderClass.getDeclaredField(e.getKey());
            field.setAccessible(true);
            field.set(builder, e.getValue());
        }

        Class<?> declaredBuilder = Class.forName(builderClassName);
        Constructor<T> targetCtor = targetClass.getDeclaredConstructor(declaredBuilder);
        targetCtor.setAccessible(true);
        return targetCtor.newInstance(builder);
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
