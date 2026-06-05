package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.core.nutrition.cache.RunningAverage;
import dev.maire.nourished.tooling.classification.ClassificationTraceStep;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context passed through the resolution pipeline to each stage handler.
 * Mutable to allow earlier stages to deposit signals for later stages.
 */
public final class StageContext {
    private final Holder<Item> holder;
    private final ResourceLocation itemId;
    @Nullable private final RecipeManager recipeManager;
    private final Map<String, RunningAverage> namespacePeers;
    private final Set<String> validKeys;
    private final Map<String, Float> communityTagSignal = new HashMap<>();
    @Nullable private final List<ClassificationTraceStep> traceOut;
    @Nullable private String recipeFailureReason;
    @Nullable private List<Map<String, Object>> archetypeMatches;
    @Nullable private Map<String, Float> negativeContributions;
    @Nullable private Map<String, List<String>> tokenDemotions;

    public StageContext(Holder<Item> holder, ResourceLocation itemId,
                        @Nullable RecipeManager recipeManager,
                        Map<String, RunningAverage> namespacePeers,
                        Set<String> validKeys) {
        this(holder, itemId, recipeManager, namespacePeers, validKeys, null);
    }

    public StageContext(Holder<Item> holder, ResourceLocation itemId,
                        @Nullable RecipeManager recipeManager,
                        Map<String, RunningAverage> namespacePeers,
                        Set<String> validKeys,
                        @Nullable List<ClassificationTraceStep> traceOut) {
        this.holder = holder;
        this.itemId = itemId;
        this.recipeManager = recipeManager;
        this.namespacePeers = namespacePeers;
        this.validKeys = validKeys;
        this.traceOut = traceOut;
    }

    public Holder<Item> holder() { return holder; }
    public ResourceLocation itemId() { return itemId; }
    @Nullable public RecipeManager recipeManager() { return recipeManager; }
    public Map<String, RunningAverage> namespacePeers() { return namespacePeers; }
    public Set<String> validKeys() { return validKeys; }
    public Map<String, Float> communityTagSignal() { return communityTagSignal; }
    @Nullable public List<ClassificationTraceStep> traceOut() { return traceOut; }

    public void setRecipeFailureReason(String reason) { this.recipeFailureReason = reason; }
    @Nullable public String recipeFailureReason() { return recipeFailureReason; }

    public List<Map<String, Object>> archetypeMatches() {
        if (archetypeMatches == null) archetypeMatches = new ArrayList<>();
        return archetypeMatches;
    }
    public boolean hasArchetypeMatches() { return archetypeMatches != null && !archetypeMatches.isEmpty(); }

    public Map<String, Float> negativeContributions() {
        if (negativeContributions == null) negativeContributions = new LinkedHashMap<>();
        return negativeContributions;
    }
    public boolean hasNegativeContributions() { return negativeContributions != null && !negativeContributions.isEmpty(); }

    public Map<String, List<String>> tokenDemotions() {
        if (tokenDemotions == null) tokenDemotions = new LinkedHashMap<>();
        return tokenDemotions;
    }
    public boolean hasTokenDemotions() { return tokenDemotions != null && !tokenDemotions.isEmpty(); }

    /**
     * Adds a trace step if tracing is enabled (traceOut is non-null).
     */
    public void addTraceStep(ClassificationTraceStep step) {
        if (traceOut != null) {
            traceOut.add(step);
        }
    }
}
