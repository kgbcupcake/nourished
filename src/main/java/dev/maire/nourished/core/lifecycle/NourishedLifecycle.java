package dev.maire.nourished.core.lifecycle;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.config.NourishedPresetRegistry;
import dev.marie.framework.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.ExcludedTooltipPulseRegistry;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NutrientWeightRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveRegistry;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientRegistry;

/**
 * Registers all config-backed registries with {@link RegistryLifecycleManager} in dependency
 * order. Called exactly once during mod construction before {@link RegistryLifecycleManager#loadAll()}.
 *
 * <p>Order rationale: {@code NutrientRegistry} provides keys consumed by every other registry,
 * so it loads first. Effect/FoodValue/FoodOverride are independent JSON loads, and Lock/Preset are
 * appended at the end. Color and ScannerSpec are registered by MarieBootstrap itself.</p>
 */
@ApiStatus.Internal
public final class NourishedLifecycle {

    private NourishedLifecycle() {}

    public static void register() {
        RegistryLifecycleManager.registerRegistry(
                "NutrientRegistry", NutrientRegistry::load, NutrientRegistry::reload,
                NutrientRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "NutrientCurveRegistry", NutrientCurveRegistry::load, NutrientCurveRegistry::reload,
                NutrientCurveRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "EffectRegistry", EffectRegistry::load, EffectRegistry::reload, EffectRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "RawFoodConfig", RawFoodConfig::load, RawFoodConfig::reload, RawFoodConfig::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodValueRegistry", FoodValueRegistry::load, FoodValueRegistry::reload, FoodValueRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodOverrideRegistry", FoodOverrideRegistry::load, FoodOverrideRegistry::reload, FoodOverrideRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ExcludedFoodOverrideRegistry", ExcludedFoodOverrideRegistry::load, ExcludedFoodOverrideRegistry::reload);
        RegistryLifecycleManager.registerRegistry(
                "ExcludedTooltipPulseRegistry", ExcludedTooltipPulseRegistry::load, ExcludedTooltipPulseRegistry::reload);
        RegistryLifecycleManager.registerRegistry(
                "NutrientWeightRegistry", NutrientWeightRegistry::load, NutrientWeightRegistry::reload,
                NutrientWeightRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "NourishedLockRegistry", NourishedLockRegistry::load, NourishedLockRegistry::reload,
                NourishedLockRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "NourishedPresetRegistry", NourishedPresetRegistry::ensureBuiltInFilesOnDisk,
                NourishedPresetRegistry::reload);
        RegistryLifecycleManager.registerRegistry(
                "ActivityDrivenNutrientRegistry", ActivityDrivenNutrientRegistry::load,
                ActivityDrivenNutrientRegistry::reload, ActivityDrivenNutrientRegistry::loadFromDatapack);

        // NutrientRegistry.loadDefinitions() already ran in the mod constructor, so real
        // nutrient colors are available here for seeding tooltip_colors.json's defaults.
        NourishedTooltipDefaults.seed();
    }
}
