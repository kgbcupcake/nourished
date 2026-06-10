package dev.maire.nourished.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.registry.MarieApiRegistries;
import dev.marie.MariesLib.registry.MarieAttributes;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.marie.MariesLib.command.MarieCommand;
import dev.maire.nourished.core.context.NourishedContextBuilder;
import dev.maire.nourished.core.lifecycle.NourishedLifecycle;
import dev.marie.MariesLib.compat.kubejs.MarieKubeJSPlugin;
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
import dev.marie.MariesLib.handler.RecipeServerHandler;
import dev.marie.MariesLib.handler.ReloadHandler;
import dev.marie.MariesLib.handler.SleepBonusHandler;
import dev.marie.MariesLib.handler.SourceAppliedHandler;
import dev.marie.MariesLib.handler.SourceEatingHandler;
import dev.marie.MariesLib.handler.TrackingPlayerEvents;
import dev.marie.MariesLib.handler.ValueDecayHandler;
import dev.marie.MariesLib.handler.ValueEffectsHandler;
import dev.maire.nourished.core.handler.NourishedGuideJoinHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthRecoveryHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthTickHandler;
import dev.maire.nourished.modules.RawFood.handler.RawFoodPenaltyHandler;
// import dev.maire.nourished.modules.Stamina.Core.StaminaAttachment; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaCombatHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaFoodHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaMovementHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaTickHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaWorldHandler; // STAMINA_SHELVED
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
        NourishedConfig.register(modContainer);
        NourishedClientConfig.register(modContainer);
        modEventBus.addListener(NourishedConfig::onModConfigLoading);
        modEventBus.addListener(NourishedConfig::onModConfigReloading);
        modEventBus.addListener(NourishedClientConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigReloading);

        NourishedContextBuilder.register();
        if (ModList.get().isLoaded("kubejs")) {
            modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLConstructModEvent.class, event ->
                    MarieKubeJSPlugin.bootstrap());
        }
        NourishedLifecycle.register();
        RegistryLifecycleManager.loadAll();
        MarieAttributes.register(modEventBus);
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
        TrackingAttachment.register(modEventBus);
        DietAttachment.register(modEventBus);
        GutHealthAttachment.register(modEventBus);
        // StaminaAttachment.register(modEventBus); // STAMINA_SHELVED
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> NourishedConfigScreen.create(parent));
            ClientEventRegistrar.register(modEventBus);
        }
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.register(new RecipeServerHandler());
        NeoForge.EVENT_BUS.register(new SourceEatingHandler());
        NeoForge.EVENT_BUS.register(new SourceAppliedHandler());
        NeoForge.EVENT_BUS.register(new RawFoodPenaltyHandler());
        NeoForge.EVENT_BUS.register(new GutHealthTickHandler());
        NeoForge.EVENT_BUS.register(new GutHealthRecoveryHandler());
        // NeoForge.EVENT_BUS.register(new StaminaMovementHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaCombatHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaWorldHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaFoodHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaTickHandler()); // STAMINA_SHELVED
        NeoForge.EVENT_BUS.register(new ValueEffectsHandler());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(() -> {
                ModCompat.discoverUnknownMods();
                if (!ModCompat.shouldDisableDecay()) {
                    NeoForge.EVENT_BUS.register(new ValueDecayHandler());
                }
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
        NeoForge.EVENT_BUS.register(new TrackingPlayerEvents());
        NeoForge.EVENT_BUS.register(new SleepBonusHandler());
        NeoForge.EVENT_BUS.register(new ReloadHandler());
        NeoForge.EVENT_BUS.register(new MarieCommand());
        if (ModList.get().isLoaded("kubejs")) {
            try {
                MarieKubeJSPlugin.bootstrap();
                if (MarieKubeJSPlugin.isRegistered()) {
                    LOGGER.info("[Nourished] Enabled KubeJS integration bridge.");
                } else {
                    LOGGER.warn("[Nourished] KubeJS is loaded but MarieKubeJSPlugin was not registered.");
                }
            } catch (Throwable t) {
                LOGGER.warn("[Nourished] Failed to initialize KubeJS integration bridge.", t);
            }
        }
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
