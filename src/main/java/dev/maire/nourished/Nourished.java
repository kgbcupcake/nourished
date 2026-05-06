package dev.maire.nourished;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.maire.nourished.command.NourishedCommand;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.config.NourishedConfigScreen;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.color.ColorRegistry;
import dev.maire.nourished.client.ClientEventRegistrar;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.handler.ConfigReloadHandler;
import dev.maire.nourished.handler.DietPlayerEvents;
import dev.maire.nourished.handler.FoodEatenHandler;
import dev.maire.nourished.handler.NutritionDecayHandler;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.nutrition.FoodValueRegistry;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Nourished.MODID)
public class Nourished {

    public static final String MODID = "nourished";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Nourished(IEventBus modEventBus, ModContainer modContainer) {
        NutrientRegistry.load();
        FoodValueRegistry.load();
        FoodOverrideRegistry.load();
        EffectRegistry.load();
        ColorRegistry.load();
        LockRegistry.load();
        PresetRegistry.ensureBuiltInFilesOnDisk();
        NourishedConfig.register(modContainer);
        NourishedClientConfig.register(modContainer);
        modEventBus.addListener(NourishedConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigReloading);
        FoodNutritionRegistry.init();
        DietAttachment.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEventRegistrar.register(modEventBus);
        }
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> NourishedConfigScreen.create(parent));
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.register(new FoodEatenHandler());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            if (!ModCompat.LSO_LOADED || !NourishedConfig.get().isCodeCompatEnabled("legendarysurvivaloverhaul")) {
                NeoForge.EVENT_BUS.register(new NutritionDecayHandler());
            }
        });
        NeoForge.EVENT_BUS.register(new DietPlayerEvents());
        NeoForge.EVENT_BUS.register(new ConfigReloadHandler());
        NeoForge.EVENT_BUS.register(new NourishedCommand());
        LOGGER.info("Nourished loaded.");
    }
}
