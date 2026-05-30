package dev.maire.nourished.core.util;

import dev.maire.nourished.core.Nourished;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Shared item tags used by client and server logic.
 */
public final class NourishedItemTags {

    public static final TagKey<Item> MEAL =
            NourishedRegistryUtils.itemTagKey(Nourished.MODID + ":meal");
    public static final TagKey<Item> LIGHT_FOOD =
            NourishedRegistryUtils.itemTagKey(Nourished.MODID + ":light_food");
    public static final TagKey<Item> RAW_FOOD_FINE =
            NourishedRegistryUtils.itemTagKey(Nourished.MODID + ":raw_food/fine");
    public static final TagKey<Item> RAW_FOOD_MILD =
            NourishedRegistryUtils.itemTagKey(Nourished.MODID + ":raw_food/mild");
    public static final TagKey<Item> RAW_FOOD_MEDIUM =
            NourishedRegistryUtils.itemTagKey(Nourished.MODID + ":raw_food/medium");
    public static final TagKey<Item> RAW_FOOD_SEVERE =
            NourishedRegistryUtils.itemTagKey(Nourished.MODID + ":raw_food/severe");

    private NourishedItemTags() {}
}
