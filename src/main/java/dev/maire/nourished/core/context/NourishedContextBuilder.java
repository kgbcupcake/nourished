package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.client.MarieClientCache;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.client.config.ExportConfigScreen;
import dev.maire.nourished.client.config.ImportConfigScreen;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.client.config.NourishedImportExport;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedPresetRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

@ApiStatus.Internal
public final class NourishedContextBuilder {

    private NourishedContextBuilder() {}

    public static void registerSlim() {
        MarieLibContext.register(MarieLibContext.builder(Nourished.MODID)
                .dataProvider(NourishedPlayerDataProvider.INSTANCE)
                .effectApplier((player, data) -> {
                    if (ModuleCache.enableEffects) {
                        NutritionEffectApplier.apply(player, data);
                    }
                })
                .effectClearer(NutritionEffectApplier::clearAll)
                .trackingDeltaSyncer(NourishedSyncHandler::syncDietDelta)
                .syncOnJoin(NourishedSyncHandler::syncOnJoin)
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
                .showJoinMessage(() -> NourishedConfig.get().showJoinMessage())
                .joinMessageLine1(NourishedJoinMessage::line1)
                .joinMessageLine2(NourishedJoinMessage::line2)
                .heavySourceBlocker(NourishedSourceRules::isHeavyBlocked)
                .sourceItemFilter(() -> NourishedItems::isNutritiousFood)
                .valueIconProvider(NutrientRegistry::getIcon)
                .sourceFamilyResolver(FoodFamilyResolver::resolve)
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
                .build());
    }

    /**
     * Legacy bootstrap — block-commented until Phase 6 gameplay verification passes.
     * Restore by uncommenting and switching {@link Nourished} back to {@code register()}.
     */
    /*
    public static void register() {
        CommunityTagStage communityTagStage = new CommunityTagStage();
        KeywordSuffixStage keywordSuffixStage = new KeywordSuffixStage();
        RecipeInheritanceStage recipeInheritanceStage =
                new RecipeInheritanceStage(RuntimeResolver.getInstance().recipeCache());
        NamespacePeerStage namespacePeerStage = new NamespacePeerStage();
        HardFallbackStage hardFallbackStage = new HardFallbackStage();

        MarieLibContext.register(MarieLibContext.builder(Nourished.MODID)
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
                .isSourceResolvable(NourishedContextBuilder::isNutritiousFood)
                .sourceItemFilter(() -> NourishedContextBuilder::isNutritiousFood)
                .valueTagScoresProvider(item -> FoodNutritionRegistry.getNutrientTagScores(item))
                .multiValueInheritanceThreshold(() -> NourishedConfig.get().multiNutrientInheritanceThreshold())
                .runtimeResolverStages(new ResolutionStageHandler[]{
                        communityTagStage, keywordSuffixStage, recipeInheritanceStage,
                        namespacePeerStage, hardFallbackStage
                })
                .stemmerIrregularForms(NourishedStemmerData.irregularForms())
                .stemmerCompoundSplits(NourishedStemmerData.compoundSplits())
                .stemmerStopWords(NourishedStemmerData.stopWords())
                .stemmerDictionary(NourishedStemmerData.dictionary())
                .onServerStarting(() -> {
                    NourishedSyncHandler.setConfigSnapshot(
                            SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get()));
                    NourishedSyncHandler.logServerStartupInfo();
                })
                .onReloadBroadcast(NourishedReloadHelper::reloadAndBroadcast)
                .onRecipeManagerBound(rm -> FoodNutritionRegistry.bindServerRecipeManager(rm))
                .onRecipeManagerCleared(() -> FoodNutritionRegistry.bindServerRecipeManager(null))
                .onCacheInvalidated(() -> {
                    FoodFamilyResolver.clearCache();
                    RuntimeFoodResolver.getInstance().invalidateCache();
                    FoodNutritionRegistry.clearPerReloadWarnings();
                    FoodNutritionRegistry.clearScannerClassifications();
                })
                .trackingMemoryConfigProvider(() -> {
                    SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
                    if (snap == null) return null;
                    return new TrackingMemoryConfig(
                            snap.memoryWindowMinutes(), snap.noveltyBonus(), snap.noveltyDecayCap(),
                            snap.diminishingFloor(), snap.startingNutrientValue());
                })
                .sourceValueResolver((stack, level) ->
                        FoodNutritionRegistry.resolveNutrientBars(stack, false, level))
                .sourceDeltaResolver((stack, level, payload, bars) -> {
                    FoodNutritionRegistry.DietDelta d =
                            FoodNutritionRegistry.computeDietDelta(
                                    stack, level, (int) payload, 0f, bars);
                    return new MarieLibContext.SourceDelta(d.calories(), d.nutrients());
                })
                .heavySourceBlocker((player, trigger) -> {
                    if (trigger.type() != ValueSourceTrigger.TriggerType.ITEM_CONSUMED)
                        return false;
                    return (int) trigger.payload() >=
                            NourishedModuleCache.heavySourcePropertyThreshold;
                })
                .lightSourceBlocker((player, trigger) -> false)
                .sourceOverrideLookup(itemId -> {
                    var ov = FoodOverrideRegistry.getOverride(itemId);
                    return ov.map(o -> new SourceOverrideRegistry.SourceOverride(
                            o.item(), o.nutrients(), o.calories(), o.enabled())).orElse(null);
                })
                .externalClassificationProvider(id ->
                        FoodNutritionRegistry.getExternalClassification(id))
                .effectApplier((player, data) -> {
                    if (ModuleCache.enableEffects) NutritionEffectApplier.apply(player, data);
                })
                .effectClearer(NutritionEffectApplier::clearAll)
                .previousEffectIds(EffectRegistry::getPreviousEffectIds)
                .effectDefinitionRegistered(id ->
                        EffectRegistry.getAll().stream().anyMatch(d -> d.effect().equals(id)))
                .valueDecayRateProvider(key -> {
                    SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
                    return snap != null ? (float) snap.decayRateFor(key)
                            : (float) NourishedConfig.get().decayRateFor(key);
                })
                .decayIntervalTicks(() -> {
                    SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
                    return snap != null ? snap.decayIntervalTicks() : 20;
                })
                .criticalThresholdProvider(key -> {
                    SyncNourishedConfigSnapshot snap = NourishedSyncHandler.getConfigSnapshot();
                    return snap != null ? (float) snap.criticalThreshold()
                            : (float) NourishedConfig.get().criticalThresholdFor(key);
                })
                .showJoinMessage(() -> NourishedConfig.get().showJoinMessage())
                .joinMessageLine1(() -> Component.literal("◆ ").withStyle(style -> style.withColor(0xF4C95D))
                        .append(Component.literal("NOURISHED").withStyle(style -> style.withColor(0x6FD3FF).withBold(true)))
                        .append(Component.literal(" ◆ ").withStyle(style -> style.withColor(0xF4C95D)))
                        .append(Component.literal(NourishedConfig.get().joinMessageLine1())
                                .withStyle(style -> style.withColor(0xCFEFFF))))
                .joinMessageLine2(() -> {
                    String line2 = NourishedConfig.get().joinMessageLine2();
                    int split = line2.indexOf(" - ");
                    Component line2Body = split >= 0
                            ? Component.literal(line2.substring(0, split + 1))
                                    .withStyle(style -> style.withColor(0xFF6B6B).withBold(true))
                                    .append(Component.literal(line2.substring(split + 1))
                                            .withStyle(style -> style.withColor(0xFFC2C2)))
                            : Component.literal(line2).withStyle(style -> style.withColor(0xFFC2C2));
                    return Component.literal("⚠ ").withStyle(ChatFormatting.RED).append(line2Body);
                })
                .trackingDeltaSyncer((player, data) -> ModNetworking.syncDietDelta(player, data))
                .syncOnJoin(NourishedSyncHandler::syncOnJoin)
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
                .clientMemoryConfigProvider(NourishedClientMemoryConfig::get)
                .playerDataProvider(new NourishedPlayerDataProvider())
                .registrationDelegate(new NourishedRegistrationDelegate())
                .heldItemTraceProvider((stack, rm) -> {
                    NutrientResolutionTrace t = FoodNutritionRegistry.resolveHeldItemTrace(stack, rm);
                    return t != null ? t.format() : "";
                })
                .heldItemClassificationTraceProvider((stack, rm) ->
                        FoodNutritionRegistry.resolveHeldItemClassificationTrace(stack, rm))
                .classifiedSourceProvider(ClassifiedSourceCollector::collectAllClassifiedSources)
                .schemaProviders(() -> java.util.List.of(
                        SchemaDefinition.forValue(),
                        SchemaDefinition.forSourceClassification(),
                        SchemaDefinition.forEffect(),
                        SchemaDefinition.forSynergy(),
                        SchemaDefinition.forSourcePairSynergy(),
                        SchemaDefinition.forMilestone(),
                        SchemaDefinition.forTrackingProfile(),
                        SchemaDefinition.forCompat()
                ))
                .build());
    }
    */
}
