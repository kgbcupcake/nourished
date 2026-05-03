package dev.maire.nourished.network;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.client.NourishedToastManager;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncDietPayload.TYPE,
                SyncDietPayload.STREAM_CODEC,
                ModNetworking::handleSyncDiet
        );
    }

    public static void syncDiet(ServerPlayer player, DietData diet) {
        PacketDistributor.sendToPlayer(player, new SyncDietPayload(diet));
    }

    private static void handleSyncDiet(SyncDietPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DietData next = payload.diet();
            NourishedToastManager.onClientDietUpdated(next);
            ClientDietCache.set(next);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(DietAttachment.DIET.get(), next);
            }
        });
    }

    public record SyncDietPayload(DietData diet) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncDietPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_diet"));

        public static final StreamCodec<FriendlyByteBuf, SyncDietPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.fromCodec(DietData.CODEC),
                        SyncDietPayload::diet,
                        SyncDietPayload::new
                );

        @Override
        public CustomPacketPayload.Type<SyncDietPayload> type() {
            return TYPE;
        }
    }
}
