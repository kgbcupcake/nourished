package dev.maire.nourished.client;

import dev.maire.nourished.client.screen.diet.DietScreen;
import dev.maire.nourished.client.screen.diet.classic.ClassicDietScreen;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.compat.MarieTooltipHelper;
import dev.marie.framework.config.FeatureFlagCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientEvents {

    private static final ItemStack DIET_BUTTON_ICON = new ItemStack(Items.GOLDEN_APPLE);

    private ClientEvents() {}

    private static final class InventoryDietButton extends Button {
        InventoryDietButton(int x, int y) {
            super(x, y, 20, 20, Component.empty(),
                    b -> Minecraft.getInstance().setScreen(openDietScreen()),
                    b -> Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.renderItem(DIET_BUTTON_ICON, getX() + 2, getY() + 2);
            if (isHovered()) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("nourished.screen.diet.tooltip.nourish"),
                        mouseX, mouseY);
            }
        }
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!FeatureFlagCache.enableTrackingScreen()) return;
        // Only gates the button's own presence — deliberately does NOT touch
        // NourishedKeys.OPEN_DIET_SCREEN's handling in onClientTick below, so the keybind keeps
        // opening the Diet Screen regardless of this setting.
        if (!NourishedClientConfig.get().showDietScreenButton()) return;

        int x = screen.getGuiLeft() - 26;
        int y = screen.getGuiTop() + 60;

        event.addListener(new InventoryDietButton(x, y));
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        var nourishedLines = MarieTooltipHelper.getTooltipLines(stack);
        if (nourishedLines.isEmpty()) {
            return;
        }
        var lines = event.getToolTip();
        lines.add(Component.empty());
        lines.addAll(nourishedLines);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (NourishedKeys.OPEN_DIET_SCREEN.consumeClick()) {
            if (!FeatureFlagCache.enableTrackingScreen()) {
                continue;
            }
            mc.setScreen(openDietScreen());
        }
    }

    private static Screen openDietScreen() {
        return NourishedClientConfig.get().dietScreenClassicMode() ? new ClassicDietScreen() : new DietScreen();
    }
}
