package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.diet.DietMemoryConfig;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public class DietPlayerEvents {

    private static final AtomicBoolean SNAPSHOT_WARN_ONCE = new AtomicBoolean(false);

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        diet.tick();
        player.setData(DietAttachment.DIET.get(), diet);
        SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
        if (snapshot != null) {
            diet.setMemoryConfig(DietMemoryConfig.fromSnapshot(snapshot));
        } else {
            warnSnapshotNull("join");
            diet.setMemoryConfig(DietMemoryConfig.fromRawConfig(NourishedConfig.get()));
        }
        NourishedSyncHandler.syncOnJoin(player);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
        if (NourishedConfig.get().showJoinMessage()) {
            player.sendSystemMessage(
                    Component.literal("◆ ").withStyle(style -> style.withColor(0xF4C95D))
                            .append(Component.literal("NOURISHED").withStyle(style -> style.withColor(0x6FD3FF).withBold(true)))
                            .append(Component.literal(" ◆ ").withStyle(style -> style.withColor(0xF4C95D)))
                            .append(Component.literal(NourishedConfig.get().joinMessageLine1()).withStyle(style -> style.withColor(0xCFEFFF)))
            );
            String line2 = NourishedConfig.get().joinMessageLine2();
            int split = line2.indexOf(" - ");
            Component line2Body = split >= 0
                    ? Component.literal(line2.substring(0, split + 1))
                            .withStyle(style -> style.withColor(0xFF6B6B).withBold(true))
                            .append(Component.literal(line2.substring(split + 1)).withStyle(style -> style.withColor(0xFFC2C2)))
                    : Component.literal(line2).withStyle(style -> style.withColor(0xFFC2C2));
            player.sendSystemMessage(
                    Component.literal("⚠ ").withStyle(ChatFormatting.RED)
                            .append(line2Body)
            );
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        diet.tick();
        player.setData(DietAttachment.DIET.get(), diet);
        SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
        if (snapshot != null) {
            diet.setMemoryConfig(DietMemoryConfig.fromSnapshot(snapshot));
        } else {
            warnSnapshotNull("respawn");
            diet.setMemoryConfig(DietMemoryConfig.fromRawConfig(NourishedConfig.get()));
        }
        NourishedSyncHandler.syncOnJoin(player);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        SNAPSHOT_WARN_ONCE.set(false);
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        ModNetworking.syncDietDelta(player, diet);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }

    private static void warnSnapshotNull(String action) {
        if (SNAPSHOT_WARN_ONCE.compareAndSet(false, true)) {
            Nourished.LOGGER.warn(
                    "[Nourished] DietPlayerEvents: snapshot null on {}, falling back to raw config. Will not warn again until server restart.",
                    action);
        }
    }
}
