package dev.maire.nourished.core.tagaudit;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tagaudit.model.TagAuditContext;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Nourished's TagAuditContext implementation — exposes real nutrient tag
 * contents, mod namespaces, and live classification inference to TagRules
 * registered against Nourished's nutrient category system.
 */
@ApiStatus.Internal
public final class NourishedTagAuditContext implements TagAuditContext {

    private static final NourishedTagAuditContext INSTANCE = new NourishedTagAuditContext();

    public static NourishedTagAuditContext get() {
        return INSTANCE;
    }

    private NourishedTagAuditContext() {}

    @Override
    public Set<String> knownCategories() {
        return new LinkedHashSet<>(NutrientRegistry.getKeys());
    }

    @Override
    public Set<ResourceLocation> itemsInCategory(String category) {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        TagKey<Item> tag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "nutrients/" + category));
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.builtInRegistryHolder().is(tag)) {
                result.add(item.builtInRegistryHolder().key().location());
            }
        }
        return result;
    }

    @Override
    public Set<String> categoriesForItem(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return Set.of();
        }
        // getNutrientTagScores applies compat-toggle filtering (isTagCompatEnabled per modid),
        // so this reflects compat-filtered tag truth, not raw datapack tag truth. Rule authors
        // querying categoriesForItem should be aware results may exclude items whose mod's compat
        // is disabled in config, even if the datapack tag includes them.
        Map<String, Float> tagScores = FoodNutritionRegistry.getNutrientTagScores(item);
        return new LinkedHashSet<>(tagScores.keySet());
    }

    @Override
    @Nullable
    public Function<ResourceLocation, Map<String, Float>> liveInferenceLookup() {
        // Calls RuntimeFoodResolver.resolve() directly, bypassing NutrientClassificationLookup.resolveNutrientBars.
        // resolveNutrientBars short-circuits on SourceRegistry.getExternalClassification, which holds
        // tag-derived classifications registered by NutrientRegistry.registerClassificationsFromTags —
        // so calling it here would echo the tag back rather than providing independent inference,
        // making tag-vs-inference comparison circular and useless. RuntimeFoodResolver.resolve()
        // has no SourceRegistry access path: it only checks its own LRU cache and the keyword/recipe/
        // peer/fallback cascade, so results are genuinely independent of the datapack tag data.
        return itemId -> {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null) {
                return Map.of();
            }
            ItemStack stack = new ItemStack(item);
            return RuntimeFoodResolver.getInstance().resolve(stack, null);
        };
    }

    @Override
    public Set<String> namespacesPresent() {
        Set<String> namespaces = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            namespaces.add(item.builtInRegistryHolder().key().location().getNamespace());
        }
        return namespaces;
    }
}
