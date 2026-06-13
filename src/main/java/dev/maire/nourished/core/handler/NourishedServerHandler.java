package dev.maire.nourished.core.handler;

import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import dev.marie.MariesLib.api.ApiStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@ApiStatus.Internal
public final class NourishedServerHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        FoodNutritionRegistry.bindServerRecipeManager(event.getServer().getRecipeManager());
        NourishedReloadHelper.reloadAndBroadcast(event.getServer());
        NourishedSyncHandler.logServerStartupInfo();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        FoodNutritionRegistry.bindServerRecipeManager(null);
    }
}
