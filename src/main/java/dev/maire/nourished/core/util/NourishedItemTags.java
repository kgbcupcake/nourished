package dev.maire.nourished.core.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Shared item tags used by client and server logic.
 */
public final class NourishedItemTags {

    public static final TagKey<Item> MEAL =
            NourishedRegistryUtils.itemTagKey("nourished:meal");
    public static final TagKey<Item> LIGHT_FOOD =
            NourishedRegistryUtils.itemTagKey("nourished:light_food");

    private NourishedItemTags() {}
}
