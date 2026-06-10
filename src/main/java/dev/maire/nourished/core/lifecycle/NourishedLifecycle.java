package dev.maire.nourished.core.lifecycle;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.color.ColorRegistry;
import dev.marie.MariesLib.config.LockRegistry;
import dev.marie.MariesLib.config.ModCompatRegistry;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;

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
                "ColorRegistry", ColorRegistry::load, ColorRegistry::reload, ColorRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "EffectRegistry", EffectRegistry::load, EffectRegistry::reload, EffectRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "RawFoodConfig", RawFoodConfig::load, RawFoodConfig::reload, RawFoodConfig::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "StaminaConfig", StaminaConfig::load, StaminaConfig::reload, StaminaConfig::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodValueRegistry", FoodValueRegistry::load, FoodValueRegistry::reload, FoodValueRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodOverrideRegistry", FoodOverrideRegistry::load, FoodOverrideRegistry::reload, FoodOverrideRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ScannerSpecRegistry", ScannerSpecRegistry::load, ScannerSpecRegistry::reload, ScannerSpecRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "LockRegistry", LockRegistry::load, LockRegistry::reload, LockRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ModCompatRegistry", ModCompatRegistry::load, () -> {});
        RegistryLifecycleManager.registerRegistry(
                "PresetRegistry", PresetRegistry::ensureBuiltInFilesOnDisk, PresetRegistry::reload);
    }
}
