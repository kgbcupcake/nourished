package dev.maire.nourished.client;

import java.util.Locale;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.client.screen.DietScreen;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedConfigScreen;
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
import net.neoforged.neoforge.client.event.InputEvent;
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
        if (!NourishedConfig.get().enableDietScreen()) return;

        int x = screen.getGuiLeft() - 26;
        int y = screen.getGuiTop() + 60;

        event.addListener(new InventoryDietButton(x, y));
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!NourishedConfig.get().enableFoodTooltips()) {
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

        var matchedBars = FoodNutritionRegistry.resolveNutrientBars(stack, true);
        DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                stack, level, food.nutrition(), food.saturation(), matchedBars);

        final float minLine = 0.05f;
        String highestKey = null;
        float highestValue = Float.NEGATIVE_INFINITY;
        for (String key : NutrientRegistry.getKeys()) {
            float v = delta.nutrients().getOrDefault(key, 0f);
            if (v > highestValue) {
                highestValue = v;
                highestKey = key;
            }
        }

        var lines = event.getToolTip();
        lines.add(Component.empty());
        lines.add(Component.literal("✦ Nourished").withStyle(ChatFormatting.GOLD));

        boolean renderedAny = false;
        for (String key : NutrientRegistry.getKeys()) {
            float v = delta.nutrients().getOrDefault(key, 0f);
            if (v < minLine) {
                continue;
            }
            renderedAny = true;
            String label = Character.toUpperCase(key.charAt(0)) + key.substring(1);
            String text = "  " + label + "  +" + String.format(Locale.ROOT, "%.1f", v);
            int color = NutrientUiColors.baseColorArgb(key);
            MutableComponent line = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }

        if (!renderedAny && highestKey != null) {
            float v = Math.max(0f, delta.nutrients().getOrDefault(highestKey, 0f));
            String label = Character.toUpperCase(highestKey.charAt(0)) + highestKey.substring(1);
            String text = "  " + label + "  +" + String.format(Locale.ROOT, "%.1f", v);
            int color = NutrientUiColors.baseColorArgb(highestKey);
            MutableComponent line = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (NourishedKeys.OPEN_CONFIG.consumeClick()) {
            mc.setScreen(NourishedConfigScreen.create(null));
        }
    }
}
