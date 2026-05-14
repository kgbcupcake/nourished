package dev.maire.nourished.core.reload;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.diagnostics.NourishedUnknownFoodLogger;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.registry.RegistryLifecycleManager;

/**
 * Thin shim that delegates to {@link RegistryLifecycleManager#reloadAll()}.
 *
 * <p>Retained so existing callers (preset import, admin command, config screen) continue to work
 * without churn. New code should call {@link RegistryLifecycleManager#reloadAll()} directly.</p>
 */
@ApiStatus.Internal
public final class NourishedReloadPipeline {

    private NourishedReloadPipeline() {}

    public static void reloadAll() {
        RegistryLifecycleManager.reloadAll();
        FoodFamilyResolver.clearCache();
        RuntimeFoodResolver.getInstance().invalidateCache();
        NourishedUnknownFoodLogger.onReload();
    }
}
