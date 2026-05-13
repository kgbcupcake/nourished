package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.registry.RegistryLifecycleManager;
import dev.maire.nourished.tooling.data.NourishedDataManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@ApiStatus.Internal
public class ConfigReloadHandler {

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            RegistryLifecycleManager.reloadAll();
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        NourishedDataManager.registerReloadListener(event);
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    RegistryLifecycleManager.loadAll(resourceManager);
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        FoodNutritionRegistry.bindServerRecipeManager(server.getRecipeManager());
                    }
                    Nourished.LOGGER.info("[Nourished] Datapack config reload complete");
                }, executor2)
        );
    }
}
