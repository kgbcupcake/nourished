package dev.maire.nourished.core.micro;

import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.marie.framework.scanner.RecipeInheritanceResolver;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Works out how much of each nutrient a food carries: explicit item/tag entries first, then the sum
 * of its recipe ingredients, then a share of its food-group classification.
 */
public final class MicroProfileResolver {

    private static final int MAX_DEPTH = 3;

    /** Where a food's nutrients came from. */
    public enum Source { EXPLICIT, RECIPE, GROUP, NONE }

    public record Profile(Map<String, Float> values, Source source) {
        static final Profile EMPTY = new Profile(Map.of(), Source.NONE);
    }

    private record RecipeSlot(RecipeManager manager, RecipeInheritanceResolver resolver) {}

    private static final Map<ResourceLocation, Profile> SERVER_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Profile> CLIENT_CACHE = new ConcurrentHashMap<>();
    private static volatile RecipeSlot serverRecipes;
    private static volatile RecipeSlot clientRecipes;

    private MicroProfileResolver() {}

    public static void clearCache() {
        SERVER_CACHE.clear();
        CLIENT_CACHE.clear();
        // /reload keeps the same RecipeManager, so the identity check in recipesFor won't catch it
        serverRecipes = null;
        clientRecipes = null;
    }

    /** Drops resolved profiles but keeps the recipe index; for food-group/tag edits that don't touch recipes. */
    public static void clearProfiles() {
        SERVER_CACHE.clear();
        CLIENT_CACHE.clear();
    }

    /** Nutrient values per serving of {@code stack}; empty for non-food, excluded, or untracked items. */
    public static Map<String, Float> resolve(ItemStack stack, @Nullable Level level) {
        return resolveProfile(stack, level).values();
    }

    /** Like {@link #resolve} but also says which step supplied the values. */
    public static Profile resolveProfile(ItemStack stack, @Nullable Level level) {
        // milk has no vanilla food properties, so ask the nutrition registry instead
        if (stack.isEmpty() || FoodNutritionRegistry.foodPropertiesForNutrition(stack, null) == null) {
            return Profile.EMPTY;
        }
        boolean clientSide = level != null && level.isClientSide();
        MicroRegistry.Snapshot snap = MicroRegistry.forSide(clientSide);
        if (!snap.settings().enabled() || snap.defs().isEmpty()) {
            return Profile.EMPTY;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (NutrientClassificationLookup.isExcluded(id.toString())) {
            return Profile.EMPTY;
        }
        Map<ResourceLocation, Profile> cache = clientSide ? CLIENT_CACHE : SERVER_CACHE;
        Profile cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        RecipeInheritanceResolver recipes = snap.settings().recipeDerivation() && level != null
                ? recipesFor(level.getRecipeManager(), clientSide) : null;
        Profile found = profileOf(id, stack, level, snap, recipes, 0, new HashSet<>());
        Profile profile = new Profile(Map.copyOf(found.values()), found.values().isEmpty() ? Source.NONE : found.source());
        // without a level recipes were skipped, so don't let that partial answer stick
        if (recipes != null || !snap.settings().recipeDerivation()) {
            cache.put(id, profile);
        }
        return profile;
    }

    /** How much one serving fills a nutrient whose profile value is 1.0; mirrors the group bars' per-bite burst. */
    public static float biteGain(FoodProperties food, boolean clientSide) {
        float base = food.nutrition() * 0.008f + food.saturation() * 0.010f + 0.004f;
        return base * MicroRegistry.forSide(clientSide).settings().gainScale();
    }

    private static Profile profileOf(ResourceLocation id, ItemStack stack, @Nullable Level level,
                                     MicroRegistry.Snapshot snap,
                                     @Nullable RecipeInheritanceResolver recipes,
                                     int depth, Set<ResourceLocation> visiting) {
        // an entry that zeroes every nutrient still counts, so derivation doesn't refill it
        Map<String, Float> explicit = explicitValues(id, stack.getItem(), snap);
        if (explicit != null) {
            return new Profile(explicit, Source.EXPLICIT);
        }
        if (recipes != null && depth < MAX_DEPTH && visiting.add(id)) {
            Map<String, Float> derived = derive(id, level, snap, recipes, depth, visiting);
            visiting.remove(id);
            if (!derived.isEmpty()) {
                return new Profile(derived, Source.RECIPE);
            }
        }
        return new Profile(groupFallback(stack, level, snap), Source.GROUP);
    }

    private static Map<String, Float> derive(ResourceLocation id, @Nullable Level level, MicroRegistry.Snapshot snap,
                                             RecipeInheritanceResolver recipes, int depth,
                                             Set<ResourceLocation> visiting) {
        // each recipe is summed on its own, then alternative recipes (furnace/smoker/campfire...) are averaged
        Map<String, Float> sum = new HashMap<>();
        int counted = 0;
        for (List<ResourceLocation> recipe : recipes.getRecipes(id)) {
            Map<String, Float> one = new HashMap<>();
            for (ResourceLocation ingredientId : recipe) {
                if (visiting.contains(ingredientId)) continue;
                Item item = BuiltInRegistries.ITEM.get(ingredientId);
                if (item == Items.AIR) continue;
                Map<String, Float> part = profileOf(ingredientId, new ItemStack(item), level, snap, recipes, depth + 1, visiting).values();
                part.forEach((k, v) -> one.merge(k, v, Float::sum));
            }
            if (one.isEmpty()) continue;
            one.forEach((k, v) -> sum.merge(k, v, Float::sum));
            counted++;
        }
        if (sum.isEmpty()) {
            return sum;
        }
        int recipeCount = counted;
        sum.replaceAll((k, v) -> v / recipeCount);
        float scale = snap.settings().recipeScale();
        float max = snap.settings().recipeMax();
        Map<String, Float> out = new LinkedHashMap<>();
        for (MicroDef def : snap.defs().values()) {
            float v = sum.getOrDefault(def.key(), 0f) * scale;
            if (v > 0f) {
                out.put(def.key(), Math.min(v, max));
            }
        }
        return out;
    }

    @Nullable
    private static Map<String, Float> explicitValues(ResourceLocation id, Item item, MicroRegistry.Snapshot snap) {
        Map<String, Float> direct = snap.itemValues().get(id);
        if (direct != null) {
            return known(direct, snap);
        }
        Map<String, Float> merged = new HashMap<>();
        boolean matched = false;
        for (var e : snap.tagValues().entrySet()) {
            if (item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, e.getKey()))) {
                matched = true;
                e.getValue().forEach((k, v) -> merged.merge(k, v, Math::max));
            }
        }
        return matched ? known(merged, snap) : null;
    }

    private static Map<String, Float> groupFallback(ItemStack stack, @Nullable Level level, MicroRegistry.Snapshot snap) {
        Map<String, Float> bars = NutrientClassificationLookup.resolveNutrientBars(stack, false, level);
        float total = 0f;
        for (float w : bars.values()) total += Math.max(0f, w);
        if (total <= 0f) {
            return Map.of();
        }
        Map<String, Float> out = new LinkedHashMap<>();
        for (MicroDef def : snap.defs().values()) {
            float v = 0f;
            for (var g : def.groupWeights().entrySet()) {
                v += Math.max(0f, bars.getOrDefault(g.getKey(), 0f)) / total * g.getValue();
            }
            if (v > 0f) {
                out.put(def.key(), v);
            }
        }
        return out;
    }

    private static Map<String, Float> known(Map<String, Float> values, MicroRegistry.Snapshot snap) {
        Map<String, Float> out = new LinkedHashMap<>();
        values.forEach((k, v) -> {
            if (v > 0f && snap.defs().containsKey(k)) out.put(k, v);
        });
        return out;
    }

    private static RecipeInheritanceResolver recipesFor(RecipeManager manager, boolean clientSide) {
        RecipeSlot slot = clientSide ? clientRecipes : serverRecipes;
        if (slot == null || slot.manager() != manager) {
            slot = new RecipeSlot(manager, new RecipeInheritanceResolver(manager));
            if (clientSide) {
                clientRecipes = slot;
                CLIENT_CACHE.clear();
            } else {
                serverRecipes = slot;
                SERVER_CACHE.clear();
            }
        }
        return slot.resolver();
    }
}
