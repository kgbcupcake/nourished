package dev.maire.nourished.compat;

import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.api.registry.MilestoneRegistry;
import dev.maire.nourished.api.registry.SynergyRegistry;
import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.nutrition.FoodNutritionRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared food tooltip formatter used by JEI/REI/EMI integrations.
 */
public final class NourishedFoodTooltipHelper {

    private NourishedFoodTooltipHelper() {}

    public static List<Component> getTooltipLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return lines;
        }

        Item item = stack.getItem();
        FoodProperties food = item.components().get(net.minecraft.core.component.DataComponents.FOOD);
        if (food == null) {
            return lines;
        }

        Map<String, Float> matchedBars = FoodNutritionRegistry.resolveNutrientBars(stack, false);
        Map<String, Float> nutrients = new java.util.LinkedHashMap<>();
        float matchedWeightTotal = 0f;
        for (float weight : matchedBars.values()) {
            matchedWeightTotal += Math.max(0f, weight);
        }
        if (matchedWeightTotal <= 0f) {
            matchedWeightTotal = 1f;
        }
        float burst = food.nutrition() * 0.008f + food.saturation() * 0.010f + 0.004f;
        float totalBurst = burst * matchedWeightTotal;
        for (Map.Entry<String, Float> entry : matchedBars.entrySet()) {
            float weight = Math.max(0f, entry.getValue());
            if (weight <= 0f) {
                continue;
            }
            nutrients.put(entry.getKey(), totalBurst * (weight / matchedWeightTotal));
        }

        lines.add(Component.literal("✦ Nourished").withStyle(ChatFormatting.GOLD));

        for (Map.Entry<String, Float> entry : nutrients.entrySet()) {
            float value = entry.getValue();
            if (value <= 0f) {
                continue;
            }
            String key = entry.getKey();
            String nutrientName = Character.toUpperCase(key.charAt(0)) + key.substring(1);
            int color = NutrientUiColors.baseColorArgb(key);
            MutableComponent line = Component.literal(nutrientName + ": +" + String.format(Locale.ROOT, "%.2f", value))
                    .withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }

        ResourceLocation thisItem = BuiltInRegistries.ITEM.getKey(item);
        addFoodSynergyLine(lines, thisItem);
        addMilestoneLine(lines, nutrients.keySet());
        return lines;
    }

    private static void addFoodSynergyLine(List<Component> lines, ResourceLocation foodId) {
        for (FoodSynergyDefinition def : SynergyRegistry.getFoodSynergies()) {
            ResourceLocation partner = null;
            if (foodId.equals(def.getFoodA())) {
                partner = def.getFoodB();
            } else if (foodId.equals(def.getFoodB())) {
                partner = def.getFoodA();
            }
            if (partner == null) {
                continue;
            }

            Item partnerItem = BuiltInRegistries.ITEM.get(partner);
            Component partnerName = new ItemStack(partnerItem).getHoverName();
            lines.add(Component.literal("✦ Pairs with: ").withStyle(ChatFormatting.AQUA).append(partnerName.copy().withStyle(ChatFormatting.AQUA)));
            return;
        }
    }

    private static void addMilestoneLine(List<Component> lines, Set<String> nutrientsInTooltip) {
        Set<String> nutrientSet = new LinkedHashSet<>(nutrientsInTooltip);
        for (String nutrient : nutrientSet) {
            List<NutrientMilestoneDefinition> milestones = MilestoneRegistry.getForNutrient(nutrient);
            if (!milestones.isEmpty()) {
                NutrientMilestoneDefinition milestone = milestones.getFirst();
                String name = milestone.getId().replace('_', ' ');
                lines.add(Component.literal("✦ Counts toward: " + name).withStyle(ChatFormatting.YELLOW));
                return;
            }
        }
    }
}
