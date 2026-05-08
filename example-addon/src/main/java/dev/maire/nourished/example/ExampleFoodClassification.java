package dev.maire.nourished.example;

import dev.maire.nourished.api.NourishedAPI;
import net.minecraft.resources.ResourceLocation;

/**
 * Demonstrates mapping concrete items to a custom nutrient through food classification registration.
 */
public final class ExampleFoodClassification {

    private ExampleFoodClassification() {}

    public static void register() {
        NourishedAPI.registerFoodClassification(ResourceLocation.parse("minecraft:apple"), "fiber", 0.35f);
        NourishedAPI.registerFoodClassification(ResourceLocation.parse("minecraft:carrot"), "fiber", 0.30f);
        NourishedAPI.registerFoodClassification(ResourceLocation.parse("minecraft:potato"), "fiber", 0.20f);
        NourishedAPI.registerFoodClassification(ResourceLocation.parse("minecraft:beetroot"), "fiber", 0.40f);
        NourishedAPI.registerFoodClassification(ResourceLocation.parse("minecraft:melon_slice"), "fiber", 0.25f);
    }
}
