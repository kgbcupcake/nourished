package dev.maire.nourished.core.reload;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientRegistry;
import dev.marie.framework.handler.ReloadPipeline;
import dev.marie.framework.resources.api.MarieResourcesAPI;
import dev.marie.framework.runtime.RuntimeResolver;
import dev.marie.framework.runtime.SourceRegistry;
import net.minecraft.server.MinecraftServer;

public final class NourishedReloadHelper {

    private NourishedReloadHelper() {}

    /**
     * Reloads MarieLib/Nourished registries and clears stale scanner classifications so
     * {@code NutrientClassificationLookup} can fall through to live inference after reload.
     */
    public static void reloadAll() {
        ReloadPipeline.reloadAll();
        invalidateClassificationCaches();
    }

    public static void reloadAndBroadcast(MinecraftServer server) {
        Nourished.reregisterReloadableDefinitions();
        invalidateClassificationCaches();
        NourishedSyncHandler.setConfigSnapshot(
                SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get()));
        NourishedSyncHandler.broadcastConfigReload(server);
        MarieResourcesAPI.broadcastConfigSyncReload(server, ActivityDrivenNutrientRegistry.SYNC_ID);
    }

    private static void invalidateClassificationCaches() {
        SourceRegistry.clearScannerClassifications();
        FoodFamilyResolver.clearCache();
        RuntimeResolver.getInstance().invalidateCache();
        RawFoodClassifier.invalidate();
    }
}
