package dev.maire.nourished.core.handler;

import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientTagClassificationSync;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class NourishedServerHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        FoodNutritionRegistry.bindServerRecipeManager(event.getServer().getRecipeManager());
        NutrientTagClassificationSync.syncFromNutrientTags();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        FoodNutritionRegistry.bindServerRecipeManager(null);
    }
}
