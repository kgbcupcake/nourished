package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.core.color.ColorRegistry;
import dev.maire.nourished.data.NourishedDataManager;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.reload.NourishedReloadPipeline;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@ApiStatus.Internal
public class ConfigReloadHandler {

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            NourishedReloadPipeline.reloadAll();
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        NourishedDataManager.registerReloadListener(event);
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    FoodValueRegistry.loadFromDatapack(resourceManager);
                    FoodOverrideRegistry.loadFromDatapack(resourceManager);
                    EffectRegistry.loadFromDatapack(resourceManager);
                    ColorRegistry.loadFromDatapack(resourceManager);
                    LockRegistry.loadFromDatapack(resourceManager);
                    ScannerSpecRegistry.loadFromDatapack(resourceManager);
                    Nourished.LOGGER.info("[Nourished] Datapack config reload complete");
                }, executor2)
        );
    }
}
