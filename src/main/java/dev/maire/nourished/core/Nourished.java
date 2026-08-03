package dev.maire.nourished.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.api.marieapi.MarieAPIVersion;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.color.ColorDefinition;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.ColorRegistry;
import dev.marie.framework.color.MarieColors;
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
import dev.maire.nourished.client.hud.caloriehistory.CalorieHudScreen;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityLogHudPanel;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientRegistry;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityModuleDispatcher;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityModuleRegistry;
import dev.maire.nourished.modules.activity_driven_nutrient.modules.CombatModule;
import dev.maire.nourished.modules.activity_driven_nutrient.modules.MiningModule;
import dev.maire.nourished.modules.activity_driven_nutrient.modules.SprintDecayModule;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.StarvationModule;
import dev.maire.nourished.modules.activity_driven_nutrient.modules.SwimDecayModule;
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
        ActivityDrivenNutrientRegistry.registerSync();
        registerColorDefinitions();

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
        ActivityModuleRegistry.register(new SprintDecayModule());
        ActivityModuleRegistry.register(new SwimDecayModule());
        ActivityModuleRegistry.register(new MiningModule());
        ActivityModuleRegistry.register(new CombatModule());
        NeoForge.EVENT_BUS.register(new ActivityModuleDispatcher());
        NeoForge.EVENT_BUS.register(new StarvationModule());
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(() -> {
                // Runs after MarieBootstrap.onCommonSetup → RegistryLifecycleManager.loadAll(), so
                // ColorRegistry and ActivityDrivenNutrientRegistry have both loaded from disk here.
                migrateNutrientColorKeys();
                ActivityDrivenNutrientRegistry.migrateLegacyColorsToColorRegistry();
                NourishedConfigValidation.runAfterInitialLoad();
                NutrientRegistry.syncAndFreeze();
                ModCompat.discoverUnknownMods();
                if (NourishedConfig.get().enableCalorieHistory()) {
                    MarieAPI.registerTracker(dev.marie.framework.tracking.tracker.definition.TrackerDefinition.daily(
                            dev.maire.nourished.api.NourishedAPI.CALORIES_TRACKER_ID,
                            NourishedConfig.get().calorieHistoryRetentionDays()));
                }
                LOGGER.info("[Nourished] Starting AutoCompatDiscovery...");
                try (var scope = MarieAPIState.openForDatapackReload()) {
                    AutoCompatDiscovery.discover();
                } catch (Exception e) {
                    LOGGER.error("[Nourished] AutoCompatDiscovery failed.", e);
                }
                MarieApiRegistries.freezeValueTrackingOnlyRegistriesAfterCommonSetup();
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

    /**
     * Registers this mod's {@link ColorDefinition}s with MarieLib's color system: one per nutrient
     * (namespaced {@code nutrient.<key>}), one per activity module (via {@link
     * ActivityDrivenNutrientRegistry#registerColors()}), and one per HUD panel that composes its
     * background from a resolved color plus its own opacity config value.
     */
    private static void registerColorDefinitions() {
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            MarieColors.registerColor(ColorDefinition.of(
                    ColorKey.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "nutrient." + def.key())),
                    def.color()));
        }
        ActivityDrivenNutrientRegistry.registerColors();
        // Matches ThemeKey.PANEL_BACKGROUND's RGB (0x101010) — the flat color both panels drew
        // before this feature existed; alpha here is nominal since draw time composes the real
        // alpha from each panel's own opacity config value.
        int defaultPanelArgb = 0xFF101010;
        // Matches ThemeKey.TEXT_PRIMARY (0xFFE0E0E0) — the flat text color both panels drew before
        // this feature existed.
        int defaultTextArgb = 0xFFE0E0E0;
        CalorieHudScreen.COLORS = MarieColors.registerColorPair(MODID, "calorieHud", defaultPanelArgb, defaultTextArgb);
        ActivityLogHudPanel.COLORS = MarieColors.registerColorPair(MODID, "activityLogHud", defaultPanelArgb, defaultTextArgb);
    }

    /**
     * One-time migration of nutrient color overrides from their old {@link ColorRegistry} key (the
     * raw nutrient key, e.g. {@code "protein"}) to the new namespaced {@link ColorKey} string (e.g.
     * {@code "nourished:nutrient.protein"}) that {@code ColorHexRowWidget}/{@code MarieAPI.resolveColor}
     * now read/write. Safe to call on every load: skips any nutrient whose new key already has an
     * override, and never deletes the old entry, so this can't lose data even if re-run.
     */
    private static void migrateNutrientColorKeys() {
        boolean migratedAny = false;
        for (String key : NutrientRegistry.getKeys()) {
            String newKey = ColorKey.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "nutrient." + key))
                    .id().toString();
            if (ColorRegistry.getArgb(newKey).isPresent()) {
                continue;
            }
            java.util.Optional<Integer> legacy = ColorRegistry.getArgb(key);
            if (legacy.isEmpty()) {
                continue;
            }
            ColorRegistry.setArgb(newKey, legacy.get());
            migratedAny = true;
            LOGGER.info("[Nourished] Migrated custom nutrient color '{}' ({}) to new key '{}'",
                    key, String.format("0x%08X", legacy.get()), newKey);
        }
        if (migratedAny) {
            ColorRegistry.save();
        }
    }
}
