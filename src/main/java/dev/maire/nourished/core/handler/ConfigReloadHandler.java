package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.reload.NourishedReloadPipeline;
import dev.maire.nourished.tooling.data.NourishedDataManager;
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
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        NourishedDataManager.registerReloadListener(event);
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
