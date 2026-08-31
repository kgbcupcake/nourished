package dev.maire.nourished.api;

import dev.marie.framework.api.effects.AbsorptionModifier;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.api.marieapi.MarieAPIVersion;
import dev.marie.framework.api.marie.MarieSeasonHook;
import dev.marie.framework.api.reporting.ApplicationHistoryView;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.progression.ProfileDefinition;
import dev.marie.framework.api.reporting.ReportProvider;
import dev.marie.framework.api.effects.SynergyDefinition;
import dev.marie.framework.api.effects.ThresholdEffect;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.api.registry.AbsorptionModifierRegistry;
import dev.marie.framework.api.registry.MilestoneRegistry;
import dev.marie.framework.api.registry.ProfileRegistry;
import dev.marie.framework.api.registry.ReportProviderRegistry;
import dev.marie.framework.api.registry.SeasonHookRegistry;
import dev.marie.framework.api.registry.SynergyRegistry;
import dev.marie.framework.compat.CompatDefinition;
import dev.marie.framework.config.FeatureFlagCache;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.marie.framework.api.impl.EmptyApplicationHistoryView;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.util.MarieRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static entry point for the Nourished public API.
 *
 * <p>All interaction with the Nourished nutrition system from external mods
 * should go through this class. Methods provide read access to player nutrition
 * state and write access to register custom nutrients, effects, compatibilities,
 * and extension hooks.</p>
 *
 * <p>This class is not instantiable. All methods are static.</p>
 */
@ApiStatus.Stable
public final class NourishedAPI {

    private static final ResourceLocation API_MODIFIER_SOURCE = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "api");

    /** MarieLib tracker id for the calories daily tracker (see {@link #getYesterdayCalories(Player)}). */
    public static final ResourceLocation CALORIES_TRACKER_ID = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "calories");

    private NourishedAPI() {}

    // ───────────────────────────────────────────────────────────────
    // Player State Queries
    // ───────────────────────────────────────────────────────────────

    /**
     * Returns the total calorie count for the given player based on their
     * current nutrient levels and recent food consumption.
     *
     * @param player the player to query
     * @return the player's current calorie value
     * @throws IllegalStateException if the nutrition system is not initialized
     */
    @ApiStatus.Stable
    public static float getTotal(Player player) {
        if (player == null) {
            return 0f;
        }
        return TrackingAttachment.getTotal(player);
    }

    /**
     * Returns the current level of a specific nutrient for the given player.
     *
     * @param player      the player to query
     * @param valueKey the internal key of the nutrient (e.g. "protein", "iron")
     * @return the nutrient level as a normalized float (0.0 to 1.0),
     *         or {@code -1.0f} if the nutrient key is not recognized
     */
    @ApiStatus.Stable
    public static float getValueLevel(Player player, String valueKey) {
        if (player == null) {
            return -1.0f;
        }
        return TrackingAttachment.getValueLevel(player, valueKey);
    }

    /**
     * Returns a read-only view of the player's food consumption memory,
     * exposing recent eating history and variety information.
     *
     * @param player the player to query
     * @return an {@link ApplicationHistoryView} for the given player
     * @throws IllegalStateException if the nutrition system is not initialized
     */
    @ApiStatus.Stable
    public static ApplicationHistoryView getSourceMemory(Player player) {
        if (player == null) {
            return EmptyApplicationHistoryView.INSTANCE;
        }
        return TrackingAttachment.getApplicationHistoryView(player);
    }
    /**
     * Alias for {@link #getTotal(Player)}.
     *
     * @param player the player to query
     * @return the player's current calorie value
     */
    @ApiStatus.Stable
    public static float getTotalCount(Player player) {
        return getTotal(player);
    }

    /**
     * Returns the player's calorie total from the most recently completed daily period.
     *
     * @param player the player to query
     * @return the previous day's calorie total, or {@code -1.0f} if calorie history is
     *         disabled ({@code enableCalorieHistory}) or no history exists yet
     */
    @ApiStatus.Stable
    public static float getYesterdayCalories(Player player) {
        if (player == null || !NourishedConfig.get().enableCalorieHistory()) {
            return -1.0f;
        }
        List<TrackerHistoryEntry> history = getCalorieHistory(player);
        return history.isEmpty() ? -1.0f : history.get(0).value();
    }

    /**
     * Returns the player's completed-period calorie history, most recent first, bounded by
     * {@code calorieHistoryRetentionDays}.
     *
     * @param player the player to query
     * @return the calorie history, or an empty list if calorie history is disabled
     *         ({@code enableCalorieHistory}) or no history exists yet
     */
    @ApiStatus.Stable
    public static List<TrackerHistoryEntry> getCalorieHistory(Player player) {
        if (player == null || !NourishedConfig.get().enableCalorieHistory()) {
            return List.of();
        }
        return dev.marie.framework.tracking.tracker.MarieTracking.getTrackerHistory(player, CALORIES_TRACKER_ID);
    }

    /**
     * Returns an aggregated diet snapshot for the given player including
     * calories, all registered nutrient levels, and food memory state.
     *
     * @param player the player to query
     * @return aggregated player diet state snapshot
     */
    @ApiStatus.Stable
    public static dev.marie.framework.api.marie.MariePlayerData getTrackingData(Player player) {
        Map<String, Float> nutrients = new LinkedHashMap<>();
        for (String valueKey : NutrientRegistry.getKeys()) {
            nutrients.put(valueKey, getValueLevel(player, valueKey));
        }
        return new dev.marie.framework.api.marie.MariePlayerData(
                getTotal(player),
                Collections.unmodifiableMap(nutrients),
                getSourceMemory(player)
        );
    }

    /**
     * Applies a direct nutrient delta by posting a {@link dev.marie.framework.api.value.ValueModifierEvent}
     * and then applying the final event amount if the event is not cancelled.
     *
     * @param player      the player to modify
     * @param valueKey the nutrient key to modify
     * @param delta       the nutrient delta to apply
     */
    @ApiStatus.Stable
    public static void modifyValue(Player player, String valueKey, float delta) {
        MarieRegistryUtils.requireValueKey(valueKey, "NourishedAPI.modifyValue");
        dev.marie.framework.api.value.ValueModifierContext ctx =
                dev.marie.framework.api.value.ValueModifierContext.of(player, API_MODIFIER_SOURCE, valueKey);
        dev.marie.framework.api.value.ValueModifierEvent modifierEvent =
                new dev.marie.framework.api.value.ValueModifierEvent(ctx, delta);
        NeoForge.EVENT_BUS.post(modifierEvent);
        if (modifierEvent.isCanceled()) {
            return;
        }
        float finalDelta = dev.maire.nourished.core.NourishedKubeIntegration.fireNutrientModifier(
                ctx, modifierEvent.getAmount());
        if (finalDelta == 0f) {
            return;
        }
        TrackingData diet = player.getData(TrackingAttachment.TRACKING.get());
        diet.addValue(valueKey, finalDelta);
        player.setData(TrackingAttachment.TRACKING.get(), diet);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ModNetworking.syncDietDelta(serverPlayer, diet);
        if (FeatureFlagCache.enableEffects()) {
            NutritionEffectApplier.apply(serverPlayer, diet);
        }
    }

    @ApiStatus.Stable
    public static String getVersion() {
        return MarieAPIVersion.VERSION;
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Nutrients & Foods
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a custom nutrient with the Nourished system. The nutrient will
     * participate in all standard mechanics (decay, thresholds, HUD rendering).
     *
     * <p>Must be called during mod initialization (before the server starts).</p>
     *
     * @param definition the nutrient definition to register
     * @throws IllegalStateException    if called after initialization is complete
     * @throws IllegalArgumentException if a nutrient with the same id already exists
     */
    @ApiStatus.Stable
    public static void registerValue(ValueDefinition definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        if (NutrientRegistry.getKeys().contains(definition.getId())) {
            throw new IllegalArgumentException("Nutrient already registered: " + definition.getId());
        }
        NutrientRegistry.registerExternal(definition);
    }

    /**
     * Alias for {@link #registerValue(ValueDefinition)}.
     *
     * @param definition the nutrient definition to register
     */
    @ApiStatus.Stable
    public static void addNutrient(ValueDefinition definition) {
        registerValue(definition);
    }

    /**
     * Registers a food item's nutrient classification, mapping a food to a
     * specific nutrient with a given contribution amount.
     *
     * @param sourceId      the registry identifier of the food item
     * @param valueKey the nutrient key this food contributes to
     * @param amount      the nutrient contribution amount per consumption
     * @throws IllegalArgumentException if the nutrient key is not registered
     */
    @ApiStatus.Stable
    public static void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        dev.marie.framework.util.MarieValidation.requireNonNullId(sourceId, "NourishedAPI.registerSourceClassification");
        if (!Float.isFinite(amount))
            throw new IllegalArgumentException(
                    "NourishedAPI.registerSourceClassification.amount must be finite");
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(sourceId)) {
            org.slf4j.LoggerFactory.getLogger(NourishedAPI.class).warn("[NourishedAPI] registerSourceClassification: item '{}' not found in BuiltInRegistries.ITEM", sourceId);
        }
        MarieRegistryUtils.requireValueKey(valueKey, "NourishedAPI.registerSourceClassification");
        dev.marie.framework.api.marieapi.MarieAPI.registerSourceClassification(sourceId, valueKey, amount);
    }

    /**
     * Alias for {@link #registerSourceClassification(ResourceLocation, String, float)}.
     *
     * @param sourceId      the registry identifier of the food item
     * @param valueKey the nutrient key this food contributes to
     * @param amount      the nutrient contribution amount per consumption
     */
    @ApiStatus.Stable
    public static void registerSource(ResourceLocation sourceId, String valueKey, float amount) {
        registerSourceClassification(sourceId, valueKey, amount);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Effects & Thresholds
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a custom effect triggered by nutrient threshold crossings.
     *
     * @param definition the effect definition describing the trigger and effect
     * @throws IllegalArgumentException if the referenced nutrient or effect doesn't exist
     */
    @ApiStatus.Stable
    public static void registerCustomEffect(ThresholdEffect definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        dev.maire.nourished.core.effect.EffectRegistry.registerExternal(definition);
    }

    /**
     * Alias for {@link #registerCustomEffect(ThresholdEffect)}.
     *
     * @param definition the effect definition describing the trigger and effect
     */
    @ApiStatus.Stable
    public static void addEffect(ThresholdEffect definition) {
        registerCustomEffect(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Compatibility
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a compatibility entry that maps food items from another mod
     * to Nourished nutrient keys.
     *
     * @param definition the compat definition with food-to-nutrient mappings
     */
    @ApiStatus.Stable
    public static void registerCompatEntry(CompatDefinition definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        dev.marie.framework.compat.ModCompat.registerExternal(definition);
    }

    /**
     * Alias for {@link #registerCompatEntry(CompatDefinition)}.
     *
     * @param definition the compat definition with food-to-nutrient mappings
     */
    @ApiStatus.Stable
    public static void addCompat(CompatDefinition definition) {
        registerCompatEntry(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Synergies & Combos
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a nutrient synergy interaction between two nutrients.
     * When both nutrients meet their conditions simultaneously, the synergy
     * effect is applied.
     *
     * @param definition the nutrient synergy definition
     * @throws IllegalArgumentException if referenced nutrients don't exist
     */
    @ApiStatus.Stable
    public static void registerValueSynergy(SynergyDefinition definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        SynergyRegistry.registerValueSynergy(definition);
    }

    /**
     * Alias for {@link #registerValueSynergy(SynergyDefinition)}.
     *
     * @param definition the nutrient synergy definition
     */
    @ApiStatus.Stable
    public static void addNutrientSynergy(SynergyDefinition definition) {
        registerValueSynergy(definition);
    }

    /**
     * Registers a food synergy (meal combo) that grants bonus nutrition
     * when two foods are consumed within a time window.
     *
     * @param definition the food synergy definition
     */
    @ApiStatus.Stable
    public static void registerSourcePairSynergy(SourcePairSynergy definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        SynergyRegistry.registerSourcePairSynergy(definition);
    }

    /**
     * Alias for {@link #registerSourcePairSynergy(SourcePairSynergy)}.
     *
     * @param definition the food synergy definition
     */
    @ApiStatus.Stable
    public static void addFoodSynergy(SourcePairSynergy definition) {
        registerSourcePairSynergy(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Profiles & Milestones
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a named diet profile archetype that players can switch between.
     *
     * @param definition the diet profile definition with custom thresholds and bonuses
     * @throws IllegalArgumentException if a profile with the same id already exists
     */
    @ApiStatus.Stable
    public static void registerTrackingProfile(ProfileDefinition definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        ProfileRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerTrackingProfile(ProfileDefinition)}.
     *
     * @param definition the diet profile definition with custom thresholds and bonuses
     */
    @ApiStatus.Stable
    public static void addProfile(ProfileDefinition definition) {
        registerTrackingProfile(definition);
    }

    /**
     * Registers a nutrient milestone that fires once when a player reaches
     * a cumulative nutrition goal.
     *
     * @param definition the milestone definition
     * @throws IllegalArgumentException if a milestone with the same id already exists
     */
    @ApiStatus.Stable
    public static void registerMilestone(MilestoneDefinition definition) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        MilestoneRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerMilestone(MilestoneDefinition)}.
     *
     * @param definition the milestone definition
     */
    @ApiStatus.Stable
    public static void addMilestone(MilestoneDefinition definition) {
        registerMilestone(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Hooks & Modifiers
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a season hook for integrating with Serene Seasons or similar mods.
     * Seasonal modifiers will be applied to nutrient decay and absorption rates.
     *
     * @param hook the season hook implementation
     */
    @ApiStatus.Stable
    public static void registerSeasonHook(MarieSeasonHook hook) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        SeasonHookRegistry.register(hook);
    }

    /**
     * Alias for {@link #registerSeasonHook(MarieSeasonHook)}.
     *
     * @param hook the season hook implementation
     */
    @ApiStatus.Stable
    public static void addSeasonHook(MarieSeasonHook hook) {
        registerSeasonHook(hook);
    }

    /**
     * Registers a nutrient absorption modifier that dynamically adjusts how much
     * of a nutrient a player absorbs based on their current state.
     *
     * @param modifier the absorption modifier implementation
     */
    @ApiStatus.Stable
    public static void registerAbsorptionModifier(AbsorptionModifier modifier) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        AbsorptionModifierRegistry.register(modifier);
    }

    /**
     * Alias for {@link #registerAbsorptionModifier(AbsorptionModifier)}.
     *
     * @param modifier the absorption modifier implementation
     */
    @ApiStatus.Stable
    public static void addAbsorptionModifier(AbsorptionModifier modifier) {
        registerAbsorptionModifier(modifier);
    }

    /**
     * Registers a diet report provider that injects custom sections into the
     * {@code /nourished} command report output.
     *
     * @param provider the report provider implementation
     */
    @ApiStatus.Stable
    public static void registerReportProvider(ReportProvider provider) {
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        ReportProviderRegistry.register(provider);
    }

    /**
     * Alias for {@link #registerReportProvider(ReportProvider)}.
     *
     * @param provider the report provider implementation
     */
    @ApiStatus.Stable
    public static void addReportSection(ReportProvider provider) {
        registerReportProvider(provider);
    }
}
