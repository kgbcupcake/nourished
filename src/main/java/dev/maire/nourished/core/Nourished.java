package dev.maire.nourished.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedAPIVersion;
import dev.maire.nourished.api.NourishedAPIState;
import dev.maire.nourished.core.registry.NourishedApiDefinitionRegistries;
import dev.maire.nourished.core.registry.NourishedAttributes;
import dev.maire.nourished.core.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.command.NourishedCommand;
import dev.maire.nourished.compat.kubejs.NourishedKubeJSPlugin;
import dev.maire.nourished.compat.AutoCompatDiscovery;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.compat.lso.LSOCompat;
import dev.maire.nourished.compat.peakstamina.PeakStaminaCompat;
import dev.maire.nourished.compat.spiceoflifeonion.SpiceOfLifeOnionCompat;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.ModCompatRegistry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.color.ColorRegistry;
import dev.maire.nourished.client.ClientEventRegistrar;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.handler.ConfigReloadHandler;
import dev.maire.nourished.core.handler.DietPlayerEvents;
import dev.maire.nourished.core.handler.FoodEatenHandler;
import dev.maire.nourished.core.handler.NourishedGuideJoinHandler;
import dev.maire.nourished.core.handler.NutritionDecayHandler;
import dev.maire.nourished.core.handler.NutritionEatingHandler;
import dev.maire.nourished.core.handler.NutritionRecipeServerHandler;
import dev.maire.nourished.core.handler.NutritionEffectsHandler;
import dev.maire.nourished.core.handler.SleepBonusHandler;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthRecoveryHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthTickHandler;
import dev.maire.nourished.modules.RawFood.handler.RawFoodPenaltyHandler;
import dev.maire.nourished.modules.Stamina.Core.StaminaAttachment;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;
import dev.maire.nourished.modules.Stamina.Handler.StaminaCombatHandler;
import dev.maire.nourished.modules.Stamina.Handler.StaminaFoodHandler;
import dev.maire.nourished.modules.Stamina.Handler.StaminaMovementHandler;
import dev.maire.nourished.modules.Stamina.Handler.StaminaTickHandler;
import dev.maire.nourished.modules.Stamina.Handler.StaminaWorldHandler;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
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
        registerLifecycleEntries();
        RegistryLifecycleManager.loadAll();
        NourishedAttributes.register(modEventBus);
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
        NourishedConfig.register(modContainer);
        NourishedClientConfig.register(modContainer);
        modEventBus.addListener(NourishedConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigReloading);
        FoodNutritionRegistry.init();
        DietAttachment.register(modEventBus);
        GutHealthAttachment.register(modEventBus);
        StaminaAttachment.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> NourishedConfigScreen.create(parent));
            ClientEventRegistrar.register(modEventBus);
        }
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.register(new NutritionRecipeServerHandler());
        NeoForge.EVENT_BUS.register(new NutritionEatingHandler());
        NeoForge.EVENT_BUS.register(new FoodEatenHandler());
        NeoForge.EVENT_BUS.register(new RawFoodPenaltyHandler());
        NeoForge.EVENT_BUS.register(new GutHealthTickHandler());
        NeoForge.EVENT_BUS.register(new GutHealthRecoveryHandler());
        NeoForge.EVENT_BUS.register(new StaminaMovementHandler());
        NeoForge.EVENT_BUS.register(new StaminaCombatHandler());
        NeoForge.EVENT_BUS.register(new StaminaWorldHandler());
        NeoForge.EVENT_BUS.register(new StaminaFoodHandler());
        NeoForge.EVENT_BUS.register(new StaminaTickHandler());
        NeoForge.EVENT_BUS.register(new NutritionEffectsHandler());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(() -> {
                ModCompat.discoverUnknownMods();
                if (!ModCompat.shouldDisableDecay()) {
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
        NeoForge.EVENT_BUS.register(new NourishedGuideJoinHandler());
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

    /**
     * Registers all config-backed registries with {@link RegistryLifecycleManager} in dependency
     * order. Called exactly once during mod construction before {@link RegistryLifecycleManager#loadAll()}.
     *
     * <p>Order rationale: {@code NutrientRegistry} provides keys consumed by every other registry,
     * so it loads first. Color/Effect/FoodValue/FoodOverride/ScannerSpec are independent JSON loads.
     * Lock/ModCompat/PresetRegistry are appended at the end; {@code ModCompatRegistry} has no reload
     * hook (no-op preserves prior behavior since it was absent from the legacy reload pipeline).</p>
     */
    private static void registerLifecycleEntries() {
        RegistryLifecycleManager.registerRegistry(
                "NutrientRegistry", NutrientRegistry::load, NutrientRegistry::reload);
        RegistryLifecycleManager.registerRegistry(
                "ColorRegistry", ColorRegistry::load, ColorRegistry::reload, ColorRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "EffectRegistry", EffectRegistry::load, EffectRegistry::reload, EffectRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "RawFoodConfig", RawFoodConfig::load, RawFoodConfig::reload, RawFoodConfig::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "StaminaConfig", StaminaConfig::load, StaminaConfig::reload, StaminaConfig::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodValueRegistry", FoodValueRegistry::load, FoodValueRegistry::reload, FoodValueRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "FoodOverrideRegistry", FoodOverrideRegistry::load, FoodOverrideRegistry::reload, FoodOverrideRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ScannerSpecRegistry", ScannerSpecRegistry::load, ScannerSpecRegistry::reload, ScannerSpecRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "LockRegistry", LockRegistry::load, LockRegistry::reload, LockRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry(
                "ModCompatRegistry", ModCompatRegistry::load, () -> {});
        RegistryLifecycleManager.registerRegistry(
                "PresetRegistry", PresetRegistry::ensureBuiltInFilesOnDisk, PresetRegistry::reload);
    }
}
