package dev.maire.nourished.client;

import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.network.ModNetworking.SyncDietDeltaPayload;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks last client nutrient snapshot and queues toasts when a value crosses into the critical band.
 */
public final class NourishedToastManager {

    private static final Map<String, Float> lastNutrients = new HashMap<>();
    private static boolean firstClientSync = true;

    private NourishedToastManager() {}

    /**
     * Call when full DietData sync applies on the client (login, respawn, dimension change).
     * Seeds state on first sync; then detects {@code before >= critical && after < critical} per nutrient.
     */
    public static void onClientDietUpdated(DietData next) {
        processNutrientUpdate(next.nutrients);
    }

    /**
     * Call when lightweight delta sync applies on the client (every food eat, decay tick).
     * Uses the same critical threshold detection as the full sync.
     */
    public static void onClientDietUpdated(SyncDietDeltaPayload delta) {
        processNutrientUpdate(delta.nutrients());
    }

    private static void processNutrientUpdate(Map<String, Float> nextNutrients) {
        List<String> keys = DietData.barOrder();

        if (firstClientSync) {
            firstClientSync = false;
            for (String key : keys) {
                lastNutrients.put(key, nextNutrients.getOrDefault(key, 0f));
            }
            return;
        }

        NourishedConfig config = NourishedConfig.get();
        Minecraft mc = Minecraft.getInstance();

        if (ModuleCache.enableEffects && ModuleCache.enableToasts && ModuleCache.enableCriticalToasts && mc.player != null) {
            for (String key : keys) {
                double crit = config.criticalThresholdFor(key);
                float before = lastNutrients.getOrDefault(key, 0f);
                float after = nextNutrients.getOrDefault(key, 0f);
                if (before >= crit && after < crit) {
                    ItemStack icon = new ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse(NutrientRegistry.getIcon(key))));
                    mc.getToasts().addToast(new CriticalNutrientToast(key, icon));
                }
            }
        }

        for (String key : keys) {
            lastNutrients.put(key, nextNutrients.getOrDefault(key, 0f));
        }
    }
}
