package dev.maire.nourished.core.reload;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.core.color.ColorRegistry;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;

@ApiStatus.Internal
public final class NourishedReloadPipeline {

    private NourishedReloadPipeline() {}

    public static void reloadAll() {
        NutrientRegistry.reload();
        FoodValueRegistry.reload();
        FoodOverrideRegistry.reload();
        EffectRegistry.reload();
        PresetRegistry.reload();
        ColorRegistry.reload();
        LockRegistry.reload();
        ScannerSpecRegistry.reload();
    }
}
