package dev.maire.nourished.core.reload;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import net.minecraft.server.MinecraftServer;

public final class NourishedReloadHelper {

    private NourishedReloadHelper() {}

    public static void reloadAndBroadcast(MinecraftServer server) {
        NourishedSyncHandler.setConfigSnapshot(
                SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get()));
        NourishedSyncHandler.broadcastConfigReload(server);
    }
}
