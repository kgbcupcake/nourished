package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.MarieContext;
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
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import dev.marie.framework.util.MarieRegistryUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

@ApiStatus.Internal
public final class NourishedContextBuilder {

    private NourishedContextBuilder() {}

    public static void registerSlim() {
        MarieContext.register(MarieContext.builder(Nourished.MODID)
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
                .decayRateFor(key -> (float) NourishedConfig.get().resolvedDecayRateFor(key))
                .multiValueInheritanceThreshold(
                        () -> NourishedConfig.get().multiNutrientInheritanceThreshold())
                .tooltipValueResolver((stack, player) -> player != null && player.level() != null
                        ? NutrientClassificationLookup.resolveNutrientBars(stack, false, player.level())
                        : NutrientClassificationLookup.resolveNutrientBars(stack, false))
                .sourceValueResolver((stack, level) ->
                        NutrientClassificationLookup.resolveNutrientBars(stack, false, level))
                .sourceDeltaResolver((stack, level, payload, bars) -> {
                    ResourceLocation itemId = MarieRegistryUtils.itemKey(stack.getItem());
                    if (itemId != null) {
                        Optional<FoodOverrideRegistry.FoodOverride> override =
                                FoodOverrideRegistry.getOverride(itemId.toString());
                        if (override.isPresent()) {
                            return new MarieContext.SourceDelta(override.get().calories(), Map.copyOf(bars));
                        }
                    }
                    float realSaturation = resolveRealSaturation(stack);
                    FoodNutritionRegistry.DietDelta d = FoodNutritionRegistry.computeDietDelta(
                            stack, level, (int) payload, realSaturation, bars);
                    return new MarieContext.SourceDelta(d.calories(), d.nutrients());
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

    /**
     * Resolves real food saturation for the diet delta burst formula.
     * Previously hardcoded to 0f at this call site, which silently zeroed out
     * the saturation term in FoodNutritionRegistry.computeDietDelta's burst
     * calculation for every food ever eaten. FoodProperties is resolved with a
     * null entity (client-preview-safe per its own contract) since no
     * LivingEntity is available in this resolver's signature.
     */
    private static float resolveRealSaturation(ItemStack stack) {
        FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(stack, null);
        return food != null ? food.saturation() : 0f;
    }
}
