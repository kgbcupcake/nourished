package dev.maire.nourished.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.data.MarieDataManager;
import dev.maire.nourished.core.datapack.NourishedDatapackCallbacks;
import dev.marie.MariesLib.registry.MarieApiRegistries;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.context.NourishedContextBuilder;
import dev.maire.nourished.core.lifecycle.NourishedLifecycle;
import dev.marie.MariesLib.core.MariesLibBootstrap;
import dev.marie.MariesLib.compat.AutoCompatDiscovery;
import dev.marie.MariesLib.compat.ModCompat;
import dev.maire.nourished.compat.lso.LSOCompat;
import dev.maire.nourished.compat.peakstamina.PeakStaminaCompat;
import dev.maire.nourished.compat.spiceoflifeonion.SpiceOfLifeOnionCompat;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.client.ClientEventRegistrar;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.NourishedTrackingData;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.handler.NourishedFoodTriggerHandler;
import dev.maire.nourished.core.handler.NourishedGuideJoinHandler;
import dev.maire.nourished.core.handler.NourishedServerHandler;
import dev.maire.nourished.core.handler.NourishedTagsHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthRecoveryHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthTickHandler;
import dev.maire.nourished.modules.RawFood.handler.RawFoodPenaltyHandler;
import dev.maire.nourished.core.network.ModNetworking;
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
        // Per-nutrient TOML sections are keyed off NutrientRegistry; load before building the spec.
        NutrientRegistry.load();
        NourishedConfig.register(modContainer);
        NourishedClientConfig.register(modContainer);
        modEventBus.addListener(NourishedConfig::onModConfigLoading);
        modEventBus.addListener(NourishedConfig::onModConfigReloading);
        modEventBus.addListener(NourishedClientConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigReloading);

        NourishedLifecycle.register();
        MariesLibBootstrap.attach(Nourished.MODID, modEventBus);
        NourishedContextBuilder.registerSlim();
        MarieDataManager.setCallbacks(new NourishedDatapackCallbacks());
        // NourishedContextBuilder.register(); // Phase 6: restore if slim bootstrap fails verification

        NourishedKubeIntegration.register();
        ModCompat.initialize();
        if (ModList.get().isLoaded("peakstamina")) {
            PeakStaminaCompat.register();
        }
        if (ModList.get().isLoaded("solonion")) {
            SpiceOfLifeOnionCompat.register();
        }
        if (ModList.get().isLoaded("legendarysurvivaloverhaul")) {
            LSOCompat.register();
        }
        FoodNutritionRegistry.init();
        TrackingData.setInstanceFactory(NourishedTrackingData::new);
        DietAttachment.register(modEventBus);
        GutHealthAttachment.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (minecraft, parent) -> NourishedConfigScreen.create(parent));
            ClientEventRegistrar.register(modEventBus);
        }
        modEventBus.addListener(ModNetworking::register);
        NourishedFoodTriggerHandler.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(NourishedTagsHandler::onTagsUpdated);
        NeoForge.EVENT_BUS.register(new NourishedServerHandler());
        NeoForge.EVENT_BUS.register(new RawFoodPenaltyHandler());
        NeoForge.EVENT_BUS.register(new GutHealthTickHandler());
        NeoForge.EVENT_BUS.register(new GutHealthRecoveryHandler());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(() -> {
                ModCompat.discoverUnknownMods();
                LOGGER.info("[Nourished] Starting AutoCompatDiscovery...");
                try (var scope = MarieAPIState.openForDatapackReload()) {
                    AutoCompatDiscovery.discover();
                } catch (Exception e) {
                    LOGGER.error("[Nourished] AutoCompatDiscovery failed.", e);
                }
                MarieApiRegistries.freezeModOnlyRegistriesAfterCommonSetup();
                MarieAPIState.close();
            });
        });
        NeoForge.EVENT_BUS.register(new NourishedGuideJoinHandler());
        TrackingAttachment.logAllValueNbtPaths();
        LOGGER.info("[Nourished] Calories NBT path: {}", TrackingAttachment.getTotalNbtPath());
        LOGGER.info("[Nourished] API v{} ready — {} nutrients, {} effects, {} compat entries registered.",
                MarieAPIVersion.VERSION,
                NutrientRegistry.getAll().size(),
                EffectRegistry.getAll().size(),
                ModCompat.getAllEntries().size());
        LOGGER.info("Nourished loaded.");
    }
}
