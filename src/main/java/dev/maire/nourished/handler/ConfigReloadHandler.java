package dev.maire.nourished.handler;

import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.color.ColorRegistry;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.nutrition.FoodValueRegistry;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class ConfigReloadHandler {

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            NutrientRegistry.reload();
            FoodValueRegistry.reload();
            FoodOverrideRegistry.reload();
            EffectRegistry.reload();
            PresetRegistry.reload();
            ColorRegistry.reload();
            LockRegistry.reload();
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    FoodValueRegistry.loadFromDatapack(resourceManager);
                    FoodOverrideRegistry.loadFromDatapack(resourceManager);
                    EffectRegistry.loadFromDatapack(resourceManager);
                    ColorRegistry.loadFromDatapack(resourceManager);
                    LockRegistry.loadFromDatapack(resourceManager);
                    Nourished.LOGGER.info("[Nourished] Datapack config reload complete");
                }, executor2)
        );
    }
}
