package dev.maire.nourished.core.network.sync;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.network.ModNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NourishedSyncHandler {

    private static volatile SyncNourishedConfigSnapshot configSnapshot;

    private NourishedSyncHandler() {}

    public static void setConfigSnapshot(SyncNourishedConfigSnapshot snapshot) {
        configSnapshot = snapshot;
    }

    public static SyncNourishedConfigSnapshot getConfigSnapshot() {
        return configSnapshot;
    }

    /** Sends config snapshot and full diet to the joining player. */
    public static void syncOnJoin(ServerPlayer player) {
        SyncNourishedConfigSnapshot snapshot = configSnapshot;
        if (snapshot != null) {
            PacketDistributor.sendToPlayer(player, snapshot);
        }
        DietData diet = player.getData(DietAttachment.DIET.get());
        ModNetworking.syncDiet(player, diet);
    }

    /** Broadcasts a new config snapshot to all connected players after a reload. */
    public static void broadcastConfigReload(MinecraftServer server) {
        SyncNourishedConfigSnapshot snapshot = configSnapshot;
        if (snapshot == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, snapshot);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DietData diet = player.getData(DietAttachment.DIET.get());
            ModNetworking.syncDiet(player, diet);
        }
    }

    /** Delegates diet delta sync to existing payload path. */
    public static void syncDietDelta(ServerPlayer player, DietData diet) {
        ModNetworking.syncDietDelta(player, diet);
    }

    public static void logServerStartupInfo() {
        Nourished.LOGGER.info(
                "[Nourished] Server startup: protocol version {}, features: {}",
                SyncNourishedConfigSnapshot.PROTOCOL_VERSION,
                "config_sync, diet_tracking, memory_system");
    }
}
