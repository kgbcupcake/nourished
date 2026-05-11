package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.registry.RegistryLifecycleManager;
import dev.maire.nourished.tooling.data.NourishedDataManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

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
                    Nourished.LOGGER.info("[Nourished] Datapack config reload complete");
                }, executor2)
        );
    }
}
