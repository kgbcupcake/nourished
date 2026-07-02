package dev.maire.nourished.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieAPI;
import dev.marie.framework.api.MarieAPIVersion;
import dev.marie.framework.api.MarieAPIState;
import dev.marie.framework.data.MarieDataManager;
import dev.maire.nourished.core.datapack.NourishedDatapackCallbacks;
import dev.marie.framework.registry.MarieApiRegistries;
import dev.marie.framework.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.context.NourishedContextBuilder;
import dev.maire.nourished.core.lifecycle.NourishedLifecycle;
import dev.marie.framework.core.MarieBootstrap;
import dev.marie.framework.compat.AutoCompatDiscovery;
import dev.marie.framework.compat.ModCompat;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.client.ClientEventRegistrar;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.NourishedTrackingData;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientExportResolver;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.command.NourishedCommand;
import dev.maire.nourished.config.validation.ColorsValidator;
import dev.maire.nourished.config.validation.EffectsValidator;
import dev.maire.nourished.config.validation.FoodOverridesValidator;
import dev.maire.nourished.config.validation.FoodValuesValidator;
import dev.maire.nourished.config.validation.LocksValidator;
import dev.maire.nourished.config.validation.NourishedConfigValidation;
import dev.maire.nourished.config.validation.NutrientsValidator;
import dev.maire.nourished.config.validation.RawFoodValidator;
import dev.maire.nourished.config.validation.ScannerSpecValidator;
import dev.maire.nourished.config.validation.SourceClassificationsValidator;
import dev.maire.nourished.core.handler.NourishedFoodTriggerHandler;
import dev.maire.nourished.core.tagaudit.NourishedTagAuditContext;
import dev.maire.nourished.core.tagaudit.rules.NamespaceBiasRule;
import dev.maire.nourished.core.tagaudit.rules.TagInferenceMismatchRule;
import dev.maire.nourished.core.handler.NourishedGuideJoinHandler;
import dev.maire.nourished.core.handler.NourishedServerHandler;
import dev.maire.nourished.core.handler.NourishedTagsHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthRecoveryHandler;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthTickHandler;
import dev.maire.nourished.modules.RawFood.handler.RawFoodPenaltyHandler;
import dev.maire.nourished.core.network.ModNetworking;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
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
        NutrientRegistry.loadDefinitions();
        NutrientRegistry.syncToValueRegistryUnfrozen();
        // ModCompat entries must exist before NourishedConfig builds compatCodeToggles/compatTagToggles.
        MarieBootstrap.attach(Nourished.MODID, modEventBus);
        ModCompat.initialize();
        NourishedConfig.register(modContainer);
        NourishedClientConfig.register(modContainer);
        modEventBus.addListener(NourishedConfig::onModConfigLoading);
        modEventBus.addListener(NourishedConfig::onModConfigReloading);
        modEventBus.addListener(NourishedClientConfig::onModConfigLoading);
        modEventBus.addListener(NourishedClientConfig::onModConfigReloading);

        NourishedLifecycle.register();
        NourishedContextBuilder.registerSlim();
        MarieAPI.registerExportResolver(
                "nourished_nutrients",
                Registries.ITEM,
                new NutrientExportResolver()
        );
        MarieAPI.registerConfigValidator(new NutrientsValidator());
        MarieAPI.registerConfigValidator(new ColorsValidator());
        MarieAPI.registerConfigValidator(new FoodOverridesValidator());
        MarieAPI.registerConfigValidator(new ScannerSpecValidator());
        MarieAPI.registerConfigValidator(new SourceClassificationsValidator());
        MarieAPI.registerConfigValidator(new EffectsValidator());
        MarieAPI.registerConfigValidator(new FoodValuesValidator());
        MarieAPI.registerConfigValidator(new LocksValidator());
        MarieAPI.registerConfigValidator(new RawFoodValidator());
        MarieAPI.registerTagRule(new TagInferenceMismatchRule());
        MarieAPI.registerTagRule(new NamespaceBiasRule());
        MarieAPI.registerTagAuditContext(Nourished.MODID, NourishedTagAuditContext.get());

        NeoForge.EVENT_BUS.register(new NourishedCommand());

        MarieDataManager.setCallbacks(new NourishedDatapackCallbacks());
        NeoForge.EVENT_BUS.addListener(MarieDataManager::registerReloadListener);

        NourishedKubeIntegration.register();
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
                // Runs after MarieBootstrap.onCommonSetup → RegistryLifecycleManager.loadAll().
                NourishedConfigValidation.runAfterInitialLoad();
                NutrientRegistry.syncAndFreeze();
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
