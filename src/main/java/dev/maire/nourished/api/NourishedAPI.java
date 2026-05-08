package dev.maire.nourished.api;

import dev.maire.nourished.api.impl.DietDataFoodMemoryView;
import dev.maire.nourished.api.registry.AbsorptionModifierRegistry;
import dev.maire.nourished.api.registry.DietProfileRegistry;
import dev.maire.nourished.api.registry.MilestoneRegistry;
import dev.maire.nourished.api.registry.ReportProviderRegistry;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.api.registry.SynergyRegistry;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

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
        DietData diet = player.getData(DietAttachment.DIET.get());
        return diet.calories;
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
        DietData diet = player.getData(DietAttachment.DIET.get());
        Float value = diet.nutrients.get(nutrientKey);
        return value != null ? value : -1.0f;
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
        DietData diet = player.getData(DietAttachment.DIET.get());
        return new DietDataFoodMemoryView(diet);
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
        if (NutrientRegistry.getKeys().contains(definition.getId())) {
            throw new IllegalArgumentException("Nutrient already registered: " + definition.getId());
        }
        NutrientRegistry.registerExternal(definition.getId(), definition.getDisplayName());
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
        if (!NutrientRegistry.getKeys().contains(nutrientKey)) {
            throw new IllegalArgumentException("Unknown nutrient key: " + nutrientKey);
        }
        dev.maire.nourished.nutrition.FoodNutritionRegistry.registerClassification(foodId, nutrientKey, amount);
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
        dev.maire.nourished.effect.EffectRegistry.registerExternal(definition);
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
        dev.maire.nourished.compat.ModCompat.registerExternal(definition);
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
        SynergyRegistry.registerNutrientSynergy(definition);
    }

    /**
     * Registers a food synergy (meal combo) that grants bonus nutrition
     * when two foods are consumed within a time window.
     *
     * @param definition the food synergy definition
     */
    public static void registerFoodSynergy(FoodSynergyDefinition definition) {
        SynergyRegistry.registerFoodSynergy(definition);
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
        DietProfileRegistry.register(definition);
    }

    /**
     * Registers a nutrient milestone that fires once when a player reaches
     * a cumulative nutrition goal.
     *
     * @param definition the milestone definition
     * @throws IllegalArgumentException if a milestone with the same id already exists
     */
    public static void registerMilestone(NutrientMilestoneDefinition definition) {
        MilestoneRegistry.register(definition);
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
        SeasonHookRegistry.register(hook);
    }

    /**
     * Registers a nutrient absorption modifier that dynamically adjusts how much
     * of a nutrient a player absorbs based on their current state.
     *
     * @param modifier the absorption modifier implementation
     */
    public static void registerAbsorptionModifier(NutrientAbsorptionModifier modifier) {
        AbsorptionModifierRegistry.register(modifier);
    }

    /**
     * Registers a diet report provider that injects custom sections into the
     * {@code /nourished} command report output.
     *
     * @param provider the report provider implementation
     */
    public static void registerReportProvider(DietReportProvider provider) {
        ReportProviderRegistry.register(provider);
    }
}
