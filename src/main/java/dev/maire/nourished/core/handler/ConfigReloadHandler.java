package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.reload.NourishedReloadPipeline;
import dev.marie.MariesLib.data.MarieDataManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@ApiStatus.Internal
public class ConfigReloadHandler {

    private static volatile boolean reloadInProgress;

    /**
     * Returns {@code true} while a config or datapack reload triggered by this handler is underway.
     */
    public static boolean isReloadInProgress() {
        return reloadInProgress || RegistryLifecycleManager.isReloadInProgress();
    }

    /**
     * Reloads config-backed registries once when the dedicated server starts.
     * Previously hooked {@code LevelEvent.Load}, which fires per dimension and caused
     * duplicate full reload cycles (overworld, nether, end) on every startup.
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        reloadInProgress = true;
        try {
            NourishedReloadPipeline.reloadAll();
        } finally {
            reloadInProgress = false;
        }
        NourishedSyncHandler.setConfigSnapshot(
                SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get()));
        NourishedSyncHandler.logServerStartupInfo();
    }

    /**
     * Rebuilds the config snapshot from current config and broadcasts it to all connected players.
     * Call this after a {@code /nourished reload} completes.
     */
    public static void reloadAndBroadcast(MinecraftServer server) {
        NourishedSyncHandler.setConfigSnapshot(
                SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get()));
        NourishedSyncHandler.broadcastConfigReload(server);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        MarieDataManager.registerReloadListener(event);
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    reloadInProgress = true;
                    try {
                        RegistryLifecycleManager.loadAll(resourceManager);
                        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                        if (server != null) {
                            FoodNutritionRegistry.bindServerRecipeManager(server.getRecipeManager());
                        }
                        Nourished.LOGGER.info("[Nourished] Datapack config reload complete");
                    } finally {
                        reloadInProgress = false;
                    }
                }, executor2)
        );
    }
}
