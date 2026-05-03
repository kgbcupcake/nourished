package dev.maire.nourished.client;

import java.util.Locale;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.client.screen.DietScreen;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.nutrition.FoodNutritionRegistry.DietDelta;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Nourished.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ClientEvents {

    private static final ItemStack GOLDEN_APPLE_STACK = new ItemStack(Items.GOLDEN_APPLE);

    private ClientEvents() {}

    private static final class InventoryDietButton extends Button {
        InventoryDietButton(int x, int y) {
            super(x, y, 20, 20, Component.empty(),
                    b -> Minecraft.getInstance().setScreen(new DietScreen()),
                    b -> Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.renderItem(GOLDEN_APPLE_STACK, getX() + 2, getY() + 2);
            if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("nourished.screen.diet.tooltip.nourish"),
                        mouseX, mouseY);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int x = screen.getGuiLeft() - 26;
        int y = screen.getGuiTop() + 60;

        event.addListener(new InventoryDietButton(x, y));
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!NourishedConfig.get().showFoodTooltips()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        if (player == null) {
            player = mc.player;
        }
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) {
            return;
        }

        FoodNutritionRegistry.NutrientValues values = FoodNutritionRegistry.getNutrients(stack, level, true);
        DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                stack, level, values, food.nutrition(), food.saturation());

        final float minLine = 0.5f;
        boolean anyAbove = false;
        for (String key : NutrientRegistry.getKeys()) {
            if (dietValueForKey(delta, key) >= minLine) {
                anyAbove = true;
                break;
            }
        }
        if (!anyAbove) {
            return;
        }

        var lines = event.getToolTip();
        lines.add(Component.empty());
        lines.add(Component.literal("✦ Nourished").withStyle(ChatFormatting.GOLD));

        for (String key : NutrientRegistry.getKeys()) {
            float v = dietValueForKey(delta, key);
            if (v < minLine) {
                continue;
            }
            String label = Character.toUpperCase(key.charAt(0)) + key.substring(1);
            String text = "  " + label + "  +" + String.format(Locale.ROOT, "%.1f", v);
            int color = NutrientUiColors.baseColorArgb(key);
            MutableComponent line = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }
    }

    private static float dietValueForKey(DietDelta delta, String key) {
        return switch (key) {
            case "fruits" -> delta.fruits();
            case "vegetables" -> delta.vegetables();
            case "proteins" -> delta.proteins();
            case "grains" -> delta.grains();
            case "sugars" -> delta.sugars();
            case "dairy" -> delta.dairy();
            default -> 0f;
        };
    }
}
