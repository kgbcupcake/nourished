package dev.maire.nourished.api;

import dev.maire.nourished.api.registry.AbsorptionModifierRegistry;
import dev.maire.nourished.api.registry.DietProfileRegistry;
import dev.maire.nourished.api.registry.MilestoneRegistry;
import dev.maire.nourished.api.registry.ReportProviderRegistry;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.api.registry.SynergyRegistry;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.impl.EmptyFoodMemoryView;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collections;
import java.util.LinkedHashMap;
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
    public static float getCalories(Player player) {
        if (player == null) {
            return 0f;
        }
        return DietAttachment.getCalories(player);
    }

    /**
     * Returns the current level of a specific nutrient for the given player.
     *
     * @param player      the player to query
     * @param nutrientKey the internal key of the nutrient (e.g. "protein", "iron")
     * @return the nutrient level as a normalized float (0.0 to 1.0),
     *         or {@code -1.0f} if the nutrient key is not recognized
     */
    public static float getNutrientLevel(Player player, String nutrientKey) {
        if (player == null) {
            return -1.0f;
        }
        return DietAttachment.getNutrientLevel(player, nutrientKey);
    }

    /**
     * Returns a read-only view of the player's food consumption memory,
     * exposing recent eating history and variety information.
     *
     * @param player the player to query
     * @return a {@link FoodMemoryView} for the given player
     * @throws IllegalStateException if the nutrition system is not initialized
     */
    public static FoodMemoryView getFoodMemory(Player player) {
        if (player == null) {
            return EmptyFoodMemoryView.INSTANCE;
        }
        return DietAttachment.getFoodMemoryView(player);
    }

    /**
     * Alias for {@link #getNutrientLevel(Player, String)}.
     *
     * @param player      the player to query
     * @param nutrientKey the nutrient key to query
     * @return the current nutrient level, or {@code -1.0f} if unrecognized
     */
    @ApiStatus.Stable
    public static float getNutrition(Player player, String nutrientKey) {
        return getNutrientLevel(player, nutrientKey);
    }

    /**
     * Alias for {@link #getCalories(Player)}.
     *
     * @param player the player to query
     * @return the player's current calorie value
     */
    @ApiStatus.Stable
    public static float getCalorieCount(Player player) {
        return getCalories(player);
    }

    /**
     * Returns an aggregated diet snapshot for the given player including
     * calories, all registered nutrient levels, and food memory state.
     *
     * @param player the player to query
     * @return aggregated player diet state snapshot
     */
    @ApiStatus.Stable
    public static NourishedPlayerData getDietData(Player player) {
        Map<String, Float> nutrients = new LinkedHashMap<>();
        for (String nutrientKey : NutrientRegistry.getKeys()) {
            nutrients.put(nutrientKey, getNutrientLevel(player, nutrientKey));
        }
        return new NourishedPlayerData(
                getCalories(player),
                Collections.unmodifiableMap(nutrients),
                getFoodMemory(player)
        );
    }

    /**
     * Applies a direct nutrient delta by posting a {@link NutrientModifierEvent}
     * and then applying the final event amount if the event is not cancelled.
     *
     * @param player      the player to modify
     * @param nutrientKey the nutrient key to modify
     * @param delta       the nutrient delta to apply
     */
    @ApiStatus.Stable
    public static void modifyNutrition(Player player, String nutrientKey, float delta) {
        NutrientModifierEvent modifierEvent = new NutrientModifierEvent(player, API_MODIFIER_SOURCE, nutrientKey, delta);
        NeoForge.EVENT_BUS.post(modifierEvent);
        if (modifierEvent.isCanceled()) {
            return;
        }
        DietData diet = player.getData(DietAttachment.DIET.get());
        diet.addNutrient(nutrientKey, modifierEvent.getAmount());
        player.setData(DietAttachment.DIET.get(), diet);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ModNetworking.syncDietDelta(serverPlayer, diet);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(serverPlayer, diet);
        }
    }

    @ApiStatus.Stable
    public static String getVersion() {
        return NourishedAPIVersion.VERSION;
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
    public static void registerNutrient(NutrientDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        if (NutrientRegistry.getKeys().contains(definition.getId())) {
            throw new IllegalArgumentException("Nutrient already registered: " + definition.getId());
        }
        NutrientRegistry.registerExternal(definition);
    }

    /**
     * Alias for {@link #registerNutrient(NutrientDefinition)}.
     *
     * @param definition the nutrient definition to register
     */
    @ApiStatus.Stable
    public static void addNutrient(NutrientDefinition definition) {
        registerNutrient(definition);
    }

    /**
     * Registers a food item's nutrient classification, mapping a food to a
     * specific nutrient with a given contribution amount.
     *
     * @param foodId      the registry identifier of the food item
     * @param nutrientKey the nutrient key this food contributes to
     * @param amount      the nutrient contribution amount per consumption
     * @throws IllegalArgumentException if the nutrient key is not registered
     */
    public static void registerFoodClassification(ResourceLocation foodId, String nutrientKey, float amount) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        dev.maire.nourished.core.util.NourishedValidation.requireNonNullId(foodId, "NourishedAPI.registerFoodClassification");
        dev.maire.nourished.core.util.NourishedValidation.requireFinite(amount, -10f, 10f, "NourishedAPI.registerFoodClassification.amount");
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(foodId)) {
            org.slf4j.LoggerFactory.getLogger(NourishedAPI.class).warn("[NourishedAPI] registerFoodClassification: item '{}' not found in BuiltInRegistries.ITEM", foodId);
        }
        NourishedRegistryUtils.requireNutrientKey(nutrientKey, "NourishedAPI.registerFoodClassification");
        dev.maire.nourished.core.nutrition.FoodNutritionRegistry.registerClassification(foodId, nutrientKey, amount);
    }

    /**
     * Alias for {@link #registerFoodClassification(ResourceLocation, String, float)}.
     *
     * @param foodId      the registry identifier of the food item
     * @param nutrientKey the nutrient key this food contributes to
     * @param amount      the nutrient contribution amount per consumption
     */
    @ApiStatus.Stable
    public static void registerFood(ResourceLocation foodId, String nutrientKey, float amount) {
        registerFoodClassification(foodId, nutrientKey, amount);
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
    public static void registerCustomEffect(EffectDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        dev.maire.nourished.core.effect.EffectRegistry.registerExternal(definition);
    }

    /**
     * Alias for {@link #registerCustomEffect(EffectDefinition)}.
     *
     * @param definition the effect definition describing the trigger and effect
     */
    @ApiStatus.Stable
    public static void addEffect(EffectDefinition definition) {
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
    public static void registerCompatEntry(CompatDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        dev.maire.nourished.compat.ModCompat.registerExternal(definition);
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
    public static void registerNutrientSynergy(NutrientSynergyDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        SynergyRegistry.registerNutrientSynergy(definition);
    }

    /**
     * Alias for {@link #registerNutrientSynergy(NutrientSynergyDefinition)}.
     *
     * @param definition the nutrient synergy definition
     */
    @ApiStatus.Stable
    public static void addNutrientSynergy(NutrientSynergyDefinition definition) {
        registerNutrientSynergy(definition);
    }

    /**
     * Registers a food synergy (meal combo) that grants bonus nutrition
     * when two foods are consumed within a time window.
     *
     * @param definition the food synergy definition
     */
    public static void registerFoodSynergy(FoodSynergyDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        SynergyRegistry.registerFoodSynergy(definition);
    }

    /**
     * Alias for {@link #registerFoodSynergy(FoodSynergyDefinition)}.
     *
     * @param definition the food synergy definition
     */
    @ApiStatus.Stable
    public static void addFoodSynergy(FoodSynergyDefinition definition) {
        registerFoodSynergy(definition);
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
    public static void registerDietProfile(DietProfileDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        DietProfileRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerDietProfile(DietProfileDefinition)}.
     *
     * @param definition the diet profile definition with custom thresholds and bonuses
     */
    @ApiStatus.Stable
    public static void addProfile(DietProfileDefinition definition) {
        registerDietProfile(definition);
    }

    /**
     * Registers a nutrient milestone that fires once when a player reaches
     * a cumulative nutrition goal.
     *
     * @param definition the milestone definition
     * @throws IllegalArgumentException if a milestone with the same id already exists
     */
    public static void registerMilestone(NutrientMilestoneDefinition definition) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        MilestoneRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerMilestone(NutrientMilestoneDefinition)}.
     *
     * @param definition the milestone definition
     */
    @ApiStatus.Stable
    public static void addMilestone(NutrientMilestoneDefinition definition) {
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
    public static void registerSeasonHook(NourishedSeasonHook hook) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        SeasonHookRegistry.register(hook);
    }

    /**
     * Alias for {@link #registerSeasonHook(NourishedSeasonHook)}.
     *
     * @param hook the season hook implementation
     */
    @ApiStatus.Stable
    public static void addSeasonHook(NourishedSeasonHook hook) {
        registerSeasonHook(hook);
    }

    /**
     * Registers a nutrient absorption modifier that dynamically adjusts how much
     * of a nutrient a player absorbs based on their current state.
     *
     * @param modifier the absorption modifier implementation
     */
    public static void registerAbsorptionModifier(NutrientAbsorptionModifier modifier) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        AbsorptionModifierRegistry.register(modifier);
    }

    /**
     * Alias for {@link #registerAbsorptionModifier(NutrientAbsorptionModifier)}.
     *
     * @param modifier the absorption modifier implementation
     */
    @ApiStatus.Stable
    public static void addAbsorptionModifier(NutrientAbsorptionModifier modifier) {
        registerAbsorptionModifier(modifier);
    }

    /**
     * Registers a diet report provider that injects custom sections into the
     * {@code /nourished} command report output.
     *
     * @param provider the report provider implementation
     */
    public static void registerReportProvider(DietReportProvider provider) {
        if (!NourishedAPIState.isRegistrationAllowed()) throw new IllegalStateException("NourishedAPI registration is closed — register during mod initialization only.");
        ReportProviderRegistry.register(provider);
    }

    /**
     * Alias for {@link #registerReportProvider(DietReportProvider)}.
     *
     * @param provider the report provider implementation
     */
    @ApiStatus.Stable
    public static void addReportSection(DietReportProvider provider) {
        registerReportProvider(provider);
    }
}
