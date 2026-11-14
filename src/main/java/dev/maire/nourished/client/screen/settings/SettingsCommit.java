package dev.maire.nourished.client.screen.settings;

import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Set;

/** Shared save, lock and default helpers for the settings pages. */
final class SettingsCommit {

    // These only change what this client shows, so they stay editable on a remote server.
    private static final Set<String> CLIENT_SIDE = Set.of(
            "enableHUD", "enableToasts", "enableCriticalToasts", "enableFoodTooltips");

    private SettingsCommit() {}

    static void commit() {
        NourishedConfig.get().saveNow();
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            NourishedReloadHelper.reloadAndBroadcast(server);
        }
    }

    static boolean hidden(String key) {
        return NourishedLockRegistry.isLocked(key);
    }

    static boolean editable(String key) {
        if (!NourishedConfigSharedWidgets.isMultiplayer()) {
            return true;
        }
        return CLIENT_SIDE.contains(key) && !NourishedLockRegistry.isServerOnly(key);
    }

    static double number(String path, double fallback) {
        return NourishedConfig.defaultFor(path) instanceof Number n ? n.doubleValue() : fallback;
    }

    static boolean flag(String path, boolean fallback) {
        return NourishedConfig.defaultFor(path) instanceof Boolean b ? b : fallback;
    }

    static String text(String path, String fallback) {
        return NourishedConfig.defaultFor(path) instanceof String s ? s : fallback;
    }

    static String t(String key) {
        return Component.translatable(key).getString();
    }
}
