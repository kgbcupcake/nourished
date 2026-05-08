package dev.maire.nourished.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.maire.nourished.compat.NourishedFoodTooltipHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class NourishedEmiPlugin implements EmiPlugin {

    public static void bootstrap() {
        // Intentionally empty. Called reflectively from client init to keep optional class loading safe.
    }

    @Override
    public void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            FoodProperties food = item.components().get(net.minecraft.core.component.DataComponents.FOOD);
            if (food == null) {
                continue;
            }
            // EMI uses comparison providers for stack metadata behavior.
            // Include Nourished tooltip hash in comparison so tooltip-sensitive stacks are treated consistently.
            int tooltipHash = NourishedFoodTooltipHelper.getTooltipLines(stack).hashCode();
            if (tooltipHash != 0) {
                registry.setDefaultComparison(item, previous -> Comparison.compareComponents());
            }
        }
    }
}
