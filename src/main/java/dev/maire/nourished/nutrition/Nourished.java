package dev.maire.nourished.nutrition;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedAPIVersion;
import dev.maire.nourished.api.NourishedAPIState;
import dev.maire.nourished.registry.NourishedApiDefinitionRegistries;
import dev.maire.nourished.command.NourishedCommand;
import dev.maire.nourished.compat.kubejs.NourishedKubeJSPlugin;
import dev.maire.nourished.compat.AutoCompatDiscovery;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.compat.peakstamina.PeakStaminaCompat;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedConfigScreen;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.color.ColorRegistry;
import dev.maire.nourished.client.ClientEventRegistrar;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.handler.ConfigReloadHandler;
import dev.maire.nourished.handler.DietPlayerEvents;
import dev.maire.nourished.handler.FoodEatenHandler;
import dev.maire.nourished.handler.NutritionDecayHandler;
import dev.maire.nourished.handler.NutritionEffectsHandler;
import dev.maire.nourished.handler.SleepBonusHandler;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.scanner.ScannerSpecRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Nourished.MODID)
@ApiStatus.Internal
public class Nourished {

    public static final String MODID = "nourished";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Nourished(IEventBus modEventBus, ModContainer modContainer) {
        NutrientRegistry.load();
        ModCompat.initialize();
        if (ModList.get().isLoaded("peakstamina")) {
            PeakStaminaCompat.register();
        }
        FoodValueRegistry.load();
        FoodOverrideRegistry.load();
        EffectRegistry.load();
        ColorRegistry.load();
        LockRegistry.load();
        ScannerSpecRegistry.load();
        PresetRegistry.ensureBuiltInFilesOnDisk();
        NourishedConfig.register(modContainer);
        NourishedClientConfig.register(modContainer);
        modEventBus.addListener(NourishedConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigReloading);
        FoodNutritionRegistry.init();
        DietAttachment.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> NourishedConfigScreen.create(parent));
            ClientEventRegistrar.register(modEventBus);
        }
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.register(new FoodEatenHandler());
        NeoForge.EVENT_BUS.register(new NutritionEffectsHandler());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(() -> {
                ModCompat.discoverUnknownMods();
                if (!ModCompat.shouldDisableEffects()) {
                    NeoForge.EVENT_BUS.register(new NutritionDecayHandler());
                }
                LOGGER.info("[Nourished] Starting AutoCompatDiscovery...");
                try (var scope = NourishedAPIState.openForDatapackReload()) {
                    AutoCompatDiscovery.discover();
                } catch (Exception e) {
                    LOGGER.error("[Nourished] AutoCompatDiscovery failed.", e);
                }
                NourishedApiDefinitionRegistries.freezeModOnlyRegistriesAfterCommonSetup();
                NourishedAPIState.close();
            });
        });
        NeoForge.EVENT_BUS.register(new DietPlayerEvents());
        NeoForge.EVENT_BUS.register(new SleepBonusHandler());
        NeoForge.EVENT_BUS.register(new ConfigReloadHandler());
        NeoForge.EVENT_BUS.register(new NourishedCommand());
        if (ModList.get().isLoaded("kubejs")) {
            try {
                NourishedKubeJSPlugin.bootstrap();
                LOGGER.info("[Nourished] Enabled KubeJS integration bridge.");
            } catch (Throwable t) {
                LOGGER.warn("[Nourished] Failed to initialize KubeJS integration bridge.", t);
            }
        }
        DietAttachment.logAllNutrientNbtPaths();
        LOGGER.info("[Nourished] Calories NBT path: {}", DietAttachment.getCaloriesNbtPath());
        LOGGER.info("[Nourished] API v{} ready — {} nutrients, {} effects, {} compat entries registered.",
                NourishedAPIVersion.VERSION,
                NutrientRegistry.getAll().size(),
                EffectRegistry.getAll().size(),
                ModCompat.getAllEntries().size());
        LOGGER.info("Nourished loaded.");
    }
}
