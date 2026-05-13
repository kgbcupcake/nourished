package dev.maire.nourished.core.diet;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.ModNetworking.SyncDietDeltaPayload;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks daily-style diet totals. Nutrient keys are driven by {@link NutrientRegistry#getKeys()}.
 * <p>
 * SCHEMA CHANGE: NutritionAttachment removed in this version. Existing player saves will reset diet data.
 * Acceptable for alpha.
 */
@ApiStatus.Internal
public class DietData {
    private static final String NUTRIENTS_CODEC_PREFIX = "nutrients.";

    /** Display / bar order — delegates to the registry so it stays in sync. */
    public static List<String> barOrder() {
        return NutrientRegistry.getKeys();
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final Decoder<DietData> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<DietData, T>> decode(DynamicOps<T> ops, T input) {
            return decodeData(ops, input).map(d -> Pair.of(d, input));
        }
    };

    public static final Codec<DietData> CODEC = Codec.of(DietData::encode, DECODER);

    private static <T> DataResult<T> encode(DietData data, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> map = ops.mapBuilder();
        map.add("calories",     Codec.FLOAT.encodeStart(ops, data.calories));
        map.add("max_calories", Codec.FLOAT.encodeStart(ops, data.maxCalories));
        for (String key : barOrder()) {
            map.add(getNutrientCodecKey(key), Codec.FLOAT.encodeStart(ops, data.nutrients.getOrDefault(key, 0f)));
            map.add("last_" + key, Codec.FLOAT.encodeStart(ops, data.lastNutrients.getOrDefault(key, 0f)));
        }
        map.add("food_memory", Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                .encodeStart(ops, data.foodMemory));
        map.add("category_memory", Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                .encodeStart(ops, data.categoryMemory));
        map.add("family_memory", Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                .encodeStart(ops, data.familyMemory));
        map.add("last_tick_time", Codec.LONG.encodeStart(ops, data.lastTickTime));
        return map.build(prefix);
    }

    private static <T> DataResult<DietData> decodeData(DynamicOps<T> ops, T input) {
        MapLike<T> map = ops.getMap(input).getOrThrow();

        DietData data = new DietData();
        data.calories    = decodeFloat(ops, map, "calories",     0f);
        data.maxCalories = decodeFloat(ops, map, "max_calories", 2000f);

        for (String key : barOrder()) {
            float nutrientValue = decodeFloatWithFallback(ops, map, getNutrientCodecKey(key), key, 0f);
            data.nutrients.put(key, nutrientValue);
            data.lastNutrients.put(key, decodeFloat(ops, map, "last_" + key, 0f));
        }

        T foodMemoryVal = map.get("food_memory");
        if (foodMemoryVal != null) {
            Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                    .parse(ops, foodMemoryVal)
                    .result()
                    .ifPresent(m -> data.foodMemory.putAll(m));
        }

        // New maps with empty fallback for old saves
        T categoryMemoryVal = map.get("category_memory");
        if (categoryMemoryVal != null) {
            Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                    .parse(ops, categoryMemoryVal)
                    .result()
                    .ifPresent(m -> data.categoryMemory.putAll(m));
        }

        T familyMemoryVal = map.get("family_memory");
        if (familyMemoryVal != null) {
            Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                    .parse(ops, familyMemoryVal)
                    .result()
                    .ifPresent(m -> data.familyMemory.putAll(m));
        }

        data.lastTickTime = decodeLong(ops, map, "last_tick_time", 0L);

        return DataResult.success(data);
    }

    private static <T> long decodeLong(DynamicOps<T> ops, MapLike<T> map, String field, long fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.LONG.parse(ops, val).result().orElse(fallback);
    }

    private static <T> float decodeFloat(DynamicOps<T> ops, MapLike<T> map, String field, float fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.FLOAT.parse(ops, val).result().orElse(fallback);
    }

    private static <T> float decodeFloatWithFallback(DynamicOps<T> ops, MapLike<T> map, String primaryField, String fallbackField, float fallback) {
        T primary = map.get(primaryField);
        if (primary != null) {
            return Codec.FLOAT.parse(ops, primary).result().orElse(fallback);
        }
        return decodeFloat(ops, map, fallbackField, fallback);
    }

    private static String getNutrientCodecKey(String nutrientKey) {
        return NUTRIENTS_CODEC_PREFIX + nutrientKey;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    public float calories;
    public float maxCalories = 2000f;
    public final LinkedHashMap<String, Float> nutrients     = new LinkedHashMap<>();
    public final LinkedHashMap<String, Float> lastNutrients = new LinkedHashMap<>();

    // Food memory maps
    public final LinkedHashMap<String, FoodMemoryEntry> foodMemory = new LinkedHashMap<>();
    public final HashMap<String, FoodMemoryEntry> categoryMemory = new HashMap<>();
    public final HashMap<String, FoodMemoryEntry> familyMemory = new HashMap<>();

    /** Last known game time in ms, used for decay calculations */
    public long lastTickTime = 0L;

    public DietData() {
        float start = startNutrientFill();
        for (String key : barOrder()) {
            nutrients.put(key, start);
            lastNutrients.put(key, start);
        }
    }

    /**
     * Deep-copies mutable maps and scalars for thread-safe handoff (e.g. client cache publish).
     */
    @ApiStatus.Internal
    public static DietData copySnapshot(DietData src) {
        DietData d = new DietData();
        d.calories = src.calories;
        d.maxCalories = src.maxCalories;
        d.nutrients.clear();
        d.nutrients.putAll(src.nutrients);
        d.lastNutrients.clear();
        d.lastNutrients.putAll(src.lastNutrients);
        d.foodMemory.clear();
        d.foodMemory.putAll(src.foodMemory);
        d.categoryMemory.clear();
        d.categoryMemory.putAll(src.categoryMemory);
        d.familyMemory.clear();
        d.familyMemory.putAll(src.familyMemory);
        d.lastTickTime = src.lastTickTime;
        return d;
    }

    /**
     * Baseline for every nutrient bar on new {@link DietData} (new players / fresh attachment).
     * Uses {@link NourishedConfig#startingNutrientValue()} (default {@code 0.5} = 50%) so typical
     * {@code below}-threshold debuffs do not apply until decay or poor diet pulls bars down.
     */
    private static float startNutrientFill() {
        try {
            return Mth.clamp((float) NourishedConfig.get().startingNutrientValue(), 0f, 1f);
        } catch (IllegalStateException ignored) {
            return 0.5f;
        }
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /**
     * Snapshot current nutrients into {@link #lastNutrients}. Call on the server before applying
     * food deltas so trends compare the previous meal state to the new state.
     */
    public void tick() {
        lastNutrients.clear();
        lastNutrients.putAll(nutrients);
    }

    /**
     * Updates the last known game time anchor.
     * Call from FoodEatenHandler on every food eat event with
     * {@code player.level().getGameTime() * 50L} to keep time
     * anchored to game ticks rather than wall-clock time.
     */
    public void tickTime(long gameTimeMs) {
        this.lastTickTime = gameTimeMs;
    }

    public void addCalories(float amount) {
        calories = Math.max(0f, calories + amount);
    }

    public void addNutrient(String key, float amount) {
        if (!nutrients.containsKey(key)) return;
        float newValue = Mth.clamp(nutrients.get(key) + amount, 0f, 1f);
        if (Float.isNaN(newValue)) {
            Nourished.LOGGER.error("[Nourished] NaN nutrient value detected for key={} — resetting to 0", key);
            newValue = 0f;
        }
        nutrients.put(key, newValue);
    }

    /** Balance score in [0, 1]: higher when all bars are closer to each other. */
    public float getBalanceScore() {
        List<String> keys = barOrder();
        float sum = 0f;
        for (String k : keys) sum += nutrients.getOrDefault(k, 0f);
        float avg = sum / keys.size();
        if (avg <= 0.0001f) return 1f;
        float dev = 0f;
        for (String k : keys) dev += Math.abs(nutrients.getOrDefault(k, 0f) - avg);
        return 1f - Math.min(1f, dev / (avg * keys.size() * 2f));
    }

    /** Extracts client-sync state for lightweight network sync (bars, calories, memory, time anchor). */
    public SyncDietDeltaPayload toDeltaPayload() {
        List<String> recentFoodIds = foodMemory.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().lastEatenTick(), a.getValue().lastEatenTick()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        List<String> neglectedCategories = getMostNeglectedCategories(2);
        List<String> fatiguedFamilies = getMostFatiguedFamilies(2, lastTickTime);
        return new SyncDietDeltaPayload(
                Map.copyOf(nutrients),
                Map.copyOf(lastNutrients),
                calories,
                maxCalories,
                getBalanceScore(),
                recentFoodIds,
                neglectedCategories,
                fatiguedFamilies,
                Map.copyOf(foodMemory),
                Map.copyOf(categoryMemory),
                Map.copyOf(familyMemory),
                lastTickTime
        );
    }

    // ── Food Memory ────────────────────────────────────────────────────────────

    /**
     * Records a food eat with full blended multiplier system.
     * <p>
     * This method:
     * <ol>
     *   <li>Performs single cleanup pass on all three memory maps</li>
     *   <li>Updates item, category, and family memories with appropriate weights</li>
     *   <li>Applies nutritional debt if threshold exceeded</li>
     *   <li>Returns blended multiplier for the food</li>
     * </ol>
     *
     * @param itemId           the specific food item identifier
     * @param dominantCategory the primary nutrient category (e.g., "proteins")
     * @param familyKey        the food family (e.g., "fish"), nullable
     * @param gameTimeMs       current game time in milliseconds
     * @return blended multiplier to apply to diet deltas
     */
    public float recordEat(String itemId, String dominantCategory, String familyKey, long gameTimeMs) {
        NourishedConfig config = NourishedConfig.get();
        int maxCount = config.memoryWindowCount();
        long streakWindowMs = config.streakWindowMs();
        float streakWeight = (float) config.streakWeight();
        long currentTime = resolveCurrentTime(gameTimeMs);

        // Single cleanup pass for all three maps (remove entries decayed below threshold)
        cleanupAllMemories(currentTime);

        // Enforce count cap on food memory (evict oldest)
        if (!foodMemory.containsKey(itemId) && foodMemory.size() >= maxCount) {
            String oldest = foodMemory.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().lastEatenTick()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest != null) foodMemory.remove(oldest);
        }

        // Update all three memories with appropriate weights
        updateMemory(foodMemory, itemId, currentTime, 1.0f, streakWindowMs, streakWeight);
        if (dominantCategory != null) {
            updateMemory(categoryMemory, dominantCategory, currentTime, 0.3f, streakWindowMs, streakWeight);
        }
        if (familyKey != null) {
            updateMemory(familyMemory, familyKey, currentTime, 0.2f, streakWindowMs, streakWeight);
        }

        // Update tick time
        this.lastTickTime = currentTime;

        // Apply nutritional debt (implemented in Phase 4)
        if (dominantCategory != null) {
            applyNutritionalDebt(dominantCategory, currentTime);
        }

        float result = computeBlendedMultiplier(itemId, dominantCategory, familyKey, currentTime);

        // Debug logging when enabled
        if (config.debugMemoryLogging()) {
            MultiplierBreakdown breakdown = getMultiplierBreakdown(itemId, dominantCategory, familyKey, currentTime);
            Nourished.LOGGER.debug(
                    "Memory breakdown for {}: item={} (w={}) cat={} (w={}) family={} (w={}) novelty={} => final={}",
                    itemId,
                    breakdown.itemContribution(), breakdown.itemWeight(),
                    breakdown.categoryContribution(), breakdown.categoryWeight(),
                    breakdown.familyContribution(), breakdown.familyWeight(),
                    breakdown.noveltyContribution(),
                    breakdown.finalMultiplier()
            );
        }

        return result;
    }

    /**
     * Helper to update a memory map with streak-aware increment.
     */
    private void updateMemory(Map<String, FoodMemoryEntry> memory, String key, long gameTimeMs,
                              float baseIncrement, long streakWindowMs, float streakWeight) {
        FoodMemoryEntry entry = memory.getOrDefault(key, new FoodMemoryEntry(0f, gameTimeMs));
        long elapsed = gameTimeMs - entry.lastEatenTick();
        boolean inStreak = elapsed <= streakWindowMs && elapsed >= 0;
        float increment = baseIncrement * (inStreak ? streakWeight : 1.0f);
        entry = new FoodMemoryEntry(entry.eatCount() + increment, gameTimeMs);
        memory.put(key, entry);
    }

    /**
     * Backward-compatible recordEat that guesses category and uses system time.
     * <p>
     * Preserved for existing callers. For new code, prefer
     * {@link #recordEat(String, String, String, long)}.
     *
     * @param itemId the food item identifier
     * @return multiplier to apply
     */
    public float recordEat(String itemId) {
        return recordEat(itemId, null, null, resolveCurrentTime(0L));
    }

    /**
     * Peek the multiplier that would apply if this food were eaten next.
     * Used for tooltip display.
     */
    public float peekMultiplier(String itemId) {
        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        long gameTimeMs = resolveCurrentTime(0L);
        FoodMemoryEntry entry = foodMemory.get(itemId);
        if (entry == null || entry.isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f)) {
            return (float) NourishedConfig.get().noveltyBonus();
        }
        // Simulate one more eat for preview
        float nextDecayed = entry.decayedEatCount(halfLifeMs, gameTimeMs) + 1f;
        return resolveMultiplier(nextDecayed);
    }

    /**
     * Applies soft nutritional debt when a category is overconsumed.
     * <p>
     * When a category's decayed eat count exceeds the debt threshold, this method
     * finds the most neglected nutrient category (lowest bar value, excluding the current)
     * and applies a small decay to it. This encourages dietary variety.
     *
     * @param dominantCategory the category being eaten
     * @param gameTimeMs       current game time in milliseconds
     */
    private void applyNutritionalDebt(String dominantCategory, long gameTimeMs) {
        NourishedConfig config = NourishedConfig.get();
        long halfLifeMs = config.memoryWindowMinutes() * 60_000L;
        float debtThreshold = (float) config.debtThreshold();
        float debtRate = (float) config.debtDecayRate();

        // Check if category memory exceeds debt threshold
        FoodMemoryEntry catEntry = categoryMemory.get(dominantCategory);
        if (catEntry == null) return;

        float decayedCatCount = catEntry.decayedEatCount(halfLifeMs, gameTimeMs);
        if (decayedCatCount < debtThreshold) return;

        // Find the most neglected category (lowest bar value, excluding current)
        String mostNeglected = null;
        float lowestValue = Float.MAX_VALUE;

        for (Map.Entry<String, Float> entry : nutrients.entrySet()) {
            String key = entry.getKey();
            if (key.equals(dominantCategory)) continue;
            float value = entry.getValue();
            if (value < lowestValue) {
                lowestValue = value;
                mostNeglected = key;
            }
        }

        if (mostNeglected == null) return;

        // Don't decay below a minimum floor (0.05)
        if (lowestValue <= 0.05f) return;

        // Apply soft decay
        float newValue = Math.max(0.05f, lowestValue - debtRate);
        nutrients.put(mostNeglected, newValue);
    }

    /**
     * Resolves diminishing multiplier using a logistic saturation curve.
     * <p>
     * Formula: multiplier = 1 - (sigmoid(decayedCount) - 0.5) * 2 * (1 - floor)
     * <p>
     * This produces smooth diminishing returns:
     * - decayedCount = 0 → noveltyBonus (for completely fresh foods)
     * - decayedCount = midpoint → ~0.5 multiplier
     * - decayedCount >> midpoint → approaches floor
     *
     * @param decayedCount the exponentially decayed eat count
     * @return multiplier in [floor, noveltyBonus]
     */
    private float resolveMultiplier(float decayedCount) {
        NourishedConfig config = NourishedConfig.get();
        double floor = config.diminishingFloor();
        double steepness = config.diminishingSteepness();
        double midpoint = config.diminishingMidpoint();
        double noveltyBonus = config.noveltyBonus();

        if (decayedCount <= 0f) {
            return (float) noveltyBonus;
        }

        // Logistic sigmoid: 1 / (1 + e^(-k*(x - m)))
        double sigmoid = 1.0 / (1.0 + Math.exp(-steepness * (decayedCount - midpoint)));

        // Map sigmoid [0.5, 1] to multiplier [1, floor]
        // sigmoid at midpoint = 0.5, so penalty = 0
        // sigmoid as x→∞ = 1, so penalty = (1-floor)
        double penalty = (sigmoid - 0.5) * 2.0 * (1.0 - floor);
        double multiplier = 1.0 - penalty;

        return (float) Math.max(floor, Math.min(noveltyBonus, multiplier));
    }

    /**
     * Computes a novelty score for a food item based on how recently/frequently it was eaten.
     * <p>
     * Returns a value in [0, 1]:
     * - 1.0 = completely novel (never eaten or fully decayed)
     * - 0.0 = heavily saturated (at or above noveltyDecayCap)
     * <p>
     * This score is used to lerp between 1.0 and noveltyBonus as a final multiplier.
     *
     * @param itemId       the food item identifier
     * @param halfLifeMs   memory half-life in milliseconds
     * @param currentTimeMs current game time in ms
     * @return novelty score in [0, 1]
     */
    public float resolveNoveltyScore(String itemId, long halfLifeMs, long currentTimeMs) {
        FoodMemoryEntry entry = foodMemory.get(itemId);
        if (entry == null) {
            return 1.0f; // Never eaten = fully novel
        }

        float decayed = entry.decayedEatCount(halfLifeMs, currentTimeMs);
        if (decayed < 0.1f) {
            return 1.0f; // Effectively forgotten = novel
        }

        double noveltyDecayCap = NourishedConfig.get().noveltyDecayCap();
        return Math.max(0f, 1.0f - (decayed / (float) noveltyDecayCap));
    }

    /**
     * Computes the blended multiplier combining item, category, and family signals
     * with dynamic weighting based on signal strength.
     * <p>
     * The blending formula:
     * <ol>
     *   <li>Get decayed counts for item, category, and family</li>
     *   <li>Resolve individual multipliers via logistic curve</li>
     *   <li>Compute dynamic weights based on signal strength</li>
     *   <li>Blend weighted contributions</li>
     *   <li>Apply novelty as final multiplicative layer</li>
     * </ol>
     *
     * @param itemId           the specific food item identifier
     * @param dominantCategory the primary nutrient category (e.g., "proteins")
     * @param familyKey        the food family (e.g., "fish"), nullable
     * @param gameTimeMs       current game time in milliseconds
     * @return blended multiplier in [floor, noveltyBonus]
     */
    public float computeBlendedMultiplier(String itemId, String dominantCategory, String familyKey, long gameTimeMs) {
        NourishedConfig config = NourishedConfig.get();
        long halfLifeMs = config.memoryWindowMinutes() * 60_000L;
        double floor = config.diminishingFloor();
        double noveltyBonus = config.noveltyBonus();

        // Get decayed counts for all three signals
        float itemDecayed = 0f;
        FoodMemoryEntry itemEntry = foodMemory.get(itemId);
        if (itemEntry != null) {
            itemDecayed = itemEntry.decayedEatCount(halfLifeMs, gameTimeMs);
        }

        float categoryDecayed = 0f;
        if (dominantCategory != null) {
            FoodMemoryEntry catEntry = categoryMemory.get(dominantCategory);
            if (catEntry != null) {
                categoryDecayed = catEntry.decayedEatCount(halfLifeMs, gameTimeMs);
            }
        }

        float familyDecayed = 0f;
        if (familyKey != null) {
            FoodMemoryEntry famEntry = familyMemory.get(familyKey);
            if (famEntry != null) {
                familyDecayed = famEntry.decayedEatCount(halfLifeMs, gameTimeMs);
            }
        }

        // Resolve individual multipliers
        float itemMultiplier = resolveMultiplier(itemDecayed);
        float categoryMultiplier = resolveMultiplier(categoryDecayed);
        float familyMultiplier = familyKey != null ? resolveMultiplier(familyDecayed) : 1.0f;

        // Dynamic weight calculation based on signal strength
        // Family gets weight proportional to how much it's been eaten
        float familyWeight = familyKey != null ? Math.min(0.2f, familyDecayed * 0.04f) : 0f;
        // Category gets weight proportional to category saturation
        float categoryWeight = Math.min(0.25f, categoryDecayed * 0.05f);
        // Item gets the remaining weight
        float itemWeight = 1.0f - categoryWeight - familyWeight;

        // Blend contributions
        // Apply novelty only to item freshness contribution, not category/family fatigue.
        float noveltyScore = resolveNoveltyScore(itemId, halfLifeMs, gameTimeMs);
        float noveltyFactor = 1.0f + (noveltyScore * ((float) noveltyBonus - 1.0f));
        float itemContribution = itemMultiplier * itemWeight * noveltyFactor;
        float categoryContribution = categoryMultiplier * categoryWeight;
        float familyContribution = familyMultiplier * familyWeight;
        float blendedMultiplier = itemContribution + categoryContribution + familyContribution;

        // Clamp to valid range
        return (float) Math.max(floor, Math.min(noveltyBonus, blendedMultiplier));
    }

    public FoodMemoryEntry getMemoryEntry(String itemId) {
        return foodMemory.get(itemId);
    }

    // ── Visualization Accessors (Render-Thread Safe) ─────────────────────────────

    /**
     * Effective multiplier for tooltip display with full category/family context.
     * Pure read, no mutations - safe for render thread.
     *
     * @param itemId           the food item identifier
     * @param dominantCategory the primary nutrient category
     * @param familyKey        the food family (nullable)
     * @param gameTimeMs       current game time in ms
     * @return the multiplier that would apply
     */
    public float peekMultiplier(String itemId, String dominantCategory, String familyKey, long gameTimeMs) {
        return computeBlendedMultiplier(itemId, dominantCategory, familyKey, gameTimeMs);
    }

    /**
     * Returns category fatigue in [0, 1] where 0 = fully saturated, 1 = completely fresh.
     *
     * @param categoryKey the nutrient category
     * @param gameTimeMs  current game time in ms
     * @return fatigue level
     */
    public float getCategoryFatigue(String categoryKey, long gameTimeMs) {
        FoodMemoryEntry entry = categoryMemory.get(categoryKey);
        if (entry == null) return 1.0f;
        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        float decayed = entry.decayedEatCount(halfLifeMs, gameTimeMs);
        // Use same logistic curve as resolveMultiplier — invert so 1.0 = fresh, 0.0 = saturated
        float multiplier = resolveMultiplier(decayed);
        double floor = NourishedConfig.get().diminishingFloor();
        double noveltyBonus = NourishedConfig.get().noveltyBonus();
        // Normalize multiplier from [floor, noveltyBonus] to [0, 1]
        return (float) ((multiplier - floor) / (noveltyBonus - floor));
    }

    /**
     * Gets novelty score for a specific item.
     *
     * @param itemId     the food item identifier
     * @param gameTimeMs current game time in ms
     * @return novelty score in [0, 1]
     */
    public float getNoveltyScore(String itemId, long gameTimeMs) {
        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        return resolveNoveltyScore(itemId, halfLifeMs, gameTimeMs);
    }

    /**
     * Returns the top N most fatigued food families (lowest multiplier).
     *
     * @param n          number of families to return
     * @param gameTimeMs current game time in ms
     * @return list of family keys sorted by fatigue (most fatigued first)
     */
    public List<String> getMostFatiguedFamilies(int n, long gameTimeMs) {
        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;

        return familyMemory.entrySet().stream()
                .filter(e -> !e.getValue().isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f))
                .sorted((a, b) -> Float.compare(
                        b.getValue().decayedEatCount(halfLifeMs, gameTimeMs),
                        a.getValue().decayedEatCount(halfLifeMs, gameTimeMs)))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns the top N most neglected nutrient categories (lowest bar values).
     *
     * @param n number of categories to return
     * @return list of category keys sorted by bar value (lowest first)
     */
    public List<String> getMostNeglectedCategories(int n) {
        return nutrients.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Full multiplier breakdown for debug display or advanced tooltips.
     *
     * @param itemId           the food item identifier
     * @param dominantCategory the primary nutrient category
     * @param familyKey        the food family (nullable)
     * @param gameTimeMs       current game time in ms
     * @return detailed breakdown record
     */
    public MultiplierBreakdown getMultiplierBreakdown(String itemId, String dominantCategory, String familyKey, long gameTimeMs) {
        NourishedConfig config = NourishedConfig.get();
        long halfLifeMs = config.memoryWindowMinutes() * 60_000L;
        double noveltyBonus = config.noveltyBonus();

        // Get decayed counts
        float itemDecayed = 0f;
        FoodMemoryEntry itemEntry = foodMemory.get(itemId);
        if (itemEntry != null) {
            itemDecayed = itemEntry.decayedEatCount(halfLifeMs, gameTimeMs);
        }

        float categoryDecayed = 0f;
        if (dominantCategory != null) {
            FoodMemoryEntry catEntry = categoryMemory.get(dominantCategory);
            if (catEntry != null) {
                categoryDecayed = catEntry.decayedEatCount(halfLifeMs, gameTimeMs);
            }
        }

        float familyDecayed = 0f;
        if (familyKey != null) {
            FoodMemoryEntry famEntry = familyMemory.get(familyKey);
            if (famEntry != null) {
                familyDecayed = famEntry.decayedEatCount(halfLifeMs, gameTimeMs);
            }
        }

        // Calculate multipliers
        float itemMultiplier = resolveMultiplier(itemDecayed);
        float categoryMultiplier = resolveMultiplier(categoryDecayed);
        float familyMultiplier = familyKey != null ? resolveMultiplier(familyDecayed) : 1.0f;

        // Calculate weights
        float familyWeight = familyKey != null ? Math.min(0.2f, familyDecayed * 0.04f) : 0f;
        float categoryWeight = Math.min(0.25f, categoryDecayed * 0.05f);
        float itemWeight = 1.0f - categoryWeight - familyWeight;

        // Calculate contributions
        float itemContribution = itemMultiplier * itemWeight;
        float categoryContribution = categoryMultiplier * categoryWeight;
        float familyContribution = familyMultiplier * familyWeight;

        // Novelty
        float noveltyScore = resolveNoveltyScore(itemId, halfLifeMs, gameTimeMs);
        float noveltyContribution = 1.0f + (noveltyScore * ((float) noveltyBonus - 1.0f));

        itemContribution *= noveltyContribution;
        float finalMultiplier = itemContribution + categoryContribution + familyContribution;
        finalMultiplier = (float) Math.max(config.diminishingFloor(), Math.min(noveltyBonus, finalMultiplier));

        return new MultiplierBreakdown(
                itemContribution, categoryContribution, familyContribution,
                noveltyContribution, finalMultiplier,
                itemWeight, categoryWeight, familyWeight
        );
    }

    /**
     * Detailed breakdown of how the multiplier was calculated.
     * Useful for debugging and advanced tooltips.
     */
    public record MultiplierBreakdown(
            float itemContribution,
            float categoryContribution,
            float familyContribution,
            float noveltyContribution,
            float finalMultiplier,
            float itemWeight,
            float categoryWeight,
            float familyWeight
    ) {}

    private long resolveCurrentTime(long tickTime) {
        if (tickTime > 0L) {
            return tickTime;
        }
        if (lastTickTime > 0L) {
            return lastTickTime;
        }
        return System.currentTimeMillis();
    }

    private void cleanupAllMemories(long currentTime) {
        long halfLifeMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        float threshold = 0.1f;
        removeExpiredEntries(foodMemory, halfLifeMs, currentTime, threshold);
        removeExpiredEntries(categoryMemory, halfLifeMs, currentTime, threshold);
        removeExpiredEntries(familyMemory, halfLifeMs, currentTime, threshold);
    }

    private void removeExpiredEntries(Map<String, FoodMemoryEntry> memory, long halfLifeMs, long currentTime, float threshold) {
        Iterator<Map.Entry<String, FoodMemoryEntry>> iterator = memory.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FoodMemoryEntry> entry = iterator.next();
            if (entry.getValue().isEffectivelyExpired(halfLifeMs, currentTime, threshold)) {
                iterator.remove();
            }
        }
    }

}
