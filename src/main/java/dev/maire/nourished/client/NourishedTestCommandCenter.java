package dev.maire.nourished.client;

import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.commandcenter.CommandCenterCard;
import dev.marie.framework.ui.commandcenter.CommandCenterCategory;
import dev.marie.framework.ui.commandcenter.CommandCenterRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Command Center "Nourished Test" tab: one-click checks for NourishedRegisterEvent. */
@ApiStatus.Internal
public final class NourishedTestCommandCenter {

    private static final String CATEGORY = "nourished.test";

    private NourishedTestCommandCenter() {}

    public static void register() {
        CommandCenterRegistry.registerCategory(new CommandCenterCategory(
                CATEGORY, Component.translatable("config.nourished.category.test"), 2));
        card("registrations", "config.nourished.commandCenter.testRegistrations",
                "config.nourished.commandCenter.testRegistrations.desc",
                NourishedColors.CC_TEST_REGISTRATIONS, "nourished debug registrations");
        card("held_food", "config.nourished.commandCenter.testHeldFood",
                "config.nourished.commandCenter.testHeldFood.desc",
                NourishedColors.CC_TEST_HELD_FOOD, "nourished debug food");
    }

    private static void card(String id, String titleKey, String subtitleKey, ColorKey accent, String command) {
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                CATEGORY + "." + id,
                CATEGORY,
                Component.translatable(titleKey),
                Component.translatable(subtitleKey),
                () -> MarieColors.resolveColor(accent),
                () -> dispatch(command)));
    }

    private static void dispatch(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand(command);
        }
    }
}
