package dev.maire.nourished.modules.Stamina.Core;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network payload for syncing stamina state to client.
 */
@ApiStatus.Internal
public record StaminaSyncPayload(
        float physicalStamina,
        float physicalMax,
        float physicalFatiguePenalty,
        float physicalBonusStamina,
        float physicalDebt,
        float mentalStamina,
        float mentalMax,
        float mentalFatiguePenalty,
        float mentalBonusStamina,
        float mentalDebt
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StaminaSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_stamina"));

    public static final StreamCodec<FriendlyByteBuf, StaminaSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeFloat(payload.physicalStamina());
                        buf.writeFloat(payload.physicalMax());
                        buf.writeFloat(payload.physicalFatiguePenalty());
                        buf.writeFloat(payload.physicalBonusStamina());
                        buf.writeFloat(payload.physicalDebt());
                        buf.writeFloat(payload.mentalStamina());
                        buf.writeFloat(payload.mentalMax());
                        buf.writeFloat(payload.mentalFatiguePenalty());
                        buf.writeFloat(payload.mentalBonusStamina());
                        buf.writeFloat(payload.mentalDebt());
                    },
                    buf -> new StaminaSyncPayload(
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat()
                    )
            );

    @Override
    public CustomPacketPayload.Type<StaminaSyncPayload> type() {
        return TYPE;
    }
}
