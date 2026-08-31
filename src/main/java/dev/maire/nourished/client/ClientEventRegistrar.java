package dev.maire.nourished.client;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.client.config.state.MarieClientState;
import dev.marie.framework.ui.api.MarieNotifications;
import dev.marie.framework.ui.commandcenter.CommandCenterCard;
import dev.marie.framework.ui.commandcenter.CommandCenterCategory;
import dev.marie.framework.ui.commandcenter.CommandCenterRegistry;
import dev.marie.framework.ui.api.EditModeCoordinator;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.client.hud.caloriehistory.CalorieHudScreen;
import dev.maire.nourished.client.NourishedClientMemoryConfig;
import dev.maire.nourished.client.screen.diet.dynamic.edit.DietModuleResetCommand;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityLogClientBuffer;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityLogHudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientEventRegistrar {

    private static final String COMMAND_CENTER_TOOLS_CATEGORY = "nourished.tools";
    private static final String COMMAND_CENTER_FRAMEWORK_CATEGORY = "nourished.framework";

    private ClientEventRegistrar() {}

    public static void register(IEventBus modEventBus) {
        DietScreenModules.registerAll();
        registerCommandCenter();
        MarieNotifications.registerClientListeners();
        EditModeCoordinator.registerGroupCapable(
                "nourished.hud",
                NourishedHUD::editTarget,
                "Drag the HUD panel to reposition, drag the corner handle to resize. H or Esc to exit.",
                NourishedKeys.EDIT_HUD.getKey().getValue(),
                NourishedHUD::showScaleConfigOnGroupEntry
        );
        EditModeCoordinator.registerGroupCapable(
                "nourished.calorieHud",
                CalorieHudScreen::instance,
                "Drag the Calorie History HUD to reposition, drag the corner handle to resize. C or Esc to exit.",
                NourishedKeys.EDIT_CALORIE_HUD.getKey().getValue(),
                CalorieHudScreen::showScaleConfigOnGroupEntry
        );
        EditModeCoordinator.registerGroupCapable(
                "nourished.activityLogHud",
                ActivityLogHudPanel::instance,
                "Drag the Activity Log HUD to reposition, drag the corner handle to resize. K or Esc to exit.",
                NourishedKeys.EDIT_ACTIVITY_LOG_HUD.getKey().getValue(),
                ActivityLogHudPanel::showScaleConfigOnGroupEntry
        );
        modEventBus.addListener(NourishedKeys::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(NourishedHUD::onClientTick);
        NeoForge.EVENT_BUS.addListener(ActivityLogHudPanel::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(ActivityLogHudPanel::onClientTick);
        NeoForge.EVENT_BUS.addListener(CalorieHudScreen::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(CalorieHudScreen::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEventRegistrar::onLogout);
        // TEMPORARY dev-only command — see DietModuleResetCommand's own javadoc for removal instructions.
        NeoForge.EVENT_BUS.addListener(DietModuleResetCommand::register);
    }

    /**
     * Command Center is a command launcher only now — every card here dispatches a real server
     * command through {@link #dispatchCommand(String)} (the same network path a typed chat command
     * takes) instead of driving client-side UI/settings shortcuts directly. The old "Diet Screen"
     * category (scale-panel toggle) was removed: the scale panel is reached only via edit mode now.
     */
    private static void registerCommandCenter() {
        CommandCenterRegistry.registerCategory(new CommandCenterCategory(
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.category.tools"),
                0
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.export_all",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.exportAll"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished export_all")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.audit_tags",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.auditTags"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished audit_tags")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.debug_activitylog",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.debugActivityLog"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished debug activitylog")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.reload",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.reload"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished reload")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.invalidate_cache",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.invalidateCache"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished invalidatecache")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.unassigned_sources",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.unassignedSources"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished get_unassigned")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.nbt_paths",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.nbtPaths"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished nbt")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.profile_list",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.listProfiles"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished profile list")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.tools.my_profile",
                COMMAND_CENTER_TOOLS_CATEGORY,
                Component.translatable("config.nourished.commandCenter.myProfile"),
                null,
                0xFF5DA9E9,
                () -> dispatchCommand("nourished profile get")
        ));

        CommandCenterRegistry.registerCategory(new CommandCenterCategory(
                COMMAND_CENTER_FRAMEWORK_CATEGORY,
                Component.translatable("config.nourished.category.framework"),
                1
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.framework.status",
                COMMAND_CENTER_FRAMEWORK_CATEGORY,
                Component.translatable("config.nourished.commandCenter.frameworkStatus"),
                null,
                0xFF7ED9A6,
                () -> dispatchCommand("marie status")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.framework.mods",
                COMMAND_CENTER_FRAMEWORK_CATEGORY,
                Component.translatable("config.nourished.commandCenter.frameworkMods"),
                null,
                0xFF7ED9A6,
                () -> dispatchCommand("marie mods")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.framework.api",
                COMMAND_CENTER_FRAMEWORK_CATEGORY,
                Component.translatable("config.nourished.commandCenter.frameworkApi"),
                null,
                0xFF7ED9A6,
                () -> dispatchCommand("marie api")
        ));
        CommandCenterRegistry.registerCard(new CommandCenterCard(
                "nourished.framework.registries",
                COMMAND_CENTER_FRAMEWORK_CATEGORY,
                Component.translatable("config.nourished.commandCenter.frameworkRegistries"),
                null,
                0xFF7ED9A6,
                () -> dispatchCommand("marie registries")
        ));
    }

    /**
     * Sends {@code command} through {@link net.minecraft.client.multiplayer.ClientPacketListener
     * #sendCommand(String)} — the exact method the chat screen calls on command submit — so the
     * server's real command dispatcher runs it and enforces {@code hasPermission(2)} exactly as if
     * the player had typed it. No Nourished/MarieLib helper for client-side command dispatch already
     * existed anywhere in this codebase; this is a thin wrapper around the vanilla method itself,
     * not a new dispatch mechanism.
     */
    private static void dispatchCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.connection.sendCommand(command);
    }

    /** Reset sync state to UNINITIALIZED on disconnect. */
    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MarieClientState.reset();
        MarieClientCache.resetDiagnostics();
        NourishedClientMemoryConfig.resetClientMemoryDiagnostics();
        ActivityLogClientBuffer.reset();
    }
}
