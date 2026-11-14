package dev.maire.nourished.client.micro;

import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.screen.diet.DietScreen;
import dev.maire.nourished.client.screen.diet.dynamic.modules.MicroNutrientsComponent;
import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Client hooks for the nutrient detail screen and its history. */
public final class MicroClientEvents {

    private MicroClientEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MicroClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(MicroClientEvents::onMouseClicked);
        NeoForge.EVENT_BUS.addListener(MicroClientEvents::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(MicroClientEvents::onUseItemFinish);
        NeoForge.EVENT_BUS.addListener(MicroClientEvents::onLogout);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (NourishedKeys.OPEN_NUTRIENTS.consumeClick()) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new NutrientDetailScreen(null, 0));
        }
    }

    private static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !(event.getScreen() instanceof DietScreen diet)) return;
        int row = MicroNutrientsComponent.rowAt(event.getMouseX(), event.getMouseY());
        if (row < 0) return;
        Minecraft.getInstance().setScreen(new NutrientDetailScreen(diet, row));
        event.setCanceled(true);
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof DietScreen) MicroNutrientsComponent.forgetBounds();
    }

    private static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player || !player.level().isClientSide()) return;
        if (event.getItem().getFoodProperties(player) == null) return;
        MicroClientHistory.recordMeal(event.getItem(), MicroProfileResolver.resolve(event.getItem(), player.level()),
                player.level().getGameTime());
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MicroRegistry.clearClientSnapshot();
        MicroClientHistory.clear();
        MicroNutrientsComponent.forgetBounds();
    }

    /** Called when the server sends new levels. */
    public static void onLevels(Player player) {
        MicroClientHistory.recordLevels(MicroAttachment.get(player));
    }
}
