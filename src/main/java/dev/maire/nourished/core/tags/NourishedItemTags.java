package dev.maire.nourished.core.tags;

import dev.maire.nourished.core.Nourished;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Nourished-owned item tags for raw food severity classification.
 */
public final class NourishedItemTags {

    private NourishedItemTags() {}

    public static TagKey<Item> rawSourceFine() {
        return tag("raw_food/fine");
    }

    public static TagKey<Item> rawSourceMild() {
        return tag("raw_food/mild");
    }

    public static TagKey<Item> rawSourceMedium() {
        return tag("raw_food/medium");
    }

    public static TagKey<Item> rawSourceSevere() {
        return tag("raw_food/severe");
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Nourished.MODID, path));
    }
}
