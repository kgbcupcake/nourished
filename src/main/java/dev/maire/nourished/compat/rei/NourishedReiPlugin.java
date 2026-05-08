package dev.maire.nourished.compat.rei;

import dev.maire.nourished.compat.NourishedFoodTooltipHelper;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class NourishedReiPlugin implements REIClientPlugin {

    public static void bootstrap() {
        // Intentionally empty. Called reflectively from client init to keep optional class loading safe.
    }

    @Override
    public void registerEntryRenderers(EntryRendererRegistry registry) {
        if (!ModList.get().isLoaded("roughlyenoughitems")) {
            return;
        }
        registry.transformTooltip(VanillaEntryTypes.ITEM, (stack, point, tooltip) -> {
            ItemStack itemStack = stack.getValue();
            if (itemStack.isEmpty()) {
                return tooltip;
            }
            FoodProperties food = itemStack.getItem().components().get(net.minecraft.core.component.DataComponents.FOOD);
            if (food == null) {
                return tooltip;
            }
            tooltip.addAllTexts(NourishedFoodTooltipHelper.getTooltipLines(itemStack));
            return tooltip;
        });
    }
}
