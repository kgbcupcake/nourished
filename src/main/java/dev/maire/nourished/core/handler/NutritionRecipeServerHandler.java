package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.marie.MariesLib.scanner.ItemScanner;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Caches {@link net.minecraft.world.item.crafting.RecipeManager} for
 * {@link FoodNutritionRegistry} after the server is ready and clears it on shutdown.
 */
@ApiStatus.Internal
public final class NutritionRecipeServerHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        FoodNutritionRegistry.bindServerRecipeManager(event.getServer().getRecipeManager());
        ItemScanner.scanAndApply(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        FoodNutritionRegistry.bindServerRecipeManager(null);
        FoodNutritionRegistry.clearScannerClassifications();
        ItemScanner.invalidateCache();
    }
}
