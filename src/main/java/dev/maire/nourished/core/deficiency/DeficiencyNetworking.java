package dev.maire.nourished.core.deficiency;

import dev.maire.nourished.core.Nourished;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

/** Sends a player's per-group deficiency pressure. {@code announce} is false for silent resyncs (login, respawn). */
public final class DeficiencyNetworking {

    public record SyncDeficiency(Map<String, Integer> pressure, boolean announce) implements CustomPacketPayload {
        public static final Type<SyncDeficiency> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_deficiency"));
        public static final StreamCodec<FriendlyByteBuf, SyncDeficiency> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeMap(p.pressure(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                    buf.writeBoolean(p.announce());
                },
                buf -> new SyncDeficiency(
                        buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt),
                        buf.readBoolean()));

        @Override
        public Type<SyncDeficiency> type() {
            return TYPE;
        }
    }

    private DeficiencyNetworking() {}

    public static void register(PayloadRegistrar registrar, IPayloadHandler<SyncDeficiency> handler) {
        registrar.playToClient(SyncDeficiency.TYPE, SyncDeficiency.STREAM_CODEC, handler);
    }

    public static void sync(ServerPlayer player, boolean announce) {
        PacketDistributor.sendToPlayer(player, new SyncDeficiency(DeficiencyAttachment.get(player), announce));
    }
}
