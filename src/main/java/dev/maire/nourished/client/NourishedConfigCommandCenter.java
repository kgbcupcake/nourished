package dev.maire.nourished.client;

import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.commandcenter.CommandCenterCard;
import dev.marie.framework.ui.commandcenter.CommandCenterCategory;
import dev.marie.framework.ui.commandcenter.CommandCenterRegistry;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.client.screen.settings.NourishedSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Command Center "Config" tab: opens the Nourished settings screens from one place. */
final class NourishedConfigCommandCenter {

    private static final String CATEGORY = "nourished.config";
    private static final String L = "config.nourished.commandCenter.";

    private NourishedConfigCommandCenter() {}

    static void register() {
        CommandCenterRegistry.registerCategory(new CommandCenterCategory(
                CATEGORY, Component.translatable("config.nourished.category.config"), 0));
        card("settings", "settings", NourishedColors.CC_CONFIG_SETTINGS,
                () -> Minecraft.getInstance().setScreen(new NourishedSettingsScreen()));
        card("full", "fullConfig", NourishedColors.CC_CONFIG_FULL,
                () -> Minecraft.getInstance().setScreen(NourishedConfigScreen.create(Minecraft.getInstance().screen)));
        card("diet_editor", "dietEditor", NourishedColors.CC_CONFIG_DIET_EDITOR,
                ClientEvents::openOrToggleScaleConfig);
    }

    private static void card(String id, String langKey, ColorKey accent, Runnable action) {
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                CATEGORY + "." + id,
                CATEGORY,
                Component.translatable(L + langKey),
                Component.translatable(L + langKey + ".desc"),
                () -> MarieColors.resolveColor(accent),
                action));
    }
}
