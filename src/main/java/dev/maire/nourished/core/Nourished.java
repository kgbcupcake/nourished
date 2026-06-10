package dev.maire.nourished.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.registry.MarieApiRegistries;
import dev.marie.MariesLib.registry.MarieAttributes;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.maire.nourished.core.command.NourishedCommand;
import dev.marie.MariesLib.compat.kubejs.MarieKubeJSPlugin;
import dev.marie.MariesLib.compat.AutoCompatDiscovery;
import dev.marie.MariesLib.compat.ModCompat;
import dev.maire.nourished.compat.lso.LSOCompat;
import dev.maire.nourished.compat.peakstamina.PeakStaminaCompat;
import dev.maire.nourished.compat.spiceoflifeonion.SpiceOfLifeOnionCompat;
import dev.marie.MariesLib.config.LockRegistry;
import dev.marie.MariesLib.config.ModCompatRegistry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedPresetRegistry;
import dev.maire.nourished.client.config.ExportConfigScreen;
import dev.maire.nourished.client.config.ImportConfigScreen;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.client.config.NourishedImportExport;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.marie.MariesLib.client.MarieClientCache;
import dev.marie.MariesLib.client.MarieClientState;
import dev.marie.MariesLib.client.MarieValueColors;
import dev.marie.MariesLib.color.ColorRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.api.ThresholdEffect;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.ValueModifierEvent;
import dev.marie.MariesLib.api.MemoryView;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MarieLibPlayerDataProvider;
import dev.marie.MariesLib.core.MarieLibRegistrationDelegate;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;
import dev.maire.nourished.client.ClientEventRegistrar;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.NourishedTrackingData;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
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
// import dev.maire.nourished.modules.Stamina.Core.StaminaAttachment; // STAMINA_SHELVED
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;
// import dev.maire.nourished.modules.Stamina.Handler.StaminaCombatHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaFoodHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaMovementHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaTickHandler; // STAMINA_SHELVED
// import dev.maire.nourished.modules.Stamina.Handler.StaminaWorldHandler; // STAMINA_SHELVED
import dev.maire.nourished.core.network.ModNetworking;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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

        MarieLibContext.register(MarieLibContext.builder(MODID)
                .valueKeys(NutrientRegistry::getKeys)
                .scannerConfidenceSpreadThreshold(() -> (float) NourishedConfig.get().scannerConfidenceSpreadThreshold())
                .compositeRatioThreshold(NourishedConfig.get()::compositeRatioThreshold)
                .scannerEnableRecipeInheritance(NourishedConfig.get()::scannerEnableRecipeInheritance)
                .enableDebugLogging(() -> ModuleCache.enableDebugLogging)
                .scannerApplyCallback(FoodNutritionRegistry::applyFromScanner)
                .valueTagChecker(() -> stack -> !FoodNutritionRegistry.getNutrientTagScores(stack.getItem()).isEmpty())
                .memoryWindowMinutes(() -> (long) NourishedConfig.get().memoryWindowMinutes())
                .memoryWindowCount(NourishedConfig.get()::memoryWindowCount)
                .streakWindowMs(() -> (long) NourishedConfig.get().streakWindowMs())
                .streakWeight(() -> (float) NourishedConfig.get().streakWeight())
                .debtThreshold(() -> (float) NourishedConfig.get().debtThreshold())
                .debtDecayRate(() -> (float) NourishedConfig.get().debtDecayRate())
                .diminishingSteepness(() -> (float) NourishedConfig.get().diminishingSteepness())
                .diminishingMidpoint(() -> (float) NourishedConfig.get().diminishingMidpoint())
                .debugMemoryLogging(NourishedConfig.get()::debugMemoryLogging)
                .isValueBeneficial(() -> NutrientRegistry::isBeneficial)
                .excessThreshold(() -> (float) NourishedConfig.get().excessThreshold())
                .lowThreshold(() -> (float) NourishedConfig.get().lowThreshold())
                .criticalThreshold(() -> (float) NourishedConfig.get().criticalThreshold())
                .tooltipValueResolver((stack, player) -> player != null && player.level() != null
                        ? FoodNutritionRegistry.resolveNutrientBars(stack, false, player.level())
                        : FoodNutritionRegistry.resolveNutrientBars(stack, false))
                .clientTrackingDataProvider(MarieClientCache::get)
                .valueColorProvider(MarieValueColors::baseColorArgb)
                .valueIconProvider(NutrientRegistry::getIcon)
                .sourceFamilyResolver(FoodFamilyResolver::resolve)
                .configScreenFactory(() -> NourishedConfigScreen.create(null))
                .exportScreenFactory(parent -> new ExportConfigScreen(parent, null))
                .importScreenFactory(parent -> new ImportConfigScreen(parent, null))
                .configExporter(NourishedImportExport::exportCurrentConfig)
                .configImporter(json -> {
                    try {
                        NourishedImportExport.applyImport(json);
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .currentConfigPresetValues(NourishedImportExport::presetValuesFromCurrentConfig)
                .ensureBuiltInPresetsOnDisk(NourishedPresetRegistry::ensureBuiltInFilesOnDisk)
                .applyPresetValues(NourishedPresetRegistry::applyPresetValues)
                .enableAllEffectsForPresets(NourishedPresetRegistry::enableAllEffects)
                .clientMemoryConfigProvider(Nourished::clientMemoryConfig)
                .playerDataProvider(new PlayerDataProvider())
                .registrationDelegate(new RegistrationDelegate())
                .build());
        if (ModList.get().isLoaded("kubejs")) {
            modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLConstructModEvent.class, event ->
                    MarieKubeJSPlugin.bootstrap());
        }
        registerLifecycleEntries();
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
        NeoForge.EVENT_BUS.register(new NutritionRecipeServerHandler());
        NeoForge.EVENT_BUS.register(new NutritionEatingHandler());
        NeoForge.EVENT_BUS.register(new FoodEatenHandler());
        NeoForge.EVENT_BUS.register(new RawFoodPenaltyHandler());
        NeoForge.EVENT_BUS.register(new GutHealthTickHandler());
        NeoForge.EVENT_BUS.register(new GutHealthRecoveryHandler());
        // NeoForge.EVENT_BUS.register(new StaminaMovementHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaCombatHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaWorldHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaFoodHandler()); // STAMINA_SHELVED
        // NeoForge.EVENT_BUS.register(new StaminaTickHandler()); // STAMINA_SHELVED
        NeoForge.EVENT_BUS.register(new NutritionEffectsHandler());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(() -> {
                ModCompat.discoverUnknownMods();
                if (!ModCompat.shouldDisableDecay()) {
                    NeoForge.EVENT_BUS.register(new NutritionDecayHandler());
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
        NeoForge.EVENT_BUS.register(new DietPlayerEvents());
        NeoForge.EVENT_BUS.register(new SleepBonusHandler());
        NeoForge.EVENT_BUS.register(new ConfigReloadHandler());
        NeoForge.EVENT_BUS.register(new NourishedCommand());
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

    private static final java.util.concurrent.atomic.AtomicBoolean CLIENT_MEMORY_WARN_ONCE = new java.util.concurrent.atomic.AtomicBoolean(false);

    public static void resetClientMemoryDiagnostics() {
        CLIENT_MEMORY_WARN_ONCE.set(false);
    }

    static TrackingMemoryConfig clientMemoryConfig() {
        Object raw = MarieClientState.getConfig();
        if (raw instanceof SyncNourishedConfigSnapshot snap) {
            return new TrackingMemoryConfig(
                    snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                    snap.diminishingFloor(), snap.startingNutrientValue());
        }
        if (CLIENT_MEMORY_WARN_ONCE.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[Nourished] MarieClientCache: config snapshot null, falling back to raw config. Will not warn again until disconnect.");
        }
        NourishedConfig cfg = NourishedConfig.get();
        return new TrackingMemoryConfig(
                cfg.memoryWindowMinutes(), cfg.noveltyBonus(), cfg.noveltyDecayCap(),
                cfg.diminishingFloor(), cfg.startingNutrientValue());
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

    private static final class PlayerDataProvider implements MarieLibPlayerDataProvider {

        private static final ResourceLocation API_MODIFIER_SOURCE =
                ResourceLocation.fromNamespaceAndPath(MODID, "api");

        @Override
        public float getTotal(Player player) {
            return TrackingAttachment.getTotal(player);
        }

        @Override
        public float getValueLevel(Player player, String valueKey) {
            return TrackingAttachment.getValueLevel(player, valueKey);
        }

        @Override
        public MemoryView getSourceMemoryView(Player player) {
            return TrackingAttachment.getSourceMemoryView(player);
        }

        @Override
        public void modifyValue(Player player, String valueKey, float delta) {
            ValueModifierEvent modifierEvent = new ValueModifierEvent(player, API_MODIFIER_SOURCE, valueKey, delta);
            NeoForge.EVENT_BUS.post(modifierEvent);
            if (modifierEvent.isCanceled()) {
                return;
            }
            TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
            data.addValue(valueKey, modifierEvent.getAmount());
            player.setData(TrackingAttachment.TRACKING.get(), data);
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            ModNetworking.syncDietDelta(serverPlayer, data);
            if (ModuleCache.enableEffects) {
                NutritionEffectApplier.apply(serverPlayer, data);
            }
        }
    }

    private static final class RegistrationDelegate implements MarieLibRegistrationDelegate {

        @Override
        public List<String> getValueKeys() {
            return NutrientRegistry.getKeys();
        }

        @Override
        public void registerValue(ValueDefinition definition) {
            NutrientRegistry.registerExternal(definition);
        }

        @Override
        public void registerEffect(ThresholdEffect definition) {
            EffectRegistry.registerExternal(definition);
        }

        @Override
        public void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount) {
            FoodNutritionRegistry.registerClassification(sourceId, valueKey, amount);
        }
    }
}
