package dev.maire.nourished.client;

import dev.marie.MariesLib.client.MarieClientCache;
import dev.marie.MariesLib.client.MarieClientState;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientEventRegistrar {

    private ClientEventRegistrar() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NourishedKeys::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEventRegistrar::onLogout);
        bootstrapCompatPlugins();
    }

    /** Reset sync state to UNINITIALIZED on disconnect. */
    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MarieClientState.reset();
        MarieClientCache.resetDiagnostics();
        NourishedClientMemoryConfig.resetClientMemoryDiagnostics();
    }

    private static void bootstrapCompatPlugins() {
        maybeBootstrap("jei", "dev.marie.MariesLib.compat.jei.MarieJeiPlugin");
        maybeBootstrap("roughlyenoughitems", "dev.marie.MariesLib.compat.rei.MarieReiPlugin");
        maybeBootstrap("emi", "dev.marie.MariesLib.compat.emi.MarieEmiPlugin");
    }

    private static void maybeBootstrap(String modId, String className) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        try {
            Class<?> type = Class.forName(className);
            type.getMethod("bootstrap").invoke(null);
            Nourished.LOGGER.info("[Nourished] Enabled {} compatibility plugin", modId);
        } catch (Throwable t) {
            Nourished.LOGGER.warn("[Nourished] Failed to initialize {} compatibility plugin", modId, t);
        }
    }
}
