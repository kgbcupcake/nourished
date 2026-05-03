package dev.maire.nourished.nutrition;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Food nutrient values and diet-bar classification are driven only by datapack item tags under
 * {@code data/nourished/tags/item/nutrients/} (see {@code nourished:nutrients/*}).
 */
public class FoodNutritionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> WARNED_ITEMS = new HashSet<>();

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    /** Diet UI deltas; primary bar comes from {@code nourished:nutrients/*} item tags. */
    public record DietDelta(float calories, float fruits, float vegetables, float proteins, float grains, float sugars, float dairy) {}

    private static final TagKey<Item> NUTRIENT_FRUITS = TagKey.create(Registries.ITEM, ResourceLocation.parse("nourished:nutrients/fruits"));
    private static final TagKey<Item> NUTRIENT_VEGETABLES = TagKey.create(Registries.ITEM, ResourceLocation.parse("nourished:nutrients/vegetables"));
    private static final TagKey<Item> NUTRIENT_PROTEINS = TagKey.create(Registries.ITEM, ResourceLocation.parse("nourished:nutrients/proteins"));
    private static final TagKey<Item> NUTRIENT_GRAINS = TagKey.create(Registries.ITEM, ResourceLocation.parse("nourished:nutrients/grains"));
    private static final TagKey<Item> NUTRIENT_SUGARS = TagKey.create(Registries.ITEM, ResourceLocation.parse("nourished:nutrients/sugars"));
    private static final TagKey<Item> NUTRIENT_DAIRY = TagKey.create(Registries.ITEM, ResourceLocation.parse("nourished:nutrients/dairy"));

    /**
     * Called after {@link NutrientRegistry#load()} (and on reload). Classification uses only datapack tags;
     * nothing is rebuilt here.
     */
    public static void init() {
        // Intentionally empty — kept for API compatibility with {@link NutrientRegistry#reload()}.
    }

    /**
     * Resolves the primary diet nutrient key for an item stack using {@code nourished:nutrients/*} tags.
     * Priority: fruits, vegetables, proteins, grains, sugars, dairy. If none match, returns {@code grains}.
     *
     * @param warnIfUnmatched when true, logs a WARN for modpack authors when defaulting to grains
     */
    public static String resolveNutrientBar(ItemStack stack, boolean warnIfUnmatched) {
        var holder = stack.getItemHolder();
        if (holder.is(NUTRIENT_FRUITS)) {
            return "fruits";
        }
        if (holder.is(NUTRIENT_VEGETABLES)) {
            return "vegetables";
        }
        if (holder.is(NUTRIENT_PROTEINS)) {
            return "proteins";
        }
        if (holder.is(NUTRIENT_GRAINS)) {
            return "grains";
        }
        if (holder.is(NUTRIENT_SUGARS)) {
            return "sugars";
        }
        if (holder.is(NUTRIENT_DAIRY)) {
            return "dairy";
        }

        Item item = stack.getItem();
        if (warnIfUnmatched) {
            String id = item.getDescriptionId();
            if (WARNED_ITEMS.add(id)) {
                LOGGER.warn(
                        "Nourished: no nutrient tag for {} — defaulting to grains. Add it to data/nourished/tags/item/nutrients/*.json",
                        id);
            }
        }
        return "grains";
    }

    /**
     * @param silent when {@code true}, skips {@link #resolveNutrientBar} unmatched-item warnings (use on client
     *               tooltips / JEI where tags may not be committed yet).
     */
    public static NutrientValues getNutrients(ItemStack stack, Level level, boolean silent) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        String bar = resolveNutrientBar(stack, !silent);
        float totalPoints = Math.max(1.0f, food.nutrition() + Math.max(0f, food.saturation()) * 0.5f);
        return nutrientValuesForBar(bar, totalPoints);
    }

    /** Same as {@link #getNutrients(ItemStack, Level, boolean)} with {@code silent == false}. */
    public static NutrientValues getNutrients(ItemStack stack, Level level) {
        return getNutrients(stack, level, false);
    }

    /** NutrientValues with primary macro weighting for the given diet bar. */
    private static NutrientValues nutrientValuesForBar(String barKey, float pts) {
        return switch (barKey) {
            case "fruits" -> new NutrientValues(0, pts * 0.4f, 0, pts * 0.8f, pts);
            case "vegetables" -> new NutrientValues(pts * 0.1f, pts * 0.35f, 0, pts, pts * 0.2f);
            case "proteins" -> new NutrientValues(pts, pts * 0.1f, pts * 0.3f, 0, 0);
            case "grains" -> new NutrientValues(pts * 0.2f, pts, pts * 0.5f, pts * 0.1f, 0);
            case "sugars" -> new NutrientValues(0, pts * 0.8f, pts * 0.15f, 0, 0);
            case "dairy" -> new NutrientValues(pts * 0.5f, 0, pts * 0.7f, 0, pts * 0.3f);
            default -> new NutrientValues(pts * 0.2f, pts, pts * 0.1f, pts * 0.1f, 0);
        };
    }

    public static DietDelta computeDietDelta(ItemStack stack, Level level, NutrientValues values, int foodNutrition, float foodSaturation) {
        Objects.requireNonNull(level, "level");
        // Calories follow hunger restored only (FoodProperties#nutrition), scaled to the ~2000 daily UI cap.
        // Do not add saturation or macro-derived carbs here — those terms double-counted magnitude and
        // exploded for high-saturation or modded foods (saturation * 12 alone could reach five digits).
        int calories = Math.max(0, Math.round(foodNutrition * 25f));
        String primary = resolveDietBarKey(stack);
        float burst = foodNutrition * 0.003f + foodSaturation * 0.004f + 0.001f;

        float fruits = values.hydration() * 0.00065f + values.vitamins() * 0.00035f;
        float vegetables = values.vitamins() * 0.001f;
        float proteins = values.protein() * 0.00135f;
        float grains = values.carbs() * 0.00085f;
        float sugars = values.fats() * 0.0005f + values.carbs() * 0.00065f;
        float dairy = values.fats() * 0.00065f + values.protein() * 0.00035f;

        switch (primary) {
            case "fruits" -> fruits += burst;
            case "vegetables" -> vegetables += burst;
            case "proteins" -> proteins += burst;
            case "grains" -> grains += burst;
            case "sugars" -> sugars += burst;
            case "dairy" -> dairy += burst;
            default -> grains += burst * 0.35f;
        }

        final float scale = 10f;
        return new DietDelta(
                calories,
                fruits * scale,
                vegetables * scale,
                proteins * scale,
                grains * scale,
                sugars * scale,
                dairy * scale);
    }

    private static String resolveDietBarKey(ItemStack stack) {
        // Same tag priority as getNutrients; do not warn again (getNutrients already warned if applicable).
        return resolveNutrientBar(stack, false);
    }
}
