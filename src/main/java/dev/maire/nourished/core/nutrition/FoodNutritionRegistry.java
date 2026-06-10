package dev.maire.nourished.core.nutrition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.compat.ModCompat;
import dev.marie.MariesLib.scan.ResolutionResult;
import dev.marie.MariesLib.scan.ResolutionStage;
import dev.marie.MariesLib.scan.RuntimeCascadeStage;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.marie.MariesLib.classification.ClassificationPipeline;
import dev.marie.MariesLib.classification.ClassificationTrace;
import dev.marie.MariesLib.classification.ClassificationTraceStep;
import dev.marie.MariesLib.classification.TraceStepId;
import dev.marie.MariesLib.classification.TraceStepStatus;
import dev.marie.MariesLib.scanner.ClassificationResult;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import dev.marie.MariesLib.scanner.RecipeInheritanceResolver;
import dev.marie.MariesLib.scanner.RecipeInheritanceResolver.RecipeInheritanceStep;
import dev.marie.MariesLib.scanner.ItemScanner;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * <h2>Nutrient pipeline resolution order</h2>
 * <p>End-to-end, bar weights and diet effects are derived in the following order. Higher steps
 * supersede lower ones where the implementation enforces precedence (see {@link #resolveNutrientBars}
 * and consumption/tooltip callers that merge {@link #getExternalClassification}).
 *
 * <ol>
 *   <li><b>Exact nutrient tags</b> — Datapack item tags under {@code data/nourished/tags/item/nutrients/}
 *       (bound to diet bars via {@link NutrientRegistry}). These are authoritative: any match from
 *       this family wins over heuristic and scanner-supplied guesses for that item.</li>
 *   <li><b>External compat registrations</b> — Classifications registered at runtime through the API
 *       ({@link dev.maire.nourished.api.NourishedAPI#registerSourceClassification} /
 *       {@link #registerClassification}). These take precedence over scanner-derived maps in
 *       {@link #getExternalClassification}.</li>
 *   <li><b>Scanner classifications</b> — Results from {@link ItemScanner} (async scan on
 *       world load), applied via {@link #applyFromScanner}. Scanner entries are skipped for
 *       items that already have nutrient tags or an API registration so tags always override scanner
 *       guesses.</li>
 *   <li><b>Blended tag + resolver results</b> — When both exact tag matches and
 *       {@link RuntimeFoodResolver} output are non-empty, {@link #blendTagAndResolverResults} merges
 *       them: tags keep full weight; the resolver may only add nutrients <em>not</em> already keyed
 *       by tags, at {@code 0.5f} weight before normalization.</li>
 *   <li><b>Runtime resolver</b> — {@link RuntimeFoodResolver} heuristic classification (stages such as
 *       community tags, keywords, recipe inheritance, peers, hard fallback). In
 *       {@link #resolveNutrientBars}, this path supplies the full map only when there are no exact
 *       nutrient tag matches; otherwise it participates only through the blend step above.</li>
 *   <li><b>Unclassified fallback</b> — Empty map: no diet bars, and tooltips show no nutrient bar
 *       lines (see unclassified messaging in {@link dev.marie.MariesLib.compat.MarieTooltipHelper}).</li>
 * </ol>
 *
 * <p><b>Datapack note:</b> Cross-mod tag entries (anything outside the {@code minecraft:} namespace,
 * including tag references like {@code #c:foods/...}) must use the object form with
 * {@code "required": false} so missing optional mods do not break tag loading and the rest of the pipeline.
 */
@ApiStatus.Internal
public class FoodNutritionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Milk buckets consume like food for effects but omit {@link DataComponents#FOOD}. Used for nutrient math everywhere
     * (consumption pipeline, HUD tooltips, JEI helper) so tag-based dairy gains match.
     */
    public static final FoodProperties MILK_BUCKET_FOOD_PROPERTIES = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0f)
            .alwaysEdible()
            .build();

    /** @GuardedBy("itself — ConcurrentHashSet") */
    private static final Set<String> WARNED_ITEMS = ConcurrentHashMap.newKeySet();

    /** @GuardedBy("itself — ConcurrentHashSet") Per-reload dedupe for empty blend warnings. */
    private static final Set<String> EMPTY_BLEND_WARNED = ConcurrentHashMap.newKeySet();

    private static final int EXTERNAL_CLASSIFICATION_CAP = 4096;

    /** @GuardedBy("itself — ConcurrentHashMap") */
    private static final Map<ResourceLocation, Map<String, Float>> EXTERNAL_CLASSIFICATIONS = new ConcurrentHashMap<>();

    /** @GuardedBy("itself — ConcurrentHashMap") */
    private static final Map<ResourceLocation, Map<String, Float>> SCANNER_CLASSIFICATIONS = new ConcurrentHashMap<>();

    /**
     * {@link FoodProperties} used when applying or previewing nourishment for an item stack. Vanilla milk buckets
     * participate in tagging but never report food properties.
     *
     * @param entity contextual entity for modded hooks; nullable on client previews
     */
    @Nullable
    public static FoodProperties foodPropertiesForNutrition(ItemStack stack, @Nullable LivingEntity entity) {
        FoodProperties base = stack.getItem().getFoodProperties(stack, entity);
        if (base != null) {
            return base;
        }
        if (stack.is(Items.MILK_BUCKET)) {
            return MILK_BUCKET_FOOD_PROPERTIES;
        }
        return null;
    }

    @Nullable
    private static volatile RecipeManager serverRecipeManager;

    @Nullable
    private static volatile RecipeInheritanceResolver serverRecipeInheritanceResolver;

    /**
     * Binds the active server {@link RecipeManager} for recipe-based diet bar inheritance.
     * Called from server lifecycle and after datapack reload; pass {@code null} on server stop.
     */
    public static void bindServerRecipeManager(@Nullable RecipeManager recipeManager) {
        serverRecipeManager = recipeManager;
        serverRecipeInheritanceResolver = recipeManager != null ? new RecipeInheritanceResolver(recipeManager) : null;
    }

    /**
     * Returns the bound server recipe manager, or {@code null} if the server is not ready.
     */
    @Nullable
    public static RecipeManager getServerRecipeManager() {
        return serverRecipeManager;
    }

    /**
     * Registers an API-driven food classification mapping a food item to a nutrient key with an amount.
     * Called by {@link dev.maire.nourished.api.NourishedAPI#registerSourceClassification}.
     */
    public static void registerClassification(ResourceLocation sourceId, String valueKey, float amount) {
        if (EXTERNAL_CLASSIFICATIONS.size() >= EXTERNAL_CLASSIFICATION_CAP && !EXTERNAL_CLASSIFICATIONS.containsKey(sourceId)) {
            LOGGER.warn("[FoodNutritionRegistry] External classification cap ({}) reached — ignoring: {} -> {}",
                    EXTERNAL_CLASSIFICATION_CAP, sourceId, valueKey);
            return;
        }
        EXTERNAL_CLASSIFICATIONS.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>()).put(valueKey, amount);
        LOGGER.info("[FoodNutritionRegistry] Registered external classification: {} -> {} ({})", sourceId, valueKey, amount);
    }

    /** Clears API-registered external classifications. Called during reload pipeline. */
    public static void clearExternalClassifications() {
        EXTERNAL_CLASSIFICATIONS.clear();
    }

    /**
     * Replaces scanner-derived classifications from per-item nutrient maps (e.g. full food scan).
     * Skips API-registered items and items that already have nutrient tags.
     */
    public static void applyFromScanner(Map<ResourceLocation, Map<String, Float>> perItemNutrients) {
        SCANNER_CLASSIFICATIONS.clear();
        for (Map.Entry<ResourceLocation, Map<String, Float>> entry : perItemNutrients.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            if (EXTERNAL_CLASSIFICATIONS.containsKey(itemId)) {
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            if (ItemScanner.hasValueTag(stack)) {
                continue;
            }
            Map<String, Float> inner = entry.getValue();
            if (inner == null || inner.isEmpty()) {
                continue;
            }
            SCANNER_CLASSIFICATIONS.put(itemId, new ConcurrentHashMap<>(inner));
        }
        RuntimeFoodResolver.getInstance().invalidateCache();
        Nourished.LOGGER.info("[FoodNutritionRegistry] Scanner applied {} classifications", SCANNER_CLASSIFICATIONS.size());
    }

    /** Clears scanner-derived classifications only. */
    public static void clearScannerClassifications() {
        SCANNER_CLASSIFICATIONS.clear();
    }

    /** Clears per-reload warning dedupe sets. Called after datapack/config reload. */
    public static void clearPerReloadWarnings() {
        WARNED_ITEMS.clear();
        EMPTY_BLEND_WARNED.clear();
    }

    /**
     * Returns classifications for a food item: API {@link #registerClassification} first, then scanner-derived
     * {@link #applyFromScanner} as fallback; {@code null} if neither applies.
     */
    public static Map<String, Float> getExternalClassification(ResourceLocation sourceId) {
        Map<String, Float> api = EXTERNAL_CLASSIFICATIONS.get(sourceId);
        if (api != null) {
            return api;
        }
        return SCANNER_CLASSIFICATIONS.get(sourceId);
    }

    /**
     * Package-private: returns true if the given item has an API-registered external classification.
     */
    static boolean hasApiClassification(ResourceLocation sourceId) {
        return EXTERNAL_CLASSIFICATIONS.containsKey(sourceId);
    }

    /**
     * Package-private: returns true if the given item has a scanner-derived classification.
     */
    static boolean hasScannerClassification(ResourceLocation sourceId) {
        return SCANNER_CLASSIFICATIONS.containsKey(sourceId);
    }

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    /** Diet UI deltas; nutrient values are driven by NutrientRegistry keys. */
    public record DietDelta(float calories, Map<String, Float> nutrients) {}

    /**
     * Full resolution trace for debug logging (server-side recipe inheritance when {@code level} is a server level).
     * {@code classifierPath} includes {@code TAG_AND_RUNTIME_BLEND} when tag matches and runtime resolver output
     * are merged to match {@link #resolveNutrientBars}.
     */
    public record NutrientResolutionDiagnostic(
            Map<String, Float> matchedBars,
            String classifierPath,
            ResolutionStage pipelineStage,
            List<String> matchedNutrientTags,
            @Nullable ClassificationResult classification,
            List<RecipeInheritanceStep> recipeInheritance,
            boolean foodOverride
    ) {
        public NutrientResolutionDiagnostic {
            matchedBars = Map.copyOf(matchedBars);
            matchedNutrientTags = List.copyOf(matchedNutrientTags);
            recipeInheritance = List.copyOf(recipeInheritance);
        }
    }

    /**
     * Called after {@link NutrientRegistry#load()} (and on reload). No registry rebuild here;
     * tagless bar resolution uses the scanner cache and classifier at query time in {@link #resolveNutrientBars}.
     */
    public static void init() {
        // Intentionally empty — kept for API compatibility with {@link NutrientRegistry#reload()}.
    }

    /**
     * Resolves diet nutrient bar weights from {@code nourished:nutrients/*} tags, with runtime inference
     * fallback via {@link RuntimeFoodResolver} when untagged. Delegates to
     * {@link #resolveNutrientBars(ItemStack, boolean, RecipeManager)} after extracting the recipe manager
     * from the level (server-side only).
     *
     * @param warnIfUnmatched when true, logs a WARN when falling back from no nutrient tags
     * @param level when non-null and not client-side, recipe inheritance may run; client tooltips should pass a
     *              client level or {@code null} so inheritance is skipped
     * @return map of nutrient bar key -> match weight
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched, @Nullable Level level) {
        RecipeManager rm = null;
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            rm = level.getServer().getRecipeManager();
        }
        if (rm == null) {
            rm = serverRecipeManager;
        }
        return resolveNutrientBars(stack, warnIfUnmatched, rm);
    }

    /**
     * Core resolution: merges {@code nourished:nutrients/*} tag matches with
     * {@link RuntimeFoodResolver} output when both are present; otherwise returns whichever side
     * has matches (resolver-only still logs {@code warnIfUnmatched} when untagged).
     *
     * @param warnIfUnmatched when true, logs a WARN when falling back from no nutrient tags
     * @param recipeManager   server recipe manager for recipe inheritance, or {@code null} to skip it
     * @return map of nutrient bar key -> match weight (never null)
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched, @Nullable RecipeManager recipeManager) {
        Item item = stack.getItem();
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack.getItem());
        if (itemId != null && ScannerSpecRegistry.get().excludedItems().contains(itemId.toString())) {
            return Map.of();
        }
        Map<String, Float> tagMatches = collectNutrientTagMatches(item);
        Map<String, Float> resolved = RuntimeFoodResolver.getInstance().resolve(stack, recipeManager);

        if (tagMatches.isEmpty()) {
            if (warnIfUnmatched) {
                String id = item.getDescriptionId();
                if (WARNED_ITEMS.add(id)) {
                    LOGGER.warn(
                            "Nourished: no nutrient tag for {} — attempting name-based guess. Add it to data/" + Nourished.MODID + "/tags/item/nutrients/*.json for accurate classification.",
                            id);
                }
            }
            return resolved.isEmpty() ? Map.of() : resolved;
        }
        if (resolved.isEmpty()) {
            return tagMatches;
        }
        return blendTagAndResolverResults(tagMatches, resolved);
    }

    /**
     * Resolves nutrient bars and captures diagnostic detail for structured debug logs.
     * Mirrors {@link #resolveNutrientBars(ItemStack, boolean, RecipeManager)}: tag-only, resolver-only,
     * unclassified, or blended when both tag matches and resolver output are non-empty.
     */
    public static NutrientResolutionDiagnostic resolveNutrientBarsDiagnostic(ItemStack stack, Level level) {
        Map<String, Float> tagMatches = collectNutrientTagMatches(stack.getItem());
        List<String> matchedTagIds = collectExactMatchedNutrientTagIds(stack.getItem(), tagMatches);

        RecipeManager rm = null;
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            rm = level.getServer().getRecipeManager();
        }
        Map<String, Float> resolved = RuntimeFoodResolver.getInstance().resolve(stack, rm);

        if (tagMatches.isEmpty()) {
            ResolutionStage stage = resolved.isEmpty() ? ResolutionStage.UNCLASSIFIED : ResolutionStage.RUNTIME_RESOLVER;
            String path = resolved.isEmpty() ? "UNCLASSIFIED" : "RUNTIME_RESOLVER";
            return new NutrientResolutionDiagnostic(
                    new LinkedHashMap<>(resolved),
                    path,
                    stage,
                    List.of("none"),
                    null,
                    List.of(),
                    false
            );
        }
        if (resolved.isEmpty()) {
            return new NutrientResolutionDiagnostic(
                    new LinkedHashMap<>(tagMatches),
                    "TAG_HIT",
                    ResolutionStage.TAG_MATCH,
                    matchedTagIds,
                    null,
                    List.of(),
                    false
            );
        }
        Map<String, Float> blended = blendTagAndResolverResults(tagMatches, resolved);
        return new NutrientResolutionDiagnostic(
                new LinkedHashMap<>(blended),
                "TAG_AND_RUNTIME_BLEND",
                ResolutionStage.BLENDED,
                matchedTagIds,
                null,
                List.of(),
                false
        );
    }

    /**
     * Full classification trace for held-item debugging via {@code /nourished debug held}.
     * Builds the trace with all intermediate steps, precedence decisions, and the final merged result.
     * Returns a {@link ClassificationTrace} with pipeline = HELD_DEBUG.
     *
     * @param stack         the item to resolve
     * @param recipeManager server recipe manager, or null for client/no-recipe-inheritance
     * @return ClassificationTrace with all steps and final outcome
     */
    public static ClassificationTrace resolveHeldItemClassificationTrace(ItemStack stack, @Nullable RecipeManager recipeManager) {
        Item item = stack.getItem();
        ResourceLocation itemId = MarieRegistryUtils.itemKey(item);
        String itemIdStr = itemId != null ? itemId.toString() : "unknown";

        List<ClassificationTraceStep> traceSteps = new ArrayList<>();

        if (itemId != null && ScannerSpecRegistry.get().excludedItems().contains(itemIdStr)) {
            Map<String, Object> discoveryDetail = new LinkedHashMap<>();
            discoveryDetail.put("itemId", itemIdStr);
            discoveryDetail.put("errorCode", "EXCLUDED_ITEM");
            traceSteps.add(new ClassificationTraceStep(
                    TraceStepId.ITEM_DISCOVERY,
                    TraceStepStatus.FAILURE,
                    "Item excluded from classification",
                    discoveryDetail));
            return ClassificationTrace.builder(itemIdStr, ClassificationPipeline.HELD_DEBUG)
                    .addSteps(traceSteps)
                    .summaryReason("excluded_items")
                    .build();
        }

        Map<String, Float> tagRaw = collectNutrientTagMatchesRaw(item);
        Map<String, Float> tagFiltered = collectNutrientTagMatches(item);

        List<String> strippedByCompat = new ArrayList<>();
        for (String key : tagRaw.keySet()) {
            if (!tagFiltered.containsKey(key)) {
                strippedByCompat.add(key);
            }
        }

        Map<String, Object> tagDetail = new LinkedHashMap<>();
        tagDetail.put("tagsMatched", tagFiltered.size());
        tagDetail.put("rawTags", new LinkedHashMap<>(tagRaw));
        tagDetail.put("afterCompat", new LinkedHashMap<>(tagFiltered));
        if (!strippedByCompat.isEmpty()) {
            tagDetail.put("strippedByCompat", strippedByCompat);
            tagDetail.put("warningCode", "TAG_COMPAT_STRIPPED");
        }
        traceSteps.add(new ClassificationTraceStep(
                TraceStepId.VALUE_TAG_LOOKUP,
                tagFiltered.isEmpty() ? TraceStepStatus.SKIPPED : TraceStepStatus.SUCCESS,
                tagFiltered.isEmpty() ? "No nutrient tags matched" : "Matched " + tagFiltered.size() + " nutrient tag(s)",
                tagDetail));

        Map<String, Float> external = itemId != null ? getExternalClassification(itemId) : null;
        NutrientResolutionTrace.ExternalSource externalSource = NutrientResolutionTrace.ExternalSource.NONE;
        if (itemId != null && hasApiClassification(itemId)) {
            externalSource = NutrientResolutionTrace.ExternalSource.API;
        } else if (itemId != null && hasScannerClassification(itemId)) {
            externalSource = NutrientResolutionTrace.ExternalSource.SCANNER;
        }
        if (external == null) {
            external = Map.of();
        }

        if (!external.isEmpty()) {
            Map<String, Object> externalDetail = new LinkedHashMap<>();
            externalDetail.put("source", externalSource.name());
            externalDetail.put("nutrients", new LinkedHashMap<>(external));
            traceSteps.add(new ClassificationTraceStep(
                    TraceStepId.EXTERNAL_CLASSIFICATION,
                    TraceStepStatus.SUCCESS,
                    "External classification from " + externalSource.name(),
                    externalDetail));
        }

        ClassificationTrace runtimeTrace = RuntimeFoodResolver.getInstance().resolveWithTrace(stack, recipeManager);
        Map<String, Float> resolved = Map.of();
        RuntimeCascadeStage cascadeStage = null;
        if (runtimeTrace != null) {
            resolved = runtimeTrace.finalBars();
            cascadeStage = runtimeTrace.cascadeStage();
            traceSteps.addAll(runtimeTrace.steps());
        }

        Map<String, Float> blendTagInput = Map.of();
        Map<String, Float> blendResolverInput = Map.of();
        Map<String, TagRuntimeBlend.Precedence> blendPrecedence = Map.of();
        Map<String, Float> blendDiscarded = Map.of();
        Map<String, Float> coreResult;

        boolean didBlend = false;
        if (tagFiltered.isEmpty()) {
            coreResult = resolved.isEmpty() ? Map.of() : new LinkedHashMap<>(resolved);
        } else if (resolved.isEmpty()) {
            coreResult = new LinkedHashMap<>(tagFiltered);
        } else {
            TagRuntimeBlend.BlendOutcome blend = TagRuntimeBlend.blend(tagFiltered, resolved);
            coreResult = new LinkedHashMap<>(blend.result());
            blendTagInput = tagFiltered;
            blendResolverInput = resolved;
            blendPrecedence = blend.perKeyPrecedence();
            blendDiscarded = blend.discardedResolver();
            didBlend = true;

            Map<String, Object> blendDetail = new LinkedHashMap<>();
            blendDetail.put("tagPrecedence", true);
            blendDetail.put("tagKeys", new ArrayList<>(blendTagInput.keySet()));
            blendDetail.put("runtimeKeys", new ArrayList<>(blendResolverInput.keySet()));
            blendDetail.put("discardedRuntimeKeys", new ArrayList<>(blendDiscarded.keySet()));
            blendDetail.put("blendedResult", new LinkedHashMap<>(coreResult));
            traceSteps.add(new ClassificationTraceStep(
                    TraceStepId.TAG_RUNTIME_BLEND,
                    TraceStepStatus.SUCCESS,
                    "Tag and runtime results blended",
                    blendDetail));
        }

        Map<String, Float> finalMerged = new LinkedHashMap<>(coreResult);
        if (!external.isEmpty()) {
            for (Map.Entry<String, Float> e : external.entrySet()) {
                finalMerged.merge(e.getKey(), e.getValue(), Float::sum);
            }
        }

        String dominant = null;
        float maxValue = 0f;
        for (Map.Entry<String, Float> entry : finalMerged.entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                dominant = entry.getKey();
            }
        }

        ResolutionStage stage;
        if (finalMerged.isEmpty()) {
            stage = ResolutionStage.UNCLASSIFIED;
        } else if (didBlend) {
            stage = ResolutionStage.BLENDED;
        } else if (!tagFiltered.isEmpty()) {
            stage = ResolutionStage.TAG_MATCH;
        } else if (!resolved.isEmpty()) {
            stage = ResolutionStage.RUNTIME_RESOLVER;
        } else if (!external.isEmpty()) {
            stage = ResolutionStage.SCANNER_CLASSIFIED;
        } else {
            stage = ResolutionStage.UNCLASSIFIED;
        }

        String summaryReason = stage == ResolutionStage.UNCLASSIFIED ? "unclassified"
                : didBlend ? "tag_and_runtime_blend"
                : !tagFiltered.isEmpty() ? "tag_match"
                : !resolved.isEmpty() ? "runtime_resolver"
                : !external.isEmpty() ? "scanner_classified"
                : "unknown";

        return ClassificationTrace.builder(itemIdStr, ClassificationPipeline.HELD_DEBUG)
                .finalBars(finalMerged)
                .dominant(dominant)
                .cascadeStage(cascadeStage)
                .tagClassified(!tagFiltered.isEmpty())
                .summaryReason(summaryReason)
                .addSteps(traceSteps)
                .build();
    }

    /**
     * Full resolution trace for held-item debugging via {@code /nourished debug held}.
     * Builds the trace with all intermediate steps, precedence decisions, and the final merged result.
     *
     * <p>The final bar map includes external classification merged in the same way as tooltip display,
     * so the trace reflects player-visible bar weights. Note: the server eat pipeline applies external
     * to <em>deltas</em> only, so trace may differ from {@link dev.maire.nourished.core.handler.FoodNutrientPipeline}
     * matched bars.</p>
     *
     * @param stack         the item to resolve
     * @param recipeManager server recipe manager, or null for client/no-recipe-inheritance
     * @return resolution trace with final stage, bars, and full trace pipeline
     */
    public static NutrientResolutionTrace resolveHeldItemTrace(ItemStack stack, @Nullable RecipeManager recipeManager) {
        Item item = stack.getItem();
        ResourceLocation itemId = MarieRegistryUtils.itemKey(item);
        String itemIdStr = itemId != null ? itemId.toString() : "unknown";

        if (itemId != null && ScannerSpecRegistry.get().excludedItems().contains(itemIdStr)) {
            return new NutrientResolutionTrace(
                    itemIdStr,
                    Map.of(), Map.of(), List.of(),
                    Map.of(), NutrientResolutionTrace.ExternalSource.NONE,
                    Map.of(), null, Map.of(),
                    Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(),
                    ResolutionStage.UNCLASSIFIED
            );
        }

        Map<String, Float> tagRaw = collectNutrientTagMatchesRaw(item);
        Map<String, Float> tagFiltered = collectNutrientTagMatches(item);

        List<String> strippedByCompat = new ArrayList<>();
        for (String key : tagRaw.keySet()) {
            if (!tagFiltered.containsKey(key)) {
                strippedByCompat.add(key);
            }
        }

        Map<String, Float> external = itemId != null ? getExternalClassification(itemId) : null;
        NutrientResolutionTrace.ExternalSource externalSource = NutrientResolutionTrace.ExternalSource.NONE;
        if (itemId != null && hasApiClassification(itemId)) {
            externalSource = NutrientResolutionTrace.ExternalSource.API;
        } else if (itemId != null && hasScannerClassification(itemId)) {
            externalSource = NutrientResolutionTrace.ExternalSource.SCANNER;
        }
        if (external == null) {
            external = Map.of();
        }

        ResolutionResult resolverResult = RuntimeFoodResolver.getInstance().resolveWithResult(stack, recipeManager);
        Map<String, Float> resolved = resolverResult != null ? resolverResult.values() : Map.of();
        RuntimeCascadeStage cascadeStage = resolverResult != null ? resolverResult.stage() : null;
        Map<String, String> rejectedSignals = resolverResult != null ? resolverResult.rejectedSignals() : Map.of();

        Map<String, Float> blendTagInput = Map.of();
        Map<String, Float> blendResolverInput = Map.of();
        Map<String, TagRuntimeBlend.Precedence> blendPrecedence = Map.of();
        Map<String, Float> blendDiscarded = Map.of();
        Map<String, Float> coreResult;

        boolean didBlend = false;
        if (tagFiltered.isEmpty()) {
            coreResult = resolved.isEmpty() ? Map.of() : new LinkedHashMap<>(resolved);
        } else if (resolved.isEmpty()) {
            coreResult = new LinkedHashMap<>(tagFiltered);
        } else {
            TagRuntimeBlend.BlendOutcome blend = TagRuntimeBlend.blend(tagFiltered, resolved);
            coreResult = new LinkedHashMap<>(blend.result());
            blendTagInput = tagFiltered;
            blendResolverInput = resolved;
            blendPrecedence = blend.perKeyPrecedence();
            blendDiscarded = blend.discardedResolver();
            didBlend = true;
        }

        Map<String, Float> finalMerged = new LinkedHashMap<>(coreResult);
        if (!external.isEmpty()) {
            for (Map.Entry<String, Float> e : external.entrySet()) {
                finalMerged.merge(e.getKey(), e.getValue(), Float::sum);
            }
        }

        ResolutionStage stage;
        if (finalMerged.isEmpty()) {
            stage = ResolutionStage.UNCLASSIFIED;
        } else if (didBlend) {
            stage = ResolutionStage.BLENDED;
        } else if (!tagFiltered.isEmpty()) {
            stage = ResolutionStage.TAG_MATCH;
        } else if (!resolved.isEmpty()) {
            stage = external.isEmpty() ? ResolutionStage.RUNTIME_RESOLVER : ResolutionStage.RUNTIME_RESOLVER;
        } else if (!external.isEmpty()) {
            stage = ResolutionStage.SCANNER_CLASSIFIED;
        } else {
            stage = ResolutionStage.UNCLASSIFIED;
        }

        warnIfEmptyWithSignals(itemIdStr, stage, tagRaw, tagFiltered, resolved, external, blendDiscarded);

        return new NutrientResolutionTrace(
                itemIdStr,
                tagRaw, tagFiltered, strippedByCompat,
                external, externalSource,
                resolved, cascadeStage, rejectedSignals,
                blendTagInput, blendResolverInput, blendPrecedence, blendDiscarded,
                finalMerged,
                stage
        );
    }

    /**
     * Logs a WARN if at least one signal existed but the final result is empty.
     * Only fires once per item per reload cycle to avoid log spam.
     */
    private static void warnIfEmptyWithSignals(
            String itemIdStr,
            ResolutionStage stage,
            Map<String, Float> tagRaw,
            Map<String, Float> tagFiltered,
            Map<String, Float> resolved,
            Map<String, Float> external,
            Map<String, Float> blendDiscarded
    ) {
        if (stage != ResolutionStage.UNCLASSIFIED) {
            return;
        }

        boolean hadSignal = !tagRaw.isEmpty() || !tagFiltered.isEmpty()
                || !resolved.isEmpty() || !external.isEmpty();
        if (!hadSignal) {
            return;
        }

        if (!EMPTY_BLEND_WARNED.add(itemIdStr)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[FoodNutritionRegistry] Empty resolution despite signals for ").append(itemIdStr).append(":\n");
        sb.append("  tagRaw=").append(tagRaw.keySet()).append("\n");
        sb.append("  tagFiltered=").append(tagFiltered.keySet()).append("\n");
        sb.append("  resolved=").append(resolved.keySet()).append("\n");
        sb.append("  external=").append(external.keySet()).append("\n");
        sb.append("  blendDiscarded=").append(blendDiscarded.keySet()).append("\n");
        sb.append("  stage=").append(stage.name());

        LOGGER.warn(sb.toString());
    }

    /**
     * Collects raw tag matches <em>before</em> compat filter, for trace comparison.
     */
    private static Map<String, Float> collectNutrientTagMatchesRaw(Item item) {
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = new ItemStack(item).getItemHolder();

        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                var tagKey = MarieRegistryUtils.itemTagKey(tagStr);
                if (holder.is(tagKey)) {
                    matches.put(def.key(), 1.0f);
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * For each matched nutrient bar key, the first {@code nourished:nutrients/...} tag string the item actually holds.
     */
    private static List<String> collectExactMatchedNutrientTagIds(Item item, Map<String, Float> tagMatches) {
        if (tagMatches.isEmpty()) {
            return List.of();
        }
        var holder = new ItemStack(item).getItemHolder();
        List<String> out = new ArrayList<>();
        for (String barKey : tagMatches.keySet()) {
            for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
                if (!def.key().equals(barKey)) {
                    continue;
                }
                for (String tagStr : def.tags()) {
                    var tagKey = MarieRegistryUtils.itemTagKey(tagStr);
                    if (holder.is(tagKey)) {
                        out.add(tagKey.location().toString());
                        break;
                    }
                }
                break;
            }
        }
        return out;
    }

    /**
     * Same as {@link #resolveNutrientBars(ItemStack, boolean, Level)} with no recipe inheritance
     * (no {@link Level} context).
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched) {
        return resolveNutrientBars(stack, warnIfUnmatched, (RecipeManager) null);
    }

    /**
     * Nutrient bar weights from datapack {@code nourished:nutrients/*} tags (compat-filtered).
     * Used by tooling that snapshots the classified food registry.
     */
    public static Map<String, Float> getNutrientTagScores(Item item) {
        Map<String, Float> matches = collectNutrientTagMatches(item);
        return matches.isEmpty() ? Map.of() : Map.copyOf(matches);
    }

    private static Map<String, Float> collectNutrientTagMatches(Item item) {
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = new ItemStack(item).getItemHolder();

        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                var tagKey = MarieRegistryUtils.itemTagKey(tagStr);
                if (holder.is(tagKey)) {
                    matches.put(def.key(), 1.0f);
                    break;
                }
            }
        }

        matches.entrySet().removeIf(entry -> {
            String namespace = itemId.getNamespace();
            String modid = ModCompat.namespaceToModid(namespace);
            return modid != null && !NourishedConfig.get().isTagCompatEnabled(modid);
        });

        return matches;
    }

    /**
     * @param silent when {@code true}, skips {@link #resolvePrimaryNutrientBar} unmatched-item warnings (use on client
     *               tooltips / JEI where tags may not be committed yet).
     */
    public static NutrientValues getNutrients(ItemStack stack, Level level, boolean silent) {
        FoodProperties food = foodPropertiesForNutrition(stack, null);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        String bar = resolvePrimaryNutrientBar(stack, !silent, level);
        float totalPoints = Math.max(1.0f, food.nutrition() + Math.max(0f, food.saturation()) * 0.5f);
        return nutrientValuesForBar(bar, totalPoints);
    }

    /** Same as {@link #getNutrients(ItemStack, Level, boolean)} with {@code silent == false}. */
    public static NutrientValues getNutrients(ItemStack stack, Level level) {
        return getNutrients(stack, level, false);
    }

    /** NutrientValues with primary macro weighting for the given diet bar, driven by FoodValueRegistry. */
    private static NutrientValues nutrientValuesForBar(String barKey, float pts) {
        return FoodValueRegistry.getValuesForCategory(barKey, pts);
    }

    private static Map<String, Float> blendTagAndResolverResults(
            Map<String, Float> tagMatches,
            Map<String, Float> resolved) {
        // Tags are authoritative — seed from them at full weight.
        // Resolver may only contribute nutrients not already covered by tags,
        // at 0.5x weight so heuristic signal supplements without competing.
        Map<String, Float> merged = new LinkedHashMap<>(tagMatches);
        resolved.forEach((k, v) -> {
            if (!tagMatches.containsKey(k)) {
                merged.merge(k, v * 0.5f, Float::sum);
            }
        });
        float total = 0f;
        for (float v : merged.values()) total += v;
        if (total <= 0f) return tagMatches;
        final float norm = total;
        Map<String, Float> result = new LinkedHashMap<>(merged.size());
        merged.forEach((k, v) -> result.put(k, v / norm));
        return result;
    }

    public static DietDelta computeDietDelta(
            ItemStack stack,
            @Nullable Level level,
            int foodNutrition,
            float foodSaturation,
            Map<String, Float> matchedBars) {
        int calories = Math.max(0, Math.round(foodNutrition * 25f));
        Objects.requireNonNull(matchedBars, "matchedBars");

        float matchedWeightTotal = 0f;
        for (float weight : matchedBars.values()) {
            matchedWeightTotal += Math.max(0f, weight);
        }
        if (matchedWeightTotal <= 0f) {
            Map<String, Float> zeros = new HashMap<>();
            for (String key : NutrientRegistry.getKeys()) {
                zeros.put(key, 0f);
            }
            return new DietDelta(calories, zeros);
        }

        float burst = foodNutrition * 0.008f + foodSaturation * 0.010f + 0.004f;
        float totalBurst = burst * matchedWeightTotal;

        Map<String, Float> nutrients = new HashMap<>();
        List<String> keys = NutrientRegistry.getKeys();

        for (String key : keys) {
            nutrients.put(key, 0f);
        }

        for (Map.Entry<String, Float> entry : matchedBars.entrySet()) {
            float weight = Math.max(0f, entry.getValue());
            if (weight <= 0f) {
                continue;
            }
            float contribution = totalBurst * (weight / matchedWeightTotal);
            String key = entry.getKey();
            if (nutrients.containsKey(key)) {
                nutrients.put(key, nutrients.get(key) + contribution);
            }
        }

        float scale = configuredNutrientGainScale();
        Map<String, Float> scaledNutrients = new HashMap<>();
        for (Map.Entry<String, Float> e : nutrients.entrySet()) {
            scaledNutrients.put(e.getKey(), e.getValue() * scale);
        }

        float perBiteMax = configuredNutrientGainPerBiteMax();
        for (String k : scaledNutrients.keySet()) {
            scaledNutrients.put(k, Math.min(scaledNutrients.get(k), perBiteMax));
        }

        return new DietDelta(calories, scaledNutrients);
    }

    public static DietDelta computeDietDelta(ItemStack stack, @Nullable Level level, int foodNutrition, float foodSaturation) {
        return computeDietDelta(
                stack,
                level,
                foodNutrition,
                foodSaturation,
                resolveNutrientBars(stack, false, level));
    }

    private static String resolvePrimaryNutrientBar(ItemStack stack, boolean warnIfUnmatched, Level level) {
        Map<String, Float> bars = resolveNutrientBars(stack, warnIfUnmatched, level);
        if (!bars.isEmpty()) return bars.keySet().iterator().next();
        return "";
    }

    private static float configuredNutrientGainScale() {
        try {
            return Mth.clamp((float) NourishedConfig.get().nutrientGainScale(), 0.5f, 20f);
        } catch (IllegalStateException ignored) {
            return 5f;
        }
    }

    private static float configuredNutrientGainPerBiteMax() {
        try {
            return Mth.clamp((float) NourishedConfig.get().nutrientGainPerBiteMax(), 0.05f, 1f);
        } catch (IllegalStateException ignored) {
            return 0.2f;
        }
    }
}
