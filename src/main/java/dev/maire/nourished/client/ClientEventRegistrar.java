package dev.maire.nourished.client;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.client.config.state.MarieClientState;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.client.screen.diet.dynamic.edit.DietModuleResetCommand;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityLogClientBuffer;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityLogHudPanel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientEventRegistrar {

    private ClientEventRegistrar() {}

    public static void register(IEventBus modEventBus) {
        DietScreenModules.registerAll();
        modEventBus.addListener(NourishedKeys::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onClientTick);
        NeoForge.EVENT_BUS.addListener(ActivityLogHudPanel::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(ActivityLogHudPanel::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEventRegistrar::onLogout);
        // TEMPORARY dev-only command — see DietModuleResetCommand's own javadoc for removal instructions.
        NeoForge.EVENT_BUS.addListener(DietModuleResetCommand::register);
    }

    /** Reset sync state to UNINITIALIZED on disconnect. */
    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MarieClientState.reset();
        MarieClientCache.resetDiagnostics();
        NourishedClientMemoryConfig.resetClientMemoryDiagnostics();
        ActivityLogClientBuffer.reset();
    }
}
