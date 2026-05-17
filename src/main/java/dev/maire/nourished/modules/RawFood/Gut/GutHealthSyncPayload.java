package dev.maire.nourished.modules.RawFood.Gut;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network payload for syncing gut health state to client.
 */
@ApiStatus.Internal
public record GutHealthSyncPayload(float gutHealth, float sensitivity) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GutHealthSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_gut_health"));

    public static final StreamCodec<FriendlyByteBuf, GutHealthSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeFloat(payload.gutHealth());
                        buf.writeFloat(payload.sensitivity());
                    },
                    buf -> new GutHealthSyncPayload(buf.readFloat(), buf.readFloat())
            );

    @Override
    public CustomPacketPayload.Type<GutHealthSyncPayload> type() {
        return TYPE;
    }
}
