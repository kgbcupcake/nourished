package dev.maire.nourished;

import com.mojang.logging.LogUtils;
import dev.maire.nourished.attachment.NutritionAttachment;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.handler.ConfigReloadHandler;
import dev.maire.nourished.handler.DietPlayerEvents;
import dev.maire.nourished.handler.FoodEatenHandler;
import dev.maire.nourished.handler.NutritionDecayHandler;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Nourished.MODID)
public class Nourished {

    public static final String MODID = "nourished";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Nourished(IEventBus modEventBus, ModContainer modContainer) {
        NutrientRegistry.load();
        FoodNutritionRegistry.init();
        NutritionAttachment.register(modEventBus);
        DietAttachment.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.register(new FoodEatenHandler());
        if (!ModCompat.LSO_LOADED) {
            NeoForge.EVENT_BUS.register(new NutritionDecayHandler());
        }
        NeoForge.EVENT_BUS.register(new DietPlayerEvents());
        NeoForge.EVENT_BUS.register(new ConfigReloadHandler());
        LOGGER.info("Nourished loaded.");
    }
}
