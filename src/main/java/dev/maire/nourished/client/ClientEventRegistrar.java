package dev.maire.nourished.client;

import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.core.Nourished;
// import dev.maire.nourished.modules.Stamina.HUD.StaminaHUD; // STAMINA_SHELVED
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
        NeoForge.EVENT_BUS.addListener(ClientEvents::onKeyInput);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onRenderGuiPost);
        // NeoForge.EVENT_BUS.addListener(StaminaHUD::onRenderGuiPost); // STAMINA_SHELVED
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onKeyInput);
        NeoForge.EVENT_BUS.addListener(ClientEventRegistrar::onLogout);
        bootstrapCompatPlugins();
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNourishedState.reset();
    }

    private static void bootstrapCompatPlugins() {
        maybeBootstrap("jei", "dev.maire.nourished.compat.jei.NourishedJeiPlugin");
        maybeBootstrap("roughlyenoughitems", "dev.maire.nourished.compat.rei.NourishedReiPlugin");
        maybeBootstrap("emi", "dev.maire.nourished.compat.emi.NourishedEmiPlugin");
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
