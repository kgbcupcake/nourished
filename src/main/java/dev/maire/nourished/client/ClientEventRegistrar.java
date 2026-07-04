package dev.maire.nourished.client;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.client.MarieClientState;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import net.neoforged.bus.api.IEventBus;
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
    }

    /** Reset sync state to UNINITIALIZED on disconnect. */
    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MarieClientState.reset();
        MarieClientCache.resetDiagnostics();
        NourishedClientMemoryConfig.resetClientMemoryDiagnostics();
    }
}
