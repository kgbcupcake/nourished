package dev.maire.nourished.client;

import dev.maire.nourished.client.hud.NourishedHUD;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientEventRegistrar {

    private ClientEventRegistrar() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NourishedKeys::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onKeyInput);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onKeyInput);
    }
}
