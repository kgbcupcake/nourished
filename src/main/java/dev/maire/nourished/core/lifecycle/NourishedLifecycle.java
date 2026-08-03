package dev.maire.nourished.core.lifecycle;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.color.ColorRegistry;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.config.NourishedPresetRegistry;
import dev.marie.framework.config.ModCompatRegistry;
import dev.marie.framework.registry.RegistryLifecycleManager;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import dev.maire.nourished.core.effect.EffectRegistry;
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
 * so it loads first. Color/Effect/FoodValue/FoodOverride/ScannerSpec are independent JSON loads.
 * Lock/ModCompat/PresetRegistry are appended at the end; {@code ModCompatRegistry} has no reload
 * hook (no-op preserves prior behavior since it was absent from the legacy reload pipeline).</p>
 */
@ApiStatus.Internal
public final class NourishedLifecycle {

    private NourishedLifecycle() {}

    public static void register() {
        RegistryLifecycleManager.registerRegistry(
                "NutrientRegistry", NutrientRegistry::load, NutrientRegistry::reload);
        RegistryLifecycleManager.registerRegistry(
                "NutrientCurveRegistry", NutrientCurveRegistry::load, NutrientCurveRegistry::reload,
                NutrientCurveRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ColorRegistry", ColorRegistry::load, ColorRegistry::reload, ColorRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "EffectRegistry", EffectRegistry::load, EffectRegistry::reload, EffectRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "RawFoodConfig", RawFoodConfig::load, RawFoodConfig::reload, RawFoodConfig::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodValueRegistry", FoodValueRegistry::load, FoodValueRegistry::reload, FoodValueRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodOverrideRegistry", FoodOverrideRegistry::load, FoodOverrideRegistry::reload, FoodOverrideRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "NutrientWeightRegistry", NutrientWeightRegistry::load, NutrientWeightRegistry::reload,
                NutrientWeightRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ScannerSpecRegistry", ScannerSpecRegistry::load, ScannerSpecRegistry::reload, ScannerSpecRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "NourishedLockRegistry", NourishedLockRegistry::load, NourishedLockRegistry::reload,
                NourishedLockRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ModCompatRegistry", ModCompatRegistry::load, () -> {});
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
