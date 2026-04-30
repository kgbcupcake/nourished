package dev.maire.nourished.handler;

import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class ConfigReloadHandler {

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            NutrientRegistry.reload();
        }
    }
}
