package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.NourishedConfigScreen.FoodValuePending;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class FoodValuesCategory {
    private FoodValuesCategory() {}
    public static void addFoodValuesCategory(ConfigBuilder builder, ConfigEntryBuilder eb, Map<String, FoodValuePending> foodValuePending) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.food_values"));

        for (FoodValueRegistry.FoodValueDef def : FoodValueRegistry.getAll()) {
            String categoryKey = def.category();
            FoodValuePending pending = new FoodValuePending(
                    def.protein(), def.carbs(), def.fats(), def.vitamins(), def.hydration()
            );
            foodValuePending.put(categoryKey, pending);

            List<AbstractConfigListEntry> entries = new ArrayList<>();
            entries.add(buildFloatSlider(eb, Component.literal("Protein"), pending.protein.get(), 0f, 2f, def.protein(), pending.protein::set));
            entries.add(buildFloatSlider(eb, Component.literal("Carbs"), pending.carbs.get(), 0f, 2f, def.carbs(), pending.carbs::set));
            entries.add(buildFloatSlider(eb, Component.literal("Fats"), pending.fats.get(), 0f, 2f, def.fats(), pending.fats::set));
            entries.add(buildFloatSlider(eb, Component.literal("Vitamins"), pending.vitamins.get(), 0f, 2f, def.vitamins(), pending.vitamins::set));
            entries.add(buildFloatSlider(eb, Component.literal("Hydration"), pending.hydration.get(), 0f, 2f, def.hydration(), pending.hydration::set));

            category.addEntry(
                    eb.startSubCategory(Component.literal(prettyKey(categoryKey)), entries)
                            .setExpanded(false)
                            .build()
            );
        }

        addReloadButton(category, eb, false);
    }
}
