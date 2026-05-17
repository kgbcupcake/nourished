package dev.maire.nourished.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.api.registry.MilestoneRegistry;
import dev.maire.nourished.api.registry.SynergyRegistry;
import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry.DietDelta;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedItemTags;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Shared food tooltip formatter used by JEI/REI/EMI integrations.
 */
@ApiStatus.Internal
public final class NourishedFoodTooltipHelper {

    private NourishedFoodTooltipHelper() {}

    public static List<Component> getTooltipLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return lines;
        }

        Minecraft mc = Minecraft.getInstance();

        FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(stack, mc.player);
        if (food == null) {
            return lines;
        }
        if (!ModuleCache.enableFoodTooltips) {
            return lines;
        }

        Level level = mc.level;
        Map<String, Float> matchedBars = level != null
                ? FoodNutritionRegistry.resolveNutrientBars(stack, false, level)
                : FoodNutritionRegistry.resolveNutrientBars(stack, false);
        Map<String, Float> external = FoodNutritionRegistry.getExternalClassification(
                NourishedRegistryUtils.itemKey(stack.getItem()));
        if (external != null) {
            Map<String, Float> merged = new LinkedHashMap<>(matchedBars);
            external.forEach((k, v) -> merged.merge(k, v, Float::sum));
            matchedBars = merged;
        }
        DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                stack, level, food.nutrition(), food.saturation(), matchedBars);

        Player player = mc.player;
        String itemId = NourishedRegistryUtils.itemKey(stack).toString();
        String dominantCategory = matchedBars.isEmpty()
                ? null
                : matchedBars.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
        String familyKey = FoodFamilyResolver.resolve(NourishedRegistryUtils.itemKey(stack));
        DietData diet = ClientDietCache.get();
        long gameTimeMs = diet.lastTickTime > 0 ? diet.lastTickTime : 0L;
        float multiplier = player != null ? diet.peekMultiplier(itemId, dominantCategory, familyKey, gameTimeMs) : 1.0f;

        lines.add(Component.literal("✦ Nourished").withStyle(ChatFormatting.GOLD));

        if (matchedBars.isEmpty()) {
            lines.add(Component.translatable("nourished.tooltip.unclassified").withStyle(ChatFormatting.GRAY));
        }

        if (multiplier < 1.0f && player != null) {
            String pct = (int) (multiplier * 100) + "%";
            lines.add(Component.translatable("nourished.tooltip.diminished", pct).withStyle(ChatFormatting.YELLOW));
        } else if (player != null) {
            lines.add(Component.translatable("nourished.tooltip.fresh").withStyle(ChatFormatting.GREEN));
        }

        final float minLine = 0.02f;
        String fmt = multiplier < 1.0f ? "%.2f" : "%.1f";
        String highestKey = null;
        float highestValue = Float.NEGATIVE_INFINITY;
        if (!matchedBars.isEmpty()) {
            for (String key : NutrientRegistry.getKeys()) {
                float v = delta.nutrients().getOrDefault(key, 0f);
                if (v > highestValue) {
                    highestValue = v;
                    highestKey = key;
                }
            }
        }

        NourishedConfig config = NourishedConfig.get();
        boolean renderedAny = false;
        for (String key : NutrientRegistry.getKeys()) {
            float base = delta.nutrients().getOrDefault(key, 0f);
            if (base < minLine) {
                continue;
            }
            float display = base * multiplier;
            renderedAny = true;
            String label = NourishedRegistryUtils.capitalizeFirst(key);
            String gain = String.format(Locale.ROOT, fmt, display);
            int color = computeTooltipColor(key, diet, display, config);
            MutableComponent line = Component.literal("  " + label + "  +" + gain)
                    .withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }

        if (!renderedAny && highestKey != null && highestValue > 0f) {
            float base = Math.max(0f, delta.nutrients().getOrDefault(highestKey, 0f));
            float display = base * multiplier;
            String label = NourishedRegistryUtils.capitalizeFirst(highestKey);
            String gain = String.format(Locale.ROOT, fmt, display);
            int color = computeTooltipColor(highestKey, diet, display, config);
            lines.add(Component.literal("  " + label + "  +" + gain).withStyle(Style.EMPTY.withColor(color)));
        }

        if (ModuleCache.enableDebugLogging && player != null) {
            var breakdown = diet.getMultiplierBreakdown(itemId, dominantCategory, familyKey, gameTimeMs);
            float fin = breakdown.finalMultiplier();
            lines.add(Component.empty());
            lines.add(Component.literal(
                    "  → " + (int) (fin * 100) + "% nutrition gain (memory blend)")
                    .withStyle(fin < 1.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        } else if (NourishedConfig.get().debugMemoryLogging() && player != null) {
            var breakdown = diet.getMultiplierBreakdown(itemId, dominantCategory, familyKey, gameTimeMs);
            lines.add(Component.empty());
            lines.add(Component.literal("  → " + (int) (breakdown.finalMultiplier() * 100) + "% nutrition gain")
                    .withStyle(breakdown.finalMultiplier() < 1.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }

        if (ModuleCache.enableDecay && ModuleCache.enableNutritionEating) {
            boolean bypassEligible =
                    (food.nutrition() <= 2 || stack.is(NourishedItemTags.LIGHT_FOOD)) && !stack.is(NourishedItemTags.MEAL);
            if (bypassEligible) {
                lines.add(Component.translatable("nourished.tooltip.light_food").withStyle(ChatFormatting.GRAY));
            }
        }

        ResourceLocation thisItem = NourishedRegistryUtils.itemKey(stack);
        if (ModuleCache.enableSynergies) {
            addFoodSynergyLine(lines, thisItem);
        }
        if (ModuleCache.enableMilestones && !matchedBars.isEmpty()) {
            addMilestoneLine(lines, matchedBars.keySet());
        }
        return lines;
    }

    private static void addFoodSynergyLine(List<Component> lines, ResourceLocation foodId) {
        List<FoodSynergyDefinition> foodSynergies = SynergyRegistry.getFoodSynergies();
        if (!ModuleCache.enableSynergies || foodSynergies.isEmpty()) {
            return;
        }
        for (FoodSynergyDefinition def : foodSynergies) {
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
        if (!ModuleCache.enableMilestones || MilestoneRegistry.getAll().isEmpty()) {
            return;
        }
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

    private static final int COL_CRITICAL = 0xFFFF5555;
    private static final int COL_WARNING = 0xFFFFAA00;
    private static final int COL_GOOD = 0xFF55FF55;

    private static int computeTooltipColor(String key, DietData diet, float gain, NourishedConfig config) {
        boolean beneficial = NutrientRegistry.isBeneficial(key);
        float current = diet.nutrients.getOrDefault(key, 0f);
        float projected = current + gain;

        if (beneficial) {
            return NutrientUiColors.baseColorArgb(key);
        } else {
            float excess = (float) config.excessThreshold();
            float low = (float) config.lowThreshold();
            if (projected > excess) {
                return COL_CRITICAL;
            } else if (projected > low) {
                return COL_WARNING;
            }
            return COL_GOOD;
        }
    }
}
