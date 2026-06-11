package dev.maire.nourished.core.nutrition;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.compat.ModCompat;
import dev.marie.MariesLib.scanner.RecipeInheritanceResolver;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Food-specific helpers retained after classification migration to
 * {@link dev.marie.MariesLib.runtime.SourceRegistry}.
 */
@ApiStatus.Internal
public final class FoodNutritionRegistry {

    /**
     * Milk buckets consume like food for effects but omit {@link DataComponents#FOOD}. Used for nutrient math everywhere
     * (consumption pipeline, HUD tooltips, JEI helper) so tag-based dairy gains match.
     */
    public static final FoodProperties MILK_BUCKET_FOOD_PROPERTIES = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0f)
            .alwaysEdible()
            .build();

    @Nullable
    private static volatile RecipeManager serverRecipeManager;

    @Nullable
    private static volatile RecipeInheritanceResolver serverRecipeInheritanceResolver;

    private FoodNutritionRegistry() {}

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
     * Called after {@link NutrientRegistry#load()} (and on reload). Kept for API compatibility.
     */
    public static void init() {
        // Classifications now live in SourceRegistry via datapack and MarieAPI.
    }

    /**
     * Nutrient bar weights from datapack {@code nourished:nutrients/*} tags (compat-filtered).
     * Used by tooling that snapshots tag-classified foods.
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
}
