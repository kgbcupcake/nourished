package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.client.MarieClientCache;
import dev.marie.MariesLib.config.FeatureFlagCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.client.config.ExportConfigScreen;
import dev.maire.nourished.client.config.ImportConfigScreen;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.client.config.NourishedImportExport;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedPresetRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.NourishedKubeIntegration;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import net.minecraft.client.gui.screens.Screen;

@ApiStatus.Internal
public final class NourishedContextBuilder {

    private NourishedContextBuilder() {}

    public static void registerSlim() {
        MarieLibContext.register(MarieLibContext.builder(Nourished.MODID)
                .dataProvider(NourishedPlayerDataProvider.INSTANCE)
                .effectApplier((player, data) -> {
                    if (FeatureFlagCache.enableEffects()) {
                        NutritionEffectApplier.apply(player, data);
                    }
                })
                .effectClearer(NutritionEffectApplier::clearAll)
                .trackingDeltaSyncer(NourishedSyncHandler::syncDietDelta)
                .syncOnJoin(NourishedSyncHandler::syncOnJoin)
                .deathNutritionBehavior(() -> NourishedConfig.get().deathNutritionBehavior())
                .configScreenFactory(() -> NourishedConfigScreen.create(null))
                .exportScreenFactory(parent -> new ExportConfigScreen((Screen) parent, null))
                .importScreenFactory(parent -> new ImportConfigScreen((Screen) parent, null))
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
                .showJoinMessage(() -> NourishedConfig.get().showJoinMessage())
                .joinMessageLine1(NourishedJoinMessage::line1)
                .joinMessageLine2(NourishedJoinMessage::line2)
                .heavySourceBlocker(NourishedSourceRules::isHeavyBlocked)
                .sourceItemFilter(() -> NourishedItems::isNutritiousFood)
                .valueIconProvider(NutrientRegistry::getIcon)
                .sourceFamilyResolver(FoodFamilyResolver::resolve)
                .valueTagScoresProvider(FoodNutritionRegistry::getNutrientTagScores)
                .registrationDelegate(new NourishedRegistrationDelegate())
                .runtimeResolverStages(NourishedResolverStages.STAGES)
                .clientTrackingDataProvider(MarieClientCache::get)
                .clientMemoryConfigProvider(NourishedClientMemoryConfig::get)
                .trackingMemoryConfigProvider(NourishedMemoryConfig::serverTrackingMemoryConfig)
                .scannerConfidenceSpreadThreshold(
                        () -> (float) NourishedConfig.get().scannerConfidenceSpreadThreshold())
                .compositeRatioThreshold(NourishedConfig.get()::compositeRatioThreshold)
                .scannerEnableRecipeInheritance(NourishedConfig.get()::scannerEnableRecipeInheritance)
                .memoryWindowMinutes(() -> (long) NourishedConfig.get().memoryWindowMinutes())
                .memoryWindowCount(NourishedConfig.get()::memoryWindowCount)
                .streakWindowMs(() -> (long) NourishedConfig.get().streakWindowMs())
                .streakWeight(() -> (float) NourishedConfig.get().streakWeight())
                .debtThreshold(() -> (float) NourishedConfig.get().debtThreshold())
                .debtDecayRate(() -> (float) NourishedConfig.get().debtDecayRate())
                .diminishingSteepness(() -> (float) NourishedConfig.get().diminishingSteepness())
                .diminishingMidpoint(() -> (float) NourishedConfig.get().diminishingMidpoint())
                .debugMemoryLogging(NourishedConfig.get()::debugMemoryLogging)
                .excessThreshold(() -> (float) NourishedConfig.get().excessThreshold())
                .lowThreshold(() -> (float) NourishedConfig.get().lowThreshold())
                .criticalThreshold(() -> (float) NourishedConfig.get().criticalThreshold())
                .decayIntervalTicks(NourishedConfig.get()::decayIntervalTicks)
                .multiValueInheritanceThreshold(
                        () -> NourishedConfig.get().multiNutrientInheritanceThreshold())
                .tooltipValueResolver((stack, player) -> player != null && player.level() != null
                        ? NutrientClassificationLookup.resolveNutrientBars(stack, false, player.level())
                        : NutrientClassificationLookup.resolveNutrientBars(stack, false))
                .sourceValueResolver((stack, level) ->
                        NutrientClassificationLookup.resolveNutrientBars(stack, false, level))
                .sourceDeltaResolver((stack, level, payload, bars) -> {
                    FoodNutritionRegistry.DietDelta d = FoodNutritionRegistry.computeDietDelta(
                            stack, level, (int) payload, 0f, bars);
                    return new MarieLibContext.SourceDelta(d.calories(), d.nutrients());
                })
                .onReloadBroadcast(NourishedReloadHelper::reloadAndBroadcast)
                .postValueModifierHook(NourishedKubeIntegration::fireNutrientModifier)
                .onCacheInvalidated(() -> {
                    FoodFamilyResolver.clearCache();
                    RuntimeFoodResolver.getInstance().invalidateCache();
                    RawFoodClassifier.invalidate();
                })
                .build());
    }
}
