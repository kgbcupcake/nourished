package dev.maire.nourished.core.reload;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import dev.marie.MariesLib.handler.ReloadPipeline;
import dev.marie.MariesLib.runtime.SourceRegistry;
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
        invalidateClassificationCaches();
        NourishedSyncHandler.setConfigSnapshot(
                SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get()));
        NourishedSyncHandler.broadcastConfigReload(server);
    }

    private static void invalidateClassificationCaches() {
        SourceRegistry.clearScannerClassifications();
        FoodFamilyResolver.clearCache();
        RuntimeFoodResolver.getInstance().invalidateCache();
        RawFoodClassifier.invalidate();
    }
}
