package dev.maire.nourished.core.micro;

import dev.maire.nourished.core.Nourished;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;

/** Payloads for nutrient definitions/values (on login and reload) and a player's nutrient levels. */
public final class MicroNetworking {

    public record SyncMicroLevels(Map<String, Float> levels) implements CustomPacketPayload {
        public static final Type<SyncMicroLevels> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_micro_levels"));
        public static final StreamCodec<FriendlyByteBuf, SyncMicroLevels> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> MicroCodecs.writeFloatMap(buf, p.levels()),
                buf -> new SyncMicroLevels(MicroCodecs.readFloatMap(buf)));

        @Override
        public Type<SyncMicroLevels> type() {
            return TYPE;
        }
    }

    public record SyncMicroData(MicroRegistry.Snapshot snapshot) implements CustomPacketPayload {
        public static final Type<SyncMicroData> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_micro_data"));
        public static final StreamCodec<FriendlyByteBuf, SyncMicroData> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> p.snapshot().write(buf),
                buf -> new SyncMicroData(MicroRegistry.Snapshot.read(buf)));

        @Override
        public Type<SyncMicroData> type() {
            return TYPE;
        }
    }

    private MicroNetworking() {}

    public static void register(PayloadRegistrar registrar,
                                IPayloadHandler<SyncMicroLevels> onLevels,
                                IPayloadHandler<SyncMicroData> onData) {
        registrar.playToClient(SyncMicroLevels.TYPE, SyncMicroLevels.STREAM_CODEC, onLevels);
        registrar.playToClient(SyncMicroData.TYPE, SyncMicroData.STREAM_CODEC, onData);
    }

    public static void syncLevels(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncMicroLevels(MicroAttachment.get(player)));
    }

    public static void syncData(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncMicroData(MicroRegistry.server()));
    }
}
